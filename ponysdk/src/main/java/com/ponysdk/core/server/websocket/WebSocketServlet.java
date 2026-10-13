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

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
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
        final UpgradeRequestData upgradeRequestData = captureUpgradeData(request);
        webSocket.setUpgradeRequestData(upgradeRequestData);
        webSocket.setApplicationManager(applicationManager);
        webSocket.setMonitor(monitor);

        final TxnContext context = new TxnContext(webSocket);
        webSocket.setContext(context);

        if (hasSessionHandler(request)) {
            // The Application is keyed on the session of the snapshot, the one UIContext.getSession() serves. Asking the
            // request again could create another one if the first was invalidated in between (a page reload closing the
            // previous socket). Only an override of captureUpgradeData that did not create it falls back to the request.
            final HttpSession session = upgradeRequestData.getSession() != null ? upgradeRequestData.getSession()
                    : (HttpSession) request.getSession();
            configureWithSession(session, context);
        }

        return webSocket;
    }

    /**
     * Snapshots everything the application reads from the upgrade request while it is still live, and creates the
     * HTTP session that the snapshot holds when the servlet context has a SessionHandler. Protected so that a subclass
     * can adjust the snapshot: an override should start from {@code super.captureUpgradeData(request)}, the step that
     * creates the session.
     * <p>
     * Do not throw to reject a client: Jetty answers a generic 500 ("Could not create WebSocket endpoint") without
     * logging the cause, and the session created here is left behind. {@link UpgradeRequestData} has a fixed set of
     * fields and nothing carries other handshake data per connection (the Application, hence the TxnContext
     * attributes, is shared by every tab of the HTTP session): a server that needs another field must add it to
     * {@link UpgradeRequestData}.
     */
    protected UpgradeRequestData captureUpgradeData(final JettyServerUpgradeRequest request) {
        java.util.Map<String, java.util.List<String>> parameterMap = null;
        java.util.Map<String, java.util.List<String>> headers = null;
        java.util.List<java.net.HttpCookie> cookies = null;
        String host = null;
        boolean secure = false;
        java.net.SocketAddress remoteSocketAddress = null;
        String userAgent = null;
        HttpSession session = null;
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
            // Create the session here, so that the snapshot holds it for the whole WebSocket lifetime and
            // createWebsocket keys the Application on it. Asked for explicitly with getSession(true) instead
            // of through JettyServerUpgradeRequest.getSession(), whose create-or-not behaviour is Jetty's
            // (12.1.13 delegates to HttpServletRequest.getSession(), which creates). A context without a
            // SessionHandler has no session to create: getSession(true) would only throw "No SessionManager".
            if (hasSessionHandler(request)) {
                session = request.getHttpServletRequest().getSession(true);
            }
        } catch (final Throwable t) {
            log.warn("Cannot read session from upgrade request", t);
        }
        return new UpgradeRequestData(parameterMap, headers, cookies, host, secure, remoteSocketAddress,
                userAgent, session);
    }

    private static boolean hasSessionHandler(final JettyServerUpgradeRequest request) {
        final ServletContextHandler contextHandler = ServletContextHandler
            .getServletContextHandler(request.getHttpServletRequest().getServletContext());
        return contextHandler != null && contextHandler.getSessionHandler() != null;
    }

    protected void configureWithSession(final HttpSession httpSession, final TxnContext context) {
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
