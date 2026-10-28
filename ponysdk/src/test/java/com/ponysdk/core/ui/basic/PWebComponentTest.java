/*
 * Copyright (c) 2011 PonySDK
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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.PWebComponent.PropertyHandle;
import com.ponysdk.core.ui.basic.PWebComponent.StorageMode;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.test.PSuite;

/**
 * Unit tests for {@link PWebComponent} — tag validation, the unified PropertyHandle API
 * (storage modes, patch flags, idempotency), per-mode set/get, attributes, event dispatch,
 * and the RFC 7396 merge-patch diff ({@link PWebComponent#computeJsonPatch}).
 */
public class PWebComponentTest extends PSuite {

    // ---- Tag name validation ----

    @Test
    public void tagNameMustContainHyphen() {
        assertThrows(IllegalArgumentException.class, () -> new PWebComponent("nohyphen"));
        assertThrows(IllegalArgumentException.class, () -> new PWebComponent(null));
        final PWebComponent ok = new PWebComponent("my-chart");
        assertEquals("my-chart", ok.getTagName());
        assertEquals(WidgetType.WEB_COMPONENT, ok.getWidgetType());
    }

    // ---- PropertyHandle configuration ----

    @Test
    public void propertyHandleIsIdempotent() {
        final PWebComponent c = new PWebComponent("my-chart");
        final PropertyHandle a = c.property("title");
        assertSame(a, c.property("title"), "property(name) must return the same handle");
    }

    @Test
    public void defaultModeIsOnHeapWithPatch() {
        final PropertyHandle h = new PWebComponent("my-chart").property("title");
        assertEquals(StorageMode.ON_HEAP, h.getMode());
        assertTrue(h.isPatchEnabled());
    }

    @Test
    public void storageModeTransitions() {
        final PWebComponent c = new PWebComponent("my-chart");
        assertEquals(StorageMode.OFF_HEAP, c.property("data").offHeap().getMode());

        final PropertyHandle stateless = c.property("tick").stateless();
        assertEquals(StorageMode.STATELESS, stateless.getMode());
        assertFalse(stateless.isPatchEnabled(), "stateless disables patch");

        assertFalse(c.property("snap").withoutPatch().isPatchEnabled());
    }

    @Test
    public void withPatchOnStatelessThrows() {
        final PWebComponent c = new PWebComponent("my-chart");
        assertThrows(IllegalStateException.class, () -> c.property("tick").stateless().withPatch());
    }

    // ---- set / get per storage mode ----

    @Test
    public void onHeapSetGetRoundTrip() {
        final PWebComponent c = new PWebComponent("my-chart");
        final PropertyHandle title = c.property("title");
        title.set("\"Revenue Q4\"");
        assertEquals("\"Revenue Q4\"", title.get());
    }

    @Test
    public void offHeapSetGetRoundTripAndBytes() {
        final PWebComponent c = new PWebComponent("my-chart");
        assertEquals(0, c.getOffHeapBytes());
        final PropertyHandle dataset = c.property("dataset").offHeap();
        dataset.set("{\"rows\":[1,2,3]}");
        assertEquals("{\"rows\":[1,2,3]}", dataset.get());
        assertTrue(c.getOffHeapBytes() > 0, "off-heap buffer should be allocated");
    }

    @Test
    public void statelessIsNotCached() {
        final PropertyHandle tick = new PWebComponent("my-chart").property("tick").stateless();
        tick.set("42");
        assertNull(tick.get(), "stateless properties are never cached server-side");
    }

    // ---- attributes: PWidget's, one way (docs/decisions/0011-one-way-to-listen.md) ----

    @Test
    public void attributeSetGetRemove() {
        final PWebComponent c = new PWebComponent("my-chart");
        assertNull(c.getAttribute("aria-label"));
        c.setAttribute("aria-label", "Revenue chart");
        assertEquals("Revenue chart", c.getAttribute("aria-label"));
        c.removeAttribute("aria-label");
        assertNull(c.getAttribute("aria-label"));
    }

    // ---- event dispatch ----

    private static JsonObject customEvent(final String name, final String detail) {
        final var builder = Json.createObjectBuilder().add(ClientToServerModel.WC_EVENT_NAME.toStringValue(), name);
        if (detail != null) builder.add(ClientToServerModel.WC_EVENT_DETAIL.toStringValue(), detail);
        return builder.build();
    }

    @Test
    public void aListenerReceivesTheParsedDetail() {
        final PWebComponent c = new PWebComponent("my-chart");
        final List<JsonValue> details = new ArrayList<>();
        c.addEventListener("point-click", details::add);

        c.onClientData(customEvent("point-click", "{\"x\":3}"));
        c.onClientData(customEvent("point-click", null));
        c.onClientData(customEvent("point-click", "not json"));
        c.onClientData(customEvent("other", "{}"));

        assertEquals(3, details.size(), "the other event is not delivered");
        assertEquals(3, details.get(0).asJsonObject().getInt("x"));
        assertEquals(JsonValue.NULL, details.get(1));
        assertEquals(Json.createValue("not json"), details.get(2));
    }

    @Test
    public void aListenerMayRemoveItselfWhileCalled() {
        final PWebComponent c = new PWebComponent("my-chart");
        final List<String> calls = new ArrayList<>();
        final HandlerRegistration[] once = new HandlerRegistration[1];
        once[0] = c.addEventListener("point-click", detail -> {
            calls.add("once");
            once[0].removeHandler();
        });
        c.addEventListener("point-click", detail -> calls.add("always"));

        c.onClientData(customEvent("point-click", null));
        c.onClientData(customEvent("point-click", null));

        assertEquals(List.of("once", "always", "always"), calls);
    }

    @Test
    public void theStorageIsChosenBeforeTheFirstValue() {
        final PWebComponent c = new PWebComponent("my-chart");
        c.property("data").set("{\"a\":1}");
        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> c.property("data").offHeap());
        assertEquals("offHeap() on property 'data', which holds a value: choose the storage before the first set", e.getMessage());
        assertThrows(IllegalStateException.class, () -> c.property("data").stateless());
        assertEquals("{\"a\":1}", c.property("data").get(), "the value stays where it is");
        c.property("rows").offHeap().withPatch().set("[]"); // chosen first: fine
    }

    // ---- computeJsonPatch (RFC 7396 merge-patch, shallow/top-level) ----

    @Test
    public void patchEmitsOnlyChangedAndAddedKeys() {
        final JsonObject patch = patch("{\"a\":1,\"b\":2}", "{\"a\":1,\"b\":3,\"c\":4}");
        assertFalse(patch.containsKey("a"), "unchanged key omitted");
        assertEquals(3, patch.getInt("b"));
        assertEquals(4, patch.getInt("c"));
        assertEquals(2, patch.size());
    }

    @Test
    public void patchEmitsNullForRemovedKeys() {
        final JsonObject patch = patch("{\"a\":1,\"b\":2}", "{\"a\":1}");
        assertEquals(JsonValue.NULL, patch.get("b"));
        assertEquals(1, patch.size());
    }

    @Test
    public void identicalValuesProduceNoPatch() {
        assertNull(PWebComponent.computeJsonPatch("{\"a\":1}", "{\"a\":1}"));
    }

    @Test
    public void nonObjectJsonProducesNoPatch() {
        // Arrays / scalars are not mergeable objects → caller falls back to a full send.
        assertNull(PWebComponent.computeJsonPatch("[1,2,3]", "[1,2,4]"));
        assertNull(PWebComponent.computeJsonPatch("not json", "{\"a\":1}"));
        assertNull(PWebComponent.computeJsonPatch("{\"a\":1}", "oops"));
    }

    @Test
    public void nestedObjectIsReplacedWholesale_shallowDiff() {
        // The diff is shallow (top-level): a changed nested object is emitted in full,
        // not as a nested sub-patch. Documents the implemented behaviour.
        final JsonObject patch = patch("{\"a\":{\"x\":1,\"y\":2}}", "{\"a\":{\"x\":1}}");
        assertTrue(patch.containsKey("a"));
        assertEquals(JsonValue.ValueType.OBJECT, patch.get("a").getValueType());
        assertEquals(1, patch.getJsonObject("a").size());
        assertEquals(1, patch.getJsonObject("a").getInt("x"));
    }

    @Test
    public void malformedObjectsProduceNoPatch() {
        assertNull(PWebComponent.computeJsonPatch("   ", "{\"a\":1}"));
        assertNull(PWebComponent.computeJsonPatch("{ broken", "{\"a\":1}"));
        assertNull(PWebComponent.computeJsonPatch(null, "{\"a\":1}"));
        assertNull(PWebComponent.computeJsonPatch("{\"a\":1}", "{ broken"));
        assertEquals("{\"a\":2}", PWebComponent.computeJsonPatch(" \t\r\n{\"a\":1}", "\n{\"a\":2}"));
    }

    // ---- Typed setters, removal, slots, JSON helpers ----

    @Test
    public void typedSettersStoreTheTextValue() {
        final PWebComponent c = new PWebComponent("my-chart");
        final PropertyHandle h = c.property("v");
        assertEquals("v", h.getName());
        h.set(1.5);
        assertEquals("1.5", h.get());
        h.set(true);
        assertEquals("true", h.get());
    }

    @Test
    public void removingAStatelessPropertyHoldsNothing() {
        final PWebComponent c = new PWebComponent("my-chart");
        final PropertyHandle h = c.property("tick").stateless();
        h.set("1");
        h.remove();
        assertNull(h.get());
    }

    @Test
    public void slotRejectsAWidgetOfAnotherWindow() {
        final PWebComponent c = new PWebComponent("my-chart");
        final PWebComponent.SlotPanel slot = c.slot("content");
        assertEquals("content", slot.getSlotName());
        final PLabel other = Element.newPLabel();
        final PWindow window = Element.newPWindow(null, null);
        other.attach(window, null);
        assertThrows(IllegalAccessError.class, () -> slot.add(other));
    }

    @Test
    public void slotAttachIsDoneOnce() {
        final PWebComponent c = new PWebComponent("my-chart");
        final PWebComponent.SlotPanel slot = c.slot("content");
        final PLabel child = Element.newPLabel();
        slot.add(child);
        PWindow.getMain().add(c);
        assertSame(PWindow.getMain(), child.getWindow());
        assertFalse(slot.attach(PWindow.getMain(), null));
    }

    @Test
    public void childAddedBeforeAttachIsAttachedWithTheComponent() {
        final PWebComponent c = new PWebComponent("my-chart");
        final PLabel child = Element.newPLabel();
        c.add(child);
        PWindow.getMain().add(c);
        assertSame(PWindow.getMain(), child.getWindow());
        assertTrue(child.isInitialized());
    }

    @Test
    public void jsonHelpersOfGeneratedClasses() {
        assertEquals("\"a\\\"b\"", PWebComponent.jsonOf("a\"b"));
        assertEquals("42", PWebComponent.jsonOf(42.0));
        assertEquals("1.5", PWebComponent.jsonOf(1.5));
        assertEquals("1.0E16", PWebComponent.jsonOf(1e16));
        assertEquals("x", PWebComponent.stringOf("\"x\""));
        assertNull(PWebComponent.stringOf("1"));
        assertNull(PWebComponent.stringOf("not json"));
        assertEquals(2.5, PWebComponent.numberOf("2.5"));
        assertNull(PWebComponent.numberOf("\"x\""));
        assertEquals(Boolean.TRUE, PWebComponent.booleanOf("true"));
        assertEquals(Boolean.FALSE, PWebComponent.booleanOf("false"));
        assertNull(PWebComponent.booleanOf("1"));
        assertNull(PWebComponent.booleanOf(null));
    }

    @Test
    public void nonCustomEventDataReachesTheTerminalHandler() {
        final PWebComponent c = new PWebComponent("my-chart");
        final List<JsonObject> received = new ArrayList<>();
        c.setTerminalHandler(e -> received.add(e.getData()));
        final JsonObject data = Json.createObjectBuilder().add("k", 1).build();
        c.onClientData(Json.createObjectBuilder().add(ClientToServerModel.NATIVE.toStringValue(), data).build());
        assertEquals(List.of(data), received);
    }

    private static JsonObject patch(final String oldJson, final String newJson) {
        final String result = PWebComponent.computeJsonPatch(oldJson, newJson);
        try (final var reader = Json.createReader(new StringReader(result))) {
            return reader.readObject();
        }
    }
}
