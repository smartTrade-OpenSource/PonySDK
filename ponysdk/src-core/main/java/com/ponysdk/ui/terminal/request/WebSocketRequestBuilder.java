
package com.ponysdk.ui.terminal.request;

import com.ponysdk.ui.terminal.socket.WebSocketClient2;

public class WebSocketRequestBuilder extends RequestBuilder {

    private final WebSocketClient2 webSocketClient;

    public WebSocketRequestBuilder(final WebSocketClient2 webSocketClient, final RequestCallback callback) {
        super(callback);
        this.webSocketClient = webSocketClient;
    }

    @Override
    public void send(final String s) {
        // try {
        // UIBuilder.getRootEventBus().fireEvent(new HttpRequestSendEvent());
        webSocketClient.send(s);

        // , new com.google.gwt.http.client.RequestCallback() {
        //
        // @Override
        // public void onResponseReceived(final Request request, final Response response) {
        //
        // UIBuilder.getRootEventBus().fireEvent(new HttpResponseReceivedEvent());
        //
        // if (response.getStatusCode() != 200) {
        // onError(request, new StatusCodeException(response.getStatusCode(), response.getStatusText()));
        // return;
        // }
        //
        // if (response.getText() == null || response.getText().isEmpty()) return;
        // callback.onDataReceived(JSONParser.parseStrict(response.getText()).isObject());
        // }
        //
        // @Override
        // public void onError(final Request request, final Throwable exception) {
        // UIBuilder.getRootEventBus().fireEvent(new HttpResponseReceivedEvent(exception));
        // callback.onError(exception);
        // }
        // });

        // } catch (final RequestException e) {
        // log.log(Level.SEVERE, "Error ", e);
        // }
    }

    @Override
    public void sendHeartbeat() {
        webSocketClient.sendHeartbeat();
    }
}
