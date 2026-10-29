/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.json.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PMenuItemTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private static PMenuItem attached(final PMenuItem item) {
        final PMenuBar menuBar = Element.newPMenuBar();
        menuBar.addItem(item);
        PWindow.getMain().add(menuBar);
        return item;
    }

    @Test
    public void anItemIsEnabledUntilDisabled() {
        final PMenuItem item = Element.newPMenuItem("Item", () -> {
        });
        assertTrue(item.isEnabled());
        item.setEnabled(false);
        assertFalse(item.isEnabled());
    }

    @Test
    public void theCommandOfADisabledItemIsNotRunWhateverTheClientSends() {
        final AtomicInteger runs = new AtomicInteger();
        final PMenuItem item = Element.newPMenuItem("Item", runs::incrementAndGet);
        final var command = Json.createObjectBuilder().add(ClientToServerModel.HANDLER_COMMAND.toStringValue(), 0).build();
        item.onClientData(command);
        assertEquals(1, runs.get());
        item.setEnabled(false);
        item.onClientData(command);
        assertEquals(1, runs.get());
    }

    @Test
    public void aNewItemHasTextAndNoCommandNorSubMenu() {
        final PMenuItem item = Element.newPMenuItem("text");
        assertEquals(WidgetType.MENU_ITEM, item.getWidgetType());
        assertEquals("text", item.getText());
        assertNull(item.getHTML());
        assertNull(item.getSubMenu());
        assertNull(item.getCmd());
    }

    @Test
    public void factoriesKeepTextHtmlCommandAndSubMenu() {
        final Runnable cmd = () -> {
        };
        final PMenuBar sub = Element.newPMenuBar();
        final PMenuBar sub2 = Element.newPMenuBar();
        assertEquals("<b>h</b>", Element.newPMenuItem("<b>h</b>", true).getHTML());
        assertEquals("t", Element.newPMenuItem("t", false).getText());
        assertSame(sub, Element.newPMenuItem("t", sub).getSubMenu());
        assertSame(sub2, Element.newPMenuItem("t", true, sub2).getSubMenu());
        assertSame(cmd, Element.newPMenuItem("t", true, cmd).getCmd());
        assertSame(cmd, Element.newPMenuItem("t", cmd).getCmd());
    }

    @Test
    public void textOrHtmlIsSentInsideTheCreate() {
        writer.clear();
        attached(Element.newPMenuItem("plain"));
        attached(Element.newPMenuItem("<b>rich</b>", true));
        assertTrue(writer.valuesOf(ServerToClientModel.TEXT).contains("plain"));
        assertTrue(writer.valuesOf(ServerToClientModel.HTML).contains("<b>rich</b>"));
    }

    @Test
    public void textAndHtmlReplaceEachOtherAndAreSentOnceWhenInitialized() {
        final PMenuItem item = Element.newPMenuItem("a");
        item.setText("b");
        assertEquals("b", item.getText());
        item.setHTML("<i>c</i>");
        assertEquals("<i>c</i>", item.getHTML());
        assertNull(item.getText());

        attached(item);
        writer.clear();
        item.setText("d");
        item.setText("d");
        assertNull(item.getHTML());
        item.setHTML("<u>e</u>");
        item.setHTML("<u>e</u>");
        assertEquals(List.of("d"), writer.valuesOf(ServerToClientModel.TEXT));
        assertEquals(List.of("<u>e</u>"), writer.valuesOf(ServerToClientModel.HTML));
    }

    @Test
    public void theCommandHandlerIsRegisteredAndIgnoredWhenHidden() {
        final AtomicInteger runs = new AtomicInteger();
        writer.clear();
        final PMenuItem item = attached(Element.newPMenuItem("cmd", runs::incrementAndGet));
        assertTrue(writer.valuesOf(ServerToClientModel.HANDLER_TYPE).contains(HandlerModel.HANDLER_COMMAND.getValue()));

        final var command = Json.createObjectBuilder().addNull(ClientToServerModel.HANDLER_COMMAND.toStringValue()).build();
        item.setVisible(false);
        item.onClientData(command);
        assertEquals(0, runs.get());
    }

    @Test
    public void aCommandWithoutRunnableIsIgnored() {
        final PMenuItem item = Element.newPMenuItem("x", (Runnable) null);
        assertDoesNotThrow(() -> item.onClientData(
            Json.createObjectBuilder().addNull(ClientToServerModel.HANDLER_COMMAND.toStringValue()).build()));
    }

    @Test
    public void otherClientDataDoesNotRunTheCommand() {
        final PMenuItem item = Element.newPMenuItem("x", () -> fail("not a command"));
        item.onClientData(Json.createObjectBuilder().add("unknown", 1).build());
    }

    @Test
    public void theSubMenuIsAttachedWithTheItem() {
        final PMenuBar sub = Element.newPMenuBar(true);
        final PMenuBar menuBar = Element.newPMenuBar();
        menuBar.addItem("sub", sub);
        writer.clear();
        PWindow.getMain().add(menuBar);
        assertTrue(sub.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(sub.getID()));
    }

    @Test
    public void enabledIsSentOnlyWhenItChanges() {
        final PMenuItem item = attached(Element.newPMenuItem("x"));
        writer.clear();
        item.setEnabled(true);
        item.setEnabled(false);
        item.setEnabled(false);
        assertEquals(List.of(false), writer.valuesOf(ServerToClientModel.ENABLED));
    }

    @Test
    public void dumpDOMIncludesTheSubMenu() {
        final PMenuBar sub = Element.newPMenuBar();
        final PMenuItem item = Element.newPMenuItem("<b>x</b>", true, sub);
        assertEquals("<li pid=\"" + item.getID() + "\"><b>x</b><ul pid=\"" + sub.getID() + "\"></ul></li>", item.dumpDOM());
    }
}
