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

import com.google.gwt.core.client.GWT;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.PonySDK;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

import elemental.client.Browser;
import elemental.html.Uint8Array;

public class PTWindow extends PTAbstractWindow implements PostMessageHandler {

    private static final Logger log = Logger.getLogger(PTWindow.class.getName());

    private static final String EMPTY = "";

    private String url = EMPTY;
    private String name = EMPTY;
    private String features;

    private boolean ready = false;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        super.create(buffer, objectId, uiBuilder);

        if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Create PTWindow #" + objectID);

        final boolean relative = buffer.readBinaryModel().getBooleanValue();

        final String rawUrl = buffer.readBinaryModel().getStringValue();
        url = rawUrl != null ? rawUrl : EMPTY;

        final String rawName = buffer.readBinaryModel().getStringValue();
        name = rawName != null ? rawName : EMPTY;

        final String rawFeatures = buffer.readBinaryModel().getStringValue();
        features = rawFeatures != null ? rawFeatures : EMPTY;

        if (relative) {
            url = GWT.getHostPageBaseURL() + url + "?" + ClientToServerModel.WINDOW_ID.toStringValue() + "=" + objectId + "&"
                    + ClientToServerModel.UI_CONTEXT_ID.toStringValue() + "=" + PonySDK.get().getContextId();
            if (PonySDK.get().isTabindexOnlyFormField()) url += "&" + ClientToServerModel.OPTION_TABINDEX_ACTIVATED.toStringValue()
                    + "=" + PonySDK.get().isTabindexOnlyFormField();
        }

        PTWindowManager.get().register(this);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.OPEN == model) {
            window = Browser.getWindow().open(url, name, features);
            // Window can be null if browser doesn't allow popup
            if (window != null) {
                window.setOnunload(event -> onClose());
            } else {
                log.log(Level.WARNING, "Can't open PTWindow #" + objectID + ". Check the browser's settings");

                final PTInstruction instruction = new PTInstruction(objectID);
                instruction.put(ClientToServerModel.HANDLER_DESTROY);
                uiBuilder.sendDataToServer(instruction);

                uiBuilder.sendWarningMessageToServer("Can't open PTWindow #" + objectID + ". Check the browser's settings", objectID);
            }
            return true;
        } else if (ServerToClientModel.CLOSE == model) {
            close(false);
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    public void close(final boolean forced) {
        if (ready && window != null && !window.isClosed()) window.close();
    }

    @Override
    public void postMessage(final Uint8Array buffer) {
        if (ready && window.isClosed()) onClose();

        if (ready) window.postMessage(buffer, "*");
    }

    @Override
    public void setReady() {
        ready = true;
        setTitle(name, window); // WORKAROUND : Set title for Google Chrome

        final PTInstruction instruction = new PTInstruction(objectID);
        instruction.put(ClientToServerModel.HANDLER_OPEN, url);
        uiBuilder.sendDataToServer(instruction);
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    protected void onClose() {
        if (ready && window != null) {
            ready = false;
            window = null;
            PTWindowManager.get().unregister(PTWindow.this);

            final PTInstruction instruction = new PTInstruction(objectID);
            instruction.put(ClientToServerModel.HANDLER_CLOSE);
            uiBuilder.sendDataToServer(instruction);

            if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Close PTWindow #" + objectID);
        }
    }

    public boolean isClosed() {
        return window == null || window.isClosed();
    }

}
