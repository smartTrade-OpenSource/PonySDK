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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        int version = com.ponysdk.core.model.ProtocolVersion.V1;
        @Override public int protocolVersion() { return version; }
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
        start(coalescing, com.ponysdk.core.model.ProtocolVersion.V1);
    }

    private void start(final boolean coalescing, final int protocolVersion) {
        wire = new Wire();
        wire.version = protocolVersion;
        final WebSocket socket = Mockito.mock(WebSocket.class);
        Mockito.when(socket.getUpgradeRequestData()).thenReturn(
                new com.ponysdk.core.server.websocket.UpgradeRequestData(Map.of(), "test", Mockito.mock(jakarta.servlet.http.HttpSession.class)));
        final TxnContext txn = new TxnContext(socket);
        txn.getWriter().setEncoder(wire);
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setUpdateCoalescingEnabled(coalescing);
        ctx = new UIContext(socket, txn, config);
        ctx.execute(() -> {
            root = Element.newPFlowPanel();
            PWindow.getMain().add(root);
        });
        wire.clear();
    }

    @BeforeEach
    public void setUp() {
        start(true);
    }

    @AfterEach
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
        assertEquals(2, wire.count("ENABLED="), "boolean toggles are not deferred");
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
        assertEquals(cells.length, wire.blocks.size(), "one update per cell");
        for (final List<String> block : wire.blocks) assertTrue(block.get(1).startsWith("TEXT=v19-"), block.get(1));
    }

    // ---- update runs (protocol 2) ----

    private PLabel[] labels(final int n) {
        final PLabel[] cells = new PLabel[n];
        ctx.execute(() -> { for (int i = 0; i < n; i++) { cells[i] = Element.newPLabel("-"); root.add(cells[i]); } });
        wire.clear();
        return cells;
    }

    @Test
    public void protocol2WritesOneSortedRunWithRelativeMarkers() {
        start(true, com.ponysdk.core.model.ProtocolVersion.UPDATE_RUNS);
        final PLabel[] c = labels(400);
        final int base = c[0].getID();
        ctx.execute(() -> {
            c[300].setText("far");          // written first, sorted last: absolute id marker
            c[3].setText("d");
            c[1].setText("b");
            c[1].setStyleProperty("color", "red");
            c[2].setText("c");              // id+1 -> UPDATE_NEXT
            c[0].setText("a");
        });
        assertEquals(1, wire.blocks.size());
        assertEquals(List.of("TYPE_UPDATE=" + base, "TEXT=a",
                "UPDATE_NEXT=null", "TEXT=b", "PUT_STYLE_KEY=color", "STYLE_VALUE=red",
                "UPDATE_NEXT=null", "TEXT=c",
                "UPDATE_NEXT=null", "TEXT=d",
                "UPDATE_NEXT_ID=" + c[300].getID(), "TEXT=far"), wire.blocks.get(0));
    }

    @Test
    public void protocol2UsesAByteDeltaForSmallGaps() {
        start(true, com.ponysdk.core.model.ProtocolVersion.UPDATE_RUNS);
        final PLabel[] c = labels(300);
        ctx.execute(() -> {
            c[0].setText("x");
            c[5].setText("y");    // gap 5 -> delta byte 3
            c[262].setText("z");  // gap 257 -> delta byte 255 (largest)
            c[263 + 1].setText("w"); // gap 2 -> delta byte 0
        });
        assertEquals(List.of("TYPE_UPDATE=" + c[0].getID(), "TEXT=x", "UPDATE_NEXT_DELTA=3", "TEXT=y",
                "UPDATE_NEXT_DELTA=-1", "TEXT=z", "UPDATE_NEXT_DELTA=0", "TEXT=w"), wire.blocks.get(0));
    }

    @Test
    public void protocol2KeepsThePropertyOrderOfOneObject() {
        start(true, com.ponysdk.core.model.ProtocolVersion.UPDATE_RUNS);
        final com.ponysdk.core.ui.basic.PHTML[] h = new com.ponysdk.core.ui.basic.PHTML[2];
        ctx.execute(() -> { h[0] = Element.newPHTML("x"); h[1] = Element.newPHTML("y"); root.add(h[0]); root.add(h[1]); });
        wire.clear();
        ctx.execute(() -> {
            h[1].setHTML("<b>1</b>");
            h[0].setText("plain");
            h[1].setText("plain");
            h[1].setHTML("<i>2</i>"); // last for h[1]
        });
        final List<String> block = wire.blocks.get(0);
        assertEquals("HTML=<i>2</i>", block.get(block.size() - 1));
        assertTrue(block.indexOf("TEXT=plain") < block.lastIndexOf("TEXT=plain"));
    }

    @Test
    public void protocol2SkipsDestroyedObjectsInsideARun() {
        start(true, com.ponysdk.core.model.ProtocolVersion.UPDATE_RUNS);
        final PLabel[] c = labels(3);
        ctx.execute(() -> {
            c[0].setText("a");
            c[1].setText("lost");
            c[2].setText("c");
            c[1].onDestroy();
        });
        assertEquals(List.of(List.of("TYPE_UPDATE=" + c[0].getID(), "TEXT=a", "UPDATE_NEXT_DELTA=0", "TEXT=c")), wire.blocks);
    }

    @Test
    public void protocol2SplitsRunsAtTheMaximumLength() {
        start(true, com.ponysdk.core.model.ProtocolVersion.UPDATE_RUNS);
        final PLabel[] c = labels(PendingUpdates.MAX_RUN + 10);
        ctx.execute(() -> { for (final PLabel l : c) l.setText("v"); });
        assertEquals(2, wire.blocks.size());
        assertEquals(PendingUpdates.MAX_RUN, wire.blocks.get(0).stream().filter(e -> e.startsWith("TEXT=")).count());
        assertEquals("TYPE_UPDATE=" + c[PendingUpdates.MAX_RUN].getID(), wire.blocks.get(1).get(0));
    }

    @Test
    public void protocol1NeverWritesRunMarkers() {
        final PLabel[] c = labels(5);
        ctx.execute(() -> { for (final PLabel l : c) l.setText("v"); });
        assertEquals(5, wire.blocks.size());
        assertEquals(0, wire.count("UPDATE_NEXT"));
    }

    @Test
    public void eachTransactionStartsFromAnEmptyBufferAndManyPropertiesChain() {
        final PLabel l = label("0");
        for (int t = 0; t < 3; t++) {
            final int tx = t;
            wire.clear();
            ctx.execute(() -> {
                for (int k = 0; k < 6; k++) l.setStyleProperty("p" + k, "v" + tx);
                l.setText("a" + tx);
                l.setStyleProperty("p2", "w" + tx); // overwrite inside a 7-slot chain
                l.setText("b" + tx);
            });
            assertEquals(1, wire.blocks.size(), "tx " + t);
            assertEquals(1, wire.count("TEXT="));
            assertEquals(1, wire.count("TEXT=b" + t));
            assertEquals(6, wire.count("PUT_STYLE_KEY="));
            assertEquals(1, wire.count("STYLE_VALUE=w" + t));
        }
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

    @Test
    public void setStyleNameReplacesTheClassListAndCanReturnToAPreviousName() {
        final PLabel l = label("0");
        for (final String name : new String[] {"up", "down", "up"}) {
            ctx.execute(() -> l.setStyleName(name));
            assertEquals(List.of(List.of("TYPE_UPDATE=" + l.getID(), "STYLE_NAME=" + name)), wire.blocks);
            wire.clear();
        }
        assertTrue(l.hasStyleName("up"));
        assertFalse(l.hasStyleName("down"), "replaced, not accumulated");
    }

    @Test
    public void onlyTheLastStyleNameOfATransactionIsSent() {
        final PLabel l = label("0");
        ctx.execute(() -> {
            l.setStyleName("a");
            l.setText("t");
            l.setStyleName("b");
        });
        assertEquals(List.of(List.of("TYPE_UPDATE=" + l.getID(), "TEXT=t", "STYLE_NAME=b")), wire.blocks);
    }

    @Test
    public void styleNameSetBeforeAttachIsSentOnceInTheCreateBlock() {
        final PLabel[] l = new PLabel[1];
        ctx.execute(() -> {
            l[0] = Element.newPLabel("x");
            l[0].addStyleName("extra");
            l[0].setStyleName("main");
            l[0].addStyleName("second");
            root.add(l[0]);
        });
        assertEquals(1, wire.count("STYLE_NAME=main"));
        assertEquals(1, wire.count("ADD_STYLE_NAME=second"));
        assertEquals(0, wire.count("ADD_STYLE_NAME=extra"), "the earlier name is replaced");
    }

    @Test
    public void classTogglesAndTextOfManyCellsShareOneUpdateRun() {
        start(true, com.ponysdk.core.model.ProtocolVersion.UPDATE_RUNS);
        final PLabel[] c = labels(4);
        ctx.execute(() -> {
            for (final PLabel l : c) {
                l.setText("1.2");
                l.removeStyleName("down");
                l.addStyleName("up");
            }
        });
        assertEquals(1, wire.blocks.size(), "one block: class changes no longer cut the run");
        assertEquals(4, wire.count("ADD_STYLE_NAME=up"));
        assertEquals(0, wire.count("REMOVE_STYLE_NAME=down"), "never added: nothing to remove");
    }

    @Test
    public void theLastClassChangeOfATransactionWins() {
        final PLabel l = label("0");
        ctx.execute(() -> {
            l.addStyleName("x");
            l.removeStyleName("x");
            l.addStyleName("x");
        });
        assertEquals(List.of(List.of("TYPE_UPDATE=" + l.getID(), "REMOVE_STYLE_NAME=x", "ADD_STYLE_NAME=x")), wire.blocks);
        assertTrue(l.hasStyleName("x"));
    }

    @Test
    public void visibilityStylePropertyAndAttributeRemovalsAreCoalesced() {
        final PLabel l = label("0");
        ctx.execute(() -> {
            l.setStyleProperty("color", "red");
            l.setAttribute("data-k", "v");
        });
        wire.clear();
        ctx.execute(() -> {
            l.setVisible(false);
            l.setText("t");
            l.setVisible(true);
            l.removeStyleProperty("color");
            l.removeAttribute("data-k");
        });
        assertEquals(List.of(List.of("TYPE_UPDATE=" + l.getID(), "TEXT=t", "WIDGET_VISIBLE=true",
            "REMOVE_STYLE_KEY=color", "REMOVE_ATTRIBUTE_KEY=data-k")), wire.blocks);
    }

    @Test
    public void smallTransactionsAfterABurstStillKeepOnlyTheLastValue() {
        final PLabel[] c = labels(300);
        // burst: grows the pooled buffer and its index far beyond the next transactions (sparse clears after)
        ctx.execute(() -> {
            for (final PLabel l : c) {
                l.setText("burst");
                for (int p = 0; p < 9; p++) l.setStyleProperty("p" + p, "v");
            }
        });
        for (int round = 0; round < 50; round++) {
            wire.clear();
            final int r = round;
            ctx.execute(() -> {
                for (int i = 0; i < 5; i++) {
                    final PLabel l = c[(r * 7 + i * 31) % c.length];
                    l.setText("a" + r);
                    l.setStyleProperty("p" + i, "x");
                    l.setText("b" + r);
                }
            });
            assertEquals(5, wire.count("TEXT="), "round " + r);
            assertEquals(5, wire.count("TEXT=b" + r), "round " + r);
            assertEquals(5, wire.count("PUT_STYLE_KEY="), "round " + r);
        }
    }
}
