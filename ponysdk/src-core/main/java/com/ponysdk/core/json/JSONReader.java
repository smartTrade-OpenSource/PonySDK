
package com.ponysdk.core.json;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONReader {

    private static Logger log = LoggerFactory.getLogger(JSONReader.class);
    private final JSONObject root;

    public JSONReader(final JSONObject root) {
        this.root = root;
    }

    private JSONObject getNode(final String[] properties) {
        try {
            if (properties.length < 2) return root;

            JSONObject node = root;
            for (int i = 0; i < properties.length - 1; i++) {
                node = node.getJSONObject(properties[i]);
                if (node == null) return null;
            }
            return node;
        } catch (final JSONException e) {
            log.error("Failed to parse #" + properties, e);
            return null;
        }
    }

    public JSONObject get(final String name) {
        try {
            final String[] properties = name.split("\\.");
            final JSONObject object = getNode(properties);
            if (object == null) return null;
            return object.getJSONObject(properties[properties.length - 1]);
        } catch (final JSONException e) {
            log.error("Failed to parse #" + name, e);
            return null;
        }
    }

    public String getString(final String name) {
        try {
            final String[] properties = name.split("\\.");
            final JSONObject object = getNode(properties);
            if (object == null) return null;
            return object.getString(properties[properties.length - 1]);
        } catch (final JSONException e) {
            log.error("Failed to parse #" + name, e);
            return null;
        }
    }

    public Integer getInteger(final String name) {
        try {
            final String[] properties = name.split("\\.");
            final JSONObject object = getNode(properties);
            if (object == null) return null;
            if (!object.has(properties[properties.length - 1])) return null;

            return object.getInt(properties[properties.length - 1]);
        } catch (final JSONException e) {
            log.error("Failed to parse #" + name, e);
            return null;
        }
    }

    public JSONObject getJSONObject() {
        return root;
    }

    @Override
    public String toString() {
        return root.toString();
    }
}
