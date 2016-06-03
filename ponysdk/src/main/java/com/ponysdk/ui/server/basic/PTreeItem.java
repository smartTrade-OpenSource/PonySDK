
package com.ponysdk.ui.server.basic;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.core.Parser;
import com.ponysdk.ui.model.ServerToClientModel;
import com.ponysdk.ui.server.model.ServerBinaryModel;
import com.ponysdk.ui.terminal.WidgetType;

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

    private String html = null;

    private final List<PTreeItem> children = new ArrayList<>();

    private boolean selected;

    private boolean open;

    private PWidget widget;

    public PTreeItem() {
    }

    public PTreeItem(final PWidget widget) {
        this.widget = widget;
    }

    public PTreeItem(final String html) {
        this.html = html;
    }

    PTreeItem(final boolean isRoot) {
        this.isRoot = isRoot;
    }

    @Override
    protected boolean attach(final int windowID) {
        final boolean result = super.attach(windowID);
        for (final PTreeItem item : children) {
            item.attach(windowID);
        }
        return result;
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);
        parser.parse(ServerToClientModel.TEXT, html);
        if (isRoot) parser.parse(ServerToClientModel.ROOT, isRoot);
    }

    private void setWidget() {
        if (widget.getParent() != null) {
            widget.removeFromParent();
        }

        if (tree != null) {
            tree.orphan(widget);
            saveRemove(widget.getID(), tree.getID());
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
        this.html = html;
        saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.TEXT, html);
        });
    }

    final void setTree(final PTree tree) {
        this.tree = tree;
        if (isRoot && tree.getWindowID() != PWindow.EMPTY_WINDOW_ID) saveAdd(tree.getID(), ID);
        if (widget != null) setWidget();
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
        item.saveAdd(item.getID(), ID, new ServerBinaryModel(ServerToClientModel.INDEX, beforeIndex));
        item.attach(windowID);
        return item;
    }

    public PTreeItem addItem(final PTreeItem item) {
        children.add(item);
        item.setTree(tree);
        item.saveAdd(item.getID(), ID);
        item.attach(windowID);
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
        saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.SELECTED, selected);
        });
    }

    public boolean isSelected() {
        return selected;
    }

    public void setState(final boolean open) {
        this.open = open;
        saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.STATE, open);
        });
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