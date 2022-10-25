package com.ponysdk.core.ui.listbox;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem;

public interface ListBoxItemRenderer<D> extends IsPWidget {

    void setItem(final ListBoxItem<D> item);

    void addSelectionHandler(final Runnable runnable);

}
