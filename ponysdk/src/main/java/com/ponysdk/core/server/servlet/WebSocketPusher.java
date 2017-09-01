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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import javax.json.JsonObject;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.AutoFlushedBuffer;

public class WebSocketPusher extends AutoFlushedBuffer implements WriteCallback {

    private static final Logger log = LoggerFactory.getLogger(WebSocketPusher.class);

    private static final byte TRUE = 1;
    private static final byte FALSE = 0;

    private static final String ENCODING_CHARSET = "UTF-8";

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
        session.getRemote().sendBytes(bufferToFlush, this);
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
                    write(model, (String) value);
                    break;
                case JSON_OBJECT:
                    write(model, (JsonObject) value);
                    break;
                default:
                    break;
            }
        } catch (final IOException e) {
            log.error("Can't write on the websocket, so we destroy the application", e);
            UIContext.get().destroy();
        }
    }

    private void write(final ServerToClientModel model) throws IOException {
        putShort(model.getValue());
    }

    private void write(final ServerToClientModel model, final boolean value) throws IOException {
        write(model, value ? TRUE : FALSE);
    }

    private void write(final ServerToClientModel model, final byte value) throws IOException {
        putShort(model.getValue());
        put(value);
    }

    private void write(final ServerToClientModel model, final short value) throws IOException {
        putShort(model.getValue());
        putShort(value);
    }

    private void write(final ServerToClientModel model, final int value) throws IOException {
        putShort(model.getValue());
        putInt(value);
    }

    private void write(final ServerToClientModel model, final long value) throws IOException {
        write(model, String.valueOf(value));
    }

    private void write(final ServerToClientModel model, final double value) throws IOException {
        write(model, String.valueOf(value));
    }

    private void write(final ServerToClientModel model, final String value) throws IOException {
        putShort(model.getValue());

        try {
            if (value != null) {
                final byte[] bytes = value.getBytes(ENCODING_CHARSET);
                final short length = (short) bytes.length;
                if (length < Short.MAX_VALUE) {
                    putShort(length);
                    put(bytes);
                } else {
                    throw new IllegalArgumentException("Message too big, use a JsonObject instead : " + value);
                }
            } else {
                putShort((short) 0);
            }
        } catch (final UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Cannot convert message : " + value);
        }
    }

    private void write(final ServerToClientModel model, final JsonObject jsonObject) throws IOException {
        final String value = jsonObject.toString();

        putShort(model.getValue());

        try {
            if (value != null) {
                final byte[] bytes = value.getBytes(ENCODING_CHARSET);
                final int length = bytes.length;
                if (length < Integer.MAX_VALUE) {
                    putInt(bytes.length);
                    put(bytes);
                } else {
                    throw new IllegalArgumentException("Message too big, can't be sent : " + value);
                }
            } else {
                putInt(0);
            }
        } catch (final UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Cannot convert message : " + value);
        }
    }

}
