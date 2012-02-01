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

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.RadioButton;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTRadioButton extends PTCheckBox {

    private static Map<String, PTRadioButton> lastSelectedRadioButtonByGroup = new HashMap<String, PTRadioButton>();

    private long objectID;

    @Override
    public void create(final Create create, final UIService uiService) {
        init(new com.google.gwt.user.client.ui.RadioButton(null));
        objectID = create.getObjectID();
    }

    @Override
    public void update(final Update update, final UIService uiService) {

        final Property mainProperty = update.getMainProperty();
        final com.google.gwt.user.client.ui.RadioButton radioButton = cast();

        for (final Property property : mainProperty.getChildProperties().values()) {
            final PropertyKey propertyKey = property.getKey();
            if (PropertyKey.NAME.equals(propertyKey)) {
                radioButton.setName(property.getValue());
            }
        }

        super.update(update, uiService);
    }

    @Override
    protected void addValueChangeHandler(final AddHandler addHandler, final UIService uiService) {
        final RadioButton radioButton = cast();

        radioButton.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

            @Override
            public void onValueChange(final ValueChangeEvent<Boolean> event) {
                fireInstruction(addHandler.getObjectID(), uiService, event.getValue());

                if (cast().getName() != null) {
                    PTRadioButton previouslySelected = lastSelectedRadioButtonByGroup.get(cast().getName());
                    if (previouslySelected != null && !previouslySelected.equals(radioButton)) {
                        fireInstruction(previouslySelected.getObjectID(), uiService, previouslySelected.cast().getValue());
                    }
                    lastSelectedRadioButtonByGroup.put(radioButton.getName(), PTRadioButton.this);
                }
            }
        });
    }

    protected void fireInstruction(final long objectID, final UIService uiService, final boolean value) {
        final EventInstruction eventInstruction = new EventInstruction(objectID, HandlerType.BOOLEAN_VALUE_CHANGE_HANDLER);
        eventInstruction.setMainPropertyValue(PropertyKey.VALUE, value);
        uiService.triggerEvent(eventInstruction);
    }

    @Override
    public com.google.gwt.user.client.ui.RadioButton cast() {
        return (com.google.gwt.user.client.ui.RadioButton) uiObject;
    }

    public long getObjectID() {
        return objectID;
    }

}
