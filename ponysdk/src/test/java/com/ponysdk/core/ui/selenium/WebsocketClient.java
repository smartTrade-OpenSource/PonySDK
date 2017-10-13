package com.ponysdk.core.ui.selenium;

import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;

public class WebsocketClient extends Endpoint implements MessageHandler.Whole, MessageHandler.Partial {

    private Session session;

    public void connect(URI uri) throws Exception {
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        ClientManager client = ClientManager.createClient();
        client.connectToServer(this, cec, uri);
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        this.session.addMessageHandler(this);
    }

    public void sendMessage(String message) throws IOException {
        session.getBasicRemote().sendText(message);
    }

    @Override
    public void onMessage(Object message) {
        System.out.println("Received message: " + message);
    }

    @Override
    public void onMessage(Object partialMessage, boolean last) {
        System.out.println("Received partialMessage: " + partialMessage);
    }
}
