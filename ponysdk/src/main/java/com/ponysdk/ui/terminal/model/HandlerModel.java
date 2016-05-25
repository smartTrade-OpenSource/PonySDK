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

package com.ponysdk.ui.terminal.model;

public enum HandlerModel {

    HANDLER_DOM_HANDLER,
    HANDLER_EMBEDED_STREAM_REQUEST_HANDLER,
    HANDLER_BOOLEAN_VALUE_CHANGE_HANDLER,
    HANDLER_DATE_VALUE_CHANGE_HANDLER,
    HANDLER_KEY_SHOW_RANGE,
    HANDLER_CHANGE_HANDLER,
    HANDLER_POPUP_POSITION_CALLBACK,
    HANDLER_RESIZE_HANDLER,
    HANDLER_STRING_VALUE_CHANGE_HANDLER,
    HANDLER_COMMAND,
    HANDLER_BEFORE_SELECTION_HANDLER,
    HANDLER_SELECTION_HANDLER,
    HANDLER_STRING_SELECTION_HANDLER,
    HANDLER_STREAM_REQUEST_HANDLER,
    HANDLER_SHOW_RANGE,
    HANDLER_OPEN_HANDLER;

    public byte getValue() {
        return (byte) ordinal();
    }
}
