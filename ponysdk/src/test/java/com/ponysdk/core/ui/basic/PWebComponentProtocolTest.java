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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.PWebComponent.SlotPanel;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

/**
 * Protocol-level tests for {@link PWebComponent}: what actually goes on the wire for property
 * set / patch / remove, attributes, events, method calls, slots and reconnect replay.
 */
public class PWebComponentProtocolTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private PWebComponent attached(final String tag) {
        final PWebComponent c = new PWebComponent(tag);
        PWindow.getMain().add(c);
        assertTrue(c.isInitialized(), "attached to the (opened) main window");
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
        assertTrue(writer.isEmpty(), "no write for an unchanged value");
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
        assertTrue(writer.isEmpty(), "off-heap dedupes identical values");
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
        assertEquals(0, writer.count(ServerToClientModel.WC_REMOVE_PROPERTY), "nothing to remove");

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
        c.setAttribute("aria-label", "Chart");
        c.removeAttribute("aria-label");
        final HandlerRegistration first = c.addEventListener("point-click", e -> {});
        final HandlerRegistration second = c.addEventListener("point-click", e -> {}); // no new LISTEN on the wire
        first.removeHandler();
        second.removeHandler(); // last one: UNLISTEN
        second.removeHandler(); // twice: no-op
        c.addEventListener("hover", e -> {}).removeHandler();
        c.call("refresh");
        c.call("zoom", 2, "x");

        assertEquals(List.of("Chart"), writer.valuesAfter(ServerToClientModel.PUT_ATTRIBUTE_KEY, "aria-label"));
        assertEquals(1, writer.count(ServerToClientModel.REMOVE_ATTRIBUTE_KEY));
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
        c.setAttribute("theme", "dark");
        c.addEventListener("ping", e -> {});
        c.property("off").offHeap().set("{\"b\":2}");
        writer.clear();

        PWindow.getMain().add(c);

        final List<ServerToClientModel> models = writer.models();
        assertTrue(models.contains(ServerToClientModel.TYPE_CREATE));
        assertEquals(List.of("x-early"), writer.valuesOf(ServerToClientModel.WC_TAG_NAME));
        assertTrue(writer.valuesAfter(ServerToClientModel.WC_SET_PROPERTY, "data").contains("{\"a\":1}"));
        assertTrue(writer.valuesAfter(ServerToClientModel.WC_SET_PROPERTY, "off").contains("{\"b\":2}"));
        assertTrue(writer.valuesAfter(ServerToClientModel.PUT_ATTRIBUTE_KEY, "theme").contains("dark"));
        assertTrue(writer.count(ServerToClientModel.WC_LISTEN_EVENT) >= 1);
    }

    @Test
    public void stateSetBeforeAttachIsSentOnlyOnce() {
        final PWebComponent c = new PWebComponent("x-once");
        c.property("data").set("{\"a\":1}");
        c.property("data").set("{\"a\":2}");
        c.setAttribute("theme", "dark");
        c.addEventListener("ping", e -> {});
        writer.clear();

        PWindow.getMain().add(c);

        assertEquals(List.of("{\"a\":2}"), writer.valuesAfter(ServerToClientModel.WC_SET_PROPERTY, "data"),
                "final value only, sent once");
        assertEquals(0, writer.count(ServerToClientModel.WC_PATCH_PROPERTY));
        assertEquals(1, writer.valuesAfter(ServerToClientModel.PUT_ATTRIBUTE_KEY, "theme").size());
        assertEquals(1, writer.count(ServerToClientModel.WC_LISTEN_EVENT));
    }

    @Test
    public void changesUndoneBeforeAttachSendNothing() {
        final PWebComponent c = new PWebComponent("x-undone");
        c.property("data").set("{\"a\":1}");
        c.property("data").remove();
        c.setAttribute("theme", "dark");
        c.removeAttribute("theme");
        c.addEventListener("ping", e -> {}).removeHandler();
        writer.clear();

        PWindow.getMain().add(c);

        for (final ServerToClientModel m : new ServerToClientModel[] { ServerToClientModel.WC_SET_PROPERTY,
                ServerToClientModel.WC_REMOVE_PROPERTY, ServerToClientModel.PUT_ATTRIBUTE_KEY,
                ServerToClientModel.REMOVE_ATTRIBUTE_KEY, ServerToClientModel.WC_LISTEN_EVENT,
                ServerToClientModel.WC_UNLISTEN_EVENT }) {
            assertEquals(0, writer.count(m), m + " must not be sent");
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

        assertTrue(label.isInitialized(), "child initialized through the slot");
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

        assertTrue(label.isInitialized(), "slot children are initialized when the WC attaches");
        assertTrue(writer.valuesOf(ServerToClientModel.WC_SLOT_NAME).contains("content"));
    }

    @Test
    public void slotPanelIsNotAStandaloneWidget() {
        final SlotPanel sp = new PWebComponent("x-slot-type").slot("s");
        assertThrows(UnsupportedOperationException.class, sp::getWidgetType);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> sp.add(sp));
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

    @Test
    public void offHeapSetNullSendsFullNullLikeOnHeap() {
        final PWebComponent c = attached("x-offnull");
        final PWebComponent.PropertyHandle h = c.property("rows").offHeap();
        h.set((String) null);
        assertTrue(writer.isEmpty(), "nothing held: nothing to clear");
        h.set("{\"a\":1}");
        writer.clear();
        h.set((String) null);
        assertEquals(1, writer.count(ServerToClientModel.WC_SET_PROPERTY));
        assertNull(writer.valuesAfter(ServerToClientModel.WC_SET_PROPERTY, "rows").get(0));
        assertNull(h.get());

        final PWebComponent early = new PWebComponent("x-offnull-early");
        early.property("rows").offHeap().set("{\"a\":1}");
        early.property("rows").set((String) null);
        writer.clear();
        PWindow.getMain().add(early);
        assertEquals(0, writer.count(ServerToClientModel.WC_SET_PROPERTY), "an empty store replays nothing");
    }

    @Test
    public void offHeapWithoutPatchSendsFullValues() {
        final PWebComponent c = attached("x-offfull");
        final PWebComponent.PropertyHandle h = c.property("rows").offHeap().withoutPatch();
        assertFalse(h.isPatchEnabled());
        h.set("{\"a\":1,\"b\":2}");
        h.set("{\"a\":1,\"b\":3}");
        assertEquals(List.of("{\"a\":1,\"b\":2}", "{\"a\":1,\"b\":3}"), writer.valuesAfter(ServerToClientModel.WC_SET_PROPERTY, "rows"));
        assertEquals(0, writer.count(ServerToClientModel.WC_PATCH_PROPERTY));
    }

    @Test
    public void patchFlagsAndStorageOfSeveralProperties() {
        final PWebComponent c = attached("x-flags");
        assertFalse(c.property("a").withoutPatch().isPatchEnabled(), "nothing enabled yet");
        assertTrue(c.property("b").withPatch().isPatchEnabled());
        assertTrue(c.property("c").withPatch().isPatchEnabled());
        assertFalse(c.property("b").withoutPatch().isPatchEnabled());

        assertNull(c.property("on").get(), "on-heap, no store yet");
        assertNull(c.property("off").offHeap().get(), "off-heap, no store yet");
        c.property("off").remove();
        c.property("on").set("1");
        c.property("off").set("2");
        // other properties hold values: the storage of a new one can still be chosen
        assertEquals(PWebComponent.StorageMode.STATELESS, c.property("s").stateless().getMode());
        assertEquals(PWebComponent.StorageMode.OFF_HEAP, c.property("o2").offHeap().getMode());
        c.property("o2").remove();
        c.property("s").remove();
        assertEquals(0, writer.count(ServerToClientModel.WC_REMOVE_PROPERTY));
    }

    @Test
    public void slotPanelRefusesAChildOfAnotherWindowAndAttachesOnce() {
        final PWebComponent c = attached("x-slot-window");
        final SlotPanel slot = c.slot("s");
        final PWindow other = Element.newPWindow(null, null);
        other.open();
        final PLabel foreign = Element.newPLabel("f");
        other.add(foreign);
        assertThrows(IllegalAccessError.class, () -> slot.add(foreign));

        final PLabel a = Element.newPLabel("a");
        final PLabel b = Element.newPLabel("b");
        PWindow.getMain().add(b);
        slot.add(a);
        slot.add(b);
        assertEquals(2, slot.getWidgetCount());
        assertSame(slot, b.getParent());
        assertFalse(slot.attach(PWindow.getMain(), null), "already attached");
    }

    @Test
    public void anEventWithoutListenersIsDropped() {
        final PWebComponent c = attached("x-noevent");
        c.onClientData(jakarta.json.Json.createObjectBuilder()
            .add(com.ponysdk.core.model.ClientToServerModel.WC_EVENT_NAME.toStringValue(), "ping").build());
        final HandlerRegistration r = c.addEventListener("ping", e -> {});
        c.addEventListener("pong", e -> {});
        r.removeHandler();
        r.removeHandler();
        assertEquals(List.of("ping"), writer.valuesOf(ServerToClientModel.WC_UNLISTEN_EVENT));
        c.onDestroy();
        assertEquals(0, c.getOffHeapBytes());
    }
}
