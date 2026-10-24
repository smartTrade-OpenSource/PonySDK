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
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
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

/**
 * {@link WebSocketServlet#captureUpgradeData} against a real Jetty 12 handshake. Jetty recycles the servlet request
 * once the upgrade completes, so the data is read from the {@link UIContext} after the socket is open, never from the
 * live request: a mocked request passes whatever the snapshot code does.
 */
public class WebSocketServletHandshakeTest {

    private Server server;
    private WebSocket client;
    // Completed by the close frame the server sends back: the server has handled the close when it arrives
    private final CompletableFuture<Void> closedByServer = new CompletableFuture<>();
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
                closedByServer.get(5, TimeUnit.SECONDS);
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
    public void testHandshakeWithoutSessionHandlerStillOpens() throws Exception {
        final UIContext uiContext = connect(false, "/?token=abc", "X-Test", "value-1");

        assertEquals(List.of("abc"), uiContext.getParameterMap().get("token"));
        assertEquals("value-1", uiContext.getHeader("X-Test"));
        assertNull(uiContext.getSession(), "no SessionHandler, so no session");
        // Without the guard getSession(true) throws "No SessionManager", which the snapshot only logs
        assertEquals(0, sessionLookups.get(), "no session lookup without a SessionHandler");
    }

    private UIContext connect(final boolean withSessions, final String path, final String header,
                              final String headerValue) throws Exception {
        final ApplicationManager applicationManager = Mockito.mock(ApplicationManager.class);
        final ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        Mockito.when(applicationManager.getConfiguration()).thenReturn(configuration);
        final CountDownLatch started = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            started.countDown();
            return null;
        }).when(applicationManager).startApplication(Mockito.any());

        final ServletContextHandler context = new ServletContextHandler(withSessions
                ? ServletContextHandler.SESSIONS : ServletContextHandler.NO_SESSIONS);
        context.addFilter(new FilterHolder((Filter) (request, response, chain) -> chain.doFilter(
            new HttpServletRequestWrapper((HttpServletRequest) request) {

                @Override
                public HttpSession getSession(final boolean create) {
                    sessionLookups.incrementAndGet();
                    return super.getSession(create);
                }

                @Override
                public HttpSession getSession() {
                    sessionLookups.incrementAndGet();
                    return super.getSession();
                }
            }, response)), "/*", EnumSet.of(DispatcherType.REQUEST));
        context.addServlet(new ServletHolder(new WebSocketServlet(applicationManager)), "/*");
        JettyWebSocketServletContainerInitializer.configure(context, null);
        final ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        server.addConnector(connector);
        server.setHandler(context);
        server.start();

        final CompletableFuture<WebSocket> opened = HttpClient.newBuilder().executor(executor).build().newWebSocketBuilder()
            .header(header, headerValue)
            .header("Cookie", "pony=cookie-1")
            .buildAsync(URI.create("ws://127.0.0.1:" + connector.getLocalPort() + path), new WebSocket.Listener() {

                @Override
                public CompletionStage<?> onClose(final WebSocket webSocket, final int statusCode, final String reason) {
                    closedByServer.complete(null);
                    return null;
                }
            });
        client = opened.get(10, TimeUnit.SECONDS);
        assertTrue(started.await(10, TimeUnit.SECONDS), "the application was not started");

        final ArgumentCaptor<UIContext> captor = ArgumentCaptor.forClass(UIContext.class);
        Mockito.verify(applicationManager).startApplication(captor.capture());
        return captor.getValue();
    }
}
