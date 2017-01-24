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

import com.google.gwt.json.client.JSONObject;
import com.ponysdk.core.model.ServerToClientModel;

public class BinaryModel {

    private ServerToClientModel model;
    private int size;

    private boolean booleanValue;
    private byte byteValue;
    private short shortValue;
    private int intValue;
    private long longValue;
    private double doubleValue;
    private String stringValue;
    private JSONObject jsonObject;

    protected BinaryModel() {
    }

    public void init(final ServerToClientModel key, final boolean value, final int size) {
        init(key, size);
        this.booleanValue = value;
    }

    public void init(final ServerToClientModel key, final byte value, final int size) {
        init(key, size);
        this.byteValue = value;
    }

    public void init(final ServerToClientModel key, final short value, final int size) {
        init(key, size);
        this.shortValue = value;
    }

    public void init(final ServerToClientModel key, final int value, final int size) {
        init(key, size);
        this.intValue = value;
    }

    public void init(final ServerToClientModel key, final long value, final int size) {
        init(key, size);
        this.longValue = value;
    }

    public void init(final ServerToClientModel key, final double value, final int size) {
        init(key, size);
        this.doubleValue = value;
    }

    public void init(final ServerToClientModel key, final String value, final int size) {
        init(key, size);
        this.stringValue = value;
    }

    public void init(final ServerToClientModel key, final JSONObject value, final int size) {
        init(key, size);
        this.jsonObject = value;
    }

    public void init(final ServerToClientModel key, final int value) {
        this.model = key;
        this.size = value;
    }

    public ServerToClientModel getModel() {
        return model;
    }

    public boolean getBooleanValue() {
        return booleanValue;
    }

    public byte getByteValue() {
        return byteValue;
    }

    public short getShortValue() {
        return shortValue;
    }

    public int getIntValue() {
        return intValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        if (model == null) return null;
        switch (model.getTypeModel()) {
            case NULL:
                return String.valueOf(model);
            case BOOLEAN:
                return model + " => " + booleanValue;
            case BYTE:
                return model + " => " + byteValue;
            case SHORT:
                return model + " => " + shortValue;
            case INTEGER:
                return model + " => " + intValue;
            case LONG:
                return model + " => " + longValue;
            case DOUBLE:
                return model + " => " + doubleValue;
            case STRING:
                return model + " => " + stringValue;
            case JSON_OBJECT:
                return model + " => " + jsonObject;
            default:
                throw new IllegalArgumentException("No model type configured");
        }
    }

    public boolean isBeginKey() {
        return ServerToClientModel.WINDOW_ID.equals(model) || ServerToClientModel.TYPE_ADD.equals(model)
                || ServerToClientModel.TYPE_ADD_HANDLER.equals(model) || ServerToClientModel.TYPE_CLOSE.equals(model)
                || ServerToClientModel.TYPE_CREATE.equals(model) || ServerToClientModel.TYPE_GC.equals(model)
                || ServerToClientModel.TYPE_HISTORY.equals(model) || ServerToClientModel.TYPE_REMOVE.equals(model)
                || ServerToClientModel.TYPE_REMOVE_HANDLER.equals(model) || ServerToClientModel.TYPE_UPDATE.equals(model);
    }

}
