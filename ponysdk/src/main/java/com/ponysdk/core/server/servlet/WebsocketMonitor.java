package com.ponysdk.core.server.servlet;

public interface WebsocketMonitor {
    void onMessageReceived(WebSocket webSocket, String text);

    void onMessageProcessed(WebSocket webSocket);

    void onBeforeFlush(WebSocket webSocket, int position);

    void onAfterFlush(WebSocket webSocket);
}
