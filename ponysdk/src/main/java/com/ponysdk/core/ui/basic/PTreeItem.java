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
public class PTreeItem extends PObject {

    private final List<PTreeItem> children = new ArrayList<>();

    private PTree tree;
    private boolean isRoot = false;
    private String html = null;
    private boolean selected;

    private boolean open;

    private PWidget widget;

    protected PTreeItem() {
    }

    protected PTreeItem(final PWidget widget) {
        this.widget = widget;
    }

    protected PTreeItem(final String html) {
        this.html = html;
    }

    PTreeItem(final boolean isRoot) {
        this.isRoot = isRoot;
    }

    @Override
    protected void init0() {
        super.init0();
        for (final PTreeItem item : children) {
            item.attach(window);
        }
    }

    @Override
    protected void enrichOnInit(final ModelWriter writer) {
        super.enrichOnInit(writer);
        writer.write(ServerToClientModel.TEXT, html);
        if (isRoot) writer.write(ServerToClientModel.ROOT, isRoot);
    }

    private void setWidget() {
        if (widget.getParent() != null) {
            widget.removeFromParent();
        }

        if (tree != null) {
            tree.orphan(widget);
            widget.saveRemove(widget.getID(), tree.getID());
        }

        if (tree != null) {
            tree.adopt(widget, this);
            saveAdd(widget.getID(), ID, new ServerBinaryModel(ServerToClientModel.WIDGET, null));
        }
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.TREE_ITEM;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public String getHtml() {
        return html;
    }

    public void setHTML(final String html) {
        if (Objects.equals(this.html, html)) return;
        this.html = html;
        saveUpdate(writer -> writer.write(ServerToClientModel.TEXT, html));
    }

    public PTree getTree() {
        return tree;
    }

    final void setTree(final PTree tree) {
        this.tree = tree;
        if (isRoot && tree.getWindow() != null) tree.saveAdd(tree.getID(), ID);
        if (widget != null) setWidget();
    }

    public int getChildCount() {
        return children.size();
    }

    public PTreeItem insertItem(final int beforeIndex, final PTreeItem item) {
        children.add(beforeIndex, item);
        item.setTree(tree);
        item.saveAdd(item.getID(), ID, new ServerBinaryModel(ServerToClientModel.INDEX, beforeIndex));
        item.attach(window);
        return item;
    }

    public PTreeItem addItem(final PTreeItem item) {
        children.add(item);
        item.setTree(tree);
        item.saveAdd(item.getID(), ID);
        item.attach(window);
        return item;
    }

    public PTreeItem addItem(final String itemHtml) {
        return addItem(new PTreeItem(itemHtml));
    }

    public boolean removeItem(final PTreeItem item) {
        tree.saveRemove(tree.getID(), ID);
        return children.remove(item);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(final boolean selected) {
        if (Objects.equals(this.selected, selected)) return;
        this.selected = selected;
        saveUpdate(writer -> writer.write(ServerToClientModel.SELECTED, selected));
    }

    public boolean getState() {
        return open;
    }

    public void setState(final boolean open) {
        if (Objects.equals(this.open, open)) return;
        this.open = open;
        saveUpdate(writer -> writer.write(ServerToClientModel.STATE, open));
    }

    public PTreeItem getChild(final int index) {
        return children.get(index);
    }

    public PWidget getWidget() {
        return widget;
    }

    @Override
    public void destroy() {
        super.destroy();
        children.forEach(PObject::destroy);
    }

}
