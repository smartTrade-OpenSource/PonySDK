
package com.ponysdk.ui.terminal.socket;

import java.util.LinkedList;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.ponysdk.ui.terminal.UIBuilder;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.request.WebSocketRequestBuilder;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.MessageEvent;
import elemental.html.Blob;
import elemental.html.FileReader;
import elemental.html.WebSocket;
import elemental.html.Window;

public class WebSocketClient2 implements EventListener {

    private final WebSocket webSocket;

    private final LinkedList<Blob> queue = new LinkedList<>();

    private final WebSocketCallback callback;
    private final FileReader fileReader;

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

        fileReader = window.newFileReader();
        fileReader.setOnload(this);
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
                final Blob blob = (Blob) ((MessageEvent) event).getData();
                if (fileReader.getReadyState() != 1) {
                    fileReader.readAsBinaryString(blob);
                } else {
                    queue.add(blob);
                }
            } else if (event.getType().equals("error")) {
                // callback.(message);
            }
        } else if (event.getSrcElement() == fileReader) {
            if (event.getType().equals("load")) {
                callback.message((String) fileReader.getResult());
            }

            if (!queue.isEmpty()) {
                final Blob blob = queue.removeFirst();
                fileReader.readAsBinaryString(blob);
            }
        }
    }

    public void sendHeartbeat() {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put(Model.HEARTBEAT.getKey(), new JSONString(""));
        jsonObject.put(Model.APPLICATION_VIEW_ID.getKey(), new JSONNumber(UIBuilder.sessionID));
        webSocket.send(jsonObject.toString());
        //
        // final int timeStamp = (int) (new Date().getTime() * .001);
        // final JSONObject jso = new JSONObject();
        // jso.put(Model.APPLICATION_PING.getKey(), new JSONNumber(timeStamp));

    }

}
