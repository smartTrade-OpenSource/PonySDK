/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.event.PVisibilityEvent;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PObjectTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private static PLabel attachedLabel() {
        final PLabel label = Element.newPLabel();
        PWindow.getMain().add(label);
        return label;
    }

    @Test
    public void identity() {
        final PLabel a = Element.newPLabel();
        final PLabel b = Element.newPLabel();
        assertNotEquals(a.getID(), b.getID());
        assertEquals(a, a);
        assertNotEquals(a, b);
        assertEquals(System.identityHashCode(a), a.hashCode());
        assertTrue(a.toString().startsWith("PLabel#" + a.getID()));
        assertNull(a.getWindow());
        assertNull(a.getFrame());
    }

    @Test
    public void data() {
        final PLabel label = Element.newPLabel();
        assertNull(label.getData());
        label.setData(null);
        assertNull(label.getData());
        label.setData(42);
        assertEquals(42, label.getData());
        label.setData(null);
        assertNull(label.getData());
    }

    @Test
    public void initializeListenersFireOnceAtInit() {
        final PLabel label = Element.newPLabel();
        final List<PObject> initialized = new ArrayList<>();
        label.addInitializeListener(initialized::add);
        label.addInitializeListener(o -> initialized.add(o));
        assertTrue(initialized.isEmpty());
        PWindow.getMain().add(label);
        assertEquals(List.of(label, label), initialized);
        assertSame(PWindow.getMain(), label.getWindow());

        // already initialized: never called
        label.addInitializeListener(initialized::add);
        assertEquals(2, initialized.size());
    }

    @Test
    public void stackedUpdatesKeepTheLastValuePerModel() {
        final PLabel label = Element.newPLabel();
        label.focus();
        label.blur();
        label.focus();
        PWindow.getMain().add(label);
        assertEquals(List.of(true), writer.valuesOf(ServerToClientModel.FOCUS));
    }

    @Test
    public void initIsDeferredUntilTheWindowOpens() {
        final PWindow window = Element.newPWindow(null, null);
        final PLabel label = Element.newPLabel();
        window.add(label);
        assertFalse(label.isInitialized());
        window.open();
        assertTrue(label.isInitialized());
        assertSame(window, label.getWindow());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_CREATE).contains(label.getID()));
    }

    @Test
    public void attachIsIdempotentOnTheSameWindow() {
        final PWindow window = Element.newPWindow(null, null);
        final PLabel label = Element.newPLabel();
        assertTrue(label.attach(window, null));
        assertFalse(label.attach(window, null));
        assertFalse(label.isInitialized());
        window.open();
        assertTrue(label.isInitialized());
    }

    @Test
    public void attachToAnotherWindowFails() {
        final PLabel label = attachedLabel();
        final PWindow other = Element.newPWindow(null, null);
        assertThrows(IllegalAccessError.class, () -> label.attach(other, null));
    }

    @Test
    public void objectInAFrameWaitsForTheFrameAndWritesItsId() {
        final PFrame frame = Element.newPFrame("url");
        final PLabel label = Element.newPLabel();
        label.attach(PWindow.getMain(), frame);
        assertFalse(label.isInitialized());
        assertSame(frame, label.getFrame());

        PWindow.getMain().add(frame);
        assertTrue(frame.isInitialized());
        assertTrue(label.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.FRAME_ID).contains(frame.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_CREATE).contains(label.getID()));

        writer.clear();
        final PVisibilityEvent.PVisibilityHandler handler = e -> {
        };
        label.setTitle("t");
        label.addVisibilityHandler(handler);
        label.removeVisibilityHandler(handler);
        assertEquals(List.of(frame.getID(), frame.getID(), frame.getID()), writer.valuesOf(ServerToClientModel.FRAME_ID));
        assertSame(frame, label.updateGroup());
        assertSame(PWindow.getMain(), attachedLabel().updateGroup());
    }

    @Test
    public void addAndRemoveInAFrameWriteTheFrameId() {
        final PFrame frame = Element.newPFrame("url");
        PWindow.getMain().add(frame);
        final PFlowPanel panel = Element.newPFlowPanel();
        panel.attach(PWindow.getMain(), frame);
        assertTrue(panel.isInitialized());

        writer.clear();
        final PLabel child = Element.newPLabel();
        panel.add(child);
        panel.remove(child);
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_REMOVE).contains(child.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.FRAME_ID).size() >= 2);
        assertTrue(writer.valuesOf(ServerToClientModel.FRAME_ID).stream().allMatch(id -> id.equals(frame.getID())));
    }

    @Test
    public void addAndRemoveOfAChildAreWritten() {
        final PFlowPanel panel = Element.newPFlowPanel();
        PWindow.getMain().add(panel);
        final PLabel child = Element.newPLabel();
        PWindow.getMain().add(child);
        writer.clear();

        // already created: removed from the root panel, then a plain add block
        panel.add(child);
        assertEquals(List.of(child.getID()), writer.valuesOf(ServerToClientModel.TYPE_REMOVE));
        assertEquals(List.of(child.getID()), writer.valuesOf(ServerToClientModel.TYPE_ADD));
        assertEquals(List.of(PWindow.getMain().getPRootPanel().getID(), panel.getID()),
            writer.valuesOf(ServerToClientModel.PARENT_OBJECT_ID));

        writer.clear();
        panel.remove(child);
        assertEquals(List.of(child.getID()), writer.valuesOf(ServerToClientModel.TYPE_REMOVE));
        assertEquals(List.of(panel.getID()), writer.valuesOf(ServerToClientModel.PARENT_OBJECT_ID));
    }

    @Test
    public void insertAtAnIndexWritesTheIndex() {
        final PFlowPanel panel = Element.newPFlowPanel();
        PWindow.getMain().add(panel);
        panel.add(Element.newPLabel());
        panel.add(Element.newPLabel());
        writer.clear();

        final PLabel child = Element.newPLabel();
        panel.insert(child, 0);
        assertEquals(List.of(child.getID()), writer.valuesOf(ServerToClientModel.TYPE_ADD));
        assertEquals(List.of(0), writer.valuesOf(ServerToClientModel.INDEX));
    }

    @Test
    public void stackedStructureAndHandlersAreReplayedOnInit() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel child = Element.newPLabel();
        final PVisibilityEvent.PVisibilityHandler handler = e -> {
        };
        panel.add(child);
        child.addVisibilityHandler(handler);
        child.removeVisibilityHandler(handler);
        panel.remove(child);
        assertTrue(writer.isEmpty());

        child.attach(PWindow.getMain(), null);
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_REMOVE).contains(child.getID()));
        assertEquals(List.of(child.getID()), writer.valuesOf(ServerToClientModel.TYPE_ADD_HANDLER));
        assertEquals(1, writer.count(ServerToClientModel.TYPE_REMOVE_HANDLER));
    }

    @Test
    public void terminalFunctions() {
        final PLabel label = attachedLabel();
        writer.clear();
        label.bindTerminalFunction("fn");
        assertEquals(List.of("fn"), writer.valuesOf(ServerToClientModel.BIND));

        final JsonObject data = Json.createObjectBuilder().add("a", 1).build();
        label.sendToNative(data);
        assertEquals(List.of(data.toString()), writer.valuesOf(ServerToClientModel.NATIVE));

        label.callTerminalFunction("noArgs");
        label.callTerminalFunction("withArgs", 1, "two");
        assertEquals(List.of("noArgs", "withArgs"), writer.valuesOf(ServerToClientModel.FUNCTION_NAME));
        assertEquals(1, writer.count(ServerToClientModel.FUNCTION_ARGS));
        assertArrayEquals(new Object[] { 1, "two" }, (Object[]) writer.valuesOf(ServerToClientModel.FUNCTION_ARGS).get(0));
    }

    @Test
    public void bindingTwiceFails() {
        final PLabel label = Element.newPLabel();
        label.bindTerminalFunction("fn");
        assertThrows(IllegalAccessError.class, () -> label.bindTerminalFunction("other"));
    }

    @Test
    public void sendToNativeWithoutBindingFails() {
        final PLabel label = Element.newPLabel();
        final JsonObject data = Json.createObjectBuilder().build();
        assertThrows(IllegalAccessError.class, () -> label.sendToNative(data));
        label.setData("x");
        assertThrows(IllegalAccessError.class, () -> label.sendToNative(data));
    }

    @Test
    public void terminalHandlerReceivesNativeData() {
        final PLabel label = attachedLabel();
        final List<JsonObject> received = new ArrayList<>();
        label.onClientData(Json.createObjectBuilder().add("other", 1).build());
        label.setTerminalHandler(e -> received.add(e.getData()));
        label.onClientData(Json.createObjectBuilder().add("other", 1).build());
        assertTrue(received.isEmpty());

        final JsonObject data = Json.createObjectBuilder().add("k", "v").build();
        label.onClientData(Json.createObjectBuilder().add(ClientToServerModel.NATIVE.toStringValue(), data).build());
        assertEquals(List.of(data), received);

        label.setTerminalHandler(null);
        label.onClientData(Json.createObjectBuilder().add(ClientToServerModel.NATIVE.toStringValue(), data).build());
        assertEquals(1, received.size());
        Element.newPLabel().setTerminalHandler(null);
    }

    @Test
    public void destroyNotifiesListenersAndStopsWrites() {
        final PLabel label = attachedLabel();
        label.bindTerminalFunction("fn");
        final List<JsonObject> received = new ArrayList<>();
        label.setTerminalHandler(e -> received.add(e.getData()));
        final AtomicInteger destroyed = new AtomicInteger();
        final PObject.DestroyListener removed = o -> destroyed.addAndGet(100);
        label.addDestroyListener(o -> destroyed.incrementAndGet());
        label.addDestroyListener(removed);
        label.addDestroyListener(o -> {
            throw new IllegalStateException("expected");
        });
        label.removeDestroyListener(removed);

        label.onDestroy();
        assertEquals(1, destroyed.get());
        assertNull(label.getWindow());
        assertFalse(label.isUpdatable());

        writer.clear();
        label.sendToNative(Json.createObjectBuilder().build());
        label.callTerminalFunction("x");
        label.onClientData(Json.createObjectBuilder()
            .add(ClientToServerModel.NATIVE.toStringValue(), Json.createObjectBuilder().build()).build());
        assertTrue(writer.isEmpty());
        assertTrue(received.isEmpty());
        assertFalse(label.beginCoalescedUpdate(writer));
    }

    @Test
    public void destroyWithoutExtrasAndRemoveUnknownListener() {
        final PLabel label = Element.newPLabel();
        label.removeDestroyListener(o -> {
        });
        label.onDestroy();
        assertNull(label.getData());
    }

    @Test
    public void destroyedObjectWritesNothing() {
        final PFlowPanel panel = Element.newPFlowPanel();
        PWindow.getMain().add(panel);
        final PLabel child = Element.newPLabel();
        panel.add(child);
        child.onDestroy();
        writer.clear();

        child.saveAdd(child.getID(), panel.getID());
        child.saveAdd(child.getID(), panel.getID(), (com.ponysdk.core.ui.model.ServerBinaryModel) null);
        child.saveRemove(child.getID(), panel.getID());
        child.addVisibilityHandler(e -> {
        });
        child.removeVisibilityHandler(null);
        child.setVisible(false);
        child.setStyleProperty("color", "red");
        child.addStyleName("x");
        child.writeUpdate(w -> w.write(ServerToClientModel.WIDGET_TITLE, "x"));
        child.writeAddHandler(w -> w.write(ServerToClientModel.HANDLER_TYPE, 0));
        assertTrue(writer.isEmpty());
    }

    @Test
    public void stackedInstructionsAreDroppedOnceDestroyed() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel child = Element.newPLabel();
        final PVisibilityEvent.PVisibilityHandler handler = e -> {
        };
        panel.add(child);
        panel.insert(Element.newPLabel(), 0);
        child.focus();
        child.callTerminalFunction("f");
        child.addVisibilityHandler(handler);
        child.removeVisibilityHandler(handler);
        panel.remove(child);
        child.onDestroy();

        // each stacked writer checks the destroy flag when it is replayed
        child.stackedInstructions.runAll();
        assertTrue(writer.isEmpty());
    }

    @Test
    public void coalescedUpdateBeginsAnUpdateBlock() {
        final PLabel label = attachedLabel();
        assertTrue(label.isUpdatable());
        assertFalse(Element.newPLabel().isUpdatable());
        writer.clear();
        assertTrue(label.beginCoalescedUpdate(writer));
        assertEquals(List.of(label.getID()), writer.valuesOf(ServerToClientModel.TYPE_UPDATE));
    }

    @Test
    public void ajaxHandler() throws Exception {
        final PLabel label = Element.newPLabel();
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        label.setAjaxHandler(null);
        label.handleAjaxRequest(request, response);

        final AtomicInteger calls = new AtomicInteger();
        label.setAjaxHandler((req, resp) -> {
            assertSame(request, req);
            assertSame(response, resp);
            calls.incrementAndGet();
        });
        label.handleAjaxRequest(request, response);
        assertEquals(1, calls.get());

        label.setAjaxHandler(null);
        label.handleAjaxRequest(request, response);
        assertEquals(1, calls.get());
    }
}
