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
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.HISTORY;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.socket.WebSocketClient;

public class CommunicationEntryPoint implements EntryPoint {

    private final static Logger log = Logger.getLogger(CommunicationEntryPoint.class.getName());

    protected WebSocketClient socketClient;

    protected RequestBuilder requestBuilder;

    protected UIBuilder uiBuilder;

    @Override
    public void onModuleLoad() {
        GWT.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            @Override
            public void onUncaughtException(final Throwable e) {
                log.log(Level.SEVERE, "PonySDK has encountered an internal error : ", e);
                Window.alert("PonySDK has encountered an internal error : " + e.getMessage());
            }
        });

        try {

            final PTInstruction requestData = new PTInstruction();

            final JSONArray cookies = new JSONArray();

            // load all cookies at startup
            final Collection<String> cookieNames = Cookies.getCookieNames();
            if (cookieNames != null) {
                final int i = 0;
                for (final String cookie : cookieNames) {
                    cookies.set(i, new JSONString(Cookies.getCookie(cookie)));
                }
            }

            requestData.put(APPLICATION.KEY, APPLICATION.KEY_.START);
            requestData.put(HISTORY.TOKEN, History.getToken());
            requestData.put(PROPERTY.COOKIE, cookies);

            final RequestCallback requestCallback = new RequestCallback() {

                @Override
                public void onDataReceived(final JSONObject data) {
                    try {
                        final List<PTInstruction> instructions = new ArrayList<PTInstruction>();

                        if (data.containsKey(APPLICATION.VIEW_ID)) {
                            final long viewID = (long) data.get(APPLICATION.VIEW_ID).isNumber().doubleValue();
                            final JSONArray jsonArray = data.get(APPLICATION.INSTRUCTIONS).isArray();

                            for (int i = 0; i < jsonArray.size(); i++) {
                                instructions.add(new PTInstruction(jsonArray.get(i).isObject().getJavaScriptObject()));
                            }

                            uiBuilder = new UIBuilder(viewID, requestBuilder);
                            uiBuilder.init();
                            uiBuilder.update(instructions);
                            uiBuilder.hideLoadingMessageBox();
                        } else {
                            final JSONArray jsonArray = data.get(APPLICATION.INSTRUCTIONS).isArray();

                            for (int i = 0; i < jsonArray.size(); i++) {
                                instructions.add(new PTInstruction(jsonArray.get(i).isObject().getJavaScriptObject()));
                            }

                            uiBuilder.update(instructions);
                            uiBuilder.hideLoadingMessageBox();
                        }
                    } catch (final RuntimeException exception) {
                        Window.alert("Loading application has failed #" + exception);
                    }
                }

                @Override
                public void onError(final Throwable exception) {
                    uiBuilder.onCommunicationError(exception);
                }
            };

            requestBuilder = new HttpRequestBuilder(requestCallback);
            requestBuilder.send(requestData.toString());

        } catch (final Exception e) {
            Window.alert("Loading application has failed #" + e);
        }
    }
}
