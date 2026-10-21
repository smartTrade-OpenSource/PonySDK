/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import com.ponysdk.core.server.websocket.UpgradeRequestData;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.server.websocket.WebsocketEncoder;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWindow;

/**
 * A block an exception interrupts is ended where it stopped, and what the transaction wrote is sent: the next block
 * never runs into it, and a failed transaction does not wait for the next flush.
 */
public class InterruptedBlockTest {

    /** Records the wire as blocks of "MODEL=value"; a field whose value is "boom" fails before it is written. */
    private static final class Wire implements WebsocketEncoder {
        final List<List<String>> blocks = new ArrayList<>();
        List<String> open;
        @Override public int protocolVersion() { return ProtocolVersion.CURRENT; }
        @Override public void beginObject() {}
        @Override public void encode(final ServerToClientModel model, final Object value) {
            if ("boom".equals(value)) throw new IllegalStateException("cannot encode " + model);
            if (model == ServerToClientModel.WINDOW_ID) return; // block routing, not under test
            if (open == null) { open = new ArrayList<>(); blocks.add(open); }
            open.add(model + "=" + value);
        }
        @Override public void endObject() { open = null; }
    }

    private Wire wire;
    private WebSocket socket;
    private UIContext ctx;
    private PLabel a;
    private PLabel b;

    private void start(final boolean coalescing) {
        wire = new Wire();
        socket = Mockito.mock(WebSocket.class);
        Mockito.when(socket.getUpgradeRequestData())
            .thenReturn(new UpgradeRequestData(Map.of(), "test", Mockito.mock(jakarta.servlet.http.HttpSession.class)));
        final TxnContext txn = new TxnContext(socket);
        txn.getWriter().setEncoder(wire);
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setUpdateCoalescingEnabled(coalescing);
        ctx = new UIContext(socket, txn, config);
        ctx.execute(() -> {
            a = Element.newPLabel();
            b = Element.newPLabel();
            PWindow.getMain().add(a);
            PWindow.getMain().add(b);
        });
        wire.blocks.clear();
        Mockito.clearInvocations(socket);
    }

    @After
    public void tearDown() {
        UIContext.remove();
    }

    @Test
    public void aFailedTransactionSendsWhatItWroteWithTheInterruptedBlockEnded() {
        start(false);
        assertFalse(ctx.execute(() -> {
            a.setText("ok");
            b.setText("boom");
        }));
        assertEquals(List.of(List.of("TYPE_UPDATE=" + a.getID(), "TEXT=ok"), List.of("TYPE_UPDATE=" + b.getID())), wire.blocks);
        assertNull("the interrupted block is ended", wire.open);
        Mockito.verify(socket).flush();
    }

    @Test
    public void theNextBlockStartsItsOwnAfterAnExceptionCaughtInTheTransaction() {
        start(false);
        assertTrue(ctx.execute(() -> {
            try {
                b.setText("boom");
            } catch (final IllegalStateException e) {
                // the application goes on
            }
            a.setText("after");
        }));
        assertEquals(List.of(List.of("TYPE_UPDATE=" + b.getID()), List.of("TYPE_UPDATE=" + a.getID(), "TEXT=after")), wire.blocks);
        assertNull(wire.open);
    }

    @Test
    public void aFailureWhileDrainingCoalescedUpdatesEndsItsBlock() {
        start(true);
        assertFalse(ctx.execute(() -> b.setText("boom"))); // deferred, fails in the commit's drain
        assertEquals(List.of(List.of("TYPE_UPDATE=" + b.getID())), wire.blocks);
        assertNull(wire.open);
    }
}
