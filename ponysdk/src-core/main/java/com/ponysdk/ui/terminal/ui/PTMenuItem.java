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

import com.google.gwt.user.client.Command;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTMenuItem extends PTUIObject {

    @Override
    public void create(final Create create, final UIService uiService) {
        final com.google.gwt.user.client.ui.MenuItem menuItem = new com.google.gwt.user.client.ui.MenuItem("?", (Command) null);
        init(create, uiService, menuItem);
    }

    @Override
    public void add(final Add add, final UIService uiService) {
        final PTMenuBar child = (PTMenuBar) uiService.getPTObject(add.getObjectID());
        cast().setSubMenu(child.cast());
    }

    @Override
    public void update(final Update update, final UIService uiService) {

        final Property mainProperty = update.getMainProperty();
        final com.google.gwt.user.client.ui.MenuItem menuItem = cast();

        if (PropertyKey.TEXT.equals(mainProperty.getKey())) {
            menuItem.setText(mainProperty.getValue());
            return;
        }

        for (final Property property : mainProperty.getChildProperties().values()) {
            final PropertyKey propertyKey = property.getPropertyKey();
            if (PropertyKey.HTML.equals(propertyKey)) {
                menuItem.setHTML(property.getValue());
            } else if (PropertyKey.TEXT.equals(propertyKey)) {
                menuItem.setText(property.getValue());
            } else if (PropertyKey.ENABLED.equals(propertyKey)) {
                menuItem.setEnabled(property.getBooleanValue());
            }
        }

        super.update(update, uiService);
    }

    @Override
    public void addHandler(final AddHandler addHandler, final UIService uiService) {
        if (HandlerType.COMMAND.equals(addHandler.getHandlerType())) {
            cast().setCommand(new Command() {

                @Override
                public void execute() {
                    final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), HandlerType.COMMAND);
                    uiService.triggerEvent(eventInstruction);
                }
            });
            return;
        }

        super.addHandler(addHandler, uiService);
    }

    @Override
    public com.google.gwt.user.client.ui.MenuItem cast() {
        return (com.google.gwt.user.client.ui.MenuItem) uiObject;
    }
}
