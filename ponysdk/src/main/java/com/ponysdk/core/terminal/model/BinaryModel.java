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

import com.google.gwt.json.client.JSONArray;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.ValueTypeModel;

import elemental.json.Json;
import elemental.json.JsonObject;

public class BinaryModel {

    private ServerToClientModel model;
    private int size;

    private boolean booleanValue;
    private int intValue;
    private long longValue;
    private float floatValue;
    private double doubleValue;
    private String stringValue;
    private JSONArray arrayValue;

    protected BinaryModel() {
    }

    public void init(final ServerToClientModel key, final boolean value, final int size) {
        init(key, size);
        this.booleanValue = value;
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

    public void init(final ServerToClientModel key, final float value, final int size) {
        init(key, size);
        this.floatValue = value;
    }

    public void init(final ServerToClientModel key, final String value, final int size) {
        init(key, size);
        this.stringValue = value;
    }

    public void init(final ServerToClientModel key, final JSONArray value, final int size) {
        init(key, size);
        this.arrayValue = value;
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

    public int getIntValue() {
        return intValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public float getFloatValue() {
        return floatValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public JsonObject getJsonObject() {
        return Json.parse(stringValue);
    }

    public JSONArray getArrayValue() {
        return arrayValue;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        if (model == null) return "";

        final ValueTypeModel typeModel = model.getTypeModel();
        if (ValueTypeModel.NULL == typeModel) return String.valueOf(model);
        else if (ValueTypeModel.BOOLEAN == typeModel) return model + " => " + booleanValue;
        else if (ValueTypeModel.BYTE == typeModel) return model + " => " + intValue;
        else if (ValueTypeModel.SHORT == typeModel) return model + " => " + intValue;
        else if (ValueTypeModel.INTEGER == typeModel) return model + " => " + intValue;
        else if (ValueTypeModel.LONG == typeModel) return model + " => " + longValue;
        else if (ValueTypeModel.DOUBLE == typeModel) return model + " => " + doubleValue;
        else if (ValueTypeModel.STRING == typeModel) return model + " => " + stringValue;
        else if (ValueTypeModel.FLOAT == typeModel) return model + " => " + floatValue;
        else if (ValueTypeModel.ARRAY == typeModel) return model + " => " + arrayValue;
        else throw new IllegalArgumentException("No model type configured : " + typeModel);
    }

}
