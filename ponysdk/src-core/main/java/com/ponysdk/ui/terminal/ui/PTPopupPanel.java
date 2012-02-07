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

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTPopupPanel extends PTSimplePanel {

    @Override
    public void create(final Create create, final UIService uiService) {
        init(create, uiService, new com.google.gwt.user.client.ui.PopupPanel(create.getMainProperty().getBooleanProperty(PropertyKey.POPUP_AUTO_HIDE)));
        final com.google.gwt.user.client.ui.PopupPanel popupPanel = cast();
        popupPanel.addCloseHandler(new CloseHandler<PopupPanel>() {

            @Override
            public void onClose(final CloseEvent<PopupPanel> event) {
                uiService.triggerEvent(new EventInstruction(create.getObjectID(), HandlerType.CLOSE_HANDLER));
            }
        });
    }

    @Override
    public void addHandler(final AddHandler addHandler, final UIService uiService) {
        if (HandlerType.POPUP_POSITION_CALLBACK.equals(addHandler.getType())) {
            final com.google.gwt.user.client.ui.PopupPanel popupPanel = cast();

            popupPanel.setVisible(false);
            popupPanel.show();

            final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), HandlerType.POPUP_POSITION_CALLBACK);
            eventInstruction.getMainProperty().setProperty(PropertyKey.OFFSETWIDTH, popupPanel.getOffsetWidth());
            eventInstruction.getMainProperty().setProperty(PropertyKey.OFFSETHEIGHT, popupPanel.getOffsetHeight());
            eventInstruction.getMainProperty().setProperty(PropertyKey.CLIENT_WIDTH, Window.getClientWidth());
            eventInstruction.getMainProperty().setProperty(PropertyKey.CLIENT_HEIGHT, Window.getClientHeight());

            uiService.triggerEvent(eventInstruction);
            return;
        }

        super.addHandler(addHandler, uiService);
    }

    @Override
    public void update(final Update update, final UIService uiService) {

        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getKey();
        final com.google.gwt.user.client.ui.PopupPanel popup = cast();

        if (PropertyKey.ANIMATION.equals(propertyKey)) {
            popup.setAnimationEnabled(property.getBooleanValue());
        } else if (PropertyKey.WORD_WRAP.equals(propertyKey)) {} else if (PropertyKey.POPUP_CENTER.equals(propertyKey)) {
            popup.center();
        } else if (PropertyKey.POPUP_SHOW.equals(propertyKey)) {
            popup.show();
        } else if (PropertyKey.POPUP_HIDE.equals(propertyKey)) {
            popup.hide();
        } else if (PropertyKey.POPUP_GLASS_ENABLED.equals(propertyKey)) {
            popup.setGlassEnabled(property.getBooleanValue());
        } else if (PropertyKey.POPUP_POSITION.equals(propertyKey)) {
            final int left = property.getIntProperty(PropertyKey.POPUP_POSITION_LEFT);
            final int top = property.getIntProperty(PropertyKey.POPUP_POSITION_TOP);
            popup.setPopupPosition(left, top);
        } else {
            super.update(update, uiService);
        }

    }

    @Override
    public com.google.gwt.user.client.ui.PopupPanel cast() {
        return (com.google.gwt.user.client.ui.PopupPanel) uiObject;
    }
}
