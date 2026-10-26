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

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.websocket.Subprotocols;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/** {@link PHost}: the element {@code <pony-app>} as its application sees it (ADR-0008). */
public class PHostTest extends PSuite {

    private static final String ATTRIBUTES = ClientToServerModel.HOST_ATTRIBUTES.toStringValue();

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void reset() {
        writer.uninstall();
        UIContext.get().removeAttribute(PHost.class.getName());
        Mockito.doReturn(Map.of()).when(UIContext.get()).getParameterMap();
        Mockito.doReturn(null).when(UIContext.get()).getHeader(Subprotocols.HEADER);
    }

    private static void handshake(final String attributesJson) {
        Mockito.doReturn(Map.of(ATTRIBUTES, List.of(attributesJson))).when(UIContext.get()).getParameterMap();
    }

    private static JsonObject fromTerminal(final PHost host, final JsonObject message) {
        host.onClientData(message);
        return message;
    }

    @Test
    public void aPageHasNoHost() {
        Mockito.doReturn(Map.of()).when(UIContext.get()).getParameterMap();
        assertNull(PHost.get());
        assertTrue(writer.isEmpty(), "nothing created");
    }

    @Test
    public void theCredentialIsTheHostsForThisContext() {
        handshake("{}");
        Mockito.doReturn("ponysdk, ponysdk.credential." + java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("token-1".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .when(UIContext.get()).getHeader(Subprotocols.HEADER);
        assertEquals("token-1", PHost.get().getCredential());
    }

    @Test
    public void noCredentialWhenTheHostGaveNone() {
        handshake("{}");
        assertNull(PHost.get().getCredential());
    }

    @Test
    public void theHandshakeAttributesAreThereBeforeAnyMessage() {
        handshake("{\"instrument\":\"EURUSD\",\"account\":\"A-12\"}");
        final PHost host = PHost.get();
        assertNotNull(host);
        assertSame(host, PHost.get(), "one per context");
        assertEquals("EURUSD", host.getAttribute("instrument"));
        assertEquals(Map.of("instrument", "EURUSD", "account", "A-12"), host.getAttributes());
        assertNull(host.getAttribute("missing"));
        assertEquals(List.of(WidgetType.HOST.getValue()), writer.valuesOf(ServerToClientModel.WIDGET_TYPE));
    }

    @Test
    public void anAttributeChangeReachesItsListenersOnce() {
        handshake("{\"instrument\":\"EURUSD\"}");
        final PHost host = PHost.get();
        final List<String> seen = new ArrayList<>();
        host.addAttributeListener("instrument", seen::add);

        fromTerminal(host, Json.createObjectBuilder()
            .add(ATTRIBUTES, Json.createObjectBuilder().add("instrument", "USDJPY")).build());
        fromTerminal(host, Json.createObjectBuilder()
            .add(ATTRIBUTES, Json.createObjectBuilder().add("instrument", "USDJPY")).build()); // unchanged: not fired
        fromTerminal(host, Json.createObjectBuilder()
            .add(ATTRIBUTES, Json.createObjectBuilder().addNull("instrument")).build()); // removed

        assertEquals(java.util.Arrays.asList("USDJPY", null), seen);
        assertNull(host.getAttribute("instrument"));
        assertFalse(host.getAttributes().containsKey("instrument"));
    }

    @Test
    public void anEventGoesToTheHostWithItsDetail() {
        handshake("{}");
        final PHost host = PHost.get();
        writer.clear();
        host.dispatchEvent("trade", Json.createObjectBuilder().add("side", "BUY").add("quantity", 1_000_000).build());
        host.dispatchEvent("ready");

        final List<Object> events = writer.valuesOf(ServerToClientModel.DISPATCH_EVENT);
        assertEquals(2, events.size());
        assertArrayEquals(new Object[] { "trade", "{\"side\":\"BUY\",\"quantity\":1000000}" }, (Object[]) events.get(0));
        assertArrayEquals(new Object[] { "ready", null }, (Object[]) events.get(1));
    }

    @Test
    public void anEventOfTheHostReachesItsListenersParsed() {
        handshake("{}");
        final PHost host = PHost.get();
        writer.clear();
        final List<JsonValue> seen = new ArrayList<>();
        final HandlerRegistration first = host.addEventListener("refresh", seen::add);
        final HandlerRegistration second = host.addEventListener("refresh", seen::add);
        assertEquals(List.of("refresh"), writer.valuesOf(ServerToClientModel.WC_LISTEN_EVENT), "listened once");

        fromTerminal(host, Json.createObjectBuilder()
            .add(ClientToServerModel.WC_EVENT_NAME.toStringValue(), "refresh")
            .add(ClientToServerModel.WC_EVENT_DETAIL.toStringValue(), "{\"depth\":5}").build());
        fromTerminal(host, Json.createObjectBuilder()
            .add(ClientToServerModel.WC_EVENT_NAME.toStringValue(), "refresh").build());

        assertEquals(4, seen.size());
        assertEquals(5, ((JsonObject) seen.get(0)).getInt("depth"));
        assertEquals(JsonValue.NULL, seen.get(2), "an event without detail");

        first.removeHandler();
        assertEquals(0, writer.count(ServerToClientModel.WC_UNLISTEN_EVENT));
        second.removeHandler();
        assertEquals(List.of("refresh"), writer.valuesOf(ServerToClientModel.WC_UNLISTEN_EVENT));
    }

    @Test
    public void anAttributeListenerGivenAsAMethodReferenceIsRemovedByItsRegistration() {
        // label::setText is a new object at each evaluation: removal by identity removed nothing
        handshake("{\"instrument\":\"EURUSD\"}");
        final PHost host = PHost.get();
        final List<String> seen = new ArrayList<>();
        final HandlerRegistration registration = host.addAttributeListener("instrument", seen::add);
        fromTerminal(host, Json.createObjectBuilder().add(ClientToServerModel.HOST_ATTRIBUTES.toStringValue(),
            Json.createObjectBuilder().add("instrument", "USDJPY")).build());
        registration.removeHandler();
        fromTerminal(host, Json.createObjectBuilder().add(ClientToServerModel.HOST_ATTRIBUTES.toStringValue(),
            Json.createObjectBuilder().add("instrument", "GBPUSD")).build());
        assertEquals(List.of("USDJPY"), seen);
    }
}
