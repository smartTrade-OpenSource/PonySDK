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

public enum HandlerModel {

    HANDLER_DOM_BLUR(true),
    HANDLER_DOM_CHANGE_HANDLER(true),
    HANDLER_DOM_CLICK(true),
    HANDLER_DOM_CONTEXT_MENU(true),
    HANDLER_DOM_DOUBLE_CLICK(true),
    HANDLER_DOM_DRAG_END(true),
    HANDLER_DOM_DRAG_ENTER(true),
    HANDLER_DOM_DRAG_LEAVE(true),
    HANDLER_DOM_DRAG_OVER(true),
    HANDLER_DOM_DRAG_START(true),
    HANDLER_DOM_DROP(true),
    HANDLER_DOM_FOCUS(true),
    HANDLER_DOM_KEY_DOWN(true),
    HANDLER_DOM_KEY_PRESS(true),
    HANDLER_DOM_KEY_UP(true),
    HANDLER_DOM_MOUSE_DOWN(true),
    HANDLER_DOM_MOUSE_OUT(true),
    HANDLER_DOM_MOUSE_OVER(true),
    HANDLER_DOM_MOUSE_UP(true),
    HANDLER_DOM_MOUSE_WHELL(true),

    HANDLER_EMBEDED_STREAM_REQUEST(false),
    HANDLER_CHANGE(false),
    HANDLER_POPUP_POSITION(false),
    HANDLER_RESIZE(false),
    HANDLER_STRING_VALUE_CHANGE(false),
    HANDLER_COMMAND(false),
    HANDLER_BEFORE_SELECTION(false),
    HANDLER_SELECTION(false),
    HANDLER_STRING_SELECTION(false),
    HANDLER_STREAM_REQUEST(false),
    HANDLER_SCROLL(false),
    HANDLER_PASTE(false);

    private static final HandlerModel[] VALUES = HandlerModel.values();

    private boolean domHandler;

    private HandlerModel(final boolean domHandler) {
        this.domHandler = domHandler;
    }

    public final byte getValue() {
        return (byte) ordinal();
    }

    public boolean isDomHandler() {
        return domHandler;
    }

    public static HandlerModel fromRawValue(final int rawValue) {
        return VALUES[rawValue];
    }

}
