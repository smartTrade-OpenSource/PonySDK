
package com.ponysdk.ui.server.basic;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains the native key codes.
 */
public enum PKeyCodes {

    ALT(18), BACKSPACE(8), CTRL(17), DELETE(46), DOWN(40), END(35), ENTER(13), ESCAPE(27), HOME(36), LEFT(37), PAGEDOWN(34), PAGEUP(33), RIGHT(39), SHIFT(16), TAB(9), UP(38);

    private static Map<Integer, PKeyCodes> codesByKey;
    static {
        codesByKey = new HashMap<Integer, PKeyCodes>();
        for (final PKeyCodes code : PKeyCodes.values()) {
            codesByKey.put(code.getCode(), code);
        }
    }

    private int code;

    PKeyCodes(final int code) {
        this.code = code;
    }

    public boolean equals(final int code) {
        return this.code == code;
    }

    public int getCode() {
        return this.code;
    }

    public String getCodeToString() {
        return this.code + "";
    }

    public static PKeyCodes fromInt(final int code) {
        return codesByKey.get(code);
    }
}
