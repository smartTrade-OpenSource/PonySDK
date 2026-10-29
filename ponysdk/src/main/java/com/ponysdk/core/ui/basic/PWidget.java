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

package com.ponysdk.core.ui.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DomHandlerConverter;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ProtocolVersion;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.event.HasPWidgets;
import com.ponysdk.core.ui.basic.event.PBlurEvent;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PContextMenuEvent;
import com.ponysdk.core.ui.basic.event.PDomEvent;
import com.ponysdk.core.ui.basic.event.PDomEvent.Type;
import com.ponysdk.core.ui.basic.event.PDoubleClickEvent;
import com.ponysdk.core.ui.basic.event.PDragEndEvent;
import com.ponysdk.core.ui.basic.event.PDragEnterEvent;
import com.ponysdk.core.ui.basic.event.PDragLeaveEvent;
import com.ponysdk.core.ui.basic.event.PDragOverEvent;
import com.ponysdk.core.ui.basic.event.PDragStartEvent;
import com.ponysdk.core.ui.basic.event.PDropEvent;
import com.ponysdk.core.ui.basic.event.PFocusEvent;
import com.ponysdk.core.ui.basic.event.PKeyDownEvent;
import com.ponysdk.core.ui.basic.event.PKeyEvent;
import com.ponysdk.core.ui.basic.event.PKeyPressEvent;
import com.ponysdk.core.ui.basic.event.PKeyPressHandler;
import com.ponysdk.core.ui.basic.event.PKeyUpEvent;
import com.ponysdk.core.ui.basic.event.PKeyUpHandler;
import com.ponysdk.core.ui.basic.event.PMouseDownEvent;
import com.ponysdk.core.ui.basic.event.PMouseEvent;
import com.ponysdk.core.ui.basic.event.PMouseOutEvent;
import com.ponysdk.core.ui.basic.event.PMouseOverEvent;
import com.ponysdk.core.ui.basic.event.PMouseUpEvent;
import com.ponysdk.core.ui.basic.event.PMouseWhellEvent;
import com.ponysdk.core.ui.basic.event.PVisibilityEvent;
import com.ponysdk.core.ui.basic.event.PVisibilityEvent.PVisibilityHandler;
import com.ponysdk.core.ui.eventbus.Event;
import com.ponysdk.core.ui.eventbus.EventHandler;
import com.ponysdk.core.ui.eventbus.EventSource;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.eventbus.CompactEventSource;
import com.ponysdk.core.ui.model.PEventType;
import com.ponysdk.core.ui.model.ServerBinaryModel;
import com.ponysdk.core.util.StringPairs;
import com.ponysdk.core.util.SetPool;
import com.ponysdk.core.util.SetUtils;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.core.writer.ModelWriterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The base class for the majority of user-interface objects. Widget adds
 * support for receiving events from the browser and being added directly to
 * {@link PPanel panels}.
 */
public abstract class PWidget extends PObject implements IsPWidget {

    private static final Logger log = LoggerFactory.getLogger(PWidget.class);
    private static final SetPool<String> styleNamesSetPool = new SetPool<>();
    private static final SetPool<PEventType> preventOrStopEventsSetPool = new SetPool<>();
    private static final SetPool<PDomEvent.Type> oneTimeHandlerCreationSetPool = new SetPool<>();

    private static final String HUNDRED_PERCENT = "100%";

    // Cached JSON keys to avoid repeated toStringValue() calls
    private static final String DOM_HANDLER_TYPE_KEY = ClientToServerModel.DOM_HANDLER_TYPE.toStringValue();
    private static final String VALUE_KEY_KEY = ClientToServerModel.VALUE_KEY.toStringValue();
    private static final String DRAG_SRC_KEY = ClientToServerModel.DRAG_SRC.toStringValue();
    private static final String EVENT_INFO_KEY = ClientToServerModel.EVENT_INFO.toStringValue();
    private static final String WIDGET_POSITION_KEY = ClientToServerModel.WIDGET_POSITION.toStringValue();
    private static final String HANDLER_WIDGET_VISIBILITY_KEY = ClientToServerModel.HANDLER_WIDGET_VISIBILITY.toStringValue();
    private static final String EVENT_KEY = ClientToServerModel.EVENT.toStringValue();
    private static final String EVENT_FIELDS_KEY = ClientToServerModel.EVENT_FIELDS.toStringValue();

    boolean visible = true;
    private IsPWidget parent;
    private SetPool<String>.ImmutableSet styleNames = styleNamesSetPool.emptyImmutableSet();
    // flat [key, value, ...] arrays (see StringPairs): no wrapper object per widget
    private String[] styleProperties;
    private String[] elementAttributes;
    private String title;
    protected int tabindex = -Integer.MAX_VALUE;
    private EventSource eventSource;


    // WORKAROUND Remove handler only server side
    private SetPool<PDomEvent.Type>.ImmutableSet oneTimeHandlerCreation = oneTimeHandlerCreationSetPool.emptyImmutableSet();


    /**
     * Rarely used state, allocated on first use (prevented/stopped events, element properties, debug
     * id, visibility handlers, addons): six reference slots that every widget paid for.
     */
    private static final class Extras {
        private SetPool<PEventType>.ImmutableSet preventEvents = preventOrStopEventsSetPool.emptyImmutableSet();
        private SetPool<PEventType>.ImmutableSet stopEvents = preventOrStopEventsSetPool.emptyImmutableSet();
        private String[] elementProperties;
        private String debugID;
        // set on a minority of widgets: kept off the widget itself (PLabel 96 -> 80 bytes)
        private String width;
        private String height;
        private String styleName;
        private boolean shown;
        private List<PVisibilityHandler> visibilityHandlers;
        private Set<PAddOn> addons;
        /** DOM events listened to by name: the options the terminal has, and the listeners. */
        private Map<String, EventListening> eventListening;
    }

    private static final class EventListening {
        private PEventOptions options;
        private final List<Consumer<PEvent>> listeners = new ArrayList<>(1);

        private EventListening(final PEventOptions options) {
            this.options = options;
        }
    }

    private Extras wx;

    private Extras wx() {
        if (wx == null) wx = new Extras();
        return wx;
    }

    public enum TabindexMode {

        FOCUSABLE(-1),
        TABULABLE(0);

        private final int tabIndex;

        TabindexMode(final int tabIndex) {
            this.tabIndex = tabIndex;
        }

        public int getTabIndex() {
            return tabIndex;
        }
    }

    protected PWidget() {
    }

    @Override
    protected void enrichForUpdate(final ModelWriter writer) {
        super.enrichForUpdate(writer);
        if (!visible) writer.write(ServerToClientModel.WIDGET_VISIBLE, false);
        final Extras x0 = wx;
        // STYLE_NAME replaces the element's default classes; the other names are added to it
        final String styleName0 = x0 != null ? x0.styleName : null;
        if (styleName0 != null) writer.write(ServerToClientModel.STYLE_NAME, styleName0);
        final SetPool<String>.ImmutableSet others = styleName0 != null ? styleNames.getRemove(styleName0) : styleNames;
        if (!others.isEmpty()) writer.write(ServerToClientModel.ADD_STYLE_NAME, others.join());
        if (this.title != null) writer.write(ServerToClientModel.WIDGET_TITLE, this.title);
        if (x0 != null && x0.height != null) writer.write(ServerToClientModel.WIDGET_HEIGHT, x0.height);
        if (x0 != null && x0.width != null) writer.write(ServerToClientModel.WIDGET_WIDTH, x0.width);
        if (wx != null && wx.debugID != null) writer.write(ServerToClientModel.ENSURE_DEBUG_ID, wx.debugID);
        final String[] styleProperties0 = styleProperties;
        if (styleProperties0 != null) {
            for (int i = 0; i < styleProperties0.length; i += 2) {
                writer.write(ServerToClientModel.PUT_STYLE_KEY, styleProperties0[i]);
                writer.write(ServerToClientModel.STYLE_VALUE, styleProperties0[i + 1]);
            }
        }
        final String[] elementAttributes0 = elementAttributes;
        if (elementAttributes0 != null) {
            for (int i = 0; i < elementAttributes0.length; i += 2) {
                writer.write(ServerToClientModel.PUT_ATTRIBUTE_KEY, elementAttributes0[i]);
                writer.write(ServerToClientModel.ATTRIBUTE_VALUE, elementAttributes0[i + 1]);
            }
        }
        final String[] properties0 = wx != null ? wx.elementProperties : null;
        if (properties0 != null) {
            for (int i = 0; i < properties0.length; i += 2) {
                writer.write(ServerToClientModel.PUT_PROPERTY_KEY, properties0[i]);
                writer.write(ServerToClientModel.PROPERTY_VALUE, properties0[i + 1]);
            }
        }
    }

    static PWidget asWidgetOrNull(final IsPWidget w) {
        return w == null ? null : w.asWidget();
    }

    /** Puts into style properties; @return true if the value changed. */
    private boolean putStyleProperty(final String name, final String value) {
        final int i = StringPairs.indexOf(styleProperties, name);
        if (i >= 0) {
            if (Objects.equals(styleProperties[i + 1], value)) return false;
            styleProperties[i + 1] = value;
        } else {
            styleProperties = StringPairs.append(styleProperties, name, value);
            if (value == null) return false; // absent == null: unchanged
        }
        return true;
    }

    private boolean putElementAttribute(final String name, final String value) {
        final int i = StringPairs.indexOf(elementAttributes, name);
        if (i >= 0) {
            if (Objects.equals(elementAttributes[i + 1], value)) return false;
            elementAttributes[i + 1] = value;
        } else {
            elementAttributes = StringPairs.append(elementAttributes, name, value);
            if (value == null) return false;
        }
        return true;
    }

    private boolean putElementProperty(final String name, final String value) {
        final Extras x = wx();
        final int i = StringPairs.indexOf(x.elementProperties, name);
        if (i >= 0) {
            if (Objects.equals(x.elementProperties[i + 1], value)) return false;
            x.elementProperties[i + 1] = value;
        } else {
            x.elementProperties = StringPairs.append(x.elementProperties, name, value);
            if (value == null) return false;
        }
        return true;
    }

    public void ensureDebugId(final String debugID) {
        if (UIContext.get().getConfiguration().isDebugMode()) {
            if (Objects.equals(getDebugID(), debugID)) return;
            wx().debugID = debugID;
            if (initialized) saveUpdate(writer -> writer.write(ServerToClientModel.ENSURE_DEBUG_ID, debugID));
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        if (Objects.equals(this.title, title)) return;
        this.title = title;
        if (initialized) saveUpdate(ServerToClientModel.WIDGET_TITLE, title);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(final boolean visible) {
        if (Objects.equals(this.visible, visible)) return;
        this.visible = visible;
        if (initialized) saveUpdate(ServerToClientModel.WIDGET_VISIBLE, visible);
    }

    public String getWidth() {
        return wx != null ? wx.width : null;
    }

    public void setWidth(final String width) {
        if (Objects.equals(getWidth(), width)) return;
        wx().width = width;
        if (initialized) saveUpdate(ServerToClientModel.WIDGET_WIDTH, width);
    }

    public String getHeight() {
        return wx != null ? wx.height : null;
    }

    public void setHeight(final String height) {
        if (Objects.equals(getHeight(), height)) return;
        wx().height = height;
        if (initialized) saveUpdate(ServerToClientModel.WIDGET_HEIGHT, height);
    }

    public String getStyleName() {
        return wx != null ? wx.styleName : null;
    }

    /**
     * Replaces all the style names of the widget by this one (an empty or null name removes them all), like
     * {@code element.className = styleName}. Use {@link #addStyleName} / {@link #removeStyleName} to change one.
     */
    public void setStyleName(final String styleName) {
        final String name = styleName != null ? styleName : "";
        if (name.equals(getStyleName()) && styleNames.size() == (name.isEmpty() ? 0 : 1)) return;
        // The whole class list is replaced: a name set before must not linger in the set (it did, and setting it
        // again later was then silently dropped - the browser kept the previous class)
        styleNames = name.isEmpty() ? styleNamesSetPool.emptyImmutableSet() : styleNamesSetPool.emptyImmutableSet().getAdd(name);
        wx().styleName = name;
        if (initialized) saveUpdate(ServerToClientModel.STYLE_NAME, name); // before: written by the create block
    }

    public String getDebugID() {
        return wx != null ? wx.debugID : null;
    }

    public IsPWidget getParent() {
        return parent;
    }

    void setParent(final IsPWidget parent) {
        this.parent = parent;
    }

    public void setStyleProperty(final String name, final String value) {
        if (putStyleProperty(name, value) && initialized) {
            saveUpdate(ServerToClientModel.PUT_STYLE_KEY, name, ServerToClientModel.STYLE_VALUE, value);
        }
    }

    public void removeStyleProperty(final String name) {
        final int i = StringPairs.indexOf(styleProperties, name);
        if (i < 0) return;
        final String old = styleProperties[i + 1];
        styleProperties = StringPairs.removeAt(styleProperties, i);
        if (old != null && initialized) saveKeyedUpdate(ServerToClientModel.REMOVE_STYLE_KEY, name);
    }

    public void forceDomId() {
        final String idValue = String.valueOf(ID);
        if (putElementProperty("id", idValue)) {
            saveUpdate(ServerToClientModel.FORCE_DOM_ID, null);
        }
    }

    public void setProperty(final String name, final String value) {
        if (putElementProperty(name, value) && initialized) {
            saveUpdate(ServerToClientModel.PUT_PROPERTY_KEY, name, ServerToClientModel.PROPERTY_VALUE, value);
        }
    }

    public void setAttribute(final String name) {
        setAttribute(name, "");
    }

    public void setAttribute(final String name, final String value) {
        if (name == null) return;

        if (!putElementAttribute(name, value)) return;
        // Before the CREATE the cached value is replayed by enrichForUpdate: stacking it as well sent it twice
        if (!initialized) return;

        saveUpdate(ServerToClientModel.PUT_ATTRIBUTE_KEY, name, ServerToClientModel.ATTRIBUTE_VALUE, value);
    }

    public void removeAttribute(final String name) {
        final int i = StringPairs.indexOf(elementAttributes, name);
        if (i < 0) return;
        final String old = elementAttributes[i + 1];
        elementAttributes = StringPairs.removeAt(elementAttributes, i);
        if (old != null && initialized) {
            saveKeyedUpdate(ServerToClientModel.REMOVE_ATTRIBUTE_KEY, name);
        }
    }

    public String getProperty(final String key) {
        return wx != null ? StringPairs.get(wx.elementProperties, key) : null;
    }

    public boolean hasAttribute(final String key) {
        return StringPairs.containsKey(elementAttributes, key);
    }

    public String getAttribute(final String key) {
        return StringPairs.get(elementAttributes, key);
    }

    public Set<Map.Entry<String, String>> getAttributes() {
        return StringPairs.entrySet(elementAttributes);
    }

    public void preventEvent(final PEventType e) {
        final Extras x = wx();
        final SetPool<PEventType>.ImmutableSet pool = x.preventEvents.getAdd(e);
        if (pool != x.preventEvents) {
            x.preventEvents = pool;
            saveUpdate(writer -> writer.write(ServerToClientModel.PREVENT_EVENT, e.getCode()));
        }
    }

    public void stopEvent(final PEventType e) {
        final Extras x = wx();
        final SetPool<PEventType>.ImmutableSet pool = x.stopEvents.getAdd(e);
        if (pool != x.stopEvents) {
            x.stopEvents = pool;
            saveUpdate(writer -> writer.write(ServerToClientModel.STOP_EVENT, e.getCode()));
        }
    }

    private boolean doAddStyleName(final String styleName) {
        final SetPool<String>.ImmutableSet s = styleNames.getAdd(styleName);
        if (s == styleNames) return false;
        styleNames = s;
        return true;
    }

    public void addStyleName(final String styleName) {
        if (styleName != null && !styleName.isEmpty() && doAddStyleName(styleName) && initialized) {
            saveKeyedUpdate(ServerToClientModel.ADD_STYLE_NAME, styleName);
        }
    }

    private boolean doRemoveStyleName(final String styleName) {
        final SetPool<String>.ImmutableSet s = styleNames.getRemove(styleName);
        if (s == styleNames) return false;
        styleNames = s;
        return true;
    }

    public void removeStyleName(final String styleName) {
        if (styleName != null && !styleName.isEmpty() && doRemoveStyleName(styleName) && initialized) {
            saveKeyedUpdate(ServerToClientModel.REMOVE_STYLE_NAME, styleName);
        }
    }

    public boolean hasStyleName(final String styleName) {
        return !styleName.isEmpty() && styleNames.contains(styleName);
    }

    @Override
    public PWidget asWidget() {
        return this;
    }

    public void setSizeFull() {
        final Extras x = wx();
        x.width = HUNDRED_PERCENT;
        x.height = HUNDRED_PERCENT;
        saveUpdate(writer -> writer.write(ServerToClientModel.WIDGET_FULL_SIZE));
    }

    public void removeDomHandler(final EventHandler handler, final PDomEvent.Type type) {
        if (destroy) return;
        if (!doRemoveDomHandler(handler, type)) {
            log.warn("No event handler of type {} found for {}", type, this);
        }
        //else{
        // TODO Handle remove DOM handler
        // if (eventBus.getHandlers(type, this).isEmpty()) {
        //     executeRemoveDomHandler(type);
        //     if (initialized) executeRemoveDomHandler(type);
        //     else safeStackedInstructions().add(() -> executeRemoveDomHandler(type));
        // }
        //}
    }

    private boolean doRemoveDomHandler(final EventHandler handler, final PDomEvent.Type type) {
        if (eventSource == null) return false;
        return eventSource.removeHandler(type, handler);
    }

    public HandlerRegistration addKeyPressHandler(final PKeyPressHandler handler) {
        final Integer[] filteredKeys = handler.getJsonFilteredKeys();
        if (filteredKeys != null)
            return addDomHandler(handler, PKeyPressEvent.TYPE, new ServerBinaryModel(ServerToClientModel.KEY_FILTER, filteredKeys));
        else return addDomHandler(handler, PKeyPressEvent.TYPE);
    }

    public HandlerRegistration addKeyUpHandler(final PKeyUpHandler handler) {
        final Integer[] filteredKeys = handler.getJsonFilteredKeys();
        if (filteredKeys != null)
            return addDomHandler(handler, PKeyUpEvent.TYPE, new ServerBinaryModel(ServerToClientModel.KEY_FILTER, filteredKeys));
        else return addDomHandler(handler, PKeyUpEvent.TYPE);
    }

    public HandlerRegistration addKeyDownHandler(final PKeyDownEvent.Handler handler) {
        final Integer[] filteredKeys = handler.getJsonFilteredKeys();
        if (filteredKeys != null)
            return addDomHandler(handler, PKeyDownEvent.TYPE, new ServerBinaryModel(ServerToClientModel.KEY_FILTER, filteredKeys));
        else return addDomHandler(handler, PKeyDownEvent.TYPE);
    }

    /**
     * Listens to a DOM event of this widget by its name ({@code input}, {@code pointerenter}, {@code wheel}...): the
     * terminal sends the fields the options name, at the rate they set (docs/decisions/0003-protocol-v4-listeners.md).
     * Options given again for a name replace the previous ones, for every listener of the name. The last listener of
     * a name removed, the terminal stops listening. A custom event's detail alone: {@code PWebComponent}'s
     * {@code addEventListener(name, Consumer<JsonValue>)}.
     *
     * @throws IllegalStateException when the context speaks a protocol below 4 ({@code maxProtocolVersion}), whose
     *         terminal cannot listen to events by name
     */
    public HandlerRegistration addEventListener(final String name, final PEventOptions options, final Consumer<PEvent> listener) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(listener, "listener");
        final int protocol = UIContext.get().getProtocolVersion();
        if (protocol < ProtocolVersion.LISTENERS) {
            throw new IllegalStateException(this + ": listening to '" + name + "' needs protocol " + ProtocolVersion.LISTENERS
                    + ", the context speaks " + protocol + " (ApplicationConfiguration.setMaxProtocolVersion)");
        }
        final Extras x = wx();
        if (x.eventListening == null) x.eventListening = new HashMap<>(2);
        EventListening listening = x.eventListening.get(name);
        final boolean tell = listening == null || !listening.options.equals(options);
        if (listening == null) x.eventListening.put(name, listening = new EventListening(options));
        listening.options = options;
        listening.listeners.add(listener);
        if (tell) saveListening(ServerToClientModel.LISTEN_EVENT, options.toJson(name));
        return () -> removeEventListener(name, listener);
    }

    private void removeEventListener(final String name, final Consumer<PEvent> listener) {
        final Map<String, EventListening> all = wx.eventListening; // created with the registration, never cleared
        final EventListening listening = all.get(name);
        if (listening == null || !listening.listeners.remove(listener) || !listening.listeners.isEmpty()) return;
        all.remove(name);
        saveListening(ServerToClientModel.UNLISTEN_EVENT, name);
    }

    /** Not coalesced: two names in one round each reach the terminal. */
    private void saveListening(final ServerToClientModel model, final String value) {
        saveUpdate(writer -> writer.write(model, value));
    }

    private void fireNamedEvent(final JsonObject instruction) {
        final Map<String, EventListening> all = wx != null ? wx.eventListening : null;
        final String name = instruction.getString(EVENT_KEY);
        final EventListening listening = all != null ? all.get(name) : null;
        if (listening == null) return; // removed meanwhile
        final JsonObject fields = instruction.containsKey(EVENT_FIELDS_KEY) ? instruction.getJsonObject(EVENT_FIELDS_KEY)
                : JsonValue.EMPTY_JSON_OBJECT;
        final PEvent event = new PEvent(this, name, fields);
        for (final Consumer<PEvent> listener : List.copyOf(listening.listeners)) listener.accept(event);
    }

    public HandlerRegistration addDomHandler(final EventHandler handler, final PDomEvent.Type type) {
        return addDomHandler(handler, type, null);
    }

    private HandlerRegistration addDomHandler(final EventHandler handler, final PDomEvent.Type type,
                                              final ServerBinaryModel binaryModel) {
        if (destroy) return null;
        if (eventSource == null) eventSource = new CompactEventSource(); // 56 B for one handler (a HashMap: 160)
        final HandlerRegistration handlerRegistration = eventSource.addHandler(type, handler);

        final SetPool<Type>.ImmutableSet pool = oneTimeHandlerCreation.getAdd(type);
        if (pool != oneTimeHandlerCreation) {
            oneTimeHandlerCreation = pool;
            final ModelWriterCallback callback = writer -> {
                writer.write(ServerToClientModel.HANDLER_TYPE, DomHandlerConverter.convert(type.getDomHandlerType()).getValue());
                if (binaryModel != null) writer.write(binaryModel.getKey(), binaryModel.getValue());
            };
            if (initialized) writeAddHandler(callback);
            else safeStackedInstructions().append(() -> writeAddHandler(callback));
        }

        return handlerRegistration;
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (!isVisible()) return;
        if (instruction.containsKey(EVENT_KEY)) {
            fireNamedEvent(instruction);
        } else if (instruction.containsKey(DOM_HANDLER_TYPE_KEY)) {
            final DomHandlerType domHandler = DomHandlerType.fromRawValue((byte) instruction.getInt(DOM_HANDLER_TYPE_KEY));
            switch (domHandler) {
                case KEY_PRESS -> fireKeyEvent(new PKeyPressEvent(this, instruction.getInt(VALUE_KEY_KEY)));
                case KEY_UP -> fireKeyEvent(new PKeyUpEvent(this, instruction.getInt(VALUE_KEY_KEY)));
                case KEY_DOWN -> fireKeyEvent(new PKeyDownEvent(this, instruction.getInt(VALUE_KEY_KEY)));
                case CLICK -> fireMouseEvent(instruction, new PClickEvent(this));
                case DOUBLE_CLICK -> fireMouseEvent(instruction, new PDoubleClickEvent(this));
                case MOUSE_OVER -> fireMouseEvent(instruction, new PMouseOverEvent(this));
                case MOUSE_OUT -> fireMouseEvent(instruction, new PMouseOutEvent(this));
                case MOUSE_DOWN -> fireMouseEvent(instruction, new PMouseDownEvent(this));
                case MOUSE_UP -> fireMouseEvent(instruction, new PMouseUpEvent(this));
                case MOUSE_WHELL -> fireMouseWheelEvent(instruction, new PMouseWhellEvent.Event(this));
                case FOCUS -> fireEvent(new PFocusEvent(this));
                case BLUR -> fireEvent(new PBlurEvent(this));
                case DRAG_START -> fireEvent(new PDragStartEvent(this));
                case DRAG_END -> fireEvent(new PDragEndEvent(this));
                case DRAG_ENTER -> fireEvent(new PDragEnterEvent(this));
                case DRAG_LEAVE -> fireEvent(new PDragLeaveEvent(this));
                case DRAG_OVER -> fireEvent(new PDragOverEvent(this));
                case DROP -> {
                    final PDropEvent dropEvent = new PDropEvent(this);
                    if (instruction.containsKey(DRAG_SRC_KEY)) {
                        final PWidget source = (PWidget) UIContext.get().getObject(instruction.getJsonNumber(DRAG_SRC_KEY).intValue());
                        dropEvent.setDragSource(source);
                    }
                    fireEvent(dropEvent);
                }
                case CONTEXT_MENU -> fireEvent(new PContextMenuEvent(this));
                default -> log.error("Dom Handler not implemented: {}", domHandler);
            }
        } else if (instruction.containsKey(HANDLER_WIDGET_VISIBILITY_KEY)) {
            final boolean shown = instruction.getBoolean(HANDLER_WIDGET_VISIBILITY_KEY);
            if (shown || wx != null) wx().shown = shown;
            final List<PVisibilityHandler> visibilityHandlers = wx != null ? wx.visibilityHandlers : null;
            if (visibilityHandlers != null) {
                final PVisibilityEvent visibilityEvent = new PVisibilityEvent(this, shown);
                visibilityHandlers.forEach(handler -> {
                    if (handler != null) {
                        handler.onVisibility(visibilityEvent);
                    }
                });
            }
        } else {
            super.onClientData(instruction);
        }
    }

    public boolean isShown() {
        return wx != null && wx.shown;
    }

    public void fireKeyEvent(final PKeyEvent<? extends EventHandler> event) {
        fireEvent(event);
    }

    public void fireMouseEvent(final JsonObject instruction, final PMouseEvent<? extends EventHandler> event) {
        if (instruction.containsKey(EVENT_INFO_KEY)) {
            final JsonArray eventInfo = instruction.getJsonArray(EVENT_INFO_KEY);

            event.setX(((JsonNumber) eventInfo.get(0)).intValue());
            event.setY(((JsonNumber) eventInfo.get(1)).intValue());
            event.setClientX(((JsonNumber) eventInfo.get(2)).intValue());
            event.setClientY(((JsonNumber) eventInfo.get(3)).intValue());
            event.setNativeButton(((JsonNumber) eventInfo.get(4)).intValue());
            event.setControlKeyDown(JsonValue.TRUE.equals(eventInfo.get(5)));
            event.setAltKeyDown(JsonValue.TRUE.equals(eventInfo.get(6)));
            event.setShiftKeyDown(JsonValue.TRUE.equals(eventInfo.get(7)));
            event.setMetaKeyDown(JsonValue.TRUE.equals(eventInfo.get(8)));
        }

        if (instruction.containsKey(WIDGET_POSITION_KEY)) {
            final JsonArray widgetInfo = instruction.getJsonArray(WIDGET_POSITION_KEY);

            event.setSourceAbsoluteLeft(((JsonNumber) widgetInfo.get(0)).intValue());
            event.setSourceAbsoluteTop(((JsonNumber) widgetInfo.get(1)).intValue());
            event.setSourceOffsetHeight(((JsonNumber) widgetInfo.get(2)).intValue());
            event.setSourceOffsetWidth(((JsonNumber) widgetInfo.get(3)).intValue());
        }

        fireEvent(event);
    }

    private void fireMouseWheelEvent(final JsonObject instruction, final PMouseWhellEvent.Event event) {
        if (instruction.containsKey(EVENT_INFO_KEY)) {
            final JsonArray eventInfo = instruction.getJsonArray(EVENT_INFO_KEY);

            event.setX(((JsonNumber) eventInfo.get(0)).intValue());
            event.setY(((JsonNumber) eventInfo.get(1)).intValue());
            event.setClientX(((JsonNumber) eventInfo.get(2)).intValue());
            event.setClientY(((JsonNumber) eventInfo.get(3)).intValue());
            event.setNativeButton(((JsonNumber) eventInfo.get(4)).intValue());
            event.setDeltaY(((JsonNumber) eventInfo.get(5)).intValue());
        }

        if (instruction.containsKey(WIDGET_POSITION_KEY)) {
            final JsonArray widgetInfo = instruction.getJsonArray(WIDGET_POSITION_KEY);

            event.setSourceAbsoluteLeft(((JsonNumber) widgetInfo.get(0)).intValue());
            event.setSourceAbsoluteTop(((JsonNumber) widgetInfo.get(1)).intValue());
            event.setSourceOffsetHeight(((JsonNumber) widgetInfo.get(2)).intValue());
            event.setSourceOffsetWidth(((JsonNumber) widgetInfo.get(3)).intValue());
        }

        fireEvent(event);
    }

    public void fireEvent(final Event<? extends EventHandler> event) {
        if (eventSource == null) return;
        UIContext.get().getRootEventBus().fireEventFromSource(event, eventSource);
    }

    public void removeFromParent() {
        if (parent instanceof HasPWidgets) ((HasPWidgets) parent).remove(this);
        else if (parent != null) throw new IllegalStateException("This widget's parent does not implement HasPWidgets");
    }

    void bindAddon(final PAddOn addon) {
        final Extras x = wx();
        if (x.addons == null) x.addons = SetUtils.newArraySet(4);
        x.addons.add(addon);
    }

    public Set<PAddOn> getAddons() {
        return wx != null ? wx.addons : null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (wx != null && wx.addons != null) {
            wx.addons.forEach(this::doDestroy);
        }
    }

    private void doDestroy(final PAddOn addon) {
        try {
            addon.onDestroy();
        } catch (final Exception e) {
            log.error("An error occurred while trying to process the destroy event", e);
        }
    }

    public void focus() {
        focus(true);
    }

    public void blur() {
        focus(false);
    }

    public void focusPreventScroll() {
        saveUpdate(ServerToClientModel.FOCUS_PREVENT_SCROLL, null);
    }

    private void focus(final boolean focused) {
        saveUpdate(ServerToClientModel.FOCUS, focused);
    }

    public void setTabindex(final TabindexMode tabindexMode) {
        setTabindex(tabindexMode.getTabIndex());
    }

    public void setTabindex(final int tabindex) {
        if (Objects.equals(this.tabindex, tabindex)) return;
        this.tabindex = tabindex;
        saveUpdate(ServerToClientModel.TABINDEX, tabindex);
    }

    public int getTabindex() {
        return tabindex;
    }

    public void addVisibilityHandler(final PVisibilityHandler visibilityHandler) {
        final Extras x = wx();
        if (x.visibilityHandlers == null) {
            x.visibilityHandlers = new ArrayList<>(1);
            saveAddHandler(HandlerModel.HANDLER_VISIBILITY);
        }
        x.visibilityHandlers.add(visibilityHandler);
    }

    public void removeVisibilityHandler(final PVisibilityHandler visibilityHandler) {
        final List<PVisibilityHandler> visibilityHandlers = wx != null ? wx.visibilityHandlers : null;
        if (visibilityHandlers != null) {
            visibilityHandlers.remove(visibilityHandler);
            if (visibilityHandlers.isEmpty()) saveRemoveHandler(HandlerModel.HANDLER_VISIBILITY);
        }
    }

    public Stream<String> getStyleNames() {
        return styleNames.stream();
    }

    public abstract String dumpDOM();

}
