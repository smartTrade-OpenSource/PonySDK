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

import java.time.Duration;

import jakarta.servlet.http.HttpSession;

import org.eclipse.jetty.ee11.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee11.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.ee11.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee11.websocket.server.JettyWebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.servlet.SessionManager;
import com.ponysdk.core.server.stm.TxnContext;

public class WebSocketServlet extends JettyWebSocketServlet {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServlet.class);

    private static final long serialVersionUID = 1L;
    private int maxIdleTime = 1000000;
    private final ApplicationManager applicationManager;
    private WebsocketMonitor monitor;

    public WebSocketServlet(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    @Override
    protected void configure(final JettyWebSocketServletFactory factory) {
        factory.setIdleTimeout(Duration.ofMillis(maxIdleTime));
        // Jetty 12 negotiates the standard "permessage-deflate" extension automatically. The Jetty 9
        // custom PonyPerMessageDeflateExtension is gone (Jetty 12 moved the deflate extension into
        // non-public websocket.core.internal). The per-WebSocket-frame byte accounting it fed is
        // reimplemented from the WebSocketConnection wire counters instead (see WebSocket#setListener).
        factory.setCreator((request, response) -> createWebsocket(request, response));
    }

    protected WebSocket createWebsocket(final JettyServerUpgradeRequest request, final JettyServerUpgradeResponse response) {
        final WebSocket webSocket = new WebSocket();
        webSocket.setRequest(request);
        // Capture request-derived data NOW, while the upgrade request is still live. Jetty 12 recycles
        // the underlying servlet request once the upgrade completes, so any later read (parameter map,
        // headers, session) throws NPE or returns another request's data. UIContext serves this snapshot.
        webSocket.setUpgradeRequestData(captureUpgradeData(request));
        webSocket.setApplicationManager(applicationManager);
        webSocket.setMonitor(monitor);

        final TxnContext context = new TxnContext(webSocket);
        webSocket.setContext(context);

        if (request.getHttpServletRequest().getServletContext().getSessionCookieConfig() != null) {
            configureWithSession(request, context);
        }

        return webSocket;
    }

    private static UpgradeRequestData captureUpgradeData(final JettyServerUpgradeRequest request) {
        java.util.Map<String, java.util.List<String>> parameterMap = null;
        java.util.Map<String, java.util.List<String>> headers = null;
        java.util.List<java.net.HttpCookie> cookies = null;
        String host = null;
        boolean secure = false;
        java.net.SocketAddress remoteSocketAddress = null;
        String userAgent = null;
        jakarta.servlet.http.HttpSession session = null;
        try {
            parameterMap = request.getParameterMap();
        } catch (final Throwable t) {
            log.warn("Cannot read parameter map from upgrade request", t);
        }
        try {
            headers = request.getHeaders();
        } catch (final Throwable t) {
            log.warn("Cannot read headers from upgrade request", t);
        }
        try {
            cookies = request.getCookies();
        } catch (final Throwable t) {
            log.warn("Cannot read cookies from upgrade request", t);
        }
        try {
            host = request.getHost();
        } catch (final Throwable t) {
            log.warn("Cannot read host from upgrade request", t);
        }
        try {
            secure = request.isSecure();
        } catch (final Throwable t) {
            log.warn("Cannot read secure flag from upgrade request", t);
        }
        try {
            remoteSocketAddress = request.getRemoteSocketAddress();
        } catch (final Throwable t) {
            log.warn("Cannot read remote socket address from upgrade request", t);
        }
        try {
            userAgent = request.getHeader("User-Agent");
        } catch (final Throwable t) {
            log.warn("Cannot read User-Agent from upgrade request", t);
        }
        try {
            // Force session creation BEFORE snapshotting: JettyServerUpgradeRequest.getSession() behaves
            // like getSession(false) and returns null when no HTTP session pre-exists. configureWithSession
            // (called later, only when a SessionCookieConfig is present) also forces creation, but the
            // snapshot must hold the real session for the whole WS lifetime, so create it here too.
            request.getHttpServletRequest().getSession(true);
            session = (jakarta.servlet.http.HttpSession) request.getSession();
        } catch (final Throwable t) {
            log.warn("Cannot read session from upgrade request", t);
        }
        return new UpgradeRequestData(parameterMap, headers, cookies, host, secure, remoteSocketAddress,
                userAgent, session);
    }

    protected void configureWithSession(final JettyServerUpgradeRequest request, final TxnContext context) {
        // Force session creation if there is no session
        request.getHttpServletRequest().getSession(true);
        final HttpSession httpSession = (HttpSession) request.getSession();
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

}
