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
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public abstract class PTWidget<T extends Widget> extends PTUIObject<T> implements IsWidget {

    private static final DomHandlerType[] DOM_HANDLER_TYPES = DomHandlerType.values();

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
            final DomHandlerType domHandlerType = DOM_HANDLER_TYPES[buffer.readBinaryModel().getByteValue()];
            addDomHandler(buffer, domHandlerType);
        } else {
            super.addHandler(buffer, handlerModel);
        }
    }

    @Override
    public void removeHandler(final ReaderBuffer buffer, final HandlerModel handlerModel) {
        if (HandlerModel.HANDLER_DOM.equals(handlerModel)) {
            // TODO Remove HANDLER_DOM
            // removeDomHandler(DOM_HANDLER_TYPES[buffer.readBinaryModel().getByteValue()]);
        } else {
            super.removeHandler(buffer, handlerModel);
        }
    }

    @Override
    public T asWidget() {
        return cast();
    }

    @Override
    public PTWidget<T> isPTWidget() {
        return this;
    }

    @Override
    public Widget asWidget(final int objectID, final UIBuilder uiService) {
        return asWidget(uiService.getPTObject(objectID));
    }

    @Override
    public Widget asWidget(final PTObject ptObject) {
        if (ptObject != null) {
            final PTWidget<?> ptWidget = ptObject.isPTWidget();
            if (ptWidget != null) return ptWidget.asWidget();
        } else {
            log.severe("No widget found for object #" + objectID);
        }
        return null;
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

        final Widget widget = asWidget();
        final JSONArray widgetInfo = new JSONArray();
        widgetInfo.set(0, new JSONNumber(widget.getAbsoluteLeft()));
        widgetInfo.set(1, new JSONNumber(widget.getAbsoluteTop()));
        widgetInfo.set(2, new JSONNumber(widget.getOffsetHeight()));
        widgetInfo.set(3, new JSONNumber(widget.getOffsetWidth()));
        eventInstruction.put(ClientToServerModel.WIDGET_POSITION, widgetInfo);

        uiBuilder.sendDataToServer(widget, eventInstruction);

        preventOrStopEvent(event);
    }

    private void triggerDomEvent(final DomHandlerType domHandlerType, final DomEvent<?> event) {
        final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
        uiBuilder.sendDataToServer(asWidget(), eventInstruction);
        preventOrStopEvent(event);
    }

    private PTInstruction buildEventInstruction(final DomHandlerType domHandlerType) {
        final PTInstruction eventInstruction = new PTInstruction(getObjectID());
        eventInstruction.put(ClientToServerModel.DOM_HANDLER_TYPE, domHandlerType.getValue());
        return eventInstruction;
    }

    private void addDomHandler(final ReaderBuffer buffer, final DomHandlerType domHandlerType) {
        final Widget widget = asWidget();
        switch (domHandlerType) {
            case CLICK:
                widget.addDomHandler(event -> triggerMouseEvent(domHandlerType, event), ClickEvent.getType());
                break;
            case DOUBLE_CLICK:
                widget.addDomHandler(event -> triggerMouseEvent(domHandlerType, event), DoubleClickEvent.getType());
                break;
            case MOUSE_OVER:
                widget.addDomHandler(event -> triggerMouseEvent(domHandlerType, event), MouseOverEvent.getType());
                break;
            case MOUSE_OUT:
                widget.addDomHandler(event -> triggerMouseEvent(domHandlerType, event), MouseOutEvent.getType());
                break;
            case MOUSE_DOWN:
                widget.addDomHandler(event -> triggerMouseEvent(domHandlerType, event), MouseDownEvent.getType());
                break;
            case MOUSE_UP:
                widget.addDomHandler(event -> triggerMouseEvent(domHandlerType, event), MouseUpEvent.getType());
                break;
            case MOUSE_WHELL:
                widget.addDomHandler(event -> triggerMouseEvent(domHandlerType, event), MouseWheelEvent.getType());
                break;
            case BLUR:
                widget.addDomHandler(event -> triggerDomEvent(domHandlerType, event), BlurEvent.getType());
                break;
            case FOCUS:
                widget.addDomHandler(event -> triggerDomEvent(domHandlerType, event), FocusEvent.getType());
                break;
            case KEY_PRESS:
                final BinaryModel binaryModel = buffer.readBinaryModel();
                final JSONArray keyFilter;
                if (ServerToClientModel.KEY_FILTER.equals(binaryModel.getModel())) {
                    keyFilter = binaryModel.getJsonObject().get(ClientToServerModel.KEY_FILTER.toStringValue()).isArray();
                } else {
                    buffer.rewind(binaryModel);
                    keyFilter = null;
                }

                widget.addDomHandler(event -> {
                    final Widget widget1 = asWidget();
                    final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
                    eventInstruction.put(ClientToServerModel.VALUE_KEY, event.getNativeEvent().getKeyCode());

                    if (keyFilter != null) {
                        for (int i = 0; i < keyFilter.size(); i++) {
                            final JSONNumber keyCode = keyFilter.get(i).isNumber();
                            if (keyCode.doubleValue() == event.getNativeEvent().getKeyCode()) {
                                uiBuilder.sendDataToServer(widget1, eventInstruction);
                                break;
                            }
                        }
                    } else {
                        uiBuilder.sendDataToServer(widget1, eventInstruction);
                    }

                    preventOrStopEvent(event);
                }, KeyPressEvent.getType());
                break;
            case KEY_UP:
                final BinaryModel keyUpModel = buffer.readBinaryModel();
                final JSONArray keyUpFilter;
                if (ServerToClientModel.KEY_FILTER.equals(keyUpModel.getModel())) {
                    keyUpFilter = keyUpModel.getJsonObject().get(ClientToServerModel.KEY_FILTER.toStringValue()).isArray();
                } else {
                    buffer.rewind(keyUpModel);
                    keyUpFilter = null;
                }

                if (widget instanceof TextBoxBase) {
                    final TextBoxBase textBox = (TextBoxBase) widget;
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
                    widget.addDomHandler(event -> {
                        final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
                        eventInstruction.put(ClientToServerModel.VALUE_KEY, event.getNativeEvent().getKeyCode());

                        if (keyUpFilter != null) {
                            for (int i = 0; i < keyUpFilter.size(); i++) {
                                final JSONNumber keyCode = keyUpFilter.get(i).isNumber();
                                if (keyCode.doubleValue() == event.getNativeEvent().getKeyCode()) {
                                    uiBuilder.sendDataToServer(widget, eventInstruction);
                                    break;
                                }
                            }
                        } else {
                            uiBuilder.sendDataToServer(widget, eventInstruction);
                        }
                        preventOrStopEvent(event);
                    }, KeyUpEvent.getType());
                }
                break;
            case DRAG_START:
                widget.getElement().setDraggable(Element.DRAGGABLE_TRUE);
                widget.addBitlessDomHandler(event -> {
                    event.setData("text", String.valueOf(getObjectID()));
                    event.getDataTransfer().setDragImage(uiObject.getElement(), 10, 10);
                    triggerDomEvent(domHandlerType, event);
                }, DragStartEvent.getType());
                break;
            case DRAG_END:
                widget.addBitlessDomHandler(event -> triggerDomEvent(domHandlerType, event), DragEndEvent.getType());
                break;
            case DRAG_ENTER:
                widget.addBitlessDomHandler(event -> triggerDomEvent(domHandlerType, event), DragEnterEvent.getType());
                break;
            case DRAG_LEAVE:
                widget.addBitlessDomHandler(event -> triggerDomEvent(domHandlerType, event), DragLeaveEvent.getType());
                break;
            case DROP:
                widget.addBitlessDomHandler(event -> {
                    // required by GWT api
                    // triggerDomEvent(addHandler.getObjectID(), domHandlerType);
                }, DragOverEvent.getType());

                widget.addBitlessDomHandler(event -> {
                    event.preventDefault();
                    final String dragWidgetID = event.getData("text");
                    final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
                    if (dragWidgetID != null) eventInstruction.put(ClientToServerModel.DRAG_SRC, Long.parseLong(dragWidgetID));
                    uiBuilder.sendDataToServer(widget, eventInstruction);
                }, DropEvent.getType());
                break;
            case CONTEXT_MENU:
                widget.addDomHandler(event -> triggerDomEvent(domHandlerType, event), ContextMenuEvent.getType());
                break;
            case CHANGE_HANDLER:
            case DRAG_OVER:
            default:
                log.info("Handler not supported #" + domHandlerType);
                break;
        }
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
