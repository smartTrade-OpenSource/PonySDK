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
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTTabLayoutPanel extends PTWidget<TabLayoutPanel> {

    @Override
    protected TabLayoutPanel createUIObject() {
        return new TabLayoutPanel(2, Unit.EM);
    }

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        final Widget w = asWidget(ptObject);
        final TabLayoutPanel tabPanel = uiObject;

        final BinaryModel binaryModel = buffer.readBinaryModel();
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.TAB_TEXT == model) {
            final String value = binaryModel.getStringValue();
            final BinaryModel beforeIndexModel = buffer.readBinaryModel();
            if (ServerToClientModel.BEFORE_INDEX == beforeIndexModel.getModel()) {
                tabPanel.insert(w, value, beforeIndexModel.getIntValue());
            } else {
                buffer.rewind(beforeIndexModel);
                tabPanel.add(w, value);
            }
        } else if (ServerToClientModel.TAB_WIDGET == model) {
            final PTWidget<?> ptWidget = (PTWidget<?>) uiBuilder.getPTObject(binaryModel.getIntValue());
            final BinaryModel beforeIndexModel = buffer.readBinaryModel();
            if (ServerToClientModel.BEFORE_INDEX == beforeIndexModel.getModel()) {
                tabPanel.insert(w, ptWidget.uiObject, beforeIndexModel.getIntValue());
            } else {
                buffer.rewind(beforeIndexModel);
                tabPanel.add(w, ptWidget.uiObject);
            }
        }
    }

    @Override
    public void remove(final ReaderBuffer buffer, final PTObject ptObject) {
        uiObject.remove(asWidget(ptObject));
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.ANIMATE == model) {
            uiObject.animate(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.VERTICAL == model) {
            uiObject.setAnimationVertical(binaryModel.getBooleanValue());
            return true;
        } else if (ServerToClientModel.ANIMATION_DURATION == model) {
            uiObject.setAnimationDuration(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.SELECTED_INDEX == model) {
            uiObject.selectTab(binaryModel.getIntValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel) {
        if (HandlerModel.HANDLER_SELECTION == handlerModel) {
            uiObject.addSelectionHandler(event -> {
                final PTInstruction eventInstruction = new PTInstruction(getObjectID());
                eventInstruction.put(ClientToServerModel.HANDLER_SELECTION, uiObject.getSelectedIndex());
                uiBuilder.sendDataToServer(uiObject, eventInstruction);
            });
        } else if (HandlerModel.HANDLER_BEFORE_SELECTION == handlerModel) {
            uiObject.addBeforeSelectionHandler(event -> {
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
