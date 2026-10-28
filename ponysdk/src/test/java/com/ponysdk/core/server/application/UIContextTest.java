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

package com.ponysdk.core.server.application;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ProtocolVersion;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.AlreadyDestroyedApplication;
import com.ponysdk.core.server.LogLevel;
import com.ponysdk.core.server.metrics.PonySDKMetrics;
import com.ponysdk.core.server.servlet.SessionManager;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.Subprotocols;
import com.ponysdk.core.server.websocket.UpgradeRequestData;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.basic.PFrame;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.eventbus.StreamHandler;
import com.ponysdk.core.ui.statistic.TerminalDataReceiver;

public class UIContextTest {

    @Test
    public void testMissingUpgradeRequestDataReadsAsEmpty() {
        final UIContext uiContext = new UIContext(Mockito.mock(WebSocket.class), Mockito.mock(TxnContext.class),
                Mockito.mock(ApplicationConfiguration.class), null);

        assertTrue(uiContext.getParameterMap().isEmpty());
        assertTrue(uiContext.getHeaders().isEmpty());
        assertTrue(uiContext.getRequestCookies().isEmpty());
        assertNull(uiContext.getHistoryToken());
        assertNull(uiContext.getSession());
        assertNull(uiContext.getHost());
        assertNull(uiContext.getRemoteSocketAddress());
        assertFalse(uiContext.isSecure());
    }

    private Application application;
    private final WebSocket socket = Mockito.mock(WebSocket.class);

    @AfterEach
    public void tearDown() {
        if (application != null) SessionManager.get().unregisterApplication(application);
        UIContext.remove();
    }

    /** A live context of a registered application, on a mocked socket. */
    private UIContext liveContext() {
        return liveContext(new ApplicationConfiguration());
    }

    private UIContext liveContext(final ApplicationConfiguration config) {
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setStringDictionaryEnabled(false);
        application = new Application("ui-context-test", Mockito.mock(HttpSession.class), config);
        SessionManager.get().registerApplication(application);
        Mockito.when(socket.getUpgradeRequestData()).thenReturn(new UpgradeRequestData(Map.of(), "Mozilla/5.0 (X11; Linux x86_64) Firefox/120.0",
            Mockito.mock(HttpSession.class)));
        final TxnContext txnContext = new TxnContext(socket);
        txnContext.setApplication(application);
        final UIContext uiContext = new UIContext(socket, txnContext, config);
        application.registerUIContext(uiContext);
        return uiContext;
    }

    private static PObject registered(final UIContext uiContext, final int id) {
        final PObject object = Mockito.mock(PObject.class);
        Mockito.when(object.getID()).thenReturn(id);
        uiContext.execute(() -> uiContext.registerObject(object));
        return object;
    }

    private static JsonObject instruction(final Object objectId) {
        final var builder = Json.createObjectBuilder().add("payload", 1);
        if (objectId instanceof Integer i) builder.add(ClientToServerModel.OBJECT_ID.toStringValue(), i);
        else if (objectId instanceof String s) builder.add(ClientToServerModel.OBJECT_ID.toStringValue(), s);
        else builder.add(ClientToServerModel.OBJECT_ID.toStringValue(), true);
        return builder.build();
    }

    @Test
    public void testClientDataReachesItsObjectByNumberOrStringId() {
        final UIContext uiContext = liveContext();
        final PObject object = registered(uiContext, 11);
        final List<String> calls = new ArrayList<>();
        final TerminalDataReceiver receiver = (target, json) -> calls.add("receiver " + target.getID());
        Mockito.doAnswer(inv -> calls.add("object")).when(object).onClientData(Mockito.any());
        uiContext.setTerminalDataReceiver(receiver);

        uiContext.execute(() -> {
            uiContext.fireClientData(instruction(11));
            uiContext.fireClientData(instruction("11"));
        });

        assertEquals(List.of("receiver 11", "object", "receiver 11", "object"), calls, "the receiver sees it first");
    }

    @Test
    public void testClientDataOfAnUnknownOrMalformedReferenceIsDropped() {
        final UIContext uiContext = liveContext();
        final PObject object = registered(uiContext, 12);
        final JsonObject orphan = Json.createObjectBuilder().add(ClientToServerModel.OBJECT_ID.toStringValue(), 999)
            .add(ClientToServerModel.PARENT_OBJECT_ID.toStringValue(), 12).build();

        uiContext.execute(() -> {
            uiContext.fireClientData(instruction(Boolean.TRUE));
            uiContext.fireClientData(instruction(998));
            uiContext.fireClientData(orphan);
        });

        Mockito.verify(object, Mockito.never()).onClientData(Mockito.any());
        assertTrue(uiContext.isAlive());
    }

    @Test
    public void testTheLogLevelChangesNothingToADroppedReferenceOrAnAlreadyDestroyedListener() {
        final UIContext uiContext = liveContext();
        final PObject object = registered(uiContext, 12);
        final JsonObject orphan = Json.createObjectBuilder().add(ClientToServerModel.OBJECT_ID.toStringValue(), 999)
            .add(ClientToServerModel.PARENT_OBJECT_ID.toStringValue(), 12).build();
        final List<UIContext> notified = new ArrayList<>();
        uiContext.addContextDestroyListener(context -> {
            throw new AlreadyDestroyedApplication("already gone");
        });
        uiContext.addContextDestroyListener(notified::add);

        try (LogLevel errorsOnly = LogLevel.set(UIContext.class, "ERROR")) {
            uiContext.execute(() -> uiContext.fireClientData(orphan));
        }
        try (LogLevel debug = LogLevel.set(UIContext.class, "DEBUG")) {
            uiContext.destroy();
        }

        Mockito.verify(object, Mockito.never()).onClientData(Mockito.any());
        assertEquals(List.of(uiContext), notified);
    }

    @Test
    public void testPushToClientReachesTheDataListeners() {
        final UIContext uiContext = liveContext();
        final List<Object> received = new ArrayList<>();
        assertFalse(uiContext.pushToClient((Object) "nobody listens"));
        assertFalse(uiContext.pushToClient(List.of("nobody listens")));
        uiContext.addDataListener(received::add);
        uiContext.addDataListener(data -> {
            throw new IllegalStateException("a broken listener");
        });

        assertTrue(uiContext.pushToClient((Object) "one"));
        assertTrue(uiContext.pushToClient(List.<Object> of("two", "three")));
        assertFalse(uiContext.pushToClient((Object) null));
        assertFalse(uiContext.pushToClient((List<Object>) null));

        assertEquals(List.of("one", "two", "three"), received, "a listener registered before a failing one gets everything");
    }

    @Test
    public void testAFailedRoundTripIsLoggedAndReleasesTheLock() {
        final UIContext uiContext = liveContext();
        Mockito.doThrow(new IllegalStateException("socket gone")).when(socket).sendRoundTrip();

        uiContext.sendRoundTrip();

        assertFalse(uiContext.isHeldByCurrentThread());
    }

    @Test
    public void testTheSocketAndApplicationCallsAreDelegated() {
        final UIContext uiContext = liveContext();
        final WebSocket.Listener listener = Mockito.mock(WebSocket.Listener.class);
        Mockito.when(socket.getProtocolBytesSent()).thenReturn(10L);
        Mockito.when(socket.getWireBytesSent()).thenReturn(7L);
        Mockito.when(socket.getWireBytesReceived()).thenReturn(3L);

        uiContext.ping();
        uiContext.connectionDead();
        uiContext.setWebSocketListener(listener);
        uiContext.setApplicationAttribute("key", "value");

        Mockito.verify(socket).ping();
        Mockito.verify(socket).connectionDead();
        Mockito.verify(socket).setListener(listener);
        assertEquals("value", uiContext.getApplicationAttribute("key"));
        assertEquals("value", application.getAttribute("key"));
        assertEquals("ui-context-test", uiContext.getApplicationId());
        assertEquals(10L, uiContext.getProtocolBytesSent());
        assertEquals(7L, uiContext.getWireBytesSent());
        assertEquals(3L, uiContext.getWireBytesReceived());
        assertNotNull(uiContext.getUserAgent());
        assertNull(uiContext.getCookie("absent"));
    }

    // ---- Suspension and resumption racing ----

    /**
     * The reconnection of a client waits until its context is suspended (WebSocket#onWebSocketOpen), on the thread
     * that closes the old socket: the context must not look suspended before its recorder is in place.
     */
    @Test
    public void testAContextIsNotSuspendedBeforeItsRecorderIsInPlace() throws Exception {
        final ApplicationConfiguration config = Mockito.spy(new ApplicationConfiguration());
        final CountDownLatch inSuspend = new CountDownLatch(1);
        final CountDownLatch goOn = new CountDownLatch(1);
        Mockito.doAnswer(inv -> {
            inSuspend.countDown();
            assertTrue(goOn.await(5, TimeUnit.SECONDS));
            return inv.callRealMethod();
        }).when(config).getMaxRecordingEntries();
        final UIContext uiContext = liveContext(config);
        final Thread closing = Thread.ofPlatform().start(() -> uiContext.suspend(60_000));
        assertTrue(inSuspend.await(5, TimeUnit.SECONDS));

        final boolean resumable = uiContext.isSuspended();
        goOn.countDown();
        closing.join(5_000);

        assertFalse(resumable, "resumed now, it would find no recorder and keep recording on the new socket");
        assertTrue(uiContext.isSuspended());
        final WebSocket newSocket = Mockito.mock(WebSocket.class);
        uiContext.resume(newSocket);
        uiContext.execute(() -> uiContext.getWriter().write(ServerToClientModel.HTML, "after"));
        Mockito.verify(newSocket).encode(ServerToClientModel.HTML, "after");
        uiContext.destroy();
    }

    /** The recorder overflowed: the context goes, and the terminal reconnecting must be told, not left waiting. */
    @Test
    public void testAResumptionOfAnOverflowedContextClosesTheNewSocket() {
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setMaxRecordingEntries(2);
        final UIContext uiContext = liveContext(config);
        uiContext.suspend(60_000);
        final WebSocket newSocket = Mockito.mock(WebSocket.class);

        uiContext.acquire(); // the destroy the overflow starts waits for this lock
        try {
            for (int i = 0; i < 5; i++) uiContext.getWriter().write(ServerToClientModel.HTML, "flood" + i);
            uiContext.resume(newSocket);
        } finally {
            uiContext.release();
        }

        Mockito.verify(newSocket).close();
        Mockito.verify(newSocket, Mockito.never()).encode(Mockito.eq(ServerToClientModel.RECONNECT_CONTEXT), Mockito.any());
    }

    @Test
    public void testAnAdministratorDestroyOfASuspendedContextSendsNothing() throws Exception {
        final UIContext uiContext = liveContext();
        final CountDownLatch destroyed = new CountDownLatch(1);
        uiContext.addContextDestroyListener(context -> destroyed.countDown());
        uiContext.suspend(60_000);

        uiContext.destroyByAdministrator();

        assertTrue(destroyed.await(5, TimeUnit.SECONDS));
        Mockito.verify(socket, Mockito.never()).encode(Mockito.eq(ServerToClientModel.DESTROY_CONTEXT), Mockito.any());
        assertEquals(SessionInfo.State.DESTROYED, uiContext.snapshot().state());
    }

    // ---- A destroyed context, a context without socket ----

    @Test
    public void testADestroyedContextIgnoresWhatComesAfter() {
        final UIContext uiContext = liveContext();
        uiContext.destroy();
        Mockito.clearInvocations(socket);

        uiContext.flush();
        uiContext.destroy();
        uiContext.destroyFromApplication();
        uiContext.disconnect();
        uiContext.addContextDestroyListener(context -> fail("destroyed already"));
        assertFalse(uiContext.pushToClient(List.<Object> of("late")));

        Mockito.verifyNoInteractions(socket);
    }

    @Test
    public void testAContextWithoutSocketAnswersWithDefaults() {
        final TxnContext txnContext = Mockito.mock(TxnContext.class);
        final UIContext uiContext = new UIContext(null, txnContext, new ApplicationConfiguration());

        uiContext.ping();
        uiContext.connectionDead();
        assertEquals(0, uiContext.getProtocolBytesSent());
        assertEquals(-1, uiContext.getWireBytesSent());
        assertEquals(-1, uiContext.getWireBytesReceived());
        assertEquals(ProtocolVersion.V1, uiContext.getProtocolVersion());
        assertNull(uiContext.getWriter());
        assertNull(uiContext.removeStreamListener(0));
    }

    // ---- Execution, client data, streams ----

    @Test
    public void testAFailingTaskIsTimedForTheMetrics() {
        final UIContext uiContext = liveContext();
        final PonySDKMetrics metrics = Mockito.mock(PonySDKMetrics.class);
        uiContext.setMetrics(metrics);

        assertFalse(uiContext.execute(() -> {
            throw new IllegalStateException("a bug");
        }));

        Mockito.verify(metrics).recordExecute(Mockito.anyLong());
        assertFalse(uiContext.isHeldByCurrentThread());
    }

    @Test
    public void testAHistoryChangeFromTheClientReachesTheHistory() {
        final UIContext uiContext = liveContext();
        final List<String> tokens = new ArrayList<>();
        uiContext.getHistory().addValueChangeHandler(event -> tokens.add(event.getData()));

        uiContext.execute(() -> uiContext.fireClientData(Json.createObjectBuilder()
            .add(ClientToServerModel.TYPE_HISTORY.toStringValue(), "page2").build()));

        assertEquals(List.of("page2"), tokens);
    }

    @Test
    public void testStreamRequestsAreKeptByTheirId() {
        final UIContext uiContext = liveContext();
        final StreamHandler first = Mockito.mock(StreamHandler.class);
        final StreamHandler second = Mockito.mock(StreamHandler.class);
        final StreamHandler embedded = Mockito.mock(StreamHandler.class);
        final PObject object = registered(uiContext, 5);
        final PFrame frame = Mockito.mock(PFrame.class);
        Mockito.when(object.getFrame()).thenReturn(frame);
        Mockito.when(object.getWindow()).thenReturn(Mockito.mock(PWindow.class));

        uiContext.execute(() -> {
            uiContext.stackStreamRequest(first);
            uiContext.stackStreamRequest(second);
            uiContext.stackEmbeddedStreamRequest(embedded, object);
        });

        assertSame(second, uiContext.removeStreamListener(1));
        assertSame(embedded, uiContext.removeStreamListener(2));
        assertSame(first, uiContext.removeStreamListener(0));
        assertNull(uiContext.removeStreamListener(0));
    }

    @Test
    public void testAnEmbeddedContextSetsItsCookiesByStreamRequests() {
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setStringDictionaryEnabled(false);
        final UpgradeRequestData handshake = new UpgradeRequestData(
            Map.of(ClientToServerModel.HOST_ATTRIBUTES.toStringValue(), List.of("{}")),
            Map.of(Subprotocols.HEADER, List.of(Subprotocols.PONYSDK)), null, null, false, null, null, null);
        final UIContext uiContext = new UIContext(socket, new TxnContext(socket), config, handshake);
        assertTrue(uiContext.isEmbedded());

        uiContext.execute(() -> {
            uiContext.getCookies().setCookie("a", "1");
            uiContext.getCookies().setCookie("b", "2");
        });

        assertNotNull(uiContext.removeStreamListener(0));
        assertNotNull(uiContext.removeStreamListener(1));
    }

    // ---- Small accessors ----

    @Test
    public void testTheHistoryTokenIsTheFirstValueOfItsParameter() {
        final String key = ClientToServerModel.TYPE_HISTORY.toStringValue();
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setStringDictionaryEnabled(false);
        assertNull(new UIContext(socket, new TxnContext(socket), config, new UpgradeRequestData(Map.of(key, List.of()), null, null))
            .getHistoryToken());
        assertEquals("home", new UIContext(socket, new TxnContext(socket), config,
            new UpgradeRequestData(Map.of(key, List.of("home", "other")), null, null)).getHistoryToken());
    }

    @Test
    public void testAContextEqualsOnlyItself() {
        final UIContext uiContext = liveContext();
        assertEquals(uiContext, uiContext);
        assertNotEquals(uiContext, null);
        assertNotEquals(uiContext, "a string");
        assertEquals(uiContext.getID(), uiContext.hashCode());
    }

    @Test
    public void testLatenciesAverageTheLastTenValues() {
        final UIContext uiContext = liveContext();
        for (int i = 1; i <= 12; i++) uiContext.addRoundtripLatencyValue(i);
        assertEquals((3 + 12) / 2.0, uiContext.getRoundtripLatency()); // 3..12
    }
}
