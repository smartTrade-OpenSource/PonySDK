/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.impl.main;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.openqa.selenium.By;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.servlet.SessionManager;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.main.EntryPoint;
import com.ponysdk.driver.PonyFrame;
import com.ponysdk.driver.PonyMessageListener;
import com.ponysdk.driver.PonySDKWebDriver;
import com.ponysdk.driver.PonySessionListener;
import com.ponysdk.driver.PonyWebElement;
import com.ponysdk.impl.java.server.JavaApplicationManager;

import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;

/**
 * The server started for real on an ephemeral loopback port, its application driven over the WebSocket by the
 * headless {@link PonySDKWebDriver}: the launcher's wiring end to end.
 */
public class PonySDKServerRunTest {

    public static final class HelloEntryPoint implements EntryPoint {

        @Override
        public void start(final UIContext uiContext) {
            final PLabel label = Element.newPLabel("Hello");
            final PButton button = Element.newPButton("Go");
            button.addClickHandler(event -> label.setText("Clicked"));
            PWindow.getMain().add(label);
            PWindow.getMain().add(button);
        }
    }

    private static PonySDKServer server() {
        final ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setApplicationContextName("app");
        configuration.setEntryPointClass(HelloEntryPoint.class);
        final JavaApplicationManager manager = new JavaApplicationManager();
        manager.setConfiguration(configuration);
        final PonySDKServer server = new PonySDKServer();
        server.setApplicationManager(manager);
        server.setHost("127.0.0.1");
        server.setPort(0);
        server.setUseSSL(false);
        server.setSessionCookieSecure(false);
        return server;
    }

    private static int port(final PonySDKServer server) {
        return ((ServerConnector) server.server.getConnectors()[0]).getLocalPort();
    }

    /** Counts down once a received message carries {@code text}. */
    private static PonyMessageListener waitingFor(final String text, final CountDownLatch latch) {
        return new PonyMessageListener() {

            @Override
            public void onSendMessage(final JsonObject message) {
            }

            @Override
            public void onReceiveMessage(final List<PonyFrame> message) {
                if (text.equals(PonySDKWebDriver.findValueForModel(message, ServerToClientModel.TEXT))) latch.countDown();
            }
        };
    }

    @Test
    public void servesAnApplicationTheDriverDrives() throws Exception {
        final PonySDKServer server = server();
        server.start();
        try {
            final int port = port(server);
            assertSame(server.getApplicationConfiguration(), server.applicationManager.getConfiguration());

            try (HttpClient http = HttpClient.newHttpClient()) {
                final HttpResponse<String> page = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/app/")).build(),
                    HttpResponse.BodyHandlers.ofString());
                assertEquals(200, page.statusCode());
            }

            final CountDownLatch opened = new CountDownLatch(1);
            final CountDownLatch closed = new CountDownLatch(1);
            final CountDownLatch hello = new CountDownLatch(1);
            final CountDownLatch clicked = new CountDownLatch(1);
            final PonyMessageListener helloListener = waitingFor("Go", hello); // the button, created after the label
            final PonyMessageListener clickedListener = waitingFor("Clicked", clicked);
            final PonySDKWebDriver driver = new PonySDKWebDriver(new PonyMessageListener() {

                @Override
                public void onSendMessage(final JsonObject message) {
                }

                @Override
                public void onReceiveMessage(final List<PonyFrame> message) {
                    helloListener.onReceiveMessage(message);
                    clickedListener.onReceiveMessage(message);
                }
            }, null, new PonySessionListener() {

                @Override
                public void onOpen(final Session session) {
                    opened.countDown();
                }

                @Override
                public void onClose(final Session session, final CloseReason closeReason) {
                    closed.countDown();
                }

                @Override
                public void onError(final Session session, final Throwable thr) {
                }
            }, true);

            final String url = "ws://127.0.0.1:" + port + "/app/ws";
            driver.get(url);
            assertTrue(opened.await(10, TimeUnit.SECONDS));
            assertEquals(url, driver.getCurrentUrl());
            assertNotNull(driver.getSessionId());
            assertTrue(hello.await(10, TimeUnit.SECONDS), "the application started and sent its widgets");
            assertTrue(driver.getContextId() > 0);

            final PonyWebElement label = driver.findElement(By.tagName("LABEL"));
            assertEquals("Hello", label.getText());
            driver.findElement(By.tagName("BUTTON")).click();
            assertTrue(clicked.await(10, TimeUnit.SECONDS), "the click ran on the server");
            assertEquals("Clicked", label.getText());

            driver.close();
            assertTrue(closed.await(10, TimeUnit.SECONDS));
        } finally {
            server.stop();
        }
    }

    @Test
    public void withADrainTimeTheLivePagesAreToldToMoveBeforeTheServerStops() throws Exception {
        final PonySDKServer server = server();
        server.setDrainMillis(2_000);
        server.start();
        try {
            final CountDownLatch hello = new CountDownLatch(1);
            final CountDownLatch destroyed = new CountDownLatch(1);
            final CountDownLatch closed = new CountDownLatch(1);
            final PonyMessageListener helloListener = waitingFor("Go", hello);
            final PonySDKWebDriver driver = new PonySDKWebDriver(new PonyMessageListener() {

                @Override
                public void onSendMessage(final JsonObject message) {
                }

                @Override
                public void onReceiveMessage(final List<PonyFrame> message) {
                    helloListener.onReceiveMessage(message);
                    if (message.stream().anyMatch(f -> f.getModel() == ServerToClientModel.DESTROY_CONTEXT)) destroyed.countDown();
                }
            }, null, new PonySessionListener() {

                @Override
                public void onOpen(final Session session) {
                }

                @Override
                public void onClose(final Session session, final CloseReason closeReason) {
                    closed.countDown();
                }

                @Override
                public void onError(final Session session, final Throwable thr) {
                }
            }, true);
            driver.get("ws://127.0.0.1:" + port(server) + "/app/ws");
            assertTrue(hello.await(10, TimeUnit.SECONDS));

            server.drainAndStop(); // what SIGTERM runs
            assertTrue(destroyed.await(10, TimeUnit.SECONDS), "the page told its context is over: it reloads");
            assertTrue(closed.await(10, TimeUnit.SECONDS));
            assertFalse(server.server.isRunning());
        } finally {
            server.stop(); // the SIGTERM hook removed
        }
    }

    @Test
    public void theHealthCheckSaysUpThenDrainingWhileThePagesMove() throws Exception {
        final PonySDKServer server = server();
        server.setDrainMillis(3_000);
        server.start();
        final Application application = new Application("health", Mockito.mock(HttpSession.class), new ApplicationConfiguration());
        final UIContext lingering = Mockito.mock(UIContext.class); // a client that does not go: the drain lasts its window
        Mockito.when(lingering.isAlive()).thenReturn(true);
        application.registerUIContext(lingering);
        try (HttpClient http = HttpClient.newHttpClient()) {
            final HttpRequest health = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port(server) + "/app/health")).build();
            final HttpResponse<String> up = http.send(health, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, up.statusCode());
            assertEquals("UP", up.body());
            assertEquals("no-store", up.headers().firstValue("Cache-Control").orElse(null));
            assertTrue(up.headers().firstValue("Set-Cookie").isEmpty(), "no session for a health check");

            SessionManager.get().registerApplication(application);
            final Thread sigterm = Thread.ofVirtual().start(server::drainAndStop);
            HttpResponse<String> draining = http.send(health, HttpResponse.BodyHandlers.ofString());
            final long deadline = System.currentTimeMillis() + 2_000;
            while (draining.statusCode() != 503 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
                draining = http.send(health, HttpResponse.BodyHandlers.ofString());
            }
            assertEquals(503, draining.statusCode());
            assertEquals("DRAINING", draining.body());
            sigterm.join();
        } finally {
            SessionManager.get().unregisterApplication(application);
            server.stop();
        }
    }

    @Test
    public void anInterruptedDrainLeavesTheServerAsItIs() throws Exception {
        final PonySDKServer server = server();
        server.setDrainMillis(5_000);
        server.start();
        final Application application = new Application("drain", Mockito.mock(HttpSession.class), new ApplicationConfiguration());
        final UIContext lingering = Mockito.mock(UIContext.class);
        Mockito.when(lingering.isAlive()).thenReturn(true);
        application.registerUIContext(lingering);
        SessionManager.get().registerApplication(application);
        try {
            Thread.currentThread().interrupt();
            server.drainAndStop();
            assertTrue(server.server.isRunning(), "not stopped by a drain that did not end");
        } finally {
            Thread.interrupted();
            SessionManager.get().unregisterApplication(application);
            server.stop();
        }
    }

    @Test
    public void aListenerFailureIsReportedAsASessionError() throws Exception {
        final PonySDKServer server = server();
        server.start();
        try {
            final RuntimeException failure = new IllegalStateException("listener failure");
            final CountDownLatch errored = new CountDownLatch(1);
            final Throwable[] reported = new Throwable[1];
            final PonySDKWebDriver driver = new PonySDKWebDriver(new PonyMessageListener() {

                @Override
                public void onSendMessage(final JsonObject message) {
                }

                @Override
                public void onReceiveMessage(final List<PonyFrame> message) {
                    throw failure;
                }
            }, null, new PonySessionListener() {

                @Override
                public void onOpen(final Session session) {
                }

                @Override
                public void onClose(final Session session, final CloseReason closeReason) {
                }

                @Override
                public void onError(final Session session, final Throwable thr) {
                    reported[0] = thr;
                    errored.countDown();
                }
            }, false);

            driver.get("ws://127.0.0.1:" + port(server) + "/app/ws");
            assertTrue(errored.await(10, TimeUnit.SECONDS), "the failure reached the session listener");
            assertSame(failure, reported[0]);

            driver.ignoreSessionStatusChanges();
            driver.close();
        } finally {
            server.stop();
        }
    }

    @Test
    public void withoutSessionListenerAFailureIsIgnoredAndAReconnectionStartsAFreshSession() throws Exception {
        final PonySDKServer server = server();
        server.start();
        try {
            final CountDownLatch failed = new CountDownLatch(1);
            final CountDownLatch hello = new CountDownLatch(1);
            final CountDownLatch clicked = new CountDownLatch(1);
            final List<List<PonyFrame>> afterFailure = new CopyOnWriteArrayList<>();
            final PonySDKWebDriver driver = new PonySDKWebDriver(new PonyMessageListener() {

                @Override
                public void onSendMessage(final JsonObject message) {
                }

                @Override
                public void onReceiveMessage(final List<PonyFrame> message) {
                    final Object text = PonySDKWebDriver.findValueForModel(message, ServerToClientModel.TEXT);
                    if (failed.getCount() == 1) {
                        if ("Go".equals(text)) {
                            failed.countDown();
                            throw new IllegalStateException("listener failure");
                        }
                        return;
                    }
                    afterFailure.add(List.copyOf(message));
                    if ("Go".equals(text)) hello.countDown();
                    if ("Clicked".equals(text)) clicked.countDown();
                }
            }, null, null, true);

            final String url = "ws://127.0.0.1:" + port(server) + "/app/ws";
            driver.get(url);
            assertTrue(failed.await(10, TimeUnit.SECONDS));
            final String first = driver.getSessionId();

            driver.get(url);
            assertNotEquals(first, driver.getSessionId(), "a new session replaces the first one");
            assertTrue(hello.await(10, TimeUnit.SECONDS), "the new session received its widgets");
            // Nothing of the failed message leaks into the next one
            assertEquals(ServerToClientModel.CREATE_CONTEXT, afterFailure.get(0).get(0).getModel());
            driver.findElement(By.tagName("BUTTON")).click();
            assertTrue(clicked.await(10, TimeUnit.SECONDS), "the click ran on the server");
            driver.close();
        } finally {
            server.stop();
        }
    }

    /** Exposes the protected connector factories. */
    private static final class ConnectorServer extends PonySDKServer {

        ServerConnector http() {
            return createHttpConnector();
        }

        ServerConnector https() {
            return createHttpsConnector();
        }
    }

    @Test
    public void connectorsListenWhereConfigured() {
        final ConnectorServer server = new ConnectorServer();
        server.setHost("127.0.0.1");
        server.setPort(8081);
        server.setSslPort(8443);
        assertTrue(server.isUseSSL());

        final ServerConnector http = server.http();
        assertEquals("127.0.0.1", http.getHost());
        assertEquals(8081, http.getPort());
        assertEquals(8443, http.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration().getSecurePort(),
            "plain http knows where the secure port is");

        server.setUseSSL(false);
        assertNotEquals(8443, server.http().getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration().getSecurePort());
    }

    @Test
    public void httpsConnectorCarriesTheTlsSettings(@TempDir final Path dir) throws Exception {
        final String trustStore = Files.createFile(dir.resolve("trust.p12")).toString();
        final ConnectorServer server = new ConnectorServer();
        server.setSslPort(9443);
        // Any classpath resource: the key store is only opened when the connector starts
        server.setSslKeyStoreFile("/" + PonySDKServerRunTest.class.getName().replace('.', '/') + ".class");
        server.setSslKeyStorePassphrase("store-pass");
        server.setSslKeyStoreType("PKCS12");
        server.setEnabledProtocols(new String[] { "TLSv1.3" });
        server.setEnabledCipherSuites("TLS_AES_128_GCM_SHA256");
        server.setNeedClientAuth(true);
        server.setSslTrustStoreFile(trustStore);
        server.setSslTrustStorePassphrase("trust-pass");
        server.setSslTrustStoreType("PKCS12");

        final ServerConnector https = server.https();

        assertEquals(9443, https.getPort());
        final SslContextFactory.Server ssl = sslOf(https);
        assertTrue(ssl.getKeyStorePath().endsWith("PonySDKServerRunTest.class"));
        assertEquals("PKCS12", ssl.getKeyStoreType());
        assertArrayEquals(new String[] { "TLSv1.3" }, ssl.getIncludeProtocols());
        assertArrayEquals(new String[] { "TLS_AES_128_GCM_SHA256" }, ssl.getIncludeCipherSuites());
        assertTrue(ssl.getNeedClientAuth());
        assertTrue(ssl.getTrustStorePath().endsWith("trust.p12"));
        assertEquals("PKCS12", ssl.getTrustStoreType());
    }

    @Test
    public void httpsWithoutClientAuthIgnoresTheTrustStore() {
        final ConnectorServer server = new ConnectorServer();
        server.setSslKeyStoreFile(PonySDKServerRunTest.class.getName().replace('.', '/') + ".class"); // class loader lookup
        server.setSslTrustStoreFile("trust.p12");

        final SslContextFactory.Server ssl = sslOf(server.https());

        assertFalse(ssl.getNeedClientAuth());
        assertNull(ssl.getTrustStorePath());
        assertArrayEquals(new String[] { "TLSv1.2" }, ssl.getIncludeProtocols(), "TLS 1.2 by default");
    }

    @Test
    public void clientAuthWithoutATrustStoreUsesTheDefaultOne() {
        final ConnectorServer server = new ConnectorServer();
        server.setSslKeyStoreFile(PonySDKServerRunTest.class.getName().replace('.', '/') + ".class");
        server.setNeedClientAuth(true);

        final SslContextFactory.Server ssl = sslOf(server.https());

        assertTrue(ssl.getNeedClientAuth());
        assertNull(ssl.getTrustStorePath());
    }

    @Test
    public void anAbsentKeyStoreIsNamed() {
        final ConnectorServer server = new ConnectorServer();
        server.setSslKeyStoreFile("absent.jks");
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, server::https);
        assertEquals("KeyStore not found #absent.jks", e.getMessage());
    }

    private static SslContextFactory.Server sslOf(final ServerConnector connector) {
        for (final ConnectionFactory factory : connector.getConnectionFactories()) {
            if (factory instanceof SslConnectionFactory ssl) return (SslContextFactory.Server) ssl.getSslContextFactory();
        }
        throw new AssertionError("no TLS on " + connector);
    }
}
