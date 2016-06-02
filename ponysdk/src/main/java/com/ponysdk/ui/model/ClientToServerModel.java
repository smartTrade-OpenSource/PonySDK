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

package com.ponysdk.ui.model;

public enum ClientToServerModel {

    HEARTBEAT,

    OBJECT_ID,

    NATIVE,
    APPLICATION_INSTRUCTIONS,
    APPLICATION_VIEW_ID,

    TYPE_CLOSE,
    TYPE_HISTORY,

    APPLICATION_ERRORS,
    APPLICATION_START,
    APPLICATION_SEQ_NUM,
    WIDGET_POSITION,
    EVENT_INFO,
    DOM_HANDLER_TYPE,

    ERROR_MSG,
    VALUE,
    YEAR,
    MONTH,
    DAY,

    PARENT_OBJECT_ID,
    VALUE_KEY,
    REPLACEMENT_STRING,
    COMMAND_ID,
    STREAM_REQUEST_ID,
    SIZE,
    DRAG_SRC,
    START_DATE,
    END_DATE,
    KEY_FILTER,
    RESULT,

    HANDLER_BOOLEAN_VALUE_CHANGE,
    HANDLER_DATE_VALUE_CHANGE,
    HANDLER_STRING_VALUE_CHANGE,
    HANDLER_BEFORE_SELECTION,
    HANDLER_CHANGE,
    HANDLER_COMMAND,
    HANDLER_CLOSE,
    HANDLER_OPEN,
    HANDLER_RESIZE,
    HANDLER_SCHEDULER,
    HANDLER_SELECTION,
    HANDLER_SHOW_RANGE,
    HANDLER_STRING_SELECTION,
    HANDLER_SUBMIT_COMPLETE,
    HANDLER_KEY,
    HANDLER_KEY_COMMAND,
    HANDLER_KEY_RESIZE,
    HANDLER_KEY_SCHEDULER;

    private ClientToServerModel() {
    }

    public String toStringValue() {
        return String.valueOf(ordinal());
    }

}