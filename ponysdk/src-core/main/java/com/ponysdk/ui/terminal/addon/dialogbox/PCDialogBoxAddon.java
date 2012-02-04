/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.ui.terminal.addon.dialogbox;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import com.ponysdk.ui.terminal.Addon;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.PonyAddOn;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;
import com.ponysdk.ui.terminal.ui.PTDialogBox;
import com.ponysdk.ui.terminal.ui.PTWidget;

@PonyAddOn
public class PCDialogBoxAddon extends PTDialogBox implements Addon {

    public static final String SIGNATURE = "com.ponysdk.ui.terminal.addon.dialogbox.PCDialogBoxAddon";

    private PCDialogBox dialogBox;

    @Override
    public void create(final Create create, final UIService uiService) {
        dialogBox = new PCDialogBox();
        dialogBox.addCloseHandler(new CloseHandler<PopupPanel>() {

            @Override
            public void onClose(final CloseEvent<PopupPanel> event) {
                uiService.triggerEvent(new EventInstruction(create.getObjectID(), HandlerType.CLOSE_HANDLER));
            }
        });

        init(dialogBox);
    }

    @Override
    public void update(final Update update, final UIService uiService) {
        final Property mainProperty = update.getMainProperty();

        for (final Property property : mainProperty.getChildProperties().values()) {
            final PropertyKey propertyKey = property.getKey();
            if (PropertyKey.POPUP_TEXT.equals(propertyKey)) {
                dialogBox.setText(property.getValue());
            } else if (PropertyKey.DIALOG_BOX_CLOSE_WIDGET.equals(propertyKey)) {
                final PTWidget widget = (PTWidget) uiService.getUIObject(property.getLongValue());
                dialogBox.setCloseWidget(widget.cast());
            } else if (PropertyKey.DIALOG_BOX_CLOSABLE.equals(propertyKey)) {
                dialogBox.setClosable(property.getBooleanValue());
            }
        }

        super.update(update, uiService);
    }

    @Override
    public String getSignature() {
        return SIGNATURE;
    }

    @Override
    public PTWidget asPTWidget() {
        return this;
    }
}
