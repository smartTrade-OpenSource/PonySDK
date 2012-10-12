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
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTTabPanel extends PTWidget<TabPanel> {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new TabPanel());
    }

    @Override
    public void add(final PTInstruction add, final UIService uiService) {

        final Widget w = asWidget(add.getObjectID(), uiService);
        final TabPanel tabPanel = uiObject;

        final int beforeIndex = add.getInt(PROPERTY.BEFORE_INDEX);

        if (add.containsKey(PROPERTY.TAB_TEXT)) {
            tabPanel.insert(w, add.getString(PROPERTY.TAB_TEXT), beforeIndex);
        } else if (add.containsKey(PROPERTY.TAB_WIDGET)) {
            final PTWidget<?> ptWidget = (PTWidget<?>) uiService.getPTObject(add.getLong(PROPERTY.TAB_WIDGET));
            tabPanel.insert(w, ptWidget.cast(), beforeIndex);
        }

        if (tabPanel.getWidgetCount() == 1) {
            tabPanel.selectTab(0);
        }
    }

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {
        final String handler = addHandler.getString(HANDLER.KEY);
        if (HANDLER.KEY_.SELECTION_HANDLER.equals(handler)) {
            uiObject.addSelectionHandler(new SelectionHandler<Integer>() {

                @Override
                public void onSelection(final SelectionEvent<Integer> event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(addHandler.getObjectID());
                    eventInstruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
                    eventInstruction.put(HANDLER.KEY, HANDLER.KEY_.SELECTION_HANDLER);
                    eventInstruction.put(PROPERTY.INDEX, event.getSelectedItem());
                    uiService.sendDataToServer(eventInstruction);
                }
            });
        } else if (HANDLER.KEY_.BEFORE_SELECTION_HANDLER.equals(handler)) {
            uiObject.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {

                @Override
                public void onBeforeSelection(final BeforeSelectionEvent<Integer> event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(addHandler.getObjectID());
                    eventInstruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
                    eventInstruction.put(HANDLER.KEY, HANDLER.KEY_.BEFORE_SELECTION_HANDLER);
                    eventInstruction.put(PROPERTY.INDEX, event.getItem());
                    uiService.sendDataToServer(eventInstruction);
                }
            });
        } else {
            super.addHandler(addHandler, uiService);
        }
    }

    @Override
    public void remove(final PTInstruction remove, final UIService uiService) {
        final Widget w = asWidget(remove.getObjectID(), uiService);
        uiObject.remove(w);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.SELECTED_INDEX)) {
            uiObject.selectTab(update.getInt(PROPERTY.SELECTED_INDEX));
        } else {
            super.update(update, uiService);
        }
    }

}
