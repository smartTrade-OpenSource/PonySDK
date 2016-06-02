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

package com.ponysdk.ui.terminal;

import java.util.ArrayList;
import java.util.List;
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
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.ponysdk.ui.model.ClientToServerModel;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.ReaderBuffer;
import com.ponysdk.ui.terminal.request.ParentWindowRequest;
import com.ponysdk.ui.terminal.request.RequestCallback;
import com.ponysdk.ui.terminal.socket.WebSocketCallback;
import com.ponysdk.ui.terminal.socket.WebSocketClient;
import com.ponysdk.ui.terminal.ui.PTWindowManager;

import elemental.client.Browser;

@ExportPackage(value = "")
@Export(value = "ponysdk", all = false)
public class PonySDK implements Exportable, UncaughtExceptionHandler {

    private static final Logger log = Logger.getLogger(PonySDK.class.getName());

    private static PonySDK INSTANCE;
    private static final UIBuilder uiBuilder = new UIBuilder();

    private int applicationViewID;

    private WebSocketClient socketClient;

    private final List<StartupListener> listener = new ArrayList<>();

    private PonySDK() {

        final elemental.html.Window window = Browser.getWindow();
        final elemental.html.Window opener = window.getOpener();

        if (opener == null) {
            Window.addWindowClosingHandler(new ClosingHandler() {

                @Override
                public void onWindowClosing(final ClosingEvent event) {
                    PTWindowManager.closeAll();
                }
            });
            Window.addCloseHandler(new CloseHandler<Window>() {

                @Override
                public void onClose(final CloseEvent<Window> event) {
                    PTWindowManager.closeAll();
                }
            });

        }

    }

    @ExportConstructor
    public static PonySDK constructor() {
        if (INSTANCE == null) {
            INSTANCE = new PonySDK();
            GWT.setUncaughtExceptionHandler(INSTANCE);
            if (log.isLoggable(Level.INFO))
                log.info("Creating PonySDK instance");
        }
        return INSTANCE;
    }

    @Export
    public void start() {
        try {
            if (log.isLoggable(Level.INFO))
                log.info("Starting PonySDK instance");
            final elemental.html.Window window = Browser.getWindow();
            final elemental.html.Window opener = window.getOpener();

            if (opener == null) {
                Integer viewID = null;
                final Storage storage = Storage.getSessionStorageIfSupported();
                if (storage != null) {
                    final String v = storage.getItem(ClientToServerModel.APPLICATION_VIEW_ID.toStringValue());
                    if (v != null && !v.isEmpty())
                        viewID = Integer.parseInt(v);
                }

                if (log.isLoggable(Level.INFO))
                    log.info("View ID : " + viewID);

                applicationViewID = viewID != null ? viewID : 0;

                final StringBuilder builder = new StringBuilder();
                builder.append(GWT.getHostPageBaseURL().replaceFirst("http", "ws"));
                builder.append("ws?");
                builder.append(ClientToServerModel.APPLICATION_VIEW_ID.toStringValue() + "=" + UIBuilder.sessionID)
                        .append("&");
                builder.append(ClientToServerModel.APPLICATION_START.toStringValue()).append("&");
                builder.append(ClientToServerModel.APPLICATION_SEQ_NUM.toStringValue() + "=" + 0).append("&");
                builder.append(ClientToServerModel.TYPE_HISTORY.toStringValue() + "=" + History.getToken());

                socketClient = new WebSocketClient(builder.toString(), new WebSocketCallback() {

                    @Override
                    public void connected() {
                        if (log.isLoggable(Level.INFO))
                            log.info("WebSoket connected");
                        uiBuilder.init(applicationViewID, socketClient.getRequestBuilder());
                    }

                    @Override
                    public void disconnected() {
                        if (log.isLoggable(Level.INFO))
                            log.info("WebSoket disconnected");
                        uiBuilder.onCommunicationError(new Exception("Websocket connection lost."));
                    }

                    /**
                     * Message from server to Main terminal
                     */
                    @Override
                    public void message(final ReaderBuffer buffer) {
                        try {
                            uiBuilder.updateMainTerminal(buffer);
                        } catch (final Exception e) {
                            log.log(Level.SEVERE, "Cannot parse " + buffer, e);
                        }
                    }

                });
            } else {
                final String windowId = Window.Location.getParameter("wid");
                uiBuilder.init(applicationViewID, new ParentWindowRequest(windowId, new RequestCallback() {

                    /**
                     * Message from Main terminal to the matching terminal
                     */
                    @Override
                    public void onDataReceived(final ReaderBuffer buffer) {
                        uiBuilder.update(buffer);
                    }

                    @Override
                    public void onDataReceived(final JSONObject object) {
                    }

                    @Override
                    public void onError(final Throwable exception) {
                    }
                }));
            }

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

    @Export
    public void executeInstruction(final JavaScriptObject jso) {
        uiBuilder.executeInstruction(jso);
    }

    @Override
    public void onUncaughtException(final Throwable e) {
        log.log(Level.SEVERE, "PonySDK has encountered an internal error : ", e);

        if (uiBuilder != null) {
            final PTInstruction instruction = new PTInstruction();
            instruction.put(ClientToServerModel.ERROR_MSG, e.getMessage());
            uiBuilder.sendDataToServer(instruction);
        }
    }

    public static native void reload() /*-{$wnd.location.reload();}-*/;

}
