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

package com.ponysdk.core.terminal.model;

import java.util.logging.Logger;

import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.ValueTypeModel;

import elemental.html.ArrayBufferView;
import elemental.html.Uint8Array;

public class ReaderBuffer {

    private static final Logger log = Logger.getLogger(ReaderBuffer.class.getName());

    private static final byte TRUE = 1;

    private static final ServerToClientModel[] SERVER_TO_CLIENT_MODELS = ServerToClientModel.values();

    private final BinaryModel currentBinaryModel;

    private Uint8Array buffer;

    private int position;

    public ReaderBuffer() {
        currentBinaryModel = new BinaryModel();
    }

    public void init(final Uint8Array buffer) {
        this.buffer = buffer;
        this.position = 0;
    }

    private static native String fromCharCode(ArrayBufferView buffer) /*-{return $wnd.decode(buffer);}-*/;

    public int getIndex() {
        return position;
    }

    public BinaryModel readBinaryModel() {
        if (hasRemaining()) {
            final ServerToClientModel key = SERVER_TO_CLIENT_MODELS[getShort()];
            int size = ValueTypeModel.SHORT.getSize();

            switch (key.getTypeModel()) {
                case NULL:
                    size += key.getTypeModel().getSize();
                    currentBinaryModel.init(key, size);
                    break;
                case BOOLEAN:
                    size += key.getTypeModel().getSize();
                    currentBinaryModel.init(key, getBoolean(), size);
                    break;
                case BYTE:
                    size += key.getTypeModel().getSize();
                    currentBinaryModel.init(key, getByte(), size);
                    break;
                case SHORT:
                    size += key.getTypeModel().getSize();
                    currentBinaryModel.init(key, getShort(), size);
                    break;
                case INTEGER:
                    size += key.getTypeModel().getSize();
                    currentBinaryModel.init(key, getInt(), size);
                    break;
                case LONG:
                    // TODO Read really a long
                    // return new BinaryModel(key, getLong(), size);
                    size += ValueTypeModel.INTEGER.getSize();
                    final int messageLongSize = getInt();
                    size += messageLongSize;
                    currentBinaryModel.init(key, Long.parseLong(getString(messageLongSize)), size);
                    break;
                case DOUBLE:
                    // TODO Read really a double
                    // return new BinaryModel(key, getDouble(), size);
                    size += ValueTypeModel.INTEGER.getSize();
                    final int messageDoubleSize = getInt();
                    size += messageDoubleSize;
                    currentBinaryModel.init(key, Double.parseDouble(getString(messageDoubleSize)), size);
                    break;
                case STRING:
                    size += ValueTypeModel.INTEGER.getSize();
                    final int messageSize = getInt();
                    size += messageSize;
                    currentBinaryModel.init(key, getString(messageSize), size);
                    break;
                case JSON_OBJECT:
                    size += ValueTypeModel.INTEGER.getSize();
                    final int jsonSize = getInt();
                    size += jsonSize;
                    currentBinaryModel.init(key, getJson(jsonSize), size);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown type model : " + key.getTypeModel());
            }
        } else {
            currentBinaryModel.init(null, 0);
        }

        return currentBinaryModel;
    }

    private boolean getBoolean() {
        final int size = ValueTypeModel.BOOLEAN.getSize();
        final boolean result = buffer.intAt(position) == TRUE;
        position += size;
        return result;
    }

    private byte getByte() {
        final int size = ValueTypeModel.BYTE.getSize();
        final byte result = (byte) buffer.intAt(position);
        position += size;
        return result;
    }

    private short getShort() {
        final int size = ValueTypeModel.SHORT.getSize();

        int result = 0;
        for (int i = position; i < position + size; i++) {
            result = (result << 8) + buffer.intAt(i);
        }

        position += size;

        return (short) result;
    }

    private int getInt() {
        final int size = ValueTypeModel.INTEGER.getSize();

        int result = 0;
        for (int i = position; i < position + size; i++) {
            result = (result << 8) + buffer.intAt(i);
        }

        position += size;

        return result;
    }

    private JSONObject getJson(final int msgSize) {
        final String s = getString(msgSize);
        try {
            return s != null ? JSONParser.parseStrict(s).isObject() : null;
        } catch (final JSONException e) {
            throw new JSONException(e.getMessage() + " : " + s, e);
        }
    }

    private String getString(final int size) {
        if (size != 0) {
            final String result = fromCharCode(buffer.subarray(position, position + size));
            position += size;
            return result;
        } else {
            return null;
        }
    }

    public void rewind(final BinaryModel binaryModel) {
        position -= binaryModel.getSize();
    }

    public boolean hasRemaining() {
        return position < buffer.getByteLength();
    }

    /**
     * Go directly to the next block
     */
    public void avoidBlock() {
        while (hasRemaining()) {
            final BinaryModel binaryModel = readBinaryModel();
            if (binaryModel.isBeginKey()) {
                rewind(binaryModel);
                break;
            }
        }
    }

    public Uint8Array slice(final int startPosition, final int endPosition) {
        return buffer.subarray(startPosition, endPosition);
    }

}
