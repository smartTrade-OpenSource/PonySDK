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

package com.ponysdk.core.server.servlet;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.AutoFlushedBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class WebSocketPusher extends AutoFlushedBuffer implements SendHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketPusher.class);

    private static final byte TRUE = 1;
    private static final byte FALSE = 0;

    private static final Charset STANDARD_CHARSET = Charset.forName("ISO-8859-1");
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private static final int MAX_UNSIGNED_BYTE_VALUE = Byte.MAX_VALUE * 2 + 1;
    private static final int MAX_UNSIGNED_SHORT_VALUE = Short.MAX_VALUE * 2 + 1;

    private final Session session;

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
            UIContext.get().destroy();
        }
    }

    @Override
    protected void doFlush(final ByteBuffer bufferToFlush) {
        session.getAsyncRemote().sendBinary(bufferToFlush, this);
    }

    @Override
    protected void closeFlusher() throws IOException {
        session.close();
    }

    protected void encode(final ServerToClientModel model, final Object value) {
        if (log.isDebugEnabled()) log.debug("Writing in the buffer : " + model + " => " + value);
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
                case STRING:
                    write(model, (String) value, STANDARD_CHARSET);
                    break;
                case STRING_UTF8:
                    write(model, (String) value, UTF8_CHARSET);
                    break;
                case JSON_OBJECT:
                    write(model, (JsonObject) value);
                    break;
                default:
                    log.error("Unknow model type : {}", model.getTypeModel());
                    break;
            }
        } catch (final IOException e) {
            log.error("Can't write on the websocket, so we destroy the application", e);
            final UIContext uiContext = UIContext.get();
            if (uiContext != null) uiContext.destroy();
        }
    }

    private void write(final ServerToClientModel model) throws IOException {
        putModelKey(model);
    }

    private void write(final ServerToClientModel model, final boolean value) throws IOException {
        write(model, value ? TRUE : FALSE);
    }

    private void write(final ServerToClientModel model, final byte value) throws IOException {
        putModelKey(model);
        put(value);
    }

    private void write(final ServerToClientModel model, final short value) throws IOException {
        putModelKey(model);
        putShort(value);
    }

    private void write(final ServerToClientModel model, final int value) throws IOException {
        putModelKey(model);
        putInt(value);
    }

    private void write(final ServerToClientModel model, final long longValue) throws IOException {
        final String value = String.valueOf(longValue);

        putModelKey(model);

        try {
            final byte[] bytes = value.getBytes(STANDARD_CHARSET);
            final int length = bytes.length;
            if (length <= MAX_UNSIGNED_BYTE_VALUE) {
                putUnsignedByte((short) length);
                put(bytes);
            } else {
                throw new IllegalArgumentException("Message too big (" + value.length() + " > " + MAX_UNSIGNED_BYTE_VALUE
                        + "), use a String instead : " + value.substring(0, Math.min(value.length(), 100)) + "...");
            }
        } catch (final UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Cannot convert message : " + value);
        }
    }

    private void write(final ServerToClientModel model, final double doubleValue) throws IOException {
        final String value = String.valueOf(doubleValue);

        putModelKey(model);

        try {
            final byte[] bytes = value.getBytes(STANDARD_CHARSET);
            final int length = bytes.length;
            if (length <= MAX_UNSIGNED_BYTE_VALUE) {
                putUnsignedByte((short) length);
                put(bytes);
            } else {
                throw new IllegalArgumentException("Message too big (" + value.length() + " > " + MAX_UNSIGNED_BYTE_VALUE
                        + "), use a String instead : " + value.substring(0, Math.min(value.length(), 100)) + "...");
            }
        } catch (final UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Cannot convert message : " + value);
        }
    }

    private void write(final ServerToClientModel model, final String value, final Charset encodingCharset) throws IOException {
        putModelKey(model);

        try {
            if (value != null) {
                final byte[] bytes = value.getBytes(encodingCharset);
                final int length = bytes.length;
                if (length <= MAX_UNSIGNED_SHORT_VALUE) {
                    putUnsignedShort(length);
                    put(bytes);
                } else {
                    throw new IllegalArgumentException("Message too big (" + value.length() + " > " + MAX_UNSIGNED_SHORT_VALUE
                            + "), use a JsonObject instead : " + value.substring(0, Math.min(value.length(), 100)) + "...");
                }
            } else {
                putUnsignedShort(0);
            }
        } catch (final UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Cannot convert message : " + value);
        }
    }

    private void write(final ServerToClientModel model, final JsonObject jsonObject) throws IOException {
        final String value = jsonObject.toString();

        putModelKey(model);

        try {
            if (value != null) {
                final byte[] bytes = value.getBytes(UTF8_CHARSET);
                putInt(bytes.length);
                put(bytes);
            } else {
                putInt(0);
            }
        } catch (final UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Cannot convert message : " + value);
        }
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

    @Override
    public void onResult(SendResult result) {
        if(result.isOK()){
            onFlushCompletion(); // TODO nciaravola si ca chie pas d'impact ?
        }else{
            Throwable t = result.getException();
            if (t instanceof Exception) {
                onFlushFailure((Exception) t);
            } else {
                // wrap error into a generic exception to notify producer thread and rethrow the original throwable
                onFlushFailure(new IOException(t));
                throw (RuntimeException) t;
            }
        }
    }
}
