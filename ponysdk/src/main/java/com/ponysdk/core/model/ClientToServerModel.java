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
    COOKIE_NAME("a"),
    COOKIE_VALUE("b"),

    TYPE_HISTORY("c"),

    WIDGET_POSITION("d"),
    POPUP_POSITION("e"),
    EVENT_INFO("f"),
    DOM_HANDLER_TYPE("g"),

    TERMINAL_LATENCY("h"),
    APPLICATION_ID("i"),

    PARENT_OBJECT_ID("j"),
    VALUE_KEY("k"),
    REPLACEMENT_STRING("l"),
    COMMAND_ID("m"),
    STREAM_REQUEST_ID("n"),
    SIZE("o"),
    DRAG_SRC("p"),
    START_DATE("q"),
    END_DATE("r"),
    KEY_FILTER("s"),
    RESULT("t"),

    OPTION_TABINDEX_ACTIVATED("u"),

    HEARTBEAT_REQUEST("v"),

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

    private ClientToServerModel(final String key) {
        this.key = key;
    }

    public final String toStringValue() {
        return key;
    }

}
