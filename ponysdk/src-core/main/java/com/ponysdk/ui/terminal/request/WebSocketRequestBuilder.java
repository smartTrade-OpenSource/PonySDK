
package com.ponysdk.ui.terminal.request;

import com.ponysdk.ui.terminal.socket.WebSocketClient2;

public class WebSocketRequestBuilder implements RequestBuilder {

    private final WebSocketClient2 webSocketClient;

    public WebSocketRequestBuilder(final WebSocketClient2 webSocketClient) {
        this.webSocketClient = webSocketClient;
    }

    @Override
    public void send(final String s) {
        webSocketClient.send(s);
    }

}
