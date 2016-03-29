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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTPopupPanel extends PTSimplePanel implements MouseDownHandler, MouseUpHandler, MouseMoveHandler {

    private boolean dragging;

    private int dragStartX;

    private int dragStartY;

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        final boolean autoHide = create.containsKey(Model.POPUP_AUTO_HIDE) ? create.getBoolean(Model.POPUP_AUTO_HIDE) : false;
        init(create, uiService, createPopupPanel(autoHide));
        addCloseHandler(create, uiService);
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
                instruction.put(Model.HANDLER_CLOSE_HANDLER);
                uiService.sendDataToServer(cast(), instruction);
            }
        });
    }

    @Override
    public void addHandler(final PTInstruction instrcution, final UIService uiService) {
        if (instrcution.containsKey(Model.HANDLER_POPUP_POSITION_CALLBACK)) {
            final PopupPanel popup = cast();
            popup.setVisible(true);
            popup.show();
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                @Override
                public void execute() {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(instrcution.getObjectID());

                    final JSONArray widgetInfo = new JSONArray();
                    widgetInfo.set(0, new JSONNumber(popup.getOffsetWidth()));
                    widgetInfo.set(1, new JSONNumber(popup.getOffsetHeight()));
                    widgetInfo.set(3, new JSONNumber(Window.getClientWidth()));
                    widgetInfo.set(4, new JSONNumber(Window.getClientHeight()));

                    eventInstruction.put(Model.HANDLER_POPUP_POSITION_CALLBACK, widgetInfo);

                    uiService.sendDataToServer(cast(), eventInstruction);
                }
            });

            return;
        }

        super.addHandler(instrcution, uiService);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        final PopupPanel popup = cast();

        if (update.containsKey(Model.ANIMATION)) {
            popup.setAnimationEnabled(update.getBoolean(Model.ANIMATION));
        } else if (update.containsKey(Model.POPUP_CENTER)) {
            popup.show();
            popup.center();
        } else if (update.containsKey(Model.POPUP_SHOW)) {
            popup.show();
        } else if (update.containsKey(Model.POPUP_POSITION_AND_SHOW)) {
            popup.setVisible(true);
        } else if (update.containsKey(Model.POPUP_HIDE)) {
            popup.hide();
        } else if (update.containsKey(Model.POPUP_GLASS_ENABLED)) {
            popup.setGlassEnabled(update.getBoolean(Model.POPUP_GLASS_ENABLED));
        } else if (update.containsKey(Model.POPUP_MODAL)) {
            popup.setModal(update.getBoolean(Model.POPUP_MODAL));
        } else if (update.containsKey(Model.POPUP_POSITION)) {
            final int left = update.getInt(Model.POPUP_POSITION_LEFT);
            final int top = update.getInt(Model.POPUP_POSITION_TOP);
            popup.setPopupPosition(left, top);
        } else if (update.containsKey(Model.POPUP_DRAGGABLE)) {
            popup.addDomHandler(this, MouseDownEvent.getType());
            popup.addDomHandler(this, MouseUpEvent.getType());
            popup.addDomHandler(this, MouseMoveEvent.getType());
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

            if (absX < Document.get().getBodyOffsetLeft() || absX >= Window.getClientWidth()
                    || absY < Document.get().getBodyOffsetTop()) {
                return;
            }

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
