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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.DomHandlerType;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.instruction.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTWidget extends PTUIObject {

    private final static Logger log = Logger.getLogger(PTWidget.class.getName());

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new Widget());
    }

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {
        final String handlerType = addHandler.getString(HANDLER.KEY);

        if (handlerType.equals(HANDLER.DOM_HANDLER)) {
            final int domHandlerType = addHandler.getInt(PROPERTY.DOM_HANDLER_CODE);
            final Widget w = asWidget(addHandler.getObjectID(), uiService);
            addDomHandler(addHandler, w, domHandlerType, uiService);
        } else {
            super.addHandler(addHandler, uiService);
        }
    }

    @Override
    public void removeHandler(final PTInstruction removeHandler, final UIService uiService) {
        if (removeHandler.containsKey(HANDLER.DOM_HANDLER)) {
            final int domHandlerType = removeHandler.getInt(PROPERTY.DOM_HANDLER_CODE);
            final Widget w = asWidget(removeHandler.getObjectID(), uiService);
            final HandlerRegistration handlerRegistration;
            // handlerRegistration.removeHandler()
            // removeDomHandler(removeHandler, w, domHandlerType, uiService);
        } else {
            super.removeHandler(removeHandler, uiService);
        }

    }

    @Override
    public Widget asWidget(final Long objectID, final UIService uiService) {
        final PTWidget child = (PTWidget) uiService.getPTObject(objectID);
        return child.cast();
    }

    protected void triggerOnClick(final PTInstruction addHandler, final Widget widget, final int domHandlerType, final UIService uiService, final ClickEvent event) {
        final PTInstruction eventInstruction = new PTInstruction();
        eventInstruction.setObjectID(addHandler.getObjectID());
        eventInstruction.put(TYPE.KEY, TYPE.EVENT);
        eventInstruction.put(HANDLER.KEY, HANDLER.DOM_HANDLER);
        eventInstruction.put(PROPERTY.DOM_HANDLER_TYPE, domHandlerType); // TODO nciaravola must be a property
        eventInstruction.put(PROPERTY.CLIENT_X, event.getClientX());
        eventInstruction.put(PROPERTY.CLIENT_Y, event.getClientY());
        eventInstruction.put(PROPERTY.SOURCE_ABSOLUTE_LEFT, widget.getAbsoluteLeft());
        eventInstruction.put(PROPERTY.SOURCE_ABSOLUTE_TOP, widget.getAbsoluteTop());
        eventInstruction.put(PROPERTY.SOURCE_OFFSET_HEIGHT, widget.getOffsetHeight());
        eventInstruction.put(PROPERTY.SOURCE_OFFSET_WIDTH, widget.getOffsetWidth());
        uiService.triggerEvent(eventInstruction);
    }

    protected void triggerOnMouseOver(final PTInstruction addHandler, final int domHandlerType, final UIService uiService) {
        final PTInstruction eventInstruction = new PTInstruction();
        eventInstruction.setObjectID(addHandler.getObjectID());
        eventInstruction.put(TYPE.KEY, TYPE.EVENT);
        eventInstruction.put(PROPERTY.DOM_HANDLER_TYPE, domHandlerType);
        uiService.triggerEvent(eventInstruction);
    }

    protected void triggerOnMouseOut(final PTInstruction addHandler, final int domHandlerType, final UIService uiService) {
        final PTInstruction eventInstruction = new PTInstruction();
        eventInstruction.setObjectID(addHandler.getObjectID());
        eventInstruction.put(TYPE.KEY, TYPE.EVENT);
        eventInstruction.put(PROPERTY.DOM_HANDLER_TYPE, domHandlerType);
        uiService.triggerEvent(eventInstruction);
    }

    protected void triggerOnKeyPress(final PTInstruction addHandler, final int domHandlerType, final UIService uiService, final KeyPressEvent event) {
        addHandler.put(TYPE.KEY, TYPE.EVENT);
        addHandler.put(PROPERTY.VALUE, event.getNativeEvent().getKeyCode());
        if (addHandler.containsKey(PROPERTY.KEY_FILTER)) {
            final JSONObject keys = addHandler.get(PROPERTY.KEY_FILTER).isObject();
            if (keys.containsKey(event.getNativeEvent().getKeyCode() + "")) {
                uiService.triggerEvent(addHandler);
            }
        } else {
            // final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(),
            // addHandler.getType());
            // eventInstruction.setMainProperty(main);
            uiService.triggerEvent(addHandler);
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
                        triggerOnMouseOver(addHandler, domHandlerType, uiService);
                    }

                }, MouseOverEvent.getType());
                break;
            case MOUSE_OUT:
                widget.addDomHandler(new MouseOutHandler() {

                    @Override
                    public void onMouseOut(final MouseOutEvent event) {
                        triggerOnMouseOut(addHandler, domHandlerType, uiService);
                    }

                }, MouseOutEvent.getType());
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
                            changeHandlerInstruction.put(TYPE.KEY, TYPE.EVENT);
                            changeHandlerInstruction.put(HANDLER.KEY, HANDLER.STRING_VALUE_CHANGE_HANDLER);
                            changeHandlerInstruction.put(PROPERTY.VALUE, textBox.getText());

                            final PTInstruction eventInstruction = new PTInstruction();
                            eventInstruction.setObjectID(addHandler.getObjectID());
                            eventInstruction.put(TYPE.KEY, TYPE.EVENT);
                            eventInstruction.put(HANDLER.KEY, HANDLER.DOM_HANDLER);
                            eventInstruction.put(PROPERTY.VALUE, event.getNativeEvent().getKeyCode());

                            if (addHandler.containsKey(PROPERTY.KEY_FILTER)) {
                                final JSONObject jsonValue = addHandler.get(PROPERTY.KEY_FILTER).isObject();
                                if (jsonValue.containsKey(event.getNativeEvent().getKeyCode() + "")) {
                                    uiService.stackEvent(changeHandlerInstruction);
                                    uiService.stackEvent(eventInstruction);
                                    uiService.flushEvents();
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
                            final PTInstruction eventInstruction = new PTInstruction();
                            eventInstruction.put(TYPE.KEY, TYPE.EVENT);
                            eventInstruction.setObjectID(addHandler.getObjectID());
                            eventInstruction.put(PROPERTY.VALUE, event.getNativeEvent().getKeyCode());
                            eventInstruction.put(PROPERTY.DOM_HANDLER_TYPE, domHandlerType);
                            if (eventInstruction.containsKey(PROPERTY.KEY_FILTER)) {
                                final JSONObject jsonValue = addHandler.get(PROPERTY.KEY_FILTER).isObject();
                                if (jsonValue.containsKey(event.getNativeEvent().getKeyCode() + "")) {
                                    uiService.triggerEvent(eventInstruction);
                                }
                            } else {
                                uiService.triggerEvent(eventInstruction);
                            }
                        }
                    }, KeyUpEvent.getType());
                }
                break;
            default:
                log.info("Handler not supported #" + h);
                break;
        }
    }

    @Override
    public Widget cast() {
        return (Widget) uiObject;
    }

}
