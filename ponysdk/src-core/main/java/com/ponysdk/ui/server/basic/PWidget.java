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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.event.Event;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.event.EventHandler;
import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.core.event.SimpleEventBus;
import com.ponysdk.core.instruction.EntryInstruction;
import com.ponysdk.core.instruction.Parser;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.server.basic.event.HasPWidgets;
import com.ponysdk.ui.server.basic.event.PBlurEvent;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PContextMenuEvent;
import com.ponysdk.ui.server.basic.event.PDomEvent;
import com.ponysdk.ui.server.basic.event.PDoubleClickEvent;
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
import com.ponysdk.ui.server.basic.event.PMouseEvent;
import com.ponysdk.ui.server.basic.event.PMouseOutEvent;
import com.ponysdk.ui.server.basic.event.PMouseOverEvent;
import com.ponysdk.ui.server.basic.event.PMouseUpEvent;
import com.ponysdk.ui.terminal.DomHandlerType;
import com.ponysdk.ui.terminal.model.Model;

/**
 * The base class for the majority of user-interface objects. Widget adds support for receiving events from
 * the browser and being added directly to {@link PPanel panels}.
 */
public abstract class PWidget extends PObject implements IsPWidget {

    private static Logger log = LoggerFactory.getLogger(PWidget.class);

    protected PWidget parent;

    protected Object data;

    private Set<String> styleNames;
    private Set<PEvent> preventEvents;
    private Set<PEvent> stopEvents;

    private EventBus domHandler;

    private Map<String, String> styleProperties;
    private Map<String, String> elementProperties;
    private Map<String, String> elementAttributes;

    protected boolean visible = true;
    private String title;
    private String width;
    private String height;
    private String styleName;
    private String stylePrimaryName;
    private String debugID;

    public PWidget(final EntryInstruction... entries) {
        super(entries);
    }

    public static PWidget asWidgetOrNull(final IsPWidget w) {
        return w == null ? null : w.asWidget();
    }

    private final Set<String> safeStyleName() {
        if (styleNames != null) return styleNames;
        styleNames = new HashSet<>();
        return styleNames;
    }

    private final Set<PEvent> safePreventEvents() {
        if (preventEvents != null) return preventEvents;
        preventEvents = new HashSet<>();
        return preventEvents;
    }

    private final Set<PEvent> safeStopEvents() {
        if (stopEvents != null) return stopEvents;
        stopEvents = new HashSet<>();
        return stopEvents;
    }

    private final Map<String, String> safeStyleProperties() {
        if (styleProperties != null) return styleProperties;
        styleProperties = new HashMap<>();
        return styleProperties;
    }

    private final Map<String, String> safeElementProperties() {
        if (elementProperties != null) return elementProperties;
        elementProperties = new HashMap<>();
        return elementProperties;
    }

    private final Map<String, String> safeElementAttributes() {
        if (elementAttributes != null) return elementAttributes;
        elementAttributes = new HashMap<>();
        return elementAttributes;
    }

    public void setVisible(final boolean visible) {
        if (Objects.equals(this.visible, visible)) return;
        this.visible = visible;
        saveUpdate(Model.WIDGET_VISIBLE, visible);
    }

    public void setWidth(final String width) {
        if (Objects.equals(this.width, width)) return;
        this.width = width;
        saveUpdate(Model.WIDGET_WIDTH, width);
    }

    public void setHeight(final String height) {
        if (Objects.equals(this.height, height)) return;
        this.height = height;
        saveUpdate(Model.WIDGET_HEIGHT, height);
    }

    public void setTitle(final String title) {
        if (Objects.equals(this.title, title)) return;
        this.title = title;
        saveUpdate(Model.WIDGET_TITLE, title);
    }

    public void setStyleName(final String styleName) {
        if (Objects.equals(this.styleName, styleName)) return;
        this.styleName = styleName;
        saveUpdate(Model.STYLE_NAME, styleName);
    }

    public void setStylePrimaryName(final String stylePrimaryName) {
        if (Objects.equals(this.stylePrimaryName, stylePrimaryName)) return;
        this.stylePrimaryName = stylePrimaryName;
        saveUpdate(Model.STYLE_PRIMARY_NAME, stylePrimaryName);
    }

    public void ensureDebugId(final String debugID) {
        if (Objects.equals(this.debugID, debugID)) return;
        this.debugID = debugID;
        saveUpdate(Model.ENSURE_DEBUG_ID, debugID);
    }

    public String getTitle() {
        return title;
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

    public PWidget getParent() {
        return parent;
    }

    public void setParent(final PWidget parent) {
        this.parent = parent;
    }

    public void setStyleProperty(final String name, final String value) {
        final String previous = safeStyleProperties().put(name, value);
        if (!Objects.equals(previous, value)) {
            final Parser parser = Txn.get().getTxnContext().getParser();
            parser.parse(Model.TYPE_UPDATE);
            parser.parse(Model.OBJECT_ID, ID);
            parser.parse(Model.PUT_STYLE_KEY, name);
            parser.parse(Model.STYLE_VALUE, value);
        }
    }

    public void removeStyleProperty(final String name) {
        final String previous = safeStyleProperties().remove(name);
        if (previous != null) {
            final Parser parser = Txn.get().getTxnContext().getParser();
            parser.parse(Model.TYPE_UPDATE);
            parser.parse(Model.OBJECT_ID, ID);
            parser.parse(Model.REMOVE_STYLE_KEY, name);
        }
    }

    public void setProperty(final String name, final String value) {
        final String previous = safeElementProperties().put(name, value);
        if (!Objects.equals(previous, value)) {
            final Parser parser = Txn.get().getTxnContext().getParser();
            parser.parse(Model.TYPE_UPDATE);
            parser.parse(Model.OBJECT_ID, ID);
            parser.parse(Model.PUT_PROPERTY_KEY, name);
            parser.parse(Model.PROPERTY_VALUE, value);
        }
    }

    public void setAttribute(final String name, final String value) {
        final String previous = safeElementAttributes().put(name, value);
        if (!Objects.equals(previous, value)) {
            final Parser parser = Txn.get().getTxnContext().getParser();
            parser.parse(Model.TYPE_UPDATE);
            parser.parse(Model.OBJECT_ID, ID);
            parser.parse(Model.PUT_ATTRIBUTE_KEY, name);
            parser.parse(Model.ATTRIBUTE_VALUE, value);
        }
    }

    public void removeAttribute(final String name) {
        final String previous = safeElementAttributes().remove(name);
        if (previous != null) {
            final Parser parser = Txn.get().getTxnContext().getParser();
            parser.parse(Model.TYPE_UPDATE);
            parser.parse(Model.OBJECT_ID, ID);
            parser.parse(Model.REMOVE_ATTRIBUTE_KEY, name);
        }
    }

    public String getProperty(final String key) {
        if (elementProperties == null) return null;
        return elementProperties.get(key);
    }

    public String getAttribute(final String key) {
        if (elementAttributes == null) return null;
        return elementAttributes.get(key);
    }

    public void preventEvent(final PEvent e) {
        if (safePreventEvents().add(e)) {
            final Parser parser = Txn.get().getTxnContext().getParser();
            parser.parse(Model.TYPE_UPDATE);
            parser.parse(Model.OBJECT_ID, ID);
            parser.parse(Model.PREVENT_EVENT, e.getCode());
        }
    }

    public void stopEvent(final PEvent e) {
        if (safeStopEvents().add(e)) {
            final Parser parser = Txn.get().getTxnContext().getParser();
            parser.parse(Model.TYPE_UPDATE);
            parser.parse(Model.OBJECT_ID, ID);
            parser.parse(Model.STOP_EVENT, e.getCode());
        }
    }

    public void addStyleName(final String styleName) {
        if (safeStyleName().add(styleName)) {
            final Parser parser = Txn.get().getTxnContext().getParser();
            parser.parse(Model.TYPE_UPDATE);
            parser.parse(Model.OBJECT_ID, ID);
            parser.parse(Model.ADD_STYLE_NAME, styleName);
        }
    }

    public void removeStyleName(final String styleName) {
        if (styleNames == null) return;
        if (styleNames.remove(styleName)) {
            removeStyle(styleName);
        }
    }

    private void removeStyle(final String styleName) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.parse(Model.TYPE_UPDATE);
        parser.parse(Model.OBJECT_ID, ID);
        parser.parse(Model.REMOVE_STYLE_NAME, styleName);
    }

    public boolean hasStyleName(final String styleName) {
        if (styleNames == null) return false;
        return styleNames.contains(styleName);
    }

    public Object getData() {
        return data;
    }

    public void setData(final Object data) {
        this.data = data;
    }

    public void setDomHandler(final EventBus domHandler) {
        this.domHandler = domHandler;
    }

    @Override
    public PWidget asWidget() {
        return this;
    }

    public void setSizeFull() {
        setWidth(Size.HUNDRED_PERCENT);
        setHeight(Size.HUNDRED_PERCENT);
    }

    public <H extends EventHandler> HandlerRegistration removeDomHandler(final H handler, final PDomEvent.Type<H> type) {
        final HandlerRegistration handlerRegistration = ensureDomHandler().addHandler(type, handler);

        if (handler instanceof JsonObject) {
            saveRemoveHandler(Model.HANDLER_DOM_HANDLER, Model.DOM_HANDLER_CODE, handler);
        } else {
            saveRemoveHandler(Model.HANDLER_DOM_HANDLER);
        }

        return handlerRegistration;
    }

    public <H extends EventHandler> HandlerRegistration removeDomHandler(final H handler, final PDomEvent.Type<H> type) {
        final HandlerRegistration handlerRegistration = ensureDomHandler().addHandler(type, handler);

        if (handler instanceof JsonObject) {
            saveRemoveHandler(Model.HANDLER_DOM_HANDLER, Model.DOM_HANDLER_CODE, handler);
        } else {
            saveRemoveHandler(Model.HANDLER_DOM_HANDLER);
        }

        return handlerRegistration;
    }

    @SuppressWarnings("unchecked")
    public <H extends EventHandler> HandlerRegistration addDomHandler(final H handler, final PDomEvent.Type<H> type) {
        final Collection<H> handlerIterator = ensureDomHandler().getHandlers(type, this);
        final HandlerRegistration handlerRegistration = domHandler.addHandlerToSource(type, this, handler);
        if (handlerIterator.isEmpty()) {
            final AddHandler addHandler = new AddHandler(getID(), HANDLER.KEY_.DOM_HANDLER);
            addHandler.put(Model.DOM_HANDLER_CODE, Model.TYPE_getDomHandlerType().ordinal());
            if (handler instanceof JsonObject) {
                try {
                    final JsonObject jso = (JsonObject) handler;
                    for (final Iterator<String> iterator = jso.keys(); iterator.hasNext();) {
                        final String key = iterator.next();
                        addHandler.put(key, jso.get(key));
                    }
                } catch (final JSONException e) {
                    log.error("Failed to copy value", e);
                }
            }
            Txn.get().getTxnContext().save(addHandler);
        }
        return handlerRegistration;
    }

    @Override
    public void onClientData(final JSONObject instruction) {
        if (instruction.has(HANDLER.KEY) && instruction.getString(HANDLER.KEY).equals(HANDLER.KEY_.DOM_HANDLER)) {
            final DomHandlerType domHandler = DomHandlerModel.TYPE_values()[instruction.getInt(Model.DOM_HANDLER_TYPE)];
            switch (domHandler) {
                case KEY_PRESS:
                    fireEvent(new PKeyPressEvent(this, instruction.getInt(Model.VALUE)));
                    break;
                case KEY_UP:
                    fireEvent(new PKeyUpEvent(this, instruction.getInt(Model.VALUE)));
                    break;
                case CLICK:
                    fireMouseEvent(instruction, new PClickEvent(this));
                    break;
                case DOUBLE_CLICK:
                    fireMouseEvent(instruction, new PDoubleClickEvent(this));
                    break;
                case MOUSE_OVER:
                    fireMouseEvent(instruction, new PMouseOverEvent(this));
                    break;
                case MOUSE_OUT:
                    fireMouseEvent(instruction, new PMouseOutEvent(this));
                    break;
                case MOUSE_DOWN:
                    fireMouseEvent(instruction, new PMouseDownEvent(this));
                    break;
                case MOUSE_UP:
                    fireMouseEvent(instruction, new PMouseUpEvent(this));
                    break;
                case FOCUS:
                    fireEvent(new PFocusEvent(this));
                    break;
                case BLUR:
                    fireEvent(new PBlurEvent(this));
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
                    if (instruction.has(Model.DRAG_SRC)) {
                        final PWidget source = UIContext.get().getObject(instruction.getLong(Model.DRAG_SRC));
                        dropEvent.setDragSource(source);
                    }
                    fireEvent(dropEvent);
                    break;
                case CONTEXT_MENU:
                    fireEvent(new PContextMenuEvent(this));
                    break;
                default:
                    log.error("Dom Handler not implemented: " + domHandler);
                    break;
            }
        } else {
            super.onClientData(instruction);
        }
    }

    private EventBus ensureDomHandler() {
        if (domHandler == null) domHandler = new SimpleEventBus();
        return domHandler;
    }

    protected <H extends EventHandler> Collection<H> getHandlerSet(final PDomEvent.Type<H> type, final Object source) {
        return ensureDomHandler().getHandlers(type, null);
    }

    public void fireMouseEvent(final JsonObject instruction, final PMouseEvent<?> event) throws JSONException {
        event.setX(instruction.getInt(Model.X));
        event.setY(instruction.getInt(Model.Y));
        event.setNativeButton(instruction.getInt(Model.NATIVE_BUTTON));
        event.setClientX(instruction.getInt(Model.CLIENT_X));
        event.setClientY(instruction.getInt(Model.CLIENT_Y));
        event.setSourceAbsoluteLeft((int) instruction.getDouble(Model.SOURCE_ABSOLUTE_LEFT));
        event.setSourceAbsoluteTop((int) instruction.getDouble(Model.SOURCE_ABSOLUTE_TOP));
        event.setSourceOffsetHeight((int) instruction.getDouble(Model.SOURCE_OFFSET_HEIGHT));
        event.setSourceOffsetWidth((int) instruction.getDouble(Model.SOURCE_OFFSET_WIDTH));

        fireEvent(event);
    }

    public void fireEvent(final Event<?> event) {
        if (domHandler == null) return;
        domHandler.fireEvent(event);
    }

    public void removeFromParent() {
        if (parent instanceof HasPWidgets) {
            ((HasPWidgets) parent).remove(this);
        } else if (parent != null) { throw new IllegalStateException("This widget's parent does not implement HasPWidgets"); }
    }

}
