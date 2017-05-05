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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.model.ServerBinaryModel;
import com.ponysdk.core.writer.ModelWriter;

/**
 * An item that can be contained within a {@link PTree}. Each tree item is
 * assigned a unique DOM id in order to support ARIA.
 */
public class PTreeItem extends PObject implements Iterable<PTreeItem> {

    private final List<PTreeItem> children = new ArrayList<>();

    private PTree tree;
    private boolean isRoot;
    private String text;
    private boolean selected;

    private boolean open;

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
    protected void init0() {
        super.init0();
        forEach(child -> {
            child.setTree(tree);
            child.attach(window);
        });

        if (widget != null) {
            if (widget.getParent() != null) widget.removeFromParent();

            if (tree != null) {
                tree.orphan(widget);
                widget.saveRemove(widget.getID(), tree.getID());
            }

            if (tree != null) {
                tree.adopt(widget, this);
                widget.attach(window);
                saveAdd(widget.getID(), ID, new ServerBinaryModel(ServerToClientModel.WIDGET, null));
            }
        }
    }

    @Override
    protected void enrichOnInit(final ModelWriter writer) {
        super.enrichOnInit(writer);
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
        saveUpdate(writer -> writer.write(ServerToClientModel.TEXT, text));
    }

    private void setTree(final PTree tree) {
        this.tree = tree;
    }

    public void setSelected(final boolean selected) {
        if (Objects.equals(this.selected, selected)) return;
        this.selected = selected;
        saveUpdate(writer -> writer.write(ServerToClientModel.SELECTED, selected));
    }

    public boolean isSelected() {
        return selected;
    }

    public void setState(final boolean open) {
        if (Objects.equals(this.open, open)) return;
        this.open = open;
        saveUpdate(writer -> writer.write(ServerToClientModel.STATE, open));
    }

    public boolean getState() {
        return open;
    }

    public PTreeItem add(final int beforeIndex, final PTreeItem item) {
        children.add(beforeIndex, item);
        item.setTree(tree);
        if (isInitialized()) item.attach(window);
        item.saveAdd(item.getID(), ID, new ServerBinaryModel(ServerToClientModel.INDEX, beforeIndex));
        return item;
    }

    public PTreeItem add(final PTreeItem item) {
        children.add(item);
        item.setTree(tree);
        if (isInitialized()) item.attach(window);
        item.saveAdd(item.getID(), ID);
        return item;
    }

    public PTreeItem add(final String itemHtml) {
        return add(Element.newPTreeItem(itemHtml));
    }

    public boolean remove(final PTreeItem item) {
        tree.saveRemove(tree.getID(), ID);
        return children.remove(item);
    }

    public PTreeItem get(final int index) {
        return children.get(index);
    }

    @Override
    public Iterator<PTreeItem> iterator() {
        return children.iterator();
    }

    public int size() {
        return children.size();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        forEach(PObject::onDestroy);
    }

}
