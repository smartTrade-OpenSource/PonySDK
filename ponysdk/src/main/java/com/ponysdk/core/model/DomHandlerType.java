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

package com.ponysdk.core.model;

public enum DomHandlerType {

    BLUR,
    CHANGE_HANDLER,
    CLICK,
    CONTEXT_MENU,
    DOUBLE_CLICK,
    DRAG_END,
    DRAG_ENTER,
    DRAG_LEAVE,
    DRAG_OVER,
    DRAG_START,
    DROP,
    FOCUS,
    KEY_DOWN,
    KEY_PRESS,
    KEY_UP,
    MOUSE_DOWN,
    MOUSE_OUT,
    MOUSE_OVER,
    MOUSE_UP,
    MOUSE_WHELL;

    private static final DomHandlerType[] VALUES = DomHandlerType.values();

    private DomHandlerType() {
    }

    public final byte getValue() {
        return (byte) ordinal();
    }

    public static DomHandlerType fromRawValue(final int rawValue) {
        return VALUES[rawValue];
    }

}
