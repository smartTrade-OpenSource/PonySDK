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
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
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
        unit = Unit.values()[buffer.readBinaryModel().getByteValue()];
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
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel, final UIBuilder uiService) {
        if (HandlerModel.HANDLER_SELECTION.equals(handlerModel)) {
            uiObject.addSelectionHandler(new SelectionHandler<Integer>() {

                @Override
                public void onSelection(final SelectionEvent<Integer> event) {
                    // FIXME not read on the server side
                    final PTInstruction eventInstruction = new PTInstruction(getObjectID());
                    eventInstruction.put(ClientToServerModel.HANDLER_SELECTION, event.getSelectedItem());
                    uiService.sendDataToServer(uiObject, eventInstruction);
                }
            });
            return;
        } else if (HandlerModel.HANDLER_BEFORE_SELECTION.equals(handlerModel)) {
            uiObject.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {

                @Override
                public void onBeforeSelection(final BeforeSelectionEvent<Integer> event) {
                    // FIXME not read on the server side
                    final PTInstruction eventInstruction = new PTInstruction(getObjectID());
                    eventInstruction.put(ClientToServerModel.HANDLER_BEFORE_SELECTION, event.getItem());
                    uiService.sendDataToServer(uiObject, eventInstruction);
                }
            });
            return;
        }

        super.addHandler(buffer, handlerModel, uiService);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.WIDGET_ID.equals(binaryModel.getModel())) {
            uiObject.showWidget(asWidget(binaryModel.getIntValue(), uiBuilder));
            return true;
        }
        if (ServerToClientModel.ANIMATE.equals(binaryModel.getModel())) {
            uiObject.animate(binaryModel.getIntValue());
            return true;
        }
        if (ServerToClientModel.ANIMATION_DURATION.equals(binaryModel.getModel())) {
            uiObject.setAnimationDuration(binaryModel.getIntValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }

    @Override
    public void remove(final ReaderBuffer buffer, final PTObject ptObject, final UIBuilder uiService) {
        uiObject.remove(asWidget(ptObject));
    }

}
