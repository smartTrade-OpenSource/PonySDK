
package com.ponysdk.ui.server.list2.selector;

public interface Selectable<T> {

    void select();

    void unselect();

    T getSelectedData();

}
