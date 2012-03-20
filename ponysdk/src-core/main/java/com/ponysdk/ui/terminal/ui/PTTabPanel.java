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
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Remove;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTTabPanel extends PTWidget {

    @Override
    public void create(final Create create, final UIService uiService) {
        init(create, uiService, new com.google.gwt.user.client.ui.TabPanel());
    }

    @Override
    public void add(final Add add, final UIService uiService) {

        final Widget w = asWidget(add.getObjectID(), uiService);
        final com.google.gwt.user.client.ui.TabPanel tabPanel = cast();

        final Property beforeIndex = add.getMainProperty().getChildProperty(PropertyKey.BEFORE_INDEX);
        final Property tabText = add.getMainProperty().getChildProperty(PropertyKey.TAB_TEXT);
        final Property tabWidget = add.getMainProperty().getChildProperty(PropertyKey.TAB_WIDGET);

        if (tabText != null) {
            tabPanel.insert(w, tabText.getValue(), beforeIndex.getIntValue());
        } else if (tabWidget != null) {
            final PTWidget ptWidget = (PTWidget) uiService.getPTObject(tabWidget.getLongValue());
            tabPanel.insert(w, ptWidget.cast(), beforeIndex.getIntValue());
        }

        if (tabPanel.getWidgetCount() == 1) {
            tabPanel.selectTab(0);
        }
    }

    @Override
    public void addHandler(final AddHandler addHandler, final UIService uiService) {

        if (HandlerType.SELECTION_HANDLER.equals(addHandler.getHandlerType())) {
            final com.google.gwt.user.client.ui.TabPanel tabPanel = cast();
            tabPanel.addSelectionHandler(new SelectionHandler<Integer>() {

                @Override
                public void onSelection(final SelectionEvent<Integer> event) {
                    final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), addHandler.getHandlerType());
                    eventInstruction.setMainPropertyValue(PropertyKey.VALUE, event.getSelectedItem());
                    uiService.triggerEvent(eventInstruction);
                }
            });
            return;
        }

        if (HandlerType.BEFORE_SELECTION_HANDLER.equals(addHandler.getHandlerType())) {
            cast().addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {

                @Override
                public void onBeforeSelection(final BeforeSelectionEvent<Integer> event) {
                    final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), HandlerType.BEFORE_SELECTION_HANDLER);
                    eventInstruction.setMainPropertyValue(PropertyKey.VALUE, event.getItem());
                    uiService.triggerEvent(eventInstruction);
                }
            });
            return;
        }

        super.addHandler(addHandler, uiService);
    }

    @Override
    public void remove(final Remove remove, final UIService uiService) {
        final com.google.gwt.user.client.ui.Widget w = asWidget(remove.getObjectID(), uiService);
        cast().remove(w);
    }

    @Override
    public void update(final Update update, final UIService uiService) {

        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getPropertyKey();

        switch (propertyKey) {
            case SELECTED_INDEX:
                cast().selectTab(property.getIntValue());
                break;

            default:
                break;
        }

        super.update(update, uiService);
    }

    @Override
    public com.google.gwt.user.client.ui.TabPanel cast() {
        return (com.google.gwt.user.client.ui.TabPanel) uiObject;
    }
}
