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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.glassfish.tyrus.client.ClientManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.application.UIContext;

/** What becomes of a context when its socket closes with 1001, from the browser or from the server (ADR-0007). */
public class WebSocketCloseTest {

    private final ApplicationConfiguration configuration = new ApplicationConfiguration();
    private final ApplicationManager applicationManager = Mockito.mock(ApplicationManager.class);
    private final CountDownLatch started = new CountDownLatch(1);
    private final Server server = new Server();
    private final ClientManager client = ClientManager.createClient();

    @BeforeEach
    public void setUp() {
        configuration.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        configuration.setReconnectionTimeoutMs(10_000);
        Mockito.when(applicationManager.getConfiguration()).thenReturn(configuration);
        Mockito.doAnswer(invocation -> {
            started.countDown();
            return null;
        }).when(applicationManager).startApplication(Mockito.any());
    }

    @AfterEach
    public void tearDown() throws Exception {
        client.shutdown();
        server.stop();
    }

    @Test
    public void testABrowserLeavingItsPageClosesWith1001AndTheContextGoesAtOnce() throws Exception {
        final Session session = connect();
        final UIContext uiContext = startedContext();
        // What a browser sends for the sockets of a document it unloads (HTML, "make disappear")
        session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, ""));
        await(() -> !uiContext.isAlive());
        assertFalse(uiContext.isAlive(), "a page gone cannot reconnect: its context is destroyed");
        assertFalse(uiContext.isSuspended());
    }

    @Test
    public void testTheServersIdleTimeoutClosesWith1001AndTheContextWaitsForItsClient() throws Exception {
        configuration.setWsIdleTimeoutMs(300);
        connect();
        final UIContext uiContext = startedContext();
        await(uiContext::isSuspended);
        assertTrue(uiContext.isSuspended(), "the server closed the socket: its client may come back");
        assertTrue(uiContext.isAlive());
    }

    private Session connect() throws Exception {
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder(new WebSocketServlet(applicationManager)), "/*");
        JettyWebSocketServletContainerInitializer.configure(context, null);
        final ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        server.addConnector(connector);
        server.setHandler(context);
        server.start();
        return client.connectToServer(new Endpoint() {

            @Override
            public void onOpen(final Session session, final EndpointConfig config) {
            }
        }, ClientEndpointConfig.Builder.create().build(), URI.create("ws://127.0.0.1:" + connector.getLocalPort() + "/"));
    }

    private UIContext startedContext() throws InterruptedException {
        assertTrue(started.await(10, TimeUnit.SECONDS), "the application was not started");
        final ArgumentCaptor<UIContext> captor = ArgumentCaptor.forClass(UIContext.class);
        Mockito.verify(applicationManager).startApplication(captor.capture());
        return captor.getValue();
    }

    /** Polls for at most 5 s: the server handles the close on its own threads. */
    private static void await(final BooleanSupplier condition) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + 5_000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) Thread.sleep(10);
    }
}
