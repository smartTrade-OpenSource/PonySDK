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

package com.ponysdk.core;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.servlet.WebSocketServlet.Buffer;
import com.ponysdk.core.socket.WebSocket;
import com.ponysdk.ui.terminal.model.ServerToClientModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class ParserImpl implements Parser {

    private static final String ENCODING_CHARSET = "UTF-8";

    private static final Logger log = LoggerFactory.getLogger(ParserImpl.class);

    private final WebSocket socket;

    private Buffer buffer;

    private int lastUpdatedID = -1;

    public ParserImpl(final WebSocket socket) {
        this.socket = socket;
    }

    @Override
    public void reset() {
        if (buffer != null) {
            socket.flush(buffer);
            buffer = null;
            lastUpdatedID = -1;
        }
    }

    @Override
    public void beginObject() {
        if (buffer == null)
            buffer = socket.getBuffer();

        final ByteBuffer socketBuffer = buffer.getSocketBuffer();

        if (socketBuffer.position() == 0) {
            socketBuffer.putShort(ServerToClientModel.APPLICATION_SEQ_NUM.getValue());
            socketBuffer.putInt(UIContext.get().getAndIncrementNextSentSeqNum());
        }
    }

    @Override
    public void endObject() {
        if (buffer.getSocketBuffer().position() >= 4096) reset();
    }

    @Override
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
        } else if (ServerToClientModel.TYPE_ADD.equals(model) || ServerToClientModel.TYPE_ADD_HANDLER.equals(model) ||
                ServerToClientModel.TYPE_CLOSE.equals(model)
                || ServerToClientModel.TYPE_CREATE.equals(model) || ServerToClientModel.TYPE_GC.equals(model) ||
                ServerToClientModel.TYPE_HISTORY.equals(model)
                || ServerToClientModel.TYPE_REMOVE.equals(model) || ServerToClientModel.TYPE_REMOVE_HANDLER.equals(model)) {
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
                // TODO For now, we write String
                // parse(model, (long) value);
                parse(model, String.valueOf(value));
                break;
            case DOUBLE:
                // TODO For now, we write String
                // parse(model, (double) value);
                parse(model, String.valueOf(value));
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

    @Override
    public void parse(final ServerToClientModel model) {
        if (log.isDebugEnabled()) log.debug("Writing in the buffer : " + model);
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.putShort(model.getValue());
    }

    private void parse(final ServerToClientModel model, final boolean value) {
        parse(model, value ? ReaderBuffer.TRUE : ReaderBuffer.FALSE);
    }

    private void parse(final ServerToClientModel model, final byte value) {
        if (log.isDebugEnabled()) log.debug("Writing in the buffer : " + model + " => " + value);
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.putShort(model.getValue());
        socketBuffer.put(value);
    }

    private void parse(final ServerToClientModel model, final short value) {
        if (log.isDebugEnabled()) log.debug("Writing in the buffer : " + model + " => " + value);
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.putShort(model.getValue());
        socketBuffer.putShort(value);
    }

    private void parse(final ServerToClientModel model, final int value) {
        if (log.isDebugEnabled()) log.debug("Writing in the buffer : " + model + " => " + value);
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.putShort(model.getValue());
        socketBuffer.putInt(value);
    }

    private void parse(final ServerToClientModel model, final long value) {
        if (log.isDebugEnabled()) log.debug("Writing in the buffer : " + model + " => " + value);
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.putShort(model.getValue());
        socketBuffer.putLong(value);
    }

    private void parse(final ServerToClientModel model, final double value) {
        if (log.isDebugEnabled()) log.debug("Writing in the buffer : " + model + " => " + value);
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.putShort(model.getValue());
        socketBuffer.putDouble(value);
    }

    private void parse(final ServerToClientModel model, final JsonObject jsonObject) {
        parse(model, jsonObject.toString());
    }

    private void parse(final ServerToClientModel model, final String value) {
        if (log.isDebugEnabled())
            log.debug("Writing in the buffer : " + model + " => " + (value != null ? value.length() : 0) + " => " + value);
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.putShort(model.getValue());
        final ByteBuffer utf8StringBuffer = UTF8StringToByteBuffer(value);
        socketBuffer.putInt(utf8StringBuffer != null ? utf8StringBuffer.capacity() : 0);
        if (utf8StringBuffer != null) socketBuffer.put(utf8StringBuffer);
    }

    private ByteBuffer UTF8StringToByteBuffer(final String value) {
        try {
            return value != null ? ByteBuffer.wrap(value.getBytes(ENCODING_CHARSET)) : null;
        } catch (final UnsupportedEncodingException e) {
            log.error("Cannot convert string");
        }

        /*
         * CharsetEncoder UTF8Encoder = Charset.forName(ENCODING_CHARSET).newEncoder();
         * try {
         * return UTF8Encoder.encode(value != null ? CharBuffer.wrap(value) : CharBuffer.wrap(""));
         * } catch (final CharacterCodingException e) {
         * log.error("Cannot convert string");
         * }
         */

        return null;
    }

}
