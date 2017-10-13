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

package com.ponysdk.core.terminal.socket;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.Scheduler;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.terminal.ReconnectionChecker;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.request.WebSocketRequestBuilder;

import elemental.client.Browser;
import elemental.events.CloseEvent;
import elemental.events.MessageEvent;
import elemental.html.ArrayBuffer;
import elemental.html.WebSocket;
import elemental.html.Window;

public class WebSocketClient implements MessageSender {

    private static final Logger log = Logger.getLogger(WebSocketClient.class.getName());

    private final WebSocket webSocket;
    private final UIBuilder uiBuilder;

    private final Window window;

    private boolean initialized;

    public WebSocketClient(final String url, final UIBuilder uiBuilder, final WebSocketDataType webSocketDataType,
            final ReconnectionChecker reconnectionChecker) {
        this.uiBuilder = uiBuilder;

        window = Browser.getWindow();
        webSocket = window.newWebSocket(url);
        webSocket.setBinaryType(webSocketDataType.getName());

        final MessageReader messageReader;
        if (WebSocketDataType.ARRAYBUFFER == webSocketDataType) messageReader = new ArrayBufferReader(this);
        else if (WebSocketDataType.BLOB == webSocketDataType) messageReader = new BlobReader(this);
        else throw new IllegalArgumentException("Wrong reader type : " + webSocketDataType);

        webSocket.setOnopen(event -> {
            if (log.isLoggable(Level.INFO)) log.info("WebSocket connected");

            Scheduler.get().scheduleFixedDelay(() -> {
                if (webSocket.getReadyState() == WebSocket.OPEN) {
                    if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Heart beat sent");
                    send(ClientToServerModel.HEARTBEAT.toStringValue());
                }
                return true;
            }, 1000);
        });
        webSocket.setOnclose(event -> {
            if (event instanceof CloseEvent) {
                final CloseEvent closeEvent = (CloseEvent) event;
                final int statusCode = closeEvent.getCode();
                if (log.isLoggable(Level.INFO)) log.info("WebSocket disconnected : " + statusCode);
                // If it's a not normal disconnection
                if (statusCode != 1000) reconnectionChecker.detectConnectionFailure();
            } else {
                log.severe("WebSocket disconnected : " + event);
                reconnectionChecker.detectConnectionFailure();
            }
        });
        webSocket.setOnerror(event -> log.severe("WebSocket error : " + event));
        webSocket.setOnmessage(event -> messageReader.read((MessageEvent) event));
    }

    @Override
    public void read(final ArrayBuffer arrayBuffer) {
        try {
            if (!initialized) {
                uiBuilder.init(new WebSocketRequestBuilder(WebSocketClient.this));
                initialized = true;
            }

            uiBuilder.updateMainTerminal(window.newUint8Array(arrayBuffer, 0, arrayBuffer.getByteLength()));
        } catch (final Exception e) {
            log.log(Level.SEVERE, "Error while processing the " + arrayBuffer, e);
        }
    }

    public void send(final String message) {
        webSocket.send(message);
    }

    public void close() {
        webSocket.close();
    }

    public enum WebSocketDataType {

        ARRAYBUFFER("arraybuffer"),
        BLOB("blob");

        private String name;

        WebSocketDataType(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

}
