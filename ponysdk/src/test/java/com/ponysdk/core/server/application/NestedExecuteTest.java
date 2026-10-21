/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.UpgradeRequestData;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.server.websocket.WebsocketEncoder;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWindow;

/** A context's execute() run from inside another context's transaction (e.g. a push to other sessions from a handler). */
public class NestedExecuteTest {

    /** The values written to one context, in order. */
    private static final class Wire implements WebsocketEncoder {
        final List<String> values = new ArrayList<>();
        @Override public int protocolVersion() { return com.ponysdk.core.model.ProtocolVersion.V1; }
        @Override public void beginObject() {}
        @Override public void encode(final ServerToClientModel model, final Object value) { values.add(model + "=" + value); }
        @Override public void endObject() {}
    }

    private static UIContext context(final Wire wire) {
        final WebSocket socket = Mockito.mock(WebSocket.class);
        Mockito.when(socket.getUpgradeRequestData())
            .thenReturn(new UpgradeRequestData(Map.of(), "test", Mockito.mock(jakarta.servlet.http.HttpSession.class)));
        final TxnContext txn = new TxnContext(socket);
        txn.getWriter().setEncoder(wire);
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setUpdateCoalescingEnabled(false);
        return new UIContext(socket, txn, config);
    }

    @After
    public void tearDown() {
        UIContext.remove();
    }

    @Test
    public void theOuterTransactionContinuesAfterANestedOneOnAnotherContext() {
        final Wire wireA = new Wire(), wireB = new Wire();
        final UIContext a = context(wireA), b = context(wireB);
        final PLabel[] labels = new PLabel[2];
        a.execute(() -> PWindow.getMain().add(labels[0] = Element.newPLabel("a")));
        b.execute(() -> PWindow.getMain().add(labels[1] = Element.newPLabel("b")));
        wireA.values.clear();
        wireB.values.clear();
        final UIContext[] seen = new UIContext[1];

        final boolean ok = a.execute(() -> {
            b.execute(() -> labels[1].setText("pushed"));
            seen[0] = UIContext.get();
            labels[0].setText("after the push");
        });

        assertTrue("the outer transaction commits", ok);
        assertSame("the outer code still runs in its own context", a, seen[0]);
        assertTrue(wireB.values.contains("TEXT=pushed"));
        assertTrue(wireA.values.contains("TEXT=after the push"));
        assertEquals("nothing of A written to B", -1, wireB.values.indexOf("TEXT=after the push"));
    }
}
