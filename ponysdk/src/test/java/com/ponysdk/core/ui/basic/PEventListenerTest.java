/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ProtocolVersion;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.event.PScrollEvent;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

/** Protocol 4 (docs/decisions/0003-protocol-v4-listeners.md): DOM events by name, handlers removed from their object. */
public class PEventListenerTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
        speak(ProtocolVersion.LISTENERS);
    }

    @AfterEach
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
        assertEquals(2, writer.count(ServerToClientModel.LISTEN_EVENT), "new options are sent");

        first.removeHandler();
        second.removeHandler();
        assertEquals(0, writer.count(ServerToClientModel.UNLISTEN_EVENT), "one listener left");
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
        assertEquals(null, events.get(0).getNumber("target.value"), "not a number: absent, as for every type");
        assertEquals(null, events.get(0).getBoolean("target.value"));
        assertEquals(null, events.get(0).getBoolean("missing"), "absent is not false");
    }

    @Test
    public void belowProtocol4ListeningFailsInsteadOfNeverFiring() {
        speak(ProtocolVersion.CREATE_IN_PARENT);
        final PTextBox box = attachedBox();
        final IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> box.addEventListener("input", PEventOptions.NONE, event -> {
            }));
        assertTrue(e.getMessage().contains("needs protocol 4, the context speaks 3"), e.getMessage());
        assertEquals(0, writer.count(ServerToClientModel.LISTEN_EVENT));
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
        assertEquals(0, writer.count(ServerToClientModel.WIDGET_ID), "the old block below 4");
        assertEquals(1, writer.count(ServerToClientModel.TYPE_REMOVE_HANDLER));
    }

    private static JsonObject event(final PWidget widget, final String name) {
        return Json.createObjectBuilder().add(ClientToServerModel.OBJECT_ID.toStringValue(), widget.getID())
            .add(ClientToServerModel.EVENT.toStringValue(), name).build();
    }

    @Test
    public void anEventWithoutListenerOrFieldsIsHandled() {
        final PTextBox box = attachedBox();
        box.onClientData(event(box, "input")); // nothing registered at all

        final List<PEvent> events = new ArrayList<>();
        box.addEventListener("input", PEventOptions.NONE, events::add);
        box.onClientData(event(box, "keydown")); // another name
        assertTrue(events.isEmpty());
        box.onClientData(event(box, "input"));
        assertEquals(1, events.size());
        assertNull(events.get(0).getString("target.value"), "no fields");
    }

    @Test
    public void removingAListenerTwiceOrOfAnotherNameUnlistensOnce() {
        final PTextBox box = attachedBox();
        final HandlerRegistration input = box.addEventListener("input", PEventOptions.NONE, e -> {
        });
        final HandlerRegistration other = box.addEventListener("keydown", PEventOptions.NONE, e -> {
        });
        input.removeHandler();
        input.removeHandler();
        assertEquals(List.of("input"), writer.valuesOf(ServerToClientModel.UNLISTEN_EVENT));
        other.removeHandler();
        assertEquals(List.of("input", "keydown"), writer.valuesOf(ServerToClientModel.UNLISTEN_EVENT));
    }

    @Test
    public void aRegistrationRemovedTwiceLeavesTheOtherListeners() {
        final PTextBox box = attachedBox();
        final HandlerRegistration first = box.addEventListener("x", PEventOptions.NONE, e -> {
        });
        box.addEventListener("x", PEventOptions.NONE, e -> {
        });
        first.removeHandler();
        first.removeHandler();
        assertEquals(0, writer.count(ServerToClientModel.UNLISTEN_EVENT));
    }
}
