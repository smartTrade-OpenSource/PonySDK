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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.spi.JsonProvider;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.Before;
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

        final ServletUpgradeRequest request = Mockito.mock(ServletUpgradeRequest.class);
        webSocket.setRequest(request);
        webSocket.setContext(Mockito.mock(TxnContext.class));

        final ApplicationManager applicationManager = Mockito.mock(ApplicationManager.class);
        final ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
        applicationConfiguration.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        Mockito.when(applicationManager.getConfiguration()).thenReturn(applicationConfiguration);
        webSocket.setApplicationManager(applicationManager);

        session = Mockito.mock(Session.class);
        Mockito.when(session.isOpen()).thenReturn(true);
        webSocket.onWebSocketConnect(session);

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
    public void testOnWebSocketTextRoundTripLatency() {
        encodedValues.clear();
        final JsonObjectBuilder job = JsonProvider.provider().createObjectBuilder();
        job.add(ClientToServerModel.HEARTBEAT_REQUEST.toStringValue(), JsonValue.NULL);
        webSocket.onWebSocketText(job.build().toString());
        assertEquals(encodedValues,
            List.of(new Pair<>(ServerToClientModel.HEARTBEAT, null), new Pair<>(ServerToClientModel.END, null)));
    }

    /**
     * Test method for {@link com.ponysdk.core.server.websocket.WebSocket#onWebSocketConnect(Session)}
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
     * Test method for {@link com.ponysdk.core.server.websocket.WebSocket#onWebSocketBinary(byte[], int, int)}.
     */
    @Test
    public void testOnWebSocketBinary() {
        // Not implemented yet
        webSocket.onWebSocketBinary(null, 0, 0);
    }

    /**
     * Test method for {@link com.ponysdk.core.server.websocket.WebSocket#close()}.
     */
    @Test
    public void testClose() {
        webSocket.close();
        Mockito.verify(session, Mockito.times(1)).close();
    }

}
