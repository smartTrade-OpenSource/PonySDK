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

import com.google.gwt.user.client.ui.ValueBoxBase;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public abstract class PTValueBoxBase<T extends ValueBoxBase<W>, W> extends PTFocusWidget<T> {

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.TEXT == model) {
            uiObject.setText(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.SELECT_ALL == model) {
            uiObject.selectAll();
            return true;
        } else if (ServerToClientModel.CURSOR_POSITION == model) {
            uiObject.setCursorPos(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.SELECTION_RANGE_START == model) {
            final int start = binaryModel.getIntValue();
            // ServerToClientModel.SELECTION_RANGE_LENGTH
            final int length = buffer.readBinaryModel().getIntValue();
            uiObject.setSelectionRange(start, length);
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel) {
        if (HandlerModel.HANDLER_CHANGE == handlerModel) {
            uiObject.addChangeHandler(event -> {
                final PTInstruction eventInstruction = new PTInstruction(getObjectID());
                eventInstruction.put(ClientToServerModel.HANDLER_CHANGE);
                uiBuilder.sendDataToServer(uiObject, eventInstruction);
            });
        } else {
            super.addHandler(buffer, handlerModel);
        }
    }

    @Override
    public void removeHandler(final ReaderBuffer buffer, final HandlerModel handlerModel) {
        if (HandlerModel.HANDLER_CHANGE == handlerModel) {
            // TODO Remove HANDLER_CHANGE
        } else {
            super.removeHandler(buffer, handlerModel);
        }
    }

}
