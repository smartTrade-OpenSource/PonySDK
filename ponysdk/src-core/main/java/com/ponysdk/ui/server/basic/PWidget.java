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
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.Parser;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.event.Event;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.event.EventHandler;
import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.core.event.SimpleEventBus;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.server.basic.event.HasPWidgets;
import com.ponysdk.ui.server.basic.event.PBlurEvent;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PContextMenuEvent;
import com.ponysdk.ui.server.basic.event.PDomEvent;
import com.ponysdk.ui.server.basic.event.PDomEvent.Type;
import com.ponysdk.ui.server.basic.event.PDoubleClickEvent;
import com.ponysdk.ui.server.basic.event.PDragEndEvent;
import com.ponysdk.ui.server.basic.event.PDragEnterEvent;
import com.ponysdk.ui.server.basic.event.PDragLeaveEvent;
import com.ponysdk.ui.server.basic.event.PDragOverEvent;
import com.ponysdk.ui.server.basic.event.PDragStartEvent;
import com.ponysdk.ui.server.basic.event.PDropEvent;
import com.ponysdk.ui.server.basic.event.PFocusEvent;
import com.ponysdk.ui.server.basic.event.PKeyPressEvent;
import com.ponysdk.ui.server.basic.event.PKeyPressFilterHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpFilterHandler;
import com.ponysdk.ui.server.basic.event.PMouseDownEvent;
import com.ponysdk.ui.server.basic.event.PMouseEvent;
import com.ponysdk.ui.server.basic.event.PMouseOutEvent;
import com.ponysdk.ui.server.basic.event.PMouseOverEvent;
import com.ponysdk.ui.server.basic.event.PMouseUpEvent;
import com.ponysdk.ui.server.model.ServerBinaryModel;
import com.ponysdk.ui.terminal.DomHandlerType;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

/**
 * The base class for the majority of user-interface objects. Widget adds support for receiving
 * events from the browser and being added directly to {@link PPanel panels}.
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

    public PWidget() {
    }

    public PWidget(final int windowID) {
        super(windowID);
    }

    public static PWidget asWidgetOrNull(final IsPWidget w) {
        return w == null ? null : w.asWidget();
    }

    private final Set<String> safeStyleName() {
        if (styleNames != null)
            return styleNames;
        styleNames = new HashSet<>();
        return styleNames;
    }

    private final Set<PEvent> safePreventEvents() {
        if (preventEvents != null)
            return preventEvents;
        preventEvents = new HashSet<>();
        return preventEvents;
    }

    private final Set<PEvent> safeStopEvents() {
        if (stopEvents != null)
            return stopEvents;
        stopEvents = new HashSet<>();
        return stopEvents;
    }

    private final Map<String, String> safeStyleProperties() {
        if (styleProperties != null)
            return styleProperties;
        styleProperties = new HashMap<>();
        return styleProperties;
    }

    private final Map<String, String> safeElementProperties() {
        if (elementProperties != null)
            return elementProperties;
        elementProperties = new HashMap<>();
        return elementProperties;
    }

    private final Map<String, String> safeElementAttributes() {
        if (elementAttributes != null)
            return elementAttributes;
        elementAttributes = new HashMap<>();
        return elementAttributes;
    }

    public void setVisible(final boolean visible) {
        if (Objects.equals(this.visible, visible))
            return;
        this.visible = visible;
        saveUpdate(ServerToClientModel.WIDGET_VISIBLE, visible);
    }

    public void setWidth(final String width) {
        if (Objects.equals(this.width, width))
            return;
        this.width = width;
        saveUpdate(ServerToClientModel.WIDGET_WIDTH, width);
    }

    public void setHeight(final String height) {
        if (Objects.equals(this.height, height))
            return;
        this.height = height;
        saveUpdate(ServerToClientModel.WIDGET_HEIGHT, height);
    }

    public void setTitle(final String title) {
        if (Objects.equals(this.title, title))
            return;
        this.title = title;
        saveUpdate(ServerToClientModel.WIDGET_TITLE, title);
    }

    public void setStyleName(final String styleName) {
        if (Objects.equals(this.styleName, styleName))
            return;
        this.styleName = styleName;
        saveUpdate(ServerToClientModel.STYLE_NAME, styleName);
    }

    public void setStylePrimaryName(final String stylePrimaryName) {
        if (Objects.equals(this.stylePrimaryName, stylePrimaryName))
            return;
        this.stylePrimaryName = stylePrimaryName;
        saveUpdate(ServerToClientModel.STYLE_PRIMARY_NAME, stylePrimaryName);
    }

    public void ensureDebugId(final String debugID) {
        if (Objects.equals(this.debugID, debugID))
            return;
        this.debugID = debugID;
        saveUpdate(ServerToClientModel.ENSURE_DEBUG_ID, debugID);
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
        if (!Objects.equals(safeStyleProperties().put(name, value), value))
            saveUpdate(ServerToClientModel.PUT_STYLE_KEY, name, ServerToClientModel.STYLE_VALUE, value);
    }

    public void removeStyleProperty(final String name) {
        if (safeStyleProperties().remove(name) != null) saveUpdate(ServerToClientModel.REMOVE_STYLE_KEY, name);
    }

    public void setProperty(final String name, final String value) {
        if (!Objects.equals(safeElementProperties().put(name, value), value))
            saveUpdate(ServerToClientModel.PUT_PROPERTY_KEY, name, ServerToClientModel.PROPERTY_VALUE, value);
    }

    public void setAttribute(final String name, final String value) {
        if (!Objects.equals(safeElementAttributes().put(name, value), value))
            saveUpdate(ServerToClientModel.PUT_ATTRIBUTE_KEY, name, ServerToClientModel.ATTRIBUTE_VALUE, value);
    }

    public void removeAttribute(final String name) {
        if (safeElementAttributes().remove(name) != null) saveUpdate(ServerToClientModel.REMOVE_ATTRIBUTE_KEY, name);
    }

    public String getProperty(final String key) {
        return elementProperties != null ? elementProperties.get(key) : null;
    }

    public String getAttribute(final String key) {
        return elementAttributes != null ? elementAttributes.get(key) : null;
    }

    public void preventEvent(final PEvent e) {
        if (safePreventEvents().add(e)) saveUpdate(ServerToClientModel.PREVENT_EVENT, e.getCode());
    }

    public void stopEvent(final PEvent e) {
        if (safeStopEvents().add(e)) saveUpdate(ServerToClientModel.STOP_EVENT, e.getCode());
    }

    public void addStyleName(final String styleName) {
        if (safeStyleName().add(styleName)) saveUpdate(ServerToClientModel.ADD_STYLE_NAME, styleName);
    }

    public void removeStyleName(final String styleName) {
        if (styleNames != null && styleNames.remove(styleName)) removeStyle(styleName);
    }

    private void removeStyle(final String styleName) {
        saveUpdate(ServerToClientModel.REMOVE_STYLE_NAME, styleName);
    }

    public boolean hasStyleName(final String styleName) {
        if (styleNames == null) return false;
        else return styleNames.contains(styleName);
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
        this.width = Size.HUNDRED_PERCENT;
        this.height = Size.HUNDRED_PERCENT;
        saveUpdate(ServerToClientModel.WIDGET_FULL_SIZE);
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

    public <H extends EventHandler> HandlerRegistration removeDomHandler(final H handler,
            final PDomEvent.Type<H> type) {
        final HandlerRegistration handlerRegistration = ensureDomHandler().addHandler(type, handler);
        saveRemoveHandler(HandlerModel.HANDLER_DOM_HANDLER);
        return handlerRegistration;
    }

    public HandlerRegistration addDomHandler(final PKeyPressFilterHandler handler) {
        return addDomHandler(handler, PKeyPressEvent.TYPE, ServerToClientModel.KEY_FILTER, handler.asJsonObject());
    }

    public HandlerRegistration addDomHandler(final PKeyUpFilterHandler handler) {
        return addDomHandler(handler, PKeyUpEvent.TYPE, ServerToClientModel.KEY_FILTER, handler.asJsonObject());
    }

    public <H extends EventHandler> HandlerRegistration addDomHandler(final H handler, final Type<H> type) {
        return addDomHandler(handler, type, null, null);
    }

    private final Deque<AddDomHandlerInstruction<? extends EventHandler>> stackedAddDomHandlerInstructions = new LinkedList<>();

    private class AddDomHandlerInstruction<H extends EventHandler> {

        private final AddDomHandler<H> updater;
        ServerToClientModel model1;
        Object value1;
        ServerToClientModel model2;
        Object value2;

        public AddDomHandlerInstruction(final AddDomHandler<H> updater, final ServerToClientModel model1, final Object value1,
                final ServerToClientModel model2, final Object value2) {
            this.updater = updater;
            this.model1 = model1;
            this.value1 = value1;
            this.model2 = model2;
            this.value2 = value2;
        }

        public void execute() {
            updater.execute(model1, value1, model2, value2);
        }
    }

    private interface AddDomHandler<H extends EventHandler> {

        void execute(ServerToClientModel model1, Object value1, ServerToClientModel model2, Object value2);
    }

    @Override
    protected boolean attach(final int windowID) {
        final boolean result = super.attach(windowID);
        if (result) {
            while (!stackedAddDomHandlerInstructions.isEmpty()) {
                final AddDomHandlerInstruction<? extends EventHandler> updater = stackedAddDomHandlerInstructions.pop();
                updater.execute();
            }
        }

        return result;
    }

    private <H extends EventHandler> HandlerRegistration addDomHandler(final H handler, final Type<H> type,
            final ServerToClientModel model2, final Object value2) {
        final Collection<H> handlerIterator = ensureDomHandler().getHandlers(type, this);
        final HandlerRegistration handlerRegistration = domHandler.addHandlerToSource(type, this, handler);
        if (handlerIterator.isEmpty()) {
            final ServerToClientModel model1 = ServerToClientModel.DOM_HANDLER_CODE;
            final Object value1 = type.getDomHandlerType().getValue();
            if (windowID != PWindow.EMPTY_WINDOW_ID) executeAddDomHandler(model1, value1, model2, value2);
            else stackedAddDomHandlerInstructions
                    .add(new AddDomHandlerInstruction(this::executeAddDomHandler, model1, value1, model2, value2));
        }
        return handlerRegistration;
    }

    private <H extends EventHandler> void executeAddDomHandler(final ServerToClientModel model1, final Object value1,
            final ServerToClientModel model2, final Object value2) {
        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        if (windowID != PWindow.MAIN_WINDOW_ID) parser.parse(ServerToClientModel.WINDOW_ID, windowID);
        parser.parse(ServerToClientModel.TYPE_ADD_HANDLER, HandlerModel.HANDLER_DOM_HANDLER.getValue());
        parser.parse(ServerToClientModel.OBJECT_ID, ID);
        parser.parse(model1, value1);
        if (model2 != null) parser.parse(model2, value2);
        parser.endObject();
    }

    private <H extends EventHandler> void executeAddDomHandlerBis(final ServerBinaryModel... binaryModels) {
        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        if (windowID != PWindow.MAIN_WINDOW_ID) parser.parse(ServerToClientModel.WINDOW_ID, windowID);
        parser.parse(ServerToClientModel.TYPE_ADD_HANDLER, HandlerModel.HANDLER_DOM_HANDLER.getValue());
        parser.parse(ServerToClientModel.OBJECT_ID, ID);
        if (binaryModels != null && binaryModels.length > 0) {
            for (final ServerBinaryModel binaryModel : binaryModels) {
                parser.parse(binaryModel.getKey(), binaryModel.getValue());
            }
        }
        parser.endObject();
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
        if (domHandler == null)
            domHandler = new SimpleEventBus();
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

    public void fireEvent(final Event<?> event) {
        if (domHandler == null)
            return;
        domHandler.fireEvent(event);
    }

    public void removeFromParent() {
        if (parent instanceof HasPWidgets) {
            ((HasPWidgets) parent).remove(this);
        } else if (parent != null) {
            throw new IllegalStateException("This widget's parent does not implement HasPWidgets");
        }
    }

}
