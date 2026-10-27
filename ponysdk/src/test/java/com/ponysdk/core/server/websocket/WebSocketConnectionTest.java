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

package com.ponysdk.core.server.websocket;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.common.WebSocketSession;
import org.eclipse.jetty.websocket.core.WebSocketConnection;
import org.eclipse.jetty.websocket.core.WebSocketCoreSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.metrics.PonySDKMetrics;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.writer.ModelWriter;

/**
 * {@link WebSocket} on a real {@link WebSocketPusher}: its context creation, the failures of its writes, the byte
 * counters of its connection and its monitor.
 */
public class WebSocketConnectionTest {

    private final List<byte[]> sent = new ArrayList<>();
    private final ApplicationConfiguration configuration = new ApplicationConfiguration();
    private final ApplicationManager applicationManager = Mockito.mock(ApplicationManager.class);
    private final TxnContext txnContext = Mockito.mock(TxnContext.class);
    private WebSocket webSocket;
    private UIContext uiContext;

    @AfterEach
    public void tearDown() {
        if (uiContext != null) uiContext.destroy();
        UIContext.remove();
    }

    private <S extends Session> S session(final S session) {
        Mockito.when(session.isOpen()).thenReturn(true);
        Mockito.doAnswer(inv -> {
            final ByteBuffer buffer = inv.getArgument(0);
            final byte[] bytes = new byte[buffer.remaining()];
            buffer.duplicate().get(bytes);
            sent.add(bytes);
            inv.getArgument(1, Callback.class).succeed();
            return null;
        }).when(session).sendBinary(Mockito.any(ByteBuffer.class), Mockito.any(Callback.class));
        return session;
    }

    /** Opens a socket on {@code session}; the context it creates, or null when it creates none. */
    private UIContext open(final Session session, final PonySDKMetrics metrics) {
        configuration.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        Mockito.when(applicationManager.getConfiguration()).thenReturn(configuration);
        webSocket = new WebSocket();
        Mockito.when(txnContext.getWriter()).thenReturn(new ModelWriter(webSocket));
        webSocket.setContext(txnContext);
        webSocket.setApplicationManager(applicationManager);
        if (metrics != null) webSocket.setMetrics(metrics);
        webSocket.onWebSocketOpen(session);
        final ArgumentCaptor<UIContext> created = ArgumentCaptor.forClass(UIContext.class);
        Mockito.verify(txnContext, Mockito.atMost(1)).registerUIContext(created.capture());
        uiContext = created.getAllValues().isEmpty() ? null : created.getValue();
        return uiContext;
    }

    private UIContext open() {
        return open(session(Mockito.mock(Session.class)), null);
    }

    @Test
    public void aSessionClosedBeforeTheOpenCreatesNoContext() {
        final Session session = Mockito.mock(Session.class);
        Mockito.when(session.isOpen()).thenReturn(false);

        assertNull(open(session, null));
        Mockito.verify(applicationManager, Mockito.never()).startApplication(Mockito.any());
    }

    @Test
    public void aContextWhoseFirstMessageCannotBeSentIsDestroyed() {
        final Session session = Mockito.mock(Session.class);
        Mockito.when(session.isOpen()).thenReturn(true);
        Mockito.doThrow(new IllegalStateException("socket gone")).when(session).sendBinary(Mockito.any(), Mockito.any());

        final UIContext created = open(session, null);

        assertNotNull(created);
        assertFalse(created.isAlive(), "an orphan never waits for a reconnection");
        Mockito.verify(applicationManager, Mockito.never()).startApplication(Mockito.any());
    }

    @Test
    public void theMetricsCountTheContextAndTheBytesSent() {
        configuration.setWsSendTimeoutMs(5_000);
        final PonySDKMetrics metrics = new PonySDKMetrics("connection-test");

        final UIContext created = open(session(Mockito.mock(Session.class)), metrics);
        assertEquals(1, metrics.getActiveContexts());
        assertSame(metrics, created.getMetrics());
        created.execute(() -> {
            webSocket.encode(ServerToClientModel.HEARTBEAT_PERIOD, 1);
            webSocket.flush();
        });

        assertEquals(sent.get(sent.size() - 1).length, metrics.getTotalBytesSent());
        created.destroy();
        assertEquals(0, metrics.getActiveContexts());
    }

    @Test
    public void aFailedSendDestroysTheContextAsAServerError() {
        final Session session = session(Mockito.mock(Session.class));
        final UIContext created = open(session, null);
        Mockito.doAnswer(inv -> {
            inv.getArgument(1, Callback.class).fail(new IOException("reset"));
            return null;
        }).when(session).sendBinary(Mockito.any(ByteBuffer.class), Mockito.any(Callback.class));

        created.execute(() -> {
            webSocket.encode(ServerToClientModel.HEARTBEAT_PERIOD, 1);
            webSocket.flush(); // the send fails, seen by the next write
        });
        assertTrue(created.isAlive());
        created.execute(() -> webSocket.encode(ServerToClientModel.HEARTBEAT_PERIOD, 2));

        assertFalse(created.isAlive());
        assertEquals(StatusCode.SERVER_ERROR, created.getCloseStatusCode());
    }

    @Test
    public void aFailedFlushDestroysTheContextAsAServerError() {
        final Session session = session(Mockito.mock(Session.class));
        final UIContext created = open(session, null);
        Mockito.doAnswer(inv -> {
            inv.getArgument(1, Callback.class).fail(new IOException("reset"));
            return null;
        }).when(session).sendBinary(Mockito.any(ByteBuffer.class), Mockito.any(Callback.class));
        created.execute(() -> {
            webSocket.encode(ServerToClientModel.HEARTBEAT_PERIOD, 1);
            webSocket.flush(); // the send fails
        });

        created.execute(webSocket::flush);
        created.execute(webSocket::sendRoundTrip);

        assertFalse(created.isAlive());
        assertEquals(StatusCode.SERVER_ERROR, created.getCloseStatusCode());
    }

    @Test
    public void aFailedIntWriteDestroysTheContextAsAServerError() {
        final Session session = session(Mockito.mock(Session.class));
        final UIContext created = open(session, null);
        Mockito.doAnswer(inv -> {
            inv.getArgument(1, Callback.class).fail(new IOException("reset"));
            return null;
        }).when(session).sendBinary(Mockito.any(ByteBuffer.class), Mockito.any(Callback.class));
        created.execute(() -> {
            webSocket.encode(ServerToClientModel.HEARTBEAT_PERIOD, 1);
            webSocket.flush(); // the send fails
        });

        created.execute(() -> webSocket.encodeInt(ServerToClientModel.TYPE_UPDATE, 3));

        assertFalse(created.isAlive());
        assertEquals(StatusCode.SERVER_ERROR, created.getCloseStatusCode(), "as for any other write");
    }

    @Test
    public void aWriteOutsideTheContextTakesItsLock() {
        final UIContext created = open();
        UIContext.remove();
        final int before = sent.size();

        webSocket.encode(ServerToClientModel.HEARTBEAT_PERIOD, 1);
        created.execute(webSocket::flush);

        assertEquals(before + 1, sent.size(), "written under the context's lock, then sent");
        assertFalse(created.isHeldByCurrentThread());
    }

    @Test
    public void anIntWriteWithAListenerGoesThroughTheListenedPath() {
        final UIContext created = open();
        final WebSocket.Listener listener = Mockito.mock(WebSocket.Listener.class);
        webSocket.setListener(listener);

        created.execute(() -> webSocket.encodeInt(ServerToClientModel.TYPE_UPDATE, 3));

        Mockito.verify(listener).onOutgoingPonyFrame(ServerToClientModel.TYPE_UPDATE, 3);
    }

    @Test
    public void aDeadConnectionIsDroppedAndItsContextDestroyed() {
        final Session session = session(Mockito.mock(Session.class));
        final UIContext created = open(session, null);

        webSocket.connectionDead();
        webSocket.onWebSocketClose(StatusCode.NO_CLOSE, null, Callback.NOOP); // Jetty's own report, after

        Mockito.verify(session).disconnect();
        assertFalse(created.isAlive());
        assertEquals(StatusCode.NO_CLOSE, created.getCloseStatusCode());
    }

    @Test
    public void errorsAndClosesBeforeAnyContextAreOnlyLogged() {
        final Session session = Mockito.mock(Session.class);
        Mockito.when(session.isOpen()).thenReturn(false);
        open(session, null);

        webSocket.onWebSocketError(new java.io.EOFException("lost"));
        webSocket.onWebSocketError(new IllegalStateException("bug"));
        webSocket.onWebSocketPong(ByteBuffer.allocate(0));
        webSocket.onWebSocketText("{\"v\":0}");
        final Callback callback = Mockito.mock(Callback.class);
        webSocket.onWebSocketClose(StatusCode.NORMAL, null, callback);

        Mockito.verify(callback).succeed();
        Mockito.verify(applicationManager, Mockito.never()).startApplication(Mockito.any());
    }

    @Test
    public void aClosedSessionIsNeitherClosedNorDroppedAgain() {
        final Session session = session(Mockito.mock(Session.class));
        open(session, null);
        Mockito.when(session.isOpen()).thenReturn(false);

        webSocket.close();
        webSocket.drop();
        webSocket.disconnect();
        webSocket.ping();

        Mockito.verify(session, Mockito.never()).close(Mockito.anyInt(), Mockito.anyString(), Mockito.any());
        Mockito.verify(session, Mockito.never()).disconnect();
        Mockito.verify(session, Mockito.never()).sendPing(Mockito.any(), Mockito.any());
    }

    @Test
    public void withoutConnectionNoByteIsCounted() {
        final WebSocket notOpened = new WebSocket();
        assertEquals(0, notOpened.getProtocolBytesSent());
        assertEquals(-1, notOpened.getWireBytesSent());
        assertEquals(-1, notOpened.getWireBytesReceived());

        open();
        assertTrue(webSocket.getProtocolBytesSent() > 0, "CREATE_CONTEXT was sent");
        assertEquals(-1, webSocket.getWireBytesSent(), "a session of no Jetty type: no connection");
        assertEquals(-1, webSocket.getWireBytesReceived());
    }

    @Test
    public void theListenerHearsTheWireBytesOfTheJettyConnection() {
        final WebSocketConnection connection = Mockito.mock(WebSocketConnection.class);
        final WebSocketCoreSession core = Mockito.mock(WebSocketCoreSession.class);
        Mockito.when(core.getConnection()).thenReturn(connection);
        final WebSocketSession session = session(Mockito.mock(WebSocketSession.class));
        Mockito.when(session.getCoreSession()).thenReturn(core);
        Mockito.when(connection.getBytesIn()).thenReturn(50L, 80L, 80L + Integer.MAX_VALUE + 10L);
        open(session, null);
        final WebSocket.Listener listener = Mockito.mock(WebSocket.Listener.class);

        webSocket.setListener(listener); // 50: what came before does not count
        webSocket.onWebSocketText("{\"v\":0}"); // 80
        webSocket.onWebSocketText("{\"v\":0}");

        Mockito.verify(listener, Mockito.times(2)).onIncomingText("{\"v\":0}");
        Mockito.verify(listener).onIncomingWebSocketFrame(0, 30);
        Mockito.verify(listener).onIncomingWebSocketFrame(0, Integer.MAX_VALUE);
        Mockito.verify(listener).onIncomingWebSocketFrame(0, 10);
        assertEquals(80L + Integer.MAX_VALUE + 10L, webSocket.getWireBytesReceived(), "read from the connection");
    }

    @Test
    public void theMonitorFollowsEachMessage() {
        open();
        final WebsocketMonitor monitor = Mockito.mock(WebsocketMonitor.class);
        webSocket.setMonitor(monitor);
        final String latency = "{\"" + ClientToServerModel.TERMINAL_LATENCY.toStringValue() + "\":" + System.nanoTime() + "}";
        final String heartbeat = "{\"" + ClientToServerModel.HEARTBEAT_REQUEST.toStringValue() + "\":0}";
        final String unknown = "{\"unknown\":1}";

        webSocket.onWebSocketText(latency);
        webSocket.onWebSocketText(heartbeat);
        webSocket.onWebSocketText(unknown);

        // onMessageUnprocessed closes every message, processed or not (WebSocketTest#testMonitorCallbacksInvoked)
        for (final String message : List.of(latency, heartbeat, unknown)) {
            Mockito.verify(monitor).onMessageReceived(webSocket, message);
            Mockito.verify(monitor).onMessageProcessed(webSocket, message);
            Mockito.verify(monitor).onMessageUnprocessed(webSocket, message);
        }
    }

    @Test
    public void aLatencyOrHeartbeatTheFastPathRefusesIsParsedAsJson() {
        final UIContext created = open();
        final long fiveSecondsAgo = System.nanoTime() - TimeUnit.SECONDS.toNanos(5);

        webSocket.onWebSocketText("{ \"" + ClientToServerModel.HEARTBEAT_REQUEST.toStringValue() + "\" : 0 }");
        webSocket.onWebSocketText("{ \"" + ClientToServerModel.TERMINAL_LATENCY.toStringValue() + "\" : " + fiveSecondsAgo + ", \""
                + ClientToServerModel.TERMINAL_TIME.toStringValue() + "\" : 2000 }");

        assertTrue(created.getRoundtripLatency() >= 5_000, "roundtrip " + created.getRoundtripLatency());
        assertEquals(2_000, created.getTerminalLatency());
        assertTrue(created.getNetworkLatency() >= 3_000);
    }
}
