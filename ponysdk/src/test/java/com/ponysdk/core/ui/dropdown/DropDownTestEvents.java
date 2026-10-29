/*
 * Copyright (c) 2026 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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
package com.ponysdk.core.ui.dropdown;

import jakarta.json.Json;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.model.PKeyCodes;

/**
 * Emulates the client events the drop down widgets react to.
 */
public final class DropDownTestEvents {

    private DropDownTestEvents() {
    }

    public static void keyDown(final PObject object, final PKeyCodes key) {
        keyDown(object, key.getCode());
    }

    public static void keyDown(final PObject object, final int keyCode) {
        key(object, DomHandlerType.KEY_DOWN, keyCode);
    }

    public static void keyUp(final PObject object, final int keyCode) {
        key(object, DomHandlerType.KEY_UP, keyCode);
    }

    public static void focus(final PObject object) {
        dom(object, DomHandlerType.FOCUS);
    }

    public static void blur(final PObject object) {
        dom(object, DomHandlerType.BLUR);
    }

    public static void visibility(final PObject object, final boolean shown) {
        object.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_WIDGET_VISIBILITY.toStringValue(), shown).build());
    }

    public static void windowEvent(final PObject addon, final String windowEvent) {
        addon.onClientData(Json.createObjectBuilder()
            .add(ClientToServerModel.NATIVE.toStringValue(), Json.createObjectBuilder().add("windowEvent", windowEvent))
            .build());
    }

    private static void key(final PObject object, final DomHandlerType type, final int keyCode) {
        object.onClientData(Json.createObjectBuilder()
            .add(ClientToServerModel.DOM_HANDLER_TYPE.toStringValue(), type.getValue())
            .add(ClientToServerModel.VALUE_KEY.toStringValue(), keyCode)
            .build());
    }

    private static void dom(final PObject object, final DomHandlerType type) {
        object.onClientData(Json.createObjectBuilder().add(ClientToServerModel.DOM_HANDLER_TYPE.toStringValue(), type.getValue()).build());
    }
}
