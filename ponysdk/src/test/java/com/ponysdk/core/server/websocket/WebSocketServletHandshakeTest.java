/*
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

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;

import org.eclipse.jetty.ee11.servlet.FilterHolder;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.servlet.SessionManager;

/**
 * {@link WebSocketServlet#captureUpgradeData} against a real Jetty 12 handshake. Jetty recycles the servlet request
 * once the upgrade completes, so the data is read from the {@link UIContext} after the socket is open, never from the
 * live request: a mocked request passes whatever the snapshot code does.
 */
public class WebSocketServletHandshakeTest {

    private Server server;
    private WebSocket client;
    // Completed by the close frame the server sends back: the server has handled the close when it arrives
    private final java.util.Map<WebSocket, CompletableFuture<Void>> closedByServer = new java.util.concurrent.ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    // Session lookups made on the upgrade request: JettyServerUpgradeRequest reads the request the filter wraps
    private final AtomicInteger sessionLookups = new AtomicInteger();

    @BeforeEach
    public void setUp() {
        server = new Server();
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Close the way a browser does: abort() makes the server log an ERROR with a ClosedChannelException stack.
        // HttpClient has no close() before JDK 21 (CI runs 17): stopping its executor is what can be released here.
        try {
            if (client != null && !client.isOutputClosed()) {
                client.sendClose(WebSocket.NORMAL_CLOSURE, "test done").get(5, TimeUnit.SECONDS);
                // Stopping the server before it handled the close makes it log an ERROR with a ClosedChannelException
                closedByServer.computeIfAbsent(client, socket -> new CompletableFuture<>()).get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            server.stop();
        }
    }

    @Test
    public void testUpgradeDataIsCapturedFromARealHandshake() throws Exception {
        final UIContext uiContext = connect(true, "/?token=abc&token=def", "X-Test", "value-1");

        assertEquals(List.of("abc", "def"), uiContext.getParameterMap().get("token"));
        assertEquals("value-1", uiContext.getHeader("x-test"));
        assertEquals(1, uiContext.getRequestCookies().stream().filter(c -> "pony".equals(c.getName())).count());
        assertEquals("cookie-1", uiContext.getRequestCookies().stream().filter(c -> "pony".equals(c.getName()))
            .findFirst().orElseThrow().getValue());
        assertNotNull(uiContext.getSession(), "the session is created before it is read");
        assertEquals(1, sessionLookups.get(), "one session creation per handshake");
        assertEquals(uiContext.getSession().getId(), uiContext.getApplicationId(),
            "the Application is keyed on the session of the snapshot");
        assertEquals("127.0.0.1", uiContext.getHost());
        assertFalse(uiContext.isSecure());
        assertNotNull(uiContext.getRemoteSocketAddress());
    }

    @Test
    public void testASessionTheSnapshotCannotCreateIsTakenFromTheRequest() throws Exception {
        failSessionCreation = true;

        final UIContext uiContext = connect(true, "/", "X-Test", "value-1");

        assertNull(uiContext.getSession(), "the snapshot holds no session");
        assertEquals(1, sessionCreations.get(), "the request creates the one the Application is keyed on");
        assertNotNull(SessionManager.get().getApplication(uiContext.getApplicationId()));
    }

    @Test
    public void testHandshakeWithoutSessionHandlerStillOpens() throws Exception {
        final UIContext uiContext = connect(false, "/?token=abc", "X-Test", "value-1");

        assertEquals(List.of("abc"), uiContext.getParameterMap().get("token"));
        assertEquals("value-1", uiContext.getHeader("X-Test"));
        assertNull(uiContext.getSession(), "no SessionHandler, so no session");
        // Without the guard getSession(true) throws "No SessionManager", which the snapshot only logs
        assertEquals(0, sessionLookups.get(), "no session lookup without a SessionHandler");
    }

    /** An embedded terminal (ADR-0009): "ponysdk" accepted, keys made, the credential readable, no key for a page. */
    @Test
    public void testAnEmbeddedTerminalGetsKeysAndItsCredential() throws Exception {
        start(true);
        final String credential = Base64.getUrlEncoder().withoutPadding().encodeToString("jwt-é".getBytes(StandardCharsets.UTF_8));
        client = open(EMBEDDED, true, Subprotocols.PONYSDK, "ponysdk.credential." + credential);
        final UIContext uiContext = started(1);

        assertEquals(Subprotocols.PONYSDK, client.getSubprotocol(), "accepted, or the browser fails the upgrade");
        assertNotNull(uiContext.getKeys());
        assertEquals("jwt-é", Subprotocols.credential(uiContext.getHeader(Subprotocols.HEADER)));
        assertSame(uiContext, SessionManager.get().getUIContextByKey(uiContext.getID(),
            uiContext.getKeys().encoded().substring(0, uiContext.getKeys().encoded().indexOf('.'))));
    }

    @Test
    public void testAPageHasNoKeysAndNoSubprotocol() throws Exception {
        final UIContext uiContext = connect(true, "/?token=abc", "X-Test", "value-1");
        assertNull(uiContext.getKeys());
        assertEquals("", client.getSubprotocol());
    }

    /**
     * A page whose link carries the host's attributes is still a page: only a {@code <pony-app>} element offers
     * "ponysdk", which a link cannot add. Otherwise the link would make the page's application embedded, with host
     * attributes of the link's author.
     */
    @Test
    public void testAPageWhoseLinkCarriesHostAttributesIsNotEmbedded() throws Exception {
        final UIContext uiContext = connect(true, "/?y=%7B%22role%22%3A%22admin%22%7D", "X-Test", "value-1");
        assertNull(uiContext.getKeys());
        assertFalse(uiContext.isEmbedded());
    }

    /**
     * Another site sends no cookie: the reconnection proves its context by key, and the upgrade creates no session,
     * which no context would ever invalidate (ADR-0009).
     */
    @Test
    public void testAReconnectionByKeyWithoutCookieResumesTheContextAndCreatesNoSession() throws Exception {
        start(true);
        final WebSocket first = open(EMBEDDED, false, Subprotocols.PONYSDK);
        final UIContext uiContext = started(1);
        final String encoded = uiContext.getKeys().encoded();
        first.sendClose(4000, "network drop").get(5, TimeUnit.SECONDS);
        final long deadline = System.currentTimeMillis() + 5000;
        while (!uiContext.isSuspended() && System.currentTimeMillis() < deadline) Thread.sleep(20);
        assertTrue(uiContext.isSuspended(), "suspended, waiting for its terminal");
        final int applications = SessionManager.get().getApplications().size();
        sessionCreations.set(0);

        client = open(EMBEDDED + "&Y=" + uiContext.getID(), false, Subprotocols.PONYSDK,
            "ponysdk.key." + encoded.substring(0, encoded.indexOf('.')));
        final long resumed = System.currentTimeMillis() + 5000;
        while (uiContext.isSuspended() && System.currentTimeMillis() < resumed) Thread.sleep(20);

        assertFalse(uiContext.isSuspended(), "resumed by its key");
        assertTrue(uiContext.isAlive());
        Mockito.verify(applicationManager, Mockito.times(1)).startApplication(Mockito.any());
        assertEquals(0, sessionCreations.get(), "no session for a reconnection by key");
        assertEquals(applications, SessionManager.get().getApplications().size(), "no Application left without context");
    }

    @Test
    public void testAWrongKeyResumesNothing() throws Exception {
        start(true);
        final WebSocket first = open(EMBEDDED, false, Subprotocols.PONYSDK);
        final UIContext uiContext = started(1);
        first.sendClose(4000, "network drop").get(5, TimeUnit.SECONDS);
        final long deadline = System.currentTimeMillis() + 5000;
        while (!uiContext.isSuspended() && System.currentTimeMillis() < deadline) Thread.sleep(20);

        client = open(EMBEDDED + "&Y=" + uiContext.getID(), false, Subprotocols.PONYSDK, "ponysdk.key.AAAAAAAAAAAAAAAAAAAAAA");
        Thread.sleep(300);
        assertTrue(uiContext.isSuspended(), "still waiting for its own terminal");
        Mockito.verify(applicationManager, Mockito.times(1)).startApplication(Mockito.any());
    }

    /** The handshake of an embedded terminal: the host's attributes, no protocol version (version 1 is enough). */
    private static final String EMBEDDED = "/?y=%7B%7D";

    private ApplicationManager applicationManager;
    private ServerConnector connector;
    private final AtomicInteger sessionCreations = new AtomicInteger();
    private volatile boolean failSessionCreation;

    private UIContext connect(final boolean withSessions, final String path, final String header,
                              final String headerValue) throws Exception {
        start(withSessions);
        client = open(path, true, header, headerValue, new String[0]);
        return started(1);
    }

    private void start(final boolean withSessions) throws Exception {
        applicationManager = Mockito.mock(ApplicationManager.class);
        final ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        configuration.setReconnectionTimeoutMs(10_000);
        Mockito.when(applicationManager.getConfiguration()).thenReturn(configuration);

        final ServletContextHandler context = new ServletContextHandler(withSessions
                ? ServletContextHandler.SESSIONS : ServletContextHandler.NO_SESSIONS);
        context.addFilter(new FilterHolder((Filter) (request, response, chain) -> chain.doFilter(
            new HttpServletRequestWrapper((HttpServletRequest) request) {

                @Override
                public HttpSession getSession(final boolean create) {
                    sessionLookups.incrementAndGet();
                    if (create && failSessionCreation) {
                        failSessionCreation = false;
                        throw new IllegalStateException("session store unavailable");
                    }
                    final HttpSession session = super.getSession(create);
                    if (session != null && session.isNew()) sessionCreations.incrementAndGet();
                    return session;
                }

                @Override
                public HttpSession getSession() {
                    sessionLookups.incrementAndGet();
                    final HttpSession session = super.getSession();
                    if (session != null && session.isNew()) sessionCreations.incrementAndGet();
                    return session;
                }
            }, response)), "/*", EnumSet.of(DispatcherType.REQUEST));
        context.addServlet(new ServletHolder(new WebSocketServlet(applicationManager)), "/*");
        JettyWebSocketServletContainerInitializer.configure(context, null);
        connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        server.addConnector(connector);
        server.setHandler(context);
        server.start();
    }

    private WebSocket open(final String path, final boolean cookie, final String... subprotocols) throws Exception {
        return open(path, cookie, "X-Test", "value", (String[]) subprotocols);
    }

    private WebSocket open(final String path, final boolean cookie, final String header, final String headerValue,
                           final String[] subprotocols) throws Exception {
        WebSocket.Builder builder = HttpClient.newBuilder().executor(executor).build().newWebSocketBuilder()
            .header(header, headerValue);
        if (cookie) builder = builder.header("Cookie", "pony=cookie-1");
        if (subprotocols.length > 0) {
            builder = builder.subprotocols(subprotocols[0], java.util.Arrays.copyOfRange(subprotocols, 1, subprotocols.length));
        }
        return builder.buildAsync(URI.create("ws://127.0.0.1:" + connector.getLocalPort() + path), new WebSocket.Listener() {

            @Override
            public CompletionStage<?> onClose(final WebSocket webSocket, final int statusCode, final String reason) {
                closedByServer.computeIfAbsent(webSocket, socket -> new CompletableFuture<>()).complete(null);
                return null;
            }
        }).get(10, TimeUnit.SECONDS);
    }

    /** The context of the n-th application started. */
    private UIContext started(final int count) throws Exception {
        final ArgumentCaptor<UIContext> captor = ArgumentCaptor.forClass(UIContext.class);
        Mockito.verify(applicationManager, Mockito.timeout(10_000).times(count)).startApplication(captor.capture());
        return captor.getAllValues().get(count - 1);
    }
}
