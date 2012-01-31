
package com.ponysdk.ui.server.select;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public interface PMultiSelectListBoxView extends IsPWidget {

    void addItem(String item);

    void unSelectItem(String item);

    void selectItem(String item);

    void addShownItemClickHandler(String item, PClickHandler clickHandler);

    void addSelectedClickHandler(String item, PClickHandler clickHandler);

}
