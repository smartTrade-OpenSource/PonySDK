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
import elemental.html.ArrayBufferView;
import elemental.html.Uint8Array;
import elemental.html.Window;

public class ReaderBuffer {

    private static final Logger log = Logger.getLogger(ReaderBuffer.class.getName());

    private static final byte TRUE = 1;

    private static final ServerToClientModel[] SERVER_TO_CLIENT_MODELS = ServerToClientModel.values();

    private final BinaryModel currentBinaryModel;

    private Uint8Array buffer;

    private int position;

    private int size;

    private Window window;

    public ReaderBuffer() {
        this.currentBinaryModel = new BinaryModel();
    }

    public void init(final Uint8Array buffer) {
        if (this.buffer != null && position < size) {
            if (this.window == null) this.window = Browser.getWindow();
            final int remaningBufferSize = this.size - this.position;
            final Uint8Array mergedBuffer = window.newUint8Array(remaningBufferSize + buffer.getByteLength());
            mergedBuffer.setElements(this.position == 0 ? this.buffer : this.buffer.subarray(this.position), 0);
            mergedBuffer.setElements(buffer, remaningBufferSize);

            this.buffer = mergedBuffer;
        } else {
            this.buffer = buffer;
        }

        this.position = 0;
        this.size = this.buffer.getByteLength();
    }

    private static final native String fromCharCode(ArrayBufferView buffer, int position, int size) /*-{
                                                                                                    return $wnd.decode(buffer, position, size);
                                                                                                    }-*/;

    public int getPosition() {
        return position;
    }

    public void setPosition(final int position) {
        this.position = position;
    }

    public BinaryModel readBinaryModel() {
        final ServerToClientModel key = SERVER_TO_CLIENT_MODELS[getShort()];
        int size = ValueTypeModel.SHORT_SIZE;

        final ValueTypeModel typeModel = key.getTypeModel();
        switch (typeModel) {
            case NULL:
                size += typeModel.getSize();
                currentBinaryModel.init(key, size);
                break;
            case BOOLEAN:
                size += typeModel.getSize();
                currentBinaryModel.init(key, getBoolean(), size);
                break;
            case BYTE:
                size += typeModel.getSize();
                currentBinaryModel.init(key, getByte(), size);
                break;
            case SHORT:
                size += typeModel.getSize();
                currentBinaryModel.init(key, getShort(), size);
                break;
            case INTEGER:
                size += typeModel.getSize();
                currentBinaryModel.init(key, getInt(), size);
                break;
            case LONG:
                // TODO Read really a long
                // return new BinaryModel(key, getLong(), size);
                size += ValueTypeModel.INTEGER_SIZE;
                final int messageLongSize = getInt();
                size += messageLongSize;
                currentBinaryModel.init(key, Long.parseLong(getString(messageLongSize)), size);
                break;
            case DOUBLE:
                // TODO Read really a double
                // return new BinaryModel(key, getDouble(), size);
                size += ValueTypeModel.INTEGER_SIZE;
                final int messageDoubleSize = getInt();
                size += messageDoubleSize;
                currentBinaryModel.init(key, Double.parseDouble(getString(messageDoubleSize)), size);
                break;
            case STRING:
                size += ValueTypeModel.INTEGER_SIZE;
                final int messageSize = getInt();
                size += messageSize;
                currentBinaryModel.init(key, getString(messageSize), size);
                break;
            case JSON_OBJECT:
                size += ValueTypeModel.INTEGER_SIZE;
                final int jsonSize = getInt();
                size += jsonSize;
                currentBinaryModel.init(key, getJson(jsonSize), size);
                break;
            default:
                throw new IllegalArgumentException("Unknown type model : " + typeModel);
        }

        return currentBinaryModel;
    }

    private boolean getBoolean() {
        if (hasEnoughRemainingBytes(ValueTypeModel.BOOLEAN_SIZE)) return buffer.intAt(position++) == TRUE;
        else throw new ArrayIndexOutOfBoundsException();
    }

    private byte getByte() {
        if (hasEnoughRemainingBytes(ValueTypeModel.BYTE_SIZE)) return (byte) buffer.intAt(position++);
        else throw new ArrayIndexOutOfBoundsException();
    }

    private short getShort() {
        if (hasEnoughRemainingBytes(ValueTypeModel.SHORT_SIZE)) {
            int result = buffer.intAt(position++);
            result = (result << 8) + buffer.intAt(position++);
            return (short) result;
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    private int getInt() {
        if (hasEnoughRemainingBytes(ValueTypeModel.INTEGER_SIZE)) {
            int result = buffer.intAt(position++);
            result = (result << 8) + buffer.intAt(position++);
            result = (result << 8) + buffer.intAt(position++);
            result = (result << 8) + buffer.intAt(position++);
            return result;
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
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
            if (hasEnoughRemainingBytes(size)) {
                final String result = fromCharCode(buffer, position, position + size);
                position += size;
                return result;
            } else {
                throw new ArrayIndexOutOfBoundsException();
            }
        } else {
            return null;
        }
    }

    public void rewind(final BinaryModel binaryModel) {
        position -= binaryModel.getSize();
    }

    public boolean hasEnoughKeyBytes() {
        return hasEnoughRemainingBytes(ValueTypeModel.SHORT_SIZE);
    }

    public boolean hasEnoughRemainingBytes(final int blockSize) {
        return position + blockSize <= size;
    }

    /**
     * Go directly to the next block
     *
     * @param dryRun
     *            If true, not really shift
     * @return Start position of the next block
     */
    public int shiftNextBlock(final boolean dryRun) {
        final int startPosition = position;
        int endPosition = -1;
        final int ServerToClientModelEnd = ServerToClientModel.END.ordinal();
        while (hasEnoughKeyBytes()) {
            try {
                final int currentKeyModel = shiftBinaryModel();
                if (ServerToClientModelEnd == currentKeyModel) {
                    endPosition = position;
                    break;
                }
            } catch (final ArrayIndexOutOfBoundsException e) {
                // No more enough bytes
                break;
            }
        }

        // No end found, it's a split message, so we rewind
        // If it's a dry run, we rewind all the time
        if (endPosition == -1 || dryRun) position = startPosition;

        return endPosition;
    }

    private final int shiftBinaryModel() {
        final ServerToClientModel key = SERVER_TO_CLIENT_MODELS[getShort()];

        final ValueTypeModel typeModel = key.getTypeModel();
        switch (typeModel) {
            case NULL:
                break;
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INTEGER:
                position += typeModel.getSize();
                break;
            case LONG:
            case DOUBLE:
            case STRING:
            case JSON_OBJECT:
                final int jsonSize = getInt();
                position += jsonSize;
                break;
            default:
                throw new IllegalArgumentException("Unknown type model : " + typeModel);
        }

        return key.ordinal();
    }

    /**
     * Slice the array [startPosition, endPosition[
     */
    public Uint8Array slice(final int startPosition, final int endPosition) {
        position = endPosition;
        return buffer.subarray(startPosition, endPosition);
    }

    @Override
    public String toString() {
        return "Buffer " + hashCode() + " ; position = " + position + " ; size = " + size;
    }

}
