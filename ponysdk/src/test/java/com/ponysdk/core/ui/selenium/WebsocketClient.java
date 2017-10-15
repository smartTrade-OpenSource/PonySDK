package com.ponysdk.core.ui.selenium;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

public class WebsocketClient extends Endpoint {

    private MessageHandler.Whole<ByteBuffer> handler;
    private Session session;

    public void connect(URI uri) throws Exception {
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        ClientManager client = ClientManager.createClient();
        client.getProperties().put(ClientProperties.REDIRECT_ENABLED, true);
        client.connectToServer(this, cec, uri);
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        if (handler != null) session.addMessageHandler(handler);
    }

    public void sendMessage(String message) throws IOException {
        session.getBasicRemote().sendText(message);
    }

    public void setMessageHandler(MessageHandler.Whole<ByteBuffer> handler) {
        this.handler = handler;
        if (session != null) session.addMessageHandler(handler);
    }

    public void close() {
        try {
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
