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
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Remove;

public class PTStackLayoutPanel extends PTResizeComposite {

    @Override
    public void create(final Create create, final UIService uiService) {
        final Unit unit = Unit.valueOf(create.getMainProperty().getStringPropertyValue(PropertyKey.UNIT));
        final StackLayoutPanel stackLayoutPanel = new StackLayoutPanel(unit);
        init(create, uiService, stackLayoutPanel);
    }

    @Override
    public void add(final Add add, final UIService uiService) {
        super.add(add, uiService);

        final Widget w = asWidget(add.getObjectID(), uiService);
        final StackLayoutPanel stackLayoutPanel = cast();

        final String header = add.getMainProperty().getStringPropertyValue(PropertyKey.HTML);
        final double headerSize = add.getMainProperty().getDoublePropertyValue(PropertyKey.SIZE);
        stackLayoutPanel.add(w, header, true, headerSize);
    }

    @Override
    public void addHandler(final AddHandler addHandler, final UIService uiService) {

        if (HandlerType.SELECTION_HANDLER.equals(addHandler.getType())) {
            final StackLayoutPanel stackLayoutPanel = cast();
            stackLayoutPanel.addSelectionHandler(new SelectionHandler<Integer>() {

                @Override
                public void onSelection(final SelectionEvent<Integer> event) {
                    final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), addHandler.getType());
                    eventInstruction.setMainPropertyValue(PropertyKey.VALUE, event.getSelectedItem());
                    uiService.triggerEvent(eventInstruction);
                }
            });
            return;
        }

        if (HandlerType.BEFORE_SELECTION_HANDLER.equals(addHandler.getType())) {
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
        final Widget w = asWidget(remove.getObjectID(), uiService);
        cast().remove(w);
    }

    @Override
    public StackLayoutPanel cast() {
        return (StackLayoutPanel) uiObject;
    }
}
