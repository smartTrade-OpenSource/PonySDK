
package com.ponysdk.core.ui.list;

public interface PSelectionModel<T> {

    boolean isSelected(T object);

    void setSelected(T object, boolean selected);

}
