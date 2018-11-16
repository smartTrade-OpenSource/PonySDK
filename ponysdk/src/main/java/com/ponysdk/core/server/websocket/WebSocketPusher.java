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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ArrayValueModel;
import com.ponysdk.core.model.BooleanModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.ValueTypeModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.AutoFlushedBuffer;

public class WebSocketPusher extends AutoFlushedBuffer implements WriteCallback {

    private static final Logger log = LoggerFactory.getLogger(WebSocketPusher.class);

    private static final int MAX_UNSIGNED_BYTE_VALUE = Byte.MAX_VALUE * 2 + 1;
    private static final int MAX_UNSIGNED_SHORT_VALUE = Short.MAX_VALUE * 2 + 1;
    private static final int MODEL_KEY_SIZE = 1;

    private final Session session;

    private static volatile WebSocketStatsRecorder statsRecorder;
    private int metaBytes;

    private WebSocket.Listener listener;

    public WebSocketPusher(final Session session, final int bufferSize, final int maxChunkSize, final long timeoutMillis) {
        super(bufferSize, true, maxChunkSize, 0.25f, timeoutMillis);
        this.session = session;
    }

    @Override
    public void flush() {
        try {
            super.flush();
        } catch (final IOException e) {
            log.error("Can't write on the websocket, so we destroy the application", e);
            UIContext.get().onDestroy();
        }
    }

    @Override
    protected void doFlush(final ByteBuffer bufferToFlush) {
        final int bytes = bufferToFlush.remaining();
        session.getRemote().sendBytes(bufferToFlush, this);
        if (listener != null) listener.onOutgoingBytes(bytes);
    }

    @Override
    protected void closeFlusher() {
        session.close();
    }

    @Override
    public void writeFailed(final Throwable t) {
        if (t instanceof Exception) {
            onFlushFailure((Exception) t);
        } else {
            // wrap error into a generic exception to notify producer thread and rethrow the original throwable
            onFlushFailure(new IOException(t));
            throw (RuntimeException) t;
        }
    }

    @Override
    public void writeSuccess() {
        onFlushCompletion();
    }

    /**
     * @param value The type can be primitives, String or Object[]
     */
    protected void encode(final ServerToClientModel model, final Object value) {
        if (log.isDebugEnabled()) log.debug("Writing in the buffer : {} => {}", model, value);
        try {
            switch (model.getTypeModel()) {
                case NULL:
                    write(model);
                    break;
                case BOOLEAN:
                    write(model, (boolean) value);
                    break;
                case BYTE:
                    write(model, (byte) value);
                    break;
                case SHORT:
                    write(model, (short) value);
                    break;
                case INTEGER:
                    write(model, (int) value);
                    break;
                case LONG:
                    write(model, (long) value);
                    break;
                case DOUBLE:
                    write(model, (double) value);
                    break;
                case FLOAT:
                    write(model, (float) value);
                    break;
                case STRING:
                    write(model, (String) value);
                    break;
                case ARRAY:
                    write(model, (Object[]) value);
                    break;
                default:
                    log.error("Unknow model type : {}", model.getTypeModel());
                    break;
            }
        } catch (final IOException e) {
            log.error("Can't write on the websocket, so we destroy the application", e);
            UIContext.get().onDestroy();
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
        if (f == value) { //value can fit in a float without losing precision
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
        } else if (o instanceof Integer) {
            return putCompressedInt((int) o);
        } else if (o instanceof String) {
            return putArrayStringElement(o);
        } else if (o instanceof Byte) {
            put(ArrayValueModel.BYTE.getValue());
            put((byte) o);
            return Byte.BYTES;
        } else if (o instanceof Short) {
            return putCompressedShort((short) o);
        } else if (o instanceof Boolean) {
            put(o.equals(Boolean.TRUE) ? ArrayValueModel.BOOLEAN_TRUE.getValue() : ArrayValueModel.BOOLEAN_FALSE.getValue());
            return 0;
        } else if (o instanceof Long) {
            return putCompressedLong((long) o);
        } else if (o instanceof Double) {
            return putCompressedDouble((double) o);
        } else if (o instanceof Float) {
            put(ArrayValueModel.FLOAT.getValue());
            putFloat((float) o);
            return Float.BYTES;
        } else {
            return putArrayStringElement(o.toString());
        }
    }

    private int putArrayStringElement(final Object o) throws IOException {
        final String s = (String) o;
        final byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        final int length = bytes.length;

        if (length <= MAX_UNSIGNED_BYTE_VALUE) {
            put(length == s.length() ? ArrayValueModel.STRING_ASCII_UINT8_LENGTH.getValue()
                    : ArrayValueModel.STRING_UTF8_UINT8_LENGTH.getValue());
            putUnsignedByte((short) length);
            metaBytes++;
        } else if (length <= MAX_UNSIGNED_SHORT_VALUE) {
            put(length == s.length() ? ArrayValueModel.STRING_ASCII_UINT16_LENGTH.getValue()
                    : ArrayValueModel.STRING_UTF8_UINT16_LENGTH.getValue());
            putUnsignedShort(length);
            metaBytes += 2;
        } else {
            put(ArrayValueModel.STRING_UTF8_INT32_LENGTH.getValue());
            putInt(length);
            metaBytes += 4;
        }

        put(bytes);

        return bytes.length;
    }

    private void write(final ServerToClientModel model, final String value) throws IOException {
        putModelKey(model);

        try {
            putString(model, value);
        } catch (final UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Cannot convert message : " + value);
        }
    }

    private void putString(final ServerToClientModel model, final String value) throws IOException {
        int metaBytes = MODEL_KEY_SIZE;
        int dataBytes;
        if (value != null) {
            final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            final int length = bytes.length;
            if (value.length() == length) { //ASCII
                if (length <= ValueTypeModel.STRING_ASCII_UINT8_MAX_LENGTH) { // 0 -> 250 (The MOST common case)
                    putUnsignedByte((short) length);
                    metaBytes += 1;
                } else if (length <= MAX_UNSIGNED_SHORT_VALUE) { // 251 -> 65,535
                    putUnsignedByte(ValueTypeModel.STRING_ASCII_UINT16);
                    putUnsignedShort(length);
                    metaBytes += 3;
                } else { // 65,536 -> 2,147,483,647
                    putUnsignedByte(ValueTypeModel.STRING_ASCII_INT32);
                    putInt(length);
                    metaBytes += 5;
                }

            } else { //UTF8
                if (length <= MAX_UNSIGNED_BYTE_VALUE) { // 0 -> 255
                    putUnsignedByte(ValueTypeModel.STRING_UTF8_UINT8);
                    putUnsignedByte((short) length);
                    metaBytes += 2;
                } else if (length <= MAX_UNSIGNED_SHORT_VALUE) { // 256 -> 65,535
                    putUnsignedByte(ValueTypeModel.STRING_UTF8_UINT16);
                    putUnsignedShort(length);
                    metaBytes += 3;
                } else { // 65,536 -> 2,147,483,647
                    putUnsignedByte(ValueTypeModel.STRING_UTF8_INT32);
                    putInt(length);
                    metaBytes += 5;
                }
            }
            put(bytes);
            dataBytes = length;
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
        return recorder != null ? recorder.isStarted() : false;
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
}
