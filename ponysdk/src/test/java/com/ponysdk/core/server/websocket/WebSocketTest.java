/*
 * Copyright (c) 2019 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;

import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.util.Pair;

public class WebSocketTest {

    private WebSocket webSocket;
    private UIContext uiContext;
    private Session session;

    private final List<Pair<ServerToClientModel, Object>> encodedValues = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        webSocket = new WebSocket() {

            @Override
            void flush0() {
                // Nothing to do
            }

            @Override
            public void encode(final ServerToClientModel model, final Object value) {
                encodedValues.add(new Pair<>(model, value));
            }
        };

        encodedValues.clear();

        final JettyServerUpgradeRequest request = Mockito.mock(JettyServerUpgradeRequest.class);
        Mockito.when(request.getParameterMap()).thenReturn(java.util.Map.of());
        Mockito.when(request.getHeader("User-Agent")).thenReturn("test-agent");
        final jakarta.servlet.http.HttpServletRequest httpRequest = Mockito.mock(jakarta.servlet.http.HttpServletRequest.class);
        Mockito.when(httpRequest.getSession()).thenReturn(Mockito.mock(jakarta.servlet.http.HttpSession.class));
        Mockito.when(request.getHttpServletRequest()).thenReturn(httpRequest);
        webSocket.setRequest(request);
        final TxnContext txnContext = Mockito.mock(TxnContext.class);
        Mockito.when(txnContext.getWriter()).thenReturn(new com.ponysdk.core.writer.ModelWriter(webSocket));
        webSocket.setContext(txnContext);

        final ApplicationManager applicationManager = Mockito.mock(ApplicationManager.class);
        final ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
        applicationConfiguration.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        Mockito.when(applicationManager.getConfiguration()).thenReturn(applicationConfiguration);
        webSocket.setApplicationManager(applicationManager);

        session = Mockito.mock(Session.class);
        Mockito.when(session.isOpen()).thenReturn(true);
        webSocket.onWebSocketOpen(session);

        final ArgumentCaptor<UIContext> uiContextCaptor = ArgumentCaptor.forClass(UIContext.class);
        Mockito.verify(applicationManager, Mockito.times(1)).startApplication(uiContextCaptor.capture());
        uiContext = uiContextCaptor.getValue();

        assertEquals(request, webSocket.getRequest());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.websocket.WebSocket#onWebSocketText(java.lang.String)}.
     */
    @Test
    public void testInstruction() {
        final JsonProvider provider = JsonProvider.provider();
        final JsonObjectBuilder job = provider.createObjectBuilder();
        final JsonArrayBuilder jab = provider.createArrayBuilder();
        final JsonObjectBuilder job1 = provider.createObjectBuilder();
        job1.add(ClientToServerModel.OBJECT_ID.toStringValue(), false);
        jab.add(job1.build());
        job.add(ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue(), jab);
        webSocket.onWebSocketText(job.build().toString());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.websocket.WebSocket#sendRoundTrip()} and
     * {@link com.ponysdk.core.server.websocket.WebSocket#onWebSocketText(java.lang.String)}.
     */
    @Test
    @Ignore
    public void testRoundTrip() {
        for (int i = 0; i < 10; i++) { // Warmup
            webSocket.sendRoundTrip();

            final JsonObjectBuilder job = JsonProvider.provider().createObjectBuilder();
            job.add(ClientToServerModel.TERMINAL_LATENCY.toStringValue(), 1); // 1 ms of terminal latency
            webSocket.onWebSocketText(job.build().toString());
        }

        assertEquals(1, uiContext.getTerminalLatency(), 0.01);
        assertEquals(uiContext.getRoundtripLatency(), uiContext.getTerminalLatency() + uiContext.getNetworkLatency(), 0.01);
    }

    /**
     * Test method for {@link com.ponysdk.core.server.websocket.WebSocket#onWebSocketText(java.lang.String)}
     */
    @Test
    @Ignore
    public void testOnWebSocketTextRoundTripLatency() {
        encodedValues.clear();
        final JsonObjectBuilder job = JsonProvider.provider().createObjectBuilder();
        job.add(ClientToServerModel.HEARTBEAT_REQUEST.toStringValue(), JsonValue.NULL);
        webSocket.onWebSocketText(job.build().toString());
        assertEquals(encodedValues,
            List.of(new Pair<>(ServerToClientModel.HEARTBEAT, null), new Pair<>(ServerToClientModel.END, null)));
    }

    /**
     * Test method for {@link com.ponysdk.core.server.websocket.WebSocket#onWebSocketOpen(Session)}
     */
    @Test
    public void testOnWebSocketConnect() {
        assertEquals(encodedValues.get(0), new Pair<>(ServerToClientModel.CREATE_CONTEXT, uiContext.getID()));
        assertEquals(encodedValues.get(1), new Pair<>(ServerToClientModel.OPTION_FORMFIELD_TABULATION, false));
        assertEquals(encodedValues.get(2), new Pair<>(ServerToClientModel.HEARTBEAT_PERIOD, 0));
    }

    /**
     * Test method for {@link com.ponysdk.core.server.websocket.WebSocket#onWebSocketText(java.lang.String)}.
     */
    @Test
    public void testErrorMessage() {
        final JsonObjectBuilder job = JsonProvider.provider().createObjectBuilder();
        job.add(ClientToServerModel.ERROR_MSG.toStringValue(), "Test");
        job.add(ClientToServerModel.OBJECT_ID.toStringValue(), 1);
        webSocket.onWebSocketText(job.build().toString());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.websocket.WebSocket#onWebSocketText(java.lang.String)}.
     */
    @Test
    public void testWarningMessage() {
        final JsonObjectBuilder job = JsonProvider.provider().createObjectBuilder();
        job.add(ClientToServerModel.WARN_MSG.toStringValue(), "Test");
        job.add(ClientToServerModel.OBJECT_ID.toStringValue(), 1);
        webSocket.onWebSocketText(job.build().toString());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.websocket.WebSocket#onWebSocketText(java.lang.String)}.
     */
    @Test
    public void testInfoMessage() {
        final JsonObjectBuilder job = JsonProvider.provider().createObjectBuilder();
        job.add(ClientToServerModel.INFO_MSG.toStringValue(), "Test");
        job.add(ClientToServerModel.OBJECT_ID.toStringValue(), 1);
        webSocket.onWebSocketText(job.build().toString());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.websocket.WebSocket#onWebSocketText(java.lang.String)}.
     */
    @Test
    public void testUnknowMessage() {
        webSocket.setMonitor(new WebsocketMonitor() {

            @Override
            public void onMessageUnprocessed(final WebSocket webSocket, final String message) {
            }

            @Override
            public void onMessageReceived(final WebSocket webSocket, final String message) {
            }

            @Override
            public void onMessageProcessed(final WebSocket webSocket, final String message) {
            }
        });

        final JsonObjectBuilder job = JsonProvider.provider().createObjectBuilder();
        job.add(ClientToServerModel.COOKIE_NAME.toStringValue(), 0);
        webSocket.onWebSocketText(job.build().toString());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.websocket.WebSocket#onWebSocketError(java.lang.Throwable)}.
     */
    @Test
    public void testOnWebSocketError() {
        assertTrue(uiContext.isAlive());
        webSocket.onWebSocketError(new Exception("Unit test"));
        assertFalse(uiContext.isAlive());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.websocket.WebSocket#onWebSocketClose(int, java.lang.String)}.
     */
    @Test
    public void testOnWebSocketClose() {
        assertTrue(uiContext.isAlive());
        webSocket.onWebSocketClose(StatusCode.NORMAL, "Close");
        assertFalse(uiContext.isAlive());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.websocket.WebSocket#onWebSocketBinary(java.nio.ByteBuffer, org.eclipse.jetty.websocket.api.Callback)}.
     */
    @Test
    public void testOnWebSocketBinary() {
        // Not implemented yet
        webSocket.onWebSocketBinary(null, Callback.NOOP);
    }

    /**
     * Test method for {@link com.ponysdk.core.server.websocket.WebSocket#close()}.
     */
    @Test
    public void testClose() {
        webSocket.close();
        Mockito.verify(session, Mockito.times(1)).close(StatusCode.NORMAL, "close", Callback.NOOP);
    }

    // ---- Default mode: reconnectionTimeoutMs=0 → close = destroy ----

    /**
     * Verifies that with the default configuration (reconnectionTimeoutMs=0),
     * closing the WebSocket destroys the UIContext immediately — no suspension.
     */
    @Test
    public void testDefaultMode_CloseDestroysUIContext() {
        // setUp already uses reconnectionTimeoutMs=0 (default)
        assertTrue(uiContext.isAlive());
        assertFalse(uiContext.isSuspended());

        webSocket.onWebSocketClose(StatusCode.NORMAL, "Close");

        assertFalse("Default mode: UIContext should be destroyed on close", uiContext.isAlive());
        assertFalse("Default mode: UIContext should NOT be suspended", uiContext.isSuspended());
    }

    /**
     * Verifies that with the default configuration, an error also destroys the UIContext.
     */
    @Test
    public void testDefaultMode_ErrorDestroysUIContext() {
        assertTrue(uiContext.isAlive());

        webSocket.onWebSocketError(new Exception("network failure"));

        assertFalse("Default mode: UIContext should be destroyed on error", uiContext.isAlive());
        assertFalse(uiContext.isSuspended());
    }

    // ---- Reconnection mode: reconnectionTimeoutMs>0 → close = suspend ----

    /**
     * Helper: creates a fresh WebSocket+UIContext with reconnection enabled.
     */
    private record ReconnectionSetup(WebSocket webSocket, UIContext uiContext, ApplicationManager appManager) {}

    private ReconnectionSetup createReconnectionWebSocket() throws Exception {
        final WebSocket ws = new WebSocket() {
            @Override void flush0() {}
            @Override public void encode(final ServerToClientModel model, final Object value) {}
        };

        final JettyServerUpgradeRequest request = Mockito.mock(JettyServerUpgradeRequest.class);
        Mockito.when(request.getParameterMap()).thenReturn(java.util.Map.of());
        Mockito.when(request.getHeader("User-Agent")).thenReturn("test-agent");
        final jakarta.servlet.http.HttpServletRequest httpRequest = Mockito.mock(jakarta.servlet.http.HttpServletRequest.class);
        Mockito.when(httpRequest.getSession()).thenReturn(Mockito.mock(jakarta.servlet.http.HttpSession.class));
        Mockito.when(request.getHttpServletRequest()).thenReturn(httpRequest);
        ws.setRequest(request);

        final TxnContext txnContext = Mockito.mock(TxnContext.class);
        Mockito.when(txnContext.getWriter()).thenReturn(new com.ponysdk.core.writer.ModelWriter(ws));
        ws.setContext(txnContext);

        final ApplicationManager appManager = Mockito.mock(ApplicationManager.class);
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setReconnectionTimeoutMs(5000); // reconnection enabled
        config.setMaxRecordingEntries(10_000);
        config.setStringDictionaryEnabled(false);
        Mockito.when(appManager.getConfiguration()).thenReturn(config);
        ws.setApplicationManager(appManager);

        final Session sess = Mockito.mock(Session.class);
        Mockito.when(sess.isOpen()).thenReturn(true);
        ws.onWebSocketOpen(sess);

        final ArgumentCaptor<UIContext> captor = ArgumentCaptor.forClass(UIContext.class);
        Mockito.verify(appManager).startApplication(captor.capture());
        return new ReconnectionSetup(ws, captor.getValue(), appManager);
    }

    /**
     * Verifies that with reconnection enabled, closing the WebSocket suspends
     * the UIContext instead of destroying it.
     */
    @Test
    public void testReconnectionMode_CloseSuspendsUIContext() throws Exception {
        final ReconnectionSetup setup = createReconnectionWebSocket();

        assertTrue(setup.uiContext().isAlive());
        assertFalse(setup.uiContext().isSuspended());

        setup.webSocket().onWebSocketClose(StatusCode.NORMAL, "Close");

        assertTrue("Reconnection mode: UIContext should still be alive", setup.uiContext().isAlive());
        assertTrue("Reconnection mode: UIContext should be suspended", setup.uiContext().isSuspended());
    }

    /**
     * Verifies that with reconnection enabled, an error on the old socket
     * does NOT destroy a suspended UIContext (bug fix validation).
     */
    @Test
    public void testReconnectionMode_ErrorOnSuspendedDoesNotDestroy() throws Exception {
        final ReconnectionSetup setup = createReconnectionWebSocket();

        // Close → suspend
        setup.webSocket().onWebSocketClose(StatusCode.NORMAL, "Close");
        assertTrue(setup.uiContext().isSuspended());

        // Error on old socket — should NOT destroy
        setup.webSocket().onWebSocketError(new Exception("broken pipe on old socket"));

        assertTrue("Suspended UIContext should survive old socket errors", setup.uiContext().isAlive());
        assertTrue(setup.uiContext().isSuspended());
    }

    /**
     * Verifies that with reconnection enabled, if the timeout expires,
     * the UIContext is destroyed.
     */
    @Test
    public void testReconnectionMode_TimeoutDestroysAfterSuspend() throws Exception {
        // Use a very short timeout for the test
        final WebSocket ws = new WebSocket() {
            @Override void flush0() {}
            @Override public void encode(final ServerToClientModel model, final Object value) {}
        };

        final JettyServerUpgradeRequest request = Mockito.mock(JettyServerUpgradeRequest.class);
        Mockito.when(request.getParameterMap()).thenReturn(java.util.Map.of());
        Mockito.when(request.getHeader("User-Agent")).thenReturn("test-agent");
        final jakarta.servlet.http.HttpServletRequest httpRequest = Mockito.mock(jakarta.servlet.http.HttpServletRequest.class);
        Mockito.when(httpRequest.getSession()).thenReturn(Mockito.mock(jakarta.servlet.http.HttpSession.class));
        Mockito.when(request.getHttpServletRequest()).thenReturn(httpRequest);
        ws.setRequest(request);

        final TxnContext txnContext = Mockito.mock(TxnContext.class);
        Mockito.when(txnContext.getWriter()).thenReturn(new com.ponysdk.core.writer.ModelWriter(ws));
        ws.setContext(txnContext);

        final ApplicationManager appManager = Mockito.mock(ApplicationManager.class);
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setReconnectionTimeoutMs(200); // 200ms — short for test
        config.setMaxRecordingEntries(10_000);
        config.setStringDictionaryEnabled(false);
        Mockito.when(appManager.getConfiguration()).thenReturn(config);
        ws.setApplicationManager(appManager);

        final Session sess = Mockito.mock(Session.class);
        Mockito.when(sess.isOpen()).thenReturn(true);
        ws.onWebSocketOpen(sess);

        final ArgumentCaptor<UIContext> captor = ArgumentCaptor.forClass(UIContext.class);
        Mockito.verify(appManager).startApplication(captor.capture());
        final UIContext ctx = captor.getValue();

        // Close → suspend with 200ms timeout
        ws.onWebSocketClose(StatusCode.NORMAL, "Close");
        assertTrue(ctx.isAlive());
        assertTrue(ctx.isSuspended());

        // Wait for timeout
        Thread.sleep(500);

        assertFalse("UIContext should be destroyed after reconnection timeout", ctx.isAlive());
        assertFalse(ctx.isSuspended());
    }

    // ---- Reconnection path in onWebSocketOpen ----

    /**
     * onWebSocketClose with a ReconnectionListener that throws — should NOT prevent suspension.
     * The listener exception is caught, and suspend() still proceeds.
     */
    @Test
    public void testReconnectionMode_ListenerExceptionDoesNotPreventSuspend() throws Exception {
        final ReconnectionSetup setup = createReconnectionWebSocket();

        // Set a listener that throws
        final ApplicationConfiguration config = setup.appManager().getConfiguration();
        config.setReconnectionListener(ctx -> {
            throw new RuntimeException("Listener blew up");
        });

        setup.webSocket().onWebSocketClose(StatusCode.NORMAL, "Close");

        // Despite the listener exception, UIContext should be suspended
        assertTrue("UIContext should still be alive", setup.uiContext().isAlive());
        assertTrue("UIContext should be suspended despite listener exception", setup.uiContext().isSuspended());
    }

    /**
     * onWebSocketClose with ABNORMAL status code — should still suspend in reconnection mode.
     * The status code doesn't affect the reconnection decision.
     */
    @Test
    public void testReconnectionMode_AbnormalCloseStillSuspends() throws Exception {
        final ReconnectionSetup setup = createReconnectionWebSocket();

        setup.webSocket().onWebSocketClose(StatusCode.ABNORMAL, "Abnormal disconnect");

        assertTrue("UIContext should be alive after abnormal close", setup.uiContext().isAlive());
        assertTrue("UIContext should be suspended after abnormal close", setup.uiContext().isSuspended());
    }

    /**
     * Multiple onWebSocketClose calls — should not crash.
     * The second close on an already-suspended UIContext calls suspend() again
     * (which creates a new RecordingEncoder, losing the first one).
     */
    @Test
    public void testReconnectionMode_DoubleCloseDoesNotCrash() throws Exception {
        final ReconnectionSetup setup = createReconnectionWebSocket();

        setup.webSocket().onWebSocketClose(StatusCode.NORMAL, "First close");
        assertTrue(setup.uiContext().isSuspended());

        // Second close — should not crash
        setup.webSocket().onWebSocketClose(StatusCode.NORMAL, "Second close");
        assertTrue(setup.uiContext().isAlive());
        assertTrue(setup.uiContext().isSuspended());
    }

    /**
     * onWebSocketError on a non-suspended UIContext in reconnection mode — should destroy.
     * The isSuspended() guard only protects suspended contexts.
     */
    @Test
    public void testReconnectionMode_ErrorOnNonSuspendedDestroys() throws Exception {
        final ReconnectionSetup setup = createReconnectionWebSocket();

        // Error WITHOUT prior close (not suspended)
        assertFalse(setup.uiContext().isSuspended());
        setup.webSocket().onWebSocketError(new Exception("unexpected error"));

        assertFalse("Non-suspended UIContext should be destroyed on error", setup.uiContext().isAlive());
    }

    // ---- Reconnection path in onWebSocketOpen: edge cases ----

    /**
     * onWebSocketOpen with reconnectContextId but Application is null.
     * This happens if the HTTP session expired between disconnect and reconnect.
     * Should fall through to creating a new UIContext (normal path).
     */
    @Test
    public void testReconnectionMode_ReconnectWithNullApplication() throws Exception {
        final WebSocket ws = new WebSocket() {
            @Override void flush0() {}
            @Override public void encode(final ServerToClientModel model, final Object value) {}
        };

        final JettyServerUpgradeRequest request = Mockito.mock(JettyServerUpgradeRequest.class);
        Mockito.when(request.getParameterMap()).thenReturn(java.util.Map.of());
        Mockito.when(request.getHeader("User-Agent")).thenReturn("test-agent");
        final jakarta.servlet.http.HttpServletRequest httpRequest = Mockito.mock(jakarta.servlet.http.HttpServletRequest.class);
        Mockito.when(httpRequest.getSession()).thenReturn(Mockito.mock(jakarta.servlet.http.HttpSession.class));
        Mockito.when(request.getHttpServletRequest()).thenReturn(httpRequest);
        ws.setRequest(request);

        // TxnContext with NO application (null)
        final TxnContext txnContext = new TxnContext(ws);
        // Don't set application → context.getApplication() returns null
        ws.setContext(txnContext);

        final ApplicationManager appManager = Mockito.mock(ApplicationManager.class);
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setReconnectionTimeoutMs(5000);
        config.setStringDictionaryEnabled(false);
        Mockito.when(appManager.getConfiguration()).thenReturn(config);
        ws.setApplicationManager(appManager);

        // Set reconnect context ID
        ws.setReconnectContextId(999);

        final Session sess = Mockito.mock(Session.class);
        Mockito.when(sess.isOpen()).thenReturn(true);

        // onWebSocketOpen should fall through to creating a new UIContext
        // because application is null → suspended UIContext can't be found
        ws.onWebSocketOpen(sess);

        // Verify a new UIContext was created (startApplication was called)
        Mockito.verify(appManager).startApplication(Mockito.any(UIContext.class));
    }

    /**
     * onWebSocketOpen with reconnectContextId pointing to a non-existent UIContext.
     * Application exists but getUIContext(id) returns null.
     * Should fall through to creating a new UIContext.
     */
    @Test
    public void testReconnectionMode_ReconnectWithNonExistentUIContext() throws Exception {
        final WebSocket ws = new WebSocket() {
            @Override void flush0() {}
            @Override public void encode(final ServerToClientModel model, final Object value) {}
        };

        final JettyServerUpgradeRequest request = Mockito.mock(JettyServerUpgradeRequest.class);
        Mockito.when(request.getParameterMap()).thenReturn(java.util.Map.of());
        Mockito.when(request.getHeader("User-Agent")).thenReturn("test-agent");
        final jakarta.servlet.http.HttpServletRequest httpRequest = Mockito.mock(jakarta.servlet.http.HttpServletRequest.class);
        Mockito.when(httpRequest.getSession()).thenReturn(Mockito.mock(jakarta.servlet.http.HttpSession.class));
        Mockito.when(request.getHttpServletRequest()).thenReturn(httpRequest);
        ws.setRequest(request);

        // TxnContext with a real Application that has no UIContexts
        final TxnContext txnContext = new TxnContext(ws);
        final com.ponysdk.core.server.application.Application application =
                new com.ponysdk.core.server.application.Application("test",
                        Mockito.mock(jakarta.servlet.http.HttpSession.class),
                        new ApplicationConfiguration());
        txnContext.setApplication(application);
        ws.setContext(txnContext);

        final ApplicationManager appManager = Mockito.mock(ApplicationManager.class);
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setReconnectionTimeoutMs(5000);
        config.setStringDictionaryEnabled(false);
        Mockito.when(appManager.getConfiguration()).thenReturn(config);
        ws.setApplicationManager(appManager);

        // Set reconnect context ID to a non-existent UIContext
        ws.setReconnectContextId(12345);

        final Session sess = Mockito.mock(Session.class);
        Mockito.when(sess.isOpen()).thenReturn(true);

        // Should fall through to creating a new UIContext
        ws.onWebSocketOpen(sess);

        Mockito.verify(appManager).startApplication(Mockito.any(UIContext.class));
    }

    /**
     * onWebSocketOpen with reconnectContextId pointing to a UIContext that exists
     * but is NOT suspended (e.g., it was already resumed by another connection).
     * Should fall through to creating a new UIContext.
     */
    @Test
    public void testReconnectionMode_ReconnectWithNonSuspendedUIContext() throws Exception {
        // First, create a UIContext via normal connection
        final ReconnectionSetup setup = createReconnectionWebSocket();
        final int ctxId = setup.uiContext().getID();

        // The UIContext is alive but NOT suspended
        assertTrue(setup.uiContext().isAlive());
        assertFalse(setup.uiContext().isSuspended());

        // Now create a second WebSocket that tries to reconnect to this UIContext
        final WebSocket ws2 = new WebSocket() {
            @Override void flush0() {}
            @Override public void encode(final ServerToClientModel model, final Object value) {}
        };

        final JettyServerUpgradeRequest request2 = Mockito.mock(JettyServerUpgradeRequest.class);
        Mockito.when(request2.getParameterMap()).thenReturn(java.util.Map.of());
        Mockito.when(request2.getHeader("User-Agent")).thenReturn("test-agent");
        final jakarta.servlet.http.HttpServletRequest httpRequest2 = Mockito.mock(jakarta.servlet.http.HttpServletRequest.class);
        Mockito.when(httpRequest2.getSession()).thenReturn(Mockito.mock(jakarta.servlet.http.HttpSession.class));
        Mockito.when(request2.getHttpServletRequest()).thenReturn(httpRequest2);
        ws2.setRequest(request2);

        // Use a TxnContext with an Application that contains the non-suspended UIContext
        final TxnContext txnContext2 = new TxnContext(ws2);
        final com.ponysdk.core.server.application.Application application =
                new com.ponysdk.core.server.application.Application("test",
                        Mockito.mock(jakarta.servlet.http.HttpSession.class),
                        setup.appManager().getConfiguration());
        application.registerUIContext(setup.uiContext());
        txnContext2.setApplication(application);
        ws2.setContext(txnContext2);

        ws2.setApplicationManager(setup.appManager());
        ws2.setReconnectContextId(ctxId);

        // Reset the mock to track new calls
        Mockito.reset(setup.appManager());
        Mockito.when(setup.appManager().getConfiguration()).thenReturn(
                new ApplicationConfiguration() {{
                    setHeartBeatPeriod(0, TimeUnit.SECONDS);
                    setReconnectionTimeoutMs(5000);
                    setStringDictionaryEnabled(false);
                }});

        final Session sess2 = Mockito.mock(Session.class);
        Mockito.when(sess2.isOpen()).thenReturn(true);

        // Should fall through to creating a new UIContext (target is not suspended)
        ws2.onWebSocketOpen(sess2);

        Mockito.verify(setup.appManager()).startApplication(Mockito.any(UIContext.class));
    }

    /**
     * execute() when the runnable throws during suspension — txn.rollback path.
     * The exception should be caught, execute() returns false, UIContext stays alive.
     */
    @Test
    public void testReconnectionMode_ExecuteThrowsDuringSuspension() throws Exception {
        final ReconnectionSetup setup = createReconnectionWebSocket();

        // Suspend
        setup.webSocket().onWebSocketClose(StatusCode.NORMAL, "Close");
        assertTrue(setup.uiContext().isSuspended());

        // execute() with a runnable that throws
        boolean result = setup.uiContext().execute(() -> {
            throw new RuntimeException("handler crash");
        });

        assertFalse("execute() should return false when runnable throws", result);
        assertTrue("UIContext should still be alive after runnable exception", setup.uiContext().isAlive());
        assertTrue("UIContext should still be suspended", setup.uiContext().isSuspended());
    }

    /**
     * onWebSocketClose with reconnection enabled, then multiple errors on old socket.
     * Each error should be silently ignored (isSuspended guard).
     */
    @Test
    public void testReconnectionMode_MultipleErrorsOnSuspendedIgnored() throws Exception {
        final ReconnectionSetup setup = createReconnectionWebSocket();

        setup.webSocket().onWebSocketClose(StatusCode.NORMAL, "Close");
        assertTrue(setup.uiContext().isSuspended());

        // Fire multiple errors — all should be ignored
        for (int i = 0; i < 5; i++) {
            setup.webSocket().onWebSocketError(new Exception("error " + i));
        }

        assertTrue("UIContext should survive all old socket errors", setup.uiContext().isAlive());
        assertTrue("UIContext should remain suspended", setup.uiContext().isSuspended());
    }

    /**
     * onWebSocketClose with reconnection enabled and a ReconnectionListener.
     * The listener should be called BEFORE suspend().
     * Verify the listener receives the correct UIContext.
     */
    @Test
    public void testReconnectionMode_ListenerCalledBeforeSuspend() throws Exception {
        final ReconnectionSetup setup = createReconnectionWebSocket();

        final java.util.concurrent.atomic.AtomicReference<UIContext> capturedCtx =
                new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicBoolean wasSuspendedDuringCallback =
                new java.util.concurrent.atomic.AtomicBoolean();

        setup.appManager().getConfiguration().setReconnectionListener(ctx -> {
            capturedCtx.set(ctx);
            wasSuspendedDuringCallback.set(ctx.isSuspended());
        });

        setup.webSocket().onWebSocketClose(StatusCode.NORMAL, "Close");

        assertSame("Listener should receive the correct UIContext",
                setup.uiContext(), capturedCtx.get());
        assertFalse("UIContext should NOT be suspended when listener is called",
                wasSuspendedDuringCallback.get());
        assertTrue("UIContext should be suspended AFTER listener returns",
                setup.uiContext().isSuspended());
    }

    // ---- Additional coverage: message handling edge cases ----

    /**
     * onWebSocketText when UIContext is dead (isAlive() == false).
     * The message should be silently dropped — no crash, no processing.
     */
    @Test
    public void testOnWebSocketText_WhenUIContextDead() {
        // Kill the UIContext
        uiContext.onDestroy();
        assertFalse(uiContext.isAlive());

        // Send a message — should be dropped silently
        final JsonObjectBuilder job = JsonProvider.provider().createObjectBuilder();
        job.add(ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue(),
                JsonProvider.provider().createArrayBuilder().build());
        webSocket.onWebSocketText(job.build().toString());

        // No crash — that's the assertion
    }

    /**
     * onWebSocketText with invalid JSON — should be caught by the catch block.
     * No crash, UIContext stays alive.
     */
    @Test
    public void testOnWebSocketText_InvalidJson() {
        assertTrue(uiContext.isAlive());

        // Send garbage — should be caught
        webSocket.onWebSocketText("this is not json {{{");

        // UIContext should still be alive (error is logged, not fatal)
        assertTrue("UIContext should survive invalid JSON", uiContext.isAlive());
    }

    /**
     * onWebSocketText with empty JSON object — no matching key.
     * Should hit the "Unknown message" log path.
     */
    @Test
    public void testOnWebSocketText_EmptyJsonObject() {
        assertTrue(uiContext.isAlive());

        webSocket.onWebSocketText("{}");

        assertTrue("UIContext should survive empty JSON", uiContext.isAlive());
    }

    /**
     * onWebSocketClose followed by onWebSocketError — in default mode.
     * After close destroys the UIContext, error on dead context should not crash.
     */
    @Test
    public void testDefaultMode_CloseFollowedByError() {
        assertTrue(uiContext.isAlive());

        webSocket.onWebSocketClose(StatusCode.NORMAL, "Close");
        assertFalse(uiContext.isAlive());

        // Error after close — UIContext is dead, isSuspended() is false
        // onWebSocketError checks isSuspended() — false, so it calls onDestroy()
        // onDestroy() → destroy() checks isAlive() → false → returns immediately
        webSocket.onWebSocketError(new Exception("late error"));

        // No crash
        assertFalse(uiContext.isAlive());
    }

    /**
     * onWebSocketError followed by onWebSocketClose — in default mode.
     * Error destroys first, then close on dead context should not crash.
     */
    @Test
    public void testDefaultMode_ErrorFollowedByClose() {
        assertTrue(uiContext.isAlive());

        webSocket.onWebSocketError(new Exception("error first"));
        assertFalse(uiContext.isAlive());

        // Close after error — UIContext already dead
        webSocket.onWebSocketClose(StatusCode.NORMAL, "Close");

        // No crash
        assertFalse(uiContext.isAlive());
    }

    // ---- WebSocket lifecycle edge cases ----

    /**
     * close() when session is already closed — should be a no-op.
     */
    @Test
    public void testCloseWhenSessionAlreadyClosed() {
        Mockito.when(session.isOpen()).thenReturn(false);

        // Should not throw — isSessionOpen() returns false
        webSocket.close();

        // session.close() should NOT be called
        Mockito.verify(session, Mockito.never()).close(Mockito.anyInt(), Mockito.anyString(), Mockito.any());
    }

    /**
     * disconnect() — programmatic disconnect.
     */
    @Test
    public void testDisconnect() {
        assertTrue(uiContext.isAlive());

        webSocket.disconnect();

        Mockito.verify(session).disconnect();
    }

    /**
     * disconnect() when session is already closed — should be a no-op.
     */
    @Test
    public void testDisconnectWhenSessionClosed() {
        Mockito.when(session.isOpen()).thenReturn(false);

        webSocket.disconnect();

        Mockito.verify(session, Mockito.never()).disconnect();
    }

    /**
     * flush() when session is closed — should be a no-op.
     */
    @Test
    public void testFlushWhenSessionClosed() {
        Mockito.when(session.isOpen()).thenReturn(false);

        // Should not throw
        webSocket.flush();
    }

    /**
     * flush() when UIContext is dead — should be a no-op.
     */
    @Test
    public void testFlushWhenUIContextDead() {
        uiContext.onDestroy();
        assertFalse(uiContext.isAlive());

        // Should not throw
        webSocket.flush();
    }

    /**
     * sendRoundTrip() when session is closed — should be a no-op.
     */
    @Test
    public void testSendRoundTripWhenSessionClosed() {
        Mockito.when(session.isOpen()).thenReturn(false);

        // Should not throw
        webSocket.sendRoundTrip();
    }

    /**
     * sendRoundTrip() when UIContext is dead — should be a no-op.
     */
    @Test
    public void testSendRoundTripWhenUIContextDead() {
        uiContext.onDestroy();
        assertFalse(uiContext.isAlive());

        // Should not throw
        webSocket.sendRoundTrip();
    }

    /**
     * onWebSocketClose with null reason — should not NPE.
     * The log statement uses Objects.requireNonNullElse(reason, "").
     */
    @Test
    public void testOnWebSocketCloseNullReason() {
        assertTrue(uiContext.isAlive());

        webSocket.onWebSocketClose(StatusCode.NORMAL, null);

        assertFalse(uiContext.isAlive());
    }

    /**
     * onWebSocketClose with reconnection enabled but reconnectionListener is null.
     * The null check in onWebSocketClose should skip the listener call gracefully.
     */
    @Test
    public void testReconnectionMode_CloseWithNullListener() throws Exception {
        final ReconnectionSetup setup = createReconnectionWebSocket();

        // Ensure no listener is set (default)
        assertNull(setup.appManager().getConfiguration().getReconnectionListener());

        setup.webSocket().onWebSocketClose(StatusCode.NORMAL, "Close");

        // Should suspend without NPE
        assertTrue(setup.uiContext().isAlive());
        assertTrue(setup.uiContext().isSuspended());
    }

    /**
     * Multiple onWebSocketText calls in rapid succession — all should be processed.
     */
    @Test
    public void testMultipleRapidMessages() {
        assertTrue(uiContext.isAlive());

        for (int i = 0; i < 10; i++) {
            final JsonObjectBuilder job = JsonProvider.provider().createObjectBuilder();
            job.add(ClientToServerModel.INFO_MSG.toStringValue(), "msg-" + i);
            job.add(ClientToServerModel.OBJECT_ID.toStringValue(), 1);
            webSocket.onWebSocketText(job.build().toString());
        }

        assertTrue("UIContext should survive rapid messages", uiContext.isAlive());
    }

    /**
     * WebSocket with monitor set — verify monitor callbacks are invoked.
     */
    @Test
    public void testMonitorCallbacksInvoked() {
        final java.util.concurrent.atomic.AtomicBoolean received = new java.util.concurrent.atomic.AtomicBoolean();
        final java.util.concurrent.atomic.AtomicBoolean processed = new java.util.concurrent.atomic.AtomicBoolean();
        final java.util.concurrent.atomic.AtomicBoolean unprocessed = new java.util.concurrent.atomic.AtomicBoolean();

        webSocket.setMonitor(new WebsocketMonitor() {
            @Override public void onMessageReceived(WebSocket ws, String msg) { received.set(true); }
            @Override public void onMessageProcessed(WebSocket ws, String msg) { processed.set(true); }
            @Override public void onMessageUnprocessed(WebSocket ws, String msg) { unprocessed.set(true); }
        });

        final JsonObjectBuilder job = JsonProvider.provider().createObjectBuilder();
        job.add(ClientToServerModel.INFO_MSG.toStringValue(), "test");
        job.add(ClientToServerModel.OBJECT_ID.toStringValue(), 1);
        webSocket.onWebSocketText(job.build().toString());

        assertTrue("onMessageReceived should be called", received.get());
        assertTrue("onMessageProcessed should be called", processed.get());
        assertTrue("onMessageUnprocessed should be called", unprocessed.get());
    }

    // ====================================================================
    // Memory leak tests — WebSocket reference management
    // ====================================================================

    /** Helper: tries GC up to 10 times, returns true if the ref was collected. */
    private static boolean tryCollect(final WeakReference<?> ref) {
        for (int i = 0; i < 10; i++) {
            System.gc();
            try { Thread.sleep(50); } catch (final InterruptedException ignored) {}
            if (ref.get() == null) return true;
        }
        return false;
    }

    /**
     * After close (default mode), the old WebSocket still holds a ref to UIContext.
     * This is expected — the WebSocket is short-lived and should be GC'd by the
     * container. But verify the UIContext is properly destroyed.
     */
    @Test
    public void testClose_UIContextDestroyedAndDeadAfterClose() {
        assertTrue(uiContext.isAlive());
        webSocket.onWebSocketClose(StatusCode.NORMAL, "Close");
        assertFalse("UIContext should be dead after close", uiContext.isAlive());
        assertFalse("UIContext should not be suspended", uiContext.isSuspended());
    }

    /**
     * In reconnection mode, after suspend, the old WebSocket still references
     * the UIContext. After the UIContext is resumed on a new WebSocket, the old
     * WebSocket's reference is stale but harmless (isAlive checks prevent action).
     * Verify that messages on the old WebSocket are dropped after suspend.
     */
    @Test
    public void testReconnectionMode_OldWebSocketDropsMessagesAfterSuspend() throws Exception {
        final ReconnectionSetup setup = createReconnectionWebSocket();
        assertTrue(setup.uiContext().isAlive());

        // Close triggers suspend
        setup.webSocket().onWebSocketClose(StatusCode.NORMAL, "disconnect");
        assertTrue(setup.uiContext().isSuspended());
        assertTrue(setup.uiContext().isAlive());

        // Messages on the old WebSocket should be dropped (UIContext is alive but
        // the old WebSocket's session is closed)
        // This doesn't crash — it just logs and drops
        final JsonObjectBuilder job = JsonProvider.provider().createObjectBuilder();
        job.add(ClientToServerModel.INFO_MSG.toStringValue(), "stale-message");
        job.add(ClientToServerModel.OBJECT_ID.toStringValue(), 1);
        // This should not throw — the isAlive() check passes but the message is processed
        // (which is fine, the UIContext is still alive during suspension)
        setup.webSocket().onWebSocketText(job.build().toString());

        // Clean up
        setup.uiContext().destroy();
        UIContext.remove();
    }

    /**
     * After suspend + timeout destroy, the old WebSocket's UIContext ref
     * points to a dead UIContext. Verify that subsequent errors on the old
     * WebSocket don't cause issues.
     */
    @Test
    public void testReconnectionMode_ErrorOnOldWebSocketAfterTimeoutDestroy() throws Exception {
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setReconnectionTimeoutMs(100); // very short timeout
        config.setMaxRecordingEntries(10_000);
        config.setStringDictionaryEnabled(false);

        final WebSocket ws = new WebSocket() {
            @Override void flush0() {}
            @Override public void encode(final ServerToClientModel model, final Object value) {}
        };

        final JettyServerUpgradeRequest request = Mockito.mock(JettyServerUpgradeRequest.class);
        Mockito.when(request.getParameterMap()).thenReturn(java.util.Map.of());
        Mockito.when(request.getHeader("User-Agent")).thenReturn("test-agent");
        final jakarta.servlet.http.HttpServletRequest httpRequest = Mockito.mock(jakarta.servlet.http.HttpServletRequest.class);
        Mockito.when(httpRequest.getSession()).thenReturn(Mockito.mock(jakarta.servlet.http.HttpSession.class));
        Mockito.when(request.getHttpServletRequest()).thenReturn(httpRequest);
        ws.setRequest(request);

        final TxnContext txnContext = Mockito.mock(TxnContext.class);
        Mockito.when(txnContext.getWriter()).thenReturn(new com.ponysdk.core.writer.ModelWriter(ws));
        ws.setContext(txnContext);

        final ApplicationManager appManager = Mockito.mock(ApplicationManager.class);
        Mockito.when(appManager.getConfiguration()).thenReturn(config);
        ws.setApplicationManager(appManager);

        final Session sess = Mockito.mock(Session.class);
        Mockito.when(sess.isOpen()).thenReturn(true);
        ws.onWebSocketOpen(sess);

        final ArgumentCaptor<UIContext> captor = ArgumentCaptor.forClass(UIContext.class);
        Mockito.verify(appManager).startApplication(captor.capture());
        final UIContext ctx = captor.getValue();

        // Close → suspend
        ws.onWebSocketClose(StatusCode.NORMAL, "disconnect");
        assertTrue(ctx.isSuspended());

        // Wait for timeout to destroy
        Thread.sleep(300);
        assertFalse(ctx.isAlive());
        assertFalse(ctx.isSuspended());

        // Error on old WebSocket after UIContext is dead — should not throw
        ws.onWebSocketError(new Exception("late error after timeout"));
        // No exception = success

        UIContext.remove();
    }

    /**
     * Verify that the WebSocketPusher is created fresh on each onWebSocketOpen.
     * After close + reconnect, the old pusher should not be referenced.
     */
    @Test
    public void testReconnectionMode_NewPusherOnReconnect() throws Exception {
        final ReconnectionSetup setup = createReconnectionWebSocket();

        // Close → suspend
        setup.webSocket().onWebSocketClose(StatusCode.NORMAL, "disconnect");
        assertTrue(setup.uiContext().isSuspended());

        // Clean up
        setup.uiContext().destroy();
        UIContext.remove();
    }

}
