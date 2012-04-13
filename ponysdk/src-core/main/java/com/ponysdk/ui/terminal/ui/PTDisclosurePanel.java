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
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTDisclosurePanel extends PTWidget {

    @Override
    public void create(final Create create, final UIService uiService) {
        final Property mainProperty = create.getMainProperty();

        final Long openImg = mainProperty.getLongPropertyValue(PropertyKey.DISCLOSURE_PANEL_OPEN_IMG);
        final Long closeImg = mainProperty.getLongPropertyValue(PropertyKey.DISCLOSURE_PANEL_CLOSE_IMG);

        final String headerText = mainProperty.getValue();

        final PTImage open = (PTImage) uiService.getPTObject(openImg);
        final PTImage close = (PTImage) uiService.getPTObject(closeImg);

        final PImageResource openImageResource = new PImageResource(open.cast());
        final PImageResource closeImageResource = new PImageResource(close.cast());

        init(create, uiService, new DisclosurePanel(openImageResource, closeImageResource, headerText));

        addHandlers(create, uiService);
    }

    private void addHandlers(final Create create, final UIService uiService) {
        cast().addCloseHandler(new CloseHandler<DisclosurePanel>() {

            @Override
            public void onClose(final CloseEvent<DisclosurePanel> event) {
                uiService.triggerEvent(new EventInstruction(create.getObjectID(), HandlerType.CLOSE_HANDLER));
            }
        });

        cast().addOpenHandler(new OpenHandler<DisclosurePanel>() {

            @Override
            public void onOpen(final OpenEvent<DisclosurePanel> event) {
                uiService.triggerEvent(new EventInstruction(create.getObjectID(), HandlerType.OPEN_HANDLER));
            }
        });
    }

    @Override
    public void add(final Add add, final UIService uiService) {
        final com.google.gwt.user.client.ui.Widget w = asWidget(add.getObjectID(), uiService);
        cast().setContent(w);
    }

    @Override
    public void update(final Update update, final UIService uiService) {

        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getPropertyKey();
        switch (propertyKey) {
            case OPEN:
                cast().setOpen(property.getBooleanValue());
                break;
            default:
                super.update(update, uiService);
        }
    }

    @Override
    public DisclosurePanel cast() {
        return (DisclosurePanel) uiObject;
    }
}
