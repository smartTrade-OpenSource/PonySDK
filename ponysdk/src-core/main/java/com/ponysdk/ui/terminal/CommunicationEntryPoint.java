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
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.HISTORY;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class CommunicationEntryPoint implements EntryPoint {

    private final static Logger log = Logger.getLogger(CommunicationEntryPoint.class.getName());

    @Override
    public void onModuleLoad() {
        GWT.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            @Override
            public void onUncaughtException(final Throwable e) {
                Window.alert("PonySDK has encountered an internal error : " + e.getMessage());
                log.log(Level.SEVERE, "PonySDK has encountered an internal error : ", e);
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

            final RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, GWT.getModuleBaseURL() + "p");

            requestData.put(APPLICATION.KEY, APPLICATION.KEY_.START);
            requestData.put(HISTORY.TOKEN, History.getToken());
            requestData.put(PROPERTY.COOKIE, cookies);

            builder.sendRequest(requestData.toString(), new RequestCallback() {

                @Override
                public void onError(final Request request, final Throwable exception) {
                    log.log(Level.SEVERE, "Error ", exception);
                    if (exception instanceof StatusCodeException) {
                        final StatusCodeException codeException = (StatusCodeException) exception;
                        if (codeException.getStatusCode() == 0) return;

                    }
                    Window.alert("Cannot inititialize the application : " + exception.getMessage() + "\n" + exception + "\nPlease reload your application");
                }

                @Override
                public void onResponseReceived(final Request request, final Response response) {
                    if (200 == response.getStatusCode()) {

                        final List<PTInstruction> instructions = new ArrayList<PTInstruction>();

                        final JSONObject object = JSONParser.parseLenient(response.getText()).isObject();

                        final long viewID = (long) object.get(APPLICATION.VIEW_ID).isNumber().doubleValue();
                        final JSONArray jsonArray = object.get(APPLICATION.INSTRUCTIONS).isArray();

                        for (int i = 0; i < jsonArray.size(); i++) {
                            instructions.add(new PTInstruction(jsonArray.get(i).isObject().getJavaScriptObject()));
                        }

                        final UIBuilder uiBuilder = new UIBuilder(viewID);
                        uiBuilder.init();
                        uiBuilder.update(instructions);
                    } else {
                        Window.alert("Couldn't retrieve JSON (" + response.getStatusText() + ")");
                    }
                }
            });

        } catch (final Exception e) {
            Window.alert("Loading application has failed #" + e);
        }
    }
}
