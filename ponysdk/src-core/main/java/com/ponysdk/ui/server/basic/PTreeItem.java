
package com.ponysdk.ui.server.basic;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.core.Parser;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

/**
 * An item that can be contained within a {@link PTree}. Each tree item is
 * assigned a unique DOM id in order to support ARIA.
 * <p>
 * <h3>Example</h3> {@example http://ponysdk.com/sample/#Tree}
 * </p>
 */
public class PTreeItem extends PObject {

    private PTree tree;

    private boolean isRoot = false;

    private String html;

    private final List<PTreeItem> children = new ArrayList<>();

    private boolean selected;

    private boolean open;

    private PWidget widget;

    PTreeItem(final boolean isRoot, final String html) {
        this.isRoot = isRoot;
        this.html = html;
        init();
    }

    PTreeItem(final boolean isRoot) {
        this(isRoot, null);
    }

    public PTreeItem() {
        this(false, null);
    }

    public PTreeItem(final String html) {
        this(false, html);
    }

    public PTreeItem(final PWidget widget) {
        this();
        this.widget = widget;
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);
        parser.parse(ServerToClientModel.TEXT, html);
        if (isRoot) parser.parse(ServerToClientModel.ROOT, isRoot);
    }

    private void setWidget() {
        if (widget == null)
            return;

        if (widget.getParent() != null) {
            widget.removeFromParent();
        }

        if (tree != null) {
            tree.orphan(widget);
            saveRemove(widget.getID(), tree.getID());
        }

        if (tree != null) {
            tree.adopt(widget, this);
            saveAdd(widget.getID(), ID, ServerToClientModel.WIDGET);
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
        this.html = html;
        saveUpdate(ServerToClientModel.TEXT, html);
    }

    final void setTree(final PTree tree) {
        this.tree = tree;
        if (isRoot) {
            saveAdd(tree.getID(), ID);
        }
        setWidget();
    }

    public PTree getTree() {
        return tree;
    }

    public int getChildCount() {
        return children.size();
    }

    public PTreeItem insertItem(final int beforeIndex, final PTreeItem item) {
        children.add(beforeIndex, item);
        item.setTree(tree);
        saveAdd(item.getID(), ID, ServerToClientModel.INDEX, beforeIndex);
        return item;
    }

    public PTreeItem addItem(final PTreeItem item) {
        children.add(item);
        item.setTree(tree);
        saveAdd(item.getID(), ID);
        return item;
    }

    public PTreeItem addItem(final String itemHtml) {
        return addItem(new PTreeItem(itemHtml));
    }

    public boolean removeItem(final PTreeItem item) {
        saveRemove(tree.getID(), ID);
        return children.remove(item);
    }

    public void setSelected(final boolean selected) {
        this.selected = selected;
        saveUpdate(ServerToClientModel.SELECTED, selected);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setState(final boolean open) {
        this.open = open;
        saveUpdate(ServerToClientModel.STATE, open);
    }

    public boolean getState() {
        return open;
    }

    public PTreeItem getChild(final int index) {
        return children.get(index);
    }

    public PWidget getWidget() {
        return widget;
    }

}