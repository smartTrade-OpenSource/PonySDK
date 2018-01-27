package com.ponysdk.core.server.servlet;

import com.ponysdk.core.server.application.AbstractApplicationManager;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.Map;

public class CustomConfigurator extends ServerEndpointConfig.Configurator {

    private final AbstractApplicationManager applicationManager;
    private ThreadLocal<HandshakeRequest> requests = new ThreadLocal<>();

    public CustomConfigurator(AbstractApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        requests.set(request);
        super.modifyHandshake(sec, request, response);
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        T endpoint = super.getEndpointInstance(endpointClass);

        if (endpoint instanceof WebSocket) {
            WebSocket webSocket = (WebSocket) endpoint;
            webSocket.setRequest(requests.get());
            webSocket.setApplicationManager(applicationManager);
        }

        return endpoint;
    }
}