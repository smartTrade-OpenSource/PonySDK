
package com.ponysdk.ui.server.list.selector;

import com.ponysdk.ui.server.basic.IsPWidget;

public interface SelectorView extends IsPWidget {

    public void addSelectorViewListener(SelectorViewListener selectorViewListener);

    public void update(SelectionMode selectionMode, int numberOfSelectedItems, int fullSize, int pageSize);

}
