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

import java.util.logging.Logger;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.DomHandlerType;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTWidget<W extends Widget> extends PTUIObject<W> {

    private final static Logger log = Logger.getLogger(PTWidget.class.getName());

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {
        final String handlerType = addHandler.getString(HANDLER.KEY);

        if (handlerType.equals(HANDLER.KEY_.DOM_HANDLER)) {
            final int domHandlerType = addHandler.getInt(PROPERTY.DOM_HANDLER_CODE);
            final Widget w = asWidget(addHandler.getObjectID(), uiService);
            addDomHandler(addHandler, w, domHandlerType, uiService);
        } else {
            super.addHandler(addHandler, uiService);
        }
    }

    @Override
    public void removeHandler(final PTInstruction removeHandler, final UIService uiService) {
        if (removeHandler.containsKey(HANDLER.KEY_.DOM_HANDLER)) {
            // final int domHandlerType = removeHandler.getInt(PROPERTY.DOM_HANDLER_CODE);
            // final Widget w = asWidget(removeHandler.getObjectID(), uiService);
            // final HandlerRegistration handlerRegistration;
            // handlerRegistration.removeHandler()
            // removeDomHandler(removeHandler, w, domHandlerType, uiService);
        } else {
            super.removeHandler(removeHandler, uiService);
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

    protected void triggerOnClick(final PTInstruction addHandler, final Widget widget, final int domHandlerType, final UIService uiService, final ClickEvent event) {
        final PTInstruction eventInstruction = buildEventInstruction(addHandler, domHandlerType);
        eventInstruction.put(PROPERTY.CLIENT_X, event.getClientX());
        eventInstruction.put(PROPERTY.CLIENT_Y, event.getClientY());
        eventInstruction.put(PROPERTY.SOURCE_ABSOLUTE_LEFT, widget.getAbsoluteLeft());
        eventInstruction.put(PROPERTY.SOURCE_ABSOLUTE_TOP, widget.getAbsoluteTop());
        eventInstruction.put(PROPERTY.SOURCE_OFFSET_HEIGHT, widget.getOffsetHeight());
        eventInstruction.put(PROPERTY.SOURCE_OFFSET_WIDTH, widget.getOffsetWidth());
        uiService.triggerEvent(eventInstruction);
    }

    protected void triggerDomEvent(final PTInstruction addHandler, final int domHandlerType, final UIService uiService) {
        final PTInstruction eventInstruction = buildEventInstruction(addHandler, domHandlerType);
        uiService.triggerEvent(eventInstruction);
    }

    private PTInstruction buildEventInstruction(final PTInstruction addHandler, final int domHandlerType) {
        final PTInstruction eventInstruction = new PTInstruction();
        eventInstruction.setObjectID(addHandler.getObjectID());
        eventInstruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
        eventInstruction.put(HANDLER.KEY, HANDLER.KEY_.DOM_HANDLER);
        eventInstruction.put(PROPERTY.DOM_HANDLER_TYPE, domHandlerType);
        return eventInstruction;
    }

    protected void triggerOnKeyPress(final PTInstruction addHandler, final int domHandlerType, final UIService uiService, final KeyPressEvent event) {

        final PTInstruction eventInstruction = buildEventInstruction(addHandler, domHandlerType);
        eventInstruction.put(PROPERTY.VALUE, event.getNativeEvent().getKeyCode());

        if (addHandler.containsKey(PROPERTY.KEY_FILTER)) {
            final JSONArray jsonArray = addHandler.get(PROPERTY.KEY_FILTER).isArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                final JSONNumber keyCode = jsonArray.get(i).isNumber();
                if (keyCode.doubleValue() == event.getNativeEvent().getKeyCode()) {
                    uiService.triggerEvent(eventInstruction);
                    break;
                }
            }
        } else {
            // final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(),
            // addHandler.getType());
            // eventInstruction.setMainProperty(main);
            uiService.triggerEvent(eventInstruction);
        }
    }

    private void addDomHandler(final PTInstruction addHandler, final Widget widget, final int domHandlerType, final UIService uiService) {

        final DomHandlerType h = DomHandlerType.values()[domHandlerType];
        switch (h) {
            case CLICK:
                widget.addDomHandler(new ClickHandler() {

                    @Override
                    public void onClick(final ClickEvent event) {
                        triggerOnClick(addHandler, widget, domHandlerType, uiService, event);
                    }

                }, ClickEvent.getType());
                break;
            case MOUSE_OVER:
                widget.addDomHandler(new MouseOverHandler() {

                    @Override
                    public void onMouseOver(final MouseOverEvent event) {
                        triggerDomEvent(addHandler, domHandlerType, uiService);
                    }

                }, MouseOverEvent.getType());
                break;
            case BLUR:
                widget.addDomHandler(new BlurHandler() {

                    @Override
                    public void onBlur(final BlurEvent event) {
                        triggerDomEvent(addHandler, domHandlerType, uiService);
                    }

                }, BlurEvent.getType());
                break;
            case FOCUS:
                widget.addDomHandler(new FocusHandler() {

                    @Override
                    public void onFocus(final FocusEvent event) {
                        triggerDomEvent(addHandler, domHandlerType, uiService);
                    }

                }, FocusEvent.getType());
                break;
            case MOUSE_OUT:
                widget.addDomHandler(new MouseOutHandler() {

                    @Override
                    public void onMouseOut(final MouseOutEvent event) {
                        triggerDomEvent(addHandler, domHandlerType, uiService);
                    }

                }, MouseOutEvent.getType());
                break;
            case MOUSE_DOWN:
                widget.addDomHandler(new MouseDownHandler() {

                    @Override
                    public void onMouseDown(final MouseDownEvent event) {
                        triggerDomEvent(addHandler, domHandlerType, uiService);
                    }

                }, MouseDownEvent.getType());
                break;
            case MOUSE_UP:
                widget.addDomHandler(new MouseUpHandler() {

                    @Override
                    public void onMouseUp(final MouseUpEvent event) {
                        triggerDomEvent(addHandler, domHandlerType, uiService);
                    }

                }, MouseUpEvent.getType());
                break;
            case KEY_PRESS:
                widget.addDomHandler(new KeyPressHandler() {

                    @Override
                    public void onKeyPress(final KeyPressEvent event) {
                        triggerOnKeyPress(addHandler, domHandlerType, uiService, event);
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
                            changeHandlerInstruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
                            changeHandlerInstruction.put(HANDLER.KEY, HANDLER.KEY_.STRING_VALUE_CHANGE_HANDLER);
                            changeHandlerInstruction.put(PROPERTY.VALUE, textBox.getText());

                            final PTInstruction eventInstruction = buildEventInstruction(addHandler, domHandlerType);
                            eventInstruction.put(PROPERTY.VALUE, event.getNativeEvent().getKeyCode());

                            if (addHandler.containsKey(PROPERTY.KEY_FILTER)) {
                                final JSONArray jsonArray = addHandler.get(PROPERTY.KEY_FILTER).isArray();
                                for (int i = 0; i < jsonArray.size(); i++) {
                                    final JSONNumber keyCode = jsonArray.get(i).isNumber();
                                    if (keyCode.doubleValue() == event.getNativeEvent().getKeyCode()) {
                                        uiService.stackEvent(changeHandlerInstruction);
                                        uiService.stackEvent(eventInstruction);
                                        uiService.flushEvents();
                                        break;
                                    }
                                }
                            } else {
                                uiService.stackEvent(changeHandlerInstruction);
                                uiService.stackEvent(eventInstruction);
                                uiService.flushEvents();
                            }
                        }
                    });
                } else {
                    widget.addDomHandler(new KeyUpHandler() {

                        @Override
                        public void onKeyUp(final KeyUpEvent event) {
                            final PTInstruction eventInstruction = buildEventInstruction(addHandler, domHandlerType);
                            eventInstruction.put(PROPERTY.VALUE, event.getNativeEvent().getKeyCode());
                            if (eventInstruction.containsKey(PROPERTY.KEY_FILTER)) {
                                final JSONArray jsonArray = addHandler.get(PROPERTY.KEY_FILTER).isArray();
                                for (int i = 0; i < jsonArray.size(); i++) {
                                    final JSONNumber keyCode = jsonArray.get(i).isNumber();
                                    if (keyCode.doubleValue() == event.getNativeEvent().getKeyCode()) {
                                        uiService.triggerEvent(eventInstruction);
                                        break;
                                    }
                                }
                            } else {
                                uiService.triggerEvent(eventInstruction);
                            }
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
                        triggerDomEvent(addHandler, domHandlerType, uiService);
                    }
                }, DragStartEvent.getType());
                break;
            case DRAG_END:
                widget.addBitlessDomHandler(new DragEndHandler() {

                    @Override
                    public void onDragEnd(final DragEndEvent event) {
                        triggerDomEvent(addHandler, domHandlerType, uiService);
                    }
                }, DragEndEvent.getType());
                break;
            case DRAG_ENTER:
                widget.addBitlessDomHandler(new DragEnterHandler() {

                    @Override
                    public void onDragEnter(final DragEnterEvent event) {
                        triggerDomEvent(addHandler, domHandlerType, uiService);
                    }
                }, DragEnterEvent.getType());
                break;
            case DRAG_LEAVE:
                widget.addBitlessDomHandler(new DragLeaveHandler() {

                    @Override
                    public void onDragLeave(final DragLeaveEvent event) {
                        triggerDomEvent(addHandler, domHandlerType, uiService);
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
                        final PTInstruction eventInstruction = buildEventInstruction(addHandler, domHandlerType);
                        if (dragWidgetID != null) eventInstruction.put(PROPERTY.DRAG_SRC, Long.parseLong(dragWidgetID));
                        uiService.triggerEvent(eventInstruction);
                    }
                }, DropEvent.getType());
                break;
            default:
                log.info("Handler not supported #" + h);
                break;
        }
    }
}
