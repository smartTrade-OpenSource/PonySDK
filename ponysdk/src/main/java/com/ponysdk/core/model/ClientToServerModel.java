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
    INFO_MSG("7"),
    COOKIES("8"),
    COOKIE_NAME("9"),
    COOKIE_VALUE("a"),

    TYPE_HISTORY("b"),

    WIDGET_POSITION("c"),
    POPUP_POSITION("d"),
    EVENT_INFO("e"),
    DOM_HANDLER_TYPE("f"),

    ERROR_MSG("g"),
    YEAR("h"),
    MONTH("i"),
    DAY("j"),

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

    HANDLER_BOOLEAN_VALUE_CHANGE("v"),
    HANDLER_DATE_VALUE_CHANGE("w"),
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
    HANDLER_SCROLL_HORIZONTAL("N"),

    APPLICATION_ID("O");

    private String key;

    ClientToServerModel(final String key) {
        this.key = key;
    }

    public String toStringValue() {
        return key;
    }

}
