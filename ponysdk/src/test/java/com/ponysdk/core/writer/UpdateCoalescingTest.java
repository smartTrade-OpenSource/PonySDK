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

package com.ponysdk.core.writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.ee11.websocket.server.JettyServerUpgradeRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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
 * Update coalescing on a real UIContext / ModelWriter (wire = what the encoder receives): only the
 * last value of a property within a transaction is sent, consecutive updates of one object share a
 * block, and nothing ever changes the relative order with non-coalesced writes.
 */
public class UpdateCoalescingTest {

    /** Records the wire as a list of blocks, each a list of "MODEL=value" strings. */
    private static final class Wire implements WebsocketEncoder {
        final List<List<String>> blocks = new ArrayList<>();
        List<String> open;
        @Override public void beginObject() {}
        @Override public void encode(final ServerToClientModel model, final Object value) {
            if (open == null) { open = new ArrayList<>(); blocks.add(open); }
            open.add(model + "=" + value);
        }
        @Override public void endObject() { open = null; }
        void clear() { blocks.clear(); open = null; }
        long count(final String prefix) { return blocks.stream().flatMap(List::stream).filter(s -> s.startsWith(prefix)).count(); }
    }

    private Wire wire;
    private UIContext ctx;
    private PFlowPanel root;

    private void start(final boolean coalescing) {
        wire = new Wire();
        final WebSocket socket = Mockito.mock(WebSocket.class);
        Mockito.when(socket.getCachedParameterMap()).thenReturn(Map.of());
        Mockito.when(socket.getCachedUserAgent()).thenReturn("test");
        Mockito.when(socket.getCachedHttpSession()).thenReturn(Mockito.mock(jakarta.servlet.http.HttpSession.class));
        final TxnContext txn = new TxnContext(socket);
        txn.getWriter().setEncoder(wire);
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setUpdateCoalescingEnabled(coalescing);
        ctx = new UIContext(socket, txn, config, Mockito.mock(JettyServerUpgradeRequest.class));
        ctx.execute(() -> {
            root = Element.newPFlowPanel();
            PWindow.getMain().add(root);
        });
        wire.clear();
    }

    @Before
    public void setUp() {
        start(true);
    }

    @After
    public void tearDown() {
        UIContext.remove();
    }

    private PLabel label(final String text) {
        final PLabel[] l = new PLabel[1];
        ctx.execute(() -> { l[0] = Element.newPLabel(text); root.add(l[0]); });
        wire.clear();
        return l[0];
    }

    @Test
    public void onlyTheLastValueOfAPropertyIsSent() {
        final PLabel l = label("0");
        ctx.execute(() -> {
            l.setText("1");
            l.setText("2");
            l.setText("3");
        });
        assertEquals(List.of(List.of("TYPE_UPDATE=" + l.getID(), "TEXT=3")), wire.blocks);
    }

    @Test
    public void consecutiveUpdatesOfOneWidgetShareABlock() {
        final PLabel l = label("0");
        ctx.execute(() -> {
            l.setText("a");
            l.setStyleProperty("color", "red");
            l.setAttribute("title", "t");
            l.setStyleProperty("color", "green"); // replaces red, moves to the end
        });
        assertEquals(List.of(List.of("TYPE_UPDATE=" + l.getID(), "TEXT=a", "PUT_ATTRIBUTE_KEY=title", "ATTRIBUTE_VALUE=t",
                "PUT_STYLE_KEY=color", "STYLE_VALUE=green")), wire.blocks);
    }

    @Test
    public void differentKeysAreKeptSeparately() {
        final PLabel l = label("0");
        ctx.execute(() -> {
            l.setStyleProperty("color", "red");
            l.setStyleProperty("width", "10px");
        });
        assertEquals(1, wire.count("STYLE_VALUE=red"));
        assertEquals(1, wire.count("STYLE_VALUE=10px"));
    }

    @Test
    public void nonCoalescedWritesKeepTheirPlaceInTheOrder() {
        final PLabel a = label("a");
        final PLabel b = label("b");
        ctx.execute(() -> {
            a.setText("a1");
            b.addStyleName("x");       // not coalescible: a1 must be written before it
            a.setText("a2");           // a new pending value, after the style
            a.removeStyleProperty("nothing"); // no-op
            a.setStyleProperty("color", "red");
            a.removeStyleProperty("color");   // must follow the put, never precede it
        });
        final List<String> flat = new ArrayList<>();
        wire.blocks.forEach(flat::addAll);
        assertTrue(flat.indexOf("TEXT=a1") < flat.indexOf("ADD_STYLE_NAME=x"));
        assertTrue(flat.indexOf("ADD_STYLE_NAME=x") < flat.indexOf("TEXT=a2"));
        assertTrue(flat.indexOf("STYLE_VALUE=red") < flat.indexOf("REMOVE_STYLE_KEY=color"));
    }

    @Test
    public void interferingPropertiesEndWithTheLastOneWritten() {
        final PLabel l = label("0");
        final com.ponysdk.core.ui.basic.PHTML h[] = new com.ponysdk.core.ui.basic.PHTML[1];
        ctx.execute(() -> { h[0] = Element.newPHTML("x"); root.add(h[0]); });
        wire.clear();
        ctx.execute(() -> {
            h[0].setHTML("<b>1</b>");
            h[0].setText("plain");
            h[0].setHTML("<i>2</i>");
        });
        final List<String> block = wire.blocks.get(0);
        assertEquals("HTML=<i>2</i>", block.get(block.size() - 1));
        assertEquals(1, wire.count("TEXT=plain"));
        assertTrue(l.getID() > 0);
    }

    @Test
    public void structuralChangesFlushPendingUpdatesFirst() {
        final PLabel l = label("0");
        ctx.execute(() -> {
            l.setText("before-remove");
            root.remove(l);
        });
        final List<String> flat = new ArrayList<>();
        wire.blocks.forEach(flat::addAll);
        assertTrue(flat.indexOf("TEXT=before-remove") >= 0);
        assertTrue(flat.indexOf("TEXT=before-remove") < flat.indexOf("TYPE_REMOVE=" + l.getID()));
    }

    @Test
    public void destroyedWidgetsAreSkipped() {
        final PLabel l = label("0");
        ctx.execute(() -> {
            l.setText("lost");
            l.onDestroy();
        });
        assertEquals(0, wire.count("TEXT=lost"));
    }

    @Test
    public void nothingStaysPendingAfterTheTransaction() {
        final PLabel l = label("0");
        ctx.execute(() -> l.setText("x"));
        assertEquals(0, ctx.getWriter().pendingUpdateCount());
        assertEquals(1, wire.count("TEXT=x"));
    }

    @Test
    public void methodCallsAndSideEffectsAreNeverCoalesced() {
        final PButton[] b = new PButton[1];
        ctx.execute(() -> { b[0] = Element.newPButton("ok"); root.add(b[0]); });
        wire.clear();
        ctx.execute(() -> {
            b[0].setEnabled(false);
            b[0].setEnabled(true);
            b[0].evalTerminalScript("a()");
            b[0].evalTerminalScript("a()");
        });
        assertEquals("boolean toggles are not deferred", 2, wire.count("ENABLED="));
        assertEquals(2, wire.count("EVAL=a()"));
    }

    @Test
    public void manyWidgetsAndChurnKeepOnlyFinalValues() {
        final PLabel[] cells = new PLabel[50];
        ctx.execute(() -> { for (int i = 0; i < cells.length; i++) { cells[i] = Element.newPLabel("-"); root.add(cells[i]); } });
        wire.clear();
        ctx.execute(() -> {
            for (int round = 0; round < 20; round++) {
                for (int i = 0; i < cells.length; i++) cells[(i * 7 + round) % cells.length].setText("v" + round + "-" + i);
            }
        });
        assertEquals("one update per cell", cells.length, wire.blocks.size());
        for (final List<String> block : wire.blocks) assertTrue(block.get(1), block.get(1).startsWith("TEXT=v19-"));
    }

    @Test
    public void disabledCoalescingWritesEveryUpdate() {
        start(false);
        final PLabel l = label("0");
        assertFalse(ctx.getWriter().isCoalescing());
        ctx.execute(() -> {
            l.setText("1");
            l.setText("2");
        });
        assertEquals(2, wire.blocks.size());
    }
}
