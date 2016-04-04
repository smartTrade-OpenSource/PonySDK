
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
        webSocketClient.send(s);
    }

    @Override
    public void sendHeartbeat() {
        webSocketClient.sendHeartbeat();
    }
}
