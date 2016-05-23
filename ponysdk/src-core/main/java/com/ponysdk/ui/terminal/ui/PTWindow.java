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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONParser;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.MessageEvent;
import elemental.html.Window;

public class PTWindow extends AbstractPTObject implements EventListener {

    private static final Logger log = Logger.getLogger(PTWindow.class.getName());

    private static final String WINDOW_EVENT_TYPE_MESSAGE = "message";
    private static final String WINDOW_EVENT_TYPE_BEFORE_UNLOAD = "beforeunload";

    private static final String EMPTY = "";

    private Window window;
    private String url;
    private String name;
    private String features;

    private UIService uiService;

    private boolean ponySDKStarted = false;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        super.create(buffer, objectId, uiService);

        if (log.isLoggable(Level.INFO))
            log.log(Level.INFO, "PTWindowID created : " + objectID);

        this.uiService = uiService;

        // ServerToClientModel.URL
        url = buffer.getBinaryModel().getStringValue();
        if (url == null)
            url = GWT.getHostPageBaseURL() + "?wid=" + objectId;

        // ServerToClientModel.NAME
        name = buffer.getBinaryModel().getStringValue();
        if (name == null)
            name = EMPTY;

        // ServerToClientModel.FEATURES
        features = buffer.getBinaryModel().getStringValue();
        if (features == null)
            features = EMPTY;

        PTWindowManager.get().register(this);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.OPEN.equals(binaryModel.getModel())) {
            window = Browser.getWindow().open(url, name, features);
            return true;
        }
        if (ServerToClientModel.TEXT.equals(binaryModel.getModel())) {
            window.postMessage(binaryModel.getStringValue(), "*");
            return true;
        }
        if (ServerToClientModel.CLOSE.equals(binaryModel.getModel())) {
            window.close();
            return true;
        }
        return false;
    }

    @Override
    public void handleEvent(final Event event) {
        if (event.getCurrentTarget() == window) {
            final String type = event.getType();
            if (WINDOW_EVENT_TYPE_MESSAGE.equals(type)) {
                final MessageEvent messageEvent = (MessageEvent) event;
                uiService.update(JSONParser.parseStrict((String) messageEvent.getData()).isObject());
            } else if (WINDOW_EVENT_TYPE_BEFORE_UNLOAD.equals(type)) {
                PTWindowManager.get().unregister(this);
                final PTInstruction instruction = new PTInstruction(objectID);
                instruction.put(ClientToServerModel.HANDLER_CLOSE_HANDLER);
                uiService.sendDataToServer(instruction);
            }
        }
    }

    public void postMessage(final ReaderBuffer buffer) {
        if (ponySDKStarted) postMessage(buffer, window);
        else log.log(Level.WARNING, "No window set");
    }

    public native void postMessage(final ReaderBuffer buffer, Window window) /*-{window.onDataReceived(buffer);}-*/;

    public void setReady() {
        ponySDKStarted = true;
    }

}
