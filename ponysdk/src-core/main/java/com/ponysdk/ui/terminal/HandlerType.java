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

import java.io.Serializable;

public class HandlerType implements Serializable {

    public HandlerType() {

    }

    public static HandlerType SELECTION_HANDLER = new HandlerType("SELECTION_HANDLER");
    public static HandlerType STRING_VALUE_CHANGE_HANDLER = new HandlerType("STRING_VALUE_CHANGE_HANDLER");
    public static HandlerType BOOLEAN_VALUE_CHANGE_HANDLER = new HandlerType("BOOLEAN_VALUE_CHANGE_HANDLER");
    public static HandlerType COMMAND = new HandlerType("COMMAND");
    public static HandlerType BEFORE_SELECTION_HANDLER = new HandlerType("BEFORE_SELECTION_HANDLER");
    public static HandlerType DATE_VALUE_CHANGE_HANDLER = new HandlerType("DATE_VALUE_CHANGE_HANDLER");
    public static HandlerType STREAM_REQUEST_HANDLER = new HandlerType("STREAM_REQUEST_HANDLER");
    public static HandlerType EMBEDED_STREAM_REQUEST_HANDLER = new HandlerType("EMBEDED_STREAM_REQUEST_HANDLER");
    public static HandlerType CHANGE_HANDLER = new HandlerType("CHANGE_HANDLER");
    public static HandlerType TIMER = new HandlerType("TIMER");
    public static HandlerType HISTORY = new HandlerType("HISTORY");
    public static HandlerType POPUP_POSITION_CALLBACK = new HandlerType("POPUP_POSITION_CALLBACK");
    public static HandlerType CLOSE_HANDLER = new HandlerType("CLOSE_HANDLER");
    public static HandlerType DOM_HANDLER = new HandlerType("DOM_HANDLER");
    public static HandlerType SUBMIT_COMPLETE_HANDLER = new HandlerType("SUBMIT_COMPLETE_HANDLER");

    private String key;

    public HandlerType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object obj) {
        return key.equalsIgnoreCase(((HandlerType) obj).getKey());
    }
}
