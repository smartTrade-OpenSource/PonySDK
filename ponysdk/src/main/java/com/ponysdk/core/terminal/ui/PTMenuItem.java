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

package com.ponysdk.core.terminal.ui;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

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
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.TEXT.ordinal() == modelOrdinal) {
            uiObject.setText(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.HTML.ordinal() == modelOrdinal) {
            uiObject.setHTML(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.ENABLED.ordinal() == modelOrdinal) {
            uiObject.setEnabled(binaryModel.getBooleanValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel) {
        if (HandlerModel.HANDLER_COMMAND.equals(handlerModel)) {
            uiObject.setScheduledCommand(() -> {
                final PTInstruction eventInstruction = new PTInstruction(getObjectID());
                eventInstruction.put(ClientToServerModel.HANDLER_COMMAND);
                uiBuilder.sendDataToServer(eventInstruction);
            });
        } else {
            super.addHandler(buffer, handlerModel);
        }
    }

    @Override
    public void removeHandler(final ReaderBuffer buffer, final HandlerModel handlerModel) {
        if (HandlerModel.HANDLER_COMMAND.equals(handlerModel)) {
            uiObject.setScheduledCommand(null);
        } else {
            super.removeHandler(buffer, handlerModel);
        }
    }

}
