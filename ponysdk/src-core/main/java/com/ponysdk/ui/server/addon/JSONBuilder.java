
package com.ponysdk.ui.server.addon;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONBuilder {

    private static Logger log = LoggerFactory.getLogger(JSONBuilder.class);

    private final JSONObject jsonObject;
    private Map<String, JSONBuilder> childs;

    public JSONBuilder() {
        jsonObject = new JSONObject();
    }

    public JSONBuilder put(final String key, final Object o) {
        try {
            jsonObject.put(key, o);
        } catch (final JSONException e) {
            log.error("Failed to encode json", e);
        }
        return this;
    }

    public JSONBuilder addChild(final String key) {
        final JSONBuilder child = new JSONBuilder();

        if (childs == null) childs = new HashMap<String, JSONBuilder>();
        childs.put(key, child);

        return child;
    }

    public JSONObject build() {
        if (childs != null) {
            try {
                for (final Entry<String, JSONBuilder> e : childs.entrySet()) {
                    final JSONObject jso = e.getValue().build();
                    jsonObject.put(e.getKey(), jso);
                }
            } catch (final JSONException e) {
                log.error("Failed to encode json", e);
            }
        }
        return jsonObject;
    }
}
