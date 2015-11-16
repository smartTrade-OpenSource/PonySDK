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
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTTabLayoutPanel extends PTWidget<TabLayoutPanel> {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new TabLayoutPanel(2, Unit.EM));
    }

    @Override
    public void add(final PTInstruction add, final UIService uiService) {

        final Widget w = asWidget(add.getObjectID(), uiService);
        final TabLayoutPanel tabPanel = uiObject;

        final int beforeIndex;
        if (add.containsKey(Model.BEFORE_INDEX)) {
            beforeIndex = add.getInt(Model.BEFORE_INDEX);
        } else {
            beforeIndex = tabPanel.getWidgetCount();
        }

        if (add.containsKey(Model.TAB_TEXT)) {
            tabPanel.insert(w, add.getString(Model.TAB_TEXT), beforeIndex);
        } else if (add.containsKey(Model.TAB_WIDGET)) {
            final PTWidget<?> ptWidget = (PTWidget<?>) uiService.getPTObject(add.getInt(Model.TAB_WIDGET));
            tabPanel.insert(w, ptWidget.cast(), beforeIndex);
        }
    }

    @Override
    public void addHandler(final PTInstruction instruction, final UIService uiService) {
        if (instruction.containsKey(Model.HANDLER_SELECTION_HANDLER)) {
            uiObject.addSelectionHandler(new SelectionHandler<Integer>() {

                @Override
                public void onSelection(final SelectionEvent<Integer> event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(instruction.getObjectID());
                    // eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.put(Model.HANDLER_SELECTION_HANDLER);
                    eventInstruction.put(Model.INDEX, uiObject.getSelectedIndex());
                    uiService.sendDataToServer(uiObject, eventInstruction);
                }
            });
        } else if (instruction.containsKey(Model.HANDLER_BEFORE_SELECTION_HANDLER)) {
            uiObject.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {

                @Override
                public void onBeforeSelection(final BeforeSelectionEvent<Integer> event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(instruction.getObjectID());
                    // eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.put(Model.HANDLER_BEFORE_SELECTION_HANDLER);
                    eventInstruction.put(Model.INDEX, event.getItem());
                    uiService.sendDataToServer(uiObject, eventInstruction);
                }
            });
        } else {
            super.addHandler(instruction, uiService);
        }

    }

    @Override
    public void remove(final PTInstruction remove, final UIService uiService) {
        final Widget w = asWidget(remove.getObjectID(), uiService);
        uiObject.remove(w);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(Model.ANIMATE)) {
            uiObject.animate(update.getInt(Model.ANIMATE));
        } else if (update.containsKey(Model.VERTICAL_ALIGNMENT)) {
            uiObject.setAnimationVertical(update.getBoolean(Model.VERTICAL_ALIGNMENT));
        } else if (update.containsKey(Model.ANIMATION_DURATION)) {
            uiObject.setAnimationDuration(update.getInt(Model.ANIMATION_DURATION));
        } else if (update.containsKey(Model.SELECTED_INDEX)) {
            uiObject.selectTab(update.getInt(Model.SELECTED_INDEX));
        } else {
            super.update(update, uiService);
        }
    }
}
