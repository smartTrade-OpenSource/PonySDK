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

public enum ClientToServerModel {

    OBJECT_ID("0"),

    UI_CONTEXT_ID("1"),
    WINDOW_ID("2"),
    FRAME_ID("3"),

    NATIVE("4"),
    APPLICATION_INSTRUCTIONS("5"),

    ERROR_MSG("6"),
    WARN_MSG("7"),
    INFO_MSG("8"),

    COOKIES("9"),

    TYPE_HISTORY("b"),

    WIDGET_POSITION("c"),
    POPUP_POSITION("d"),
    EVENT_INFO("e"),
    DOM_HANDLER_TYPE("f"),

    TERMINAL_LATENCY("g"),
    APPLICATION_ID("h"),

    PARENT_OBJECT_ID("i"),
    VALUE_KEY("j"),
    REPLACEMENT_STRING("k"),
    COMMAND_ID("l"),
    STREAM_REQUEST_ID("m"),
    SIZE("n"),
    DRAG_SRC("o"),
    START_DATE("p"),
    END_DATE("q"),
    KEY_FILTER("r"),
    RESULT("s"),

    OPTION_TABINDEX_ACTIVATED("t"),

    HEARTBEAT_REQUEST("u"),

    HANDLER_BOOLEAN_VALUE_CHANGE("A"),
    HANDLER_DATE_VALUE_CHANGE("B"),
    HANDLER_STRING_VALUE_CHANGE("C"),
    HANDLER_BEFORE_SELECTION("D"),
    HANDLER_COMMAND("E"),
    HANDLER_CLOSE("F"),
    HANDLER_OPEN("G"),
    HANDLER_RESIZE("H"),
    HANDLER_SELECTION("I"),
    HANDLER_SHOW_RANGE("J"),
    HANDLER_STRING_SELECTION("K"),
    HANDLER_SUBMIT_COMPLETE("L"),
    HANDLER_SCROLL("M"),
    HANDLER_SCROLL_HEIGHT("N"),
    HANDLER_SCROLL_WIDTH("O"),
    HANDLER_SCROLL_VERTICAL("P"),
    HANDLER_SCROLL_HORIZONTAL("Q"),
    HANDLER_CHANGE("R"),
    HANDLER_PASTE("S"),
    HANDLER_WIDGET_VISIBILITY("T"),
    HANDLER_DOCUMENT_VISIBILITY("U"),
    HANDLER_DESTROY("V");

    private String key;

    ClientToServerModel(final String key) {
        this.key = key;
    }

    public final String toStringValue() {
        return key;
    }

}
