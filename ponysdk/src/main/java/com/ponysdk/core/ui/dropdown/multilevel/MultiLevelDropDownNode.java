
package com.ponysdk.core.ui.dropdown.multilevel;

import java.util.Collection;

import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem;

public class MultiLevelDropDownNode<D> {

    private final Collection<ListBoxItem<MultiLevelDropDownNode<D>>> items;
    private final D value;

    /**
     * Create a MultiLevelDropDownNode which will behave as a final node value.
     *
     * @param value
     *            the value wrapped by the node.
     */
    public MultiLevelDropDownNode(final D value) {
        this.items = null;
        this.value = value;
    }

    /**
     * Create a MultiLevelDropDownNode which will behave as intermediate node and which may contains leaves or other
     * nodes.
     *
     * @param items
     *            a collection of items wrapped by the node.
     */
    public MultiLevelDropDownNode(final Collection<ListBoxItem<MultiLevelDropDownNode<D>>> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("MultiLevelDropDownNode can't be empty when behaving as a node");
        this.items = items;
        this.value = null;
    }

    public Collection<ListBoxItem<MultiLevelDropDownNode<D>>> getItems() {
        return items;
    }

    public D getValue() {
        return value;
    }

    public boolean isLeaf() {
        return value != null;
    }

}
