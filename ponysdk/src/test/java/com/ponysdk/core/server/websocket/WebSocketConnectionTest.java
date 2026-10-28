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
import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.metrics.PonySDKMetrics;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.ui.basic.PObject;
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
    {
        configuration.setHeartBeatPeriod(0, TimeUnit.SECONDS);
    }

    private UIContext open(final Session session, final PonySDKMetrics metrics) {
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

    // ---- Reconnection races, opens and closes without a context ----

    private static final int RECONNECTED_ID = 7;

    /** A socket reconnecting to {@code old}, found in the caller's session. */
    private void reconnect(final UIContext old, final Session session) {
        final Application application = Mockito.mock(Application.class);
        Mockito.when(application.getUIContext(RECONNECTED_ID)).thenReturn(old);
        Mockito.when(txnContext.getApplication()).thenReturn(application);
        webSocket = new WebSocket();
        webSocket.setContext(txnContext);
        webSocket.setReconnectContextId(RECONNECTED_ID);
        webSocket.onWebSocketOpen(session);
    }

    private static UIContext suspendable() {
        final UIContext old = Mockito.mock(UIContext.class);
        Mockito.when(old.isAlive()).thenReturn(true);
        return old;
    }

    /** The client reconnects before the server has seen the close of its old socket: the suspension lands meanwhile. */
    @Test
    public void aReconnectionWaitsForTheSuspensionStillInProgress() {
        final UIContext old = suspendable();
        Mockito.when(old.isSuspended()).thenReturn(false, false, true);

        reconnect(old, session(Mockito.mock(Session.class)));

        Mockito.verify(old).resume(webSocket);
    }

    @Test
    public void aContextDyingWhileItsReconnectionWaitsIsLost() {
        final UIContext old = suspendable();
        Mockito.when(old.isAlive()).thenReturn(true, true, false);
        final Session session = session(Mockito.mock(Session.class));

        reconnect(old, session);

        Mockito.verify(old, Mockito.never()).resume(Mockito.any());
        Mockito.verify(session).close(Mockito.eq(StatusCode.NORMAL), Mockito.eq("context lost"), Mockito.any());
        assertEquals(1, sent.size(), "the CREATE_CONTEXT that makes the page reload");
    }

    @Test
    public void aDestroyedContextIsLostWithoutWaiting() {
        final UIContext old = Mockito.mock(UIContext.class);
        final Session session = session(Mockito.mock(Session.class));

        reconnect(old, session);

        Mockito.verify(old, Mockito.never()).resume(Mockito.any());
        Mockito.verify(session).close(Mockito.eq(StatusCode.NORMAL), Mockito.eq("context lost"), Mockito.any());
    }

    @Test
    public void anInterruptedReconnectionWaitGivesUpAndKeepsTheInterrupt() {
        final UIContext old = suspendable();
        final Session session = session(Mockito.mock(Session.class));

        Thread.currentThread().interrupt();
        reconnect(old, session);

        assertTrue(Thread.interrupted(), "the interrupt status is kept");
        Mockito.verify(old, Mockito.never()).resume(Mockito.any());
        Mockito.verify(session).close(Mockito.eq(StatusCode.NORMAL), Mockito.eq("context lost"), Mockito.any());
    }

    @Test
    public void aSocketWithoutContextIsClosedDroppedAndDisconnectedAsAnother() {
        final UIContext old = suspendable();
        Mockito.when(old.isReconnectionHeld()).thenReturn(true);
        final Session session = session(Mockito.mock(Session.class));
        reconnect(old, session);
        Mockito.verify(session).close(Mockito.eq(WebSocket.CLOSE_RECONNECTION_HELD), Mockito.anyString(), Mockito.any());

        webSocket.close();
        webSocket.drop();
        webSocket.disconnect();

        Mockito.verify(session).close(Mockito.eq(StatusCode.NORMAL), Mockito.anyString(), Mockito.any());
        Mockito.verify(session).close(Mockito.eq(WebSocket.CLOSE_DROPPED), Mockito.anyString(), Mockito.any());
        Mockito.verify(session).disconnect();
    }

    @Test
    public void aDroppedSocketClosesAsANetworkFailure() {
        final Session session = session(Mockito.mock(Session.class));
        open(session, null);

        webSocket.drop();

        Mockito.verify(session).close(Mockito.eq(WebSocket.CLOSE_DROPPED), Mockito.anyString(), Mockito.any());
    }

    @Test
    public void aSocketNeverOpenedIgnoresCloseAndDeath() {
        final WebSocket notOpened = new WebSocket();
        notOpened.close();
        notOpened.connectionDead();
        notOpened.onWebSocketClose(StatusCode.NORMAL, null, Callback.NOOP);
        assertEquals(0, notOpened.getProtocolBytesSent());
    }

    @Test
    public void aPingAsksTheOpenSessionForAPong() {
        final Session session = session(Mockito.mock(Session.class));
        open(session, null);

        webSocket.ping();

        Mockito.verify(session).sendPing(Mockito.any(ByteBuffer.class), Mockito.any());
    }

    @Test
    public void closeCodesOutsideTheKnownAndApplicationRangesStillDestroy() {
        final UIContext created = open();
        webSocket.onWebSocketClose(1234, "odd", Callback.NOOP);
        assertFalse(created.isAlive());
        assertEquals(1234, created.getCloseStatusCode());
    }

    @Test
    public void aCloseCodeAboveTheApplicationRangeStillDestroys() {
        final UIContext created = open();
        webSocket.onWebSocketClose(5000, null, Callback.NOOP);
        assertEquals(5000, created.getCloseStatusCode());
    }

    // ---- Context creation options ----

    @Test
    public void theHeartbeatPeriodIsAnnouncedInSecondsWhenEnabled() {
        configuration.setEnableClientToServerHeartBeat(true);
        configuration.setHeartBeatPeriod(2, TimeUnit.MINUTES);
        configuration.setWsSendTimeoutMs(0); // the default timeout

        open();

        final ByteBuffer first = ByteBuffer.wrap(sent.get(0));
        first.position(1 + 2 + 1 + 1); // CREATE_CONTEXT id, OPTION_FORMFIELD_TABULATION
        assertEquals(ServerToClientModel.HEARTBEAT_PERIOD.getValue(), first.get() & 0xFF);
        assertEquals(120, first.getInt());
    }

    @Test
    public void aDisabledHeartbeatIsAnnouncedAsZero() {
        configuration.setEnableClientToServerHeartBeat(false);
        configuration.setHeartBeatPeriod(2, TimeUnit.MINUTES);

        open();

        final ByteBuffer first = ByteBuffer.wrap(sent.get(0));
        first.position(1 + 2 + 1 + 1 + 1);
        assertEquals(0, first.getInt());
    }

    /** The application is destroyed (its HTTP session expires) while a socket opens one of its contexts. */
    @Test
    public void aContextDestroyedWhileItOpensIsNeitherStartedNorCounted() {
        Mockito.doAnswer(inv -> {
            inv.getArgument(0, UIContext.class).destroy();
            return null;
        }).when(txnContext).registerUIContext(Mockito.any());
        final PonySDKMetrics metrics = new PonySDKMetrics("destroyed-at-open-test");

        final UIContext created = open(session(Mockito.mock(Session.class)), metrics);

        assertFalse(created.isAlive());
        Mockito.verify(applicationManager, Mockito.never()).startApplication(Mockito.any());
        assertEquals(0, metrics.getActiveContexts(), "no context left counted");
    }
    @Test
    public void theWireBytesOfTheJettyConnectionGoToTheMetrics() {
        final WebSocketConnection connection = Mockito.mock(WebSocketConnection.class);
        final WebSocketCoreSession core = Mockito.mock(WebSocketCoreSession.class);
        Mockito.when(core.getConnection()).thenReturn(connection);
        final WebSocketSession session = session(Mockito.mock(WebSocketSession.class));
        Mockito.when(session.getCoreSession()).thenReturn(core);
        Mockito.when(connection.getBytesOut()).thenReturn(10L, 40L);
        final PonySDKMetrics metrics = new PonySDKMetrics("wire-test");

        final UIContext created = open(session, metrics);
        created.execute(() -> {
            webSocket.encode(ServerToClientModel.HEARTBEAT_PERIOD, 1);
            webSocket.flush();
        });

        assertEquals(sent.get(sent.size() - 1).length, metrics.getTotalBytesSent(), "counted once the listener is set");
    }

    // ---- Messages from the terminal ----

    @Test
    public void aLatencyWithALetterIsNoFastPathLatency() {
        assertEquals(WebSocket.NOT_A_LATENCY_MESSAGE, WebSocket.parseLatencyMessage("{\"h\":12a}"));
    }

    @Test
    public void aJsonLatencyWithoutTerminalTimeIsAllTheNetworksAndGoesToTheMetrics() {
        final PonySDKMetrics metrics = Mockito.spy(new PonySDKMetrics("latency-test"));
        final UIContext created = open(session(Mockito.mock(Session.class)), metrics);

        webSocket.onWebSocketText("{ \"" + ClientToServerModel.TERMINAL_LATENCY.toStringValue() + "\" : " + System.nanoTime() + " }");

        assertEquals(0, created.getTerminalLatency());
        Mockito.verify(metrics).recordRoundtrip(Mockito.anyLong());
    }

    @Test
    public void aTerminalLogNamesTheObjectItConcerns() {
        final UIContext created = open();
        final PObject object = Mockito.mock(PObject.class);
        Mockito.when(object.getID()).thenReturn(42);
        created.registerObject(object);
        final WebsocketMonitor monitor = Mockito.mock(WebsocketMonitor.class);
        webSocket.setMonitor(monitor);
        final String objectKey = ClientToServerModel.OBJECT_ID.toStringValue();
        final List<String> messages = new ArrayList<>();
        for (final ClientToServerModel level : List.of(ClientToServerModel.ERROR_MSG, ClientToServerModel.WARN_MSG,
            ClientToServerModel.INFO_MSG)) {
            messages.add("{\"" + level.toStringValue() + "\":\"known\",\"" + objectKey + "\":42}");
            messages.add("{\"" + level.toStringValue() + "\":\"unknown\",\"" + objectKey + "\":43}");
            messages.add("{\"" + level.toStringValue() + "\":\"none\"}");
        }

        messages.forEach(webSocket::onWebSocketText);

        for (final String message : messages) Mockito.verify(monitor).onMessageProcessed(webSocket, message);
    }

    @Test
    public void aRoundTripAlreadyWaitingForTheNextFlushIsSentAlone() {
        final UIContext created = open();
        final int before = sent.size();

        created.execute(() -> {
            webSocket.sendRoundTrip(); // the context just sent: it waits for the next flush
            webSocket.sendRoundTrip(); // still nothing flushed: sent now
        });

        assertEquals(before + 1, sent.size());
        assertEquals(ServerToClientModel.ROUNDTRIP_LATENCY.getValue(), sent.get(before)[0] & 0xFF);
    }

    @Test
    public void incomingTextWithoutJettyConnectionCountsNoWireBytes() {
        open();
        final WebSocket.Listener listener = Mockito.mock(WebSocket.Listener.class);
        webSocket.setListener(listener);

        webSocket.onWebSocketText("{\"v\":0}");

        Mockito.verify(listener).onIncomingText("{\"v\":0}");
        Mockito.verify(listener, Mockito.never()).onIncomingWebSocketFrame(Mockito.anyInt(), Mockito.anyInt());
    }
}
