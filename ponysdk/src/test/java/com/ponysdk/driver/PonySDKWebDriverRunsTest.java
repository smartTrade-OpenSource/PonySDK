package com.ponysdk.driver;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.websocket.ByteBufferPool;
import com.ponysdk.core.server.websocket.PusherAccess;

import jakarta.json.JsonObject;

/** The driver splits protocol-2 update runs into one message per object (same shape as protocol 1). */
public class PonySDKWebDriverRunsTest {

    @Test
    public void updateRunIsSplitPerObjectWithItsHeader() throws Exception {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final Session session = Mockito.mock(Session.class);
        Mockito.when(session.isOpen()).thenReturn(true);
        Mockito.doAnswer(inv -> {
            final ByteBuffer buf = inv.getArgument(0);
            final byte[] chunk = new byte[buf.remaining()];
            buf.duplicate().get(chunk);
            stream.writeBytes(chunk);
            ((Callback) inv.getArgument(1)).succeed();
            return null;
        }).when(session).sendBinary(Mockito.any(ByteBuffer.class), Mockito.any(Callback.class));
        final PusherAccess p = new PusherAccess(session, new ByteBufferPool(1 << 16, 2));
        p.encodeInt(ServerToClientModel.WINDOW_ID, 9);
        p.encodeInt(ServerToClientModel.TYPE_UPDATE, 5);
        p.encode(ServerToClientModel.TEXT, "a");
        p.encode(ServerToClientModel.UPDATE_NEXT, null);                 // -> 6
        p.encode(ServerToClientModel.TEXT, "b");
        p.encode(ServerToClientModel.PUT_STYLE_KEY, "color");
        p.encode(ServerToClientModel.STYLE_VALUE, "red");
        p.encode(ServerToClientModel.UPDATE_NEXT_DELTA, (byte) 255);     // -> 6 + 2 + 255 = 263
        p.encode(ServerToClientModel.TEXT, "c");
        p.encodeInt(ServerToClientModel.UPDATE_NEXT_ID, 40_000);         // -> 40000 (4-byte uint31)
        p.encode(ServerToClientModel.TEXT, "d");
        p.encode(ServerToClientModel.END, null);
        p.flush();

        final List<List<PonyFrame>> received = new ArrayList<>();
        final PonySDKWebDriver driver = new PonySDKWebDriver(new PonyMessageListener() {
            @Override public void onSendMessage(final JsonObject message) {}
            @Override public void onReceiveMessage(final List<PonyFrame> message) { received.add(new ArrayList<>(message)); }
        }, null, null, false);
        final Method onMessage = PonySDKWebDriver.class.getDeclaredMethod("onMessage", ByteBuffer.class);
        onMessage.setAccessible(true);
        onMessage.invoke(driver, ByteBuffer.wrap(stream.toByteArray()));

        assertEquals(4, received.size());
        assertEquals("[model=WINDOW_ID, value=9, model=TYPE_UPDATE, value=5, model=TEXT, value=a]", received.get(0).toString());
        assertEquals("[model=WINDOW_ID, value=9, model=TYPE_UPDATE, value=6, model=TEXT, value=b, model=PUT_STYLE_KEY, value=color, model=STYLE_VALUE, value=red]",
                received.get(1).toString());
        assertEquals("[model=WINDOW_ID, value=9, model=TYPE_UPDATE, value=263, model=TEXT, value=c]", received.get(2).toString());
        assertEquals("[model=WINDOW_ID, value=9, model=TYPE_UPDATE, value=40000, model=TEXT, value=d]", received.get(3).toString());
    }

    @Test
    public void aCreateCarryingItsParentAddsTheElement() throws Exception {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final Session session = Mockito.mock(Session.class);
        Mockito.when(session.isOpen()).thenReturn(true);
        Mockito.doAnswer(inv -> {
            final ByteBuffer buf = inv.getArgument(0);
            final byte[] chunk = new byte[buf.remaining()];
            buf.duplicate().get(chunk);
            stream.writeBytes(chunk);
            ((Callback) inv.getArgument(1)).succeed();
            return null;
        }).when(session).sendBinary(Mockito.any(ByteBuffer.class), Mockito.any(Callback.class));
        final PusherAccess p = new PusherAccess(session, new ByteBufferPool(1 << 16, 2));
        p.encodeInt(ServerToClientModel.WINDOW_ID, 0);
        p.encodeInt(ServerToClientModel.TYPE_CREATE, 5);
        p.encode(ServerToClientModel.WIDGET_TYPE, (byte) com.ponysdk.core.model.WidgetType.FLOW_PANEL.getValue());
        p.encode(ServerToClientModel.END, null);
        p.encodeInt(ServerToClientModel.TYPE_CREATE, 6);
        p.encode(ServerToClientModel.WIDGET_TYPE, (byte) com.ponysdk.core.model.WidgetType.LABEL.getValue());
        p.encode(ServerToClientModel.TEXT, "x");
        p.encodeInt(ServerToClientModel.PARENT_OBJECT_ID, 5); // protocol 3: created in its parent
        p.encode(ServerToClientModel.END, null);
        p.flush();

        final PonySDKWebDriver driver = new PonySDKWebDriver(new PonyMessageListener() {
            @Override public void onSendMessage(final JsonObject message) {}
            @Override public void onReceiveMessage(final List<PonyFrame> message) {}
        }, null, null, false);
        final Method onMessage = PonySDKWebDriver.class.getDeclaredMethod("onMessage", ByteBuffer.class);
        onMessage.setAccessible(true);
        onMessage.invoke(driver, ByteBuffer.wrap(stream.toByteArray()));

        final java.lang.reflect.Field f = PonySDKWebDriver.class.getDeclaredField("elements");
        f.setAccessible(true);
        final java.util.Map<?, ?> elements = (java.util.Map<?, ?>) f.get(driver);
        final PonyWebElement panel = (PonyWebElement) elements.get(5);
        final PonyWebElement label = (PonyWebElement) elements.get(6);
        assertEquals(panel, label.parent);
        assertEquals(List.of(label), panel.children);
    }

    @Test
    public void announcesItsProtocolVersionOnTheUrl() throws Exception {
        final PonySDKWebDriver driver = new PonySDKWebDriver();
        final Method m = PonySDKWebDriver.class.getDeclaredMethod("withProtocolVersion", String.class);
        m.setAccessible(true);
        assertEquals("ws://h/app/ws?w=" + com.ponysdk.core.model.ProtocolVersion.CURRENT, m.invoke(driver, "ws://h/app/ws"));
        assertEquals("ws://h/app/ws?a=1&w=" + com.ponysdk.core.model.ProtocolVersion.CURRENT, m.invoke(driver, "ws://h/app/ws?a=1"));
        assertEquals("ws://h/app/ws?w=1", m.invoke(driver, "ws://h/app/ws?w=1"));
        driver.setProtocolVersion(1);
        assertEquals("ws://h/app/ws?w=1", m.invoke(driver, "ws://h/app/ws"));
    }
}
