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

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTTextBoxBase extends PTValueBoxBase<String> {

    @Override
    public void addHandler(final AddHandler addHandler, final UIService uiService) {
        if (HandlerType.STRING_VALUE_CHANGE_HANDLER.equals(addHandler.getType())) {
            cast().addValueChangeHandler(new ValueChangeHandler<String>() {

                @Override
                public void onValueChange(ValueChangeEvent<String> event) {
                    final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), HandlerType.STRING_VALUE_CHANGE_HANDLER);
                    eventInstruction.setMainPropertyValue(PropertyKey.VALUE, event.getValue());
                    uiService.triggerEvent(eventInstruction);
                }
            });
        }

        super.addHandler(addHandler, uiService);
    }

    @Override
    public void update(Update update, UIService uiService) {

        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getKey();
        if (PropertyKey.TEXT.equals(propertyKey)) {
            cast().setText(property.getValue());
            return;
        }

        super.update(update, uiService);
    }

    @Override
    public com.google.gwt.user.client.ui.TextBoxBase cast() {
        return (com.google.gwt.user.client.ui.TextBoxBase) uiObject;
    }
}
