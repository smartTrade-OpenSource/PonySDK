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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.PWebComponent.SlotPanel;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

/**
 * Protocol-level tests for {@link PWebComponent}: what actually goes on the wire for property
 * set / patch / remove, attributes, events, method calls, slots and reconnect replay.
 */
public class PWebComponentProtocolTest extends PSuite {

    private CapturingModelWriter writer;

    @Before
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @After
    public void restoreWriter() {
        writer.uninstall();
    }

    private PWebComponent attached(final String tag) {
        final PWebComponent c = new PWebComponent(tag);
        PWindow.getMain().add(c);
        assertTrue("attached to the (opened) main window", c.isInitialized());
        writer.clear();
        return c;
    }

    // ---- property set / patch ----

    @Test
    public void firstSetSendsFullValue() {
        final PWebComponent c = attached("x-full");
        c.property("data").set("{\"a\":1}");
        assertEquals(List.of("{\"a\":1}"), writer.valuesAfter(ServerToClientModel.WC_SET_PROPERTY, "data"));
        assertEquals(0, writer.count(ServerToClientModel.WC_PATCH_PROPERTY));
    }

    @Test
    public void sameValueTwiceSendsNothing() {
        final PWebComponent c = attached("x-same");
        c.property("data").set("{\"a\":1}");
        writer.clear();
        c.property("data").set("{\"a\":1}");
        assertTrue("no write for an unchanged value", writer.isEmpty());
    }

    @Test
    public void smallChangeInLargeObjectSendsPatch() {
        final PWebComponent c = attached("x-patch");
        final String big = "{\"title\":\"a fairly long title that dwarfs the change\",\"count\":1}";
        c.property("data").set(big);
        writer.clear();
        c.property("data").set(big.replace("\"count\":1", "\"count\":2"));

        assertEquals(List.of("{\"count\":2}"), writer.valuesAfter(ServerToClientModel.WC_PATCH_PROPERTY, "data"));
        assertEquals(0, writer.count(ServerToClientModel.WC_SET_PROPERTY));
    }

    @Test
    public void patchNotSmallerThanValueFallsBackToFullSend() {
        final PWebComponent c = attached("x-nopatch");
        c.property("data").set("{\"a\":1}");
        writer.clear();
        c.property("data").set("{\"b\":2}"); // patch {"b":2,"a":null} is longer than the value
        assertEquals(List.of("{\"b\":2}"), writer.valuesAfter(ServerToClientModel.WC_SET_PROPERTY, "data"));
        assertEquals(0, writer.count(ServerToClientModel.WC_PATCH_PROPERTY));
    }

    @Test
    public void explicitNullInNewValueIsSentInFull() {
        // In a merge-patch null means "delete": the client would drop the key instead of setting null
        final PWebComponent c = attached("x-null");
        final String before = "{\"label\":\"some long enough label to make a patch attractive\",\"sel\":1}";
        c.property("data").set(before);
        writer.clear();
        final String after = before.replace("\"sel\":1", "\"sel\":null");
        c.property("data").set(after);
        assertEquals(List.of(after), writer.valuesAfter(ServerToClientModel.WC_SET_PROPERTY, "data"));
        assertEquals(0, writer.count(ServerToClientModel.WC_PATCH_PROPERTY));
    }

    @Test
    public void scalarPropertyUpdatesAreSentInFull() {
        final PWebComponent c = attached("x-scalar");
        c.property("count").set(1);
        c.property("count").set(2);
        assertEquals(List.of("1", "2"), writer.valuesAfter(ServerToClientModel.WC_SET_PROPERTY, "count"));
        assertEquals(0, writer.count(ServerToClientModel.WC_PATCH_PROPERTY));
    }

    @Test
    public void withoutPatchAlwaysSendsFull() {
        final PWebComponent c = attached("x-wopatch");
        final PWebComponent.PropertyHandle h = c.property("data").withoutPatch();
        final String big = "{\"title\":\"a fairly long title that dwarfs the change\",\"count\":1}";
        h.set(big);
        h.set(big.replace("1}", "2}"));
        assertEquals(2, writer.valuesAfter(ServerToClientModel.WC_SET_PROPERTY, "data").size());
        assertEquals(0, writer.count(ServerToClientModel.WC_PATCH_PROPERTY));
    }

    @Test
    public void offHeapPropertyPatchesToo() {
        final PWebComponent c = attached("x-offheap");
        final PWebComponent.PropertyHandle h = c.property("rows").offHeap();
        final String big = "{\"title\":\"a fairly long title that dwarfs the change\",\"count\":1}";
        h.set(big);
        writer.clear();
        h.set(big);
        assertTrue("off-heap dedupes identical values", writer.isEmpty());
        h.set(big.replace("\"count\":1", "\"count\":7"));
        assertEquals(List.of("{\"count\":7}"), writer.valuesAfter(ServerToClientModel.WC_PATCH_PROPERTY, "rows"));
        assertEquals(big.replace("\"count\":1", "\"count\":7"), h.get());
    }

    @Test
    public void statelessSendsEveryValueEvenIfUnchanged() {
        final PWebComponent c = attached("x-stateless");
        final PWebComponent.PropertyHandle h = c.property("tick").stateless();
        h.set("1");
        h.set("1");
        assertEquals(List.of("1", "1"), writer.valuesAfter(ServerToClientModel.WC_SET_PROPERTY, "tick"));
    }

    @Test
    public void setNullAfterValueSendsFullNull() {
        final PWebComponent c = attached("x-setnull");
        c.property("data").set("{\"a\":1}");
        writer.clear();
        c.property("data").set((String) null);
        assertEquals(1, writer.count(ServerToClientModel.WC_SET_PROPERTY));
        assertNull(writer.valuesAfter(ServerToClientModel.WC_SET_PROPERTY, "data").get(0));
    }

    @Test
    public void removeSendsRemoveOnlyWhenCached() {
        final PWebComponent c = attached("x-remove");
        c.property("never").remove();
        assertEquals("nothing to remove", 0, writer.count(ServerToClientModel.WC_REMOVE_PROPERTY));

        c.property("data").set("{\"a\":1}");
        c.property("data").remove();
        assertEquals(1, writer.count(ServerToClientModel.WC_REMOVE_PROPERTY));
        assertNull(c.property("data").get());

        final PWebComponent.PropertyHandle off = c.property("off").offHeap();
        off.set("{\"a\":1}");
        off.remove();
        assertEquals(2, writer.count(ServerToClientModel.WC_REMOVE_PROPERTY));
    }

    // ---- attributes, events, methods ----

    @Test
    public void attributesEventsAndMethodsAreWritten() {
        final PWebComponent c = attached("x-misc");
        c.attr("aria-label", "Chart");
        c.removeAttr("aria-label");
        final java.util.function.Consumer<jakarta.json.JsonObject> l1 = e -> {};
        final java.util.function.Consumer<jakarta.json.JsonObject> l2 = e -> {};
        c.onEvent("point-click", l1);
        c.onEvent("point-click", l2); // second listener: no new LISTEN on the wire
        c.removeEventListener("point-click", l1);
        c.removeEventListener("point-click", l2); // last one: UNLISTEN
        c.onEvent("hover", l1);
        c.removeAllEventListeners("hover");
        c.removeAllEventListeners("unknown"); // no-op
        c.call("refresh");
        c.call("zoom", 2, "x");

        assertEquals(List.of("Chart"), writer.valuesAfter(ServerToClientModel.WC_SET_ATTRIBUTE, "aria-label"));
        assertEquals(1, writer.count(ServerToClientModel.WC_REMOVE_ATTRIBUTE));
        assertEquals(2, writer.count(ServerToClientModel.WC_LISTEN_EVENT));
        assertEquals(2, writer.count(ServerToClientModel.WC_UNLISTEN_EVENT));
        assertEquals(2, writer.count(ServerToClientModel.WC_CALL_METHOD));
        assertEquals(1, writer.count(ServerToClientModel.WC_METHOD_ARGS));
    }

    // ---- creation & reconnect replay ----

    @Test
    public void stateSetBeforeAttachIsReplayedAtCreation() {
        final PWebComponent c = new PWebComponent("x-early");
        c.property("data").set("{\"a\":1}");
        c.attr("theme", "dark");
        c.onEvent("ping", e -> {});
        c.property("off").offHeap().set("{\"b\":2}");
        writer.clear();

        PWindow.getMain().add(c);

        final List<ServerToClientModel> models = writer.models();
        assertTrue(models.contains(ServerToClientModel.TYPE_CREATE));
        assertEquals(List.of("x-early"), writer.valuesOf(ServerToClientModel.WC_TAG_NAME));
        assertTrue(writer.valuesAfter(ServerToClientModel.WC_SET_PROPERTY, "data").contains("{\"a\":1}"));
        assertTrue(writer.valuesAfter(ServerToClientModel.WC_SET_PROPERTY, "off").contains("{\"b\":2}"));
        assertTrue(writer.valuesAfter(ServerToClientModel.WC_SET_ATTRIBUTE, "theme").contains("dark"));
        assertTrue(writer.count(ServerToClientModel.WC_LISTEN_EVENT) >= 1);
    }

    @Test
    public void stateSetBeforeAttachIsSentOnlyOnce() {
        final PWebComponent c = new PWebComponent("x-once");
        c.property("data").set("{\"a\":1}");
        c.property("data").set("{\"a\":2}");
        c.attr("theme", "dark");
        c.onEvent("ping", e -> {});
        writer.clear();

        PWindow.getMain().add(c);

        assertEquals("final value only, sent once", List.of("{\"a\":2}"),
                writer.valuesAfter(ServerToClientModel.WC_SET_PROPERTY, "data"));
        assertEquals(0, writer.count(ServerToClientModel.WC_PATCH_PROPERTY));
        assertEquals(1, writer.valuesAfter(ServerToClientModel.WC_SET_ATTRIBUTE, "theme").size());
        assertEquals(1, writer.count(ServerToClientModel.WC_LISTEN_EVENT));
    }

    @Test
    public void changesUndoneBeforeAttachSendNothing() {
        final PWebComponent c = new PWebComponent("x-undone");
        c.property("data").set("{\"a\":1}");
        c.property("data").remove();
        c.attr("theme", "dark");
        c.removeAttr("theme");
        final java.util.function.Consumer<jakarta.json.JsonObject> l = e -> {};
        c.onEvent("ping", l);
        c.removeEventListener("ping", l);
        c.onEvent("pong", l);
        c.removeAllEventListeners("pong");
        writer.clear();

        PWindow.getMain().add(c);

        for (final ServerToClientModel m : new ServerToClientModel[] { ServerToClientModel.WC_SET_PROPERTY,
                ServerToClientModel.WC_REMOVE_PROPERTY, ServerToClientModel.WC_SET_ATTRIBUTE,
                ServerToClientModel.WC_REMOVE_ATTRIBUTE, ServerToClientModel.WC_LISTEN_EVENT,
                ServerToClientModel.WC_UNLISTEN_EVENT }) {
            assertEquals(m + " must not be sent", 0, writer.count(m));
        }
    }

    @Test
    public void methodCallsBeforeAttachAreStillDelivered() {
        final PWebComponent c = new PWebComponent("x-callearly");
        c.call("init");
        c.property("tick").stateless().set("5"); // stateless: not replayed, so must be stacked
        writer.clear();

        PWindow.getMain().add(c);

        assertEquals(1, writer.count(ServerToClientModel.WC_CALL_METHOD));
        assertEquals(List.of("5"), writer.valuesAfter(ServerToClientModel.WC_SET_PROPERTY, "tick"));
    }

    // ---- slots ----

    @Test
    public void slotPanelIsIdempotentAndAddsWithSlotName() {
        final PWebComponent c = attached("x-slots");
        final SlotPanel toolbar = c.slot("toolbar");
        assertSame(toolbar, c.slot("toolbar"));
        assertEquals("toolbar", toolbar.getSlotName());

        final PLabel label = Element.newPLabel("hi");
        toolbar.add(label);

        assertTrue("child initialized through the slot", label.isInitialized());
        assertSame(toolbar, label.getParent());
        assertEquals(List.of("toolbar"), writer.valuesOf(ServerToClientModel.WC_SLOT_NAME));
        assertEquals(List.of(c.getID()), writer.valuesOf(ServerToClientModel.PARENT_OBJECT_ID));
        assertEquals(1, writer.count(ServerToClientModel.TYPE_ADD));
    }

    @Test
    public void slotDeclaredBeforeAttachInitializesChildrenOnAttach() {
        final PWebComponent c = new PWebComponent("x-slot-early");
        final PLabel label = Element.newPLabel("early");
        c.slot("content").add(label);
        assertFalse(label.isInitialized());

        PWindow.getMain().add(c);

        assertTrue("slot children are initialized when the WC attaches", label.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.WC_SLOT_NAME).contains("content"));
    }

    @Test
    public void slotPanelIsNotAStandaloneWidget() {
        final SlotPanel sp = new PWebComponent("x-slot-type").slot("s");
        assertThrows(UnsupportedOperationException.class, sp::getWidgetType);
        assertThrows(IllegalStateException.class, () -> sp.add(sp));
    }

    // ---- lifecycle ----

    @Test
    public void destroyReleasesOffHeapMemory() {
        final PWebComponent c = attached("x-destroy");
        c.property("rows").offHeap().set("{\"rows\":[1,2,3]}");
        assertTrue(c.getOffHeapBytes() > 0);
        c.onDestroy();
        assertEquals(0, c.getOffHeapBytes());
        assertTrue(c.toString().contains("x-destroy"));
    }
}
