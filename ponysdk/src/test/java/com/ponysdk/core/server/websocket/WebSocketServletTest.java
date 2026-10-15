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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.eclipse.jetty.ee11.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee11.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.ee11.websocket.server.JettyWebSocketServletFactory;
import org.junit.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.servlet.SessionManager;

/**
 * Tests the anti-CSWSH Origin validation logic of {@link WebSocketServlet#isOriginAllowed}.
 */
public class WebSocketServletTest {

    @Test
    public void missingOriginIsAllowed_nonBrowserClient() {
        // Non-browser clients (e.g. the Pony driver, tests) send no Origin and carry no ambient cookies.
        assertTrue(WebSocketServlet.isOriginAllowed(null, "app.example.com", null));
        assertTrue(WebSocketServlet.isOriginAllowed("", "app.example.com", null));
    }

    @Test
    public void sameOriginIsAllowed() {
        assertTrue(WebSocketServlet.isOriginAllowed("https://app.example.com", "app.example.com", null));
        assertTrue(WebSocketServlet.isOriginAllowed("https://app.example.com:8443", "app.example.com:8443", null));
        assertTrue(WebSocketServlet.isOriginAllowed("http://localhost:8081", "localhost:8081", null));
    }

    @Test
    public void crossOriginIsRejected() {
        assertFalse(WebSocketServlet.isOriginAllowed("https://evil.example.com", "app.example.com", null));
        assertFalse("different port is a different origin",
            WebSocketServlet.isOriginAllowed("https://app.example.com:9999", "app.example.com:8443", null));
    }

    @Test
    public void allowListPermitsExtraOrigins() {
        final Set<String> allow = Set.of("https://trusted.partner.com");
        assertTrue(WebSocketServlet.isOriginAllowed("https://trusted.partner.com", "app.example.com", allow));
        // Still rejects an origin that is neither same-origin nor allow-listed
        assertFalse(WebSocketServlet.isOriginAllowed("https://evil.example.com", "app.example.com", allow));
    }

    @Test
    public void malformedOriginOrMissingHostIsRejected() {
        assertFalse("no scheme → cannot verify", WebSocketServlet.isOriginAllowed("app.example.com", "app.example.com", null));
        assertFalse("null host → cannot verify", WebSocketServlet.isOriginAllowed("https://app.example.com", null, null));
    }

    // ── configure() / createWebsocket() / configureWithSession() ──

    /** Builds a WebSocketServlet plus a fully-stubbed upgrade request/response graph with sane defaults. */
    private static final class Fixture {
        final ApplicationConfiguration config = mock(ApplicationConfiguration.class);
        final ApplicationManager appManager = mock(ApplicationManager.class);
        final WebSocketServlet servlet;
        final JettyServerUpgradeRequest request = mock(JettyServerUpgradeRequest.class);
        final JettyServerUpgradeResponse response = mock(JettyServerUpgradeResponse.class);
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        final ServletContext servletContext = mock(ServletContext.class);
        final HttpSession httpSession = mock(HttpSession.class);
        final String sessionId = "sess-" + UUID.randomUUID();

        Fixture() {
            when(appManager.getConfiguration()).thenReturn(config);
            servlet = new WebSocketServlet(appManager);
            // Default config: no origin check, deflate on, no reconnection.
            when(config.isWsOriginCheckEnabled()).thenReturn(false);
            when(config.isWsPermessageDeflateEnabled()).thenReturn(true);
            when(config.getReconnectionTimeoutMs()).thenReturn(0L);
            when(config.getWsAllowedOrigins()).thenReturn(null);
            // Request graph used by WebSocket.setRequest + createWebsocket.
            when(request.getParameterMap()).thenReturn(Map.of());
            when(request.getHeader("User-Agent")).thenReturn("test-agent");
            when(request.getHttpServletRequest()).thenReturn(httpRequest);
            when(httpRequest.getSession()).thenReturn(httpSession);
            when(httpRequest.getSession(true)).thenReturn(httpSession);
            when(httpRequest.getServletContext()).thenReturn(servletContext);
            when(servletContext.getSessionCookieConfig()).thenReturn(null); // skip session setup by default
            when(httpSession.getId()).thenReturn(sessionId);
        }
    }

    @Test
    public void createWebsocket_rejectsCrossOriginWith403AndReturnsNull() {
        final Fixture f = new Fixture();
        when(f.config.isWsOriginCheckEnabled()).thenReturn(true);
        when(f.request.getHeader("Origin")).thenReturn("https://evil.example.com");
        when(f.request.getHeader("Host")).thenReturn("app.example.com");

        assertNull(f.servlet.createWebsocket(f.request, f.response));
        verify(f.response).setStatusCode(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void createWebsocket_createsSocketWhenOriginCheckDisabled() {
        final Fixture f = new Fixture();

        final WebSocket ws = f.servlet.createWebsocket(f.request, f.response);

        assertNotNull(ws);
        verify(f.response, never()).setStatusCode(anyInt());
    }

    @Test
    public void createWebsocket_disablesPermessageDeflateWhenConfigured() {
        final Fixture f = new Fixture();
        when(f.config.isWsPermessageDeflateEnabled()).thenReturn(false);

        assertNotNull(f.servlet.createWebsocket(f.request, f.response));
        verify(f.response).setExtensions(List.of()); // deflate stripped
    }

    @Test
    public void createWebsocket_toleratesMalformedReconnectId() {
        final Fixture f = new Fixture();
        when(f.config.getReconnectionTimeoutMs()).thenReturn(5_000L);
        when(f.httpRequest.getParameter(ClientToServerModel.RECONNECT_UI_CONTEXT_ID.toStringValue()))
            .thenReturn("not-a-number");

        // Must not throw — the malformed value is caught and ignored.
        assertNotNull(f.servlet.createWebsocket(f.request, f.response));
    }

    @Test
    public void createWebsocket_acceptsValidReconnectId() {
        final Fixture f = new Fixture();
        when(f.config.getReconnectionTimeoutMs()).thenReturn(5_000L);
        when(f.httpRequest.getParameter(ClientToServerModel.RECONNECT_UI_CONTEXT_ID.toStringValue()))
            .thenReturn("42");

        assertNotNull(f.servlet.createWebsocket(f.request, f.response));
    }

    // Session creation and Application registration are covered against a real Jetty 12 handshake (with and
    // without a SessionHandler) by WebSocketServletHandshakeTest: the gate is the context's SessionHandler,
    // which a mocked ServletContext cannot provide.

    @Test
    public void configure_setsIdleTimeoutMaxInboundSizeAndCreator() {
        final Fixture f = new Fixture();
        when(f.config.getWsIdleTimeoutMs()).thenReturn(12_345L);
        when(f.config.getWsMaxInboundMessageSize()).thenReturn(1024);

        final JettyWebSocketServletFactory factory = mock(JettyWebSocketServletFactory.class);
        f.servlet.configure(factory);

        verify(factory).setIdleTimeout(Duration.ofMillis(12_345L));
        verify(factory).setMaxTextMessageSize(1024);
        verify(factory).setCreator(any());
    }

    @Test
    public void configure_appliesTheInputBufferSizeOnlyWhenPositive() {
        final Fixture f = new Fixture();
        when(f.config.getWsInputBufferSize()).thenReturn(1024);
        final JettyWebSocketServletFactory factory = mock(JettyWebSocketServletFactory.class);
        f.servlet.configure(factory);
        verify(factory).setInputBufferSize(1024);

        final Fixture g = new Fixture();
        when(g.config.getWsInputBufferSize()).thenReturn(0);
        final JettyWebSocketServletFactory factory2 = mock(JettyWebSocketServletFactory.class);
        g.servlet.configure(factory2);
        org.mockito.Mockito.verify(factory2, org.mockito.Mockito.never()).setInputBufferSize(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    public void protocolVersionIsNegotiatedDownToWhatBothSidesSupport() {
        final com.ponysdk.core.server.application.ApplicationConfiguration config =
                new com.ponysdk.core.server.application.ApplicationConfiguration();
        assertEquals("old terminal: no parameter", 1, WebSocketServlet.negotiateProtocolVersion(null, config));
        assertEquals(1, WebSocketServlet.negotiateProtocolVersion("garbage", config));
        assertEquals(1, WebSocketServlet.negotiateProtocolVersion("-3", config));
        assertEquals(2, WebSocketServlet.negotiateProtocolVersion("2", config));
        assertEquals("newer terminal", com.ponysdk.core.model.ProtocolVersion.CURRENT, WebSocketServlet.negotiateProtocolVersion("99", config));
        config.setMaxProtocolVersion(1);
        assertEquals("server kill switch", 1, WebSocketServlet.negotiateProtocolVersion("2", config));
    }
}
