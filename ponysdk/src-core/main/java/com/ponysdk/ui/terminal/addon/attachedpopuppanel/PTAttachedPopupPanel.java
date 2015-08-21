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

package com.ponysdk.ui.terminal.addon.attachedpopuppanel;

import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.PopupPanel;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.ui.PTPopupPanel;
import com.ponysdk.ui.terminal.ui.PTUIObject;

public class PTAttachedPopupPanel extends PTPopupPanel {

    private PTUIObject<?> attached;

    @Override
    public void create(final PTInstruction create, final UIService uiService) {

        attached = (PTUIObject<?>) uiService.getPTObject(create.getLong(PROPERTY.WIDGET));

        super.create(create, uiService);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.REPAINT)) {
            cast().setPopupPosition(attached.cast().getAbsoluteLeft(), attached.cast().getAbsoluteTop() + attached.cast().getOffsetHeight());
            cast().setWidth(attached.cast().getOffsetWidth() + "px");
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    protected PopupPanel createPopupPanel(final boolean autoHide) {
        final PopupPanel popup = new PopupPanel(autoHide) {

            @Override
            protected void onPreviewNativeEvent(final NativePreviewEvent event) {
                PTAttachedPopupPanel.this.onPreviewNativeEvent(event);

                super.onPreviewNativeEvent(event);
            }

            @Override
            public void show() {
                setPopupPosition(attached.cast().getAbsoluteLeft(), attached.cast().getAbsoluteTop() + attached.cast().getOffsetHeight());
                setWidth(attached.cast().getOffsetWidth() + "px");
                super.show();
            }
        };
        return popup;
    }

}
