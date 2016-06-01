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

package com.ponysdk.ui.terminal.model;

import java.util.logging.Logger;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.ponysdk.ui.model.ServerToClientModel;
import com.ponysdk.ui.model.ValueTypeModel;
import com.ponysdk.ui.terminal.UIBuilder;

import elemental.client.Browser;
import elemental.html.ArrayBuffer;
import elemental.html.Uint8Array;
import elemental.html.Window;

public class ReaderBuffer {

    private static final Logger log = Logger.getLogger(UIBuilder.class.getName());

    public static final byte TRUE = 1;
    public static final byte FALSE = 0;

    private final ArrayBuffer message;
    private int position;

    public ReaderBuffer(final ArrayBuffer message) {
        this.message = message;
    }

    public int getIndex() {
        return position;
    }

    public int getByteLength() {
        return message.getByteLength();
    }

    public BinaryModel getBinaryModel() {
        if (!hasRemaining()) return BinaryModel.NULL;
        try {
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
        } catch (final Exception e) {
            // log.log(Level.SEVERE, "Cannot parse " + getString(), e);
            throw e;
        }
    }

    private boolean getBoolean() {
        final Window window = Browser.getWindow();
        final int size = ValueTypeModel.BOOLEAN.getSize();
        final Uint8Array arrayType = window.newUint8Array(message, position, size);
        position += size;
        return arrayType.intAt(0) == TRUE;
    }

    private byte getByte() {
        final Window window = Browser.getWindow();
        final int size = ValueTypeModel.BYTE.getSize();
        final Uint8Array arrayType = window.newUint8Array(message, position, size);
        position += size;
        return (byte) arrayType.intAt(0);
    }

    private short getShort() {
        final Window window = Browser.getWindow();
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
        final Window window = Browser.getWindow();
        final int size = ValueTypeModel.INTEGER.getSize();
        final Uint8Array arrayType = window.newUint8Array(message, position, size);
        position += size;

        int result = 0;
        for (int i = 0; i < arrayType.length(); i++) {
            result = (result << 8) + arrayType.intAt(i);
        }

        return result;
    }

    private long getLong() {
        throw new IllegalArgumentException("Not implemented yet");
    }

    private double getDouble() {
        throw new IllegalArgumentException("Not implemented yet");
    }

    public JSONObject getJson(final int msgSize) {
        return JSONParser.parseStrict(getString(msgSize)).isObject();
    }

    private String getString() {
        final String result = fromCharCode(message.slice(position));
        position = message.getByteLength();
        return result;
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

    private static native String fromCharCode(ArrayBuffer buf) /*-{return new TextDecoder('utf-8').decode(buf);}-*/;

    public void rewind(final BinaryModel binaryModel) {
        position -= binaryModel.getSize();
    }

    public boolean hasRemaining() {
        return position < getByteLength();
    }

}
