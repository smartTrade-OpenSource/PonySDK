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

import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTTabPanel extends PTWidget<TabPanel> {

    private UIService uiService;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        super.create(buffer, objectId, uiService);

        this.uiService = uiService;
    }

    @Override
    protected TabPanel createUIObject() {
        return new TabPanel();
    }

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        final Widget w = asWidget(ptObject);

        final BinaryModel binaryModel = buffer.getBinaryModel();
        if (Model.TAB_TEXT.equals(binaryModel.getModel())) {
            final BinaryModel beforeIndexModel = buffer.getBinaryModel();
            if (Model.BEFORE_INDEX.equals(beforeIndexModel.getModel())) {
                uiObject.insert(w, binaryModel.getStringValue(), beforeIndexModel.getIntValue());
            } else {
                buffer.rewind(beforeIndexModel);
                uiObject.add(w, binaryModel.getStringValue());
            }
        } else if (Model.TAB_WIDGET.equals(binaryModel.getModel())) {
            final PTWidget<?> ptWidget = (PTWidget<?>) uiService.getPTObject(binaryModel.getIntValue());
            final BinaryModel beforeIndexModel = buffer.getBinaryModel();
            if (Model.BEFORE_INDEX.equals(beforeIndexModel.getModel())) {
                uiObject.insert(w, ptWidget.cast(), beforeIndexModel.getIntValue());
            } else {
                buffer.rewind(beforeIndexModel);
                uiObject.add(w, ptWidget.cast());
            }
        }

        if (uiObject.getWidgetCount() == 1) {
            uiObject.selectTab(0);
        }
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel, final UIService uiService) {
        if (HandlerModel.HANDLER_SELECTION_HANDLER.equals(handlerModel)) {
            uiObject.addSelectionHandler(new SelectionHandler<Integer>() {

                @Override
                public void onSelection(final SelectionEvent<Integer> event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(getObjectID());
                    // eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.put(HandlerModel.HANDLER_SELECTION_HANDLER);
                    eventInstruction.put(Model.INDEX, event.getSelectedItem());
                    uiService.sendDataToServer(uiObject, eventInstruction);
                }
            });
        }
        if (HandlerModel.HANDLER_BEFORE_SELECTION_HANDLER.equals(handlerModel)) {
            uiObject.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {

                @Override
                public void onBeforeSelection(final BeforeSelectionEvent<Integer> event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(getObjectID());
                    // eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.put(HandlerModel.HANDLER_BEFORE_SELECTION_HANDLER);
                    eventInstruction.put(Model.INDEX, event.getItem());
                    uiService.sendDataToServer(uiObject, eventInstruction);
                }
            });
        } else {
            super.addHandler(buffer, handlerModel, uiService);
        }
    }

    @Override
    public void remove(final ReaderBuffer buffer, final PTObject ptObject, final UIService uiService) {
        uiObject.remove(asWidget(ptObject));
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (Model.SELECTED_INDEX.equals(binaryModel.getModel())) {
            uiObject.selectTab(binaryModel.getIntValue());
            return true;
        }
        if (Model.ANIMATION.equals(binaryModel.getModel())) {
            uiObject.setAnimationEnabled(binaryModel.getBooleanValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }

}
