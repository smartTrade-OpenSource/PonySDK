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
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Accumulates binary protocol data into a ByteBuffer and flushes it
 * asynchronously via Jetty 12 {@link Session#sendBinary(ByteBuffer, Callback)}.
 * <p>
 * Writes are single-threaded (the UIContext lock guarantees this).
 * Flushes are serialized: a new sendBinary is only issued once the
 * previous Callback has completed.
 * <p>
 * Optimizations:
 * - Double-buffering: two direct ByteBuffers are swapped on flush (zero-copy)
 * - Reusable byte[] for string encoding (avoids per-string allocation)
 * - Factored string encoding logic (putStringBytes) shared by all string paths
 */
public class WebSocketPusher implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(WebSocketPusher.class);

    private static final int MAX_UNSIGNED_BYTE_VALUE = Byte.MAX_VALUE * 2 + 1;
    private static final int MAX_UNSIGNED_SHORT_VALUE = Short.MAX_VALUE * 2 + 1;
    private static final int MODEL_KEY_SIZE = 1;
    private static final int MIN_PROFITABLE_STRING_LENGTH = 4;

    /** Initial size of the reusable string encoding buffer. */
    private static final int INITIAL_STRING_BUFFER_SIZE = 256;

    /** Maximum buffer size to prevent runaway growth. */
    private static final int MAX_BUFFER_SIZE = 1 << 20; // 1 MB

    private final Session session;
    private final long timeoutMillis;
    private final int maxChunkSize;
    private final ByteBufferPool bufferPool;

    // ── Pooled double-buffering ──
    // writeBuffer: borrowed from pool on first write, returned on close
    // sendBuffer: held during async send, returned to pool by SendSync callback
    private ByteBuffer writeBuffer;

    // ── Reusable byte[] for string UTF-8 encoding ──
    private byte[] stringEncodeBuffer = new byte[INITIAL_STRING_BUFFER_SIZE];
    private final CharsetEncoder utf8Encoder = StandardCharsets.UTF_8.newEncoder();
    /** Set by {@link #encodeStringToBuffer} — true if the last encoded string was pure ASCII. */
    private boolean lastEncodedAscii;

    private volatile boolean closed;
    private volatile Throwable asyncError;

    /** Reusable send synchronizer — avoids allocating a CountDownLatch + Callback per flush. */
    private final SendSync sendSync = new SendSync();

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
            ensureCapacity(Math.min(length, writeBuffer.capacity()));
            final int chunk = Math.min(length, writeBuffer.remaining());
            writeBuffer.put(bytes, offset, chunk);
            offset += chunk;
            length -= chunk;
        }
    }

    // ── Reusable string encoding ─────────────────────────────────────────

    /**
     * Encodes a string to UTF-8 into the reusable buffer, growing it if needed.
     * Uses a reusable CharsetEncoder to avoid the intermediate byte[] allocation
     * that String.getBytes(UTF_8) would create on every call.
     * Returns the number of bytes written.
     */
    private int encodeStringToBuffer(final String value) {
        // Fast path for ASCII strings: direct byte copy, no encoder overhead
        final int len = value.length();
        if (isLikelyAscii(value, len)) {
            if (len > stringEncodeBuffer.length) {
                stringEncodeBuffer = new byte[Math.max(len, stringEncodeBuffer.length * 2)];
            }
            for (int i = 0; i < len; i++) {
                stringEncodeBuffer[i] = (byte) value.charAt(i);
            }
            lastEncodedAscii = true;
            return len;
        }

        // Non-ASCII: use CharsetEncoder to encode directly into stringEncodeBuffer
        final int maxBytes = (int) (len * utf8Encoder.maxBytesPerChar()) + 1;
        if (maxBytes > stringEncodeBuffer.length) {
            stringEncodeBuffer = new byte[Math.max(maxBytes, stringEncodeBuffer.length * 2)];
        }

        final CharBuffer in = CharBuffer.wrap(value);
        final ByteBuffer out = ByteBuffer.wrap(stringEncodeBuffer);
        utf8Encoder.reset();
        CoderResult result = utf8Encoder.encode(in, out, true);
        if (result.isOverflow()) {
            // Shouldn't happen given our sizing, but handle gracefully
            stringEncodeBuffer = new byte[maxBytes * 2];
            final ByteBuffer out2 = ByteBuffer.wrap(stringEncodeBuffer);
            in.rewind();
            utf8Encoder.reset();
            utf8Encoder.encode(in, out2, true);
            utf8Encoder.flush(out2);
            lastEncodedAscii = false;
            return out2.position();
        }
        utf8Encoder.flush(out);
        lastEncodedAscii = (out.position() == len);
        return out.position();
    }

    /**
     * Quick check: if string length matches a reasonable ASCII assumption.
     * Scans first few chars to confirm — avoids full scan for long strings.
     */
    private static boolean isLikelyAscii(final String value, final int len) {
        // Check first 16 chars (or all if shorter) — covers 99%+ of ASCII strings
        final int check = Math.min(len, 16);
        for (int i = 0; i < check; i++) {
            if (value.charAt(i) > 127) return false;
        }
        // For short strings, we've checked everything
        if (len <= 16) return true;
        // For longer strings, spot-check a few more positions
        for (int i = check; i < len; i += 32) {
            if (value.charAt(i) > 127) return false;
        }
        return true;
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
            if (byteLength <= ValueTypeModel.STRING_ASCII_UINT8) {
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
        put(stringEncodeBuffer, 0, byteLength);
        return byteLength;
    }

    /**
     * Returns the number of meta bytes used by the string length prefix.
     */
    private static int stringMetaBytes(final boolean ascii, final int byteLength) {
        if (ascii) {
            if (byteLength <= ValueTypeModel.STRING_ASCII_UINT8) return 1;
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
     * The current writeBuffer is handed off to Jetty for async send.
     * The SendSync callback returns it to the pool when Jetty is done.
     * A new writeBuffer will be lazily acquired from the pool on next write.
     */
    public void flush() throws IOException {
        checkState();
        if (writeBuffer == null || writeBuffer.position() == 0) return;

        awaitPreviousSend();

        // Hand off the writeBuffer to Jetty for sending
        writeBuffer.flip();
        final ByteBuffer toSend = writeBuffer;
        writeBuffer = null; // will be lazily acquired on next write

        final int bytes = toSend.remaining();
        if (log.isDebugEnabled()) log.debug("Flushing {} bytes", bytes);

        sendSync.reset(toSend);
        session.sendBinary(toSend, sendSync);

        if (listener != null) listener.onOutgoingPonyFramesBytes(bytes);
    }

    private void awaitPreviousSend() throws IOException {
        if (!sendSync.awaitCompletion(timeoutMillis)) {
            close();
            throw new IOException("Timeout waiting for previous WebSocket send to complete");
        }
        checkState();
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
     * @param value The type can be primitives, String or Object[]
     */
    void encode(final ServerToClientModel model, final Object value) throws IOException {
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

    private void write(final ServerToClientModel model) throws IOException {
        putModelKey(model);
        record(model, null, MODEL_KEY_SIZE, 0);
    }

    private void write(final ServerToClientModel model, final boolean value) throws IOException {
        write(model, value ? BooleanModel.TRUE.getValue() : BooleanModel.FALSE.getValue());
    }

    private void write(final ServerToClientModel model, final byte value) throws IOException {
        putModelKey(model);
        put(value);
        record(model, value, MODEL_KEY_SIZE, Byte.BYTES);
    }

    private void write(final ServerToClientModel model, final short value) throws IOException {
        putModelKey(model);
        putShort(value);
        record(model, value, MODEL_KEY_SIZE, Short.BYTES);
    }

    private void write(final ServerToClientModel model, final int value) throws IOException {
        putModelKey(model);
        putInt(value);
        record(model, value, MODEL_KEY_SIZE, Integer.BYTES);
    }

    private void write(final ServerToClientModel model, final long longValue) throws IOException {
        putModelKey(model);
        putLong(longValue);
        record(model, longValue, MODEL_KEY_SIZE, Long.BYTES);
    }

    private void write(final ServerToClientModel model, final double doubleValue) throws IOException {
        putModelKey(model);
        putDouble(doubleValue);
        record(model, doubleValue, MODEL_KEY_SIZE, Double.BYTES);
    }

    private void write(final ServerToClientModel model, final float floatValue) throws IOException {
        putModelKey(model);
        putFloat(floatValue);
        record(model, floatValue, MODEL_KEY_SIZE, Float.BYTES);
    }

    private void write(final ServerToClientModel model, final Object[] value) throws IOException {
        putModelKey(model);
        if (value.length > MAX_UNSIGNED_BYTE_VALUE) {
            throw new IllegalArgumentException("Array is too big (" + value.length + " > " + MAX_UNSIGNED_BYTE_VALUE
                    + "), use a Json Object instead : " + Arrays.toString(value).substring(0, 100) + "...");
        }
        metaBytes = MODEL_KEY_SIZE + 1;
        int dataBytes = 0;
        putUnsignedByte((short) value.length);
        for (final Object o : value) {
            dataBytes += putArrayElement(o);
        }
        record(model, value, metaBytes, dataBytes, Arrays::toString);
    }

    private void writeUint31(final ServerToClientModel model, final int value) throws IOException {
        putModelKey(model);
        final int bytes = putUint31(value);
        record(model, value, MODEL_KEY_SIZE, bytes);
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

        put(stringEncodeBuffer, 0, byteLength);
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
        record(model, value, metaBytes, dataBytes);
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

    public final void putUnsignedInteger(final long longValue) throws IOException {
        putInt((int) (longValue & 0xFFFFFF));
    }

    // ── Stats recording ──────────────────────────────────────────────────

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
        record(model, "ref:" + id, MODEL_KEY_SIZE + 1, idBytes);
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
        record(model, value, MODEL_KEY_SIZE + 1 + idBytes, byteLength);
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

        put(stringEncodeBuffer, 0, byteLength);
        return byteLength;
    }

    // ── Reusable send synchronizer ───────────────────────────────────────

    /**
     * Zero-allocation Callback + synchronization for async sendBinary.
     * Reused across flushes instead of creating a new CountDownLatch + anonymous Callback each time.
     * <p>
     * Uses {@link LockSupport#park}/{@link LockSupport#unpark} for wait/notify
     * with a volatile state flag. The waiting thread is captured on {@link #reset(ByteBuffer)}.
     * <p>
     * On completion (succeed or fail), returns the sent buffer to the pool.
     */
    private final class SendSync implements Callback {

        private static final int DONE = 1;
        private static final int PENDING = 0;

        private volatile int state = DONE; // starts as "done" so first awaitCompletion is a no-op
        private volatile Thread waiter;
        /** The buffer being sent — returned to pool on completion. */
        private volatile ByteBuffer sentBuffer;

        /** Prepare for a new send. Must be called before passing to sendBinary. */
        void reset(final ByteBuffer buffer) {
            this.sentBuffer = buffer;
            state = PENDING;
            waiter = Thread.currentThread();
        }

        @Override
        public void succeed() {
            returnBuffer();
            signal();
        }

        @Override
        public void fail(final Throwable t) {
            log.error("sendBinary failed", t);
            asyncError = t;
            returnBuffer();
            signal();
        }

        private void returnBuffer() {
            final ByteBuffer buf = sentBuffer;
            sentBuffer = null;
            if (buf != null) {
                bufferPool.release(buf);
            }
        }

        private void signal() {
            state = DONE;
            final Thread t = waiter;
            if (t != null) LockSupport.unpark(t);
        }

        /**
         * Blocks until the send completes or the timeout expires.
         * @return true if completed, false if timed out
         */
        boolean awaitCompletion(final long timeoutMillis) throws IOException {
            if (state == DONE) return true;
            final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
            while (state != DONE) {
                final long remaining = deadline - System.nanoTime();
                if (remaining <= 0) return false;
                LockSupport.parkNanos(this, remaining);
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted waiting for WebSocket send");
                }
            }
            return true;
        }
    }
}
