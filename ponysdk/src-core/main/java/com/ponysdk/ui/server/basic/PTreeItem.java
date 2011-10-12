package com.ponysdk.ui.server.basic;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.Remove;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTreeItem {
    private static final String DOT = ".";

    private PTreeItem parent;
    private final List<PTreeItem> children = new ArrayList<PTreeItem>();
    private PWidget widget;
    private PTree tree;
    private boolean selected;
    private int position;
    private boolean visible = true;

    public PTreeItem() {
    }

    public PTreeItem(PWidget widget) {
        setWidget(widget);
    }

    public PTreeItem(String item) {
        setWidget(new PLabel(item));
    }

    public PTreeItem addItem(PTreeItem item) {
        maybeRemoveItemFromParent(item);
        insertItem(getChildCount(), item);
        return item;
    }

    public PTreeItem addItem(String item) {
        final PTreeItem treeItem = new PTreeItem(new PLabel(item));
        return addItem(treeItem);
    }

    void maybeRemoveItemFromParent(PTreeItem item) {
        if ((item.getParentItem() != null) || (item.getTree() != null)) {
            item.remove();
        }
    }

    public void remove() {
        if (parent != null) {
            parent.removeItem(this);
        } else if (tree != null) {
            tree.removeItem(this);
        }
    }

    public void insertItem(int beforeIndex, PTreeItem item) throws IndexOutOfBoundsException {
        maybeRemoveItemFromParent(item);

        final int childCount = getChildCount();
        if (beforeIndex < 0 || beforeIndex > childCount) {
            throw new IndexOutOfBoundsException();
        }

        item.setParentItem(this);
        children.add(beforeIndex, item);

        for (int i = beforeIndex; i < children.size(); i++) {
            children.get(i).position = i;
        }

        item.setTree(tree);
    }

    public PTreeItem getParentItem() {
        return parent;
    }

    public void setParentItem(PTreeItem parent) {
        this.parent = parent;
    }

    public final PTree getTree() {
        return tree;
    }

    public void setWidget(PWidget newWidget) {
        if (newWidget.getParent() != null) {
            newWidget.removeFromParent();
        }

        if (widget != null) {
            if (tree != null) {
                tree.orphan(widget);
                final Remove remove = new Remove(widget.getID(), tree.getID());
                widget.getPonySession().stackInstruction(remove);
            }
        }

        widget = newWidget;

        if (newWidget != null) {
            if (tree != null) {
                tree.adopt(widget, this);
                final Add add = new Add(widget.getID(), tree.getID());
                add.getMainProperty().setProperty(PropertyKey.TREE_ITEM_POSITION_PATH, buildPositionPath());
                widget.getPonySession().stackInstruction(add);
            }
        }
    }

    public String buildPositionPath() {
        PTreeItem parent = this.parent;
        String path = position + "";

        while (parent != null) {
            path = parent.position + DOT + path;
            parent = parent.parent;
        }
        tree.updateTreeItemPosition(path, this);
        return path;
    }

    public PTreeItem getChild(int index) {
        if ((index < 0) || (index >= children.size())) {
            return null;
        }

        return children.get(index);
    }

    public int getChildCount() {
        return children.size();
    }

    public int getChildIndex(PTreeItem child) {
        return children.indexOf(child);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void removeItem(PTreeItem item) {
        if (children.remove(item)) {
            for (int i = 0; i < children.size(); i++) {
                children.get(i).position = i;
            }
            item.setTree(null);
            item.setParentItem(null);
        }
    }

    int getPosition() {
        return position;
    }

    void setPosition(int position) {
        this.position = position;
    }

    void setTree(PTree newTree) {
        if (tree == newTree) {
            return;
        }

        if (tree != null) {
            if (tree.getSelectedItem() == this) {
                tree.setSelectedItem(null);
            }

            if (widget != null) {
                tree.orphan(widget);
                final Remove remove = new Remove(widget.getID(), newTree.getID());
                widget.getPonySession().stackInstruction(remove);
            }
        }

        tree = newTree;
        for (final PTreeItem item : children) {
            item.setTree(newTree);
        }

        if (newTree != null) {
            if (widget != null) {
                newTree.adopt(widget, this);
                final Add add = new Add(widget.getID(), newTree.getID());
                add.getMainProperty().setProperty(PropertyKey.TREE_ITEM_POSITION_PATH, buildPositionPath());
                widget.getPonySession().stackInstruction(add);
            }
        }
    }

    public String getText() {
        if (widget instanceof PLabel) {
            return ((PLabel) widget).getText();
        }
        return "N/A";
    }

    public PWidget getWidget() {
        return widget;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        final Update update = new Update(widget.getID(), tree.getID());

        final Property property = new Property(PropertyKey.WIDGET_VISIBLE, visible);
        property.setProperty(PropertyKey.TREE_ITEM_POSITION_PATH, buildPositionPath());

        update.setMainProperty(property);

        widget.getPonySession().stackInstruction(update);

    }

    public boolean isVisible() {
        return visible;
    }

}