
package com.ponysdk.ui.server.list2.selector;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.list.SelectionMode;

public interface SelectorView extends IsPWidget {

    public void addSelectorViewListener(SelectorViewListener selectorViewListener);

    public void update(SelectionMode selectionMode, int numberOfSelectedItems, int fullSize, int pageSize);

}
