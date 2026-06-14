package com.ponysdk.sample.client.page.addon.flexlayout;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

public class FlexKeymap {
    private final Map<String, KeyBinding> bindings = new LinkedHashMap<>();

    public static FlexKeymap defaults() {
        final FlexKeymap km = new FlexKeymap();
        km.bindings.put(FlexAction.TOGGLE_LEFT.getKey(), KeyBinding.ctrlShift("b"));
        km.bindings.put(FlexAction.TOGGLE_RIGHT.getKey(), KeyBinding.ctrlShift("e"));
        km.bindings.put(FlexAction.TOGGLE_BOTTOM.getKey(), KeyBinding.ctrl("j"));
        km.bindings.put(FlexAction.CLOSE_ALL.getKey(), KeyBinding.ctrlShift("w"));
        km.bindings.put(FlexAction.UNDO.getKey(), KeyBinding.ctrl("z"));
        km.bindings.put(FlexAction.REDO.getKey(), KeyBinding.ctrl("y"));
        km.bindings.put(FlexAction.CLOSE_ACTIVE_TAB.getKey(), KeyBinding.ctrl("w"));
        km.bindings.put(FlexAction.COMMAND_PALETTE.getKey(), KeyBinding.ctrlShift("p"));
        return km;
    }

    public FlexKeymap bind(final FlexAction action, final KeyBinding key) {
        bindings.put(action.getKey(), key);
        return this;
    }

    public FlexKeymap unbind(final FlexAction action) {
        bindings.put(action.getKey(), null);
        return this;
    }

    public String toJson() {
        final JsonObjectBuilder b = Json.createObjectBuilder();
        for (final Map.Entry<String, KeyBinding> e : bindings.entrySet()) {
            if (e.getValue() == null) {
                b.addNull(e.getKey());
            } else {
                b.add(e.getKey(), Json.createReader(new java.io.StringReader(e.getValue().toJson())).readObject());
            }
        }
        return b.build().toString();
    }
}
