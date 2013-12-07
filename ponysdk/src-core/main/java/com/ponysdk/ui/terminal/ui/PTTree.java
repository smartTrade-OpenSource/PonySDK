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
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTTree extends PTWidget<Tree> {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        final Tree tree = new Tree();
        init(create, uiService, tree);
    }

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {
        final String handler = addHandler.getString(HANDLER.KEY);

        if (HANDLER.KEY_.SELECTION_HANDLER.equals(handler)) {
            uiObject.addSelectionHandler(new SelectionHandler<TreeItem>() {

                @Override
                public void onSelection(final SelectionEvent<TreeItem> event) {
                    final PTObject ptObject = uiService.getPTObject(event.getSelectedItem());

                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(addHandler.getObjectID());
                    eventInstruction.put(TYPE.KEY_.EVENT, TYPE.KEY_.EVENT);
                    eventInstruction.put(HANDLER.KEY, HANDLER.KEY_.SELECTION_HANDLER);
                    eventInstruction.put(PROPERTY.WIDGET, ptObject.getObjectID());
                    uiService.sendDataToServer(eventInstruction);
                }
            });
        } else {
            super.addHandler(addHandler, uiService);
        }

    }

    @Override
    public void remove(final PTInstruction remove, final UIService uiService) {
        uiObject.remove(asWidget(remove.getObjectID(), uiService));
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.ANIMATION)) {
            uiObject.setAnimationEnabled(update.getBoolean(PROPERTY.ANIMATION));
        } else {
            super.update(update, uiService);
        }
    }
}
