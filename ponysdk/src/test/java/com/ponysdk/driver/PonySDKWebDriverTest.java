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

package com.ponysdk.driver;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jetty.websocket.api.Callback;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.By;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.websocket.ByteBufferPool;
import com.ponysdk.core.server.websocket.PusherAccess;
import com.ponysdk.core.ui.model.PKeyCodes;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;

/**
 * The driver as a client of the binary protocol, without a network: frames encoded by the server's own encoder are
 * fed to it, and what it sends back is captured from a mocked WebSocket session.
 */
public class PonySDKWebDriverTest {

    /** Server-encoded bytes, through the real encoder. */
    private static final class Wire {

        private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        private final PusherAccess pusher;

        Wire() {
            final org.eclipse.jetty.websocket.api.Session session = Mockito.mock(org.eclipse.jetty.websocket.api.Session.class);
            Mockito.when(session.isOpen()).thenReturn(true);
            Mockito.doAnswer(inv -> {
                final ByteBuffer buf = inv.getArgument(0);
                final byte[] chunk = new byte[buf.remaining()];
                buf.duplicate().get(chunk);
                stream.writeBytes(chunk);
                ((Callback) inv.getArgument(1)).succeed();
                return null;
            }).when(session).sendBinary(Mockito.any(ByteBuffer.class), Mockito.any(Callback.class));
            pusher = new PusherAccess(session, new ByteBufferPool(1 << 16, 2));
        }

        Wire put(final ServerToClientModel model, final Object value) throws IOException {
            pusher.encode(model, value);
            return this;
        }

        Wire id(final ServerToClientModel model, final int value) throws IOException {
            pusher.encodeInt(model, value);
            return this;
        }

        Wire end() throws IOException {
            return put(ServerToClientModel.END, null);
        }

        Wire create(final int id, final WidgetType type) throws IOException {
            return id(ServerToClientModel.TYPE_CREATE, id).put(ServerToClientModel.WIDGET_TYPE, (byte) type.getValue()).end();
        }

        byte[] bytes() throws IOException {
            pusher.flush();
            return stream.toByteArray();
        }
    }

    private final List<String> sent = new ArrayList<>();
    private final List<JsonObject> sentJson = new ArrayList<>();
    private final List<String> received = new ArrayList<>();
    private final int[] bandwidth = new int[2]; // received, sent
    private Session session;
    private RemoteEndpoint.Basic remote;

    private PonySDKWebDriver driver(final boolean handleImplicitCommunication) throws Exception {
        final PonySDKWebDriver driver = new PonySDKWebDriver(new PonyMessageListener() {

            @Override
            public void onSendMessage(final JsonObject message) {
                sentJson.add(message);
            }

            @Override
            public void onReceiveMessage(final List<PonyFrame> message) {
                received.add(message.toString());
            }
        }, new PonyBandwithListener() {

            @Override
            public void onSend(final int bytes) {
                bandwidth[1] += bytes;
            }

            @Override
            public void onReceive(final int bytes) {
                bandwidth[0] += bytes;
            }

            @Override
            public void onSendCompressed(final int bytes) {
            }

            @Override
            public void onReceiveCompressed(final int bytes) {
            }
        }, null, handleImplicitCommunication);
        return attachSession(driver);
    }

    private PonySDKWebDriver attachSession(final PonySDKWebDriver driver) throws Exception {
        session = Mockito.mock(Session.class);
        remote = Mockito.mock(RemoteEndpoint.Basic.class);
        Mockito.when(session.getBasicRemote()).thenReturn(remote);
        Mockito.when(session.getId()).thenReturn("session-1");
        Mockito.doAnswer(inv -> sent.add(inv.getArgument(0))).when(remote).sendText(Mockito.anyString());
        final Field clientField = PonySDKWebDriver.class.getDeclaredField("client");
        clientField.setAccessible(true);
        final Field sessionField = WebsocketClient.class.getDeclaredField("session");
        sessionField.setAccessible(true);
        sessionField.set(clientField.get(driver), session);
        return driver;
    }

    private static void receive(final PonySDKWebDriver driver, final byte[] bytes) throws Exception {
        final Method onMessage = PonySDKWebDriver.class.getDeclaredMethod("onMessage", ByteBuffer.class);
        onMessage.setAccessible(true);
        try {
            onMessage.invoke(driver, ByteBuffer.wrap(bytes));
        } catch (final InvocationTargetException e) {
            if (e.getCause() instanceof Exception cause) throw cause;
            throw e;
        }
    }

    private static String instruction(final String json) {
        return "{\"5\":[" + json + "]}";
    }

    @Test
    public void tracksTheWidgetTree() throws Exception {
        final PonySDKWebDriver driver = driver(false);
        receive(driver, new Wire() //
            .create(1, WidgetType.FLOW_PANEL).create(2, WidgetType.LABEL).create(3, WidgetType.BUTTON) //
            .id(ServerToClientModel.TYPE_ADD, 2).id(ServerToClientModel.PARENT_OBJECT_ID, 1).end() //
            .id(ServerToClientModel.TYPE_ADD, 3).id(ServerToClientModel.PARENT_OBJECT_ID, 1).put(ServerToClientModel.INDEX, 0).end()
            .bytes());

        final PonyWebElement panel = driver.findElementByPonyId(1);
        final PonyWebElement label = driver.findElementByPonyId(2);
        final PonyWebElement button = driver.findElementByPonyId(3);
        assertEquals(List.of(button, label), panel.getChildren());
        assertSame(panel, label.getParent());
        assertEquals(WidgetType.BUTTON, button.getWidgetType());
        assertEquals(3, button.getObjectID());

        receive(driver, new Wire().id(ServerToClientModel.TYPE_REMOVE, 3).end().bytes());
        assertEquals(List.of(label), panel.getChildren());
        assertNull(button.getParent());

        receive(driver, new Wire().id(ServerToClientModel.TYPE_GC, 3).end().bytes());
        assertNull(driver.findElementByPonyId(3));
        assertEquals(List.of(label), driver.findElementsAsPony(By.cssSelector("FLOW_PANEL > LABEL")));

        driver.clear();
        assertNull(driver.findElementByPonyId(1));
    }

    @Test
    public void ignoresInstructionsOnUnknownWidgets() throws Exception {
        final PonySDKWebDriver driver = driver(false);
        receive(driver, new Wire() //
            .id(ServerToClientModel.TYPE_CREATE, 1).end() // no widget type
            .create(2, WidgetType.LABEL) //
            .id(ServerToClientModel.TYPE_ADD, 99).id(ServerToClientModel.PARENT_OBJECT_ID, 2).end() //
            .id(ServerToClientModel.TYPE_ADD, 2).end() // no parent
            .id(ServerToClientModel.TYPE_ADD, 2).id(ServerToClientModel.PARENT_OBJECT_ID, 98).end() // unknown parent
            .id(ServerToClientModel.TYPE_REMOVE, 2).end() // not attached
            .id(ServerToClientModel.TYPE_REMOVE, 96).end() // unknown
            .id(ServerToClientModel.TYPE_CREATE, 3).put(ServerToClientModel.WIDGET_TYPE, (byte) WidgetType.LABEL.getValue())
            .id(ServerToClientModel.PARENT_OBJECT_ID, 95).end() // created in an unknown parent
            .id(ServerToClientModel.TYPE_UPDATE, 97).put(ServerToClientModel.TEXT, "lost").end() //
            .bytes());

        assertNull(driver.findElementByPonyId(1));
        final PonyWebElement label = driver.findElementByPonyId(2);
        assertNull(label.getParent());
        assertNull(label.getText());
        assertNull(driver.findElementByPonyId(3).getParent());
        assertEquals(2, driver.findElements(By.tagName("LABEL")).size());
    }

    @Test
    public void appliesUpdatesToTheirWidget() throws Exception {
        final PonySDKWebDriver driver = driver(false);
        receive(driver, new Wire().create(1, WidgetType.LABEL) //
            .id(ServerToClientModel.TYPE_UPDATE, 1) //
            .put(ServerToClientModel.TEXT, "hello") //
            .put(ServerToClientModel.PUT_ATTRIBUTE_KEY, "id") //
            .put(ServerToClientModel.ATTRIBUTE_VALUE, "greeting") //
            .put(ServerToClientModel.STYLE_NAME, "a b") //
            .put(ServerToClientModel.ADD_STYLE_NAME, "c d") //
            .put(ServerToClientModel.REMOVE_STYLE_NAME, "a d") //
            .put(ServerToClientModel.WIDGET_VISIBLE, false) //
            .put(ServerToClientModel.ENABLED, false) //
            .end().bytes());

        final PonyWebElement label = driver.findElement(By.id("greeting"));
        assertEquals("hello", label.getText());
        assertEquals("greeting", label.getAttribute("id"));
        assertEquals(List.of("b", "c"), List.copyOf(label.getStyles()));
        assertFalse(label.isDisplayed());
        assertFalse(label.isEnabled());
        assertSame(label, driver.findElementsAsPony(By.cssSelector(".b.c")).get(0));

        receive(driver, new Wire().id(ServerToClientModel.TYPE_UPDATE, 1) //
            .put(ServerToClientModel.HTML, "<b>bold</b>") //
            .put(ServerToClientModel.REMOVE_ATTRIBUTE_KEY, "id") //
            .put(ServerToClientModel.PUT_ATTRIBUTE_KEY, "title") // without a value: ignored
            .put(ServerToClientModel.WIDGET_VISIBLE, true) //
            .put(ServerToClientModel.ENABLED, true) //
            .end().bytes());
        assertEquals("<b>bold</b>", label.getText());
        assertTrue(label.getAttributes().isEmpty());
        assertTrue(label.isDisplayed());
        assertTrue(label.isEnabled());

        receive(driver, new Wire().id(ServerToClientModel.TYPE_UPDATE, 1) //
            .put(ServerToClientModel.FUNCTION_ARGS, new Object[] { 1, "a", null, true }) //
            .end().bytes());
        assertEquals("[1, a, null, true]", label.getText());
    }

    @Test
    public void decodesEveryValueType() throws Exception {
        final PonySDKWebDriver driver = driver(false);
        final String longAscii = "x".repeat(300);
        final String longUtf8 = "é".repeat(200);
        final String hugeUtf8 = "é".repeat(40_000);
        receive(driver, new Wire().id(ServerToClientModel.TYPE_UPDATE, 1) //
            .put(ServerToClientModel.FUNCTION_ARGS, new Object[] { null, false, true, (byte) 1, (short) 300, 70_000, 5_000_000_000L,
                    0.1, 1.5f, "ascii", longAscii, "café", longUtf8, hugeUtf8 }) //
            .put(ServerToClientModel.TEXT, longAscii) //
            .put(ServerToClientModel.HTML, "café") //
            .put(ServerToClientModel.VALUE, longUtf8) //
            .put(ServerToClientModel.PROPERTY_VALUE, hugeUtf8) //
            .end().bytes());

        assertEquals(1, received.size());
        final String expected = "[model=TYPE_UPDATE, value=1, model=FUNCTION_ARGS, value=" + Arrays.toString(new Object[] { null,
                false, true, (byte) 1, (short) 300, 70_000, 5_000_000_000L, 0.1, 1.5f, "ascii", longAscii, "café", longUtf8,
                hugeUtf8 }) + ", model=TEXT, value=" + longAscii + ", model=HTML, value=café, model=VALUE, value=" + longUtf8
                + ", model=PROPERTY_VALUE, value=" + hugeUtf8 + "]";
        assertEquals(expected, received.get(0));
    }

    @Test
    public void decodesScalarsAndAVeryLongAsciiString() throws Exception {
        final PonySDKWebDriver driver = driver(false);
        final String huge = "a".repeat(70_000);
        receive(driver, new Wire().id(ServerToClientModel.TYPE_UPDATE, 1) //
            .put(ServerToClientModel.VALUE_CHECKBOX, (byte) 2) //
            .put(ServerToClientModel.WIDTH, 12.5) //
            .put(ServerToClientModel.HEARTBEAT_PERIOD, 1_000) //
            .put(ServerToClientModel.TEXT, huge) //
            .end().bytes());
        assertEquals(List.of("[model=TYPE_UPDATE, value=1, model=VALUE_CHECKBOX, value=2, model=WIDTH, value=12.5, "
                + "model=HEARTBEAT_PERIOD, value=1000, model=TEXT, value=" + huge + "]"), received);
    }

    @Test
    public void instructionsWithoutAKnownWidgetAreIgnored() throws Exception {
        final PonySDKWebDriver driver = driver(false);
        receive(driver, new Wire().create(1, WidgetType.LABEL) //
            .put(ServerToClientModel.TEXT, "no target").end() //
            .id(ServerToClientModel.TYPE_UPDATE, 97) //
            .put(ServerToClientModel.PUT_ATTRIBUTE_KEY, "k").put(ServerToClientModel.ATTRIBUTE_VALUE, "v") //
            .put(ServerToClientModel.REMOVE_ATTRIBUTE_KEY, "k") //
            .put(ServerToClientModel.STYLE_NAME, "a") //
            .put(ServerToClientModel.ADD_STYLE_NAME, "b") //
            .put(ServerToClientModel.REMOVE_STYLE_NAME, "a") //
            .put(ServerToClientModel.WIDGET_VISIBLE, false) //
            .put(ServerToClientModel.ENABLED, false) //
            .put(ServerToClientModel.FUNCTION_ARGS, new Object[] { 1 }) //
            .end().bytes());

        final PonyWebElement label = driver.findElementByPonyId(1);
        assertNull(label.getText());
        assertTrue(label.getAttributes().isEmpty());
        assertTrue(label.getStyles().isEmpty());
        assertTrue(label.isDisplayed());
        assertEquals(3, received.size());
    }

    @Test
    public void framesThatAreNotAnUpdateRunAreDispatchedAsReceived() throws Exception {
        final PonySDKWebDriver driver = driver(false);
        receive(driver, new Wire() //
            .id(ServerToClientModel.TYPE_UPDATE, 1).id(ServerToClientModel.TYPE_UPDATE, 2).end() //
            .put(ServerToClientModel.UPDATE_NEXT, null).put(ServerToClientModel.TEXT, "t").end() //
            .bytes());
        assertEquals(List.of("[model=TYPE_UPDATE, value=1, model=TYPE_UPDATE, value=2]",
            "[model=UPDATE_NEXT, value=null, model=TEXT, value=t]"), received);
    }

    @Test
    public void aDictionaryReferenceBeyondTheTableClosesTheConnection() throws Exception {
        final PonySDKWebDriver driver = driver(false);
        receive(driver, new byte[] { (byte) ServerToClientModel.TEXT.getValue(), (byte) 249, 0x10, 0 }); // id 4096
        Mockito.verify(session).close();
        assertTrue(received.isEmpty());
    }

    @Test
    public void framesSplitAnywhereAcrossMessagesDecodeTheSame() throws Exception {
        final byte[] bytes = new Wire().create(1, WidgetType.LABEL) //
            .id(ServerToClientModel.TYPE_UPDATE, 1).put(ServerToClientModel.TEXT, "y".repeat(5_000)).end() //
            .id(ServerToClientModel.TYPE_UPDATE, 40_000).put(ServerToClientModel.FUNCTION_ARGS, new Object[] { 2L, "z" }).end() //
            .bytes();
        receive(driver(false), bytes);
        final List<String> whole = List.copyOf(received);
        assertEquals(3, whole.size());

        for (int split = 1; split < bytes.length; split += 7) {
            received.clear();
            final PonySDKWebDriver driver = driver(false);
            receive(driver, Arrays.copyOfRange(bytes, 0, split));
            receive(driver, Arrays.copyOfRange(bytes, split, bytes.length));
            assertEquals(whole, received, "split at " + split);
        }

        received.clear();
        final PonySDKWebDriver driver = driver(false);
        for (final byte b : bytes) {
            receive(driver, new byte[] { b });
        }
        assertEquals(whole, received, "byte by byte");
        assertEquals("y".repeat(5_000), driver.findElementByPonyId(1).getText());
    }

    @Test
    public void anUnknownDictionaryReferenceClosesTheConnection() throws Exception {
        final PonySDKWebDriver driver = driver(false);
        // TEXT, a string tagged as a dictionary reference to id 7, never defined
        final byte[] bytes = { (byte) ServerToClientModel.TEXT.getValue(), (byte) 249, 0, 7 };
        receive(driver, bytes);
        Mockito.verify(session).close();
        assertTrue(received.isEmpty());
    }

    @Test
    public void countsTheReceivedBytes() throws Exception {
        final byte[] bytes = new Wire().create(1, WidgetType.LABEL).bytes();
        receive(driver(false), bytes);
        assertEquals(bytes.length, bandwidth[0]);
    }

    @Test
    public void answersTheImplicitCommunication() throws Exception {
        final PonySDKWebDriver driver = driver(true);
        assertTrue(driver.isHandleImplicitCommunication());
        receive(driver, new Wire() //
            .put(ServerToClientModel.ADD_COOKIE, "lang").put(ServerToClientModel.VALUE, "fr").end() //
            .put(ServerToClientModel.ADD_COOKIE, "ignored").end() // no value
            .id(ServerToClientModel.CREATE_CONTEXT, 12).end() //
            .put(ServerToClientModel.HISTORY_FIRE_EVENTS, true).put(ServerToClientModel.TYPE_HISTORY, "page1").end() //
            .put(ServerToClientModel.HISTORY_FIRE_EVENTS, true).end() // no token
            .put(ServerToClientModel.HISTORY_FIRE_EVENTS, false).put(ServerToClientModel.TYPE_HISTORY, "page2").end() //
            .put(ServerToClientModel.ROUNDTRIP_LATENCY, 42L).end() //
            .create(5, WidgetType.WINDOW) //
            .put(ServerToClientModel.REMOVE_COOKIE, "lang").end() //
            .id(ServerToClientModel.TYPE_UPDATE, 5).put(ServerToClientModel.OPEN, null).end() //
            .id(ServerToClientModel.TYPE_UPDATE, 77).put(ServerToClientModel.OPEN, null).end() // unknown window
            .bytes());

        assertEquals(12, driver.getContextId());
        assertEquals("page1", driver.getTitle());
        assertEquals(List.of( //
            instruction("{\"0\":0,\"9\":[{\"a\":\"lang\",\"b\":\"fr\"}]}"), //
            instruction("{\"c\":\"page1\"}"), //
            "{\"h\":42}", //
            instruction("{\"0\":5,\"G\":\"\"}"), //
            instruction("{\"0\":0,\"9\":[]}")), sent);
        assertEquals(sent, sentJson.stream().map(JsonObject::toString).toList());
        assertEquals(sent.stream().mapToInt(String::length).sum(), bandwidth[1]);
    }

    @Test
    public void withoutImplicitCommunicationNothingIsSentButTheStateIsKept() throws Exception {
        final PonySDKWebDriver driver = driver(false);
        receive(driver, new Wire() //
            .id(ServerToClientModel.CREATE_CONTEXT, 3).end() //
            .put(ServerToClientModel.HISTORY_FIRE_EVENTS, true).put(ServerToClientModel.TYPE_HISTORY, "page1").end() //
            .put(ServerToClientModel.ROUNDTRIP_LATENCY, 42L).end() //
            .create(5, WidgetType.WINDOW) //
            .id(ServerToClientModel.TYPE_UPDATE, 5).put(ServerToClientModel.OPEN, null).end() //
            .bytes());

        assertEquals(3, driver.getContextId());
        assertEquals("page1", driver.getTitle());
        assertTrue(sent.isEmpty());
    }

    @Test
    public void elementsSendTheirEvents() throws Exception {
        final PonySDKWebDriver driver = driver(false);
        receive(driver, new Wire().create(4, WidgetType.TEXTBOX).bytes());
        final PonyWebElement box = driver.findElementByPonyId(4);

        box.click();
        box.doubleClick();
        box.mouseOver();
        box.focus();
        box.sendKeys("ab", "c");
        box.sendKeyDown(PKeyCodes.ENTER);
        box.sendKeyUp(PKeyCodes.ENTER);
        box.sendApplicationInstruction(com.ponysdk.core.model.ClientToServerModel.VALUE_KEY, 1L);
        box.sendApplicationInstruction(com.ponysdk.core.model.ClientToServerModel.VALUE_KEY, 1.5);
        box.sendApplicationInstruction(com.ponysdk.core.model.ClientToServerModel.VALUE_KEY, true);
        box.sendApplicationInstruction(com.ponysdk.core.model.ClientToServerModel.VALUE_KEY, Json.createValue("v"));

        final String click = Integer.toString(com.ponysdk.core.model.DomHandlerType.CLICK.getValue());
        final String enter = Integer.toString(PKeyCodes.ENTER.getCode());
        assertEquals(instruction("{\"0\":4,\"g\":" + click + "}"), sent.get(0));
        assertEquals(instruction("{\"0\":4,\"C\":\"ab\"}"), sent.get(4));
        assertEquals(instruction("{\"0\":4,\"C\":\"c\"}"), sent.get(5));
        assertEquals(instruction("{\"0\":4,\"g\":" + com.ponysdk.core.model.DomHandlerType.KEY_DOWN.getValue() + ",\"k\":" + enter + "}"),
            sent.get(6));
        assertEquals(instruction("{\"0\":4,\"g\":" + com.ponysdk.core.model.DomHandlerType.KEY_UP.getValue() + ",\"k\":" + enter + "}"),
            sent.get(7));
        assertEquals(instruction("{\"0\":4,\"k\":1}"), sent.get(8));
        assertEquals(instruction("{\"0\":4,\"k\":1.5}"), sent.get(9));
        assertEquals(instruction("{\"0\":4,\"k\":true}"), sent.get(10));
        assertEquals(instruction("{\"0\":4,\"k\":\"v\"}"), sent.get(11));
        assertEquals(12, sent.size());
        assertEquals("c", box.getText(), "the last keys sent");
        box.clear();
        assertNull(box.getText());
    }

    @Test
    public void aFailedSendIsReported() throws Exception {
        final PonySDKWebDriver driver = driver(false);
        Mockito.doThrow(new IOException("broken pipe")).when(remote).sendText(Mockito.anyString());
        final PonyIOException e = assertThrows(PonyIOException.class, driver::sendCookies);
        assertEquals("broken pipe", e.getCause().getMessage());
        assertTrue(sentJson.isEmpty(), "a message that failed is not reported as sent");
    }

    @Test
    public void printsThePageAsXml() throws Exception {
        final PonySDKWebDriver driver = driver(false);
        receive(driver, new Wire().create(1, WidgetType.FLOW_PANEL).create(2, WidgetType.LABEL) //
            .id(ServerToClientModel.TYPE_ADD, 2).id(ServerToClientModel.PARENT_OBJECT_ID, 1).end() //
            .id(ServerToClientModel.TYPE_UPDATE, 2).put(ServerToClientModel.TEXT, "hi").put(ServerToClientModel.STYLE_NAME, "s")
            .put(ServerToClientModel.PUT_ATTRIBUTE_KEY, "k").put(ServerToClientModel.ATTRIBUTE_VALUE, "v")
            .put(ServerToClientModel.WIDGET_VISIBLE, false).put(ServerToClientModel.ENABLED, false).end() //
            .bytes());

        assertEquals("<WEB>\n" //
                + "\t<FLOW_PANEL id='1' >\n" //
                + "\t\t<LABEL id='2' class='[s]' attributes='{k=v}' displayed='false' enabled='false' >hi\n" //
                + "\t\t</LABEL>\n" //
                + "\t</FLOW_PANEL>\n" //
                + "</WEB>", driver.getPageSource());
        final java.io.StringWriter xml = new java.io.StringWriter();
        driver.printAsXml(xml);
        assertEquals(driver.getPageSource(), xml.toString(), "printAsXml writes the page source");
    }

    @Test
    public void sessionAndUnsupportedWebDriverFeatures() throws Exception {
        final PonySDKWebDriver driver = driver(false);
        assertEquals("session-1", driver.getSessionId());
        assertNull(driver.getCurrentUrl());
        assertNull(driver.getWindowHandles());
        assertNull(driver.getWindowHandle());
        assertNull(driver.switchTo());
        assertNull(driver.navigate());
        assertNull(driver.manage());
        driver.quit();
        driver.ignoreSessionStatusChanges();
        assertEquals(com.ponysdk.core.model.ProtocolVersion.CURRENT, driver.getProtocolVersion());

        driver.close();
        Mockito.verify(session).close();
    }

    @Test
    public void aSessionThatFailsToCloseIsLogged() throws Exception {
        final PonySDKWebDriver driver = driver(false);
        Mockito.doThrow(new IOException("already closed")).when(session).close();
        driver.close();
        Mockito.verify(session).close();
    }

    @Test
    public void withoutListenersMessagesAreStillDecodedAndSent() throws Exception {
        final PonySDKWebDriver driver = attachSession(new PonySDKWebDriver(null, null, null, false));
        receive(driver, new Wire().create(1, WidgetType.LABEL) //
            .id(ServerToClientModel.TYPE_UPDATE, 1).put(ServerToClientModel.TEXT, "hi").end().bytes());
        assertEquals("hi", driver.findElementByPonyId(1).getText());

        driver.findElementByPonyId(1).click();
        assertEquals(1, sent.size());
    }

    @Test
    public void ioExceptionCarriesItsMessageAndCause() {
        final Throwable cause = new IOException("cause");
        assertNull(new PonyIOException().getMessage());
        assertEquals("m", new PonyIOException("m").getMessage());
        assertSame(cause, new PonyIOException(cause).getCause());
        final PonyIOException full = new PonyIOException("m", cause, false, false);
        assertEquals("m", full.getMessage());
        assertSame(cause, full.getCause());
        assertEquals(0, full.getStackTrace().length, "not writable");
        full.addSuppressed(new IOException());
        assertEquals(0, full.getSuppressed().length, "suppression disabled");
    }

    @Test
    public void aDriverNeverConnectedHasNoSession() {
        final PonySDKWebDriver driver = new PonySDKWebDriver();
        assertNull(driver.getSessionId());
        driver.close();
        assertThrows(PonyIOException.class, () -> driver.get("ws://bad host/"));
        assertNull(driver.getCurrentUrl());
    }
}
