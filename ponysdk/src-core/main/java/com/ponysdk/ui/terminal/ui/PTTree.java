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

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.TreeItem;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Remove;

public class PTTree extends PTWidget {

    @Override
    public void create(final Create create, final UIService uiService) {
        final com.google.gwt.user.client.ui.Tree tree = new com.google.gwt.user.client.ui.Tree();
        tree.setAnimationEnabled(true);
        init(create, uiService, tree);
    }

    @Override
    public void addHandler(final AddHandler addHandler, final UIService uiService) {

        if (HandlerType.SELECTION_HANDLER.equals(addHandler.getHandlerType())) {
            final com.google.gwt.user.client.ui.Tree tree = cast();
            tree.addSelectionHandler(new SelectionHandler<TreeItem>() {

                @Override
                public void onSelection(final SelectionEvent<TreeItem> event) {
                    PTObject ptObject = uiService.getPTObject(event.getSelectedItem());
                    final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), addHandler.getHandlerType());
                    eventInstruction.setMainPropertyValue(PropertyKey.WIDGET, ptObject.getObjectID());
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
    public com.google.gwt.user.client.ui.Tree cast() {
        return (com.google.gwt.user.client.ui.Tree) uiObject;
    }

}
