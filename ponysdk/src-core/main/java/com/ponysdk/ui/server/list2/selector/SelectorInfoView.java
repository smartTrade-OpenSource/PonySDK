
package com.ponysdk.ui.server.list2.selector;

import com.ponysdk.ui.server.basic.IsPWidget;

public interface SelectorInfoView extends IsPWidget {

    void showAllSelected(int numberOfSelectedItems);

    void showSelectAllOption(int numberOfSelectedItems, int fullSize);

    void showClearSelectionOption(final int numberOfSelectedIntems);

    void hide();

    public void addSelectorInfoViewListener(SelectorInfoViewListener selectorInfoViewListener);

}
