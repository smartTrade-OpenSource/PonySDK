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

import java.util.Objects;

import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public abstract class PTTextBoxBase<T extends TextBoxBase> extends PTValueBoxBase<T, String> {

    private String lastValue;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        super.create(buffer, objectId, uiBuilder);

        uiObject.addValueChangeHandler(event -> {
            this.lastValue = event.getValue();
            final PTInstruction eventInstruction = new PTInstruction(getObjectID());
            eventInstruction.put(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE, lastValue);
            uiBuilder.sendDataToServer(uiObject, eventInstruction);
        });
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.PLACEHOLDER == model) {
            uiObject.getElement().setAttribute("placeholder", binaryModel.getStringValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    protected void triggerKeyUpEvent(final DomHandlerType domHandlerType, final KeyUpEvent event, final int[] keyFilter) {
        if (!enabled) return;
        final int nativeKeyCode = event.getNativeKeyCode();
        if (keyFilter != null) {
            for (final int keyCode : keyFilter) {
                if (keyCode == nativeKeyCode) {
                    final String newValue = uiObject.getText();
                    if (!Objects.equals(newValue, this.lastValue)) {
                        this.lastValue = newValue;
                        final PTInstruction changeHandlerInstruction = new PTInstruction(getObjectID());
                        changeHandlerInstruction.put(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE, this.lastValue);
                        uiBuilder.sendDataToServer(changeHandlerInstruction);
                    }

                    final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
                    eventInstruction.put(ClientToServerModel.VALUE_KEY, nativeKeyCode);
                    uiBuilder.sendDataToServer(eventInstruction);

                    break;
                }
            }
        } else {
            final String newValue = uiObject.getText();
            if (!Objects.equals(newValue, this.lastValue)) {
                this.lastValue = newValue;
                final PTInstruction changeHandlerInstruction = new PTInstruction(getObjectID());
                changeHandlerInstruction.put(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE, this.lastValue);
                uiBuilder.sendDataToServer(changeHandlerInstruction);
            }

            final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
            eventInstruction.put(ClientToServerModel.VALUE_KEY, nativeKeyCode);
            uiBuilder.sendDataToServer(eventInstruction);
        }
        preventOrStopEvent(event);
    }

}
