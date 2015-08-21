
package com.ponysdk.ui.terminal.instruction;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.ponysdk.ui.terminal.model.Model;

public class PTInstruction extends JSONObject {

    private final static Logger log = Logger.getLogger(PTInstruction.class.getName());

    public PTInstruction() {}

    public PTInstruction(final JavaScriptObject javaScriptObject) {
        super(javaScriptObject);
    }

    @Override
    public JSONValue get(final String key) {
        try {
            if (key == null) log.severe("null key");
            return super.get(key);
        } catch (final Exception exception) {
            log.log(Level.SEVERE, "Error ", exception);
            return null;
        }
    }

    public JSONValue get(final Model key) {
        return get(key.getKey());
    }

    public Long getObjectID() {
        final JSONValue jsonValue = get(Model.OBJECT_ID.getKey());
        return (long) jsonValue.isNumber().doubleValue();
    }

    public void setObjectID(final long objectID) {
        put(Model.OBJECT_ID.getKey(), objectID);
    }

    public Long getParentID() {
        if (containsKey(Model.PARENT_OBJECT_ID.getKey())) {
            final JSONValue jsonValue = get(Model.PARENT_OBJECT_ID.getKey());
            return (long) jsonValue.isNumber().doubleValue();
        } else {
            return null;
        }
    }

    public JSONObject getObject(final String key) {
        return get(key).isObject();
    }

    public JSONObject getObject(final Model key) {
        return get(key.getKey()).isObject();
    }

    public int getInt(final Model key) {
        return (int) get(key.getKey()).isNumber().doubleValue();
    }

    public int getInt(final String key) {
        return (int) get(key).isNumber().doubleValue();
    }

    public double getDouble(final Model key) {
        return get(key.getKey()).isNumber().doubleValue();
    }

    public double getDouble(final String key) {
        return get(key).isNumber().doubleValue();
    }

    public long getLong(final String key) {
        return (long) get(key).isNumber().doubleValue();
    }

    public long getLong(final Model key) {
        return (long) get(key.getKey()).isNumber().doubleValue();
    }

    public Boolean getBoolean(final String key) {
        return get(key).isBoolean().booleanValue();
    }

    public Boolean getBoolean(final Model key) {
        return get(key.getKey()).isBoolean().booleanValue();
    }

    public String getString(final String key) {
        return get(key).isString().stringValue();
    }

    public String getString(final Model key) {
        return get(key.getKey()).isString().stringValue();
    }

    public void put(final Model key) {
        put(key.getKey());
    }

    public JSONValue put(final Model key, final JSONValue jsonValue) {
        return put(key.getKey(), jsonValue);
    }

    public void put(final String key) {
        put(key, new JSONString(""));
    }

    public void put(final Model key, final String value) {
        put(key.getKey(), new JSONString(value));
    }

    public void put(final String key, final String value) {
        put(key, new JSONString(value));
    }

    public void put(final Model key, final int value) {
        put(key.getKey(), new JSONNumber(value));
    }

    public void put(final String key, final int value) {
        put(key, new JSONNumber(value));
    }

    public void put(final Model key, final boolean value) {
        put(key.getKey(), JSONBoolean.getInstance(value));
    }

    public void put(final String key, final boolean value) {
        put(key, JSONBoolean.getInstance(value));
    }

    public void put(final Model key, final double value) {
        put(key.getKey(), new JSONNumber(value));
    }

    public void put(final String key, final double value) {
        put(key, new JSONNumber(value));
    }

    public void put(final Model key, final JavaScriptObject value) {
        put(key.getKey(), new JSONObject(value));
    }

    public void put(final String key, final JavaScriptObject value) {
        put(key, new JSONObject(value));
    }

    public boolean containsKey(final Model key) {
        return containsKey(key.getKey());
    }

}
