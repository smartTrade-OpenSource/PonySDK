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

import com.google.gwt.user.client.ui.DisclosurePanel;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTDisclosurePanel extends PTWidget<DisclosurePanel> {

    private static final String OPENNED = "images/disclosure_openned.png";
    private static final String CLOSED = "images/disclosure_closed.png";

    private String headerText;
    private PImageResource openImageResource;
    private PImageResource closeImageResource;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        headerText = buffer.readBinaryModel().getStringValue();

        // TODO nciaravola pass url + size in parameters
        openImageResource = new PImageResource(OPENNED, 0, 0, 14, 14);
        closeImageResource = new PImageResource(CLOSED, 0, 0, 14, 14);
        super.create(buffer, objectId, uiService);
        addHandlers(buffer, uiService);
    }

    @Override
    protected DisclosurePanel createUIObject() {
        return new DisclosurePanel(openImageResource, closeImageResource, headerText);
    }

    private void addHandlers(final ReaderBuffer buffer, final UIBuilder uiService) {
        uiObject.addCloseHandler(event -> {
            final PTInstruction instruction = new PTInstruction(getObjectID());
            instruction.put(ClientToServerModel.HANDLER_CLOSE);
            uiService.sendDataToServer(uiObject, instruction);
        });

        uiObject.addOpenHandler(event -> {
            final PTInstruction instruction = new PTInstruction(getObjectID());
            instruction.put(ClientToServerModel.HANDLER_OPEN);
            uiService.sendDataToServer(uiObject, instruction);
        });
    }

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        uiObject.setContent(asWidget(ptObject));
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.OPEN.ordinal() == modelOrdinal) {
            uiObject.setOpen(true);
            return true;
        } else if (ServerToClientModel.CLOSE.ordinal() == modelOrdinal) {
            uiObject.setOpen(false);
            return true;
        } else if (ServerToClientModel.ANIMATION.ordinal() == modelOrdinal) {
            uiObject.setAnimationEnabled(binaryModel.getBooleanValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

}
