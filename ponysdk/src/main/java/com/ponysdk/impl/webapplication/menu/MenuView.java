
package com.ponysdk.impl.webapplication.menu;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.event.HasPSelectionHandlers;

public interface MenuView extends IsPWidget, HasPSelectionHandlers<MenuItem> {

    void addItem(MenuItem item);

    void selectItem(MenuItem item);

}
