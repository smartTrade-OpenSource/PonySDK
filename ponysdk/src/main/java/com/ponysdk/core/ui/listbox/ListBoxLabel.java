package com.ponysdk.core.ui.listbox;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PDoubleClickEvent;
import com.ponysdk.core.ui.basic.event.PDoubleClickHandler;
import com.ponysdk.core.ui.basic.event.PDragEndEvent;
import com.ponysdk.core.ui.basic.event.PDragEndHandler;
import com.ponysdk.core.ui.basic.event.PDragEnterEvent;
import com.ponysdk.core.ui.basic.event.PDragEnterHandler;
import com.ponysdk.core.ui.basic.event.PDragLeaveEvent;
import com.ponysdk.core.ui.basic.event.PDragOverEvent;
import com.ponysdk.core.ui.basic.event.PDragStartEvent;
import com.ponysdk.core.ui.basic.event.PDragStartHandler;
import com.ponysdk.core.ui.basic.event.PDropEvent;
import com.ponysdk.core.ui.basic.event.PDropHandler;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem;
import com.ponysdk.core.util.Incubation;

public interface ListBoxLabel extends IsPWidget{

     String getText();

    void setText(final String text);

     String getAttributeLinkedToValue();

    /**
     * Link an HTML attribute (like "data-title") directly to the value.
     */
    @Incubation(since = "2.8.11")
     void setAttributeLinkedToValue(final String attributeLinkedToValue);

    HandlerRegistration addClickHandler(final PClickHandler handler);

     HandlerRegistration addDoubleClickHandler(final PDoubleClickHandler handler);

     HandlerRegistration addDragEndHandler(final PDragEndHandler handler);

     HandlerRegistration addDragEnterHandler(final PDragEnterHandler handler);

     HandlerRegistration addDragStartHandler(final PDragStartHandler handler);

     HandlerRegistration addDragLeaveHandler(final PDragLeaveEvent.Handler handler);

     HandlerRegistration addDragOverHandler(final PDragOverEvent.Handler handler);

     HandlerRegistration addDropHandler(final PDropHandler handler);

     String dumpDOM();
    
    
	
}
