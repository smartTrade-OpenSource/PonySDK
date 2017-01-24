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
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.ui.basic.event.HasPHandlers;
import com.ponysdk.core.ui.basic.event.HasPWidgets;
import com.ponysdk.core.ui.basic.event.PBlurEvent;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PContextMenuEvent;
import com.ponysdk.core.ui.basic.event.PDomEvent;
import com.ponysdk.core.ui.basic.event.PDoubleClickEvent;
import com.ponysdk.core.ui.basic.event.PDragEndEvent;
import com.ponysdk.core.ui.basic.event.PDragEnterEvent;
import com.ponysdk.core.ui.basic.event.PDragLeaveEvent;
import com.ponysdk.core.ui.basic.event.PDragOverEvent;
import com.ponysdk.core.ui.basic.event.PDragStartEvent;
import com.ponysdk.core.ui.basic.event.PDropEvent;
import com.ponysdk.core.ui.basic.event.PFocusEvent;
import com.ponysdk.core.ui.basic.event.PKeyPressEvent;
import com.ponysdk.core.ui.basic.event.PKeyPressFilterHandler;
import com.ponysdk.core.ui.basic.event.PKeyUpEvent;
import com.ponysdk.core.ui.basic.event.PKeyUpFilterHandler;
import com.ponysdk.core.ui.basic.event.PMouseDownEvent;
import com.ponysdk.core.ui.basic.event.PMouseEvent;
import com.ponysdk.core.ui.basic.event.PMouseOutEvent;
import com.ponysdk.core.ui.basic.event.PMouseOverEvent;
import com.ponysdk.core.ui.basic.event.PMouseUpEvent;
import com.ponysdk.core.ui.eventbus.Event;
import com.ponysdk.core.ui.eventbus.EventBus;
import com.ponysdk.core.ui.eventbus.EventHandler;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.eventbus.SimpleEventBus;
import com.ponysdk.core.ui.model.PEventType;
import com.ponysdk.core.ui.model.ServerBinaryModel;
import com.ponysdk.core.writer.ModelWriter;

/**
 * The base class for the majority of user-interface objects. Widget adds
 * support for receiving events from the browser and being added directly to
 * {@link PPanel panels}.
 */
public abstract class PWidget extends PObject implements IsPWidget, HasPHandlers {

    private static final Logger log = LoggerFactory.getLogger(PWidget.class);

    private static final String HUNDRED_PERCENT = "100%";

    protected Object data;
    boolean visible = true;
    private IsPWidget parent;
    private Set<String> styleNames;
    private Set<PEventType> preventEvents;
    private Set<PEventType> stopEvents;
    private EventBus domHandler;
    private Map<String, String> styleProperties;
    private Map<String, String> elementProperties;
    private Map<String, String> elementAttributes;
    private String title;
    private String width;
    private String height;
    private String styleName;
    private String stylePrimaryName;
    private String debugID;

    private PAddOn addon;

    static PWidget asWidgetOrNull(final IsPWidget w) {
        return w == null ? null : w.asWidget();
    }

    private Set<String> safeStyleName() {
        if (styleNames == null) styleNames = new HashSet<>();
        return styleNames;
    }

    private Set<PEventType> safePreventEvents() {
        if (preventEvents == null) preventEvents = new HashSet<>();
        return preventEvents;
    }

    private Set<PEventType> safeStopEvents() {
        if (stopEvents == null) stopEvents = new HashSet<>();
        return stopEvents;
    }

    private Map<String, String> safeStyleProperties() {
        if (styleProperties == null) styleProperties = new HashMap<>();
        return styleProperties;
    }

    private Map<String, String> safeElementProperties() {
        if (elementProperties == null) elementProperties = new HashMap<>();
        return elementProperties;
    }

    private Map<String, String> safeElementAttributes() {
        if (elementAttributes == null) elementAttributes = new HashMap<>();
        return elementAttributes;
    }

    public void ensureDebugId(final String debugID) {
        if (Objects.equals(this.debugID, debugID)) return;
        this.debugID = debugID;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.ENSURE_DEBUG_ID, debugID));
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        if (Objects.equals(this.title, title)) return;
        this.title = title;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.WIDGET_TITLE, title));
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(final boolean visible) {
        if (Objects.equals(this.visible, visible)) return;
        this.visible = visible;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.WIDGET_VISIBLE, visible));
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(final String width) {
        if (Objects.equals(this.width, width)) return;
        this.width = width;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.WIDGET_WIDTH, width));
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(final String height) {
        if (Objects.equals(this.height, height)) return;
        this.height = height;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.WIDGET_HEIGHT, height));
    }

    public String getStyleName() {
        return styleName;
    }

    public void setStyleName(final String styleName) {
        if (Objects.equals(this.styleName, styleName)) return;
        this.styleName = styleName;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.STYLE_NAME, styleName));
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
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.STYLE_PRIMARY_NAME, stylePrimaryName));
    }

    public IsPWidget getParent() {
        return parent;
    }

    void setParent(final IsPWidget parent) {
        this.parent = parent;
    }

    public void setStyleProperty(final String name, final String value) {
        if (!Objects.equals(safeStyleProperties().put(name, value), value)) {
            saveUpdate((writer) -> {
                writer.writeModel(ServerToClientModel.PUT_STYLE_KEY, name);
                writer.writeModel(ServerToClientModel.STYLE_VALUE, value);
            });
        }
    }

    public void removeStyleProperty(final String name) {
        if (safeStyleProperties().remove(name) != null) {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.REMOVE_STYLE_KEY, name));
        }
    }

    public void setProperty(final String name, final String value) {
        if (!Objects.equals(safeElementProperties().put(name, value), value)) {
            saveUpdate((writer) -> {
                writer.writeModel(ServerToClientModel.PUT_PROPERTY_KEY, name);
                writer.writeModel(ServerToClientModel.PROPERTY_VALUE, value);
            });
        }
    }

    public void setAttribute(final String name) {
        setAttribute(name, null);
    }

    public void setAttribute(final String name, final String value) {
        if (name == null) return;

        if (!safeElementAttributes().containsKey(name)) {
            safeElementAttributes().put(name, value);
        } else if (Objects.equals(safeElementAttributes().put(name, value), value)) {
            return;
        }
        saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.PUT_ATTRIBUTE_KEY, name);
            writer.writeModel(ServerToClientModel.ATTRIBUTE_VALUE, value);
        });
    }

    public void removeAttribute(final String name) {
        if (safeElementAttributes().remove(name) != null) {
            saveUpdate((writer) -> writer.writeModel(ServerToClientModel.REMOVE_ATTRIBUTE_KEY, name));
        }
    }

    public String getProperty(final String key) {
        return elementProperties != null ? elementProperties.get(key) : null;
    }

    public String getAttribute(final String key) {
        return elementAttributes != null ? elementAttributes.get(key) : null;
    }

    public void preventEvent(final PEventType e) {
        if (safePreventEvents().add(e)) {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.PREVENT_EVENT, e.getCode()));
        }
    }

    public void stopEvent(final PEventType e) {
        if (safeStopEvents().add(e)) {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.STOP_EVENT, e.getCode()));
        }
    }

    public void addStyleName(final String styleName) {
        if (safeStyleName().add(styleName)) {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.ADD_STYLE_NAME, styleName));
        }
    }

    public void removeStyleName(final String styleName) {
        if (styleNames != null && styleNames.remove(styleName)) removeStyle(styleName);
    }

    private void removeStyle(final String styleName) {
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.REMOVE_STYLE_NAME, styleName));
    }

    public boolean hasStyleName(final String styleName) {
        return styleNames != null && styleNames.contains(styleName);
    }

    public Object getData() {
        return data;
    }

    public void setData(final Object data) {
        this.data = data;
    }

    @Override
    public PWidget asWidget() {
        return this;
    }

    public void setSizeFull() {
        this.width = HUNDRED_PERCENT;
        this.height = HUNDRED_PERCENT;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.WIDGET_FULL_SIZE));
    }

    // public <H extends EventHandler> HandlerRegistration
    // removeDomHandler(final JsonObject handler, final
    // PDomEvent.Type<H> type) {
    // final HandlerRegistration handlerRegistration =
    // ensureDomHandler().addHandler(type, handler);
    //
    // saveRemoveHandler(Model.HANDLER_DOM_HANDLER, Model.DOM_HANDLER_CODE,
    // handler);
    //
    // return handlerRegistration;
    // }

    public <H extends EventHandler> HandlerRegistration removeDomHandler(final H handler, final PDomEvent.Type<H> type) {
        final HandlerRegistration handlerRegistration = ensureDomHandler().addHandler(type, handler);
        saveRemoveHandler(HandlerModel.HANDLER_DOM);
        return handlerRegistration;
    }

    public HandlerRegistration addDomHandler(final PKeyPressFilterHandler handler) {
        return addDomHandler(handler, PKeyPressEvent.TYPE,
                new ServerBinaryModel(ServerToClientModel.KEY_FILTER, handler.asJsonObject()));
    }

    public HandlerRegistration addDomHandler(final PKeyUpFilterHandler handler) {
        return addDomHandler(handler, PKeyUpEvent.TYPE, new ServerBinaryModel(ServerToClientModel.KEY_FILTER, handler.asJsonObject()));
    }

    public <H extends EventHandler> HandlerRegistration addDomHandler(final H handler, final PDomEvent.Type<H> type) {
        return addDomHandler(handler, type, null);
    }

    private <H extends EventHandler> HandlerRegistration addDomHandler(final H handler, final PDomEvent.Type<H> type,
            final ServerBinaryModel binaryModel) {
        final Collection<H> handlerIterator = ensureDomHandler().getHandlers(type, this);
        final HandlerRegistration handlerRegistration = domHandler.addHandlerToSource(type, this, handler);
        if (handlerIterator.isEmpty()) {
            final ServerBinaryModel binaryModel1 = new ServerBinaryModel(ServerToClientModel.DOM_HANDLER_CODE,
                    type.getDomHandlerType().getValue());
            if (windowID != PWindow.EMPTY_WINDOW_ID)
                executeAddDomHandler(binaryModel1, binaryModel);
            else
                stackedInstructions.add(() -> executeAddDomHandler(binaryModel1, binaryModel));
        }
        return handlerRegistration;
    }

    private <H extends EventHandler> void executeAddDomHandler(final ServerBinaryModel... binaryModels) {
        final ModelWriter writer = Txn.getWriter();
        writer.beginObject();
        if (windowID != PWindow.getMain().getID()) writer.writeModel(ServerToClientModel.WINDOW_ID, windowID);
        writer.writeModel(ServerToClientModel.TYPE_ADD_HANDLER, HandlerModel.HANDLER_DOM.getValue());
        writer.writeModel(ServerToClientModel.OBJECT_ID, ID);
        if (binaryModels != null) {
            for (final ServerBinaryModel binaryModel : binaryModels) {
                if (binaryModel != null) writer.writeModel(binaryModel.getKey(), binaryModel.getValue());
            }
        }
        writer.endObject();
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        final String domHandlerType = ClientToServerModel.DOM_HANDLER_TYPE.toStringValue();
        if (instruction.containsKey(domHandlerType)) {
            final DomHandlerType domHandler = DomHandlerType.values()[instruction.getInt(domHandlerType)];
            switch (domHandler) {
                case KEY_PRESS:
                    fireEvent(new PKeyPressEvent(this, instruction.getInt(ClientToServerModel.VALUE_KEY.toStringValue())));
                    break;
                case KEY_UP:
                    fireEvent(new PKeyUpEvent(this, instruction.getInt(ClientToServerModel.VALUE_KEY.toStringValue())));
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
                    final String dragSrc = ClientToServerModel.DRAG_SRC.toStringValue();
                    if (instruction.containsKey(dragSrc)) {
                        final PWidget source = UIContext.get().getObject(instruction.getJsonNumber(dragSrc).intValue());
                        dropEvent.setDragSource(source);
                    }
                    fireEvent(dropEvent);
                    break;
                case CONTEXT_MENU:
                    fireEvent(new PContextMenuEvent(this));
                    break;
                case CHANGE_HANDLER:
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

    public void fireMouseEvent(final JsonObject instruction, final PMouseEvent<?> event) {
        final String eventInfoKey = ClientToServerModel.EVENT_INFO.toStringValue();
        if (instruction.containsKey(eventInfoKey)) {
            final JsonArray eventInfo = instruction.getJsonArray(eventInfoKey);

            event.setX(((JsonNumber) eventInfo.get(0)).intValue());
            event.setY(((JsonNumber) eventInfo.get(1)).intValue());
            event.setClientX(((JsonNumber) eventInfo.get(2)).intValue());
            event.setClientY(((JsonNumber) eventInfo.get(3)).intValue());
            event.setNativeButton(((JsonNumber) eventInfo.get(4)).intValue());
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
    public void fireEvent(final Event<?> event) {
        if (domHandler == null) return;
        domHandler.fireEvent(event);
    }

    public void removeFromParent() {
        if (parent instanceof HasPWidgets)
            ((HasPWidgets) parent).remove(this);
        else if (parent != null) throw new IllegalStateException("This widget's parent does not implement HasPWidgets");
    }

    void bindAddon(final PAddOn addon) {
        this.addon = addon;
    }

    boolean isAddonAlreadyBound(final PAddOn addon) {
        return this.addon != null && !Objects.equals(this.addon, addon);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " #" + ID;
    }

}
