
package com.ponysdk.ui.server.list.selector;

public interface Selectable<T> {

    void select();

    void unselect();

    T getSelectedData();

    void addSelectableListener(SelectableListener selectableListener);

}
