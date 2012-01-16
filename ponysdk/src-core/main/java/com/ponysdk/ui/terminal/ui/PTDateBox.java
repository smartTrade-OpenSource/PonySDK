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

import java.util.Date;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTDateBox extends PTWidget {

    @Override
    public void create(Create create, UIService uiService) {
        init(new com.google.gwt.user.datepicker.client.DateBox());
    }

    @Override
    public void addHandler(final AddHandler addHandler, final UIService uiService) {

        if (HandlerType.DATE_VALUE_CHANGE_HANDLER.equals(addHandler.getType())) {
            final com.google.gwt.user.datepicker.client.DateBox dateBox = cast();
            final TextBox textBox = dateBox.getTextBox();
            dateBox.addValueChangeHandler(new ValueChangeHandler<Date>() {

                @Override
                public void onValueChange(ValueChangeEvent<Date> event) {
                    final String date = dateBox.getTextBox().getText();
                    final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), HandlerType.DATE_VALUE_CHANGE_HANDLER);
                    eventInstruction.setMainPropertyValue(PropertyKey.VALUE, date);
                    uiService.triggerEvent(eventInstruction);
                }
            });
            textBox.addValueChangeHandler(new ValueChangeHandler<String>() {

                @Override
                public void onValueChange(ValueChangeEvent<String> event) {
                    final String date = dateBox.getTextBox().getText();
                    final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), HandlerType.DATE_VALUE_CHANGE_HANDLER);
                    eventInstruction.setMainPropertyValue(PropertyKey.VALUE, date);
                    uiService.triggerEvent(eventInstruction);
                }
            });
            return;
        }

        super.addHandler(addHandler, uiService);
    }

    @Override
    public void update(Update update, UIService uiService) {

        final Property mainProperty = update.getMainProperty();
        final com.google.gwt.user.datepicker.client.DateBox dateBox = cast();

        for (final Property property : mainProperty.getChildProperties().values()) {
            final PropertyKey propertyKey = property.getKey();
            if (PropertyKey.VALUE.equals(propertyKey)) {
                dateBox.getTextBox().setText(property.getValue());
            } else if (PropertyKey.DATE_FORMAT.equals(propertyKey)) {
                DefaultFormat format = null;
                switch (property.getIntValue()) {
                    case 0:
                        format = new DefaultFormat(DateTimeFormat.getFormat(PredefinedFormat.DATE_FULL));
                        dateBox.setFormat(format);
                        break;
                    case 1:
                        format = new DefaultFormat(DateTimeFormat.getFormat(PredefinedFormat.DATE_LONG));
                        break;
                    case 2:
                        format = new DefaultFormat(DateTimeFormat.getFormat(PredefinedFormat.DATE_MEDIUM));
                        break;
                    case 3:
                        format = new DefaultFormat(DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT));
                        break;

                    default:
                        format = new DefaultFormat(DateTimeFormat.getFormat(PredefinedFormat.DATE_MEDIUM));
                        break;
                }
                dateBox.setFormat(format);
                break;
            } else if (PropertyKey.DATE_FORMAT_PATTERN.equals(propertyKey)) {
                dateBox.setFormat(new DefaultFormat(DateTimeFormat.getFormat(property.getValue())));
            } else if (PropertyKey.ENABLED.equals(propertyKey)) {
                dateBox.setEnabled(property.getBooleanValue());
            }
        }

        super.update(update, uiService);
    }

    @Override
    public com.google.gwt.user.datepicker.client.DateBox cast() {
        return (com.google.gwt.user.datepicker.client.DateBox) uiObject;
    }
}
