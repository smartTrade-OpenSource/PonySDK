
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

public class HttpRequestBuilder extends RequestBuilder {

    private final static Logger log = Logger.getLogger(HttpRequestBuilder.class.getName());

    private final com.google.gwt.http.client.RequestBuilder requestBuilder = new com.google.gwt.http.client.RequestBuilder(com.google.gwt.http.client.RequestBuilder.POST, GWT.getHostPageBaseURL() + "p");

    public HttpRequestBuilder(final RequestCallback callback) {
        super(callback);
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

    @Override
    public void sendHeartbeat() {}
}
