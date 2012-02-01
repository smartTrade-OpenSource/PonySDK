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
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTCheckBox extends PTButtonBase {

    @Override
    public void create(Create create, UIService uiService) {
        init(new com.google.gwt.user.client.ui.CheckBox());
    }

    @Override
    public void addHandler(final AddHandler addHandler, final UIService uiService) {

        if (HandlerType.BOOLEAN_VALUE_CHANGE_HANDLER.equals(addHandler.getType())) {
            addValueChangeHandler(addHandler, uiService);
            return;
        }

        super.addHandler(addHandler, uiService);
    }

    protected void addValueChangeHandler(final AddHandler addHandler, final UIService uiService) {
        cast().addValueChangeHandler(new ValueChangeHandler<Boolean>() {

            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), HandlerType.BOOLEAN_VALUE_CHANGE_HANDLER);
                eventInstruction.setMainPropertyValue(PropertyKey.VALUE, event.getValue());
                uiService.triggerEvent(eventInstruction);
            }
        });
    }

    @Override
    public void update(Update update, UIService uiService) {

        final Property mainProperty = update.getMainProperty();
        final com.google.gwt.user.client.ui.CheckBox checkBox = cast();

        for (final Property property : mainProperty.getChildProperties().values()) {
            final PropertyKey propertyKey = property.getKey();
            if (PropertyKey.VALUE.equals(propertyKey)) {
                checkBox.setValue(property.getBooleanValue());
            } else if (PropertyKey.TEXT.equals(propertyKey)) {
                checkBox.setText(property.getValue());
            } else if (PropertyKey.HTML.equals(propertyKey)) {
                checkBox.setHTML(property.getValue());
            }
        }

        super.update(update, uiService);
    }

    @Override
    public com.google.gwt.user.client.ui.CheckBox cast() {
        return (com.google.gwt.user.client.ui.CheckBox) uiObject;
    }

}
