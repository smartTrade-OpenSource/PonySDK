package com.ponysdk.core.ui.listbox;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem;

public class LabelListBoxItemRenderer<D> implements ListBoxItemRenderer<D> {

    private static final String DISABLED = "disabled";

    private final PLabel label;

    public LabelListBoxItemRenderer() {
        this.label = Element.newPLabel();
    }

    @Override
    public void setItem(final ListBoxItem<D> item) {
        label.setText(item.getLabel());
        label.setTitle(item.getLabel());
        if (item.isEnabled()) label.removeAttribute(DISABLED);
        else label.setAttribute(DISABLED);
    }

    @Override
    public void addSelectionHandler(final Runnable runnable) {
        label.addClickHandler(e -> runnable.run());
    }

    @Override
    public PWidget asWidget() {
        return label;
    }
}