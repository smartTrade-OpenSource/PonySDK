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
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.DragEndEvent;
import com.google.gwt.event.dom.client.DragEndHandler;
import com.google.gwt.event.dom.client.DragEnterEvent;
import com.google.gwt.event.dom.client.DragEnterHandler;
import com.google.gwt.event.dom.client.DragLeaveEvent;
import com.google.gwt.event.dom.client.DragLeaveHandler;
import com.google.gwt.event.dom.client.DragOverEvent;
import com.google.gwt.event.dom.client.DragOverHandler;
import com.google.gwt.event.dom.client.DragStartEvent;
import com.google.gwt.event.dom.client.DragStartHandler;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.DropHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.DomHandlerType;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public abstract class PTWidget<T extends Widget> extends PTUIObject<T> implements IsWidget {

    private static final String HUNDRED_PERCENT = "100%";

    private final static Logger log = Logger.getLogger(PTWidget.class.getName());

    private final Set<Integer> preventedEvents = new HashSet<>();
    private final Set<Integer> stoppedEvents = new HashSet<>();

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.WIDGET_FULL_SIZE.equals(binaryModel.getModel())) {
            uiObject.setWidth(HUNDRED_PERCENT);
            uiObject.setHeight(HUNDRED_PERCENT);
            return true;
        }
        if (ServerToClientModel.PREVENT_EVENT.equals(binaryModel.getModel())) {
            preventedEvents.add(binaryModel.getIntValue());
            return true;
        }
        if (ServerToClientModel.STOP_EVENT.equals(binaryModel.getModel())) {
            stoppedEvents.add(binaryModel.getIntValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel, final UIBuilder uiService) {
        if (HandlerModel.HANDLER_DOM.equals(handlerModel)) {
            // ServerToClientModel.DOM_HANDLER_CODE
            final DomHandlerType domHandlerType = DomHandlerType.values()[buffer.readBinaryModel().getByteValue()];
            addDomHandler(buffer, domHandlerType, uiService);
        } else {
            super.addHandler(buffer, handlerModel, uiService);
        }
    }

    @Override
    public void removeHandler(final ReaderBuffer buffer, final UIBuilder uiService) {
        /*
         * FIXME
         * if (buffer.containsKey(HandlerModel.HANDLER_DOM_HANDLER)) {
         * final int domHandlerType =
         * removeHandler.getInt(Model.DOM_HANDLER_CODE);
         * final Widget w = asWidget(removeHandler.getObjectID(),
         * uiService);
         * final HandlerRegistration handlerRegistration;
         * handlerRegistration.removeHandler()
         * removeDomHandler(removeHandler, w, domHandlerType, uiService);
         * } else {
         * super.removeHandler(buffer, uiService);
         * }
         */
        super.removeHandler(buffer, uiService);
    }

    @Override
    public T asWidget() {
        return uiObject;
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
            if (ptWidget != null)
                return ptWidget.asWidget();
        } else {
            log.severe("No widget found for object #" + objectID);
        }
        return null;
    }

    protected void triggerMouseEvent(final DomHandlerType domHandlerType,
            final UIBuilder uiService, final MouseEvent<?> event) {
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

        uiService.sendDataToServer(widget, eventInstruction);

        preventOrStopEvent(event);
    }

    private void triggerDomEvent(final DomHandlerType domHandlerType, final UIBuilder uiService, final DomEvent<?> event) {
        final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
        uiService.sendDataToServer(asWidget(), eventInstruction);
        preventOrStopEvent(event);
    }

    private PTInstruction buildEventInstruction(final DomHandlerType domHandlerType) {
        final PTInstruction eventInstruction = new PTInstruction(getObjectID());
        eventInstruction.put(ClientToServerModel.DOM_HANDLER_TYPE, domHandlerType.getValue());
        return eventInstruction;
    }

    private void addDomHandler(final ReaderBuffer buffer, final DomHandlerType domHandlerType, final UIBuilder uiService) {
        final Widget widget = asWidget();
        switch (domHandlerType) {
            case CLICK:
                widget.addDomHandler(new ClickHandler() {

                    @Override
                    public void onClick(final ClickEvent event) {
                        triggerMouseEvent(domHandlerType, uiService, event);
                    }

                }, ClickEvent.getType());
                break;
            case DOUBLE_CLICK:
                widget.addDomHandler(new DoubleClickHandler() {

                    @Override
                    public void onDoubleClick(final DoubleClickEvent event) {
                        triggerMouseEvent(domHandlerType, uiService, event);
                    }
                }, DoubleClickEvent.getType());
                break;
            case MOUSE_OVER:
                widget.addDomHandler(new MouseOverHandler() {

                    @Override
                    public void onMouseOver(final MouseOverEvent event) {
                        triggerMouseEvent(domHandlerType, uiService, event);
                    }

                }, MouseOverEvent.getType());
                break;
            case MOUSE_OUT:
                widget.addDomHandler(new MouseOutHandler() {

                    @Override
                    public void onMouseOut(final MouseOutEvent event) {
                        triggerMouseEvent(domHandlerType, uiService, event);
                    }

                }, MouseOutEvent.getType());
                break;
            case MOUSE_DOWN:
                widget.addDomHandler(new MouseDownHandler() {

                    @Override
                    public void onMouseDown(final MouseDownEvent event) {
                        triggerMouseEvent(domHandlerType, uiService, event);
                    }

                }, MouseDownEvent.getType());
                break;
            case MOUSE_UP:
                widget.addDomHandler(new MouseUpHandler() {

                    @Override
                    public void onMouseUp(final MouseUpEvent event) {
                        triggerMouseEvent(domHandlerType, uiService, event);
                    }

                }, MouseUpEvent.getType());
                break;
            case BLUR:
                widget.addDomHandler(new BlurHandler() {

                    @Override
                    public void onBlur(final BlurEvent event) {
                        triggerDomEvent(domHandlerType, uiService, event);
                    }

                }, BlurEvent.getType());
                break;
            case FOCUS:
                widget.addDomHandler(new FocusHandler() {

                    @Override
                    public void onFocus(final FocusEvent event) {
                        triggerDomEvent(domHandlerType, uiService, event);
                    }

                }, FocusEvent.getType());
                break;
            case KEY_PRESS:
                final BinaryModel binaryModel = buffer.readBinaryModel();
                final BinaryModel keyFilter;
                if (ServerToClientModel.KEY_FILTER.equals(binaryModel.getModel())) {
                    keyFilter = binaryModel;
                } else {
                    buffer.rewind(binaryModel);
                    keyFilter = null;
                }

                widget.addDomHandler(new KeyPressHandler() {

                    @Override
                    public void onKeyPress(final KeyPressEvent event) {
                        final Widget widget = asWidget();
                        final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
                        eventInstruction.put(ClientToServerModel.VALUE_KEY, event.getNativeEvent().getKeyCode());

                        if (keyFilter != null) {
                            final JSONArray jsonArray = keyFilter.getJsonObject().get(ClientToServerModel.KEY_FILTER.toStringValue())
                                    .isArray();
                            for (int i = 0; i < jsonArray.size(); i++) {
                                final JSONNumber keyCode = jsonArray.get(i).isNumber();
                                if (keyCode.doubleValue() == event.getNativeEvent().getKeyCode()) {
                                    uiService.sendDataToServer(widget, eventInstruction);
                                    break;
                                }
                            }
                        } else {
                            uiService.sendDataToServer(widget, eventInstruction);
                        }

                        preventOrStopEvent(event);
                    }

                }, KeyPressEvent.getType());
                break;
            case KEY_UP:
                final BinaryModel keyUpModel = buffer.readBinaryModel();
                final BinaryModel keyUpFilter;
                if (ServerToClientModel.KEY_FILTER.equals(keyUpModel.getModel())) {
                    keyUpFilter = keyUpModel;
                } else {
                    buffer.rewind(keyUpModel);
                    keyUpFilter = null;
                }

                if (widget instanceof TextBoxBase) {
                    final TextBoxBase textBox = (TextBoxBase) widget;
                    textBox.addKeyUpHandler(new KeyUpHandler() {

                        @Override
                        public void onKeyUp(final KeyUpEvent event) {
                            final PTInstruction changeHandlerInstruction = new PTInstruction(getObjectID());
                            changeHandlerInstruction.put(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE, textBox.getText());

                            final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
                            eventInstruction.put(ClientToServerModel.VALUE_KEY, event.getNativeEvent().getKeyCode());

                            if (keyUpFilter != null) {
                                final JSONArray jsonArray = keyUpFilter.getJsonObject()
                                        .get(ClientToServerModel.KEY_FILTER.toStringValue())
                                        .isArray();
                                for (int i = 0; i < jsonArray.size(); i++) {
                                    final JSONNumber keyCode = jsonArray.get(i).isNumber();
                                    if (keyCode.doubleValue() == event.getNativeEvent().getKeyCode()) {
                                        uiService.sendDataToServer(changeHandlerInstruction);
                                        uiService.sendDataToServer(eventInstruction);
                                        break;
                                    }
                                }
                            } else {
                                uiService.sendDataToServer(changeHandlerInstruction);
                                uiService.sendDataToServer(eventInstruction);
                            }
                            preventOrStopEvent(event);
                        }
                    });
                } else {
                    widget.addDomHandler(new KeyUpHandler() {

                        @Override
                        public void onKeyUp(final KeyUpEvent event) {
                            final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
                            eventInstruction.put(ClientToServerModel.VALUE_KEY, event.getNativeEvent().getKeyCode());

                            if (keyUpFilter != null) {
                                final JSONArray jsonArray = keyUpFilter.getJsonObject()
                                        .get(ClientToServerModel.KEY_FILTER.toStringValue())
                                        .isArray();
                                for (int i = 0; i < jsonArray.size(); i++) {
                                    final JSONNumber keyCode = jsonArray.get(i).isNumber();
                                    if (keyCode.doubleValue() == event.getNativeEvent().getKeyCode()) {
                                        uiService.sendDataToServer(widget, eventInstruction);
                                        break;
                                    }
                                }
                            } else {
                                uiService.sendDataToServer(widget, eventInstruction);
                            }
                            preventOrStopEvent(event);
                        }
                    }, KeyUpEvent.getType());
                }
                break;
            case DRAG_START:
                widget.getElement().setDraggable(Element.DRAGGABLE_TRUE);
                widget.addBitlessDomHandler(new DragStartHandler() {

                    @Override
                    public void onDragStart(final DragStartEvent event) {
                        event.setData("text", String.valueOf(getObjectID()));
                        event.getDataTransfer().setDragImage(uiObject.getElement(), 10, 10);
                        triggerDomEvent(domHandlerType, uiService, event);
                    }
                }, DragStartEvent.getType());
                break;
            case DRAG_END:
                widget.addBitlessDomHandler(new DragEndHandler() {

                    @Override
                    public void onDragEnd(final DragEndEvent event) {
                        triggerDomEvent(domHandlerType, uiService, event);
                    }
                }, DragEndEvent.getType());
                break;
            case DRAG_ENTER:
                widget.addBitlessDomHandler(new DragEnterHandler() {

                    @Override
                    public void onDragEnter(final DragEnterEvent event) {
                        triggerDomEvent(domHandlerType, uiService, event);
                    }
                }, DragEnterEvent.getType());
                break;
            case DRAG_LEAVE:
                widget.addBitlessDomHandler(new DragLeaveHandler() {

                    @Override
                    public void onDragLeave(final DragLeaveEvent event) {
                        triggerDomEvent(domHandlerType, uiService, event);
                    }
                }, DragLeaveEvent.getType());
                break;
            case DROP:
                widget.addBitlessDomHandler(new DragOverHandler() {

                    @Override
                    public void onDragOver(final DragOverEvent event) {
                        // required by GWT api
                        // triggerDomEvent(addHandler.getObjectID(),
                        // domHandlerType,
                        // uiService);
                    }
                }, DragOverEvent.getType());

                widget.addBitlessDomHandler(new DropHandler() {

                    @Override
                    public void onDrop(final DropEvent event) {
                        event.preventDefault();
                        final String dragWidgetID = event.getData("text");
                        final PTInstruction eventInstruction = buildEventInstruction(domHandlerType);
                        if (dragWidgetID != null)
                            eventInstruction.put(ClientToServerModel.DRAG_SRC, Long.parseLong(dragWidgetID));
                        uiService.sendDataToServer(widget, eventInstruction);
                    }
                }, DropEvent.getType());
                break;
            case CONTEXT_MENU:
                widget.addDomHandler(new ContextMenuHandler() {

                    @Override
                    public void onContextMenu(final ContextMenuEvent event) {
                        triggerDomEvent(domHandlerType, uiService, event);
                    }
                }, ContextMenuEvent.getType());
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
        if (preventedEvents.isEmpty())
            return;

        final int typeInt = Event.as(event.getNativeEvent()).getTypeInt();
        if (preventedEvents.contains(typeInt)) {
            event.preventDefault();
        }
    }

    private void stopEvent(final DomEvent<?> event) {
        if (stoppedEvents.isEmpty())
            return;

        final int typeInt = Event.as(event.getNativeEvent()).getTypeInt();
        if (stoppedEvents.contains(typeInt)) {
            event.stopPropagation();
        }
    }
}
