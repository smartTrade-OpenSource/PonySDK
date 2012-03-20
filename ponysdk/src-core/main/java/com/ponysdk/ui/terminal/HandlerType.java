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

package com.ponysdk.ui.terminal;

import java.util.HashMap;
import java.util.Map;

public enum HandlerType {

    SELECTION_HANDLER,
    STRING_SELECTION_HANDLER,
    STRING_VALUE_CHANGE_HANDLER,
    BOOLEAN_VALUE_CHANGE_HANDLER,
    COMMAND,
    BEFORE_SELECTION_HANDLER,
    DATE_VALUE_CHANGE_HANDLER,
    STREAM_REQUEST_HANDLER,
    EMBEDED_STREAM_REQUEST_HANDLER,
    CHANGE_HANDLER,
    TIMER,
    SCHEDULER,
    HISTORY,
    POPUP_POSITION_CALLBACK,
    CLOSE_HANDLER,
    DOM_HANDLER,
    SUBMIT_COMPLETE_HANDLER;

    private static Map<String, HandlerType> typeByCode = new HashMap<String, HandlerType>();

    static {
        for (final HandlerType handlerType : HandlerType.values()) {
            typeByCode.put(handlerType.getCode(), handlerType);
        }
    }

    public String getCode() {
        return String.valueOf(ordinal());
    }

    public static HandlerType from(final String code) {
        return typeByCode.get(code);
    }
}
