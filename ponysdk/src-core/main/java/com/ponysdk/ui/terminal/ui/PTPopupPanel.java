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

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.ponysdk.ui.terminal.Dictionnary;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTPopupPanel extends PTSimplePanel implements MouseDownHandler, MouseUpHandler, MouseMoveHandler {

    private boolean dragging;

    private int dragStartX;

    private int dragStartY;

    private int clientLeft;

    private int windowWidth;

    private int clientTop;

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        final boolean autoHide = create.getBoolean(PROPERTY.POPUP_AUTO_HIDE);

        init(create, uiService, createPopupPanel(autoHide));

        addCloseHandler(create, uiService);

        windowWidth = Window.getClientWidth();
        clientLeft = Document.get().getBodyOffsetLeft();
        clientTop = Document.get().getBodyOffsetTop();
    }

    protected PopupPanel createPopupPanel(final boolean autoHide) {
        final PopupPanel popup = new PopupPanel(autoHide) {

            @Override
            protected void onPreviewNativeEvent(final NativePreviewEvent event) {
                PTPopupPanel.this.onPreviewNativeEvent(event);

                super.onPreviewNativeEvent(event);
            }
        };
        return popup;
    }

    protected void addCloseHandler(final PTInstruction create, final UIService uiService) {
        cast().addCloseHandler(new CloseHandler<PopupPanel>() {

            @Override
            public void onClose(final CloseEvent<PopupPanel> event) {
                final PTInstruction instruction = new PTInstruction();
                instruction.setObjectID(create.getObjectID());
                instruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
                instruction.put(HANDLER.KEY, Dictionnary.HANDLER.KEY_.CLOSE_HANDLER);
                uiService.triggerEvent(instruction);
            }
        });
    }

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {
        final String handler = addHandler.getString(HANDLER.KEY);

        if (HANDLER.KEY_.POPUP_POSITION_CALLBACK.equals(handler)) {
            final PopupPanel popupPanel = cast();

            popupPanel.setVisible(false);
            popupPanel.show();

            final PTInstruction eventInstruction = new PTInstruction();
            eventInstruction.setObjectID(addHandler.getObjectID());
            eventInstruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
            eventInstruction.put(HANDLER.KEY, HANDLER.KEY_.POPUP_POSITION_CALLBACK);
            eventInstruction.put(PROPERTY.OFFSETWIDTH, popupPanel.getOffsetWidth());
            eventInstruction.put(PROPERTY.OFFSETHEIGHT, popupPanel.getOffsetHeight());
            eventInstruction.put(PROPERTY.CLIENT_WIDTH, Window.getClientWidth());
            eventInstruction.put(PROPERTY.CLIENT_HEIGHT, Window.getClientHeight());
            uiService.triggerEvent(eventInstruction);
            return;
        }

        super.addHandler(addHandler, uiService);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        final PopupPanel popup = cast();

        if (update.containsKey(PROPERTY.ANIMATION)) {
            popup.setAnimationEnabled(update.getBoolean(PROPERTY.ANIMATION));
        } else if (update.containsKey(PROPERTY.POPUP_CENTER)) {
            popup.center();
        } else if (update.containsKey(PROPERTY.POPUP_SHOW)) {
            popup.show();
        } else if (update.containsKey(PROPERTY.POPUP_HIDE)) {
            popup.hide();
        } else if (update.containsKey(PROPERTY.POPUP_GLASS_ENABLED)) {
            popup.setGlassEnabled(update.getBoolean(PROPERTY.POPUP_GLASS_ENABLED));
        } else if (update.containsKey(PROPERTY.POPUP_MODAL)) {
            popup.setModal(update.getBoolean(PROPERTY.POPUP_MODAL));
        } else if (update.containsKey(PROPERTY.POPUP_POSITION)) {
            final int left = update.getInt(PROPERTY.POPUP_POSITION_LEFT);
            final int top = update.getInt(PROPERTY.POPUP_POSITION_TOP);
            popup.setPopupPosition(left, top);
        } else if (update.containsKey(PROPERTY.POPUP_DRAGGABLE)) {
            final boolean draggable = update.containsKey(PROPERTY.POPUP_DRAGGABLE);
            if (draggable) {
                popup.addDomHandler(this, MouseDownEvent.getType());
                popup.addDomHandler(this, MouseUpEvent.getType());
                popup.addDomHandler(this, MouseMoveEvent.getType());
            }
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public void onMouseDown(final MouseDownEvent event) {
        if (DOM.getCaptureElement() == null) {
            dragging = true;
            DOM.setCapture(cast().getElement());
            dragStartX = event.getX();
            dragStartY = event.getY();
        }
    }

    @Override
    public void onMouseMove(final MouseMoveEvent event) {
        if (dragging) {
            final int absX = event.getX() + uiObject.getAbsoluteLeft();
            final int absY = event.getY() + uiObject.getAbsoluteTop();

            if (absX < clientLeft || absX >= windowWidth || absY < clientTop) { return; }

            cast().setPopupPosition(absX - dragStartX, absY - dragStartY);
        }
    }

    @Override
    public void onMouseUp(final MouseUpEvent event) {
        dragging = false;
        DOM.releaseCapture(uiObject.getElement());
    }

    @Override
    public PopupPanel cast() {
        return (PopupPanel) uiObject;
    }

    protected void onPreviewNativeEvent(final NativePreviewEvent event) {
        // NativeEvent nativeEvent = event.getNativeEvent();
        //
        // if (!event.isCanceled() && (event.getTypeInt() == Event.ONMOUSEDOWN) /* &&
        // isCaptionEvent(nativeEvent) */) {
        // nativeEvent.preventDefault();
        // }
    }
}
