/*
 * Copyright (c) 2017 PonySDK
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.json.JsonValue;
import javax.json.spi.JsonProvider;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.writer.ModelWriter;

/**
 * Transparent reconnection on 2.8.x: a context whose WebSocket is lost is suspended, and resumed on the WebSocket
 * its terminal reconnects with, from the bytes the terminal had received.
 */
public class WebSocketReconnectionTest {

    private static final int OBJECT_ID = 7;

    private ApplicationConfiguration configuration;
    private ApplicationManager applicationManager;
    private Application application;

    /** A terminal's connection: the bytes the server sent on it, and its state. */
    private static final class Connection {

        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        final Session session = Mockito.mock(Session.class);
        final WebSocket webSocket = new WebSocket();
        boolean open = true;
        int closeCode = -1;

        Connection(final Map<String, List<String>> parameters, final Application application,
                   final ApplicationManager applicationManager) {
            final RemoteEndpoint remote = Mockito.mock(RemoteEndpoint.class);
            Mockito.doAnswer(invocation -> {
                final ByteBuffer buffer = invocation.getArgument(0);
                final byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                sent.write(bytes, 0, bytes.length);
                ((WriteCallback) invocation.getArgument(1)).writeSuccess();
                return null;
            }).when(remote).sendBytes(Mockito.any(ByteBuffer.class), Mockito.any(WriteCallback.class));
            Mockito.when(session.getRemote()).thenReturn(remote);
            Mockito.when(session.isOpen()).thenAnswer(invocation -> open);
            Mockito.doAnswer(invocation -> {
                open = false;
                closeCode = invocation.getArgument(0);
                return null;
            }).when(session).close(Mockito.anyInt(), Mockito.anyString());
            Mockito.doAnswer(invocation -> {
                open = false;
                return null;
            }).when(session).close();
            try {
                Mockito.doAnswer(invocation -> {
                    open = false;
                    return null;
                }).when(session).disconnect();
            } catch (final java.io.IOException e) {
                throw new IllegalStateException(e);
            }

            final ServletUpgradeRequest request = Mockito.mock(ServletUpgradeRequest.class);
            Mockito.when(request.getParameterMap()).thenReturn(parameters);
            webSocket.setRequest(request);
            webSocket.setApplicationManager(applicationManager);
            final TxnContext context = new TxnContext(webSocket);
            context.setApplication(application);
            webSocket.setContext(context);
            webSocket.onWebSocketConnect(session);
        }

        byte[] bytes() {
            return sent.toByteArray();
        }

        /** The network is cut: nothing more reaches the terminal, and the server sees the close. */
        void lose(final int statusCode) {
            open = false;
            webSocket.onWebSocketClose(statusCode, "test");
        }
    }

    /** An HTTP session that does nothing (this Mockito cannot mock the servlet API on Java 21). */
    private static HttpSession httpSession() {
        return (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(), new Class<?>[] { HttpSession.class },
            (proxy, method, args) -> method.getReturnType() == boolean.class ? false : null);
    }

    @Before
    public void setUp() {
        configuration = new ApplicationConfiguration();
        configuration.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        configuration.setReconnectionTimeoutMs(TimeUnit.SECONDS.toMillis(60));
        applicationManager = Mockito.mock(ApplicationManager.class);
        Mockito.when(applicationManager.getConfiguration()).thenReturn(configuration);
        application = new Application("session", httpSession(), configuration);
    }

    private Connection connect() {
        return new Connection(Collections.emptyMap(), application, applicationManager);
    }

    private Connection reconnect(final UIContext uiContext, final long receivedBytes) {
        final Map<String, List<String>> parameters = new HashMap<>();
        parameters.put(ClientToServerModel.RECONNECT_UI_CONTEXT_ID.toStringValue(), List.of(String.valueOf(uiContext.getID())));
        parameters.put(ClientToServerModel.RECONNECT_OFFSET.toStringValue(), List.of(String.valueOf(receivedBytes)));
        return new Connection(parameters, application, applicationManager);
    }

    private static UIContext contextOf(final Connection connection) {
        return connection.webSocket.getUIContext();
    }

    /** Sets the text of the test object, in a transaction of its own, as a pushing application does. */
    private static void setText(final UIContext uiContext, final String text) {
        assertTrue(uiContext.execute(() -> {
            final ModelWriter writer = uiContext.getWriter();
            writer.write(ServerToClientModel.TYPE_UPDATE, OBJECT_ID);
            writer.write(ServerToClientModel.TEXT, text);
            writer.endObject();
        }));
    }

    private static boolean contains(final byte[] bytes, final String text) {
        return new String(bytes, StandardCharsets.ISO_8859_1).contains(text);
    }

    private static byte[] reconnectBlock(final UIContext uiContext) {
        final int id = uiContext.getID();
        return new byte[] { (byte) ServerToClientModel.RECONNECT_CONTEXT.getValue(), (byte) (id >> 8), (byte) id,
                            (byte) ServerToClientModel.END.getValue() };
    }

    @Test
    public void aLostContextIsSuspendedThenResumedWithWhatTheTerminalHadNotReceived() {
        final Connection first = connect();
        final UIContext uiContext = contextOf(first);
        setText(uiContext, "before-1");
        setText(uiContext, "before-2");
        final byte[] firstBytes = first.bytes();

        // The terminal received the stream up to the middle of the last block: the rest was in flight
        final int received = firstBytes.length - 5;
        first.lose(StatusCode.ABNORMAL);
        assertTrue(uiContext.isSuspended());
        assertTrue(uiContext.isAlive());

        setText(uiContext, "during-1");
        setText(uiContext, "during-2");
        assertEquals("nothing is sent while suspended", firstBytes.length, first.bytes().length);

        final Connection second = reconnect(uiContext, received);
        final byte[] secondBytes = second.bytes();

        assertSame(uiContext, contextOf(second));
        assertFalse(uiContext.isSuspended());
        Mockito.verify(applicationManager, Mockito.times(1)).startApplication(Mockito.any());
        // First what was in flight, from the very byte the terminal stopped at
        assertArrayEquals(Arrays.copyOfRange(firstBytes, received, firstBytes.length),
            Arrays.copyOf(secondBytes, firstBytes.length - received));
        // Then the writes made while suspended, compacted: the last text only
        assertTrue(contains(secondBytes, "during-2"));
        assertFalse(contains(secondBytes, "during-1"));
        assertFalse("the terminal is not sent a new context", contains(secondBytes, "before-1"));
        // Then the block that tells the terminal it was resumed
        assertArrayEquals(reconnectBlock(uiContext),
            Arrays.copyOfRange(secondBytes, secondBytes.length - 4, secondBytes.length));

        // And the context carries on on the new WebSocket
        setText(uiContext, "after");
        assertTrue(contains(second.bytes(), "after"));
    }

    @Test
    public void offsetsCarryOnAcrossReconnections() {
        final Connection first = connect();
        final UIContext uiContext = contextOf(first);
        setText(uiContext, "one");
        final int receivedOnFirst = first.bytes().length;
        first.lose(StatusCode.ABNORMAL);

        final Connection second = reconnect(uiContext, receivedOnFirst);
        setText(uiContext, "two");
        final long receivedOnBoth = receivedOnFirst + second.bytes().length;
        second.lose(StatusCode.ABNORMAL);

        final Connection third = reconnect(uiContext, receivedOnBoth);
        assertSame(uiContext, contextOf(third));
        assertArrayEquals(reconnectBlock(uiContext), third.bytes());
    }

    @Test
    public void aGoingAwayCloseIsResumable() {
        // What a proxy sends when it restarts: the page itself sends 1005 when it unloads
        final Connection connection = connect();
        final UIContext uiContext = contextOf(connection);
        connection.lose(StatusCode.SHUTDOWN);

        assertTrue(uiContext.isSuspended());
    }

    @Test
    public void withoutReconnectionAPageParameterNamedLikeTheResumeOneIsIgnored() {
        configuration.setReconnectionTimeoutMs(0);
        final Map<String, List<String>> parameters = new HashMap<>();
        parameters.put(ClientToServerModel.RECONNECT_UI_CONTEXT_ID.toStringValue(), List.of("2026"));
        final Connection connection = new Connection(parameters, application, applicationManager);

        assertNotNull("a new context, as for any page", contextOf(connection));
        assertEquals(-1, connection.closeCode);
    }

    @Test
    public void theResumeParametersAppendedByTheTerminalWinOverThePagesOwn() {
        final Connection first = connect();
        final UIContext uiContext = contextOf(first);
        final int received = first.bytes().length;
        first.lose(StatusCode.ABNORMAL);

        final Map<String, List<String>> parameters = new HashMap<>();
        // The page's own query comes first in the WebSocket URL, the terminal's resume parameters last
        parameters.put(ClientToServerModel.RECONNECT_UI_CONTEXT_ID.toStringValue(), List.of("2026", String.valueOf(uiContext.getID())));
        parameters.put(ClientToServerModel.RECONNECT_OFFSET.toStringValue(), List.of(String.valueOf(received)));
        final Connection second = new Connection(parameters, application, applicationManager);

        assertSame(uiContext, contextOf(second));
    }

    @Test
    public void aMalformedOffsetIsRefused() {
        final Connection first = connect();
        final UIContext uiContext = contextOf(first);
        first.lose(StatusCode.ABNORMAL);

        final Map<String, List<String>> parameters = new HashMap<>();
        parameters.put(ClientToServerModel.RECONNECT_UI_CONTEXT_ID.toStringValue(), List.of(String.valueOf(uiContext.getID())));
        parameters.put(ClientToServerModel.RECONNECT_OFFSET.toStringValue(), List.of("not-a-number"));
        final Connection second = new Connection(parameters, application, applicationManager);

        assertEquals(WebSocket.CLOSE_RESUME_REFUSED, second.closeCode);
        assertTrue("left to its timeout", uiContext.isSuspended());
    }

    @Test
    public void aNestedReleaseKeepsTheCurrentContext() {
        final UIContext uiContext = contextOf(connect());
        uiContext.acquire();
        try {
            uiContext.acquire();
            uiContext.release();
            assertSame(uiContext, UIContext.get());
        } finally {
            uiContext.release();
        }
        assertNull(UIContext.get());
    }

    @Test
    public void aContextLostDuringAnExecuteKeepsRecordingItsWrites() {
        final Connection first = connect();
        final UIContext uiContext = contextOf(first);
        final int received = first.bytes().length;
        assertTrue(uiContext.execute(() -> {
            // The connection is lost while the application writes, as a failed flush reports it
            first.lose(StatusCode.ABNORMAL);
            final ModelWriter writer = uiContext.getWriter();
            writer.write(ServerToClientModel.TYPE_UPDATE, OBJECT_ID);
            writer.write(ServerToClientModel.TEXT, "written-while-lost");
            writer.endObject();
        }));
        assertTrue(uiContext.isSuspended());

        final Connection second = reconnect(uiContext, received);
        assertTrue(contains(second.bytes(), "written-while-lost"));
    }

    @Test
    public void aResumeIsRefusedWhenTheBytesTheTerminalMissedAreNoLongerHeld() {
        configuration.setReplayBufferMaxBytes(1 << 14);
        final Connection first = connect();
        final UIContext uiContext = contextOf(first);
        final char[] big = new char[1000];
        Arrays.fill(big, 'x');
        for (int i = 0; i < 40; i++) {
            setText(uiContext, i + new String(big));
        }
        first.lose(StatusCode.ABNORMAL);

        final Connection second = reconnect(uiContext, 0);

        assertEquals(WebSocket.CLOSE_RESUME_REFUSED, second.closeCode);
        assertNull(contextOf(second));
        assertFalse("a context its terminal cannot come back to is destroyed", uiContext.isAlive());
    }

    @Test
    public void acknowledgedBytesAreReleased() {
        final Connection first = connect();
        final UIContext uiContext = contextOf(first);
        final char[] big = new char[1000];
        Arrays.fill(big, 'x');
        for (int i = 0; i < 40; i++) {
            setText(uiContext, i + new String(big));
        }
        final int received = first.bytes().length;
        heartbeat(first, received);
        setText(uiContext, "next");

        assertNull("released", first.webSocket.getBytesFrom(0));
        assertNotNull("not acknowledged yet", first.webSocket.getBytesFrom(received));
    }

    private static void heartbeat(final Connection connection, final long receivedBytes) {
        connection.webSocket.onWebSocketText(JsonProvider.provider().createObjectBuilder()
            .add(ClientToServerModel.HEARTBEAT_CLIENT.toStringValue(), JsonValue.NULL)
            .add(ClientToServerModel.RECEIVED_OFFSET.toStringValue(), receivedBytes).build().toString());
    }

    @Test
    public void aTerminalBackBeforeTheServerSawTheLossResumesItsContext() {
        final Connection first = connect();
        final UIContext uiContext = contextOf(first);
        setText(uiContext, "one");
        final int received = first.bytes().length;
        // A cut with no close: the server still believes the first WebSocket alive

        final Connection second = reconnect(uiContext, received);

        assertSame(uiContext, contextOf(second));
        assertFalse(first.open);
        assertArrayEquals(reconnectBlock(uiContext), second.bytes());
    }

    @Test
    public void aPageThatGoesAwayEndsItsContext() {
        for (final int statusCode : new int[] { StatusCode.NORMAL, StatusCode.NO_CODE }) {
            final Connection connection = connect();
            final UIContext uiContext = contextOf(connection);
            connection.lose(statusCode);

            assertFalse("closed with " + statusCode, uiContext.isAlive());
        }
    }

    @Test
    public void withoutReconnectionALostContextIsDestroyed() {
        configuration.setReconnectionTimeoutMs(0);
        final Connection connection = connect();
        final UIContext uiContext = contextOf(connection);
        connection.lose(StatusCode.ABNORMAL);

        assertFalse(uiContext.isAlive());
        assertNull(connection.webSocket.getBytesFrom(0));
    }

    @Test
    public void aSuspendedContextIsDestroyedAtItsTimeout() throws InterruptedException {
        configuration.setReconnectionTimeoutMs(100);
        final Connection connection = connect();
        final UIContext uiContext = contextOf(connection);
        connection.lose(StatusCode.ABNORMAL);
        assertTrue(uiContext.isSuspended());

        final long deadline = System.currentTimeMillis() + 5000;
        while (uiContext.isAlive() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertFalse(uiContext.isAlive());
    }

    @Test
    public void aResumeToAnotherSessionsContextIsRefused() {
        final Connection first = connect();
        final UIContext uiContext = contextOf(first);
        first.lose(StatusCode.ABNORMAL);

        final Application otherSession = new Application("other", httpSession(), configuration);
        final Map<String, List<String>> parameters = new HashMap<>();
        parameters.put(ClientToServerModel.RECONNECT_UI_CONTEXT_ID.toStringValue(), List.of(String.valueOf(uiContext.getID())));
        parameters.put(ClientToServerModel.RECONNECT_OFFSET.toStringValue(), List.of("0"));
        final Connection intruder = new Connection(parameters, otherSession, applicationManager);

        assertEquals(WebSocket.CLOSE_RESUME_REFUSED, intruder.closeCode);
        assertTrue("the context of the first session is left as it is", uiContext.isSuspended());
    }
}
