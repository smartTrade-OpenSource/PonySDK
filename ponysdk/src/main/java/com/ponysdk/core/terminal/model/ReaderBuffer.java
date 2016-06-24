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

import elemental.client.Browser;
import elemental.html.ArrayBuffer;
import elemental.html.Uint8Array;
import elemental.html.Window;

public class ReaderBuffer {

    private static final Logger log = Logger.getLogger(ReaderBuffer.class.getName());

    private static final byte TRUE = 1;

    private final ArrayBuffer message;
    private int position;

    private final Window window;

    public ReaderBuffer(final ArrayBuffer message) {
        this.message = message;
        this.window = Browser.getWindow();
    }

    public int getIndex() {
        return position;
    }

    private int getByteLength() {
        return message.getByteLength();
    }

    public BinaryModel readBinaryModel() {
        if (!hasRemaining()) return BinaryModel.NULL;
        final ServerToClientModel key = ServerToClientModel.values()[getShort()];
        int size = ValueTypeModel.SHORT.getSize();

        switch (key.getTypeModel()) {
            case NULL:
                size += key.getTypeModel().getSize();
                return new BinaryModel(key, size);
            case BOOLEAN:
                size += key.getTypeModel().getSize();
                return new BinaryModel(key, getBoolean(), size);
            case BYTE:
                size += key.getTypeModel().getSize();
                return new BinaryModel(key, getByte(), size);
            case SHORT:
                size += key.getTypeModel().getSize();
                return new BinaryModel(key, getShort(), size);
            case INTEGER:
                size += key.getTypeModel().getSize();
                return new BinaryModel(key, getInt(), size);
            case LONG:
                // TODO Read really a long
                // return new BinaryModel(key, getLong(), size);
            case DOUBLE:
                // TODO Read really a double
                // return new BinaryModel(key, getDouble(), size);
            case STRING:
                size += ValueTypeModel.INTEGER.getSize();
                final int messageSize = getInt();
                size += messageSize;
                return new BinaryModel(key, getString(messageSize), size);
            case JSON_OBJECT:
                size += ValueTypeModel.INTEGER.getSize();
                final int jsonSize = getInt();
                size += jsonSize;
                return new BinaryModel(key, getJson(jsonSize), size);
            default:
                throw new IllegalArgumentException("Unknown type model : " + key.getTypeModel());
        }
    }

    private boolean getBoolean() {
        final int size = ValueTypeModel.BOOLEAN.getSize();
        final Uint8Array arrayType = window.newUint8Array(message, position, size);
        position += size;
        return arrayType.intAt(0) == TRUE;
    }

    private byte getByte() {
        final int size = ValueTypeModel.BYTE.getSize();
        final Uint8Array arrayType = window.newUint8Array(message, position, size);
        position += size;
        return (byte) arrayType.intAt(0);
    }

    private short getShort() {
        final int size = ValueTypeModel.SHORT.getSize();
        final Uint8Array arrayType = window.newUint8Array(message, position, size);

        position += size;

        int result = 0;
        for (int i = 0; i < arrayType.length(); i++) {
            result = (result << 8) + arrayType.intAt(i);
        }

        return (short) result;
    }

    private int getInt() {
        final int size = ValueTypeModel.INTEGER.getSize();
        final Uint8Array arrayType = window.newUint8Array(message, position, size);
        position += size;

        int result = 0;
        for (int i = 0; i < arrayType.length(); i++) {
            result = (result << 8) + arrayType.intAt(i);
        }

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

    private String getString(final int end) {
        if (end != 0) {
            final String result = fromCharCode(message.slice(position, position + end));
            position += end;
            return result;
        } else {
            return null;
        }
    }

    private static native String fromCharCode(ArrayBuffer buf) /*-{return $wnd.decode(buf);}-*/;

    public void rewind(final BinaryModel binaryModel) {
        position -= binaryModel.getSize();
    }

    public boolean hasRemaining() {
        return position < getByteLength();
    }

    @Override
    public String toString() {
        return "ReaderBuffer {" +
                "window=" + window.getName() +
                ", position=" + position +
                "}";
    }
}
