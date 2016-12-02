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
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTPopupPanel extends PTSimplePanel implements MouseDownHandler, MouseUpHandler, MouseMoveHandler {

    boolean autoHide;
    private boolean dragging;
    private int dragStartX;
    private int dragStartY;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        final BinaryModel binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.POPUP_AUTO_HIDE.equals(binaryModel.getModel())) {
            autoHide = binaryModel.getBooleanValue();
        } else {
            autoHide = false;
            buffer.rewind(binaryModel);
        }

        super.create(buffer, objectId, uiService);

        addCloseHandler(uiService);
    }

    @Override
    protected PopupPanel createUIObject() {
        return new PopupPanel(autoHide) {

            @Override
            protected void onPreviewNativeEvent(final NativePreviewEvent event) {
                PTPopupPanel.this.onPreviewNativeEvent(event);
                super.onPreviewNativeEvent(event);
            }
        };
    }

    protected void addCloseHandler(final UIBuilder uiService) {
        cast().addCloseHandler(new CloseHandler<PopupPanel>() {

            @Override
            public void onClose(final CloseEvent<PopupPanel> event) {
                final PTInstruction instruction = new PTInstruction(getObjectID());
                instruction.put(ClientToServerModel.HANDLER_CLOSE);
                uiService.sendDataToServer(cast(), instruction);
            }
        });
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel, final UIBuilder uiService) {
        if (HandlerModel.HANDLER_POPUP_POSITION.equals(handlerModel)) {
            final PopupPanel popup = cast();
            popup.setVisible(true);
            popup.show();
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                @Override
                public void execute() {
                    final PTInstruction eventInstruction = new PTInstruction(getObjectID());

                    final JSONArray widgetInfo = new JSONArray();
                    int i = 0;
                    widgetInfo.set(i++, new JSONNumber(popup.getOffsetWidth()));
                    widgetInfo.set(i++, new JSONNumber(popup.getOffsetHeight()));
                    widgetInfo.set(i++, new JSONNumber(Window.getClientWidth()));
                    widgetInfo.set(i++, new JSONNumber(Window.getClientHeight()));

                    eventInstruction.put(ClientToServerModel.POPUP_POSITION, widgetInfo);

                    uiService.sendDataToServer(cast(), eventInstruction);
                }
            });
        } else {
            super.addHandler(buffer, handlerModel, uiService);
        }
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final PopupPanel popup = cast();
        if (ServerToClientModel.ANIMATION.equals(binaryModel.getModel())) {
            popup.setAnimationEnabled(binaryModel.getBooleanValue());
            return true;
        } else if (ServerToClientModel.POPUP_CENTER.equals(binaryModel.getModel())) {
            popup.show();
            popup.center();
            return true;
        } else if (ServerToClientModel.POPUP_SHOW.equals(binaryModel.getModel())) {
            popup.show();
            return true;
        } else if (ServerToClientModel.POPUP_POSITION_AND_SHOW.equals(binaryModel.getModel())) {
            popup.setVisible(true);
            return true;
        } else if (ServerToClientModel.POPUP_HIDE.equals(binaryModel.getModel())) {
            popup.hide();
            return true;
        } else if (ServerToClientModel.POPUP_GLASS_ENABLED.equals(binaryModel.getModel())) {
            popup.setGlassEnabled(binaryModel.getBooleanValue());
            return true;
        } else if (ServerToClientModel.POPUP_MODAL.equals(binaryModel.getModel())) {
            popup.setModal(binaryModel.getBooleanValue());
            return true;
        } else if (ServerToClientModel.POPUP_POSITION_LEFT.equals(binaryModel.getModel())) {
            final int left = binaryModel.getIntValue();
            // ServerToClientModel.POPUP_POSITION_TOP
            final int top = buffer.readBinaryModel().getIntValue();
            popup.setPopupPosition(left, top);
            return true;
        } else if (ServerToClientModel.POPUP_DRAGGABLE.equals(binaryModel.getModel())) {
            popup.addDomHandler(this, MouseDownEvent.getType());
            popup.addDomHandler(this, MouseUpEvent.getType());
            popup.addDomHandler(this, MouseMoveEvent.getType());
            return true;
        } else {
            return super.update(buffer, binaryModel);
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

    private void onPreviewNativeEvent(final NativePreviewEvent event) {
        // NativeEvent nativeEvent = eventbus.getNativeEvent();
        //
        // if (!eventbus.isCanceled() && (eventbus.getTypeInt() == Event.ONMOUSEDOWN)
        // /* &&
        // isCaptionEvent(nativeEvent) */) {
        // nativeEvent.preventDefault();
        // }
    }
}
