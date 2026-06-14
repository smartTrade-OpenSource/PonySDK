package com.ponysdk.sample.client.page.addon.flexlayout;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

/**
 * Keyboard shortcut map for FlexLayout actions, supporting bind and unbind.
 *
 * <pre>{@code
 * FlexKeymap keymap = FlexKeymap.defaults()
 *     .bind(FlexAction.UNDO, KeyBinding.ctrl("z"))
 *     .unbind(FlexAction.REDO);
 * }</pre>
 */
public class FlexKeymap {
    private final Map<String, KeyBinding> bindings = new LinkedHashMap<>();

    /** Creates a keymap pre-configured with default bindings for all standard actions. */
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

    /** Binds a keyboard shortcut to the given action, replacing any previous binding. */
    public FlexKeymap bind(final FlexAction action, final KeyBinding key) {
        bindings.put(action.getKey(), key);
        return this;
    }

    /** Removes the keyboard shortcut for the given action (serializes as null). */
    public FlexKeymap unbind(final FlexAction action) {
        bindings.put(action.getKey(), null);
        return this;
    }

    /** Serializes this keymap to a JSON object string. */
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
