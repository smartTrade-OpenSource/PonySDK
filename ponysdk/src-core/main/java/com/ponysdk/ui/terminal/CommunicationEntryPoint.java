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

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
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
import com.ponysdk.ui.terminal.event.CommunicationErrorEvent;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class CommunicationEntryPoint implements EntryPoint, Callback<Void, Exception> {

    private final static Logger log = Logger.getLogger(CommunicationEntryPoint.class.getName());

    private static EventBus rootEventBus = new SimpleEventBus();

    protected RequestBuilder requestBuilder;
    protected UIBuilder uiBuilder;

    protected long applicationViewID;
    // protected int scriptToLoad = 0;

    protected JSONObject data;

    public static EventBus getRootEventBus() {
        return rootEventBus;
    }

    @Override
    public void onModuleLoad() {
        GWT.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            @Override
            public void onUncaughtException(final Throwable e) {
                log.log(Level.SEVERE, "PonySDK has encountered an internal error : ", e);
                Window.alert("PonySDK has encountered an internal error : " + e.getMessage());
            }
        });

        exportTriggerEvent();

        try {

            Long viewID = null;
            final Storage storage = Storage.getSessionStorageIfSupported();
            if (storage != null) {
                final String v = storage.getItem(APPLICATION.VIEW_ID);
                if (v != null) viewID = Long.parseLong(v);
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
                            CommunicationEntryPoint.this.data = data;
                            applicationViewID = (long) data.get(APPLICATION.VIEW_ID).isNumber().doubleValue();

                            if (storage != null) storage.setItem(APPLICATION.VIEW_ID, Long.toString(applicationViewID));

                            // if (data.containsKey(APPLICATION.SCRIPTS)) {
                            // final JSONArray scripts = data.get(APPLICATION.SCRIPTS).isArray();
                            //
                            // scriptToLoad = scripts.size();
                            //
                            // for (int i = 0; i < scripts.size(); i++) {
                            // final String script = scripts.get(i).isString().stringValue();
                            // ScriptInjector.fromUrl(GWT.getHostPageBaseURL() +
                            // script).setCallback(CommunicationEntryPoint.this).inject();
                            // }
                            // } else {
                            initUIBuilder();
                            // }
                        } else {
                            uiBuilder.update(data);
                            uiBuilder.hideLoadingMessageBox();
                        }
                    } catch (final RuntimeException exception) {
                        Window.alert("Loading application has failed #" + exception);
                    }
                }

                @Override
                public void onError(final Throwable exception) {
                    uiBuilder.onCommunicationError(exception);

                    rootEventBus.fireEvent(new CommunicationErrorEvent(exception));
                }

            };

            requestBuilder = new HttpRequestBuilder(requestCallback);
            requestBuilder.send(requestData.toString());

        } catch (final Exception e) {
            Window.alert("Loading application has failed #" + e);
        }
    }

    protected void initUIBuilder() {
        uiBuilder = new UIBuilder(applicationViewID, requestBuilder);
        uiBuilder.init();
        uiBuilder.update(data);
        uiBuilder.hideLoadingMessageBox();
    }

    public void sendDataToServer(final String objectID, final JavaScriptObject jsObject) {
        final PTInstruction instruction = new PTInstruction();
        instruction.setObjectID(Long.parseLong(objectID));
        instruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
        instruction.put(PROPERTY.NATIVE, jsObject);
        uiBuilder.sendDataToServer(instruction);
    }

    public native void exportTriggerEvent() /*-{
                                                 var that = this;
                                                 $wnd.sendDataToServer = function(id, element) {
                                                 $entry(that.@com.ponysdk.ui.terminal.CommunicationEntryPoint::sendDataToServer(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(id, element));
                                                 }
                                                 }-*/;

    public static native void reload() /*-{$wnd.location.reload();}-*/;

    @Override
    public void onFailure(final Exception reason) {
        Window.alert("Cannot load native script : " + reason.getMessage());
    }

    @Override
    public void onSuccess(final Void result) {
        // scriptToLoad--;
        // if (scriptToLoad == 0) initUIBuilder();
    }
}
