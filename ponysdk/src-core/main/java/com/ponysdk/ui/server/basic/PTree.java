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
package com.ponysdk.ui.server.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.ui.server.basic.event.HasPSelectionHandlers;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.EventInstruction;

public class PTree extends PWidget implements HasPSelectionHandlers<PTreeItem> {

    private final List<PSelectionHandler<PTreeItem>> selectionHandlers = new ArrayList<PSelectionHandler<PTreeItem>>();

    private final Map<PWidget, PTreeItem> childWidgets = new HashMap<PWidget, PTreeItem>();
    private final Map<String, PTreeItem> childByPath = new HashMap<String, PTreeItem>();

    private final String ROOT_PATH = "0";

    private final PTreeItem root = new PTreeItem(new PLabel("root"));
    private PTreeItem curSelection;

    public PTree() {
        root.setTree(this);
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.TREE;
    }

    public PTreeItem addItem(String item) {
        return root.addItem(item);
    }

    public PTreeItem addItem(PTreeItem item) {
        return root.addItem(item);
    }

    public void removeItem(PTreeItem item) {
        root.removeItem(item);
    }

    public PTreeItem getSelectedItem() {
        return curSelection;
    }

    public void setSelectedItem(PTreeItem item) {
        if (curSelection != null) {
            curSelection.setSelected(false);
        }
        curSelection = item;
        curSelection.setSelected(true);
    }

    public PTreeItem getItem(int index) {
        return root.getChild(index);
    }

    /**
     * Gets the number of items contained at the root of this tree.
     * 
     * @return this tree's item count
     */
    public int getItemCount() {
        return root.getChildCount();
    }

    void orphan(PWidget widget) {
        assert (widget.getParent() == this);
        widget.setParent(null);
        childWidgets.remove(widget);
    }

    void adopt(PWidget widget, PTreeItem item) {
        assert (!childWidgets.containsKey(widget));
        childWidgets.put(widget, item);
        widget.setParent(this);
    }

    @Override
    public void addSelectionHandler(PSelectionHandler<PTreeItem> handler) {
        selectionHandlers.add(handler);
        final AddHandler addHandler = new AddHandler(getID(), HandlerType.SELECTION_HANDLER);
        getPonySession().stackInstruction(addHandler);
    }

    @Override
    public void removeSelectionHandler(PSelectionHandler<PTreeItem> handler) {
        selectionHandlers.remove(handler);
    }

    @Override
    public List<PSelectionHandler<PTreeItem>> getSelectionHandlers() {
        return selectionHandlers;
    }

    @Override
    public void onEventInstruction(EventInstruction event) {
        if (HandlerType.SELECTION_HANDLER.equals(event.getHandlerType())) {
            for (final PSelectionHandler<PTreeItem> handler : getSelectionHandlers()) {
                final PSelectionEvent<PTreeItem> selectionEvent = new PSelectionEvent<PTreeItem>();
                selectionEvent.setSelectedItem(getItemByPath(event.getMainProperty().getValue()));
                handler.onSelection(selectionEvent);
            }
        } else {
            super.onEventInstruction(event);
        }
    }

    public PTreeItem getItemByPath(String data) {
        if (ROOT_PATH.equals(data)) {
            return root;
        }
        return childByPath.get(data);
    }

    public void updateTreeItemPosition(String path, PTreeItem item) {
        childByPath.put(path, item);
    }

    public PTreeItem getRoot() {
        return root;
    }
}
