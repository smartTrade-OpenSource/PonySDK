/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.ByteBufferPool;
import com.ponysdk.core.server.websocket.PusherAccess;
import com.ponysdk.core.server.websocket.UpgradeRequestData;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.server.websocket.WebsocketEncoder;

/**
 * animate() goes through the real encoder: ANIMATE is an INTEGER, and a long failed its cast in the middle of the
 * block, so the transaction rolled back with the update half written.
 */
public class AnimateEncodingTest {

    @After
    public void tearDown() {
        UIContext.remove();
    }

    @Test
    public void everyLayoutPanelAnimates() {
        final PusherAccess pusher = new PusherAccess(Mockito.mock(Session.class), new ByteBufferPool(1 << 15, 4));
        final WebSocket socket = Mockito.mock(WebSocket.class);
        Mockito.when(socket.getUpgradeRequestData())
            .thenReturn(new UpgradeRequestData(Map.of(), "test", Mockito.mock(jakarta.servlet.http.HttpSession.class)));
        final TxnContext txn = new TxnContext(socket);
        txn.getWriter().setEncoder(new WebsocketEncoder() {
            @Override public void beginObject() {}
            @Override public void endObject() { encode(ServerToClientModel.END, null); }
            @Override public void encode(final ServerToClientModel model, final Object value) {
                try {
                    pusher.encode(model, value);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        final UIContext ctx = new UIContext(socket, txn, config);

        final PAnimatedLayout[] panels = new PAnimatedLayout[4];
        assertTrue(ctx.execute(() -> {
            panels[0] = Element.newPLayoutPanel();
            panels[1] = Element.newPDockLayoutPanel(PUnit.PX);
            panels[2] = Element.newPStackLayoutPanel(PUnit.PX);
            panels[3] = Element.newPTabLayoutPanel();
            for (final PAnimatedLayout panel : panels) PWindow.getMain().add((PWidget) panel);
        }));
        for (final PAnimatedLayout panel : panels) {
            assertTrue(panel.getClass().getSimpleName(), ctx.execute(() -> panel.animate(Duration.ofMillis(250))));
        }
    }
}
