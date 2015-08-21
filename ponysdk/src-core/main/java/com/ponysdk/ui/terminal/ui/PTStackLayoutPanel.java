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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.StackLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTStackLayoutPanel extends PTWidget<StackLayoutPanel> {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new StackLayoutPanel(Unit.values()[create.getInt(Model.UNIT)]));
    }

    @Override
    public void add(final PTInstruction add, final UIService uiService) {
        super.add(add, uiService);

        final Widget w = asWidget(add.getObjectID(), uiService);
        final String header = add.getString(Model.HTML);
        final double headerSize = add.getDouble(Model.SIZE);

        uiObject.add(w, header, true, headerSize);
    }

    @Override
    public void addHandler(final PTInstruction instruction, final UIService uiService) {
        if (instruction.containsKey(Model.HANDLER_SELECTION_HANDLER)) {
            uiObject.addSelectionHandler(new SelectionHandler<Integer>() {

                @Override
                public void onSelection(final SelectionEvent<Integer> event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(instruction.getObjectID());
                    eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.put(Model.HANDLER_SELECTION_HANDLER);
                    eventInstruction.put(Model.VALUE, event.getSelectedItem());
                    uiService.sendDataToServer(uiObject, eventInstruction);
                }
            });
            return;
        } else if (instruction.containsKey(Model.HANDLER_BEFORE_SELECTION_HANDLER)) {
            uiObject.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {

                @Override
                public void onBeforeSelection(final BeforeSelectionEvent<Integer> event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(instruction.getObjectID());
                    eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.put(Model.HANDLER_BEFORE_SELECTION_HANDLER);
                    eventInstruction.put(Model.VALUE, event.getItem());
                    uiService.sendDataToServer(uiObject, eventInstruction);
                }
            });
            return;
        }

        super.addHandler(instruction, uiService);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(Model.OPEN)) {
            uiObject.showWidget(asWidget(update.getLong(Model.OPEN), uiService));
        } else if (update.containsKey(Model.ANIMATE)) {
            uiObject.animate(update.getInt(Model.ANIMATE));
        } else if (update.containsKey(Model.ANIMATION_DURATION)) {
            uiObject.setAnimationDuration(update.getInt(Model.ANIMATION_DURATION));
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public void remove(final PTInstruction remove, final UIService uiService) {
        uiObject.remove(asWidget(remove.getObjectID(), uiService));
    }

}
