/*
 * Copyright (c) 2017 PonySDK
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

package com.ponysdk.core.server.servlet;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.listener.ProcessListener;

public class WebSocketServer implements WebSocketListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    private WebsocketMonitor monitor;

    private Session session;

    private ProcessListener processListener;

    public WebSocketServer() {
    }

    @Override
    public void onWebSocketConnect(final Session session) {
        this.session = session;
        processListener.onConnected();
    }

    @Override
    public void onWebSocketError(final Throwable throwable) {
        log.error("websocket error", throwable);
    }

    @Override
    public void onWebSocketClose(final int statusCode, final String reason) {
        if (monitor != null) monitor.closing(this, statusCode, reason);

        try {
            close(statusCode, reason);
        } catch (final Exception e) {
            log.error("Cannot close websocket status code {}, reason {}", statusCode, reason);
        } finally {
            if (monitor != null) monitor.closed(this, statusCode, reason);
        }
    }

    @Override
    public void onWebSocketText(final String text) {
        if (monitor != null) monitor.processing(this, text);
        try {
            process(text);
        } catch (final Exception e) {
            log.error("Cannot process text {}", text, e);
        } finally {
            if (monitor != null) monitor.processed(this, text);
        }
    }

    @Override
    public void onWebSocketBinary(final byte[] payload, final int offset, final int len) {
        if (monitor != null) monitor.processing(this, payload, offset, len);
        try {
            process(payload, offset, len);
        } catch (final Exception e) {
            log.error("Cannot process bytes", e);
        } finally {
            if (monitor != null) monitor.processed(this, payload, offset, len);
        }
    }

    protected void close(final int statusCode, final String reason) {
        processListener.onClose();
    }

    protected void process(final String text) {
        processListener.process(text);
    }

    protected void process(final byte[] payload, final int offset, final int len) {
        processListener.process(payload, offset, len);
    }

    public void close() {
        if (isOpen()) {
            if (log.isInfoEnabled()) log.info("Closing websocket programaticly");
            session.close();
        }
    }

    public boolean isOpen() {
        return session != null && session.isOpen();
    }

    public void setMonitor(final WebsocketMonitor monitor) {
        this.monitor = monitor;
    }

    public void sendString(final String text) {
        if (monitor != null) monitor.sending(this, text);
        try {
            session.getRemote().sendString(text);
        } catch (final Exception e) {
            log.error("Cannot process text {}", text, e);
        } finally {
            if (monitor != null) monitor.sent(this, text);
        }
    }

    public void setProcessListener(final ProcessListener processListener) {
        this.processListener = processListener;
    }

}
