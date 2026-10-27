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
import java.util.concurrent.TimeUnit;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.server.servlet.SessionManager;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.UpgradeRequestData;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.basic.PObject;
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
        final ApplicationConfiguration config = new ApplicationConfiguration();
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
}
