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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportConstructor;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.Exportable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.request.HttpRequestBuilder;
import com.ponysdk.ui.terminal.request.ParentWindowRequest;
import com.ponysdk.ui.terminal.request.RequestBuilder;
import com.ponysdk.ui.terminal.request.RequestCallback;
import com.ponysdk.ui.terminal.socket.WebSocketCallback;
import com.ponysdk.ui.terminal.socket.WebSocketClient2;

@ExportPackage(value = "")
@Export(value = "ponysdk", all = false)
public class PonySDK implements Exportable, UncaughtExceptionHandler, WebSocketCallback {

    private final static Logger log = Logger.getLogger(PonySDK.class.getName());

    private static PonySDK INSTANCE;
    protected UIBuilder uiBuilder = new UIBuilder();

    protected RequestBuilder requestBuilder;
    protected int applicationViewID;

    private WebSocketClient2 socketClient;

    private PonySDK() {}

    @ExportConstructor
    public static PonySDK constructor() {
        if (INSTANCE == null) {
            INSTANCE = new PonySDK();
            log.info("Creating PonySDK instance");
        }
        return INSTANCE;
    }

    @Export
    public void start() {
        log.info("Starting PonySDK instance");

        try {
            Integer viewID = null;
            final Storage storage = Storage.getSessionStorageIfSupported();
            if (storage != null) {
                final String v = storage.getItem(Model.APPLICATION_VIEW_ID.getKey());
                if (v != null && !v.isEmpty()) viewID = Integer.parseInt(v);
            }
            log.info("View ID : " + viewID);

            final PTInstruction requestData = new PTInstruction();

            final JSONArray cookies = new JSONArray();

            // // load all cookies at startup
            // final Collection<String> cookieNames = Cookies.getCookieNames();
            // if (cookieNames != null) {
            // int i = 0;
            // for (final String cookie : cookieNames) {
            // final JSONObject jsoObject = new JSONObject();
            // jsoObject.put(Model.KEY.getKey(), new JSONString(cookie));
            // jsoObject.put(Model.VALUE.getKey(), new JSONString(Cookies.getCookie(cookie)));
            // cookies.set(i++, jsoObject);
            // }
            // }

            requestData.put(Model.APPLICATION_START);
            requestData.put(Model.APPLICATION_SEQ_NUM, 0);
            requestData.put(Model.HISTORY_TOKEN, History.getToken());
            // requestData.put(Model.COOKIES, cookies);

            if (viewID != null) {
                applicationViewID = viewID;
                requestData.put(Model.APPLICATION_VIEW_ID, viewID);
            } else {
                applicationViewID = 0;
            }

            final StringBuilder builder = new StringBuilder();
            builder.append(GWT.getHostPageBaseURL().replaceFirst("http", "ws"));
            builder.append("ws?");
            builder.append(Model.APPLICATION_VIEW_ID.getKey() + "=" + UIBuilder.sessionID);
            builder.append("&");
            builder.append(Model.APPLICATION_START.getKey());
            builder.append("&");
            builder.append(Model.APPLICATION_SEQ_NUM.getKey() + "=" + 0);
            builder.append("&");
            builder.append(Model.HISTORY_TOKEN.getKey() + "=" + History.getToken());
            // builder.append( requestData.put(Model.COOKIES, cookies);

            socketClient = new WebSocketClient2(builder.toString(), this);

            // socketClient.connect(builder.toString());

            // final RequestCallback requestCallback = new RequestCallback() {
            //
            // @Override
            // public void onDataReceived(final JSONObject data) {
            // try {
            // if (data.containsKey(Model.APPLICATION_VIEW_ID.getKey())) {
            // applicationViewID = (long)
            // data.get(Model.APPLICATION_VIEW_ID.getKey()).isNumber().doubleValue();
            //
            // if (storage != null) storage.setItem(Model.APPLICATION_VIEW_ID.getKey(),
            // Long.toString(applicationViewID));
            //
            // uiBuilder.init(applicationViewID, requestBuilder);
            // }
            //
            // uiBuilder.update(data);
            //
            // } catch (final RuntimeException exception) {
            // log.log(Level.SEVERE, "Failed to process data with error #" + exception.getMessage() + ", data:
            // " + data, exception);
            // }
            // }
            //
            // @Override
            // public void onError(final Throwable exception) {
            // uiBuilder.onCommunicationError(exception);
            // }
            //
            // };
            //
            // requestBuilder = newRequestBuilder(requestCallback);
            // requestBuilder.send(requestData.toString());

        } catch (final Throwable e) {
            log.log(Level.SEVERE, "Loading application has failed #" + e.getMessage(), e);
        }
    }

    // TODO pure js factory to get the connector ?
    protected RequestBuilder newRequestBuilder(final RequestCallback requestCallback) {
        final String windowID = Window.Location.getParameter("wid");
        if (windowID != null) { return new ParentWindowRequest(windowID, requestCallback); }
        return new HttpRequestBuilder(requestCallback);
    }

    @Export
    public void sendDataToServer(final String objectID, final JavaScriptObject jsObject) {
        final PTInstruction instruction = new PTInstruction();
        instruction.setObjectID(Long.parseLong(objectID));
        instruction.put(Model.TYPE_EVENT);
        instruction.put(Model.NATIVE, jsObject);
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

    public static native void reload() /*-{$wnd.location.reload();}-*/;

    @Override
    public void onUncaughtException(final Throwable e) {
        log.log(Level.SEVERE, "PonySDK has encountered an internal error : ", e);

        if (uiBuilder != null) {
            final PTInstruction instruction = new PTInstruction();
            instruction.put(Model.TYPE_EVENT);
            instruction.put(Model.ERROR_MSG, e.getMessage());
            uiBuilder.sendDataToServer(instruction);
            log.log(Level.SEVERE, "PonySDK has encountered an internal error : ", e);
        }
    }

    @Override
    public void connected() {
        log.info("WebSoket connected");
        uiBuilder.init(applicationViewID, socketClient.getRequestBuilder());
    }

    @Override
    public void disconnected() {
        log.info("WebSoket disconnected");

        uiBuilder.onCommunicationError(new Exception("Websocket connection lost."));
    }

    @Override
    public void message(final String message) {
        try {
            // GWT.log(message);
            final JSONObject data = JSONParser.parseStrict(message).isObject();
            uiBuilder.update(data);
        } catch (final Exception e) {
            GWT.log("Cannot parse " + message + " dlskqjlkdjqs", e);
        }
    }

}
