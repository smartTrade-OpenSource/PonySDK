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

    NATIVE("4"),
    APPLICATION_INSTRUCTIONS("5"),
    INFO_MSG("6"),
    COOKIES("7"),
    COOKIE_NAME("8"),
    COOKIE_VALUE("9"),

    TYPE_HISTORY("a"),

    WIDGET_POSITION("b"),
    POPUP_POSITION("c"),
    EVENT_INFO("d"),
    DOM_HANDLER_TYPE("e"),

    ERROR_MSG("f"),
    YEAR("g"),
    MONTH("h"),
    DAY("i"),

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

    HANDLER_BOOLEAN_VALUE_CHANGE("u"),
    HANDLER_DATE_VALUE_CHANGE("v"),
    HANDLER_STRING_VALUE_CHANGE("x"),
    HANDLER_BEFORE_SELECTION("y"),
    HANDLER_CHANGE("z"),
    HANDLER_COMMAND("A"),
    HANDLER_CLOSE("B"),
    HANDLER_OPEN("C"),
    HANDLER_RESIZE("D"),
    HANDLER_SELECTION("E"),
    HANDLER_SHOW_RANGE("F"),
    HANDLER_STRING_SELECTION("G"),
    HANDLER_SUBMIT_COMPLETE("H"),

    PING_SERVER("I"),

    HANDLER_SCROLL("J"),
    HANDLER_SCROLL_HEIGHT("K"),
    HANDLER_SCROLL_WIDTH("L"),
    HANDLER_SCROLL_VERTICAL("M"),
    HANDLER_SCROLL_HORIZONTAL("N");

    private String key;

    ClientToServerModel(final String key) {
        this.key = key;
    }

    public String toStringValue() {
        return key;
    }

}
