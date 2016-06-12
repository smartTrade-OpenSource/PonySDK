package com.ponysdk.core.server.servlet;

public interface WebsocketMonitor {
    void onMessageReceived(WebSocketServlet.WebSocket webSocket, String text);

    void onMessageProcessed(WebSocketServlet.WebSocket webSocket);

    void onBeforeFlush(WebSocketServlet.WebSocket webSocket, int position);

    void onAfterFlush(WebSocketServlet.WebSocket webSocket);
}
