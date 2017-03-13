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

package com.ponysdk.core.terminal;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.request.ParentWindowRequest;
import com.ponysdk.core.terminal.request.RequestCallback;
import com.ponysdk.core.terminal.socket.WebSocketClient;
import com.ponysdk.core.terminal.socket.WebSocketClient.WebSocketDataType;
import com.ponysdk.core.terminal.ui.PTWindowManager;

import jsinterop.annotations.JsType;

@JsType
public class PonySDK implements UncaughtExceptionHandler {

    private final Logger log = Logger.getLogger(PonySDK.class.getName());

    private final UIBuilder uiBuilder = new UIBuilder();

    private WebSocketClient socketClient;

    private boolean started;

    @SuppressWarnings("unusable-by-js")
    public static Integer uiContextId;

    public PonySDK() {
    }

    public void start() {
        if (started) return;
        GWT.setUncaughtExceptionHandler(this);
        if (log.isLoggable(Level.INFO)) log.info("Creating PonySDK instance");
        try {
            final String windowId = Window.Location.getParameter(ClientToServerModel.WINDOW_ID.toStringValue());

            if (log.isLoggable(Level.INFO)) log.info("Starting PonySDK instance");
            if (windowId == null) {
                Window.addCloseHandler(event -> close());

                final String builder = GWT.getHostPageBaseURL().replaceFirst("http", "ws") + "ws?"
                        + ClientToServerModel.TYPE_HISTORY.toStringValue() + "=" + History.getToken();

                socketClient = new WebSocketClient(builder, uiBuilder, WebSocketDataType.ARRAYBUFFER);
            } else {
                uiContextId = Integer.parseInt(Window.Location.getParameter(ClientToServerModel.UI_CONTEXT_ID.toStringValue()));
                uiBuilder.init(new ParentWindowRequest(windowId, new RequestCallback() {

                    /**
                     * Message from Main terminal to the matching terminal
                     */
                    @Override
                    public void onDataReceived(final ReaderBuffer buffer) {
                        uiBuilder.updateWindowTerminal(buffer);
                    }

                }));
            }
            started = true;
        } catch (final Throwable e) {
            log.log(Level.SEVERE, "Loading application has failed #" + e.getMessage(), e);
        }
    }

    /**
     * From other terminal to the server
     */
    public void sendDataToServerFromWindow(final String jsObject) {
        uiBuilder.sendDataToServer(JSONParser.parseStrict(jsObject));
    }

    /**
     * From Main terminal to the server
     */
    public void sendDataToServer(final Object objectID, final JavaScriptObject jsObject) {
        final PTInstruction instruction = new PTInstruction(Integer.valueOf(objectID.toString()));
        instruction.put(ClientToServerModel.NATIVE, jsObject);
        uiBuilder.sendDataToServer(instruction);
    }

    public void setReadyWindow(final int windowID) {
        uiBuilder.setReadyWindow(windowID);
    }

    public void registerCommunicationError(final CommunicationErrorHandler communicationErrorClosure) {
        uiBuilder.registerCommunicationError(communicationErrorClosure);
    }

    public void registerAddOnFactory(final String signature, final JavascriptAddOnFactory javascriptAddOnFactory) {
        uiBuilder.registerJavascriptAddOnFactory(signature, javascriptAddOnFactory);
    }

    @Override
    public void onUncaughtException(final Throwable e) {
        log.log(Level.SEVERE, "PonySDK has encountered an internal error : ", e);
        uiBuilder.sendErrorMessageToServer(e.getMessage());
    }

    public void close() {
        socketClient.close();
        PTWindowManager.closeAll();
    }

}
