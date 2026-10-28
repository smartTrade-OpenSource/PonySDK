/*
 * Copyright (c) 2017 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.server.websocket;

import com.ponysdk.core.model.ArrayValueModel;
import com.ponysdk.core.model.BooleanModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.ValueTypeModel;
import com.ponysdk.core.server.application.StringDictionary;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Accumulates binary protocol data into a ByteBuffer and flushes it
 * asynchronously via Jetty 12 {@link Session#sendBinary(ByteBuffer, Callback)}.
 * <p>
 * Writes are single-threaded (the UIContext lock guarantees this).
 * Sends are serialized: one sendBinary at a time, in flush order. A flush made while the client is still reading
 * the previous message does not wait for it: it joins a short per-session queue that the send callback drains
 * (see {@link Sender}); only a client stuck beyond that queue makes the flushing thread wait, then times out.
 * <p>
 * Optimizations:
 * - Pooled buffers: the write buffer is borrowed from a {@link ByteBufferPool} and handed off to
 *   Jetty on flush; the Sender callback returns it to the pool (no per-flush copy)
 * - Reusable byte[] for string encoding (avoids per-string allocation)
 * - Factored string encoding logic (putStringBytes) shared by all string paths
 */
public class WebSocketPusher implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(WebSocketPusher.class);

    private static final int MAX_UNSIGNED_BYTE_VALUE = Byte.MAX_VALUE * 2 + 1;
    private static final int MAX_UNSIGNED_SHORT_VALUE = Short.MAX_VALUE * 2 + 1;
    private static final int MODEL_KEY_SIZE = 1;
    private static final int MIN_PROFITABLE_STRING_LENGTH = 4;
    /** First ASCII length that needs an explicit tag: below it, the length byte itself is the tag. */
    private static final int MIN_TAGGED_ASCII_LENGTH = ValueTypeModel.STRING_DICTIONARY_ADD;

    /** Initial size of the reusable string encoding buffer. */
    private static final int INITIAL_STRING_BUFFER_SIZE = 256;

    /** Maximum buffer size to prevent runaway growth. */
    private static final int MAX_BUFFER_SIZE = 1 << 20; // 1 MB

    private final Session session;
    private final long timeoutMillis;
    private final int maxChunkSize;
    private final ByteBufferPool bufferPool;

    // ── Pooled buffering ──
    // writeBuffer: borrowed from the pool on first write, handed off to Jetty on flush
    // (returned to the pool by the Sender callback), or released on close.
    private ByteBuffer writeBuffer;

    // ── Reusable byte[] for string UTF-8 encoding ──
    private byte[] stringEncodeBuffer = new byte[INITIAL_STRING_BUFFER_SIZE];
    /**
     * Bytes of the last encoded string: {@link #stringEncodeBuffer} for short strings, the JDK's own array for long
     * ones (written from it, not copied into the buffer, which therefore stays sized by short strings).
     */
    private byte[] encodedBytes;
    /** Set by {@link #encodeStringToBuffer} — true if the last encoded string was pure ASCII. */
    private boolean lastEncodedAscii;

    private volatile boolean closed;
    private volatile Throwable asyncError;

    /** Sends the flushed buffers in order, one at a time (a single reusable Callback). */
    private final Sender sender = new Sender();

    private static volatile WebSocketStatsRecorder statsRecorder;
    private int metaBytes;

    private WebSocket.Listener listener;
    private StringDictionary stringDictionary;

    public WebSocketPusher(final Session session, final int maxChunkSize, final long timeoutMillis,
                           final ByteBufferPool bufferPool) {
        this.session = session;
        this.timeoutMillis = timeoutMillis;
        this.maxChunkSize = maxChunkSize;
        this.bufferPool = bufferPool;
        // writeBuffer is acquired lazily on first write
    }

    // ── Buffer write primitives ──────────────────────────────────────────

    private void ensureCapacity(final int needed) throws IOException {
        // Hot path: room in the current buffer and no proactive chunk flush due. Closed / async-error
        // state is checked once per encode() / flush() instead of on every primitive put.
        final ByteBuffer b = writeBuffer;
        if (b != null && b.remaining() >= needed && b.position() < maxChunkSize) return;
        ensureCapacitySlow(needed);
    }

    private void ensureCapacitySlow(final int needed) throws IOException {
        checkState();
        // Lazy acquire: borrow a buffer from the pool on first write
        if (writeBuffer == null) {
            writeBuffer = bufferPool.acquire();
        }
        // Proactive chunking: if the buffer already has maxChunkSize bytes written,
        // flush now to keep WebSocket frames small and let heartbeats through.
        if (writeBuffer.position() >= maxChunkSize && writeBuffer.remaining() >= needed) {
            flush();
        }
        // flush() hands the buffer off to Jetty and nulls it — re-acquire before using it again.
        if (writeBuffer == null) {
            writeBuffer = bufferPool.acquire();
        }
        if (writeBuffer.remaining() < needed) {
            // Try flushing first to free the buffer
            if (writeBuffer.position() > 0) {
                flush();
            }
            // After flush, writeBuffer is a fresh buffer from the pool
            // If still not enough (single value > pool buffer capacity), grow
            if (writeBuffer == null) {
                writeBuffer = bufferPool.acquire();
            }
            if (writeBuffer.remaining() < needed) {
                growBuffer(needed);
            }
        }
    }

    /**
     * Grows the write buffer to accommodate the needed bytes.
     * Preserves any data already in the write buffer.
     * Note: grown buffers won't be returned to the pool (wrong capacity).
     */
    private void growBuffer(final int needed) throws IOException {
        final int required = writeBuffer.position() + needed;
        int newSize = writeBuffer.capacity();
        while (newSize < required) {
            newSize = Math.min(newSize * 2, MAX_BUFFER_SIZE);
            if (newSize < required) {
                if (newSize >= MAX_BUFFER_SIZE) {
                    throw new IOException("Write of " + needed + " bytes exceeds max buffer size " + MAX_BUFFER_SIZE);
                }
            }
        }
        if (log.isDebugEnabled()) log.debug("Growing WebSocket buffer from {} to {} bytes", writeBuffer.capacity(), newSize);

        final ByteBuffer newWrite = ByteBuffer.allocateDirect(newSize);
        writeBuffer.flip();
        newWrite.put(writeBuffer);
        // Return the old (pool-sized) buffer to the pool
        bufferPool.release(writeBuffer);
        writeBuffer = newWrite;
    }

    private void checkState() throws IOException {
        if (closed) throw new IOException("WebSocketPusher is closed");
        final Throwable err = asyncError;
        if (err != null) {
            asyncError = null;
            close();
            throw new IOException("Async send failed", err);
        }
    }

    public final void put(final byte b) throws IOException {
        ensureCapacity(1);
        writeBuffer.put(b);
    }

    public final void putShort(final short s) throws IOException {
        ensureCapacity(2);
        writeBuffer.putShort(s);
    }

    public final void putInt(final int i) throws IOException {
        ensureCapacity(4);
        writeBuffer.putInt(i);
    }

    public final void putLong(final long l) throws IOException {
        ensureCapacity(8);
        writeBuffer.putLong(l);
    }

    public final void putFloat(final float f) throws IOException {
        ensureCapacity(4);
        writeBuffer.putFloat(f);
    }

    public final void putDouble(final double d) throws IOException {
        ensureCapacity(8);
        writeBuffer.putDouble(d);
    }

    public final void put(final byte[] bytes) throws IOException {
        put(bytes, 0, bytes.length);
    }

    public final void put(final byte[] bytes, int offset, int length) throws IOException {
        while (length > 0) {
            // Pool capacity, not writeBuffer.capacity(): writeBuffer is null before the first write
            // and right after a flush (it is acquired lazily by ensureCapacity).
            ensureCapacity(Math.min(length, bufferPool.bufferCapacity()));
            final int chunk = Math.min(length, writeBuffer.remaining());
            writeBuffer.put(bytes, offset, chunk);
            offset += chunk;
            length -= chunk;
        }
    }

    // ── Reusable string encoding ─────────────────────────────────────────

    /**
     * From this length on, String.getBytes(UTF_8) (JDK intrinsic, vectorized copy of a Latin-1 string)
     * beats the per-char loop despite its allocation: measured equal at 64 chars, 1.7x faster at 200,
     * 3.5x at 1000+ for ASCII (JSON, HTML), equal for accented text.
     */
    static final int JDK_ENCODER_MIN_LENGTH = 128;

    /** Encodes a string to UTF-8 into {@link #encodedBytes} and returns its byte length. */
    private int encodeStringToBuffer(final String value) {
        final int len = value.length();

        if (len >= JDK_ENCODER_MIN_LENGTH) {
            final byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
            encodedBytes = utf8;
            // UTF-8 length == char count only if every char is ASCII (an unpaired surrogate becomes
            // '?', a valid ASCII byte, exactly like encodeUtf8 below)
            lastEncodedAscii = utf8.length == len;
            return utf8.length;
        }

        encodedBytes = stringEncodeBuffer;
        // Fast path for ASCII: copy each char in a single pass, bailing to the UTF-8 path on the
        // first non-ASCII char. A sampling heuristic is unsafe here — a missed non-ASCII char would
        // be silently truncated by the (byte) cast and corrupt the value on the wire.
        if (len > stringEncodeBuffer.length) {
            stringEncodeBuffer = new byte[Math.max(len, stringEncodeBuffer.length * 2)];
            encodedBytes = stringEncodeBuffer;
        }
        int i = 0;
        for (; i < len; i++) {
            final char c = value.charAt(i);
            if (c > 127) break; // non-ASCII → fall back to the UTF-8 encoder below
            stringEncodeBuffer[i] = (byte) c;
        }
        if (i == len) {
            lastEncodedAscii = true;
            return len;
        }

        // Non-ASCII: encode the remainder by hand, continuing after the ASCII prefix already copied.
        // Worst case is 3 bytes per UTF-16 char (a surrogate pair is 2 chars -> 4 bytes).
        final int maxBytes = i + (len - i) * 3;
        if (maxBytes > stringEncodeBuffer.length) {
            stringEncodeBuffer = Arrays.copyOf(stringEncodeBuffer, Math.max(maxBytes, stringEncodeBuffer.length * 2));
            encodedBytes = stringEncodeBuffer;
        }
        lastEncodedAscii = false;
        return encodeUtf8(value, i, len, stringEncodeBuffer, i);
    }

    /**
     * Zero-allocation UTF-8 encoding of {@code value[from, to)} into {@code out} at {@code pos}.
     * Unpaired surrogates are replaced by {@code '?'}, like {@link String#getBytes}: a CharsetEncoder
     * reports them as malformed and stops, which silently truncated the value on the wire.
     *
     * @return the position after the last byte written
     */
    static int encodeUtf8(final String value, final int from, final int to, final byte[] out, int pos) {
        for (int i = from; i < to; i++) {
            final char c = value.charAt(i);
            if (c < 0x80) {
                out[pos++] = (byte) c;
            } else if (c < 0x800) {
                out[pos++] = (byte) (0xC0 | c >> 6);
                out[pos++] = (byte) (0x80 | c & 0x3F);
            } else if (Character.isSurrogate(c)) {
                final char low;
                if (Character.isHighSurrogate(c) && i + 1 < to && Character.isLowSurrogate(low = value.charAt(i + 1))) {
                    final int cp = Character.toCodePoint(c, low);
                    out[pos++] = (byte) (0xF0 | cp >> 18);
                    out[pos++] = (byte) (0x80 | cp >> 12 & 0x3F);
                    out[pos++] = (byte) (0x80 | cp >> 6 & 0x3F);
                    out[pos++] = (byte) (0x80 | cp & 0x3F);
                    i++;
                } else {
                    out[pos++] = '?';
                }
            } else {
                out[pos++] = (byte) (0xE0 | c >> 12);
                out[pos++] = (byte) (0x80 | c >> 6 & 0x3F);
                out[pos++] = (byte) (0x80 | c & 0x3F);
            }
        }
        return pos;
    }

    // ── Factored string writing ──────────────────────────────────────────

    /**
     * Writes the length-prefixed string bytes (ASCII/UTF-8 aware) to the buffer.
     * This is the single factored method used by putString, putStringRaw,
     * writeStringOrRef (dictionary add), and putArrayStringBytes.
     *
     * @return the number of data bytes written (excluding length prefix)
     */
    private int putStringBytes(final String value, final int byteLength) throws IOException {
        final boolean ascii = lastEncodedAscii;
        if (ascii) {
            // The 1-byte length form must stay below the dictionary tags (248 ADD, 249 REF): lengths
            // 248-250 used to be read back as a dictionary entry and corrupted the stream.
            if (byteLength < MIN_TAGGED_ASCII_LENGTH) {
                putUnsignedByte((short) byteLength);
            } else if (byteLength <= MAX_UNSIGNED_SHORT_VALUE) {
                putUnsignedByte(ValueTypeModel.STRING_ASCII_UINT16);
                putUnsignedShort(byteLength);
            } else {
                putUnsignedByte(ValueTypeModel.STRING_ASCII_UINT32);
                putInt(byteLength);
            }
        } else {
            if (byteLength <= MAX_UNSIGNED_BYTE_VALUE) {
                putUnsignedByte(ValueTypeModel.STRING_UTF8_UINT8);
                putUnsignedByte((short) byteLength);
            } else if (byteLength <= MAX_UNSIGNED_SHORT_VALUE) {
                putUnsignedByte(ValueTypeModel.STRING_UTF8_UINT16);
                putUnsignedShort(byteLength);
            } else {
                putUnsignedByte(ValueTypeModel.STRING_UTF8_INT32);
                putInt(byteLength);
            }
        }
        put(encodedBytes, 0, byteLength);
        return byteLength;
    }

    /**
     * Returns the number of meta bytes used by the string length prefix.
     */
    private static int stringMetaBytes(final boolean ascii, final int byteLength) {
        if (ascii) {
            if (byteLength < MIN_TAGGED_ASCII_LENGTH) return 1;
            else if (byteLength <= MAX_UNSIGNED_SHORT_VALUE) return 3;
            else return 5;
        } else {
            if (byteLength <= MAX_UNSIGNED_BYTE_VALUE) return 2;
            else if (byteLength <= MAX_UNSIGNED_SHORT_VALUE) return 3;
            else return 5;
        }
    }

    // ── Flush / Close (double-buffering) ─────────────────────────────────

    /**
     * Flushes accumulated data to the WebSocket as a single binary message.
     * <p>
     * The current writeBuffer is handed off for an async send (or queued behind the one in flight).
     * The Sender callback returns it to the pool when Jetty is done.
     * A new writeBuffer will be lazily acquired from the pool on next write.
     */
    public void flush() throws IOException {
        checkState();
        if (writeBuffer == null || writeBuffer.position() == 0) return;

        // Reset eviction budget for next write cycle
        if (stringDictionary != null) stringDictionary.resetEvictionBudget();

        // Hand off the writeBuffer to Jetty for sending
        writeBuffer.flip();
        final ByteBuffer toSend = writeBuffer;
        writeBuffer = null; // will be lazily acquired on next write

        final int bytes = toSend.remaining();
        if (log.isDebugEnabled()) log.debug("Flushing {} bytes", bytes);

        protocolBytesSent += bytes;
        if (!sender.send(toSend)) {
            checkState(); // a send failed while this flush waited: that error, not a timeout
            close();
            throw new IOException("Timeout waiting for previous WebSocket send to complete");
        }

        if (listener != null) {
            listener.onOutgoingPonyFramesBytes(bytes);
            sampleOutgoingWireBytes();
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            // Return any held writeBuffer to the pool
            if (writeBuffer != null) {
                bufferPool.release(writeBuffer);
                writeBuffer = null;
            }
            sender.discard();
            if (session.isOpen()) {
                session.close(StatusCode.NORMAL, "close", Callback.NOOP);
            }
        }
    }

    public boolean isClosed() {
        return closed;
    }

    // ── Protocol encoding ────────────────────────────────────────────────

    /**
     * Writes one protocol field, the model key then its typed value, into the current buffer: what every update is
     * made of. Public for tools that show the encoding (the showcase's frame anatomy).
     *
     * @param value The type can be primitives, String or Object[]
     */
    public void encode(final ServerToClientModel model, final Object value) throws IOException {
        checkState();
        switch (model.getTypeModel()) {
            case NULL -> write(model);
            case BOOLEAN -> write(model, (boolean) value);
            case BYTE -> write(model, (byte) value);
            case SHORT -> write(model, (short) value);
            case UINT31 -> writeUint31(model, (int) value);
            case INTEGER -> write(model, (int) value);
            case LONG -> write(model, (long) value);
            case DOUBLE -> write(model, (double) value);
            case FLOAT -> write(model, (float) value);
            case STRING -> writeStringOrRef(model, (String) value);
            case ARRAY -> write(model, (Object[]) value);
            default -> log.error("Unknown model type : {}", model.getTypeModel());
        }
    }

    /** Primitive int variant of {@link #encode}: no boxing for ids and other int values. */
    void encodeInt(final ServerToClientModel model, final int value) throws IOException {
        checkState();
        switch (model.getTypeModel()) {
            case UINT31 -> writeUint31(model, value);
            case INTEGER -> write(model, value);
            default -> encode(model, (Object) value); // same conversions/errors as the boxed path
        }
    }

    private void write(final ServerToClientModel model) throws IOException {
        putModelKey(model);
        if (statsRecorder != null) record(model, null, MODEL_KEY_SIZE, 0);
    }

    private void write(final ServerToClientModel model, final boolean value) throws IOException {
        write(model, value ? BooleanModel.TRUE.getValue() : BooleanModel.FALSE.getValue());
    }

    private void write(final ServerToClientModel model, final byte value) throws IOException {
        putModelKey(model);
        put(value);
        if (statsRecorder != null) record(model, value, MODEL_KEY_SIZE, Byte.BYTES);
    }

    private void write(final ServerToClientModel model, final short value) throws IOException {
        putModelKey(model);
        putShort(value);
        if (statsRecorder != null) record(model, value, MODEL_KEY_SIZE, Short.BYTES);
    }

    private void write(final ServerToClientModel model, final int value) throws IOException {
        putModelKey(model);
        putInt(value);
        if (statsRecorder != null) record(model, value, MODEL_KEY_SIZE, Integer.BYTES);
    }

    private void write(final ServerToClientModel model, final long longValue) throws IOException {
        putModelKey(model);
        putLong(longValue);
        if (statsRecorder != null) record(model, longValue, MODEL_KEY_SIZE, Long.BYTES);
    }

    private void write(final ServerToClientModel model, final double doubleValue) throws IOException {
        putModelKey(model);
        putDouble(doubleValue);
        if (statsRecorder != null) record(model, doubleValue, MODEL_KEY_SIZE, Double.BYTES);
    }

    private void write(final ServerToClientModel model, final float floatValue) throws IOException {
        putModelKey(model);
        putFloat(floatValue);
        if (statsRecorder != null) record(model, floatValue, MODEL_KEY_SIZE, Float.BYTES);
    }

    private void write(final ServerToClientModel model, final Object[] value) throws IOException {
        putModelKey(model);
        // Length: 0..254 in a single byte; 255 is an escape meaning a uint31 length (2 or 4 bytes)
        // follows. This lifts the old 255-element cap while keeping small arrays at one length byte.
        final int lengthBytes;
        if (value.length < ArrayValueModel.LENGTH_UINT31_ESCAPE) {
            putUnsignedByte((short) value.length);
            lengthBytes = 1;
        } else {
            putUnsignedByte((short) ArrayValueModel.LENGTH_UINT31_ESCAPE);
            lengthBytes = 1 + putUint31(value.length);
        }
        metaBytes = MODEL_KEY_SIZE + lengthBytes;
        int dataBytes = 0;
        for (final Object o : value) {
            dataBytes += putArrayElement(o);
        }
        if (statsRecorder != null) record(model, value, metaBytes, dataBytes, Arrays::toString);
    }

    private void writeUint31(final ServerToClientModel model, final int value) throws IOException {
        putModelKey(model);
        final int bytes = putUint31(value);
        if (statsRecorder != null) record(model, value, MODEL_KEY_SIZE, bytes);
    }

    private int putUint31(final int value) throws IOException {
        if (value < 0) {
            throw new IllegalArgumentException("Invalid UINT31 value : " + value + " must be unsigned");
        } else if (value <= Short.MAX_VALUE) {
            putShort((short) value);
            return Short.BYTES;
        } else {
            putInt(value | 0x80_00_00_00);
            return Integer.BYTES;
        }
    }

    private int putCompressedLong(final long value) throws IOException {
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            put(ArrayValueModel.BYTE.getValue());
            put((byte) value);
            return Byte.BYTES;
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            put(ArrayValueModel.SHORT.getValue());
            putShort((short) value);
            return Short.BYTES;
        } else if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            put(ArrayValueModel.INTEGER.getValue());
            putInt((int) value);
            return Integer.BYTES;
        } else {
            put(ArrayValueModel.LONG.getValue());
            putLong(value);
            return Long.BYTES;
        }
    }

    private int putCompressedInt(final int value) throws IOException {
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            put(ArrayValueModel.BYTE.getValue());
            put((byte) value);
            return Byte.BYTES;
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            put(ArrayValueModel.SHORT.getValue());
            putShort((short) value);
            return Short.BYTES;
        } else {
            put(ArrayValueModel.INTEGER.getValue());
            putInt(value);
            return Integer.BYTES;
        }
    }

    private int putCompressedShort(final short value) throws IOException {
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            put(ArrayValueModel.BYTE.getValue());
            put((byte) value);
            return Byte.BYTES;
        } else {
            put(ArrayValueModel.SHORT.getValue());
            putShort(value);
            return Short.BYTES;
        }
    }

    private int putCompressedDouble(final double value) throws IOException {
        final float f = (float) value;
        if (f == value) {
            put(ArrayValueModel.FLOAT.getValue());
            putFloat(f);
            return Float.BYTES;
        } else {
            put(ArrayValueModel.DOUBLE.getValue());
            putDouble(value);
            return Double.BYTES;
        }
    }

    private int putArrayElement(final Object o) throws IOException {
        metaBytes++;
        if (o == null) {
            put(ArrayValueModel.NULL.getValue());
            return 0;
        } else if (o instanceof Integer i) {
            return putCompressedInt(i);
        } else if (o instanceof String s) {
            return putArrayStringElementOrRef(s);
        } else if (o instanceof Byte b) {
            put(ArrayValueModel.BYTE.getValue());
            put(b);
            return Byte.BYTES;
        } else if (o instanceof Short s) {
            return putCompressedShort(s);
        } else if (o instanceof Boolean b) {
            put(b ? ArrayValueModel.BOOLEAN_TRUE.getValue() : ArrayValueModel.BOOLEAN_FALSE.getValue());
            return 0;
        } else if (o instanceof Long l) {
            return putCompressedLong(l);
        } else if (o instanceof Double d) {
            return putCompressedDouble(d);
        } else if (o instanceof Float f) {
            put(ArrayValueModel.FLOAT.getValue());
            putFloat(f);
            return Float.BYTES;
        } else {
            return putArrayStringElementOrRef(o.toString());
        }
    }

    private int putArrayStringElement(final String s) throws IOException {
        final int byteLength = encodeStringToBuffer(s);
        final boolean ascii = lastEncodedAscii;

        if (byteLength <= MAX_UNSIGNED_BYTE_VALUE) {
            put(ascii ? ArrayValueModel.STRING_ASCII_UINT8_LENGTH.getValue()
                    : ArrayValueModel.STRING_UTF8_UINT8_LENGTH.getValue());
            putUnsignedByte((short) byteLength);
            metaBytes++;
        } else if (byteLength <= MAX_UNSIGNED_SHORT_VALUE) {
            put(ascii ? ArrayValueModel.STRING_ASCII_UINT16_LENGTH.getValue()
                    : ArrayValueModel.STRING_UTF8_UINT16_LENGTH.getValue());
            putUnsignedShort(byteLength);
            metaBytes += 2;
        } else {
            put(ArrayValueModel.STRING_UTF8_UINT32_LENGTH.getValue());
            putInt(byteLength);
            metaBytes += 4;
        }

        put(encodedBytes, 0, byteLength);
        return byteLength;
    }

    private void write(final ServerToClientModel model, final String value) throws IOException {
        putModelKey(model);
        int metaBytes = MODEL_KEY_SIZE;
        int dataBytes;
        if (value != null) {
            final int byteLength = encodeStringToBuffer(value);
            metaBytes += stringMetaBytes(lastEncodedAscii, byteLength);
            putStringBytes(value, byteLength);
            dataBytes = byteLength;
        } else {
            putUnsignedByte((short) 0);
            metaBytes += 1;
            dataBytes = 0;
        }
        if (statsRecorder != null) record(model, value, metaBytes, dataBytes);
    }

    private void putModelKey(final ServerToClientModel model) throws IOException {
        putUnsignedByte(model.getValue());
    }

    public final void putUnsignedByte(final short shortValue) throws IOException {
        put((byte) (shortValue & 0xFF));
    }

    public final void putUnsignedShort(final int intValue) throws IOException {
        putShort((short) (intValue & 0xFFFF));
    }

    // ── Stats recording ──────────────────────────────────────────────────
    // Every write site guards with "statsRecorder != null" before calling record(): the arguments
    // (boxed primitives, "ref:" + id) would otherwise be built on every write even though stats
    // recording is off in production.

    public static synchronized boolean startRecordingStats(final long timeout, final TimeUnit unit,
                                                           final Consumer<WebSocketStats> listener,
                                                           final BiFunction<ServerToClientModel, Object, String> groupBy) {
        if (statsRecorder != null) return false;
        statsRecorder = new WebSocketStatsRecorder(stats -> {
            synchronized (WebSocketPusher.class) {
                statsRecorder = null;
            }
            if (listener != null) listener.accept(stats);
        }, groupBy);
        return statsRecorder.start(timeout, unit);
    }

    public static boolean isRecordingStats() {
        final WebSocketStatsRecorder recorder = statsRecorder;
        return recorder != null && recorder.isStarted();
    }

    public static WebSocketStats stopRecordingStats() {
        final WebSocketStatsRecorder recorder = statsRecorder;
        return recorder != null ? recorder.stop() : null;
    }

    private static <T> void record(final ServerToClientModel model, final T value, final int metaBytes, final int dataBytes,
                                   final Function<T, Object> valueConverter) {
        final WebSocketStatsRecorder recorder = statsRecorder;
        if (recorder != null) recorder.record(model, value, metaBytes, dataBytes, valueConverter);
    }

    private static <T> void record(final ServerToClientModel model, final T value, final int metaBytes, final int dataBytes) {
        final WebSocketStatsRecorder recorder = statsRecorder;
        if (recorder != null) recorder.record(model, value, metaBytes, dataBytes);
    }

    // Jetty 12 connection exposing true post-compression wire byte counters (set by WebSocket at open).
    private org.eclipse.jetty.websocket.core.WebSocketConnection connection;
    // volatile: written on the flush path and re-based on the app thread (rebaseWireBytes)
    private volatile long lastBytesOut;

    void setConnection(final org.eclipse.jetty.websocket.core.WebSocketConnection connection) {
        this.connection = connection;
    }

    // Protocol bytes handed to Jetty (before permessage-deflate). Written on the flush path, read for display.
    private volatile long protocolBytesSent;

    long getProtocolBytesSent() {
        return protocolBytesSent;
    }

    /** Bytes written on the socket (framing + compressed payload), or -1 if the Jetty counter is unavailable. */
    long getWireBytesSent() {
        final org.eclipse.jetty.websocket.core.WebSocketConnection c = connection;
        return c != null ? c.getBytesOut() : -1;
    }

    /**
     * OUTGOING on-the-wire (post-compression) bytes, as a delta of the Jetty connection counter, reported to
     * {@link WebSocket.Listener#onOutgoingWebSocketFrame}. Per-sample values are approximate (sendBinary is
     * async), window totals are exact.
     */
    private void sampleOutgoingWireBytes() {
        final org.eclipse.jetty.websocket.core.WebSocketConnection c = connection;
        if (c == null) return;
        long delta = c.getBytesOut() - lastBytesOut;
        lastBytesOut += delta;
        while (delta > 0) { // int-typed callback: split, never overflow on long sessions
            final int chunk = (int) Math.min(delta, Integer.MAX_VALUE);
            listener.onOutgoingWebSocketFrame(0, chunk);
            delta -= chunk;
        }
    }

    /** Re-baselines the outgoing wire counter, so the first sample covers only traffic after the listener is set. */
    void rebaseWireBytes() {
        final org.eclipse.jetty.websocket.core.WebSocketConnection c = connection;
        if (c != null) lastBytesOut = c.getBytesOut();
    }

    void setWebSocketListener(final WebSocket.Listener listener) {
        this.listener = listener;
    }

    public void setStringDictionary(final StringDictionary stringDictionary) {
        this.stringDictionary = stringDictionary;
    }

    public StringDictionary getStringDictionary() {
        return stringDictionary;
    }

    // ── String Dictionary protocol ───────────────────────────────────────

    private void writeStringRef(final ServerToClientModel model, final int id) throws IOException {
        putModelKey(model);
        putUnsignedByte(ValueTypeModel.STRING_DICTIONARY_REF);
        final int idBytes = putUint31(id);
        if (statsRecorder != null) record(model, "ref:" + id, MODEL_KEY_SIZE + 1, idBytes);
    }

    private void writeStringOrRef(final ServerToClientModel model, final String value) throws IOException {
        if (stringDictionary == null) {
            write(model, value);
            return;
        }

        if (value == null) {
            write(model, (String) null);
            return;
        }

        if (value.length() < MIN_PROFITABLE_STRING_LENGTH) {
            final int existingId = stringDictionary.getId(value);
            if (existingId != StringDictionary.NOT_INTERNED) {
                writeStringRef(model, existingId);
            } else {
                write(model, value);
            }
            return;
        }

        final long result = stringDictionary.internOrGet(value);
        final int id = StringDictionary.InternResult.id(result);

        if (id == StringDictionary.NOT_INTERNED) {
            write(model, value);
            return;
        }

        if (StringDictionary.InternResult.isExisting(result)) {
            writeStringRef(model, id);
            return;
        }

        // New dictionary entry: inline add
        putModelKey(model);
        putUnsignedByte(ValueTypeModel.STRING_DICTIONARY_ADD);
        final int idBytes = putUint31(id);
        final int byteLength = encodeStringToBuffer(value);
        putStringBytes(value, byteLength);
        if (statsRecorder != null) record(model, value, MODEL_KEY_SIZE + 1 + idBytes, byteLength);
    }

    private int putArrayStringElementOrRef(final String s) throws IOException {
        if (stringDictionary == null) {
            return putArrayStringElement(s);
        }

        if (s.length() < MIN_PROFITABLE_STRING_LENGTH) {
            final int existingId = stringDictionary.getId(s);
            if (existingId != StringDictionary.NOT_INTERNED) {
                put(ArrayValueModel.STRING_DICTIONARY_REF.getValue());
                final int idBytes = putUint31(existingId);
                metaBytes += idBytes;
                return 0;
            }
            return putArrayStringElement(s);
        }

        final long result = stringDictionary.internOrGet(s);
        final int id = StringDictionary.InternResult.id(result);

        if (id == StringDictionary.NOT_INTERNED) {
            return putArrayStringElement(s);
        }

        if (StringDictionary.InternResult.isExisting(result)) {
            put(ArrayValueModel.STRING_DICTIONARY_REF.getValue());
            final int idBytes = putUint31(id);
            metaBytes += idBytes;
            return 0;
        }

        // New dictionary entry in array context
        final int byteLength = encodeStringToBuffer(s);
        final boolean ascii = lastEncodedAscii;
        put(ArrayValueModel.STRING_DICTIONARY_ADD.getValue());
        final int idBytes = putUint31(id);
        metaBytes += idBytes;

        if (byteLength <= MAX_UNSIGNED_BYTE_VALUE) {
            put(ascii ? ArrayValueModel.STRING_ASCII_UINT8_LENGTH.getValue()
                    : ArrayValueModel.STRING_UTF8_UINT8_LENGTH.getValue());
            putUnsignedByte((short) byteLength);
            metaBytes++;
        } else if (byteLength <= MAX_UNSIGNED_SHORT_VALUE) {
            put(ascii ? ArrayValueModel.STRING_ASCII_UINT16_LENGTH.getValue()
                    : ArrayValueModel.STRING_UTF8_UINT16_LENGTH.getValue());
            putUnsignedShort(byteLength);
            metaBytes += 2;
        } else {
            put(ArrayValueModel.STRING_UTF8_UINT32_LENGTH.getValue());
            putInt(byteLength);
            metaBytes += 4;
        }

        put(encodedBytes, 0, byteLength);
        return byteLength;
    }

    // ── Sends ────────────────────────────────────────────────────────────

    /** Flushed buffers waiting behind the one in flight, at most: a client stuck past them blocks the flush. */
    static final int QUEUE_LIMIT = 4;

    /**
     * Sends flushed buffers one at a time, in order. A buffer flushed while a send is in flight waits in a short
     * queue, appended to the last waiting buffer when it fits (so a burst of small flushes leaves as one message),
     * and the completion callback sends the next one: the thread that writes to a slow client does not wait for it.
     * The queue is bounded ({@link #QUEUE_LIMIT} pooled buffers): a client that stays stuck past it makes the flush
     * wait, then time out, as when every flush waited for the previous send.
     */
    private final class Sender implements Callback {

        private final ReentrantLock lock = new ReentrantLock();
        private final Condition room = lock.newCondition();
        private final ArrayDeque<ByteBuffer> queue = new ArrayDeque<>(QUEUE_LIMIT);
        /** The buffer Jetty is sending (back to the pool on completion), null when idle. Guarded by lock. */
        private ByteBuffer inFlight;

        /** @return false, the buffer back to the pool, if the queue stayed full for the send timeout or a send failed meanwhile */
        boolean send(final ByteBuffer buffer) throws IOException {
            lock.lock();
            try {
                if (inFlight != null) {
                    final ByteBuffer last = queue.peekLast();
                    if (last != null && last.capacity() - last.limit() >= buffer.remaining()) {
                        final int end = last.limit();
                        last.limit(last.capacity()).position(end);
                        last.put(buffer).flip();
                        bufferPool.release(buffer);
                        return true;
                    }
                    long nanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
                    while (inFlight != null && queue.size() >= QUEUE_LIMIT) {
                        if (nanos <= 0) {
                            bufferPool.release(buffer);
                            return false;
                        }
                        nanos = room.awaitNanos(nanos);
                    }
                    if (inFlight != null) {
                        queue.addLast(buffer);
                        return true;
                    }
                    if (asyncError != null) { // a send failed while this one waited: the connection is gone
                        bufferPool.release(buffer);
                        return false;
                    }
                    // else the client caught up and the queue drained: this buffer goes out now
                }
                inFlight = buffer;
            } catch (final InterruptedException e) {
                bufferPool.release(buffer);
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted waiting for WebSocket send", e);
            } finally {
                lock.unlock();
            }
            session.sendBinary(buffer, this); // outside the lock: Jetty may complete it right here
            return true;
        }

        @Override
        public void succeed() {
            final ByteBuffer next;
            lock.lock();
            try {
                bufferPool.release(inFlight);
                next = inFlight = queue.pollFirst();
                room.signalAll();
            } finally {
                lock.unlock();
            }
            if (next != null) session.sendBinary(next, this);
        }

        @Override
        public void fail(final Throwable t) {
            log.error("sendBinary failed", t);
            asyncError = t;
            lock.lock();
            try {
                bufferPool.release(inFlight);
                inFlight = null;
            } finally {
                lock.unlock();
            }
            discard(); // the next write surfaces the error and closes
        }

        /** Drops what has not been sent yet (closed or failed connection). */
        void discard() {
            lock.lock();
            try {
                for (ByteBuffer b; (b = queue.pollFirst()) != null; ) bufferPool.release(b);
                room.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }
}
