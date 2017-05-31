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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.PonySDK;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

import elemental.html.Uint8Array;

public class PTFrame extends PTWidget<HTMLPanel> implements PostMessageHandler {

    private static final Logger log = Logger.getLogger(PTFrame.class.getName());

    private String url;

    private boolean ready = false;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder builder) {
        if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Create PTFrame #" + objectId);

        final BinaryModel binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.URL.ordinal() == binaryModel.getModel().ordinal()) {
            url = binaryModel.getStringValue();
        } else {
            url = "";
            buffer.rewind(binaryModel);
        }

        super.create(buffer, objectId, builder);
    }

    @Override
    protected HTMLPanel createUIObject() {
        final HTMLPanel iframe = new HTMLPanel("iframe", "");

        final String frameID = ClientToServerModel.FRAME_ID.toStringValue() + "=" + objectID;
        final String contextID = ClientToServerModel.UI_CONTEXT_ID.toStringValue() + "=" + PonySDK.get().getContextId();

        url += url.contains("?") ? "&" : "?";
        url += contextID + "&" + frameID;

        iframe.getElement().setAttribute("src", url);
        return iframe;
    }

    @Override
    public void postMessage(final Uint8Array buffer) {
        postMessage(uiObject.getElement(), buffer);
    }

    @Override
    public void setReady() {
        ready = true;
        final PTInstruction instruction = new PTInstruction(objectID);
        instruction.put(ClientToServerModel.HANDLER_OPEN, url);
        uiBuilder.sendDataToServer(instruction);
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    public native void postMessage(Element element, final Uint8Array buffer) /*-{
                                                                             element.contentWindow.postMessage(buffer, '*');
                                                                             }-*/;

}
