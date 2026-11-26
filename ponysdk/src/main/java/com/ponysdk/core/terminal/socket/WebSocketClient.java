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

public class WebSocketClient {

    private static final Logger log = Logger.getLogger(WebSocketClient.class.getName());

    /** Close code of a server that cannot resume the context: the page reloads. */
    private static final int CLOSE_RESUME_REFUSED = 4001;

    private final Window window;
    private final String url;
    private final UIBuilder uiBuilder;
    private final ReconnectionChecker reconnectionChecker;
    private WebSocket webSocket;
    private long lastMessageTime = -1;

    /** Bytes received from the server since the context was created, across reconnections. */
    private double receivedBytes;

    public WebSocketClient(final String url, final UIBuilder uiBuilder, final ReconnectionChecker reconnectionChecker) {
        this.window = Browser.getWindow();
        this.url = url;
        this.uiBuilder = uiBuilder;
        this.reconnectionChecker = reconnectionChecker;
        open(url, false);
    }

    /**
     * Opens a new WebSocket to resume the given context, from the bytes received so far. The server answers with
     * what was not received and a RECONNECT_CONTEXT block, or closes it with {@link #CLOSE_RESUME_REFUSED}.
     */
    public void reconnect(final int contextId) {
        final String resumeUrl = url + "&" + ClientToServerModel.RECONNECT_UI_CONTEXT_ID.toStringValue() + "=" + contextId + "&"
                + ClientToServerModel.RECONNECT_OFFSET.toStringValue() + "=" + (long) receivedBytes;
        open(resumeUrl, true);
    }

    private void open(final String socketUrl, final boolean resuming) {
        final WebSocket socket = window.newWebSocket(socketUrl);
        this.webSocket = socket;
        socket.setBinaryType("arraybuffer");
        socket.setOnopen(event -> {
            if (socket != webSocket) return;
            // A resumed context keeps its graphical system: only the first connection initializes it
            if (!resuming) uiBuilder.init(new WebSocketRequestBuilder(WebSocketClient.this));
            if (log.isLoggable(Level.INFO)) log.info(resuming ? "WebSocket reconnected" : "WebSocket connected");
            lastMessageTime = System.currentTimeMillis();
        });
        socket.setOnclose(event -> {
            if (socket != webSocket) return;
            if (event instanceof CloseEvent) {
                final CloseEvent closeEvent = (CloseEvent) event;
                final int statusCode = closeEvent.getCode();
                if (log.isLoggable(Level.INFO)) log.info("WebSocket disconnected : " + statusCode);
                if (resuming && statusCode == CLOSE_RESUME_REFUSED) reconnectionChecker.onResumeRefused();
                // If it's a not normal disconnection
                // A socket opened to resume a context failed whatever its close code: the checker tries again
                else if (resuming || statusCode != 1000) reconnectionChecker.detectConnectionFailure();
            } else {
                log.severe("WebSocket disconnected : " + event);
                reconnectionChecker.detectConnectionFailure();
            }
        });
        socket.setOnerror(event -> {
            log.severe("WebSocket error : " + event);
        });
        socket.setOnmessage(event -> {
            if (socket != webSocket) return;
            lastMessageTime = System.currentTimeMillis();
            final Object data = ((MessageEvent) event).getData();
            if (data instanceof ArrayBuffer) {
                final ArrayBuffer buffer = (ArrayBuffer) data; //TODO nciaravola avoid cast ?
                // Counted before it is read: a block cut by the end of the frame is completed by the next bytes,
                // from this socket or from the one a reconnection opens
                receivedBytes += buffer.getByteLength();
                try {
                    uiBuilder.updateMainTerminal(window.newUint8Array(buffer, 0, buffer.getByteLength()));
                } catch (final Exception e) {
                    log.log(Level.SEVERE, "Error while processing the " + buffer, e);
                }
            }
        });
    }

    public void send(final String message) {
        // Not while a reconnection opens the WebSocket (send would throw) nor once it is closed: an instruction sent
        // while the connection is lost is dropped, never sent twice
        if (webSocket.getReadyState() == 1) webSocket.send(message);
        else if (log.isLoggable(Level.FINE)) log.fine("Connection lost, a message was dropped");
    }

    public void close() {
        webSocket.close();
    }

    /** True while the socket is being opened, or open. */
    public boolean isConnectingOrOpen() {
        return webSocket.getReadyState() <= 1;
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

    /** Bytes received from the server since the context was created. */
    public double getReceivedBytes() {
        return receivedBytes;
    }

    /**
     <a href="https://developer.mozilla.org/en-US/docs/Web/API/WebSocket/readyState">See ready state codes</a>
     */
    public int getReadyState() { return webSocket.getReadyState(); }
}
