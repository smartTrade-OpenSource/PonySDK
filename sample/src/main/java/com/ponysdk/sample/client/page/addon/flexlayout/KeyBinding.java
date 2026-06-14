package com.ponysdk.sample.client.page.addon.flexlayout;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

public class KeyBinding {
    private boolean ctrl;
    private boolean shift;
    private boolean alt;
    private final String key;

    private KeyBinding(final String key) { this.key = key; }

    public static KeyBinding ctrl(final String key) {
        final KeyBinding kb = new KeyBinding(key);
        kb.ctrl = true;
        return kb;
    }

    public static KeyBinding ctrlShift(final String key) {
        final KeyBinding kb = new KeyBinding(key);
        kb.ctrl = true;
        kb.shift = true;
        return kb;
    }

    public static KeyBinding key(final String key) {
        return new KeyBinding(key);
    }

    public String toJson() {
        final JsonObjectBuilder b = Json.createObjectBuilder();
        if (ctrl) b.add("ctrl", true);
        if (shift) b.add("shift", true);
        if (alt) b.add("alt", true);
        b.add("key", key);
        return b.build().toString();
    }
}
