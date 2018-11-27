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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DomHandlerConverter;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.ui.basic.event.HasPHandlers;
import com.ponysdk.core.ui.basic.event.HasPKeyPressHandlers;
import com.ponysdk.core.ui.basic.event.HasPKeyUpHandlers;
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
import com.ponysdk.core.ui.basic.event.PKeyDownHandler;
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
import com.ponysdk.core.ui.eventbus.Event;
import com.ponysdk.core.ui.eventbus.EventHandler;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.model.PEventType;
import com.ponysdk.core.ui.model.ServerBinaryModel;
import com.ponysdk.core.util.SetPool;
import com.ponysdk.core.util.SetUtils;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.core.writer.ModelWriterCallback;

/**
 * The base class for the majority of user-interface objects. Widget adds
 * support for receiving events from the browser and being added directly to
 * {@link PPanel panels}.
 */
public abstract class PWidget extends PObject implements IsPWidget, HasPHandlers, HasPKeyPressHandlers, HasPKeyUpHandlers {

    private static final Logger log = LoggerFactory.getLogger(PWidget.class);
    private static final SetPool<String> styleNamesSetPool = new SetPool<>();
    private static final SetPool<PEventType> preventOrStopEventsSetPool = new SetPool<>();
    private static final SetPool<PDomEvent.Type> oneTimeHandlerCreationSetPool = new SetPool<>();

    private static final String HUNDRED_PERCENT = "100%";

    boolean visible = true;
    private IsPWidget parent;
    private SetPool<String>.ImmutableSet styleNames = styleNamesSetPool.emptyImmutableSet();
    private SetPool<PEventType>.ImmutableSet preventEvents = preventOrStopEventsSetPool.emptyImmutableSet();
    private SetPool<PEventType>.ImmutableSet stopEvents = preventOrStopEventsSetPool.emptyImmutableSet();
    private Map<String, String> styleProperties;
    private Map<String, String> elementProperties;
    private Map<String, String> elementAttributes;
    private String title;
    private String width;
    private String height;
    private String styleName;
    private String stylePrimaryName;
    private String debugID;
    private boolean focused;
    protected int tabindex = -Integer.MAX_VALUE;

    private Set<PAddOn> addons;

    // WORKAROUND Remove handler only server side
    private SetPool<PDomEvent.Type>.ImmutableSet oneTimeHandlerCreation = oneTimeHandlerCreationSetPool.emptyImmutableSet();

    public enum TabindexMode {

        FOCUSABLE(-1),
        TABULABLE(0);

        private int tabIndex;

        private TabindexMode(final int tabIndex) {
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
        if (!styleNames.isEmpty()) {
            writer.write(ServerToClientModel.ADD_STYLE_NAME, styleNames.stream().collect(Collectors.joining(" ")));
        }
    }

    static PWidget asWidgetOrNull(final IsPWidget w) {
        return w == null ? null : w.asWidget();
    }

    private Map<String, String> safeStyleProperties() {
        if (styleProperties == null) styleProperties = new HashMap<>(8);
        return styleProperties;
    }

    private Map<String, String> safeElementProperties() {
        if (elementProperties == null) elementProperties = new HashMap<>(8);
        return elementProperties;
    }

    private Map<String, String> safeElementAttributes() {
        if (elementAttributes == null) elementAttributes = new HashMap<>(8);
        return elementAttributes;
    }

    public void ensureDebugId(final String debugID) {
        if (UIContext.get().getConfiguration().isDebugMode()) {
            if (Objects.equals(this.debugID, debugID)) return;
            this.debugID = debugID;
            saveUpdate(writer -> writer.write(ServerToClientModel.ENSURE_DEBUG_ID, debugID));
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        if (Objects.equals(this.title, title)) return;
        this.title = title;
        saveUpdate(ServerToClientModel.WIDGET_TITLE, title);
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
        saveUpdate(ServerToClientModel.WIDGET_WIDTH, width);
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(final String height) {
        if (Objects.equals(this.height, height)) return;
        this.height = height;
        saveUpdate(ServerToClientModel.WIDGET_HEIGHT, height);
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

    public String getStylePrimaryName() {
        return stylePrimaryName;
    }

    public void setStylePrimaryName(final String stylePrimaryName) {
        if (Objects.equals(this.stylePrimaryName, stylePrimaryName)) return;
        this.stylePrimaryName = stylePrimaryName;
        saveUpdate(ServerToClientModel.STYLE_PRIMARY_NAME, stylePrimaryName);
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
        if (safeStyleProperties().remove(name) != null) saveUpdate(writer -> writer.write(ServerToClientModel.REMOVE_STYLE_KEY, name));
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
        setAttribute(name, null);
    }

    public void setAttribute(final String name, final String value) {
        if (name == null) return;

        // HTML specs is the value is equals to the name if value is null or empty
        final String newValue = value != null && !value.isEmpty() ? value : name.toLowerCase();

        if (!safeElementAttributes().containsKey(name)) safeElementAttributes().put(name, newValue);
        else if (Objects.equals(safeElementAttributes().put(name, newValue), newValue)) return;

        saveUpdate(writer -> {
            writer.write(ServerToClientModel.PUT_ATTRIBUTE_KEY, name);
            writer.write(ServerToClientModel.ATTRIBUTE_VALUE, newValue);
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

    public String getAttribute(final String key) {
        return elementAttributes != null ? elementAttributes.get(key) : null;
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
        final Collection<EventHandler> handlers = UIContext.getRootEventBus().getHandlers(type, this);
        if (handlers.contains(handler)) {
            UIContext.removeHandlerFromSource(type, this, handler);
            // TODO Handle remove DOM handler
            // if (eventBus.getHandlers(type, this).isEmpty()) {
            //     executeRemoveDomHandler(type);
            //     if (initialized) executeRemoveDomHandler(type);
            //     else safeStackedInstructions().add(() -> executeRemoveDomHandler(type));
            // }
        } else {
            log.warn("No event handler of type {} found for {}", type, this);
        }
    }

    private void executeRemoveDomHandler(final PDomEvent.Type type) {
        if (destroy) return;
        final ModelWriter writer = Txn.get().getWriter();
        writer.beginObject(window.getID());
        writer.write(ServerToClientModel.TYPE_REMOVE_HANDLER, ID);
        writer.write(ServerToClientModel.HANDLER_TYPE, DomHandlerConverter.convert(type.getDomHandlerType()).getValue());
        writer.endObject();
    }

    @Override
    public HandlerRegistration addKeyPressHandler(final PKeyPressHandler handler) {
        final Integer[] filteredKeys = handler.getJsonFilteredKeys();
        if (filteredKeys != null)
            return addDomHandler(handler, PKeyPressEvent.TYPE, new ServerBinaryModel(ServerToClientModel.KEY_FILTER, filteredKeys));
        else return addDomHandler(handler, PKeyPressEvent.TYPE);
    }

    @Override
    public HandlerRegistration addKeyUpHandler(final PKeyUpHandler handler) {
        final Integer[] filteredKeys = handler.getJsonFilteredKeys();
        if (filteredKeys != null)
            return addDomHandler(handler, PKeyUpEvent.TYPE, new ServerBinaryModel(ServerToClientModel.KEY_FILTER, filteredKeys));
        else return addDomHandler(handler, PKeyUpEvent.TYPE);
    }

    public HandlerRegistration addKeyDownHandler(final PKeyDownHandler handler) {
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
        final HandlerRegistration handlerRegistration = UIContext.addHandlerToSource(type, this, handler);

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
        final String domHandlerType = ClientToServerModel.DOM_HANDLER_TYPE.toStringValue();
        if (instruction.containsKey(domHandlerType)) {
            final DomHandlerType domHandler = DomHandlerType.fromRawValue((byte) instruction.getInt(domHandlerType));
            switch (domHandler) {
                case KEY_PRESS:
                    fireKeyEvent(instruction,
                        new PKeyPressEvent(this, instruction.getInt(ClientToServerModel.VALUE_KEY.toStringValue())));
                    break;
                case KEY_UP:
                    fireKeyEvent(instruction,
                        new PKeyUpEvent(this, instruction.getInt(ClientToServerModel.VALUE_KEY.toStringValue())));
                    break;
                case KEY_DOWN:
                    fireKeyEvent(instruction,
                        new PKeyDownEvent(this, instruction.getInt(ClientToServerModel.VALUE_KEY.toStringValue())));
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
                case MOUSE_WHELL:
                    fireMouseWheelEvent(instruction, new PMouseWhellEvent.Event(this));
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
                    final String dragSrc = ClientToServerModel.DRAG_SRC.toStringValue();
                    if (instruction.containsKey(dragSrc)) {
                        final PWidget source = (PWidget) UIContext.get().getObject(instruction.getJsonNumber(dragSrc).intValue());
                        dropEvent.setDragSource(source);
                    }
                    fireEvent(dropEvent);
                    break;
                case CONTEXT_MENU:
                    fireEvent(new PContextMenuEvent(this));
                    break;
                case CHANGE_HANDLER:
                default:
                    log.error("Dom Handler not implemented: {}", domHandler);
                    break;
            }
        } else {
            super.onClientData(instruction);
        }
    }

    public void fireKeyEvent(final JsonObject instruction, final PKeyEvent<? extends EventHandler> event) {
        fireEvent(event);
    }

    public void fireMouseEvent(final JsonObject instruction, final PMouseEvent<? extends EventHandler> event) {
        final String eventInfoKey = ClientToServerModel.EVENT_INFO.toStringValue();
        if (instruction.containsKey(eventInfoKey)) {
            final JsonArray eventInfo = instruction.getJsonArray(eventInfoKey);

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

        final String widgetPositionKey = ClientToServerModel.WIDGET_POSITION.toStringValue();
        if (instruction.containsKey(widgetPositionKey)) {
            final JsonArray widgetInfo = instruction.getJsonArray(widgetPositionKey);

            event.setSourceAbsoluteLeft(((JsonNumber) widgetInfo.get(0)).intValue());
            event.setSourceAbsoluteTop(((JsonNumber) widgetInfo.get(1)).intValue());
            event.setSourceOffsetHeight(((JsonNumber) widgetInfo.get(2)).intValue());
            event.setSourceOffsetWidth(((JsonNumber) widgetInfo.get(3)).intValue());
        }

        fireEvent(event);
    }

    private void fireMouseWheelEvent(final JsonObject instruction, final PMouseWhellEvent.Event event) {
        final String eventInfoKey = ClientToServerModel.EVENT_INFO.toStringValue();
        if (instruction.containsKey(eventInfoKey)) {
            final JsonArray eventInfo = instruction.getJsonArray(eventInfoKey);

            event.setX(((JsonNumber) eventInfo.get(0)).intValue());
            event.setY(((JsonNumber) eventInfo.get(1)).intValue());
            event.setClientX(((JsonNumber) eventInfo.get(2)).intValue());
            event.setClientY(((JsonNumber) eventInfo.get(3)).intValue());
            event.setNativeButton(((JsonNumber) eventInfo.get(4)).intValue());
            event.setDeltaY(((JsonNumber) eventInfo.get(5)).intValue());
        }

        final String widgetPositionKey = ClientToServerModel.WIDGET_POSITION.toStringValue();
        if (instruction.containsKey(widgetPositionKey)) {
            final JsonArray widgetInfo = instruction.getJsonArray(widgetPositionKey);

            event.setSourceAbsoluteLeft(((JsonNumber) widgetInfo.get(0)).intValue());
            event.setSourceAbsoluteTop(((JsonNumber) widgetInfo.get(1)).intValue());
            event.setSourceOffsetHeight(((JsonNumber) widgetInfo.get(2)).intValue());
            event.setSourceOffsetWidth(((JsonNumber) widgetInfo.get(3)).intValue());
        }

        fireEvent(event);
    }

    @Override
    public void fireEvent(final Event<? extends EventHandler> event) {
        UIContext.fireEventFromSource(event, this);
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
        if (addons != null) addons.forEach(PAddOn::onDestroy);
    }

    public void focus() {
        focus(true);
    }

    public void blur() {
        focus(false);
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

    /**
     * @deprecated Use {@link #getAddons()} instead
     * @return First binded addon
     */
    @Deprecated
    public PAddOn getAddon() {
        return addons != null && !addons.isEmpty() ? addons.iterator().next() : null;
    }

    /**
     * @deprecated Add a FocusHandler if you want to know the focused state
     */
    @Deprecated
    public boolean isFocused() {
        return focused;
    }

}
