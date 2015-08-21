
package com.ponysdk.ui.server.basic;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains the native event codes.
 */
public enum PEvent {

    ONBLUR(0x01000),
    ONCHANGE(0x00400),
    ONCLICK(0x00001),
    ONDBLCLICK(0x00002),
    ONERROR(0x10000),
    ONFOCUS(0x00800),
    ONGESTURECHANGE(0x2000000),
    ONGESTUREEND(0x4000000),
    ONGESTURESTART(0x1000000),
    ONKEYDOWN(0x00080),
    ONKEYPRESS(0x00100),
    ONKEYUP(0x00200),
    ONLOAD(0x08000),
    ONLOSECAPTURE(0x02000),
    ONMOUSEDOWN(0x00004),
    ONMOUSEMOVE(0x00040),
    ONMOUSEOUT(0x00020),
    ONMOUSEOVER(0x00010),
    ONMOUSEUP(0x00008),
    ONMOUSEWHEEL(0x20000),
    ONPASTE(0x80000),
    ONSCROLL(0x04000),
    ONTOUCHCANCEL(0x800000),
    ONTOUCHEND(0x400000),
    ONTOUCHMOVE(0x200000),
    ONTOUCHSTART(0x100000),
    ONCONTEXTMENU(0x40000);

    private static Map<Integer, PEvent> eventsByCode;

    static {
        eventsByCode = new HashMap<>();
        for (final PEvent code : PEvent.values()) {
            eventsByCode.put(code.getCode(), code);
        }
    }

    private int code;

    PEvent(final int code) {
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

    public static PEvent fromInt(final int code) {
        return eventsByCode.get(code);
    }
}
