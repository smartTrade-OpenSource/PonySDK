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
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTDisclosurePanel extends PTWidget<DisclosurePanel> {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        final Long openImg = create.getLong(PROPERTY.DISCLOSURE_PANEL_OPEN_IMG);
        final Long closeImg = create.getLong(PROPERTY.DISCLOSURE_PANEL_CLOSE_IMG);
        final String headerText = create.getString(PROPERTY.TEXT);

        final PTImage open = (PTImage) uiService.getPTObject(openImg);
        final PTImage close = (PTImage) uiService.getPTObject(closeImg);

        final PImageResource openImageResource = new PImageResource(open.cast());
        final PImageResource closeImageResource = new PImageResource(close.cast());

        init(create, uiService, new DisclosurePanel(openImageResource, closeImageResource, headerText));

        addHandlers(create, uiService);
    }

    private void addHandlers(final PTInstruction create, final UIService uiService) {
        uiObject.addCloseHandler(new CloseHandler<DisclosurePanel>() {

            @Override
            public void onClose(final CloseEvent<DisclosurePanel> event) {
                final PTInstruction instruction = new PTInstruction();
                instruction.setObjectID(create.getObjectID());
                instruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
                instruction.put(HANDLER.KEY, HANDLER.KEY_.CLOSE_HANDLER);

                uiService.sendDataToServer(instruction);
            }
        });

        uiObject.addOpenHandler(new OpenHandler<DisclosurePanel>() {

            @Override
            public void onOpen(final OpenEvent<DisclosurePanel> event) {
                final PTInstruction instruction = new PTInstruction();
                instruction.setObjectID(create.getObjectID());
                instruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
                instruction.put(HANDLER.KEY, HANDLER.KEY_.OPEN_HANDLER);

                uiService.sendDataToServer(instruction);
            }
        });
    }

    @Override
    public void add(final PTInstruction add, final UIService uiService) {
        uiObject.setContent(asWidget(add.getObjectID(), uiService));
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.OPEN)) {
            uiObject.setOpen(update.getBoolean(PROPERTY.OPEN));
        } else {
            super.update(update, uiService);
        }
    }

}
