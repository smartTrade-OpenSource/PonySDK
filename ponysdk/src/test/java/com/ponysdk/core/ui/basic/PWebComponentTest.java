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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import org.junit.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.PWebComponent.PropertyHandle;
import com.ponysdk.core.ui.basic.PWebComponent.StorageMode;
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
        assertSame("property(name) must return the same handle", a, c.property("title"));
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
        assertFalse("stateless disables patch", stateless.isPatchEnabled());

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
        assertTrue("off-heap buffer should be allocated", c.getOffHeapBytes() > 0);
    }

    @Test
    public void statelessIsNotCached() {
        final PropertyHandle tick = new PWebComponent("my-chart").property("tick").stateless();
        tick.set("42");
        assertNull("stateless properties are never cached server-side", tick.get());
    }

    // ---- attributes ----

    @Test
    public void attributeSetGetRemove() {
        final PWebComponent c = new PWebComponent("my-chart");
        assertNull(c.getAttr("aria-label"));
        c.attr("aria-label", "Revenue chart");
        assertEquals("Revenue chart", c.getAttr("aria-label"));
        c.removeAttr("aria-label");
        assertNull(c.getAttr("aria-label"));
    }

    // ---- event dispatch ----

    @Test
    public void onEventDispatchesClientData() {
        final PWebComponent c = new PWebComponent("my-chart");
        final boolean[] fired = { false };
        c.onEvent("point-click", ev -> fired[0] = true);

        final JsonObject clientEvent = Json.createObjectBuilder()
                .add(ClientToServerModel.WC_EVENT_NAME.toStringValue(), "point-click")
                .build();
        c.onClientData(clientEvent);

        assertTrue("registered listener must receive the custom event", fired[0]);
    }

    // ---- computeJsonPatch (RFC 7396 merge-patch, shallow/top-level) ----

    @Test
    public void patchEmitsOnlyChangedAndAddedKeys() {
        final JsonObject patch = patch("{\"a\":1,\"b\":2}", "{\"a\":1,\"b\":3,\"c\":4}");
        assertFalse("unchanged key omitted", patch.containsKey("a"));
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

    private static JsonObject patch(final String oldJson, final String newJson) {
        final String result = PWebComponent.computeJsonPatch(oldJson, newJson);
        try (final var reader = Json.createReader(new StringReader(result))) {
            return reader.readObject();
        }
    }
}
