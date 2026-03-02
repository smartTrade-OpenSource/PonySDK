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

package com.ponysdk.core.server.websocket;

import com.ponysdk.core.server.metrics.PonySDKMetrics;
import com.ponysdk.core.model.ClientToServerModel;

import java.time.Duration;

import jakarta.servlet.http.HttpSession;

import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.servlet.SessionManager;
import com.ponysdk.core.server.stm.TxnContext;

public class WebSocketServlet extends JettyWebSocketServlet {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServlet.class);

    private int maxIdleTime = 1000000;
    private final ApplicationManager applicationManager;
    private WebsocketMonitor monitor;
    private PonySDKMetrics metrics;

    public WebSocketServlet(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    @Override
    protected void configure(final JettyWebSocketServletFactory factory) {
        factory.setIdleTimeout(Duration.ofMillis(maxIdleTime));
        // permessage-deflate is enabled by default in Jetty 12, no need to register manually
        factory.setCreator(this::createWebsocket);
    }

    protected WebSocket createWebsocket(final JettyServerUpgradeRequest request, final JettyServerUpgradeResponse response) {
        final WebSocket webSocket = new WebSocket();
        webSocket.setRequest(request);
        webSocket.setApplicationManager(applicationManager);
        webSocket.setMonitor(monitor);
        webSocket.setMetrics(metrics);

        // Note: this TxnContext is orphaned on successful reconnection — the resumed UIContext
        // keeps its original TxnContext. The orphan has no strong refs from long-lived objects
        // and will be GC'd normally. Creating it unconditionally keeps the code simple.
        final TxnContext context = new TxnContext(webSocket);
        webSocket.setContext(context);

        // Transparent reconnection: if client sends a uiContextId, try to resume
        final String reconnectIdParam = request.getHttpServletRequest()
                .getParameter(ClientToServerModel.RECONNECT_UI_CONTEXT_ID.toStringValue());
        if (reconnectIdParam != null && applicationManager.getConfiguration().getReconnectionTimeoutMs() > 0) {
            try {
                final int uiContextId = Integer.parseInt(reconnectIdParam);
                webSocket.setReconnectContextId(uiContextId);
            } catch (final NumberFormatException e) {
                log.warn("Invalid reconnect uiContextId: {}", reconnectIdParam);
            }
        }

        if (request.getHttpServletRequest().getServletContext().getSessionCookieConfig() != null) {
            configureWithSession(request, context);
        }

        return webSocket;
    }

    protected void configureWithSession(final JettyServerUpgradeRequest request, final TxnContext context) {
        // Force session creation if there is no session
        request.getHttpServletRequest().getSession(true);
        final HttpSession httpSession = request.getHttpServletRequest().getSession();
        if (httpSession != null) {
            final String applicationId = httpSession.getId();

            Application application = SessionManager.get().getApplication(applicationId);
            if (application == null) {
                application = new Application(applicationId, httpSession, applicationManager.getConfiguration());
                SessionManager.get().registerApplication(application);
            }
            context.setApplication(application);
        } else {
            log.error("No HTTP session found");
            throw new IllegalStateException("No HTTP session found");
        }
    }

    public void setMaxIdleTime(final int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public void setWebsocketMonitor(final WebsocketMonitor monitor) {
        this.monitor = monitor;
    }

    public void setMetrics(final PonySDKMetrics metrics) {
        this.metrics = metrics;
    }

}
