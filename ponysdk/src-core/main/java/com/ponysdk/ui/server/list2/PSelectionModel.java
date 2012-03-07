
package com.ponysdk.ui.server.list2;

public interface PSelectionModel<T> {

    boolean isSelected(T object);

    void setSelected(T object, boolean selected);

}
