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

import com.ponysdk.core.terminal.ReconnectionChecker;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.request.WebSocketRequestBuilder;

import elemental2.core.ArrayBuffer;
import elemental2.core.Uint8Array;
import elemental2.dom.CloseEvent;
import elemental2.dom.DomGlobal;
import elemental2.dom.MessageEvent;
import elemental2.dom.WebSocket;

public class WebSocketClient {

    private static final Logger log = Logger.getLogger(WebSocketClient.class.getName());

    private final WebSocket webSocket;

    private long lastMessageTime = -1;

    public WebSocketClient(final String url, final UIBuilder uiBuilder, final ReconnectionChecker reconnectionChecker) {
        this.webSocket = new WebSocket(url);
        this.webSocket.binaryType = "arraybuffer";

        webSocket.onopen = event -> {
            uiBuilder.init(new WebSocketRequestBuilder(WebSocketClient.this));
            if (log.isLoggable(Level.INFO)) log.info("WebSocket connected");
            lastMessageTime = System.currentTimeMillis();
        };

        webSocket.onclose = event -> {
            if (event instanceof CloseEvent closeEvent) {
                final int statusCode = closeEvent.code;
                if (log.isLoggable(Level.INFO)) log.info("WebSocket disconnected : " + statusCode);
                if (statusCode != 1000) reconnectionChecker.detectConnectionFailure();
            } else {
                log.severe("WebSocket disconnected : " + event);
                reconnectionChecker.detectConnectionFailure();
            }
        };

        webSocket.onerror = event -> {
            log.severe("WebSocket error : " + event);
        };

        webSocket.onmessage = event -> {
            lastMessageTime = System.currentTimeMillis();

            final Object data = ((MessageEvent) event).data;
            if (data instanceof ArrayBuffer buffer) {
                try {
                    uiBuilder.updateMainTerminal(new Uint8Array(buffer, 0, getByteLength(buffer)));
                } catch (final Exception e) {
                    log.log(Level.SEVERE, "Error while processing the " + buffer, e);
                }
            }
        };
    }

    /**
     * Sends a text message (legacy JSON path, kept for backward compatibility).
     */
    public void send(final String message) {
        webSocket.send(message);
    }

    /**
     * Sends a binary message (new compact binary protocol).
     */
    public void sendBinary(final ArrayBuffer buffer) {
        webSocket.send(buffer);
    }

    public void close() {
        webSocket.close();
    }

    public void close(final int code, final String reason) {
        webSocket.close(code, reason);
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    private static int getByteLength(final ArrayBuffer buf) {
        return buf.byteLength;
    }

}
