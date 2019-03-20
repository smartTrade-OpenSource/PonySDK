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

package com.ponysdk.core.ui.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.context.UIContextImpl;
import com.ponysdk.core.ui.basic.event.PSelectionEvent;
import com.ponysdk.core.ui.basic.event.PSelectionHandler;

/**
 * A standard hierarchical tree widget. The tree contains a hierarchy of
 * {@link PTreeItem TreeItems} that the user can open, close, and select.
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-Tree</dt>
 * <dd>the tree itself</dd>
 * <dt>.gwt-Tree .gwt-TreeItem</dt>
 * <dd>a tree item</dd>
 * <dt>.gwt-Tree .gwt-TreeItem-selected</dt>
 * <dd>a selected tree item</dd>
 * </dl>
 */
public class PTree extends PWidget implements Iterable<PTreeItem> {

    private final Map<PWidget, PTreeItem> childWidgets = new HashMap<>();

    private final PTreeItem root;
    private boolean animationEnabled = false;
    private PTreeItem selectedItem;

    private List<PSelectionHandler<PTreeItem>> selectionHandlers;

    protected PTree() {
        root = new PTreeItem(this);
        root.saveAdd(root.getID(), ID);
    }

    @Override
    void init0() {
        super.init0();
        root.attach(window, frame);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.TREE;
    }

    public void setAnimationEnabled(final boolean animationEnabled) {
        if (Objects.equals(this.animationEnabled, animationEnabled)) return;
        this.animationEnabled = animationEnabled;
        saveUpdate(ServerToClientModel.ANIMATION, animationEnabled);
    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    public void setSelectedItem(final PTreeItem selectedItem) {
        if (Objects.equals(this.selectedItem, selectedItem)) return;
        this.selectedItem = selectedItem;
        saveUpdate(ServerToClientModel.SELECTED_INDEX, selectedItem != null ? selectedItem.getID() : -1);
        if (selectionHandlers != null) {
            final PSelectionEvent<PTreeItem> selectionEvent = new PSelectionEvent<>(this, selectedItem);
            selectionHandlers.forEach(handler -> handler.onSelection(selectionEvent));
        }
    }

    public PTreeItem getSelectedItem() {
        return selectedItem;
    }

    public PTreeItem add(final String item) {
        return add(Element.newPTreeItem(item));
    }

    public PTreeItem add(final PTreeItem item) {
        return root.add(item);
    }

    public void remove(final PTreeItem item) {
        root.remove(item);
    }

    public PTreeItem get(final int index) {
        return root.get(index);
    }

    @Override
    public Iterator<PTreeItem> iterator() {
        return root.iterator();
    }

    /**
     * Gets the number of items contained at the root of this tree.
     *
     * @return this tree's item count
     */
    public int size() {
        return root.size();
    }

    void orphan(final PWidget child) {
        if (child.getParent() == this) {
            child.setParent(null);
            childWidgets.remove(child);
        } else if (child.getParent() != null) {
            throw new IllegalStateException("Can't adopt an widget attached to another parent");
        }
    }

    void adopt(final PWidget widget, final PTreeItem item) {
        if (!childWidgets.containsKey(widget)) {
            childWidgets.put(widget, item);
            widget.setParent(this);
        } else {
            throw new IllegalStateException("Can't adopt an already widget attached to a parent");
        }
    }

    public void addSelectionHandler(final PSelectionHandler<PTreeItem> handler) {
        if (selectionHandlers == null) selectionHandlers = new ArrayList<>();
        selectionHandlers.add(handler);
    }

    public void removeSelectionHandler(final PSelectionHandler<PTreeItem> handler) {
        if (selectionHandlers != null) selectionHandlers.remove(handler);
    }

    public PTreeItem getRoot() {
        return root;
    }

    public void clear() {
        root.clear();
        saveUpdate(writer -> writer.write(ServerToClientModel.CLEAR));
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (!isVisible()) return;
        if (instruction.containsKey(ClientToServerModel.HANDLER_SELECTION.toStringValue())) {
            final int widgetId = instruction.getJsonNumber(ClientToServerModel.HANDLER_SELECTION.toStringValue()).intValue();
            selectedItem = (PTreeItem) UIContextImpl.get().getObject(widgetId);
            if (selectionHandlers != null) {
                final PSelectionEvent<PTreeItem> selectionEvent = new PSelectionEvent<>(this, selectedItem);
                selectionHandlers.forEach(handler -> handler.onSelection(selectionEvent));
            }
        } else if (instruction.containsKey(ClientToServerModel.HANDLER_OPEN.toStringValue())) {
            final int widgetId = instruction.getJsonNumber(ClientToServerModel.HANDLER_OPEN.toStringValue()).intValue();
            final PTreeItem item = (PTreeItem) UIContextImpl.get().getObject(widgetId);
            item.openState = true;
        } else if (instruction.containsKey(ClientToServerModel.HANDLER_CLOSE.toStringValue())) {
            final int widgetId = instruction.getJsonNumber(ClientToServerModel.HANDLER_CLOSE.toStringValue()).intValue();
            final PTreeItem item = (PTreeItem) UIContextImpl.get().getObject(widgetId);
            item.openState = false;
        } else {
            super.onClientData(instruction);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        root.onDestroy();
    }

    @Override
    protected String dumpDOM() {
        return "<div>" + root.dumpDOM() + "</div>";
    }
}
