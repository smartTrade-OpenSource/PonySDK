
package com.ponysdk.impl.webapplication.menu;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.event.HasPSelectionHandlers;

public interface MenuView extends IsPWidget, HasPSelectionHandlers<String> {

    void addCategory(String category);

    void addItem(String category, String caption);

    void selectItem(String category, String caption);

}
