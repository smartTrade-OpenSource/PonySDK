
package com.ponysdk.impl.webapplication.menu;

import java.util.Collection;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.event.HasPSelectionHandlers;

public interface MenuView extends IsPWidget, HasPSelectionHandlers<String> {

    void addItem(Collection<String> categories, String caption);

    void selectItem(Collection<String> categories, String caption);

}
