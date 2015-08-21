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

package com.ponysdk.ui.terminal.ui;

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
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.DomHandlerType;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTWidget<W extends Widget> extends PTUIObject<W> {

    private final static Logger log = Logger.getLogger(PTWidget.class.getName());

    private final Set<Integer> preventedEvents = new HashSet<>();
    private final Set<Integer> stoppedEvents = new HashSet<>();

    @Override
    public void addHandler(final PTInstruction instruction, final UIService uiService) {
        if (instruction.containsKey(Model.HANDLER_DOM_HANDLER)) {
            final int domHandlerType = instruction.getInt(Model.DOM_HANDLER_CODE);
            final Widget w = asWidget(instruction.getObjectID(), uiService);
            addDomHandler(instruction, w, domHandlerType, uiService);
        } else {
            super.addHandler(instruction, uiService);
        }
    }

    @Override
    public void removeHandler(final PTInstruction removeHandler, final UIService uiService) {
        if (removeHandler.containsKey(Model.HANDLER_DOM_HANDLER)) {
            // final int domHandlerType = removeHandler.getInt(Model.DOM_HANDLER_CODE);
            // final Widget w = asWidget(removeHandler.getObjectID(), uiService);
            // final HandlerRegistration handlerRegistration;
            // handlerRegistration.removeHandler()
            // removeDomHandler(removeHandler, w, domHandlerType, uiService);
        } else {
            super.removeHandler(removeHandler, uiService);
        }

    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(Model.PREVENT_EVENT)) {
            preventedEvents.add(update.getInt(Model.PREVENT_EVENT));
        } else if (update.containsKey(Model.STOP_EVENT)) {
            stoppedEvents.add(update.getInt(Model.STOP_EVENT));
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public Widget asWidget(final Long objectID, final UIService uiService) {
        return ((PTWidget<?>) uiService.getPTObject(objectID)).cast();
    }

    //
    // @Override
    // public W cast() {
    // return uiObject;
    // }

    protected void triggerMouseEvent(final PTInstruction addHandler, final Widget widget, final DomHandlerType domHandlerType, final UIService uiService, final MouseEvent<?> event) {
        final PTInstruction eventInstruction = buildEventInstruction(addHandler, domHandlerType);
        eventInstruction.put(Model.CLIENT_X, event.getClientX());
        eventInstruction.put(Model.CLIENT_Y, event.getClientY());
        eventInstruction.put(Model.X, event.getX());
        eventInstruction.put(Model.Y, event.getY());
        eventInstruction.put(Model.NATIVE_BUTTON, event.getNativeButton());
        eventInstruction.put(Model.SOURCE_ABSOLUTE_LEFT, widget.getAbsoluteLeft());
        eventInstruction.put(Model.SOURCE_ABSOLUTE_TOP, widget.getAbsoluteTop());
        eventInstruction.put(Model.SOURCE_OFFSET_HEIGHT, widget.getOffsetHeight());
        eventInstruction.put(Model.SOURCE_OFFSET_WIDTH, widget.getOffsetWidth());
        uiService.sendDataToServer(widget, eventInstruction);
        preventOrStopEvent(event);
    }

    protected void triggerDomEvent(final PTInstruction addHandler, final Widget widget, final DomHandlerType domHandlerType, final UIService uiService, final DomEvent<?> event) {
        final PTInstruction eventInstruction = buildEventInstruction(addHandler, domHandlerType);
        uiService.sendDataToServer(widget, eventInstruction);
        preventOrStopEvent(event);
    }

    private PTInstruction buildEventInstruction(final PTInstruction addHandler, final DomHandlerType domHandlerType) {
        final PTInstruction eventInstruction = new PTInstruction();
        eventInstruction.setObjectID(addHandler.getObjectID());
        eventInstruction.put(Model.TYPE_EVENT);
        eventInstruction.put(Model.HANDLER_KEY_DOM_HANDLER);
        eventInstruction.put(Model.DOM_HANDLER_TYPE, domHandlerType.ordinal());
        return eventInstruction;
    }

    protected void triggerOnKeyPress(final PTInstruction addHandler, final Widget widget, final DomHandlerType domHandlerType, final UIService uiService, final KeyPressEvent event) {

        final PTInstruction eventInstruction = buildEventInstruction(addHandler, domHandlerType);
        eventInstruction.put(Model.VALUE, event.getNativeEvent().getKeyCode());

        if (addHandler.containsKey(Model.KEY_FILTER)) {
            final JSONArray jsonArray = addHandler.get(Model.KEY_FILTER).isArray();
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

    private void addDomHandler(final PTInstruction addHandler, final Widget widget, final int domHandlerType, final UIService uiService) {

        final DomHandlerType h = DomHandlerType.values()[domHandlerType];
        switch (h) {
            case CLICK:
                widget.addDomHandler(new ClickHandler() {

                    @Override
                    public void onClick(final ClickEvent event) {
                        triggerMouseEvent(addHandler, widget, h, uiService, event);
                    }

                }, ClickEvent.getType());
                break;
            case DOUBLE_CLICK:
                widget.addDomHandler(new DoubleClickHandler() {

                    @Override
                    public void onDoubleClick(final DoubleClickEvent event) {
                        triggerMouseEvent(addHandler, widget, h, uiService, event);
                    }
                }, DoubleClickEvent.getType());
                break;
            case MOUSE_OVER:
                widget.addDomHandler(new MouseOverHandler() {

                    @Override
                    public void onMouseOver(final MouseOverEvent event) {
                        triggerMouseEvent(addHandler, widget, h, uiService, event);
                    }

                }, MouseOverEvent.getType());
                break;
            case MOUSE_OUT:
                widget.addDomHandler(new MouseOutHandler() {

                    @Override
                    public void onMouseOut(final MouseOutEvent event) {
                        triggerMouseEvent(addHandler, widget, h, uiService, event);
                    }

                }, MouseOutEvent.getType());
                break;
            case MOUSE_DOWN:
                widget.addDomHandler(new MouseDownHandler() {

                    @Override
                    public void onMouseDown(final MouseDownEvent event) {
                        triggerMouseEvent(addHandler, widget, h, uiService, event);
                    }

                }, MouseDownEvent.getType());
                break;
            case MOUSE_UP:
                widget.addDomHandler(new MouseUpHandler() {

                    @Override
                    public void onMouseUp(final MouseUpEvent event) {
                        triggerMouseEvent(addHandler, widget, h, uiService, event);
                    }

                }, MouseUpEvent.getType());
                break;
            case BLUR:
                widget.addDomHandler(new BlurHandler() {

                    @Override
                    public void onBlur(final BlurEvent event) {
                        triggerDomEvent(addHandler, widget, h, uiService, event);
                    }

                }, BlurEvent.getType());
                break;
            case FOCUS:
                widget.addDomHandler(new FocusHandler() {

                    @Override
                    public void onFocus(final FocusEvent event) {
                        triggerDomEvent(addHandler, widget, h, uiService, event);
                    }

                }, FocusEvent.getType());
                break;
            case KEY_PRESS:
                widget.addDomHandler(new KeyPressHandler() {

                    @Override
                    public void onKeyPress(final KeyPressEvent event) {
                        triggerOnKeyPress(addHandler, widget, h, uiService, event);
                    }

                }, KeyPressEvent.getType());
                break;
            case KEY_UP:

                if (widget instanceof TextBoxBase) {
                    final TextBoxBase textBox = (TextBoxBase) widget;
                    textBox.addKeyUpHandler(new KeyUpHandler() {

                        @Override
                        public void onKeyUp(final KeyUpEvent event) {
                            final PTInstruction changeHandlerInstruction = new PTInstruction();
                            changeHandlerInstruction.setObjectID(addHandler.getObjectID());
                            changeHandlerInstruction.put(Model.TYPE_EVENT);
                            changeHandlerInstruction.put(Model.HANDLER_STRING_VALUE_CHANGE_HANDLER);
                            changeHandlerInstruction.put(Model.VALUE, textBox.getText());

                            final PTInstruction eventInstruction = buildEventInstruction(addHandler, h);
                            eventInstruction.put(Model.VALUE, event.getNativeEvent().getKeyCode());

                            if (addHandler.containsKey(Model.KEY_FILTER)) {
                                final JSONArray jsonArray = addHandler.get(Model.KEY_FILTER).isArray();
                                for (int i = 0; i < jsonArray.size(); i++) {
                                    final JSONNumber keyCode = jsonArray.get(i).isNumber();
                                    if (keyCode.doubleValue() == event.getNativeEvent().getKeyCode()) {
                                        uiService.stackInstrution(changeHandlerInstruction);
                                        uiService.stackInstrution(eventInstruction);
                                        uiService.flushEvents();
                                        break;
                                    }
                                }
                            } else {
                                uiService.stackInstrution(changeHandlerInstruction);
                                uiService.stackInstrution(eventInstruction);
                                uiService.flushEvents();
                            }
                            preventOrStopEvent(event);
                        }
                    });
                } else {
                    widget.addDomHandler(new KeyUpHandler() {

                        @Override
                        public void onKeyUp(final KeyUpEvent event) {
                            final PTInstruction eventInstruction = buildEventInstruction(addHandler, h);
                            eventInstruction.put(Model.VALUE, event.getNativeEvent().getKeyCode());

                            if (addHandler.containsKey(Model.KEY_FILTER)) {
                                final JSONArray jsonArray = addHandler.get(Model.KEY_FILTER).isArray();
                                for (int i = 0; i < jsonArray.size(); i++) {
                                    final JSONNumber keyCode = jsonArray.get(i).isNumber();
                                    if (keyCode.doubleValue() == event.getNativeEvent().getKeyCode()) {
                                        uiService.sendDataToServer(widget, eventInstruction);
                                        uiService.flushEvents();
                                        break;
                                    }
                                }
                            } else {
                                uiService.sendDataToServer(widget, eventInstruction);
                                uiService.flushEvents();
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
                        event.setData("text", Long.toString(addHandler.getObjectID()));
                        event.getDataTransfer().setDragImage(uiObject.getElement(), 10, 10);
                        triggerDomEvent(addHandler, widget, h, uiService, event);
                    }
                }, DragStartEvent.getType());
                break;
            case DRAG_END:
                widget.addBitlessDomHandler(new DragEndHandler() {

                    @Override
                    public void onDragEnd(final DragEndEvent event) {
                        triggerDomEvent(addHandler, widget, h, uiService, event);
                    }
                }, DragEndEvent.getType());
                break;
            case DRAG_ENTER:
                widget.addBitlessDomHandler(new DragEnterHandler() {

                    @Override
                    public void onDragEnter(final DragEnterEvent event) {
                        triggerDomEvent(addHandler, widget, h, uiService, event);
                    }
                }, DragEnterEvent.getType());
                break;
            case DRAG_LEAVE:
                widget.addBitlessDomHandler(new DragLeaveHandler() {

                    @Override
                    public void onDragLeave(final DragLeaveEvent event) {
                        triggerDomEvent(addHandler, widget, h, uiService, event);
                    }
                }, DragLeaveEvent.getType());
                break;
            case DROP:
                widget.addBitlessDomHandler(new DragOverHandler() {

                    @Override
                    public void onDragOver(final DragOverEvent event) {
                        // required by GWT api
                        // triggerDomEvent(addHandler, domHandlerType, uiService);
                    }
                }, DragOverEvent.getType());

                widget.addBitlessDomHandler(new DropHandler() {

                    @Override
                    public void onDrop(final DropEvent event) {
                        event.preventDefault();
                        final String dragWidgetID = event.getData("text");
                        final PTInstruction eventInstruction = buildEventInstruction(addHandler, h);
                        if (dragWidgetID != null) eventInstruction.put(Model.DRAG_SRC, Long.parseLong(dragWidgetID));
                        uiService.sendDataToServer(widget, eventInstruction);
                    }
                }, DropEvent.getType());
                break;
            case CONTEXT_MENU:
                widget.addDomHandler(new ContextMenuHandler() {

                    @Override
                    public void onContextMenu(final ContextMenuEvent event) {
                        triggerDomEvent(addHandler, widget, h, uiService, event);
                    }
                }, ContextMenuEvent.getType());
                break;
            default:
                log.info("Handler not supported #" + h);
                break;
        }
    }

    protected void preventOrStopEvent(final DomEvent<?> event) {
        preventEvent(event);
        stopEvent(event);
    }

    private void preventEvent(final DomEvent<?> event) {
        if (preventedEvents.isEmpty()) return;

        final int typeInt = Event.as(event.getNativeEvent()).getTypeInt();
        if (preventedEvents.contains(typeInt)) {
            event.preventDefault();
        }
    }

    private void stopEvent(final DomEvent<?> event) {
        if (stoppedEvents.isEmpty()) return;

        final int typeInt = Event.as(event.getNativeEvent()).getTypeInt();
        if (stoppedEvents.contains(typeInt)) {
            event.stopPropagation();
        }
    }
}
