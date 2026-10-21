/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ProtocolVersion;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.server.websocket.WebsocketEncoder;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWindow;

/**
 * Protocol 3 (create in parent): the add of an object written right after its create block joins that block, as a
 * trailing PARENT_OBJECT_ID. Anything written in between, or an add carrying more than the parent, keeps the add block.
 */
public class CreateInParentTest {

    /** Records the wire as blocks of "MODEL=value"; {@code open} is the block not yet ended. */
    private static final class Wire implements WebsocketEncoder {
        final List<List<String>> blocks = new ArrayList<>();
        List<String> open;
        int version;
        @Override public int protocolVersion() { return version; }
        @Override public void beginObject() {}
        @Override public void encode(final ServerToClientModel model, final Object value) {
            if (model == ServerToClientModel.WINDOW_ID) return; // block routing, not under test
            if (open == null) { open = new ArrayList<>(); blocks.add(open); }
            open.add(model + "=" + value);
        }
        @Override public void endObject() { open = null; }
    }

    private Wire wire;
    private UIContext ctx;
    private PFlowPanel root;

    private void start(final int protocolVersion) {
        wire = new Wire();
        wire.version = protocolVersion;
        final WebSocket socket = Mockito.mock(WebSocket.class);
        Mockito.when(socket.getUpgradeRequestData()).thenReturn(new com.ponysdk.core.server.websocket.UpgradeRequestData(Map.of(),
            "test", Mockito.mock(jakarta.servlet.http.HttpSession.class)));
        final TxnContext txn = new TxnContext(socket);
        txn.getWriter().setEncoder(wire);
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        ctx = new UIContext(socket, txn, config);
        ctx.execute(() -> {
            root = Element.newPFlowPanel();
            PWindow.getMain().add(root);
        });
        wire.blocks.clear();
    }

    @After
    public void tearDown() {
        UIContext.remove();
    }

    @Test
    public void aLeafIsCreatedInItsParent() {
        start(ProtocolVersion.CREATE_IN_PARENT);
        final PLabel[] l = new PLabel[1];
        ctx.execute(() -> root.add(l[0] = Element.newPLabel("a")));
        assertEquals(List.of(List.of("TYPE_CREATE=" + l[0].getID(), "WIDGET_TYPE=" + com.ponysdk.core.model.WidgetType.LABEL.getValue(), "TEXT=a",
            "PARENT_OBJECT_ID=" + root.getID())), wire.blocks);
        assertNull("every block is ended by the transaction", wire.open);
    }

    @Test
    public void protocol2KeepsTheAddBlock() {
        start(ProtocolVersion.UPDATE_RUNS);
        final PLabel[] l = new PLabel[1];
        ctx.execute(() -> root.add(l[0] = Element.newPLabel("a")));
        assertEquals(2, wire.blocks.size());
        assertEquals(List.of("TYPE_ADD=" + l[0].getID(), "PARENT_OBJECT_ID=" + root.getID()), wire.blocks.get(1));
    }

    @Test
    public void aPanelBuiltBeforeAttachCreatesItsChildrenInItThenIsAdded() {
        start(ProtocolVersion.CREATE_IN_PARENT);
        final PFlowPanel[] row = new PFlowPanel[1];
        final PLabel[] l = new PLabel[2];
        ctx.execute(() -> {
            row[0] = Element.newPFlowPanel();
            row[0].add(l[0] = Element.newPLabel("a"));
            row[0].add(l[1] = Element.newPLabel("b"));
            root.add(row[0]);
        });
        // the row's children are written between its create and its add: the row keeps its add block, its
        // children (leaves) are created in it
        assertEquals(4, wire.blocks.size());
        assertEquals("TYPE_CREATE=" + row[0].getID(), wire.blocks.get(0).get(0));
        assertEquals("PARENT_OBJECT_ID=" + row[0].getID(), last(wire.blocks.get(1)));
        assertEquals("PARENT_OBJECT_ID=" + row[0].getID(), last(wire.blocks.get(2)));
        assertEquals(List.of("TYPE_ADD=" + row[0].getID(), "PARENT_OBJECT_ID=" + root.getID()), wire.blocks.get(3));
    }

    @Test
    public void aStackedInstructionBeforeTheAddKeepsTheOrder() {
        start(ProtocolVersion.CREATE_IN_PARENT);
        final PButton[] b = new PButton[1];
        ctx.execute(() -> {
            b[0] = Element.newPButton("go");
            b[0].addClickHandler(e -> {});
            root.add(b[0]);
        });
        assertEquals(3, wire.blocks.size());
        assertEquals("TYPE_ADD_HANDLER=" + b[0].getID(), wire.blocks.get(1).get(0));
        assertEquals(List.of("TYPE_ADD=" + b[0].getID(), "PARENT_OBJECT_ID=" + root.getID()), wire.blocks.get(2));
    }

    @Test
    public void anInsertKeepsItsAddBlock() {
        start(ProtocolVersion.CREATE_IN_PARENT);
        final PLabel[] l = new PLabel[3];
        ctx.execute(() -> {
            root.add(l[0] = Element.newPLabel("a"));
            root.add(l[1] = Element.newPLabel("b"));
        });
        wire.blocks.clear();
        ctx.execute(() -> root.insert(l[2] = Element.newPLabel("c"), 0));
        assertEquals(2, wire.blocks.size());
        assertEquals(List.of("TYPE_ADD=" + l[2].getID(), "PARENT_OBJECT_ID=" + root.getID(), "INDEX=0"), wire.blocks.get(1));
    }

    @Test
    public void aCreateWithoutAddIsEndedBeforeTheNextBlock() {
        start(ProtocolVersion.CREATE_IN_PARENT);
        final PLabel[] l = new PLabel[2];
        ctx.execute(() -> {
            root.add(l[0] = Element.newPLabel("a"));
            l[0].setText("b"); // written after the fused create, in its own update block
        });
        assertEquals(2, wire.blocks.size());
        assertEquals("PARENT_OBJECT_ID=" + root.getID(), last(wire.blocks.get(0)));
        assertEquals(List.of("TYPE_UPDATE=" + l[0].getID(), "TEXT=b"), wire.blocks.get(1));
        assertNull(wire.open);
    }

    private static String last(final List<String> block) {
        return block.get(block.size() - 1);
    }
}
