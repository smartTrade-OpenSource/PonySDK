/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.model.ServerBinaryModel;
import com.ponysdk.core.writer.ModelWriter;

/**
 * An item that can be contained within a {@link PTree}. Each tree item is assigned a unique DOM id
 * in order to support ARIA.
 */
public class PTreeItem extends PObject implements Iterable<PTreeItem> {

    private PTree tree;
    private PTreeItem parent;
    private List<PTreeItem> children;

    private boolean isRoot;
    private String text;
    protected boolean openState;
    private PWidget widget;

    protected PTreeItem() {
    }

    protected PTreeItem(final PWidget widget) {
        this.widget = widget;
    }

    protected PTreeItem(final String text) {
        this.text = text;
    }

    PTreeItem(final PTree tree) {
        this.isRoot = true;
        this.tree = tree;
    }

    @Override
    void init0() {
        super.init0();
        forEach(child -> {
            child.setTree(tree);
            child.attach(window, frame);
        });

        if (widget != null) {
            if (widget.getParent() != null) widget.removeFromParent();

            if (tree != null) {
                tree.orphan(widget);
                widget.saveRemove(widget.getID(), tree.getID());
            }

            if (tree != null) {
                tree.adopt(widget, this);
                widget.attach(window, frame);
                saveAdd(widget.getID(), ID, new ServerBinaryModel(ServerToClientModel.WIDGET, null));
            }
        }
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
        if (this.text != null) writer.write(ServerToClientModel.TEXT, text);
        if (this.isRoot) writer.write(ServerToClientModel.TREE_ROOT, tree.getID());
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.TREE_ITEM;
    }

    public PWidget getWidget() {
        return widget;
    }

    public void setText(final String text) {
        if (Objects.equals(this.text, text)) return;
        this.text = text;
        if (initialized) saveUpdate(ServerToClientModel.TEXT, text);
    }

    private void setTree(final PTree tree) {
        this.tree = tree;
    }

    public void setSelected(final boolean newSelected) {
        final boolean selected = isSelected();
        if (!selected && newSelected) tree.setSelectedItem(this);
        else if (selected && !newSelected) tree.setSelectedItem(null);
    }

    public boolean isSelected() {
        return this.equals(tree.getSelectedItem());
    }

    public void setState(final boolean openState) {
        if (Objects.equals(this.openState, openState)) return;
        this.openState = openState;
        if (openState) saveUpdate(ServerToClientModel.OPEN, openState);
        else saveUpdate(ServerToClientModel.CLOSE, openState);
    }

    public boolean getState() {
        return openState;
    }

    public PTreeItem add(final int beforeIndex, final PTreeItem item) {
        if (children == null) children = new ArrayList<>();
        children.add(beforeIndex, item);
        item.setParent(this);
        item.setTree(tree);
        if (isInitialized()) item.attach(window, frame);

        if (!isInitialized()) item.saveAdd(item.getID(), ID);
        else item.saveAdd(item.getID(), ID, new ServerBinaryModel(ServerToClientModel.INDEX, beforeIndex));

        return item;
    }

    public PTreeItem add(final PTreeItem item) {
        if (children == null) children = new ArrayList<>();
        children.add(item);
        item.setParent(this);
        item.setTree(tree);
        if (isInitialized()) item.attach(window, frame);
        item.saveAdd(item.getID(), ID);
        return item;
    }

    public PTreeItem add(final String itemHtml) {
        return add(Element.newPTreeItem(itemHtml));
    }

    public boolean remove(final PTreeItem item) {
        item.saveRemove(item.getID(), ID);
        return children.remove(item);
    }

    public PTreeItem get(final int index) {
        return children != null ? children.get(index) : null;
    }

    @Override
    public Iterator<PTreeItem> iterator() {
        return children != null ? children.iterator() : Collections.emptyIterator();
    }

    public int size() {
        return children != null ? children.size() : 0;
    }

    void setParent(final PTreeItem pTreeItem) {
        this.parent = pTreeItem;
    }

    public void removeFromParent() {
        if (!isRoot) parent.remove(this);
        else tree.removeFromParent();
    }

    void clear() {
        children.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        forEach(PObject::onDestroy);
    }

}
