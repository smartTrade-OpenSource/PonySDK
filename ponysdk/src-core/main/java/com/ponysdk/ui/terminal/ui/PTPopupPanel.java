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
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTPopupPanel extends PTSimplePanel implements MouseDownHandler, MouseUpHandler, MouseMoveHandler {

    private boolean dragging;

    private int dragStartX;

    private int dragStartY;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        boolean autoHide = false;
        final BinaryModel binaryModel = buffer.getBinaryModel();
        if (Model.POPUP_AUTO_HIDE.equals(binaryModel.getModel())) {
            autoHide = binaryModel.getBooleanValue();
        } else {
            buffer.rewind(binaryModel);
        }
        this.uiObject = createPopupPanel(autoHide);
        this.objectID = objectId;
        uiService.registerUIObject(this.objectID, uiObject);
        addCloseHandler(uiService);
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

    protected void addCloseHandler(final UIService uiService) {
        cast().addCloseHandler(new CloseHandler<PopupPanel>() {

            @Override
            public void onClose(final CloseEvent<PopupPanel> event) {
                final PTInstruction instruction = new PTInstruction();
                instruction.setObjectID(getObjectID());
                instruction.put(Model.HANDLER_CLOSE_HANDLER);
                uiService.sendDataToServer(cast(), instruction);
            }
        });
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel, final UIService uiService) {
        if (HandlerModel.HANDLER_POPUP_POSITION_CALLBACK.equals(handlerModel)) {
            final PopupPanel popup = cast();
            popup.setVisible(true);
            popup.show();
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                @Override
                public void execute() {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(getObjectID());

                    final JSONArray widgetInfo = new JSONArray();
                    widgetInfo.set(0, new JSONNumber(popup.getOffsetWidth()));
                    widgetInfo.set(1, new JSONNumber(popup.getOffsetHeight()));
                    widgetInfo.set(3, new JSONNumber(Window.getClientWidth()));
                    widgetInfo.set(4, new JSONNumber(Window.getClientHeight()));

                    eventInstruction.put(Model.WIDGET_POSITION, widgetInfo);

                    uiService.sendDataToServer(cast(), eventInstruction);
                }
            });

            return;
        } else {
            super.addHandler(buffer, handlerModel, uiService);
        }
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final PopupPanel popup = cast();
        if (Model.ANIMATION.equals(binaryModel.getModel())) {
            popup.setAnimationEnabled(binaryModel.getBooleanValue());
            return true;
        }
        if (Model.POPUP_CENTER.equals(binaryModel.getModel())) {
            popup.show();
            popup.center();
            return true;
        }
        if (Model.POPUP_SHOW.equals(binaryModel.getModel())) {
            popup.show();
            return true;
        }
        if (Model.POPUP_POSITION_AND_SHOW.equals(binaryModel.getModel())) {
            popup.setVisible(true);
            return true;
        }
        if (Model.POPUP_HIDE.equals(binaryModel.getModel())) {
            popup.hide();
            return true;
        }
        if (Model.POPUP_GLASS_ENABLED.equals(binaryModel.getModel())) {
            popup.setGlassEnabled(binaryModel.getBooleanValue());
            return true;
        }
        if (Model.POPUP_MODAL.equals(binaryModel.getModel())) {
            popup.setModal(binaryModel.getBooleanValue());
            return true;
        }
        if (Model.POPUP_POSITION_LEFT.equals(binaryModel.getModel())) {
            final int left = binaryModel.getIntValue();
            // Model.POPUP_POSITION_TOP
            final int top = buffer.getBinaryModel().getIntValue();
            popup.setPopupPosition(left, top);
            return true;
        }
        if (Model.POPUP_DRAGGABLE.equals(binaryModel.getModel())) {
            popup.addDomHandler(this, MouseDownEvent.getType());
            popup.addDomHandler(this, MouseUpEvent.getType());
            popup.addDomHandler(this, MouseMoveEvent.getType());
            return true;
        }
        return super.update(buffer, binaryModel);
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
        // if (!event.isCanceled() && (event.getTypeInt() == Event.ONMOUSEDOWN)
        // /* &&
        // isCaptionEvent(nativeEvent) */) {
        // nativeEvent.preventDefault();
        // }
    }
}
