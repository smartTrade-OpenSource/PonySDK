/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.ui.terminal.socket;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;
import com.ponysdk.ui.terminal.model.ServerToClientModel;
import com.ponysdk.ui.terminal.request.RequestBuilder;
import com.ponysdk.ui.terminal.request.WebSocketRequestBuilder;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.MessageEvent;
import elemental.html.ArrayBuffer;
import elemental.html.WebSocket;
import elemental.html.Window;

public class WebSocketClient2 implements EventListener {

    private static final Logger log = Logger.getLogger(WebSocketClient2.class.getName());

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

    private RequestBuilder requestBuilder;

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

    public RequestBuilder getRequestBuilder() {
        return requestBuilder;
    }

    public void send(final String message) {
        webSocket.send(message);
    }

    @Override
    public void handleEvent(final Event event) {
        if (event.getSrcElement() == webSocket) {
            if (EventType.OPEN.getText().equals(event.getType())) {
                requestBuilder = new WebSocketRequestBuilder(this);
                callback.connected();
            } else if (EventType.CLOSE.getText().equals(event.getType())) {
                callback.disconnected();
            } else if (EventType.MESSAGE.getText().equals(event.getType())) {
                final ArrayBuffer arrayBuffer = (ArrayBuffer) ((MessageEvent) event).getData();

                try {
                    final ReaderBuffer buffer = new ReaderBuffer(arrayBuffer);
                    // Get the first element on the message, always a key of element of the Model enum
                    final BinaryModel type = buffer.getBinaryModel();

                    if (type.getModel() == ServerToClientModel.HEARTBEAT) {
                        if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Heart beat");
                        sendHeartbeat();
                    } else if (type.getModel() == ServerToClientModel.APPLICATION_SEQ_NUM) {
                        callback.message(buffer);
                    } else {
                        log.severe("Unknown model : " + type.getModel());
                    }
                } catch (final Exception e) {
                    log.log(Level.SEVERE, "Cannot parse " + arrayBuffer, e);
                }
            }
        }
    }

    private void sendHeartbeat() {
        // Only send one character, no need a json message
        /*
         * final JSONObject jsonObject = new JSONObject();
         * jsonObject.put(Model.HEARTBEAT.toStringValue(), JSONNull.getInstance());
         * jsonObject.put(Model.APPLICATION_VIEW_ID.toStringValue(), new
         * JSONNumber(UIBuilder.sessionID));
         * send(jsonObject.toString());
         */
        send(ClientToServerModel.HEARTBEAT.toStringValue());
    }

    public void close() {
        webSocket.close();
    }

}
