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
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Remove;

public class PTTree extends PTWidget {

    @Override
    public void create(Create create, UIService uiService) {
        final com.google.gwt.user.client.ui.Tree tree = new com.google.gwt.user.client.ui.Tree();
        tree.setAnimationEnabled(true);
        init(tree);
    }

    @Override
    public void add(Add add, UIService uiService) {

        final com.google.gwt.user.client.ui.Widget w = asWidget(add.getObjectID(), uiService);
        final com.google.gwt.user.client.ui.Tree tree = cast();
        final String positionPath = add.getMainProperty().getStringProperty(PropertyKey.TREE_ITEM_POSITION_PATH);
        final String[] positions = positionPath.split("\\.");
        TreeItem item = null;
        for (int i = 1; i <= positions.length - 2; i++) {
            if (item == null)
                item = tree.getItem(Integer.parseInt(positions[i]));
            else
                item = item.getChild(Integer.parseInt(positions[i]));
        }

        if (positionPath.equals("0"))
            return; // root

        if (item == null) {
            item = new TreeItem(w);
            tree.addItem(item);
        } else {
            item.addItem(new TreeItem(w));
        }

    }

    @Override
    public void addHandler(final AddHandler addHandler, final UIService uiService) {

        if (HandlerType.SELECTION_HANDLER.equals(addHandler.getType())) {
            final com.google.gwt.user.client.ui.Tree tree = cast();
            tree.addSelectionHandler(new SelectionHandler<TreeItem>() {
                @Override
                public void onSelection(SelectionEvent<TreeItem> event) {

                    String path = "0";
                    String childPath = "";
                    final TreeItem selectedTreeItem = event.getSelectedItem();
                    TreeItem item = selectedTreeItem;
                    while (item != null) {
                        final TreeItem parentItem = item.getParentItem();
                        if (parentItem != null) {
                            final int childIndex = parentItem.getChildIndex(item);
                            if (childPath.isEmpty()) {
                                childPath = String.valueOf(childIndex);
                            } else {
                                childPath = childIndex + "." + childPath;
                            }
                        } else {
                            // temp not optimal?
                            for (int i = 0; i < tree.getItemCount(); i++) {
                                if (tree.getItem(i).equals(item)) {
                                    if (childPath.isEmpty())
                                        childPath = i + "";
                                    else
                                        childPath = i + "." + childPath;
                                    break;
                                }
                            }
                        }
                        item = parentItem;
                    }
                    final int ROOT = 0;
                    if (!childPath.isEmpty()) {
                        path = ROOT + "." + childPath;
                    }
                    final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), addHandler.getType());
                    eventInstruction.setMainPropertyValue(PropertyKey.VALUE, path);
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
    public com.google.gwt.user.client.ui.Tree cast() {
        return (com.google.gwt.user.client.ui.Tree) uiObject;
    }

}
