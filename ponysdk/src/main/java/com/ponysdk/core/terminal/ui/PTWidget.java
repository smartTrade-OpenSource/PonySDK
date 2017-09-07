/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.terminal.ui;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DragEndEvent;
import com.google.gwt.event.dom.client.DragEnterEvent;
import com.google.gwt.event.dom.client.DragLeaveEvent;
import com.google.gwt.event.dom.client.DragOverEvent;
import com.google.gwt.event.dom.client.DragStartEvent;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public abstract class PTWidget<T extends Widget> extends PTUIObject<T> implements IsWidget {

    private static final String HUNDRED_PERCENT = "100%";

    private static final Logger log = Logger.getLogger(PTWidget.class.getName());

    private Set<Integer> preventedEvents;
    private Set<Integer> stoppedEvents;

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.WIDGET_FULL_SIZE.ordinal() == modelOrdinal) {
            uiObject.setWidth(HUNDRED_PERCENT);
            uiObject.setHeight(HUNDRED_PERCENT);
            return true;
        } else if (ServerToClientModel.PREVENT_EVENT.ordinal() == modelOrdinal) {
            if (preventedEvents == null) preventedEvents = new HashSet<>();
            preventedEvents.add(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.STOP_EVENT.ordinal() == modelOrdinal) {
            if (stoppedEvents == null) stoppedEvents = new HashSet<>();
            stoppedEvents.add(binaryModel.getIntValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel) {
        if (HandlerModel.HANDLER_DOM.equals(handlerModel)) {
            // ServerToClientModel.DOM_HANDLER_CODE
            final DomHandlerType domHandlerType = DomHandlerType.fromRawValue(buffer.readBinaryModel().getByteValue());
            addDomHandler(buffer, domHandlerType);
        } else {
            super.addHandler(buffer, handlerModel);
        }
    }

    @Override
    public void removeHandler(final ReaderBuffer buffer, final HandlerModel handlerModel) {
        if (HandlerModel.HANDLER_DOM.equals(handlerModel)) {
            // TODO Remove HANDLER_DOM
            // removeDomHandler(DomHandlerType.fromByte(buffer.readBinaryModel().getByteValue()));
        } else {
            super.removeHandler(buffer, handlerModel);
        }
    }

    @Override
    public T asWidget() {
        return uiObject;
    }

    private void addDomHandler(final ReaderBuffer buffer, final DomHandlerType domHandlerType) {
        if (DomHandlerType.CLICK.equals(domHandlerType)) {
            uiObject.addDomHandler(event -> triggerMouseEvent(domHandlerType, event), ClickEvent.getType());
        } else if (DomHandlerType.DOUBLE_CLICK.equals(domHandlerType)) {
            uiObject.addDomHandler(event -> triggerMouseEvent(domHandlerType, event), DoubleClickEvent.getType());
        } else if (DomHandlerType.MOUSE_OVER.equals(domHandlerType)) {
            uiObject.addDomHandler(event -> triggerMouseEvent(domHandlerType, event), MouseOverEvent.getType());
        } else if (DomHandlerType.MOUSE_OUT.equals(domHandlerType)) {
            uiObject.addDomHandler(event -> triggerMouseEvent(domHandlerType, event), MouseOutEvent.getType());
        } else if (DomHandlerType.MOUSE_DOWN.equals(domHandlerType)) {
            uiObject.addDomHandler(event -> triggerMouseEvent(domHandlerType, event), MouseDownEvent.getType());
        } else if (DomHandlerType.MOUSE_UP.equals(domHandlerType)) {
            uiObject.addDomHandler(event -> triggerMouseEvent(domHandlerType, event), MouseUpEvent.getType());
        } else if (DomHandlerType.MOUSE_WHELL.equals(domHandlerType)) {
            uiObject.addDomHandler(event -> triggerMouseWhellEvent(domHandlerType, event), MouseWheelEvent.getType());
        } else if (DomHandlerType.BLUR.equals(domHandlerType)) {
            uiObject.addDomHandler(event -> triggerDomEvent(domHandlerType, event), BlurEvent.getType());
        } else if (DomHandlerType.FOCUS.equals(domHandlerType)) {
            uiObject.addDomHandler(event -> triggerDomEvent(domHandlerType, event), FocusEvent.getType());
        } else if (DomHandlerType.KEY_PRESS.equals(domHandlerType)) {
            final BinaryModel binaryModel = buffer.readBinaryModel();
            final JSONArray keyFilter;
            if (ServerToClientModel.KEY_FILTER.equals(binaryModel.getModel())) {
                keyFilter = binaryModel.getJsonObject().get(ClientToServerModel.KEY_FILTER.toStringValue()).isArray();
            } else {
                buffer.rewind(binaryModel);
                keyFilter = null;
            }

            uiObject.addDomHandler(event -> {
                final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
                eventInstruction.put(ClientToServerModel.VALUE_KEY, event.getNativeEvent().getKeyCode());

                if (keyFilter != null) {
                    for (int i = 0; i < keyFilter.size(); i++) {
                        final JSONNumber keyCode = keyFilter.get(i).isNumber();
                        if (keyCode.doubleValue() == event.getNativeEvent().getKeyCode()) {
                            uiBuilder.sendDataToServer(uiObject, eventInstruction);
                            break;
                        }
                    }
                } else {
                    uiBuilder.sendDataToServer(uiObject, eventInstruction);
                }

                preventOrStopEvent(event);
            }, KeyPressEvent.getType());
        } else if (DomHandlerType.KEY_UP.equals(domHandlerType)) {
            final BinaryModel keyUpModel = buffer.readBinaryModel();
            final JSONArray keyUpFilter;
            if (ServerToClientModel.KEY_FILTER.equals(keyUpModel.getModel())) {
                keyUpFilter = keyUpModel.getJsonObject().get(ClientToServerModel.KEY_FILTER.toStringValue()).isArray();
            } else {
                buffer.rewind(keyUpModel);
                keyUpFilter = null;
            }

            if (uiObject instanceof TextBoxBase) {
                final TextBoxBase textBox = (TextBoxBase) uiObject;
                textBox.addKeyUpHandler(event -> {
                    final PTInstruction changeHandlerInstruction = new PTInstruction(getObjectID());
                    changeHandlerInstruction.put(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE, textBox.getText());

                    final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
                    eventInstruction.put(ClientToServerModel.VALUE_KEY, event.getNativeEvent().getKeyCode());

                    if (keyUpFilter != null) {
                        for (int i = 0; i < keyUpFilter.size(); i++) {
                            final JSONNumber keyCode = keyUpFilter.get(i).isNumber();
                            if (keyCode.doubleValue() == event.getNativeEvent().getKeyCode()) {
                                uiBuilder.sendDataToServer(changeHandlerInstruction);
                                uiBuilder.sendDataToServer(eventInstruction);
                                break;
                            }
                        }
                    } else {
                        uiBuilder.sendDataToServer(changeHandlerInstruction);
                        uiBuilder.sendDataToServer(eventInstruction);
                    }
                    preventOrStopEvent(event);
                });
            } else {
                uiObject.addDomHandler(event -> {
                    final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
                    eventInstruction.put(ClientToServerModel.VALUE_KEY, event.getNativeEvent().getKeyCode());

                    if (keyUpFilter != null) {
                        for (int i = 0; i < keyUpFilter.size(); i++) {
                            final JSONNumber keyCode = keyUpFilter.get(i).isNumber();
                            if (keyCode.doubleValue() == event.getNativeEvent().getKeyCode()) {
                                uiBuilder.sendDataToServer(uiObject, eventInstruction);
                                break;
                            }
                        }
                    } else {
                        uiBuilder.sendDataToServer(uiObject, eventInstruction);
                    }
                    preventOrStopEvent(event);
                }, KeyUpEvent.getType());
            }
        } else if (DomHandlerType.DRAG_START.equals(domHandlerType)) {
            uiObject.getElement().setDraggable(Element.DRAGGABLE_TRUE);
            uiObject.addBitlessDomHandler(event -> {
                event.setData("text", String.valueOf(getObjectID()));
                event.getDataTransfer().setDragImage(uiObject.getElement(), 10, 10);
                triggerDomEvent(domHandlerType, event);
            }, DragStartEvent.getType());
        } else if (DomHandlerType.DRAG_END.equals(domHandlerType)) {
            uiObject.addBitlessDomHandler(event -> triggerDomEvent(domHandlerType, event), DragEndEvent.getType());
        } else if (DomHandlerType.DRAG_ENTER.equals(domHandlerType)) {
            uiObject.addBitlessDomHandler(event -> triggerDomEvent(domHandlerType, event), DragEnterEvent.getType());
        } else if (DomHandlerType.DRAG_LEAVE.equals(domHandlerType)) {
            uiObject.addBitlessDomHandler(event -> triggerDomEvent(domHandlerType, event), DragLeaveEvent.getType());
        } else if (DomHandlerType.DROP.equals(domHandlerType)) {
            uiObject.addBitlessDomHandler(event -> {
                // required by GWT api
                // triggerDomEvent(addHandler.getObjectID(), domHandlerType);
            }, DragOverEvent.getType());

            uiObject.addBitlessDomHandler(event -> {
                event.preventDefault();
                final String dragWidgetID = event.getData("text");
                final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
                if (dragWidgetID != null) eventInstruction.put(ClientToServerModel.DRAG_SRC, Long.parseLong(dragWidgetID));
                uiBuilder.sendDataToServer(uiObject, eventInstruction);
            }, DropEvent.getType());
        } else if (DomHandlerType.CONTEXT_MENU.equals(domHandlerType)) {
            uiObject.addDomHandler(event -> triggerDomEvent(domHandlerType, event), ContextMenuEvent.getType());
        } else {
            log.info("Handler not supported #" + domHandlerType);
        }
    }

    private PTInstruction buildEventInstruction(final DomHandlerType domHandlerType) {
        final PTInstruction eventInstruction = new PTInstruction(getObjectID());
        eventInstruction.put(ClientToServerModel.DOM_HANDLER_TYPE, domHandlerType.getValue());
        return eventInstruction;
    }

    private void triggerDomEvent(final DomHandlerType domHandlerType, final DomEvent<?> event) {
        final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
        uiBuilder.sendDataToServer(uiObject, eventInstruction);
        preventOrStopEvent(event);
    }

    protected void triggerMouseEvent(final DomHandlerType domHandlerType, final MouseEvent<?> event) {
        final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);

        final JSONArray eventInfo = new JSONArray();
        eventInfo.set(0, new JSONNumber(event.getClientX()));
        eventInfo.set(1, new JSONNumber(event.getClientY()));
        eventInfo.set(2, new JSONNumber(event.getX()));
        eventInfo.set(3, new JSONNumber(event.getY()));
        eventInfo.set(4, new JSONNumber(event.getNativeButton()));
        eventInstruction.put(ClientToServerModel.EVENT_INFO, eventInfo);

        final JSONArray widgetInfo = new JSONArray();
        widgetInfo.set(0, new JSONNumber(uiObject.getAbsoluteLeft()));
        widgetInfo.set(1, new JSONNumber(uiObject.getAbsoluteTop()));
        widgetInfo.set(2, new JSONNumber(uiObject.getOffsetHeight()));
        widgetInfo.set(3, new JSONNumber(uiObject.getOffsetWidth()));
        eventInstruction.put(ClientToServerModel.WIDGET_POSITION, widgetInfo);

        uiBuilder.sendDataToServer(uiObject, eventInstruction);

        preventOrStopEvent(event);
    }

    private void triggerMouseWhellEvent(final DomHandlerType domHandlerType, final MouseWheelEvent event) {
        final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
        final JSONArray eventInfo = new JSONArray();
        eventInfo.set(0, new JSONNumber(event.getClientX()));
        eventInfo.set(1, new JSONNumber(event.getClientY()));
        eventInfo.set(2, new JSONNumber(event.getX()));
        eventInfo.set(3, new JSONNumber(event.getY()));
        eventInfo.set(4, new JSONNumber(event.getNativeButton()));
        eventInfo.set(5, new JSONNumber(event.getDeltaY()));
        eventInstruction.put(ClientToServerModel.EVENT_INFO, eventInfo);

        final JSONArray widgetInfo = new JSONArray();
        widgetInfo.set(0, new JSONNumber(uiObject.getAbsoluteLeft()));
        widgetInfo.set(1, new JSONNumber(uiObject.getAbsoluteTop()));
        widgetInfo.set(2, new JSONNumber(uiObject.getOffsetHeight()));
        widgetInfo.set(3, new JSONNumber(uiObject.getOffsetWidth()));
        eventInstruction.put(ClientToServerModel.WIDGET_POSITION, widgetInfo);

        uiBuilder.sendDataToServer(uiObject, eventInstruction);

        preventOrStopEvent(event);
    }

    private void preventOrStopEvent(final DomEvent<?> event) {
        preventEvent(event);
        stopEvent(event);
    }

    private void preventEvent(final DomEvent<?> event) {
        if (preventedEvents != null && !preventedEvents.isEmpty()) {
            final int typeInt = Event.as(event.getNativeEvent()).getTypeInt();
            if (preventedEvents.contains(typeInt)) event.preventDefault();
        }
    }

    private void stopEvent(final DomEvent<?> event) {
        if (stoppedEvents != null && !stoppedEvents.isEmpty()) {
            final int typeInt = Event.as(event.getNativeEvent()).getTypeInt();
            if (stoppedEvents.contains(typeInt)) event.stopPropagation();
        }
    }
}
