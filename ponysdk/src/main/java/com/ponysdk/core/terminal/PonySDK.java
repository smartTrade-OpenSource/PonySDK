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

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportConstructor;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.Exportable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
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

@ExportPackage(value = "")
@Export(value = "ponysdk")
public class PonySDK implements Exportable, UncaughtExceptionHandler {

    private static final Logger log = Logger.getLogger(PonySDK.class.getName());

    private static PonySDK INSTANCE;

    private final UIBuilder uiBuilder = new UIBuilder();

    private WebSocketClient socketClient;

    private boolean started = false;

    public static Integer uiContextId;

    private PonySDK() {
    }

    @ExportConstructor
    public static PonySDK constructor() {
        if (INSTANCE == null) {
            INSTANCE = new PonySDK();
            GWT.setUncaughtExceptionHandler(INSTANCE);
            if (log.isLoggable(Level.INFO)) log.info("Creating PonySDK instance");
        }
        return INSTANCE;
    }

    @Export
    public void start() {
        if (started) return;
        try {
            final String windowId = Window.Location.getParameter(ClientToServerModel.WINDOW_ID.toStringValue());

            if (log.isLoggable(Level.INFO)) log.info("Starting PonySDK instance");
            if (windowId == null) {
                Window.addCloseHandler(new CloseHandler<Window>() {

                    @Override
                    public void onClose(final CloseEvent<Window> event) {
                        close();
                    }
                });

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
                        uiBuilder.updatWindowTerminal(buffer);
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
    @Export
    public void sendDataToServer(final String jsObject) {
        uiBuilder.sendDataToServer(JSONParser.parseStrict(jsObject));
    }

    /**
     * From Main terminal to the server
     */
    @Export
    public void sendDataToServer(final int objectID, final JavaScriptObject jsObject) {
        final PTInstruction instruction = new PTInstruction(objectID);
        instruction.put(ClientToServerModel.NATIVE, jsObject);
        uiBuilder.sendDataToServer(instruction);
    }

    /**
     * From Main terminal to the server
     */
    @Export
    public void sendDataToServer(final String objectID, final JavaScriptObject jsObject) {
        sendDataToServer(Integer.parseInt(objectID), jsObject);
    }

    @Export
    public void setReadyWindow(final int windowID) {
        uiBuilder.setReadyWindow(windowID);
    }

    @Export
    public void registerCommunicationError(final CommunicationErrorHandler communicationErrorClosure) {
        uiBuilder.registerCommunicationError(communicationErrorClosure);
    }

    @Export
    public void registerAddOnFactory(final String signature, final JavascriptAddOnFactory javascriptAddOnFactory) {
        uiBuilder.registerJavascriptAddOnFactory(signature, javascriptAddOnFactory);
    }

    @Override
    public void onUncaughtException(final Throwable e) {
        log.log(Level.SEVERE, "PonySDK has encountered an internal error : ", e);

        final PTInstruction instruction = new PTInstruction();
        instruction.put(ClientToServerModel.ERROR_MSG, e.getMessage());
        uiBuilder.sendDataToServer(instruction);
    }

    @Export
    public void close() {
        socketClient.close();
        PTWindowManager.closeAll();
    }

}
