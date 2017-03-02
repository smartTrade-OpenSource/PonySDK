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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.event.HasPAnimation;
import com.ponysdk.core.ui.basic.event.HasPSelectionHandlers;
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
public class PTree extends PWidget implements HasPSelectionHandlers<PTreeItem>, HasPAnimation {

    private final List<PSelectionHandler<PTreeItem>> selectionHandlers = new ArrayList<>();
    private final Map<PWidget, PTreeItem> childWidgets = new HashMap<>();

    private final PTreeItem root;
    private boolean animationEnabled = false;
    private PTreeItem curSelection;

    protected PTree() {
        root = new PTreeItem(true);
        root.saveAdd(root.getID(), ID);
    }

    @Override
    protected void init0() {
        super.init0();
        root.setTree(this);
        root.attach(windowID);
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

    void orphan(final PWidget child) {
        if (child.getParent() == this) {
            child.setParent(null);
            childWidgets.remove(child);
        } else {
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

    @Override
    public void addSelectionHandler(final PSelectionHandler<PTreeItem> handler) {
        selectionHandlers.add(handler);
        saveAddHandler(HandlerModel.HANDLER_SELECTION);
    }

    @Override
    public void removeSelectionHandler(final PSelectionHandler<PTreeItem> handler) {
        selectionHandlers.remove(handler);
        saveRemoveHandler(HandlerModel.HANDLER_SELECTION);
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (instruction.containsKey(ClientToServerModel.HANDLER_SELECTION.toStringValue())) {
            final int widgetId = instruction.getJsonNumber(ClientToServerModel.HANDLER_SELECTION.toStringValue()).intValue();
            final PSelectionEvent<PTreeItem> selectionEvent = new PSelectionEvent<>(this, UIContext.get().getObject(widgetId));
            for (final PSelectionHandler<PTreeItem> handler : selectionHandlers) {
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
        if (Objects.equals(this.animationEnabled, animationEnabled)) return;
        this.animationEnabled = animationEnabled;
        saveUpdate((writer) -> writer.writeModel(ServerToClientModel.ANIMATION, animationEnabled));
    }

    @Override
    public void destroy() {
        super.destroy();
        root.destroy();
    }
}
