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

package com.ponysdk.ui.terminal.request;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.ponysdk.ui.terminal.UIBuilder;
import com.ponysdk.ui.terminal.event.HttpRequestSendEvent;
import com.ponysdk.ui.terminal.event.HttpResponseReceivedEvent;

/**
 * @deprecated Useless
 */
@Deprecated
public class HttpRequestBuilder implements RequestBuilder {

    protected final RequestCallback callback;

    private final static Logger log = Logger.getLogger(HttpRequestBuilder.class.getName());

    private final com.google.gwt.http.client.RequestBuilder requestBuilder = new com.google.gwt.http.client.RequestBuilder(
            com.google.gwt.http.client.RequestBuilder.POST, GWT.getHostPageBaseURL() + "p");

    public HttpRequestBuilder(final RequestCallback callback) {
        this.callback = callback;
    }

    @Override
    public void send(final String s) {
        try {
            UIBuilder.getRootEventBus().fireEvent(new HttpRequestSendEvent());
            requestBuilder.sendRequest(s, new com.google.gwt.http.client.RequestCallback() {

                @Override
                public void onResponseReceived(final Request request, final Response response) {

                    UIBuilder.getRootEventBus().fireEvent(new HttpResponseReceivedEvent());

                    if (response.getStatusCode() != 200) {
                        onError(request, new StatusCodeException(response.getStatusCode(), response.getStatusText()));
                        return;
                    }

                    if (response.getText() == null || response.getText().isEmpty()) return;
                    callback.onDataReceived(JSONParser.parseStrict(response.getText()).isObject());
                }

                @Override
                public void onError(final Request request, final Throwable exception) {
                    UIBuilder.getRootEventBus().fireEvent(new HttpResponseReceivedEvent(exception));
                    callback.onError(exception);
                }
            });

        } catch (final RequestException e) {
            log.log(Level.SEVERE, "Error ", e);
        }
    }

}
