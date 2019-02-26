/*
 * Copyright (c) 2018 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

public class DomHandlerConverter {

    public static final HandlerModel convert(final DomHandlerType type) {
        if (DomHandlerType.BLUR == type) return HandlerModel.HANDLER_DOM_BLUR;
        else if (DomHandlerType.CHANGE_HANDLER == type) return HandlerModel.HANDLER_DOM_CHANGE_HANDLER;
        else if (DomHandlerType.CLICK == type) return HandlerModel.HANDLER_DOM_CLICK;
        else if (DomHandlerType.CONTEXT_MENU == type) return HandlerModel.HANDLER_DOM_CONTEXT_MENU;
        else if (DomHandlerType.DOUBLE_CLICK == type) return HandlerModel.HANDLER_DOM_DOUBLE_CLICK;
        else if (DomHandlerType.DRAG_END == type) return HandlerModel.HANDLER_DOM_DRAG_END;
        else if (DomHandlerType.DRAG_ENTER == type) return HandlerModel.HANDLER_DOM_DRAG_ENTER;
        else if (DomHandlerType.DRAG_LEAVE == type) return HandlerModel.HANDLER_DOM_DRAG_LEAVE;
        else if (DomHandlerType.DRAG_OVER == type) return HandlerModel.HANDLER_DOM_DRAG_OVER;
        else if (DomHandlerType.DRAG_START == type) return HandlerModel.HANDLER_DOM_DRAG_START;
        else if (DomHandlerType.DROP == type) return HandlerModel.HANDLER_DOM_DROP;
        else if (DomHandlerType.FOCUS == type) return HandlerModel.HANDLER_DOM_FOCUS;
        else if (DomHandlerType.KEY_DOWN == type) return HandlerModel.HANDLER_DOM_KEY_DOWN;
        else if (DomHandlerType.KEY_PRESS == type) return HandlerModel.HANDLER_DOM_KEY_PRESS;
        else if (DomHandlerType.KEY_UP == type) return HandlerModel.HANDLER_DOM_KEY_UP;
        else if (DomHandlerType.MOUSE_DOWN == type) return HandlerModel.HANDLER_DOM_MOUSE_DOWN;
        else if (DomHandlerType.MOUSE_OUT == type) return HandlerModel.HANDLER_DOM_MOUSE_OUT;
        else if (DomHandlerType.MOUSE_OVER == type) return HandlerModel.HANDLER_DOM_MOUSE_OVER;
        else if (DomHandlerType.MOUSE_UP == type) return HandlerModel.HANDLER_DOM_MOUSE_UP;
        else if (DomHandlerType.MOUSE_WHELL == type) return HandlerModel.HANDLER_DOM_MOUSE_WHELL;
        else throw new IllegalArgumentException("Undefined DomHandlerType : " + type);
    }

    public static final DomHandlerType convert(final HandlerModel type) {
        if (HandlerModel.HANDLER_DOM_BLUR == type) return DomHandlerType.BLUR;
        else if (HandlerModel.HANDLER_DOM_CHANGE_HANDLER == type) return DomHandlerType.CHANGE_HANDLER;
        else if (HandlerModel.HANDLER_DOM_CLICK == type) return DomHandlerType.CLICK;
        else if (HandlerModel.HANDLER_DOM_CONTEXT_MENU == type) return DomHandlerType.CONTEXT_MENU;
        else if (HandlerModel.HANDLER_DOM_DOUBLE_CLICK == type) return DomHandlerType.DOUBLE_CLICK;
        else if (HandlerModel.HANDLER_DOM_DRAG_END == type) return DomHandlerType.DRAG_END;
        else if (HandlerModel.HANDLER_DOM_DRAG_ENTER == type) return DomHandlerType.DRAG_ENTER;
        else if (HandlerModel.HANDLER_DOM_DRAG_LEAVE == type) return DomHandlerType.DRAG_LEAVE;
        else if (HandlerModel.HANDLER_DOM_DRAG_OVER == type) return DomHandlerType.DRAG_OVER;
        else if (HandlerModel.HANDLER_DOM_DRAG_START == type) return DomHandlerType.DRAG_START;
        else if (HandlerModel.HANDLER_DOM_DROP == type) return DomHandlerType.DROP;
        else if (HandlerModel.HANDLER_DOM_FOCUS == type) return DomHandlerType.FOCUS;
        else if (HandlerModel.HANDLER_DOM_KEY_DOWN == type) return DomHandlerType.KEY_DOWN;
        else if (HandlerModel.HANDLER_DOM_KEY_PRESS == type) return DomHandlerType.KEY_PRESS;
        else if (HandlerModel.HANDLER_DOM_KEY_UP == type) return DomHandlerType.KEY_UP;
        else if (HandlerModel.HANDLER_DOM_MOUSE_DOWN == type) return DomHandlerType.MOUSE_DOWN;
        else if (HandlerModel.HANDLER_DOM_MOUSE_OUT == type) return DomHandlerType.MOUSE_OUT;
        else if (HandlerModel.HANDLER_DOM_MOUSE_OVER == type) return DomHandlerType.MOUSE_OVER;
        else if (HandlerModel.HANDLER_DOM_MOUSE_UP == type) return DomHandlerType.MOUSE_UP;
        else if (HandlerModel.HANDLER_DOM_MOUSE_WHELL == type) return DomHandlerType.MOUSE_WHELL;
        else throw new IllegalArgumentException("Undefined HandlerModel : " + type);
    }

}
