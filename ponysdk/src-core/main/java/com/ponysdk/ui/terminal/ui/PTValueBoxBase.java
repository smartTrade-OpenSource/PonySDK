/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.ui.terminal.ui;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.EventInstruction;

public class PTValueBoxBase<T> extends PTFocusWidget {

    @Override
    public void addHandler(final AddHandler addHandler, final UIService uiService) {

        if (HandlerType.CHANGE_HANDLER.equals(addHandler.getHandlerType())) {
            cast().addChangeHandler(new ChangeHandler() {

                @Override
                public void onChange(ChangeEvent event) {
                    final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), HandlerType.CHANGE_HANDLER);
                    uiService.triggerEvent(eventInstruction);
                }
            });
            return;
        }

        super.addHandler(addHandler, uiService);
    }

    @SuppressWarnings("unchecked")
    @Override
    public com.google.gwt.user.client.ui.ValueBoxBase<T> cast() {
        return (com.google.gwt.user.client.ui.ValueBoxBase<T>) uiObject;
    }

}
