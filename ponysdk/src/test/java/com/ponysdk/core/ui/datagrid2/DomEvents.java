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
package com.ponysdk.core.ui.datagrid2;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PWidget;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * Builds the JSON instructions a terminal would send for DOM events, so that tests go through the same
 * {@code onClientData} path as the browser.
 */
public final class DomEvents {

    private DomEvents() {
    }

    public static void domEvent(final PWidget widget, final DomHandlerType type) {
        widget.onClientData(Json.createObjectBuilder().add(ClientToServerModel.DOM_HANDLER_TYPE.toStringValue(), type.getValue()).build());
    }

    public static void keyUp(final PWidget widget, final int keyCode) {
        widget.onClientData(Json.createObjectBuilder()
            .add(ClientToServerModel.DOM_HANDLER_TYPE.toStringValue(), DomHandlerType.KEY_UP.getValue())
            .add(ClientToServerModel.VALUE_KEY.toStringValue(), keyCode)
            .build());
    }

    public static void click(final PWidget widget, final boolean shift) {
        widget.onClientData(Json.createObjectBuilder()
            .add(ClientToServerModel.DOM_HANDLER_TYPE.toStringValue(), DomHandlerType.CLICK.getValue())
            .add(ClientToServerModel.EVENT_INFO.toStringValue(),
                Json.createArrayBuilder().add(0).add(0).add(0).add(0).add(1).add(false).add(false).add(shift).add(false))
            .build());
    }

    public static void stringValueChange(final PWidget widget, final String value) {
        widget.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue(), value).build());
    }

    public static void booleanValueChange(final PWidget widget, final boolean value) {
        widget.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_BOOLEAN_VALUE_CHANGE.toStringValue(), value).build());
    }

    public static void terminalData(final PObject object, final JsonObjectBuilder data) {
        object.onClientData(Json.createObjectBuilder().add(ClientToServerModel.NATIVE.toStringValue(), data).build());
    }
}
