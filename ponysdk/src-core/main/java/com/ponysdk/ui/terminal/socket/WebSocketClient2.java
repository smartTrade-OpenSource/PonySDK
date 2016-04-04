
package com.ponysdk.ui.terminal.socket;

import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.ponysdk.ui.terminal.UIBuilder;
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
        webSocket.setBinaryType("arraybuffer");
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
            if (event.getType().equals("open")) {
                requestBuilder = new WebSocketRequestBuilder(this, null);
                callback.connected();
            } else if (event.getType().equals("close")) {
                callback.disconnected();
            } else if (event.getType().equals("message")) {
                final ArrayBuffer arrayBuffer = (ArrayBuffer) ((MessageEvent) event).getData();
                callback.message(arrayBuffer);
            }
        }
    }

    public void sendHeartbeat() {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put(Model.HEARTBEAT.getKey(), JSONNull.getInstance());
        jsonObject.put(Model.APPLICATION_VIEW_ID.getKey(), new JSONNumber(UIBuilder.sessionID));
        webSocket.send(jsonObject.toString());
        //
        // final int timeStamp = (int) (new Date().getTime() * .001);
        // final JSONObject jso = new JSONObject();
        // jso.put(Model.APPLICATION_PING.getKey(), new JSONNumber(timeStamp));
    }

}
