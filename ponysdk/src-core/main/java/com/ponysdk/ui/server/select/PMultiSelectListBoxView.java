
package com.ponysdk.ui.server.select;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.event.HasPBlurHandlers;
import com.ponysdk.ui.server.basic.event.HasPFocusHandlers;
import com.ponysdk.ui.server.basic.event.HasPKeyUpHandlers;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public interface PMultiSelectListBoxView extends IsPWidget, HasPKeyUpHandlers, HasPFocusHandlers, HasPBlurHandlers {

    void addItem(String item);

    void unSelectItem(String item);

    void selectItem(String item);

    void addShownItemClickHandler(String item, PClickHandler clickHandler);

    void addSelectedClickHandler(String item, PClickHandler clickHandler);

    void focusSelectedItem(String item);

    void blurSelectedItem(String item);

}
