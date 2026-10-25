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

        // Jetty allocates a text builder of inputBufferSize for every incoming message: keep it small
        // for PonySDK's small upstream messages (see ApplicationConfiguration#setWsInputBufferSize).
        final int inputBuffer = config != null ? config.getWsInputBufferSize() : 0;
        if (inputBuffer > 0) factory.setInputBufferSize(inputBuffer);

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

        // An embedded terminal offers the subprotocol "ponysdk" (ADR-0009): a browser that offers some fails an upgrade
        // that accepts none
        final String protocols = request.getHeader(Subprotocols.HEADER);
        if (Subprotocols.offered(protocols)) response.setAcceptedSubProtocol(Subprotocols.PONYSDK);
        final String reconnectKey = Subprotocols.key(protocols);

        // Optionally disable permessage-deflate (frame compression) for this connection. Useful for
        // CPU/latency-sensitive deployments, and required by clients that cannot decode compressed
        // frames (e.g. the headless Pony driver).
        if (config != null && !config.isWsPermessageDeflateEnabled()) {
            response.setExtensions(java.util.List.of());
        }

        final WebSocket webSocket = new WebSocket();
        // Capture request-derived data NOW, while the upgrade request is still live. Jetty 12 recycles
        // the underlying servlet request once the upgrade completes, so any later read (parameter map,
        // headers, session) throws NPE or returns another request's data. UIContext serves this snapshot.
        final UpgradeRequestData upgradeRequestData = captureUpgradeData(request);
        webSocket.setUpgradeRequestData(upgradeRequestData);
        webSocket.setApplicationManager(applicationManager);
        webSocket.setMonitor(monitor);
        webSocket.setMetrics(metrics);

        // Note: this TxnContext is orphaned on successful reconnection — the resumed UIContext
        // keeps its original TxnContext. The orphan has no strong refs from long-lived objects
        // and will be GC'd normally. Creating it unconditionally keeps the code simple.
        final TxnContext context = new TxnContext(webSocket);
        webSocket.setContext(context);

        // Transparent reconnection: the client may request resuming a suspended UIContext by id.
        // The resume is authorized by the caller's HTTP session (see WebSocket.onWebSocketOpen ->
        // Application.getUIContext), or for an embedded terminal by its context's key, offered as a
        // subprotocol, out of the URL (ADR-0009): the id alone grants nothing.
        webSocket.setProtocolVersion(negotiateProtocolVersion(
                request.getHttpServletRequest().getParameter(ClientToServerModel.PROTOCOL_VERSION.toStringValue()), config));

        final String reconnectIdParam = request.getHttpServletRequest()
                .getParameter(ClientToServerModel.RECONNECT_UI_CONTEXT_ID.toStringValue());
        if (reconnectIdParam != null && applicationManager.getConfiguration().getReconnectionTimeoutMs() > 0) {
            try {
                final int uiContextId = Integer.parseInt(reconnectIdParam);
                webSocket.setReconnectContextId(uiContextId);
                webSocket.setReconnectKey(reconnectKey);
            } catch (final NumberFormatException e) {
                // Do not echo the raw client-supplied value into the logs (log-injection vector).
                log.warn("Ignoring malformed reconnect uiContextId parameter");
            }
        }

        if (hasSessionHandler(request)) {
            // The Application is keyed on the session of the snapshot, the one UIContext.getSession() serves. Asking the
            // request again could create another one if the first was invalidated in between (a page reload closing the
            // previous socket). Only an override of captureUpgradeData that did not create it falls back to the request.
            // A reconnection by key without the cookie has none, and needs none: its context keeps its own (ADR-0009)
            final HttpSession session = upgradeRequestData.getSession() != null ? upgradeRequestData.getSession()
                    : reconnectKey != null ? null : (HttpSession) request.getSession();
            if (session != null) configureWithSession(session, context);
        }

        return webSocket;
    }

    /**
     * Snapshots everything the application reads from the upgrade request while it is still live, and creates the
     * HTTP session that the snapshot holds when the servlet context has a SessionHandler, unless the upgrade is a
     * reconnection by key that sends no cookie: that session would be left without a context (ADR-0009). Protected so
     * that a subclass
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
                session = request.getHttpServletRequest().getSession(Subprotocols.key(request.getHeader(Subprotocols.HEADER)) == null);
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

    /**
     * min(version announced by the terminal, configured maximum); version 1 when the parameter is absent
     * (terminals older than the negotiation) or malformed.
     */
    static int negotiateProtocolVersion(final String announced, final ApplicationConfiguration config) {
        int version = com.ponysdk.core.model.ProtocolVersion.V1;
        if (announced != null) {
            try {
                version = Math.max(com.ponysdk.core.model.ProtocolVersion.V1, Integer.parseInt(announced));
            } catch (final NumberFormatException e) {
                version = com.ponysdk.core.model.ProtocolVersion.V1;
            }
        }
        final int max = config != null ? config.getMaxProtocolVersion() : com.ponysdk.core.model.ProtocolVersion.CURRENT;
        return Math.min(version, Math.min(max, com.ponysdk.core.model.ProtocolVersion.CURRENT));
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
