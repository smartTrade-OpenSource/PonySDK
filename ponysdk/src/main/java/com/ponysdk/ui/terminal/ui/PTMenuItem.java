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
import com.ponysdk.ui.model.ClientToServerModel;
import com.ponysdk.ui.model.HandlerModel;
import com.ponysdk.ui.model.ServerToClientModel;
import com.ponysdk.ui.terminal.UIBuilder;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTMenuItem extends PTUIObject<MenuItem> {

    @Override
    protected MenuItem createUIObject() {
        return new MenuItem("?", (Command) null);
    }

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        final PTMenuBar child = (PTMenuBar) ptObject;
        uiObject.setSubMenu(child.uiObject);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.TEXT.equals(binaryModel.getModel())) {
            uiObject.setText(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.HTML.equals(binaryModel.getModel())) {
            uiObject.setHTML(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.ENABLED.equals(binaryModel.getModel())) {
            uiObject.setEnabled(binaryModel.getBooleanValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel, final UIBuilder uiService) {
        if (HandlerModel.HANDLER_COMMAND.equals(handlerModel)) {
            uiObject.setScheduledCommand(new ScheduledCommand() {

                @Override
                public void execute() {
                    final PTInstruction eventInstruction = new PTInstruction(getObjectID());
                    // eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.put(ClientToServerModel.HANDLER_COMMAND);
                    uiService.sendDataToServer(eventInstruction);
                }
            });
        } else {
            super.addHandler(buffer, handlerModel, uiService);
        }
    }
}
