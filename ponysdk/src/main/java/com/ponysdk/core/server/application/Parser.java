/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.core.server.application;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.servlet.WebSocket;

public class Parser {

    private static final byte TRUE = 1;
    private static final byte FALSE = 0;

    private static final String ENCODING_CHARSET = "UTF-8";

    private static final Logger log = LoggerFactory.getLogger(Parser.class);

    private final WebSocket socket;

    private ByteBuffer buffer;

    private int lastUpdatedID = -1;

    public Parser(final WebSocket socket) {
        this.socket = socket;
    }

    public void beginObject() {
        if (buffer == null) buffer = socket.getBuffer();
    }

    public void endObject() {
        if (buffer == null) return;
        if (buffer.position() >= 1024) flush();
    }

    public void flush() {
        if (buffer != null) {
            socket.flush(buffer);
            buffer = null;
            lastUpdatedID = -1;
        }
    }

    public void parse(final ServerToClientModel model, final Object value) {
        if (ServerToClientModel.TYPE_UPDATE.equals(model)) {
            final int newUpdatedID = (int) value;
            if (lastUpdatedID == newUpdatedID) {
                if (log.isDebugEnabled())
                    log.debug("A consecutive update on the same id " + lastUpdatedID + ", so we concatenate the instructions");
                return;
            } else {
                lastUpdatedID = newUpdatedID;
            }
        } else if (ServerToClientModel.TYPE_ADD.equals(model) || ServerToClientModel.TYPE_ADD_HANDLER.equals(model)
                || ServerToClientModel.TYPE_CLOSE.equals(model) || ServerToClientModel.TYPE_CREATE.equals(model)
                || ServerToClientModel.TYPE_GC.equals(model) || ServerToClientModel.TYPE_HISTORY.equals(model)
                || ServerToClientModel.TYPE_REMOVE.equals(model) || ServerToClientModel.TYPE_REMOVE_HANDLER.equals(model)
                || ServerToClientModel.WINDOW_ID.equals(model)) {
            lastUpdatedID = -1;
        }

        switch (model.getTypeModel()) {
            case NULL:
                encode(buffer, model);
                break;
            case BOOLEAN:
                encode(buffer, model, (boolean) value);
                break;
            case BYTE:
                encode(buffer, model, (byte) value);
                break;
            case SHORT:
                encode(buffer, model, (short) value);
                break;
            case INTEGER:
                encode(buffer, model, (int) value);
                break;
            case LONG:
                encode(buffer, model, (long) value);
                break;
            case DOUBLE:
                encode(buffer, model, (double) value);
                break;
            case STRING:
                encode(buffer, model, (String) value);
                break;
            case JSON_OBJECT:
                encode(buffer, model, (JsonObject) value);
                break;
            default:
                break;
        }
    }

    public static final void encode(final ByteBuffer buffer, final ServerToClientModel model) {
        if (log.isDebugEnabled()) log.debug("Writing in the buffer : " + model + " (position : " + buffer.position() + ")");
        buffer.putShort(model.getValue());
    }

    public static final void encode(final ByteBuffer buffer, final ServerToClientModel model, final boolean value) {
        encode(buffer, model, value ? TRUE : FALSE);
    }

    public static final void encode(final ByteBuffer buffer, final ServerToClientModel model, final byte value) {
        if (log.isDebugEnabled())
            log.debug("Writing in the buffer : " + model + " => " + value + " (position : " + buffer.position() + ")");
        buffer.putShort(model.getValue());
        buffer.put(value);
    }

    public static final void encode(final ByteBuffer buffer, final ServerToClientModel model, final short value) {
        log.error("Writing in the buffer : " + model + " => " + value + " (position : " + buffer.position() + ")");
        buffer.putShort(model.getValue());
        buffer.putShort(value);
    }

    public static final void encode(final ByteBuffer buffer, final ServerToClientModel model, final int value) {
        if (log.isDebugEnabled())
            log.debug("Writing in the buffer : " + model + " => " + value + " (position : " + buffer.position() + ")");
        buffer.putShort(model.getValue());
        buffer.putInt(value);
    }

    public static final void encode(final ByteBuffer buffer, final ServerToClientModel model, final long value) {
        encode(buffer, model, String.valueOf(value));
    }

    public static final void encode(final ByteBuffer buffer, final ServerToClientModel model, final double value) {
        encode(buffer, model, String.valueOf(value));
    }

    public static final void encode(final ByteBuffer buffer, final ServerToClientModel model, final JsonObject jsonObject) {
        encode(buffer, model, jsonObject.toString());
    }

    public static final void encode(final ByteBuffer buffer, final ServerToClientModel model, final String value) {
        if (log.isDebugEnabled()) log.debug("Writing in the buffer : " + model + " => " + value + " (size : "
                + (value != null ? value.length() : 0) + ")" + " (position : " + buffer.position() + ")");
        buffer.putShort(model.getValue());

        try {
            if (value != null) {
                final byte[] bytes = value.getBytes(ENCODING_CHARSET);
                buffer.putInt(bytes.length);
                buffer.put(bytes);
            } else {
                buffer.putInt(0);
            }
        } catch (final UnsupportedEncodingException e) {
            log.error("Cannot convert string");
        }
    }

}
