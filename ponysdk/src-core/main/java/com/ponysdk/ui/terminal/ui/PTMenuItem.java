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

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTMenuItem extends PTUIObject<MenuItem> {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        final MenuItem menuItem = new MenuItem("?", (Command) null);
        init(create, uiService, menuItem);
    }

    @Override
    public void add(final PTInstruction add, final UIService uiService) {
        final PTMenuBar child = (PTMenuBar) uiService.getPTObject(add.getObjectID());
        uiObject.setSubMenu(child.uiObject);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {

        if (update.containsKey(PROPERTY.TEXT)) {
            uiObject.setText(update.getString(PROPERTY.TEXT));
        } else if (update.containsKey(PROPERTY.HTML)) {
            uiObject.setHTML(update.getString(PROPERTY.HTML));
        } else if (update.containsKey(PROPERTY.ENABLED)) {
            uiObject.setEnabled(update.getBoolean(PROPERTY.ENABLED));
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {
        final String handler = addHandler.getString(HANDLER.KEY);
        if (HANDLER.KEY_.COMMAND.equals(handler)) {
            uiObject.setScheduledCommand(new ScheduledCommand() {

                @Override
                public void execute() {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(addHandler.getObjectID());
                    eventInstruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
                    eventInstruction.put(HANDLER.KEY, HANDLER.KEY_.COMMAND);
                    uiService.triggerEvent(eventInstruction);
                }
            });
        } else {
            super.addHandler(addHandler, uiService);
        }
    }
}
