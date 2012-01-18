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

public class PTTabLayoutPanel extends PTResizeComposite {

    // TODO mbarbier WTF ??
    private int tabIndex = 0;

    private Widget tabWidget = null;

    @Override
    public void create(Create create, UIService uiService) {
        init(new com.google.gwt.user.client.ui.TabLayoutPanel(2, Unit.EM));
    }

    @Override
    public void add(Add add, UIService uiService) {

        final Widget w = asWidget(add.getObjectID(), uiService);
        final com.google.gwt.user.client.ui.TabLayoutPanel tabLayoutPanel = cast();

        if (add.getMainProperty().getKey().equals(PropertyKey.TAB_WIDGET)) {
            tabIndex = add.getMainProperty().getIntProperty(PropertyKey.TAB_WIDGET);
            tabWidget = w;
        } else {
            final Property childProperty = add.getMainProperty().getChildProperty(PropertyKey.TAB_TEXT);
            if (childProperty != null) {
                tabLayoutPanel.add(w, childProperty.getValue());
            } else {
                tabLayoutPanel.insert(w, tabWidget, tabIndex);
            }
        }

    }

    @Override
    public void addHandler(final AddHandler addHandler, final UIService uiService) {

        if (HandlerType.SELECTION_HANDLER.equals(addHandler.getType())) {
            final com.google.gwt.user.client.ui.TabLayoutPanel tabLayoutPanel = cast();
            tabLayoutPanel.addSelectionHandler(new SelectionHandler<Integer>() {

                @Override
                public void onSelection(SelectionEvent<Integer> event) {
                    final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), addHandler.getType());
                    eventInstruction.setMainPropertyValue(PropertyKey.VALUE, tabLayoutPanel.getSelectedIndex());
                    uiService.triggerEvent(eventInstruction);
                }
            });
            return;
        }

        if (HandlerType.BEFORE_SELECTION_HANDLER.equals(addHandler.getType())) {
            cast().addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {

                @Override
                public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
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
    public void remove(Remove remove, UIService uiService) {
        final com.google.gwt.user.client.ui.Widget w = asWidget(remove.getObjectID(), uiService);
        cast().remove(w);
    }

    @Override
    public void update(Update update, UIService uiService) {

        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getKey();
        if (PropertyKey.ANIMATION.equals(propertyKey)) {
            cast().animate(1);
            return;
        }

        super.update(update, uiService);
    }

    @Override
    public com.google.gwt.user.client.ui.TabLayoutPanel cast() {
        return (com.google.gwt.user.client.ui.TabLayoutPanel) uiObject;
    }
}
