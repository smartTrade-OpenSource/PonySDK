
package com.ponysdk.impl.webapplication.menu;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.event.HasPSelectionHandlers;

public interface MenuView extends IsPWidget, HasPSelectionHandlers<MenuItem> {

    void addItem(MenuItem item);

    void selectItem(MenuItem item);

}
