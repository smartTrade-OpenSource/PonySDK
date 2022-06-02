package com.ponysdk.core.ui.listbox;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PWidget.TabindexMode;
import com.ponysdk.core.ui.basic.event.PKeyDownEvent;
import com.ponysdk.core.ui.basic.event.PKeyUpHandler;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;

public interface ListBoxTextBox extends IsPWidget {

	void setPlaceholder(String placeholder);
	void setTabindex(TabindexMode tabulable);
    HandlerRegistration addKeyDownHandler(final PKeyDownEvent.Handler handler);
    HandlerRegistration addKeyUpHandler(final PKeyUpHandler handler);
	void setText(String  text);
	void focus();
	String getText();
}
