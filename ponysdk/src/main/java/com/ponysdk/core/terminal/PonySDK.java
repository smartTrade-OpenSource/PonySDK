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
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.MappingPath;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.request.FrameRequestBuilder;
import com.ponysdk.core.terminal.request.WindowRequestBuilder;
import com.ponysdk.core.terminal.socket.WebSocketClient;
import com.ponysdk.core.terminal.ui.PTObject;
import com.ponysdk.core.terminal.ui.PTWindowManager;

import elemental.client.Browser;
import elemental.xml.XMLHttpRequest;
import jsinterop.annotations.JsType;

@JsType
public class PonySDK implements UncaughtExceptionHandler {

    private static final Logger log = Logger.getLogger(PonySDK.class.getName());

    private static PonySDK INSTANCE;

    private final UIBuilder uiBuilder = new UIBuilder();

    private int contextId;
    private WebSocketClient socketClient;
    private boolean started;

    private boolean tabindexOnlyFormField;

    public PonySDK() {
        if (INSTANCE != null) throw new RuntimeException("Cannot instanciate PonySDK twice");
        INSTANCE = this;
    }

    public static final PonySDK get() {
        return INSTANCE;
    }

    public void start() {
        if (started) return;

        GWT.setUncaughtExceptionHandler(this);

        try {
            final String child = Window.Location.getParameter(ClientToServerModel.UI_CONTEXT_ID.toStringValue());

            if (child == null) startMainContext();
            else startChildContext();

            started = true;
        } catch (final Throwable e) {
            log.log(Level.SEVERE, "Loading application has failed #" + e.getMessage(), e);
        }
    }

    private void startMainContext() {
        Window.addCloseHandler(event -> close());
        final String builder = GWT.getHostPageBaseURL().replaceFirst("http", "ws") + MappingPath.WEBSOCKET + "?"
                + ClientToServerModel.TYPE_HISTORY.toStringValue() + "=" + History.getToken();
        final ReconnectionChecker reconnectionChecker = new ReconnectionChecker();
        socketClient = new WebSocketClient(builder, uiBuilder, reconnectionChecker);

        reconnectionChecker.checkConnection();
    }

    private void startChildContext() {
        final String windowId = Window.Location.getParameter(ClientToServerModel.WINDOW_ID.toStringValue());
        final String frameId = Window.Location.getParameter(ClientToServerModel.FRAME_ID.toStringValue());

        contextId = Integer.parseInt(Window.Location.getParameter(ClientToServerModel.UI_CONTEXT_ID.toStringValue()));
        final String tabindexOnlyFormFieldRaw = Window.Location
            .getParameter(ClientToServerModel.OPTION_TABINDEX_ACTIVATED.toStringValue());
        if (tabindexOnlyFormFieldRaw != null) tabindexOnlyFormField = Boolean.parseBoolean(tabindexOnlyFormFieldRaw);

        uiBuilder.init(windowId != null ? new WindowRequestBuilder(windowId, buffer -> uiBuilder.updateWindowTerminal(buffer))
                : new FrameRequestBuilder(frameId, buffer -> uiBuilder.updateFrameTerminal(buffer)));
    }

    /**
     * From other terminal to the server
     */
    public void sendDataToServerFromWindow(final String jsObject) {
        uiBuilder.sendDataToServer(JSONParser.parseStrict(jsObject));
    }

    /**
     * From Main terminal to the server
     * Ajax implementation
     */
    public void sendDataToServer(final Object objectID, final JavaScriptObject jsObject, final AjaxCallback callback) {
        if (callback == null) {
            final PTInstruction instruction = new PTInstruction(Integer.valueOf(objectID.toString()));
            instruction.put(ClientToServerModel.NATIVE, jsObject);
            uiBuilder.sendDataToServer(instruction);
        } else {
            final XMLHttpRequest xhr = Browser.getWindow().newXMLHttpRequest();

            final PTObject ptObject = uiBuilder.getPTObject(Integer.parseInt(objectID.toString()));

            xhr.setOnload(evt -> callback.setAjaxResponse(xhr.getResponseText()));

            xhr.open("GET", MappingPath.AJAX.toString());
            xhr.setRequestHeader(ClientToServerModel.UI_CONTEXT_ID.name(), String.valueOf(contextId));
            xhr.setRequestHeader(ClientToServerModel.OBJECT_ID.name(), String.valueOf(ptObject.getObjectID()));

            final JSONObject jsonArray = new JSONObject(jsObject);
            for (final String key : jsonArray.keySet()) {
                final String value;
                final JSONValue jsonValue = jsonArray.get(key);
                if (jsonValue != null) {
                    final JSONString stringValue = jsonValue.isString();
                    value = stringValue != null ? stringValue.stringValue() : jsonValue.toString();
                } else {
                    value = "";
                }
                xhr.setRequestHeader(key, value);
            }

            xhr.send();
        }
    }

    public void request(final Object objectID, final JavaScriptObject jsObject, final AjaxCallback callback) {
        sendDataToServer(objectID, jsObject, callback);
    }

    public void setReadyFrame(final int frameID) {
        uiBuilder.setReadyFrame(frameID);
    }

    public void setReadyWindow(final int windowID) {
        uiBuilder.setReadyWindow(windowID);
    }

    public void registerAddOnFactory(final String signature, final JavascriptAddOnFactory javascriptAddOnFactory) {
        uiBuilder.registerJavascriptAddOnFactory(signature, javascriptAddOnFactory);
    }

    public String getHostPageBaseURL() {
        return GWT.getHostPageBaseURL();
    }

    @Override
    public void onUncaughtException(final Throwable e) {
        uiBuilder.sendExceptionMessageToServer(e);
    }

    public void close() {
        socketClient.close();
        PTWindowManager.closeAll();
    }

    public int getContextId() {
        return contextId;
    }

    public void setContextId(final int contextId) {
        this.contextId = contextId;
    }

    public boolean isTabindexOnlyFormField() {
        return tabindexOnlyFormField;
    }

    public void setTabindexOnlyFormField(final boolean tabindexOnlyFormField) {
        this.tabindexOnlyFormField = tabindexOnlyFormField;
    }

}
