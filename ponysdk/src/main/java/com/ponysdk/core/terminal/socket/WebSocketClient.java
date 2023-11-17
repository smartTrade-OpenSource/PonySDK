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

import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.request.WebSocketRequestBuilder;
import elemental2.core.ArrayBuffer;
import elemental2.core.Uint8Array;
import elemental2.dom.*;
import elemental2.dom.WebSocket.OnmessageFn.EventMessageEventTypeParameterUnionType;

import java.util.logging.Level;
import java.util.logging.Logger;

public class WebSocketClient {

    private static final Logger log = Logger.getLogger(WebSocketClient.class.getName());

    private final String url;
    private WebSocket webSocket;
    private final UIBuilder uiBuilder;
    private long lastMessageTime = -1;

    public WebSocketClient(final String url, final UIBuilder uiBuilder) {
        this.uiBuilder = uiBuilder;
        this.url = url;
    }

    public void connect() {
        webSocket = new WebSocket(url);
        webSocket.binaryType = "arraybuffer";
        webSocket.onopen = this::onOpen;
        webSocket.onclose = this::onClose;
        webSocket.onerror = this::onError;
        webSocket.onmessage = this::onMessage;
    }

    private void onOpen(Event event) {
        log.info("WebSocket connected");
        uiBuilder.init(new WebSocketRequestBuilder(WebSocketClient.this));
        lastMessageTime = System.currentTimeMillis();
    }

    private void onClose(CloseEvent event) {
        log.info("WebSocket disconnected: " + event.code);
        DomGlobal.setTimeout(p0 -> {
            log.info("Trying to reconnect");
            connect();
        }, 1000);
    }

    private void onError(Event event) {
        log.severe("WebSocket error : " + event);
    }

    private void onMessage(MessageEvent<EventMessageEventTypeParameterUnionType> event) {
        lastMessageTime = System.currentTimeMillis();
        ArrayBuffer buffer = event.data.asArrayBuffer();
        try {
            uiBuilder.updateMainTerminal(new Uint8Array(buffer));
        } catch (final Exception e) {
            log.log(Level.SEVERE, "Error while processing the " + buffer, e);
        }
    }

    public void send(final String message) {
        webSocket.send(message);
    }

    public void close() {
        webSocket.close();
    }

    public void close(final int code, final String reason) {
        webSocket.close(code, reason);
    }

    /**
     * @return the lastMessageTime
     */
    public long getLastMessageTime() {
        return lastMessageTime;
    }

}
