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
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.History;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;
import com.ponysdk.ui.terminal.model.ServerToClientModel;
import com.ponysdk.ui.terminal.request.RequestBuilder;
import com.ponysdk.ui.terminal.socket.WebSocketCallback;
import com.ponysdk.ui.terminal.socket.WebSocketClient2;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.ArrayBuffer;
import elemental.html.StorageEvent;

@ExportPackage(value = "")
@Export(value = "ponysdk", all = false)
public class PonySDK implements Exportable, UncaughtExceptionHandler, WebSocketCallback, EventListener {

    private static final Logger log = Logger.getLogger(PonySDK.class.getName());

    private static PonySDK INSTANCE;
    protected static UIBuilder uiBuilder = new UIBuilder();

    protected RequestBuilder requestBuilder;
    protected int applicationViewID;

    private WebSocketClient2 socketClient;

    private final List<StartupListener> listener = new ArrayList<>();

    private PonySDK() {
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

    @Override
    public void handleEvent(final Event event) {
        final StorageEvent storageEvent = (StorageEvent) event;
        // final Integer windowID = Integer.valueOf(storageEvent.getKey());
        uiBuilder.update(JSONParser.parseStrict(storageEvent.getNewValue()).isObject());
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

                if (viewID != null)
                    applicationViewID = viewID;
                else
                    applicationViewID = 0;

                final StringBuilder builder = new StringBuilder();
                builder.append(GWT.getHostPageBaseURL().replaceFirst("http", "ws"));
                builder.append("ws?");
                builder.append(ClientToServerModel.APPLICATION_VIEW_ID.toStringValue() + "=" + UIBuilder.sessionID);
                builder.append("&");
                builder.append(ClientToServerModel.APPLICATION_START.toStringValue());
                builder.append("&");
                builder.append(ClientToServerModel.APPLICATION_SEQ_NUM.toStringValue() + "=" + 0);
                builder.append("&");
                builder.append(ClientToServerModel.TYPE_HISTORY.toStringValue() + "=" + History.getToken());

                socketClient = new WebSocketClient2(builder.toString(), this);
            } else {
                exportOnDataReceived();

                // window.addEventListener("storage", this, false);

                final Event createEvent = opener.getDocument().createEvent("ponysdk.onstarted");
                window.dispatchEvent(createEvent);

                // exportWindowReceiver(window);
                // window.getDocument().addEventListener("message", new
                // EventListener() {
                //
                // @Override
                // public void handleEvent(final Event event) {
                // GWT.log("data received in window : " + event);
                // final MessageEvent messageEvent = (MessageEvent) event;
                // uiBuilder.update(JSONParser.parseStrict((String)
                // messageEvent.getData()).isObject());
                // }
                // }, false);
            }

        } catch (final Throwable e) {
            log.log(Level.SEVERE, "Loading application has failed #" + e.getMessage(), e);
        }
    }

    public native void exportOnDataReceived() /*-{
                                              var that = this;
                                              $wnd.onDataReceived = function(text) {
                                              $entry(that.@com.ponysdk.ui.terminal.PonySDK::onDataReceived(Ljava/lang/String;)(text));
                                              }
                                              }-*/;

    public void onDataReceived(final String text) {
        uiBuilder.update(JSONParser.parseStrict(text).isObject());
    }

    @Export
    public void sendDataToServer(final int objectID, final JavaScriptObject jsObject) {
        final PTInstruction instruction = new PTInstruction();
        instruction.setObjectID(objectID);
        instruction.put(ClientToServerModel.NATIVE, jsObject);
        uiBuilder.sendDataToServer(instruction);
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

    @Override
    public void message(final ArrayBuffer message) {
        try {
            final ReaderBuffer buffer = new ReaderBuffer(message);
            // Get the first element on the message, always a key of element of
            // the Model enum
            final BinaryModel type = buffer.getBinaryModel();

            if (type.getModel() == ServerToClientModel.HEARTBEAT) {
                if (log.isLoggable(Level.FINE))
                    log.log(Level.FINE, "Heart beat");
                socketClient.getRequestBuilder().sendHeartbeat();
            } else if (type.getModel() == ServerToClientModel.APPLICATION_SEQ_NUM) {
                uiBuilder.update(buffer);
            } else {
                log.severe("" + type.getModel());
            }
        } catch (final Exception e) {
            log.log(Level.SEVERE, "Cannot parse " + message, e);
        }
    }

    public static native void reload() /*-{$wnd.location.reload();}-*/;

}
