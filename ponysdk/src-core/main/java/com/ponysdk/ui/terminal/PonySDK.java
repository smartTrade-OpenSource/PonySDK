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

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportConstructor;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.Exportable;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.HISTORY;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.request.HttpRequestBuilder;
import com.ponysdk.ui.terminal.request.ParentWindowRequest;
import com.ponysdk.ui.terminal.request.RequestBuilder;
import com.ponysdk.ui.terminal.request.RequestCallback;

@ExportPackage(value = "")
@Export(value = "ponysdk", all = false)
public class PonySDK implements Exportable {

    private final static Logger log = Logger.getLogger(PonySDK.class.getName());

    private static PonySDK INSTANCE;
    protected UIBuilder uiBuilder = new UIBuilder();

    protected RequestBuilder requestBuilder;
    protected long applicationViewID;

    private PonySDK() {}

    @ExportConstructor
    public static PonySDK constructor() {
        if (INSTANCE == null) {
            INSTANCE = new PonySDK();
            log("Creating ponysdk instance");
        }
        return INSTANCE;
    }

    @Export
    public void start() {
        log("starting...");

        try {

            Long viewID = null;
            final Storage storage = Storage.getSessionStorageIfSupported();
            if (storage != null) {
                final String v = storage.getItem(APPLICATION.VIEW_ID);
                if (v != null && !v.isEmpty()) viewID = Long.parseLong(v);
            }
            final PTInstruction requestData = new PTInstruction();

            final JSONArray cookies = new JSONArray();

            // load all cookies at startup
            final Collection<String> cookieNames = Cookies.getCookieNames();
            if (cookieNames != null) {
                int i = 0;
                for (final String cookie : cookieNames) {
                    final JSONObject jsoObject = new JSONObject();
                    jsoObject.put(PROPERTY.KEY, new JSONString(cookie));
                    jsoObject.put(PROPERTY.VALUE, new JSONString(Cookies.getCookie(cookie)));
                    cookies.set(i++, jsoObject);
                }
            }

            requestData.put(APPLICATION.KEY, APPLICATION.KEY_.START);
            requestData.put(APPLICATION.SEQ_NUM, 0);
            requestData.put(HISTORY.TOKEN, History.getToken());
            requestData.put(PROPERTY.COOKIES, cookies);

            if (viewID != null) requestData.put(APPLICATION.VIEW_ID, viewID);

            final RequestCallback requestCallback = new RequestCallback() {

                @Override
                public void onDataReceived(final JSONObject data) {
                    try {
                        if (data.containsKey(APPLICATION.VIEW_ID)) {
                            applicationViewID = (long) data.get(APPLICATION.VIEW_ID).isNumber().doubleValue();

                            if (storage != null) storage.setItem(APPLICATION.VIEW_ID, Long.toString(applicationViewID));

                            uiBuilder.init(applicationViewID, requestBuilder);
                        }

                        uiBuilder.update(data);

                    } catch (final RuntimeException exception) {
                        log.log(Level.SEVERE, "Failed to process data with error #" + exception.getMessage() + ", data: " + data, exception);
                    }
                }

                @Override
                public void onError(final Throwable exception) {
                    uiBuilder.onCommunicationError(exception);
                }

            };

            requestBuilder = newRequestBuilder(requestCallback);
            requestBuilder.send(requestData.toString());

        } catch (final Exception e) {
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
        instruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
        instruction.put(PROPERTY.NATIVE, jsObject);
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

    public static native void log(String msg) /*-{ if($wnd.console) $wnd.console.log(msg);}-*/;
}
