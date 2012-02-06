
package com.ponysdk.impl.webapplication.menu;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;

public interface MenuView extends IsPWidget {

    void addCategory(String category);

    void addItem(String category, String caption);

    void addSelectionHandler(PSelectionHandler<String> handler);

    void selectItem(String category, String caption);

}
