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

package com.ponysdk.ui.server.basic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.event.Event;
import com.ponysdk.core.event.EventHandler;
import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.core.event.SimpleEventBus;
import com.ponysdk.core.instruction.AddHandler;
import com.ponysdk.core.instruction.RemoveHandler;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.server.basic.event.HasPWidgets;
import com.ponysdk.ui.server.basic.event.PBlurEvent;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PDomEvent;
import com.ponysdk.ui.server.basic.event.PDragEndEvent;
import com.ponysdk.ui.server.basic.event.PDragEnterEvent;
import com.ponysdk.ui.server.basic.event.PDragLeaveEvent;
import com.ponysdk.ui.server.basic.event.PDragOverEvent;
import com.ponysdk.ui.server.basic.event.PDragStartEvent;
import com.ponysdk.ui.server.basic.event.PDropEvent;
import com.ponysdk.ui.server.basic.event.PFocusEvent;
import com.ponysdk.ui.server.basic.event.PKeyPressEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PMouseDownEvent;
import com.ponysdk.ui.server.basic.event.PMouseOutEvent;
import com.ponysdk.ui.server.basic.event.PMouseOverEvent;
import com.ponysdk.ui.server.basic.event.PMouseUpEvent;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.DomHandlerType;

/**
 * The base class for the majority of user-interface objects. Widget adds support for receiving events from
 * the browser and being added directly to {@link PPanel panels}.
 */
public abstract class PWidget extends PObject implements IsPWidget {

    private static Logger log = LoggerFactory.getLogger(PWidget.class);

    protected PWidget parent;

    private boolean visible = true;

    protected Object data;

    private final Set<String> styleNames = new HashSet<String>();

    private final Map<String, String> styleProperties = new HashMap<String, String>();

    private final SimpleEventBus domHandler = new SimpleEventBus();

    private final Map<String, String> elementProperties = new HashMap<String, String>();

    private String title;
    private String width;
    private String height;
    private String styleName;
    private String stylePrimaryName;
    private String debugID;

    public static PWidget asWidgetOrNull(final IsPWidget w) {
        return w == null ? null : w.asWidget();
    }

    protected void stackUpdate(final String key, final int value) {
        final Update update = new Update(getID());
        update.put(key, value);
        getUIContext().stackInstruction(update);
    }

    protected void stackUpdate(final String key, final String value) {
        final Update update = new Update(getID());
        update.put(key, value);
        getUIContext().stackInstruction(update);
    }

    public void setWidth(final String width) {
        this.width = width;
        stackUpdate(PROPERTY.WIDGET_WIDTH, width);
    }

    public void setHeight(final String height) {
        this.height = height;
        stackUpdate(PROPERTY.WIDGET_HEIGHT, height);
    }

    public PWidget getParent() {
        return parent;
    }

    public void setParent(final PWidget parent) {
        this.parent = parent;
    }

    public void setStyleProperty(final String name, final String value) {
        styleProperties.put(name, value);
        final Update update = new Update(ID);
        update.put(PROPERTY.STYLE_KEY, name);
        update.put(PROPERTY.STYLE_VALUE, value);
        getUIContext().stackInstruction(update);
    }

    public void setProperty(final String name, final String value) {
        elementProperties.put(name, value);
        final Update update = new Update(ID);
        update.put(PROPERTY.ELEMENT_PROPERTY_KEY, name);
        update.put(PROPERTY.ELEMENT_PROPERTY_VALUE, value);
        getUIContext().stackInstruction(update);
    }

    public void setStyleName(final String styleName) {
        this.styleName = styleName;
        final Update update = new Update(ID);
        update.put(PROPERTY.STYLE_NAME, styleName);
        getUIContext().stackInstruction(update);
    }

    public void setStylePrimaryName(final String styleName) {
        this.stylePrimaryName = styleName;
        final Update update = new Update(ID);
        update.put(PROPERTY.STYLE_PRIMARY_NAME, styleName);
        getUIContext().stackInstruction(update);
    }

    public void addStyleName(final String styleName) {
        if (styleNames.add(styleName)) {
            final Update update = new Update(ID);
            update.put(PROPERTY.ADD_STYLE_NAME, styleName);
            getUIContext().stackInstruction(update);
        }
    }

    public void removeStyleName(final String styleName) {
        if (styleNames.remove(styleName)) {
            final Update update = new Update(ID);
            update.put(PROPERTY.REMOVE_STYLE_NAME, styleName);
            getUIContext().stackInstruction(update);
        }
    }

    public void ensureDebugId(final String debugID) {
        this.debugID = debugID;
        final Update update = new Update(ID);
        update.put(PROPERTY.ENSURE_DEBUG_ID, debugID);
        getUIContext().stackInstruction(update);
    }

    @Override
    public PWidget asWidget() {
        return this;
    }

    public void setSizeFull() {
        setWidth("100%");
        setHeight("100%");
    }

    public void setVisible(final boolean visible) {
        this.visible = visible;
        final Update update = new Update(ID);
        update.put(PROPERTY.WIDGET_VISIBLE, visible);
        getUIContext().stackInstruction(update);
    }

    public void setTitle(final String title) {
        this.title = title;
        final Update update = new Update(ID);
        update.put(PROPERTY.WIDGET_TITLE, title);
        getUIContext().stackInstruction(update);
    }

    public String getTitle() {
        return title;
    }

    public Object getData() {
        return data;
    }

    public void setData(final Object data) {
        this.data = data;
    }

    public <H extends EventHandler> HandlerRegistration removeDomHandler(final H handler, final PDomEvent.Type<H> type) {
        final HandlerRegistration handlerRegistration = domHandler.addHandler(type, handler);
        final RemoveHandler removeHandler = new RemoveHandler(getID(), HANDLER.KEY_.DOM_HANDLER);
        if (handler instanceof JSONObject) {
            removeHandler.put(PROPERTY.DOM_HANDLER_CODE, handler);
        }
        getUIContext().stackInstruction(removeHandler);
        return handlerRegistration;
    }

    @SuppressWarnings("unchecked")
    public <H extends EventHandler> HandlerRegistration addDomHandler(final H handler, final PDomEvent.Type<H> type) {
        final HandlerRegistration handlerRegistration = domHandler.addHandler(type, handler);
        final AddHandler addHandler = new AddHandler(getID(), HANDLER.KEY_.DOM_HANDLER);
        addHandler.put(PROPERTY.DOM_HANDLER_CODE, type.getDomHandlerType().ordinal());
        if (handler instanceof JSONObject) {
            try {
                final JSONObject jso = (JSONObject) handler;
                for (final Iterator<String> iterator = jso.keys(); iterator.hasNext();) {
                    final String key = iterator.next();
                    addHandler.put(key, jso.get(key));
                }
            } catch (final JSONException e) {
                log.error("Failed to copy value", e);
            }
        }
        getUIContext().stackInstruction(addHandler);
        return handlerRegistration;
    }

    @Override
    public void onEventInstruction(final JSONObject instruction) throws JSONException {
        if (instruction.has(HANDLER.KEY) && instruction.getString(HANDLER.KEY).equals(HANDLER.KEY_.DOM_HANDLER)) {
            final DomHandlerType domHandler = DomHandlerType.values()[instruction.getInt(PROPERTY.DOM_HANDLER_TYPE)];
            switch (domHandler) {
                case KEY_PRESS:
                    fireEvent(new PKeyPressEvent(this, instruction.getInt(PROPERTY.VALUE)));
                    break;
                case KEY_UP:
                    fireEvent(new PKeyUpEvent(this, instruction.getInt(PROPERTY.VALUE)));
                    break;
                case CLICK:
                    final PClickEvent event = new PClickEvent(this);
                    event.setClientX(instruction.getInt(PROPERTY.CLIENT_X));
                    event.setClientY(instruction.getInt(PROPERTY.CLIENT_Y));
                    event.setSourceAbsoluteLeft((int) instruction.getDouble(PROPERTY.SOURCE_ABSOLUTE_LEFT));
                    event.setSourceAbsoluteTop((int) instruction.getDouble(PROPERTY.SOURCE_ABSOLUTE_TOP));
                    event.setSourceOffsetHeight((int) instruction.getDouble(PROPERTY.SOURCE_OFFSET_HEIGHT));
                    event.setSourceOffsetWidth((int) instruction.getDouble(PROPERTY.SOURCE_OFFSET_WIDTH));
                    fireEvent(event);
                    break;
                case MOUSE_OVER:
                    fireEvent(new PMouseOverEvent(this));
                    break;
                case FOCUS:
                    fireEvent(new PFocusEvent(this));
                    break;
                case BLUR:
                    fireEvent(new PBlurEvent(this));
                    break;
                case MOUSE_OUT:
                    fireEvent(new PMouseOutEvent(this));
                    break;
                case MOUSE_DOWN:
                    fireEvent(new PMouseDownEvent(this));
                    break;
                case MOUSE_UP:
                    fireEvent(new PMouseUpEvent(this));
                    break;
                case DRAG_START:
                    fireEvent(new PDragStartEvent(this));
                    break;
                case DRAG_END:
                    fireEvent(new PDragEndEvent(this));
                    break;
                case DRAG_ENTER:
                    fireEvent(new PDragEnterEvent(this));
                    break;
                case DRAG_LEAVE:
                    fireEvent(new PDragLeaveEvent(this));
                    break;
                case DRAG_OVER:
                    fireEvent(new PDragOverEvent(this));
                    break;
                case DROP:
                    final PDropEvent dropEvent = new PDropEvent(this);
                    if (instruction.has(PROPERTY.DRAG_SRC)) {
                        final PWidget source = UIContext.get().getObject(instruction.getLong(PROPERTY.DRAG_SRC));
                        dropEvent.setDragSource(source);
                    }
                    fireEvent(dropEvent);
                    break;
                default:
                    log.error("Dom Handler not implemented: " + domHandler);
                    break;
            }
        } else {
            super.onEventInstruction(instruction);
        }
    }

    protected <H extends EventHandler> Set<H> getHandlerSet(final PDomEvent.Type<H> type, final Object source) {
        return domHandler.getHandlerSet(type, null);
    }

    public void fireEvent(final Event<?> event) {
        domHandler.fireEvent(event);
    }

    public void removeFromParent() {
        if (parent instanceof HasPWidgets) {
            ((HasPWidgets) parent).remove(this);
        } else if (parent != null) { throw new IllegalStateException("This widget's parent does not implement HasPWidgets"); }
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

    public String getStylePrimaryName() {
        return stylePrimaryName;
    }

}
