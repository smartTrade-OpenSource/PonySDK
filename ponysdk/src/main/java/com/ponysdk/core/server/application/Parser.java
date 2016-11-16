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

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.servlet.BufferManager.Buffer;
import com.ponysdk.core.server.servlet.WebSocketServlet;

public class Parser {

    private static final Logger log = LoggerFactory.getLogger(Parser.class);

    private final WebSocketServlet.WebSocket socket;

    private Buffer buffer;

    private int lastUpdatedID = -1;

    public Parser(final WebSocketServlet.WebSocket socket) {
        this.socket = socket;
    }

    public void beginObject() {
        if (buffer == null) buffer = socket.getBuffer();
        if (buffer.position() == 0) buffer.put(ServerToClientModel.BEGIN_OBJECT.getValue());
    }

    public void endObject() {
        if (buffer == null) return;
        if (buffer.position() >= 4096) flush();
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
                parse(model);
                break;
            case BOOLEAN:
                parse(model, (boolean) value);
                break;
            case BYTE:
                parse(model, (byte) value);
                break;
            case SHORT:
                parse(model, (short) value);
                break;
            case INTEGER:
                parse(model, (int) value);
                break;
            case LONG:
                parse(model, (long) value);
                break;
            case DOUBLE:
                parse(model, (double) value);
                break;
            case STRING:
                parse(model, (String) value);
                break;
            case JSON_OBJECT:
                parse(model, (JsonObject) value);
                break;
            default:
                break;
        }
    }

    private void parse(final ServerToClientModel model) {
        if (log.isDebugEnabled()) log.debug("Writing in the buffer : " + model + " (position : " + buffer.position() + ")");
        buffer.put(model.getValue());
    }

    private void parse(final ServerToClientModel model, final boolean value) {
        if (log.isDebugEnabled())
            log.debug("Writing in the buffer : " + model + " => " + value + " (position : " + buffer.position() + ")");
        buffer.put(model.getValue());
        buffer.put(value);
    }

    private void parse(final ServerToClientModel model, final byte value) {
        if (log.isDebugEnabled())
            log.debug("Writing in the buffer : " + model + " => " + value + " (position : " + buffer.position() + ")");
        buffer.put(model.getValue());
        buffer.put(value);
    }

    private void parse(final ServerToClientModel model, final short value) {
        log.error("Writing in the buffer : " + model + " => " + value + " (position : " + buffer.position() + ")");
        buffer.put(model.getValue());
        buffer.put(value);
    }

    private void parse(final ServerToClientModel model, final int value) {
        if (log.isDebugEnabled())
            log.debug("Writing in the buffer : " + model + " => " + value + " (position : " + buffer.position() + ")");
        buffer.put(model.getValue());
        buffer.put(value);
    }

    private void parse(final ServerToClientModel model, final JsonObject jsonObject) {
        parse(model, jsonObject.toString());
    }

    private void parse(final ServerToClientModel model, final long value) {
        if (log.isDebugEnabled())
            log.debug("Writing in the buffer : " + model + " => " + value + " (position : " + buffer.position() + ")");
        buffer.put(model.getValue());
        buffer.put(value);
    }

    private void parse(final ServerToClientModel model, final double value) {
        if (log.isDebugEnabled())
            log.debug("Writing in the buffer : " + model + " => " + value + " (position : " + buffer.position() + ")");
        buffer.put(model.getValue());
        buffer.put(value);
    }

    private void parse(final ServerToClientModel model, final String value) {
        if (log.isDebugEnabled()) log.debug("Writing in the buffer : " + model + " => " + value + " (size : "
                + (value != null ? value.length() : 0) + ")" + " (position : " + buffer.position() + ")");
        buffer.put(model.getValue());
        buffer.put(value);
    }

}
