
package com.ponysdk.core.ui.list;

import java.util.List;

public interface HasPData<T> {

    PSelectionModel<T> getSelectionModel();

    T getVisibleItem(int indexOnPage);

    int getVisibleItemCount();

    Iterable<T> getVisibleItems();

    void setData(List<T> values);

    void setSelectionModel(PSelectionModel<T> selectionModel);

}
