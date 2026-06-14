package com.ponysdk.sample.client.page.addon.flexlayout;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

/**
 * Represents a keyboard shortcut with modifier keys (Ctrl, Shift, Alt) and a key character.
 *
 * <pre>{@code
 * KeyBinding binding = KeyBinding.ctrlShift("p"); // Ctrl+Shift+P
 * }</pre>
 */
public class KeyBinding {
    private boolean ctrl;
    private boolean shift;
    private boolean alt;
    private final String key;

    private KeyBinding(final String key) { this.key = key; }

    /** Creates a Ctrl+key binding. */
    public static KeyBinding ctrl(final String key) {
        final KeyBinding kb = new KeyBinding(key);
        kb.ctrl = true;
        return kb;
    }

    /** Creates a Ctrl+Shift+key binding. */
    public static KeyBinding ctrlShift(final String key) {
        final KeyBinding kb = new KeyBinding(key);
        kb.ctrl = true;
        kb.shift = true;
        return kb;
    }

    /** Creates a Ctrl+Alt+key binding. */
    public static KeyBinding ctrlAlt(final String key) {
        final KeyBinding kb = new KeyBinding(key);
        kb.ctrl = true;
        kb.alt = true;
        return kb;
    }

    /** Creates a Ctrl+Shift+Alt+key binding. */
    public static KeyBinding ctrlShiftAlt(final String key) {
        final KeyBinding kb = new KeyBinding(key);
        kb.ctrl = true;
        kb.shift = true;
        kb.alt = true;
        return kb;
    }

    /** Creates an Alt+key binding. */
    public static KeyBinding alt(final String key) {
        final KeyBinding kb = new KeyBinding(key);
        kb.alt = true;
        return kb;
    }

    /** Creates a Shift+key binding. */
    public static KeyBinding shift(final String key) {
        final KeyBinding kb = new KeyBinding(key);
        kb.shift = true;
        return kb;
    }

    /** Creates a Shift+Alt+key binding. */
    public static KeyBinding shiftAlt(final String key) {
        final KeyBinding kb = new KeyBinding(key);
        kb.shift = true;
        kb.alt = true;
        return kb;
    }

    /** Creates a binding with explicit modifier flags. */
    public static KeyBinding of(final boolean ctrl, final boolean shift, final boolean alt, final String key) {
        final KeyBinding kb = new KeyBinding(key);
        kb.ctrl = ctrl;
        kb.shift = shift;
        kb.alt = alt;
        return kb;
    }

    /** Creates a binding with no modifiers (key only). */
    public static KeyBinding key(final String key) {
        return new KeyBinding(key);
    }

    /** Serializes this key binding to a JSON object string. */
    public String toJson() {
        final JsonObjectBuilder b = Json.createObjectBuilder();
        if (ctrl) b.add("ctrl", true);
        if (shift) b.add("shift", true);
        if (alt) b.add("alt", true);
        b.add("key", key);
        return b.build().toString();
    }
}
