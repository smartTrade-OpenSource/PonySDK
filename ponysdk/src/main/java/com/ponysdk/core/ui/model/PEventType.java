/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.ui.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains the native eventbus codes.
 */
public enum PEventType {

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
    ONCONTEXTMENU(0x40000),
    FOCUSEVENTS(0x00800 | 0x01000),
    KEYEVENTS(0x00080 | 0x00100 | 0x00200),
    MOUSEEVENTS(0x00004 | 0x00008 | 0x00040 | 0x00010 | 0x00020),
    OUCHEVENTS(0x100000 | 0x200000 | 0x400000 | 0x800000),
    GESTUREEVENTS(0x1000000 | 0x2000000 | 0x4000000);

    private static final Map<Integer, PEventType> eventsByCode = new HashMap<>();

    static {
        for (final PEventType code : PEventType.values()) {
            eventsByCode.put(code.getCode(), code);
        }
    }

    private int code;

    PEventType(final int code) {
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

    public static PEventType fromInt(final int code) {
        return eventsByCode.get(code);
    }
}
