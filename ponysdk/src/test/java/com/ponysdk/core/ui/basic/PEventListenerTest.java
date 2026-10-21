/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ProtocolVersion;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.event.PEvent;
import com.ponysdk.core.ui.basic.event.PEventOptions;
import com.ponysdk.core.ui.basic.event.PScrollEvent;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

/** Protocol 4 (docs/decisions/0003-protocol-v4-listeners.md): DOM events by name, handlers removed from their object. */
public class PEventListenerTest extends PSuite {

    private CapturingModelWriter writer;

    @Before
    public void captureWrites() {
        writer = CapturingModelWriter.install();
        speak(ProtocolVersion.LISTENERS);
    }

    @After
    public void restore() {
        writer.uninstall();
        Mockito.doCallRealMethod().when(UIContext.get()).getProtocolVersion();
    }

    private static void speak(final int version) {
        Mockito.doReturn(version).when(UIContext.get()).getProtocolVersion();
    }

    private PTextBox attachedBox() {
        final PTextBox box = Element.newPTextBox();
        PWindow.getMain().add(box);
        writer.clear();
        return box;
    }

    @Test
    public void theTerminalListensOncePerNameAndOptions() {
        final PTextBox box = attachedBox();
        final PEventOptions options = PEventOptions.fields("target.value").debounce(Duration.ofMillis(300));
        final HandlerRegistration first = box.addEventListener("input", options, e -> {
        });
        final HandlerRegistration second = box.addEventListener("input", options, e -> {
        });
        assertEquals(List.of("{\"name\":\"input\",\"fields\":[\"target.value\"],\"debounce\":300}"),
            writer.valuesOf(ServerToClientModel.LISTEN_EVENT));

        box.addEventListener("input", options.throttle(Duration.ofMillis(100)), e -> {
        });
        assertEquals("new options are sent", 2, writer.count(ServerToClientModel.LISTEN_EVENT));

        first.removeHandler();
        second.removeHandler();
        assertEquals("one listener left", 0, writer.count(ServerToClientModel.UNLISTEN_EVENT));
    }

    @Test
    public void theLastListenerRemovedStopsTheTerminal() {
        final PTextBox box = attachedBox();
        final HandlerRegistration registration = box.addEventListener("pointerenter", PEventOptions.NONE, e -> {
        });
        registration.removeHandler();
        assertEquals(List.of("pointerenter"), writer.valuesOf(ServerToClientModel.UNLISTEN_EVENT));
    }

    @Test
    public void anEventReachesTheListenersOfItsName() {
        final PTextBox box = attachedBox();
        final List<PEvent> events = new ArrayList<>();
        box.addEventListener("input", PEventOptions.fields("target.value", "inputType"), events::add);
        box.addEventListener("keydown", PEventOptions.fields("key"), e -> {
            throw new AssertionError("not this one");
        });
        box.onClientData(Json.createObjectBuilder()
            .add(ClientToServerModel.OBJECT_ID.toStringValue(), box.getID())
            .add(ClientToServerModel.EVENT.toStringValue(), "input")
            .add(ClientToServerModel.EVENT_FIELDS.toStringValue(), Json.createObjectBuilder()
                .add("target.value", "abc").addNull("inputType"))
            .build());
        assertEquals(1, events.size());
        assertEquals("input", events.get(0).getName());
        assertEquals("abc", events.get(0).getString("target.value"));
        assertEquals(null, events.get(0).getString("inputType"));
        assertTrue(Double.isNaN(events.get(0).getNumber("target.value")));
    }

    @Test
    public void belowProtocol4TheTerminalIsNotTold() {
        speak(ProtocolVersion.CREATE_IN_PARENT);
        final PTextBox box = attachedBox();
        box.addEventListener("input", PEventOptions.NONE, e -> {
        }).removeHandler();
        assertEquals(0, writer.count(ServerToClientModel.LISTEN_EVENT) + writer.count(ServerToClientModel.UNLISTEN_EVENT));
    }

    @Test
    public void aRemovedHandlerNamesItsObjectFromProtocol4() {
        final PScrollPanel panel = Element.newPScrollPanel();
        PWindow.getMain().add(panel);
        final PScrollEvent.PScrollHandler handler = e -> {
        };
        panel.addScrollHandler(handler);
        writer.clear();
        panel.removeScrollHandler(handler);
        assertEquals(List.of(panel.getID()), writer.valuesOf(ServerToClientModel.WIDGET_ID));

        speak(ProtocolVersion.CREATE_IN_PARENT);
        panel.addScrollHandler(handler);
        writer.clear();
        panel.removeScrollHandler(handler);
        assertEquals("the old block below 4", 0, writer.count(ServerToClientModel.WIDGET_ID));
        assertEquals(1, writer.count(ServerToClientModel.TYPE_REMOVE_HANDLER));
    }
}
