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

    HEARTBEAT("0"),

    OBJECT_ID("1"),

    UI_CONTEXT_ID("2"),
    WINDOW_ID("3"),
    FRAME_ID("4"),

    NATIVE("5"),
    APPLICATION_INSTRUCTIONS("6"),

    ERROR_MSG("7"),
    WARN_MSG("8"),
    INFO_MSG("9"),

    COOKIES("a"),
    COOKIE_NAME("b"),
    COOKIE_VALUE("c"),

    TYPE_HISTORY("d"),

    WIDGET_POSITION("e"),
    POPUP_POSITION("f"),
    EVENT_INFO("g"),
    DOM_HANDLER_TYPE("h"),

    PING_SERVER("i"),
    APPLICATION_ID("j"),

    PARENT_OBJECT_ID("k"),
    VALUE_KEY("l"),
    REPLACEMENT_STRING("m"),
    COMMAND_ID("n"),
    STREAM_REQUEST_ID("o"),
    SIZE("p"),
    DRAG_SRC("q"),
    START_DATE("r"),
    END_DATE("s"),
    KEY_FILTER("t"),
    RESULT("u"),

    OPTION_TABINDEX_ACTIVATED("v"),

    HANDLER_BOOLEAN_VALUE_CHANGE("w"),
    HANDLER_DATE_VALUE_CHANGE("x"),
    HANDLER_STRING_VALUE_CHANGE("y"),
    HANDLER_BEFORE_SELECTION("z"),
    HANDLER_COMMAND("A"),
    HANDLER_CLOSE("B"),
    HANDLER_OPEN("C"),
    HANDLER_RESIZE("D"),
    HANDLER_SELECTION("E"),
    HANDLER_SHOW_RANGE("F"),
    HANDLER_STRING_SELECTION("G"),
    HANDLER_SUBMIT_COMPLETE("H"),
    HANDLER_SCROLL("I"),
    HANDLER_SCROLL_HEIGHT("J"),
    HANDLER_SCROLL_WIDTH("K"),
    HANDLER_SCROLL_VERTICAL("L"),
    HANDLER_SCROLL_HORIZONTAL("M"),
    HANDLER_CHANGE("N"),
    HANDLER_PASTE("O"),
    HANDLER_DESTROY("P");

    private String key;

    private ClientToServerModel(final String key) {
        this.key = key;
    }

    public final String toStringValue() {
        return key;
    }

}
