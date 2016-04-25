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

import javax.json.JsonObject;

import com.ponysdk.core.UIContext;
import com.ponysdk.ui.server.basic.event.HasPAnimation;
import com.ponysdk.ui.server.basic.event.HasPSelectionHandlers;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

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
 * <p>
 * <h3>Example</h3> {@example http://ponysdk.com/sample/#Tree}
 * </p>
 */
public class PTree extends PWidget implements HasPSelectionHandlers<PTreeItem>, HasPAnimation {

    private final List<PSelectionHandler<PTreeItem>> selectionHandlers = new ArrayList<>();

    private final Map<PWidget, PTreeItem> childWidgets = new HashMap<>();

    private boolean animationEnabled = false;
    private final PTreeItem root;

    private PTreeItem curSelection;

    public PTree() {
        init();
        root = new PTreeItem(true);
        root.setTree(this);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.TREE;
    }

    public PTreeItem addItem(final String item) {
        return root.addItem(item);
    }

    public PTreeItem addItem(final PTreeItem item) {
        return root.addItem(item);
    }

    public void removeItem(final PTreeItem item) {
        root.removeItem(item);
    }

    public PTreeItem getSelectedItem() {
        return curSelection;
    }

    public void setSelectedItem(final PTreeItem item) {
        if (curSelection != null) {
            curSelection.setSelected(false);
        }
        curSelection = item;
        curSelection.setSelected(true);
    }

    public PTreeItem getItem(final int index) {
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

    void orphan(final PWidget widget) {
        assert widget.getParent() == this;
        widget.setParent(null);
        childWidgets.remove(widget);
    }

    void adopt(final PWidget widget, final PTreeItem item) {
        assert !childWidgets.containsKey(widget);
        childWidgets.put(widget, item);
        widget.setParent(this);
    }

    @Override
    public void addSelectionHandler(final PSelectionHandler<PTreeItem> handler) {
        selectionHandlers.add(handler);
        saveAddHandler(HandlerModel.HANDLER_SELECTION_HANDLER);
    }

    @Override
    public void removeSelectionHandler(final PSelectionHandler<PTreeItem> handler) {
        selectionHandlers.remove(handler);
        saveRemoveHandler(HandlerModel.HANDLER_SELECTION_HANDLER);
    }

    @Override
    public List<PSelectionHandler<PTreeItem>> getSelectionHandlers() {
        return selectionHandlers;
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        final String handlerSelection = ClientToServerModel.HANDLER_SELECTION_HANDLER.toStringValue();
        if (instruction.containsKey(handlerSelection)) {
            final PTreeItem treeItem = UIContext.get().getObject(instruction.getJsonNumber(handlerSelection).intValue());
            final PSelectionEvent<PTreeItem> selectionEvent = new PSelectionEvent<>(this, treeItem);
            for (final PSelectionHandler<PTreeItem> handler : getSelectionHandlers()) {
                handler.onSelection(selectionEvent);
            }
        } else {
            super.onClientData(instruction);
        }
    }

    public PTreeItem getRoot() {
        return root;
    }

    @Override
    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    @Override
    public void setAnimationEnabled(final boolean animationEnabled) {
        this.animationEnabled = animationEnabled;
        saveUpdate(ServerToClientModel.ANIMATION, animationEnabled);
    }
}
