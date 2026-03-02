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

import com.ponysdk.core.model.*;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.event.*;
import com.ponysdk.core.ui.basic.event.PDomEvent.Type;
import com.ponysdk.core.ui.basic.event.PVisibilityEvent.PVisibilityHandler;
import com.ponysdk.core.ui.eventbus.*;
import com.ponysdk.core.ui.model.PEventType;
import com.ponysdk.core.ui.model.ServerBinaryModel;
import com.ponysdk.core.util.CompactStringMap;
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

    boolean visible = true;
    private IsPWidget parent;
    private SetPool<String>.ImmutableSet styleNames = styleNamesSetPool.emptyImmutableSet();
    private SetPool<PEventType>.ImmutableSet preventEvents = preventOrStopEventsSetPool.emptyImmutableSet();
    private SetPool<PEventType>.ImmutableSet stopEvents = preventOrStopEventsSetPool.emptyImmutableSet();
    private CompactStringMap styleProperties;
    private CompactStringMap elementProperties;
    private CompactStringMap elementAttributes;
    private String title;
    private String width;
    private String height;
    private String styleName;
    private String debugID;
    private boolean focused;
    protected int tabindex = -Integer.MAX_VALUE;
    private List<PVisibilityHandler> visibilityHandlers;
    private EventSource eventSource;

    private Set<PAddOn> addons;

    // WORKAROUND Remove handler only server side
    private SetPool<PDomEvent.Type>.ImmutableSet oneTimeHandlerCreation = oneTimeHandlerCreationSetPool.emptyImmutableSet();

    private boolean shown;

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
        if (this.styleName != null) writer.write(ServerToClientModel.STYLE_NAME, this.styleName);
        if (!styleNames.isEmpty()) {
            writer.write(ServerToClientModel.ADD_STYLE_NAME, styleNames.stream().collect(Collectors.joining(" ")));
        }
        if (this.title != null) writer.write(ServerToClientModel.WIDGET_TITLE, this.title);
        if (this.height != null) writer.write(ServerToClientModel.WIDGET_HEIGHT, this.height);
        if (this.width != null) writer.write(ServerToClientModel.WIDGET_WIDTH, this.width);
        if (this.debugID != null) writer.write(ServerToClientModel.ENSURE_DEBUG_ID, this.debugID);
        if (styleProperties != null) {
            for (final var entry : styleProperties.entrySet()) {
                writer.write(ServerToClientModel.PUT_STYLE_KEY, entry.getKey());
                writer.write(ServerToClientModel.STYLE_VALUE, entry.getValue());
            }
        }
        if (elementAttributes != null) {
            for (final var entry : elementAttributes.entrySet()) {
                writer.write(ServerToClientModel.PUT_ATTRIBUTE_KEY, entry.getKey());
                writer.write(ServerToClientModel.ATTRIBUTE_VALUE, entry.getValue());
            }
        }
        if (elementProperties != null) {
            for (final var entry : elementProperties.entrySet()) {
                writer.write(ServerToClientModel.PUT_PROPERTY_KEY, entry.getKey());
                writer.write(ServerToClientModel.PROPERTY_VALUE, entry.getValue());
            }
        }
    }

    static PWidget asWidgetOrNull(final IsPWidget w) {
        return w == null ? null : w.asWidget();
    }

    private CompactStringMap safeStyleProperties() {
        if (styleProperties == null) styleProperties = new CompactStringMap();
        return styleProperties;
    }

    private CompactStringMap safeElementProperties() {
        if (elementProperties == null) elementProperties = new CompactStringMap();
        return elementProperties;
    }

    private CompactStringMap safeElementAttributes() {
        if (elementAttributes == null) elementAttributes = new CompactStringMap();
        return elementAttributes;
    }

    public void ensureDebugId(final String debugID) {
        if (UIContext.get().getConfiguration().isDebugMode()) {
            if (Objects.equals(this.debugID, debugID)) return;
            this.debugID = debugID;
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
        saveUpdate(ServerToClientModel.WIDGET_VISIBLE, visible);
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(final String width) {
        if (Objects.equals(this.width, width)) return;
        this.width = width;
        if (initialized) saveUpdate(ServerToClientModel.WIDGET_WIDTH, width);
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(final String height) {
        if (Objects.equals(this.height, height)) return;
        this.height = height;
        if (initialized) saveUpdate(ServerToClientModel.WIDGET_HEIGHT, height);
    }

    public String getStyleName() {
        return styleName;
    }

    public void setStyleName(final String styleName) {
        if (Objects.equals(this.styleName, styleName)) return;
        if (styleName != null && !styleName.isEmpty() && doAddStyleName(styleName) && initialized) {
            this.styleName = styleName;
            saveUpdate(ServerToClientModel.STYLE_NAME, styleName);
        }
    }

    public String getDebugID() {
        return debugID;
    }

    public IsPWidget getParent() {
        return parent;
    }

    void setParent(final IsPWidget parent) {
        this.parent = parent;
    }

    public void setStyleProperty(final String name, final String value) {
        if (!Objects.equals(safeStyleProperties().put(name, value), value)) {
            saveUpdate(writer -> {
                writer.write(ServerToClientModel.PUT_STYLE_KEY, name);
                writer.write(ServerToClientModel.STYLE_VALUE, value);
            });
        }
    }

    public void removeStyleProperty(final String name) {
        if (safeStyleProperties().remove(name) != null)
            saveUpdate(writer -> writer.write(ServerToClientModel.REMOVE_STYLE_KEY, name));
    }

    public void forceDomId() {
        final String idValue = String.valueOf(ID);
        if (!Objects.equals(safeElementProperties().put("id", idValue), idValue)) {
            saveUpdate(ServerToClientModel.FORCE_DOM_ID, null);
        }
    }

    public void setProperty(final String name, final String value) {
        if (!Objects.equals(safeElementProperties().put(name, value), value)) {
            saveUpdate(writer -> {
                writer.write(ServerToClientModel.PUT_PROPERTY_KEY, name);
                writer.write(ServerToClientModel.PROPERTY_VALUE, value);
            });
        }
    }

    public void setAttribute(final String name) {
        setAttribute(name, "");
    }

    public void setAttribute(final String name, final String value) {
        if (name == null) return;

        if (Objects.equals(safeElementAttributes().put(name, value), value)) return;

        saveUpdate(writer -> {
            writer.write(ServerToClientModel.PUT_ATTRIBUTE_KEY, name);
            writer.write(ServerToClientModel.ATTRIBUTE_VALUE, value);
        });
    }

    public void removeAttribute(final String name) {
        if (safeElementAttributes().remove(name) != null) {
            saveUpdate(writer -> writer.write(ServerToClientModel.REMOVE_ATTRIBUTE_KEY, name));
        }
    }

    public String getProperty(final String key) {
        return elementProperties != null ? elementProperties.get(key) : null;
    }

    public boolean hasAttribute(final String key) {
        return elementAttributes != null && elementAttributes.containsKey(key);
    }

    public String getAttribute(final String key) {
        return elementAttributes != null ? elementAttributes.get(key) : null;
    }

    public Set<Map.Entry<String, String>> getAttributes() {
        return elementAttributes == null ? Collections.emptySet() : elementAttributes.entrySet();
    }

    public void preventEvent(final PEventType e) {
        final SetPool<PEventType>.ImmutableSet pool = preventEvents.getAdd(e);
        if (pool != preventEvents) {
            preventEvents = pool;
            saveUpdate(writer -> writer.write(ServerToClientModel.PREVENT_EVENT, e.getCode()));
        }
    }

    public void stopEvent(final PEventType e) {
        final SetPool<PEventType>.ImmutableSet pool = stopEvents.getAdd(e);
        if (pool != stopEvents) {
            stopEvents = pool;
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
            saveUpdate(writer -> writer.write(ServerToClientModel.ADD_STYLE_NAME, styleName));
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
            saveUpdate(writer -> writer.write(ServerToClientModel.REMOVE_STYLE_NAME, styleName));
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
        this.width = HUNDRED_PERCENT;
        this.height = HUNDRED_PERCENT;
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

    public HandlerRegistration addDomHandler(final EventHandler handler, final PDomEvent.Type type) {
        return addDomHandler(handler, type, null);
    }

    private HandlerRegistration addDomHandler(final EventHandler handler, final PDomEvent.Type type,
                                              final ServerBinaryModel binaryModel) {
        if (destroy) return null;
        if (eventSource == null) eventSource = new CompactEventSource();
        final HandlerRegistration handlerRegistration = eventSource.addHandler(type, handler);

        final SetPool<Type>.ImmutableSet pool = oneTimeHandlerCreation.getAdd(type);
        if (pool != oneTimeHandlerCreation) {
            oneTimeHandlerCreation = pool;
            final ModelWriterCallback callback = writer -> {
                writer.write(ServerToClientModel.HANDLER_TYPE, DomHandlerConverter.convert(type.getDomHandlerType()).getValue());
                if (binaryModel != null) writer.write(binaryModel.getKey(), binaryModel.getValue());
            };
            if (initialized) writeAddHandler(callback);
            else safeStackedInstructions().put(saveKey++, () -> writeAddHandler(callback));
        }

        return handlerRegistration;
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (!isVisible()) return;
        if (instruction.containsKey(DOM_HANDLER_TYPE_KEY)) {
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
            shown = instruction.getBoolean(HANDLER_WIDGET_VISIBILITY_KEY);
            if (visibilityHandlers != null) {
                final PVisibilityEvent visibilityEvent = new PVisibilityEvent(this, shown);
                visibilityHandlers.forEach(handler -> handler.onVisibility(visibilityEvent));
            }
        } else {
            super.onClientData(instruction);
        }
    }

    public boolean isShown() {
        return shown;
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
        if (this.addons == null) this.addons = SetUtils.newArraySet(4);
        this.addons.add(addon);
    }

    public Set<PAddOn> getAddons() {
        return addons;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (addons != null) {
            addons.forEach(this::doDestroy);
        }
    }

    private void doDestroy(PAddOn addon) {
        try {
            addon.onDestroy();
        } catch (Exception e) {
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
        this.focused = true;
        saveUpdate(ServerToClientModel.FOCUS_PREVENT_SCROLL, null);
    }

    private void focus(final boolean focused) {
        this.focused = focused;
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
        if (visibilityHandlers == null) {
            visibilityHandlers = new ArrayList<>(1);
            saveAddHandler(HandlerModel.HANDLER_VISIBILITY);
        }
        visibilityHandlers.add(visibilityHandler);
    }

    public void removeVisibilityHandler(final PVisibilityHandler visibilityHandler) {
        if (visibilityHandlers != null) {
            visibilityHandlers.remove(visibilityHandler);
            if (visibilityHandlers.isEmpty()) saveRemoveHandler(HandlerModel.HANDLER_VISIBILITY);
        }
    }

    public Stream<String> getStyleNames() {
        return styleNames.stream();
    }

    /**
     * @deprecated Add a FocusHandler if you want to know the focused state
     */
    @Deprecated
    public boolean isFocused() {
        return focused;
    }

    public abstract String dumpDOM();

}
