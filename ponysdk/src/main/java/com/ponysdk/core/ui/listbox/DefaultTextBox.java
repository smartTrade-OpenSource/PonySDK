package com.ponysdk.core.ui.listbox;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWidget.TabindexMode;
import com.ponysdk.core.ui.basic.event.PKeyDownEvent.Handler;
import com.ponysdk.core.ui.basic.event.PKeyUpHandler;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;

public class DefaultTextBox implements ListBoxTextBox {
	private final PTextBox textBox = Element.newPTextBox();
	
	@Override
	public PWidget asWidget() {
		return textBox.asWidget();
	}

	@Override
	public void setPlaceholder(String placeholder) {
		textBox.setPlaceholder(placeholder);
	}

	@Override
	public void setTabindex(TabindexMode tabulable) {
		textBox.setTabindex(tabulable);
	}

	@Override
	public HandlerRegistration addKeyDownHandler(Handler handler) {
		return textBox.addKeyDownHandler(handler);
	}

	@Override
	public HandlerRegistration addKeyUpHandler(PKeyUpHandler handler) {
		return textBox.addKeyUpHandler(handler);
	}

	@Override
	public void setText(String text) {
		textBox.setText(text);
	}

	@Override
	public void focus() {
		textBox.focus();
	}

	@Override
	public String getText() {
		return textBox.getText();
	}

}
