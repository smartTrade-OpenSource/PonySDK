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
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.Uint8Array;
import elemental.html.Window;

public class PTWindow extends AbstractPTObject {

    private static final Logger log = Logger.getLogger(PTWindow.class.getName());

    private static final String EMPTY = "";

    private Window window;
    private String url;
    private String name;
    private String features;

    private UIBuilder uiService;

    private boolean ready = false;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder builder) {
        super.create(buffer, objectId, builder);

        if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Create PTWindow #" + objectID);

        uiService = builder;

        url = buffer.readBinaryModel().getStringValue();
        if (url == null) url = GWT.getHostPageBaseURL() + "?" + ClientToServerModel.WINDOW_ID.toStringValue() + "=" + objectId + "&"
                + ClientToServerModel.UI_CONTEXT_ID.toStringValue() + "=" + PonySDK.uiContextId;

        name = buffer.readBinaryModel().getStringValue();
        if (name == null) name = EMPTY;
        features = buffer.readBinaryModel().getStringValue();
        if (features == null) features = EMPTY;

        PTWindowManager.get().register(this);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.OPEN.equals(binaryModel.getModel())) {
            window = Browser.getWindow().open(url, name, features);
            window.setOnunload(new EventListener() {

                @Override
                public void handleEvent(final Event event) {
                    onClose();
                }
            });
            return true;
        } else if (ServerToClientModel.PRINT.equals(binaryModel.getModel())) {
            window.print();
            return true;
        } else if (ServerToClientModel.WINDOW_TITLE.equals(binaryModel.getModel())) {
            setTitle(binaryModel.getStringValue(), window);
            return true;
        } else if (ServerToClientModel.CLOSE.equals(binaryModel.getModel())) {
            close(false);
            return true;
        }
        return false;
    }

    public void close(final boolean forced) {
        window.close();
    }

    public void postMessage(final Uint8Array buffer) {
        if (ready && window.isClosed()) onClose();

        if (ready) window.postMessage(buffer, "*");
    }

    public void setReady() {
        ready = true;
        setTitle(name, window); // WORKAROUND : Set title for Google Chrome

        final PTInstruction instruction = new PTInstruction(objectID);
        instruction.put(ClientToServerModel.HANDLER_OPEN, url);
        uiService.sendDataToServer(instruction);
    }

    public boolean isReady() {
        return ready;
    }

    public final native void setTitle(String title, Window window) /*-{
                                                                   window.document.title = title;
                                                                   }-*/;

    protected void onClose() {
        if (ready && window != null) {
            ready = false;
            window = null;
            PTWindowManager.get().unregister(PTWindow.this);

            final PTInstruction instruction = new PTInstruction(objectID);
            instruction.put(ClientToServerModel.HANDLER_CLOSE);
            uiService.sendDataToServer(instruction);

            if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Close PTWindow #" + objectID);
        }
    }

    public boolean isClosed() {
        return window.isClosed();
    }

}
