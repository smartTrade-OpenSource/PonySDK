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
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

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

        if (update.containsKey(Model.TEXT)) {
            uiObject.setText(update.getString(Model.TEXT));
        } else if (update.containsKey(Model.HTML)) {
            uiObject.setHTML(update.getString(Model.HTML));
        } else if (update.containsKey(Model.ENABLED)) {
            uiObject.setEnabled(update.getBoolean(Model.ENABLED));
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public void addHandler(final PTInstruction instruction, final UIService uiService) {
        if (instruction.containsKey(Model.HANDLER_COMMAND)) {
            uiObject.setScheduledCommand(new ScheduledCommand() {

                @Override
                public void execute() {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(instruction.getObjectID());
                    // eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.put(Model.HANDLER_COMMAND);
                    uiService.sendDataToServer(eventInstruction);
                }
            });
        } else {
            super.addHandler(instruction, uiService);
        }
    }
}
