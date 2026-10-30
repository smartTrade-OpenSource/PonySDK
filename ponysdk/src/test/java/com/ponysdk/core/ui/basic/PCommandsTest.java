/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

/** Client commands (docs/decisions/0027-client-commands.md): what travels in LISTEN_EVENT, and how a widget registers it. */
public class PCommandsTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
        Mockito.doReturn(ProtocolVersion.LISTENERS).when(UIContext.get()).getProtocolVersion();
    }

    @AfterEach
    public void restore() {
        writer.uninstall();
        Mockito.doCallRealMethod().when(UIContext.get()).getProtocolVersion();
    }

    private static PTextBox attached() {
        final PTextBox box = Element.newPTextBox();
        PWindow.getMain().add(box);
        return box;
    }

    private static JsonObject event(final PWidget widget, final String name) {
        return Json.createObjectBuilder().add(ClientToServerModel.OBJECT_ID.toStringValue(), widget.getID())
            .add(ClientToServerModel.EVENT.toStringValue(), name).build();
    }

    @Test
    public void everyCommandIsWrittenInOrderWithItsTargetId() {
        final PTextBox a = attached();
        final PTextBox b = attached();
        final PCommands commands = PCommands.NONE.show(a).hide(a).toggle(b).addClass(a, "open").removeClass(a, "open")
            .toggleClass(b, "on").setAttribute(a, "aria-expanded", "true").removeAttribute(a, "data-x").focus(b)
            .dispatch(a, "my-event");
        final String json = PEventOptions.NONE.commands(commands).toJson("click");
        assertEquals("{\"name\":\"click\",\"commands\":[[\"show\"," + a.getID() + "],[\"hide\"," + a.getID() + "],[\"toggle\","
                + b.getID() + "],[\"addClass\"," + a.getID() + ",\"open\"],[\"removeClass\"," + a.getID() + ",\"open\"],"
                + "[\"toggleClass\"," + b.getID() + ",\"on\"],[\"setAttribute\"," + a.getID() + ",\"aria-expanded\",\"true\"],"
                + "[\"removeAttribute\"," + a.getID() + ",\"data-x\"],[\"focus\"," + b.getID() + "],[\"dispatch\"," + a.getID()
                + ",\"my-event\"]]}", json);
    }

    @Test
    public void commandsAreValuesAndOptionsKeepThem() {
        final PTextBox a = attached();
        final PCommands one = PCommands.NONE.toggle(a);
        assertEquals(one, PCommands.NONE.toggle(a));
        assertEquals(one.hashCode(), PCommands.NONE.toggle(a).hashCode());
        assertNotEquals(one, PCommands.NONE.show(a));
        assertNotEquals(one, "toggle");
        assertTrue(PCommands.NONE.isEmpty());
        assertTrue(!one.isEmpty());
        assertTrue(PCommands.NONE.toggle(a) != PCommands.NONE, "a copy: NONE stays empty");
        assertTrue(PCommands.NONE.isEmpty());

        final PEventOptions options = PEventOptions.fields("x").commands(one);
        assertEquals(options, PEventOptions.NONE.commands(one).withFields("x"));
        assertNotEquals(options, PEventOptions.fields("x"));
        assertNotEquals(options, PEventOptions.fields("x").commands(PCommands.NONE.show(a)));
        assertEquals(options.hashCode(), PEventOptions.NONE.commands(one).withFields("x").hashCode());
        assertTrue(options.throttle(Duration.ofMillis(5)).hasCommands(), "kept by the other options");
        assertEquals("{\"name\":\"click\"}", PEventOptions.NONE.commands(one).commands(PCommands.NONE).toJson("click"), "none: nothing written");
        assertEquals("{\"name\":\"click\",\"send\":false}", PEventOptions.NONE.toJson("click", false));
    }

    @Test
    public void whatTheTerminalWouldRefuseIsRefusedHere() {
        final PTextBox a = attached();
        assertThrows(NullPointerException.class, () -> PCommands.NONE.show(null));
        assertThrows(NullPointerException.class, () -> PCommands.NONE.addClass(a, null));
        assertThrows(NullPointerException.class, () -> PCommands.NONE.setAttribute(a, "title", null));
        assertThrows(NullPointerException.class, () -> PCommands.NONE.setAttribute(a, null, "x"));
        assertThrows(NullPointerException.class, () -> PEventOptions.NONE.commands(null));
        for (final String name : List.of("", " ", "two words", "tab\there"))
            assertThrows(IllegalArgumentException.class, () -> PCommands.NONE.toggleClass(a, name), "'" + name + "'");
        assertThrows(IllegalArgumentException.class, () -> PCommands.NONE.dispatch(a, "two words"));
        for (final String name : List.of("", "a b", "1a", "-a", "a=b", "a\"b"))
            assertThrows(IllegalArgumentException.class, () -> PCommands.NONE.removeAttribute(a, name), "'" + name + "'");
        for (final String name : List.of("onclick", "onLoad", "ONERROR")) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> PCommands.NONE.setAttribute(a, name, "alert(1)"));
            assertTrue(e.getMessage().contains("event handler"), e.getMessage());
        }
        PCommands.NONE.setAttribute(a, "aria-expanded", "").setAttribute(a, "data-state.x_y:z", "v").setAttribute(a, "_x", "v");
    }

    @Test
    public void aCompositeTargetIsItsWidget() {
        final PTextBox box = attached();
        final IsPWidget composite = () -> box;
        assertEquals(PCommands.NONE.toggle(box), PCommands.NONE.toggle(composite));
    }

    @Test
    public void commandsAloneListenWithoutSendingAndStopWhenRemoved() {
        final PTextBox box = attached();
        final PTextBox panel = attached();
        writer.clear();
        final PEventOptions options = PEventOptions.NONE.commands(PCommands.NONE.toggle(panel));
        final HandlerRegistration registration = box.addEventListener("click", options);
        assertEquals(List.of("{\"name\":\"click\",\"commands\":[[\"toggle\"," + panel.getID() + "]],\"send\":false}"),
            writer.valuesOf(ServerToClientModel.LISTEN_EVENT));

        box.addEventListener("click", options); // the same: the terminal is not told again
        assertEquals(1, writer.count(ServerToClientModel.LISTEN_EVENT));
        final HandlerRegistration replacing = box.addEventListener("click", options.preventDefault());
        assertEquals(2, writer.count(ServerToClientModel.LISTEN_EVENT), "other options replace the commands");

        registration.removeHandler(); // replaced by a later registration: its handle stops nothing
        assertEquals(0, writer.count(ServerToClientModel.UNLISTEN_EVENT));
        replacing.removeHandler();
        assertEquals(List.of("click"), writer.valuesOf(ServerToClientModel.UNLISTEN_EVENT));
        replacing.removeHandler();
        assertEquals(1, writer.count(ServerToClientModel.UNLISTEN_EVENT), "twice, once");
    }

    @Test
    public void aServerListenerWithCommandsTellsTheTerminalToSend() {
        final PTextBox box = attached();
        final PTextBox panel = attached();
        writer.clear();
        final List<PEvent> events = new ArrayList<>();
        final HandlerRegistration registration = box.addEventListener("click",
            PEventOptions.NONE.commands(PCommands.NONE.hide(panel)), events::add);
        assertEquals(List.of("{\"name\":\"click\",\"commands\":[[\"hide\"," + panel.getID() + "]]}"),
            writer.valuesOf(ServerToClientModel.LISTEN_EVENT));
        UIContext.get().fireClientData(event(box, "click"));
        assertEquals(1, events.size());
        assertTrue(panel.isVisible(), "the server's view does not follow the browser's");
        registration.removeHandler();
        assertEquals(List.of("click"), writer.valuesOf(ServerToClientModel.UNLISTEN_EVENT));
    }

    @Test
    public void commandsAloneAndAListenerOfTheSameNameDoNotMix() {
        final PTextBox box = attached();
        final PTextBox panel = attached();
        final PEventOptions options = PEventOptions.NONE.commands(PCommands.NONE.toggle(panel));

        final HandlerRegistration commandsOnly = box.addEventListener("click", options);
        final IllegalStateException first = assertThrows(IllegalStateException.class,
            () -> box.addEventListener("click", options, e -> {
            }));
        assertTrue(first.getMessage().contains("has commands without listener"), first.getMessage());
        commandsOnly.removeHandler();

        box.addEventListener("click", options, e -> {
        });
        final IllegalStateException second = assertThrows(IllegalStateException.class, () -> box.addEventListener("click", options));
        assertTrue(second.getMessage().contains("has a listener"), second.getMessage());
    }

    @Test
    public void commandsAloneNeedCommandsAndProtocol4() {
        final PTextBox box = attached();
        final IllegalArgumentException none = assertThrows(IllegalArgumentException.class,
            () -> box.addEventListener("click", PEventOptions.fields("x")));
        assertTrue(none.getMessage().contains("No command"), none.getMessage());
        assertThrows(NullPointerException.class, () -> box.addEventListener("click", null));
        assertThrows(NullPointerException.class, () -> box.addEventListener(null, PEventOptions.NONE.commands(PCommands.NONE.show(box))));
        assertThrows(NullPointerException.class, () -> box.addEventListener("click", PEventOptions.NONE, null));

        Mockito.doReturn(ProtocolVersion.CREATE_IN_PARENT).when(UIContext.get()).getProtocolVersion();
        final IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> box.addEventListener("click", PEventOptions.NONE.commands(PCommands.NONE.show(box))));
        assertTrue(e.getMessage().contains("needs protocol 4, the context speaks 3"), e.getMessage());
        assertEquals(0, writer.count(ServerToClientModel.LISTEN_EVENT));
    }
}
