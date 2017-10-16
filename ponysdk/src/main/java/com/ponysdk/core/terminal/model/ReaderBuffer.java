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

    public static final int NOT_FULL_BUFFER_POSITION = -1;
    private static final byte TRUE = 1;

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
            if (this.window == null) {
                this.window = Browser.getWindow();
                createSetElementsMethodOnUint8Array();
            }
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

    // WORKAROUND : No setElements on Uint8Array but Elemental need it, create a passthrough
    private static final native void createSetElementsMethodOnUint8Array() /*-{
                                                                           Uint8Array.prototype.setElements = function(array, offset) { this.set(array, offset) };
                                                                           }-*/;

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
        final ServerToClientModel key = getModelKey();
        int size = getModelKeySize();

        final ValueTypeModel typeModel = key.getTypeModel();
        if (ValueTypeModel.INTEGER == typeModel) {
            size += ValueTypeModel.INTEGER_SIZE;
            currentBinaryModel.init(key, getInt(), size);
        } else if (ValueTypeModel.STRING == typeModel) {
            size += ValueTypeModel.SHORT_SIZE;
            final int messageSize = getUnsignedShort();
            size += messageSize;
            currentBinaryModel.init(key, getString(messageSize), size);
        } else if (ValueTypeModel.JSON_OBJECT == typeModel) {
            size += ValueTypeModel.INTEGER_SIZE;
            final int jsonSize = getInt();
            size += jsonSize;
            currentBinaryModel.init(key, getJson(jsonSize), size);
        } else if (ValueTypeModel.NULL == typeModel) {
            currentBinaryModel.init(key, size);
        } else if (ValueTypeModel.BOOLEAN == typeModel) {
            size += ValueTypeModel.BOOLEAN_SIZE;
            currentBinaryModel.init(key, getBoolean(), size);
        } else if (ValueTypeModel.BYTE == typeModel) {
            size += ValueTypeModel.BYTE_SIZE;
            currentBinaryModel.init(key, getByte(), size);
        } else if (ValueTypeModel.DOUBLE == typeModel) {
            // TODO Read really a double
            // return new BinaryModel(key, getDouble(), size);
            size += ValueTypeModel.BYTE_SIZE;
            final short messageDoubleSize = getUnsignedByte();
            size += messageDoubleSize;
            currentBinaryModel.init(key, Double.parseDouble(getString(messageDoubleSize)), size);
        } else if (ValueTypeModel.LONG == typeModel) {
            // TODO Read really a long
            // return new BinaryModel(key, getLong(), size);
            size += ValueTypeModel.BYTE_SIZE;
            final short messageLongSize = getUnsignedByte();
            size += messageLongSize;
            currentBinaryModel.init(key, Long.parseLong(getString(messageLongSize)), size);
        } else if (ValueTypeModel.SHORT == typeModel) {
            size += ValueTypeModel.SHORT_SIZE;
            currentBinaryModel.init(key, getShort(), size);
        } else {
            // Never have to happen
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

    private short getUnsignedByte() {
        return (short) (getByte() & 0xFF);
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

    private int getUnsignedShort() {
        return getShort() & 0xFFFF;
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

    private long getUnsignedInt() {
        return getInt() & 0xFFFFFF;
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
        return hasEnoughRemainingBytes(getModelKeySize());
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
        int endPosition = NOT_FULL_BUFFER_POSITION;
        while (hasEnoughKeyBytes()) {
            try {
                final ServerToClientModel currentKeyModel = shiftBinaryModel();
                if (ServerToClientModel.END == currentKeyModel) {
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
        if (endPosition == NOT_FULL_BUFFER_POSITION || dryRun) position = startPosition;

        return endPosition;
    }

    private final ServerToClientModel shiftBinaryModel() {
        final ServerToClientModel key = getModelKey();

        final ValueTypeModel typeModel = key.getTypeModel();

        if (ValueTypeModel.INTEGER == typeModel) {
            position += ValueTypeModel.INTEGER_SIZE;
        } else if (ValueTypeModel.STRING == typeModel) {
            final int stringSize = getUnsignedShort();
            position += stringSize;
        } else if (ValueTypeModel.JSON_OBJECT == typeModel) {
            final int jsonSize = getInt();
            position += jsonSize;
        } else if (ValueTypeModel.NULL == typeModel) {
            // Nothing to do
        } else if (ValueTypeModel.BOOLEAN == typeModel) {
            position += ValueTypeModel.BOOLEAN_SIZE;
        } else if (ValueTypeModel.BYTE == typeModel) {
            position += ValueTypeModel.BYTE_SIZE;
        } else if (ValueTypeModel.DOUBLE == typeModel) {
            final short doubleSize = getUnsignedByte();
            position += doubleSize;
        } else if (ValueTypeModel.LONG == typeModel) {
            final short longSize = getUnsignedByte();
            position += longSize;
        } else if (ValueTypeModel.SHORT == typeModel) {
            position += ValueTypeModel.SHORT_SIZE;
        } else {
            throw new IllegalArgumentException("Unknown type model : " + typeModel);
        }

        return key;
    }

    /**
     * Get the model key
     */
    private ServerToClientModel getModelKey() {
        return ServerToClientModel.fromRawValue(getUnsignedByte());
    }

    /**
     * Get the model key size
     */
    private static final int getModelKeySize() {
        return ValueTypeModel.BYTE_SIZE;
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
