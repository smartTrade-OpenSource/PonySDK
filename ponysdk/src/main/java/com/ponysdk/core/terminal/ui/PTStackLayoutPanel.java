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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.StackLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTStackLayoutPanel extends PTWidget<StackLayoutPanel> {

    private Unit unit;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        // ServerToClientModel.UNIT
        unit = Unit.values()[buffer.readBinaryModel().getIntValue()];
        super.create(buffer, objectId, uiBuilder);
    }

    @Override
    protected StackLayoutPanel createUIObject() {
        return new StackLayoutPanel(unit);
    }

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        super.add(buffer, ptObject);

        final Widget w = asWidget(ptObject);
        final String header = buffer.readBinaryModel().getStringValue();
        final double headerSize = buffer.readBinaryModel().getDoubleValue();

        uiObject.add(w, header, true, headerSize);
    }

    @Override
    public void remove(final ReaderBuffer buffer, final PTObject ptObject) {
        uiObject.remove(asWidget(ptObject));
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.WIDGET_ID == model) {
            uiObject.showWidget(asWidget(binaryModel.getIntValue(), uiBuilder));
            return true;
        } else if (ServerToClientModel.ANIMATE == model) {
            uiObject.animate(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.ANIMATION_DURATION == model) {
            uiObject.setAnimationDuration(binaryModel.getIntValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel) {
        if (HandlerModel.HANDLER_SELECTION == handlerModel) {
            uiObject.addSelectionHandler(event -> {
                // FIXME not read on the server side
                final PTInstruction eventInstruction = new PTInstruction(getObjectID());
                eventInstruction.put(ClientToServerModel.HANDLER_SELECTION, event.getSelectedItem());
                uiBuilder.sendDataToServer(uiObject, eventInstruction);
            });
        } else if (HandlerModel.HANDLER_BEFORE_SELECTION == handlerModel) {
            uiObject.addBeforeSelectionHandler(event -> {
                // FIXME not read on the server side
                final PTInstruction eventInstruction = new PTInstruction(getObjectID());
                eventInstruction.put(ClientToServerModel.HANDLER_BEFORE_SELECTION, event.getItem());
                uiBuilder.sendDataToServer(uiObject, eventInstruction);
            });
        } else {
            super.addHandler(buffer, handlerModel);
        }
    }

    @Override
    public void removeHandler(final ReaderBuffer buffer, final HandlerModel handlerModel) {
        if (HandlerModel.HANDLER_SELECTION == handlerModel) {
            // TODO Remove HANDLER_SELECTION
        } else if (HandlerModel.HANDLER_BEFORE_SELECTION == handlerModel) {
            // TODO Remove HANDLER_BEFORE_SELECTION
        } else {
            super.removeHandler(buffer, handlerModel);
        }
    }

}
