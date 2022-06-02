package com.ponysdk.core.ui.listbox;

import java.util.List;
import java.util.function.Consumer;

import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PDoubleClickHandler;
import com.ponysdk.core.ui.basic.event.PDragEndHandler;
import com.ponysdk.core.ui.basic.event.PDragEnterHandler;
import com.ponysdk.core.ui.basic.event.PDragLeaveEvent.Handler;
import com.ponysdk.core.ui.basic.event.PDragStartHandler;
import com.ponysdk.core.ui.basic.event.PDropHandler;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem;
import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem.ListBoxItemType;

public class DefaultListBoxLabel implements ListBoxLabel{
	private  PLabel label = Element.newPLabel();
	
	
	@Override
	public void setText(String text) {
		System.err.println(text);
		label.setText(text);
	}
	
	@Override
	public HandlerRegistration addClickHandler(PClickHandler handler) {
		return asWidget().addClickHandler(handler);
	}

	@Override
	public String dumpDOM() {
		return asWidget().dumpDOM();
	}
	
	@Override
	public String getText() {
		return asWidget().getText();
	}

	@Override
	public String getAttributeLinkedToValue() {
		return asWidget().getAttributeLinkedToValue();
	}

	@Override
	public void setAttributeLinkedToValue(String attributeLinkedToValue) {
		asWidget().setAttributeLinkedToValue(attributeLinkedToValue);
	}

	@Override
	public HandlerRegistration addDoubleClickHandler(PDoubleClickHandler handler) {
		return asWidget().addDoubleClickHandler(handler);
	}

	@Override
	public HandlerRegistration addDragEndHandler(PDragEndHandler handler) {
		return asWidget().addDragEndHandler(handler);
	}

	@Override
	public HandlerRegistration addDragEnterHandler(PDragEnterHandler handler) {
		return asWidget().addDragEnterHandler(handler);
	}

	@Override
	public HandlerRegistration addDragStartHandler(PDragStartHandler handler) {
		return asWidget().addDragStartHandler(handler);
	}

	@Override
	public HandlerRegistration addDragLeaveHandler(Handler handler) {
		return asWidget().addDragLeaveHandler(handler);
	}

	@Override
	public HandlerRegistration addDragOverHandler(com.ponysdk.core.ui.basic.event.PDragOverEvent.Handler handler) {
		return asWidget().addDragOverHandler(handler);
	}

	@Override
	public HandlerRegistration addDropHandler(PDropHandler handler) {
		return asWidget().addDropHandler(handler);
	}

	@Override
	public PLabel asWidget() {
		return label;
	}
}