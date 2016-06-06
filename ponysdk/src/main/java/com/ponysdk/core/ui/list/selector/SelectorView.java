
package com.ponysdk.core.ui.list.selector;

import com.ponysdk.core.ui.basic.IsPWidget;

public interface SelectorView extends IsPWidget {

    void addSelectorViewListener(SelectorViewListener selectorViewListener);

    void update(SelectionMode selectionMode, int numberOfSelectedItems, int fullSize, int pageSize);

}
