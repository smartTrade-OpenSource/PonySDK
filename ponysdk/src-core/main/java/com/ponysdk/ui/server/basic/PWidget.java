package com.ponysdk.ui.server.basic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.PonySession;
import com.ponysdk.core.event.Event;
import com.ponysdk.core.event.EventHandler;
import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.core.event.SimpleEventBus;
import com.ponysdk.ui.server.basic.event.HasPProperties;
import com.ponysdk.ui.server.basic.event.HasPWidgets;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PDomEvent;
import com.ponysdk.ui.server.basic.event.PKeyPressEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PMouseOutEvent;
import com.ponysdk.ui.server.basic.event.PMouseOverEvent;
import com.ponysdk.ui.terminal.DomHandlerType;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *  
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

public abstract class PWidget extends PObject implements IsPWidget {

    private static Logger log = LoggerFactory.getLogger(PWidget.class);

    protected PWidget parent;
    private boolean visible = true;
    private String title;

    protected Object data;

    private final Set<String> styleNames = new HashSet<String>();
    private final Map<String, String> styleProperties = new HashMap<String, String>();

    private final SimpleEventBus domHandler = new SimpleEventBus();

    private String width;
    private String height;
    private String styleName;
    private String debugID;

    protected void setMainProperty(Property mainProperty) {
        create.setMainProperty(mainProperty);
    }

    public static PWidget asWidgetOrNull(IsPWidget w) {
        return w == null ? null : w.asWidget();
    }

    protected void stackUpdate(PropertyKey key, String value) {
        final Update update = new Update(getID());
        update.setMainPropertyValue(key, value);
        getPonySession().stackInstruction(update);
    }

    public void setWidth(String width) {
        this.width = width;
        stackUpdate(PropertyKey.WIDGET_WIDTH, width);
    }

    public void setHeight(String height) {
        this.height = height;
        stackUpdate(PropertyKey.WIDGET_HEIGHT, height);
    }

    public PonySession getPonySession() {
        return PonySession.getCurrent();
    }

    public PWidget getParent() {
        return parent;
    }

    public void setParent(PWidget parent) {
        this.parent = parent;
    }

    public void setStyleProperty(String name, String value) {
        styleProperties.put(name, value);
        final Update update = new Update(ID);
        update.setMainPropertyKey(PropertyKey.STYLE_PROPERTY);
        update.getMainProperty().setProperty(PropertyKey.STYLE_KEY, name);
        update.getMainProperty().setProperty(PropertyKey.STYLE_VALUE, value);
        getPonySession().stackInstruction(update);
    }

    public void setStyleName(String styleName) {
        this.styleName = styleName;

        final Update update = new Update(ID);
        update.setMainPropertyValue(PropertyKey.STYLE_NAME, styleName);
        getPonySession().stackInstruction(update);
    }

    public void addStyleName(String styleName) {
        if (styleNames.add(styleName)) {
            final Update update = new Update(ID);
            update.setMainPropertyValue(PropertyKey.ADD_STYLE_NAME, styleName);
            getPonySession().stackInstruction(update);
        }
    }

    public void removeStyleName(String styleName) {
        if (styleNames.remove(styleName)) {
            final Update update = new Update(ID);
            update.setMainPropertyValue(PropertyKey.REMOVE_STYLE_NAME, styleName);
            getPonySession().stackInstruction(update);
        }
    }

    public void ensureDebugId(String debugID) {
        this.debugID = debugID;
        final Update update = new Update(ID);
        update.setMainPropertyValue(PropertyKey.ENSURE_DEBUG_ID, debugID);
        getPonySession().stackInstruction(update);
    }

    @Override
    public PWidget asWidget() {
        return this;
    }

    public void setSizeFull() {
        setWidth("100%");
        setHeight("100%");
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        final Update update = new Update(ID);
        update.setMainPropertyValue(PropertyKey.WIDGET_VISIBLE, visible);
        getPonySession().stackInstruction(update);
    }

    public void setTitle(String title) {
        this.title = title;
        final Update update = new Update(ID);
        update.setMainPropertyValue(PropertyKey.WIDGET_TITLE, title);
        getPonySession().stackInstruction(update);
    }

    public String getTitle() {
        return title;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public <H extends EventHandler> HandlerRegistration addDomHandler(final H handler, final PDomEvent.Type<H> type) {
        final HandlerRegistration handlerRegistration = domHandler.addHandler(type, handler);
        final AddHandler addHandler = createAddHandlerInstruction(type);
        if (handler instanceof HasPProperties)
            addHandler.getMainProperty().setProperties(((HasPProperties) handler).getProperties());
        getPonySession().stackInstruction(addHandler);
        return handlerRegistration;
    }

    private <H extends EventHandler> AddHandler createAddHandlerInstruction(final PDomEvent.Type<H> type) {
        final AddHandler addHandler = new AddHandler(getID(), HandlerType.DOM_HANDLER);
        final Property property = new Property(PropertyKey.DOM_HANDLER, type.getDomHandlerType().ordinal());
        addHandler.setMainProperty(property);
        return addHandler;
    }

    @Override
    public void onEventInstruction(EventInstruction instruction) {

        if (HandlerType.DOM_HANDLER.equals(instruction.getHandlerType())) {
            final DomHandlerType domHandler = DomHandlerType.values()[instruction.getMainProperty().getIntProperty(PropertyKey.DOM_HANDLER)];
            switch (domHandler) {
            case KEY_PRESS:
                fireEvent(new PKeyPressEvent(this, instruction.getMainProperty().getIntValue()));
                break;
            case KEY_UP:
                fireEvent(new PKeyUpEvent(this, instruction.getMainProperty().getIntValue()));
                break;
            case CLICK:
                final PClickEvent event = new PClickEvent(this);
                event.setClientX(instruction.getMainProperty().getIntProperty(PropertyKey.CLIENT_X));
                event.setClientY(instruction.getMainProperty().getIntProperty(PropertyKey.CLIENT_Y));
                event.setSourceAbsoluteLeft((int) instruction.getMainProperty().getDoubleProperty(PropertyKey.SOURCE_ABSOLUTE_LEFT));
                event.setSourceAbsoluteTop((int) instruction.getMainProperty().getDoubleProperty(PropertyKey.SOURCE_ABSOLUTE_TOP));
                event.setSourceOffsetHeight((int) instruction.getMainProperty().getDoubleProperty(PropertyKey.SOURCE_OFFSET_HEIGHT));
                event.setSourceOffsetWidth((int) instruction.getMainProperty().getDoubleProperty(PropertyKey.SOURCE_OFFSET_WIDTH));
                fireEvent(event);
                break;
            case MOUSE_OVER:
                fireEvent(new PMouseOverEvent(this));
                break;
			case MOUSE_OUT:
				fireEvent(new PMouseOutEvent(this));
				break;
            default:
                log.error("Dom Handler not implemented: " + domHandler);
                break;
            }
        }
    }

    protected <H extends EventHandler> List<H> getHandlerList(final PDomEvent.Type<H> type, Object source) {
        return domHandler.getHandlerList(type, null);
    }

    public void fireEvent(Event<?> event) {
        domHandler.fireEvent(event);
    }

    public void removeFromParent() {
        if (parent instanceof HasPWidgets) {
            ((HasPWidgets) parent).remove(this);
        } else if (parent != null) {
            throw new IllegalStateException("This widget's parent does not implement HasPWidgets");
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public String getWidth() {
        return width;
    }

    public String getHeight() {
        return height;
    }

    public String getStyleName() {
        return styleName;
    }

    public String getDebugID() {
        return debugID;
    }

}
