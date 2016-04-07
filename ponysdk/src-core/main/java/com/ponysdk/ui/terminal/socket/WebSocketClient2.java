
package com.ponysdk.ui.terminal.socket;

import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.request.WebSocketRequestBuilder;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.MessageEvent;
import elemental.html.ArrayBuffer;
import elemental.html.WebSocket;
import elemental.html.Window;

public class WebSocketClient2 implements EventListener {

    private static final String ARRAYBUFFER_TYPE = "arraybuffer";

    private static enum EventType {
        OPEN("open"),
        CLOSE("close"),
        MESSAGE("message");

        private String text;

        private EventType(final String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    private final WebSocket webSocket;

    private final WebSocketCallback callback;

    private WebSocketRequestBuilder requestBuilder;

    public WebSocketRequestBuilder getRequestBuilder() {
        return requestBuilder;
    }

    public WebSocketClient2(final String url, final WebSocketCallback callback) {
        this.callback = callback;

        final Window window = Browser.getWindow();

        webSocket = window.newWebSocket(url);

        webSocket.setOnclose(this);
        webSocket.setOnerror(this);
        webSocket.setOnmessage(this);
        webSocket.setOnopen(this);
        webSocket.setBinaryType(ARRAYBUFFER_TYPE);
    }

    public void send(final String message) {
        webSocket.send(message);
    }

    public void close() {
        webSocket.close();
    }

    public boolean isSupported() {
        return true;
    }

    @Override
    public void handleEvent(final Event event) {
        if (event.getSrcElement() == webSocket) {
            if (EventType.OPEN.getText().equals(event.getType())) {
                requestBuilder = new WebSocketRequestBuilder(this, null);
                callback.connected();
            } else if (EventType.CLOSE.getText().equals(event.getType())) {
                callback.disconnected();
            } else if (EventType.MESSAGE.getText().equals(event.getType())) {
                final ArrayBuffer arrayBuffer = (ArrayBuffer) ((MessageEvent) event).getData();
                callback.message(arrayBuffer);
            }
        }
    }

    public void sendHeartbeat() {
        // Only send one character, no need a json message
        /*
         * final JSONObject jsonObject = new JSONObject();
         * jsonObject.put(Model.HEARTBEAT.toStringValue(), JSONNull.getInstance());
         * jsonObject.put(Model.APPLICATION_VIEW_ID.toStringValue(), new
         * JSONNumber(UIBuilder.sessionID));
         * webSocket.send(jsonObject.toString());
         */
        webSocket.send(Model.HEARTBEAT.toStringValue());
    }

}
