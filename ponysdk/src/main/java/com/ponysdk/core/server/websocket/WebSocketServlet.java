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
import java.util.Set;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
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
        final ApplicationConfiguration config = applicationManager.getConfiguration();

        // #3 Idle timeout — configurable (falls back to the legacy field if unset).
        final long idleMs = config != null && config.getWsIdleTimeoutMs() > 0 ? config.getWsIdleTimeoutMs() : maxIdleTime;
        factory.setIdleTimeout(Duration.ofMillis(idleMs));

        // #2 Bound inbound (client -> server) text messages to guard against memory abuse.
        final int maxInbound = config != null ? config.getWsMaxInboundMessageSize() : 0;
        if (maxInbound > 0) factory.setMaxTextMessageSize(maxInbound);

        // #5 permessage-deflate is negotiated and enabled by default in Jetty 12; PonySDK's binary
        // protocol + string dictionary already compress heavily, so deflate is the final wire layer.
        // The compression *level* is not exposed by this factory API, but it can be turned off per
        // connection via config.wsPermessageDeflateEnabled (see createWebsocket).
        factory.setCreator(this::createWebsocket);
    }

    protected WebSocket createWebsocket(final JettyServerUpgradeRequest request, final JettyServerUpgradeResponse response) {
        // #1 Anti-CSWSH: WebSocket upgrades are NOT subject to the same-origin policy, and the
        // browser attaches the session cookie regardless of the calling page's origin. Validate
        // the Origin before creating anything; reject cross-origin upgrades with a 403.
        final ApplicationConfiguration config = applicationManager.getConfiguration();
        if (config != null && config.isWsOriginCheckEnabled()) {
            final String origin = request.getHeader("Origin");
            // Prefer X-Forwarded-Host (set by reverse proxies to the host the browser used);
            // browsers cannot set this header on a WS handshake, so it is safe for this check.
            String host = request.getHeader("X-Forwarded-Host");
            if (host == null || host.isEmpty()) host = request.getHeader("Host");
            else {
                final int comma = host.indexOf(',');
                if (comma >= 0) host = host.substring(0, comma).trim();
            }
            if (!isOriginAllowed(origin, host, config.getWsAllowedOrigins())) {
                log.warn("Rejected WebSocket upgrade from a disallowed Origin");
                response.setStatusCode(HttpServletResponse.SC_FORBIDDEN);
                return null;
            }
        }

        // Optionally disable permessage-deflate (frame compression) for this connection. Useful for
        // CPU/latency-sensitive deployments, and required by clients that cannot decode compressed
        // frames (e.g. the headless Pony driver).
        if (config != null && !config.isWsPermessageDeflateEnabled()) {
            response.setExtensions(java.util.List.of());
        }

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

        // Transparent reconnection: the client may request resuming a suspended UIContext by id.
        // The resume is authorized strictly by the caller's HTTP session (see
        // WebSocket.onWebSocketOpen -> Application.getUIContext), so the id alone grants nothing.
        // It travels as a query parameter because browsers cannot set custom headers on the
        // WebSocket handshake; moving it off the URL would require a transport change
        // (subprotocol or post-open handshake) on the GWT terminal.
        final String reconnectIdParam = request.getHttpServletRequest()
                .getParameter(ClientToServerModel.RECONNECT_UI_CONTEXT_ID.toStringValue());
        if (reconnectIdParam != null && applicationManager.getConfiguration().getReconnectionTimeoutMs() > 0) {
            try {
                final int uiContextId = Integer.parseInt(reconnectIdParam);
                webSocket.setReconnectContextId(uiContextId);
            } catch (final NumberFormatException e) {
                // Do not echo the raw client-supplied value into the logs (log-injection vector).
                log.warn("Ignoring malformed reconnect uiContextId parameter");
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

    /**
     * CSWSH guard. Returns {@code true} if the upgrade Origin is acceptable:
     * <ul>
     *   <li>no/empty Origin header (non-browser client — no ambient cookies to abuse) → allowed;</li>
     *   <li>Origin explicitly present in {@code allowedOrigins} → allowed;</li>
     *   <li>otherwise same-origin only: the Origin authority (host[:port]) must equal {@code host}.</li>
     * </ul>
     */
    static boolean isOriginAllowed(final String origin, final String host, final Set<String> allowedOrigins) {
        if (origin == null || origin.isEmpty()) return true;
        if (allowedOrigins != null && allowedOrigins.contains(origin)) return true;
        final String authority = originAuthority(origin);
        return authority != null && host != null && authority.equalsIgnoreCase(host);
    }

    /** Extracts {@code host[:port]} from an Origin such as {@code "https://app.example.com:8443"}. */
    private static String originAuthority(final String origin) {
        final int scheme = origin.indexOf("://");
        if (scheme < 0) return null;
        final String rest = origin.substring(scheme + 3);
        final int slash = rest.indexOf('/');
        return slash >= 0 ? rest.substring(0, slash) : rest;
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
