
package com.ponysdk.ui.terminal.instruction;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;

public class PTInstruction extends JSONObject {

    public PTInstruction() {}

    public PTInstruction(final JavaScriptObject javaScriptObject) {
        super(javaScriptObject);
    }

    public Long getObjectID() {
        final JSONValue jsonValue = get(PROPERTY.OBJECT_ID);
        return (long) jsonValue.isNumber().doubleValue();
    }

    public void setObjectID(final long objectID) {
        put(PROPERTY.OBJECT_ID, objectID);
    }

    public Long getParentID() {
        if (containsKey(PROPERTY.PARENT_ID)) {
            final JSONValue jsonValue = get(PROPERTY.PARENT_ID);
            return (long) jsonValue.isNumber().doubleValue();
        } else {
            return null;
        }
    }

    public int getInt(final String key) {
        return (int) get(key).isNumber().doubleValue();
    }

    public double getDouble(final String key) {
        return get(key).isNumber().doubleValue();
    }

    public long getLong(final String key) {
        return (long) get(key).isNumber().doubleValue();
    }

    public Boolean getBoolean(final String key) {
        return get(key).isBoolean().booleanValue();
    }

    public String getString(final String key) {
        return get(key).isString().stringValue();
    }

    public void put(final String key, final String value) {
        put(key, new JSONString(value));
    }

    public void put(final String key, final int value) {
        put(key, new JSONNumber(value));
    }

    public void put(final String key, final boolean value) {
        put(key, JSONBoolean.getInstance(value));
    }

    public void put(final String key, final double value) {
        put(key, new JSONNumber(value));
    }

}
