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

import com.google.gwt.user.client.ui.RootPanel;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import elemental.client.Browser;
import elemental.dom.Document;

public class PTRootPanel extends PTAbsolutePanel {

    private String rootId;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        final BinaryModel binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.ROOT_ID == binaryModel.getModel()) rootId = binaryModel.getStringValue();
        else buffer.rewind(binaryModel);

        super.create(buffer, objectId, uiService);

        final Document document = Browser.getWindow().getDocument();
        if (PTAbstractWindow.isPageVisibilityAPI(document)) {
            document.addEventListener("visibilitychange", event -> sendDocumentVisibility(document));
            sendDocumentVisibility(document);
        }
    }

    @Override
    protected RootPanel createUIObject() {
        return rootId != null ? RootPanel.get(rootId) : RootPanel.get();
    }

    private void sendDocumentVisibility(final Document document) {
        final PTInstruction eventInstruction = new PTInstruction(objectID);
        eventInstruction.put(ClientToServerModel.HANDLER_DOCUMENT_VISIBILITY, PTAbstractWindow.isDocumentVisible(document));
        uiBuilder.sendDataToServer(eventInstruction);
    }

}
