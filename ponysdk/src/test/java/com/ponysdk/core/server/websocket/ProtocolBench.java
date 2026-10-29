/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.websocket;

import java.io.IOException;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ProtocolVersion;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.Conflator;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.route.Route;
import com.ponysdk.core.ui.route.RouteParameters;
import com.ponysdk.core.ui.route.Router;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.basic.PEventOptions;

/**
 * Reproducible server-side bench of the real write path: a {@link UIContext} driving a real {@link WebSocketPusher}
 * (string dictionary, coalescing, update runs), the frames captured as sent, then deflated as permessage-deflate
 * would (raw deflate, sync flush, context takeover, level 3) for the wire size.
 * <p>
 * Per scenario: protocol and wire bytes, CPU and allocated bytes per operation on the application thread (deflate
 * excluded: it runs afterwards on the captured frames), and the retained heap per row. {@code -Dbench.stats=true}
 * adds the byte breakdown per protocol model.
 *
 * <pre>./gradlew :ponysdk:bench [-Pbench.args="v1|v2|v3 [scenario prefix] [op multiplier]"]</pre>
 */
public final class ProtocolBench {

    // ---- capture --------------------------------------------------------------------------------------------

    /** Sent frames, copied into one preallocated array: no allocation on the measured path. */
    private static final class Capture {
        final byte[] data;
        final int[] ends;
        int size, frames;

        Capture(final int bytes) {
            data = new byte[bytes];
            ends = new int[bytes >> 6];
        }

        void add(final ByteBuffer b) {
            final int n = b.remaining();
            b.get(data, size, n);
            size += n;
            ends[frames++] = size;
        }

        void clear() {
            size = 0;
            frames = 0;
        }

        /** permessage-deflate size of the captured frames (headers included), one shared deflate context. */
        long wire() {
            final Deflater d = new Deflater(3, true);
            final byte[] out = new byte[1 << 20];
            long total = 0;
            int from = 0;
            for (int f = 0; f < frames; f++) {
                d.setInput(data, from, ends[f] - from);
                int n = 0, k;
                do {
                    k = d.deflate(out, 0, out.length, Deflater.SYNC_FLUSH);
                    n += k;
                } while (k == out.length);
                n -= 4; // the 00 00 FF FF tail is not sent
                total += n + (n < 126 ? 2 : n < 65536 ? 4 : 10);
                from = ends[f];
            }
            d.end();
            return total;
        }
    }

    private static final class Bench {
        final Capture capture;
        final WebSocketPusher pusher;
        final UIContext ctx;
        PFlowPanel root;

        Bench(final int protocolVersion) {
            this(protocolVersion, 128 << 20);
        }

        Bench(final int protocolVersion, final int captureBytes) {
            capture = new Capture(captureBytes);
            final Session session = (Session) Proxy.newProxyInstance(Session.class.getClassLoader(),
                new Class<?>[] {Session.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "sendBinary" -> {
                        capture.add((ByteBuffer) args[0]);
                        ((Callback) args[1]).succeed();
                        yield null;
                    }
                    case "isOpen" -> true;
                    case "hashCode" -> 1;
                    case "equals" -> proxy == args[0];
                    default -> null;
                });
            pusher = new WebSocketPusher(session, 1 << 15, 1_000, new ByteBufferPool(1 << 15, 128));
            final WebSocket socket = new WebSocket() {
                @Override
                public void flush() {
                    try {
                        pusher.flush();
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };
            socket.setUpgradeRequestData(new UpgradeRequestData(Map.of(), "bench", null));
            socket.setProtocolVersion(protocolVersion); // as the handshake does: the context speaks the bench's version
            final TxnContext txn = new TxnContext(socket);
            final WebsocketEncoder direct = new WebsocketEncoder() {
                @Override public void beginObject() {}
                @Override public void endObject() { encode(ServerToClientModel.END, null); }
                @Override public int protocolVersion() { return protocolVersion; }
                @Override public void encode(final ServerToClientModel model, final Object value) {
                    try { pusher.encode(model, value); } catch (final IOException e) { throw new IllegalStateException(e); }
                }
                @Override public void encodeInt(final ServerToClientModel model, final int value) {
                    try { pusher.encodeInt(model, value); } catch (final IOException e) { throw new IllegalStateException(e); }
                }
            };
            txn.getWriter().setEncoder(direct);
            final ApplicationConfiguration config = new ApplicationConfiguration();
            config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
            ctx = new UIContext(socket, txn, config);
            pusher.setStringDictionary(ctx.getStringDictionary());
            ctx.execute(() -> {
                root = Element.newPFlowPanel();
                PWindow.getMain().add(root);
            });
            capture.clear();
        }
    }

    // ---- scenarios ------------------------------------------------------------------------------------------

    private static final String[] NAMES = {"EUR/USD", "GBP/USD", "USD/JPY", "USD/CHF", "AUD/USD", "NZD/USD", "USD/CAD",
                                           "EUR/GBP", "EUR/JPY", "GBP/JPY"};

    /** A blotter row: 2 labels, a text box, a button with a click handler, a check box. */
    private static PFlowPanel row(final int i) {
        final PFlowPanel row = Element.newPFlowPanel();
        row.addStyleName("row");
        final PLabel name = Element.newPLabel(NAMES[i % NAMES.length] + " " + i);
        name.addStyleName("name");
        final PLabel price = Element.newPLabel("1." + (10_000 + i * 7 % 90_000));
        price.addStyleName("price");
        final var qty = Element.newPTextBox();
        qty.setPlaceholder("Quantity");
        final var buy = Element.newPButton("Buy");
        buy.addStyleName("buy");
        buy.addClickHandler(e -> {});
        final var alert = Element.newPCheckBox("Alert");
        row.add(name);
        row.add(price);
        row.add(qty);
        row.add(buy);
        row.add(alert);
        return row;
    }

    /** A screen of a few rows, as the views of the router scenario. */
    private abstract static class BenchView implements IsPWidget {

        private final PFlowPanel panel = Element.newPFlowPanel();

        BenchView(final int rows) {
            for (int r = 0; r < rows; r++) panel.add(row(r));
        }

        @Override
        public PFlowPanel asWidget() {
            return panel;
        }
    }

    @Route("orders")
    static final class BenchList extends BenchView {
        BenchList() {
            super(5);
        }
    }

    @Route("orders/:id")
    static final class BenchDetail extends BenchView {
        BenchDetail(final RouteParameters parameters) {
            super(5 + parameters.getInt("id") % 2);
        }
    }

    @Route("orders/:id/edit")
    static final class BenchEdit extends BenchView {
        BenchEdit() {
            super(2);
        }
    }

    private interface Op {
        void run(Bench b, int iteration);
    }

    private record Result(String name, int ops, long protocol, long wire, long cpuNs, long allocated) {
        void print() {
            System.out.printf("%-34s %8.1f B  %8.1f B  %8.2f µs  %9.0f B%n", name, (double) protocol / ops,
                (double) wire / ops, cpuNs / 1_000.0 / ops, (double) allocated / ops);
        }
    }

    private static final com.sun.management.ThreadMXBean THREADS =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    /** Scenario filter (name prefix) and op multiplier, for profiling: {@code -Pbench.args="v2 page 10"}. */
    private static String only = "";
    private static int repeat = 1;

    private static void run(final String name, final int protocolVersion, final Op setup, final Op op,
                            final int warmup, final int ops) {
        if (!name.startsWith(only)) return;
        measure(name, protocolVersion, setup, op, warmup * repeat, ops * repeat).print();
    }

    private static Result measure(final String name, final int protocolVersion, final Op setup, final Op op,
                                  final int warmup, final int ops) {
        // warm the JIT on a throwaway context, then measure a fresh one (its dictionary starts empty, like a session)
        final Bench w = new Bench(protocolVersion);
        w.ctx.execute(() -> setup.run(w, 0));
        for (int i = 0; i < warmup; i++) {
            final int k = i;
            w.ctx.execute(() -> op.run(w, k));
            if (w.capture.size > w.capture.data.length / 2) w.capture.clear();
        }
        w.ctx.destroy();

        final Bench b = new Bench(protocolVersion);
        b.ctx.execute(() -> setup.run(b, 0));
        b.capture.clear();
        final boolean stats = Boolean.getBoolean("bench.stats");
        if (stats) WebSocketPusher.startRecordingStats(1, TimeUnit.HOURS, null, null);
        final long cpu0 = THREADS.getCurrentThreadCpuTime();
        final long alloc0 = THREADS.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < ops; i++) {
            final int k = i;
            b.ctx.execute(() -> op.run(b, k));
        }
        final long cpu = THREADS.getCurrentThreadCpuTime() - cpu0;
        final long alloc = THREADS.getCurrentThreadAllocatedBytes() - alloc0;
        final Result r = new Result(name, ops, b.capture.size, b.capture.wire(), cpu, alloc);
        if (Boolean.getBoolean("bench.debug")) System.out.printf("   frames/op=%.2f%n", (double) b.capture.frames / ops);
        if (stats) printStats(name, WebSocketPusher.stopRecordingStats(), ops);
        b.ctx.destroy();
        return r;
    }

    private static void printStats(final String name, final WebSocketStats s, final int ops) {
        final long total = s.getSummaryBandwidth().getTotal();
        System.out.printf("   %s: protocol bytes per model (meta / data per op)%n", name);
        s.getBandwidthPerServerToClientModel().entrySet().stream()
            .sorted((x, y) -> Long.compare(y.getValue().getTotal(), x.getValue().getTotal()))
            .forEach(e -> System.out.printf("     %-22s %5.1f%%  %8.1f / %8.1f B%n", e.getKey(),
                100.0 * e.getValue().getTotal() / total, (double) e.getValue().getMeta() / ops,
                (double) e.getValue().getData() / ops));
    }

    /**
     * ADR-0013: quotes of 100 instruments from a feed thread, between two flushes the client takes; per quote. An
     * {@code execute} per quote, or a {@link Conflator} drained once after them (on this thread, to be measured).
     */
    private static void stream(final int protocolVersion, final int quotes, final boolean conflated) {
        final String name = "stream: " + quotes + " quotes/flush, " + (conflated ? "conflated" : "execute each");
        if (!name.startsWith(only)) return;
        final int instruments = 100;
        final int warmup = 200 * repeat, ops = 300 * repeat;
        Result result = null;
        for (final int rounds : new int[] {warmup, ops}) {
            final Bench b = new Bench(protocolVersion);
            final List<PLabel> cells = new ArrayList<>(instruments);
            b.ctx.execute(() -> {
                for (int c = 0; c < instruments; c++) {
                    final PLabel l = Element.newPLabel("1.00000");
                    b.root.add(l);
                    cells.add(l);
                }
            });
            b.capture.clear();
            final ArrayDeque<Runnable> drains = new ArrayDeque<>();
            final Conflator<Integer, String> conflator = Conflator.<Integer, String> builder(b.ctx,
                (instrument, price) -> cells.get(instrument).setText(price)).executor(drains::add).build();
            final Random rnd = new Random(11);
            final String[] prices = new String[4_096];
            for (int p = 0; p < prices.length; p++) prices[p] = "1." + (10_000 + rnd.nextInt(90_000));
            final long cpu0 = THREADS.getCurrentThreadCpuTime();
            final long alloc0 = THREADS.getCurrentThreadAllocatedBytes();
            for (int op = 0; op < rounds; op++) {
                for (int q = 0; q < quotes; q++) {
                    final int instrument = rnd.nextInt(instruments);
                    final String price = prices[rnd.nextInt(prices.length)];
                    if (conflated) conflator.offer(instrument, price);
                    else b.ctx.execute(() -> cells.get(instrument).setText(price));
                }
                Runnable drain;
                while ((drain = drains.poll()) != null) drain.run();
            }
            final long cpu = THREADS.getCurrentThreadCpuTime() - cpu0;
            final long alloc = THREADS.getCurrentThreadAllocatedBytes() - alloc0;
            final int perQuote = rounds * quotes;
            result = new Result(name, perQuote, b.capture.size, b.capture.wire(), cpu, alloc);
            if (rounds == ops) System.out.printf("   frames per quote %.3f, applied %d of %d%n", (double) b.capture.frames / perQuote,
                conflated ? conflator.getApplied() : perQuote, perQuote);
            b.ctx.destroy();
        }
        result.print();
    }

    /** Retained heap per row of {@link #row}: 5 widgets, 1 handler, styles. */
    private static void heap(final int protocolVersion) {
        final int rows = 5_000;
        final Bench b = new Bench(protocolVersion, 64 << 20);
        final List<PWidget> keep = new ArrayList<>(rows);
        final long before = usedAfterGc();
        final Map<String, long[]> h0 = Boolean.getBoolean("bench.histo") ? histogram() : null;
        b.ctx.execute(() -> {
            for (int i = 0; i < rows; i++) {
                final PFlowPanel r = row(i);
                b.root.add(r);
                keep.add(r);
            }
        });
        b.capture.clear();
        final long after = usedAfterGc();
        if (h0 != null) printHistogramDiff("row", h0, rows);
        System.out.printf("%-34s %8.0f B per row (5 widgets)  %6.0f B per widget%n", "heap: blotter row",
            (double) (after - before) / rows, (double) (after - before) / rows / 6);
        if (keep.size() != rows) throw new AssertionError();
        b.ctx.destroy();

        final List<Bench> contexts = new ArrayList<>();
        new Bench(protocolVersion, 1 << 12).ctx.destroy(); // class init, shared state
        final long c0 = usedAfterGc();
        final Map<String, long[]> hc = Boolean.getBoolean("bench.histo") ? histogram() : null;
        for (int i = 0; i < 200; i++) contexts.add(new Bench(protocolVersion, 1 << 12));
        final long c1 = usedAfterGc();
        if (hc != null) printHistogramDiff("UIContext", hc, contexts.size());
        System.out.printf("%-34s %8.0f B per UIContext%n", "heap: empty UIContext", (double) (c1 - c0) / contexts.size());
        contexts.forEach(x -> x.ctx.destroy());

        // A PFlexTable of labels (DataGrid v1, list views): table bookkeeping per cell, the label included
        final Bench t = new Bench(protocolVersion, 64 << 20);
        final int tableRows = 2_000, tableColumns = 10;
        final com.ponysdk.core.ui.basic.PFlexTable[] table = new com.ponysdk.core.ui.basic.PFlexTable[1];
        final long t0 = usedAfterGc();
        t.ctx.execute(() -> {
            table[0] = Element.newPFlexTable();
            t.root.add(table[0]);
            for (int r = 0; r < tableRows; r++) {
                for (int c = 0; c < tableColumns; c++) table[0].setWidget(r, c, Element.newPLabel("1.0"));
            }
        });
        t.capture.clear();
        final long t1 = usedAfterGc();
        System.out.printf("%-34s %8.0f B per cell (a label in a PFlexTable)%n", "heap: table cell",
            (double) (t1 - t0) / (tableRows * tableColumns));
        if (table[0].getRowCount() != tableRows) throw new AssertionError();
        t.ctx.destroy();
    }

    private static void printHistogramDiff(final String unit, final Map<String, long[]> h0, final int count) {
        final Map<String, long[]> h1 = histogram();
        System.out.printf("   retained per %s, by class (instances, bytes):%n", unit);
        h1.entrySet().stream().map(e -> {
            final long[] o = h0.getOrDefault(e.getKey(), new long[2]);
            return Map.entry(e.getKey(), new long[] {e.getValue()[0] - o[0], e.getValue()[1] - o[1]});
        }).filter(e -> e.getValue()[1] > count * 2L).sorted((x, y) -> Long.compare(y.getValue()[1], x.getValue()[1]))
            .limit(30).forEach(e -> System.out.printf("     %6.2f  %7.1f B  %s%n", (double) e.getValue()[0] / count,
                (double) e.getValue()[1] / count, e.getKey()));
    }

    private static Map<String, long[]> histogram() {
        try {
            final javax.management.ObjectName name = new javax.management.ObjectName("com.sun.management:type=DiagnosticCommand");
            final String out = (String) ManagementFactory.getPlatformMBeanServer().invoke(name, "gcClassHistogram",
                new Object[] {new String[0]}, new String[] {String[].class.getName()});
            final Map<String, long[]> m = new java.util.HashMap<>();
            for (final String line : out.split("\n")) {
                final String[] f = line.trim().split("\\s+");
                if (f.length >= 4 && f[0].endsWith(":")) m.put(f[3], new long[] {Long.parseLong(f[1]), Long.parseLong(f[2])});
            }
            return m;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static long usedAfterGc() {
        final Runtime rt = Runtime.getRuntime();
        long used = Long.MAX_VALUE;
        for (int i = 0; i < 6; i++) {
            System.gc();
            try { Thread.sleep(30); } catch (final InterruptedException e) { Thread.currentThread().interrupt(); }
            used = Math.min(used, rt.totalMemory() - rt.freeMemory());
        }
        return used;
    }

    public static void main(final String[] args) {
        final int v = args.length > 0 && args[0].matches("v\\d") ? Integer.parseInt(args[0].substring(1)) : ProtocolVersion.CURRENT;
        if (args.length > 1) only = args[1].replace('_', ' ');
        if (args.length > 2) repeat = Integer.parseInt(args[2]);
        System.out.printf("PonySDK protocol bench, protocol v%d, %s%n", v, System.getProperty("java.version"));
        System.out.printf("%-34s %10s  %10s  %11s  %11s%n", "scenario (per op)", "protocol", "wire", "cpu", "alloc");

        final List<PLabel> cells = new ArrayList<>();
        final Op grid = (b, i) -> {
            cells.clear();
            final PFlowPanel g = Element.newPFlowPanel();
            for (int c = 0; c < 240; c++) {
                final PLabel l = Element.newPLabel("1.00000");
                g.add(l);
                cells.add(l);
            }
            b.root.add(g);
        };

        run("page: 100 blotter rows", v, (b, i) -> {}, (b, i) -> {
            b.root.clear();
            for (int r = 0; r < 100; r++) b.root.add(row(r));
        }, 300, 300);

        final Random rnd = new Random(7);
        run("tick: 60 of 240 cells, text+class", v, grid, (b, i) -> {
            for (int k = 0; k < 60; k++) {
                final PLabel l = cells.get(rnd.nextInt(cells.size()));
                final boolean up = rnd.nextBoolean();
                l.setText("1." + (10_000 + rnd.nextInt(90_000)));
                l.setStyleName(up ? "up" : "down");
            }
        }, 3_000, 5_000);

        run("tick: 60 cells, text+class toggle", v, grid, (b, i) -> {
            for (int k = 0; k < 60; k++) {
                final PLabel l = cells.get(rnd.nextInt(cells.size()));
                final boolean up = rnd.nextBoolean();
                l.setText("1." + (10_000 + rnd.nextInt(90_000)));
                l.removeStyleName(up ? "down" : "up");
                l.addStyleName(up ? "up" : "down");
            }
        }, 3_000, 5_000);

        run("tick: 60 cells, text+style colour", v, grid, (b, i) -> {
            for (int k = 0; k < 60; k++) {
                final PLabel l = cells.get(rnd.nextInt(cells.size()));
                l.setText("1." + (10_000 + rnd.nextInt(90_000)));
                l.setStyleProperty("color", rnd.nextBoolean() ? "#16a34a" : "#dc2626");
            }
        }, 3_000, 5_000);

        run("tick: 1 cell (latency path)", v, grid, (b, i) -> cells.get(i % 240).setText("1." + (10_000 + i % 90_000)),
            20_000, 50_000);

        final List<PFlowPanel> list = new ArrayList<>();
        run("churn: replace 20 rows", v, (b, i) -> {
            for (int r = 0; r < 20; r++) {
                final PFlowPanel row = row(r);
                b.root.add(row);
                list.add(row);
            }
        }, (b, i) -> {
            for (final PFlowPanel r : list) r.removeFromParent();
            list.clear();
            for (int r = 0; r < 20; r++) {
                final PFlowPanel row = row(i * 20 + r);
                b.root.add(row);
                list.add(row);
            }
        }, 1_000, 2_000);

        // ADR-0020: a navigation between two views of a router (match, build, swap, destroy, the token), against the
        // same swap written by hand: what the router adds
        final Router[] router = new Router[1];
        run("route: navigate between 2 views", v, (b, i) -> {
            final PSimplePanel panel = Element.newPSimplePanel();
            b.root.add(panel);
            router[0] = Router.builder(panel).views(BenchList.class, BenchDetail.class, BenchEdit.class).start();
        }, (b, i) -> router[0].navigate(i % 2 == 0 ? "orders/7" : "orders"), 2_000, 5_000);

        final PSimplePanel[] panel = new PSimplePanel[1];
        final PWidget[] shown = new PWidget[1];
        run("route: the same swap by hand", v, (b, i) -> {
            panel[0] = Element.newPSimplePanel();
            b.root.add(panel[0]);
            shown[0] = null;
        }, (b, i) -> {
            final PFlowPanel view = new BenchView(i % 2 == 0 ? 6 : 5) { }.asWidget(); // what the two views above build
            panel[0].setWidget(view);
            if (shown[0] != null) shown[0].onDestroy();
            shown[0] = view;
            UIContext.get().getHistory().newItem(i % 2 == 0 ? "orders/7" : "orders", false);
        }, 2_000, 5_000);

        for (final int quotes : new int[] {100, 1_000}) {
            stream(v, quotes, false);
            stream(v, quotes, true);
        }

        // The pending buffer is pooled per thread: one 3 000-update burst (op 0, warm-up included) leaves it grown
        // for every later small transaction on this thread. Last on purpose, it would skew the scenarios after it.
        run("tick: 1 cell after a burst", v, grid, (b, i) -> {
            if (i == 0) for (final PLabel l : cells) for (int p = 0; p < 13; p++) l.setStyleProperty("p" + p, "v");
            cells.get(i % 240).setText("1." + (10_000 + i % 90_000));
        }, 20_000, 50_000);

        if ("heap".startsWith(only) || only.isEmpty()) heap(v);
        if ("inbound".startsWith(only) || only.isEmpty()) inbound(v);
    }

    // ---- from the terminal ----------------------------------------------------------------------------------

    /**
     * What a message from the terminal costs the server, on the path of WebSocket.onWebSocketText: the JSON parsed,
     * then each instruction given to its object in a transaction (fireClientData), whose answer is written. Parse
     * and dispatch are also measured apart: a binary format from the terminal could only save on the parse.
     */
    private static void inbound(final int protocolVersion) {
        final Bench b = new Bench(protocolVersion);
        final int[] ids = new int[4];
        b.ctx.execute(() -> {
            final var button = Element.newPButton("Buy");
            button.addClickHandler(e -> {});
            final PTextBox quantity = Element.newPTextBox();
            quantity.addValueChangeHandler(e -> {});
            final PScrollPanel scroll = Element.newPScrollPanel();
            scroll.addScrollHandler(e -> {});
            final PTextBox search = Element.newPTextBox();
            search.addEventListener("input", PEventOptions.fields("target.value"), e -> {});
            b.root.add(button, quantity, scroll, search);
            ids[0] = button.getID();
            ids[1] = quantity.getID();
            ids[2] = scroll.getID();
            ids[3] = search.getID();
        });
        final String i = ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue();
        final String[][] messages = {
            {"click", "{\"" + i + "\":[{\"0\":" + ids[0] + ",\"g\":2,\"f\":[412,233,32,11,1,false,false,false,false],\"d\":[380,222,64,24]}]}"},
            {"value change", "{\"" + i + "\":[{\"0\":" + ids[1] + ",\"C\":\"1250000\"}]}"},
            {"scroll", "{\"" + i + "\":[{\"0\":" + ids[2] + ",\"M\":0,\"N\":540,\"O\":0,\"P\":120,\"Q\":0}]}"},
            {"event by name", "{\"" + i + "\":[{\"0\":" + ids[3] + ",\"Z\":\"input\",\"z\":{\"target.value\":\"EUR/USD\"}}]}"},
        };
        System.out.printf("%-34s %10s  %11s  %11s  %11s  %11s%n", "from the terminal (per message)", "text", "parse",
            "dispatch", "total", "alloc");
        for (final String[] m : messages) {
            final String text = m[1];
            final JsonObject parsed = parse(b, text);
            final JsonObject instruction = parsed.getJsonArray(i).getJsonObject(0);
            final int warmup = 200_000;
            final int ops = 500_000;
            for (int k = 0; k < warmup; k++) {
                parse(b, text);
                b.ctx.execute(() -> b.ctx.fireClientData(instruction));
                if (b.capture.size > b.capture.data.length / 2) b.capture.clear();
            }
            b.capture.clear();
            long cpu0 = THREADS.getCurrentThreadCpuTime();
            for (int k = 0; k < ops; k++) parse(b, text);
            final long parse = THREADS.getCurrentThreadCpuTime() - cpu0;
            cpu0 = THREADS.getCurrentThreadCpuTime();
            for (int k = 0; k < ops; k++) {
                b.ctx.execute(() -> b.ctx.fireClientData(instruction));
                if (b.capture.size > b.capture.data.length / 2) b.capture.clear();
            }
            final long dispatch = THREADS.getCurrentThreadCpuTime() - cpu0;
            cpu0 = THREADS.getCurrentThreadCpuTime();
            final long alloc0 = THREADS.getCurrentThreadAllocatedBytes();
            for (int k = 0; k < ops; k++) {
                final JsonObject json = parse(b, text);
                b.ctx.execute(() -> {
                    final var array = json.getJsonArray(i);
                    for (int n = 0; n < array.size(); n++) b.ctx.fireClientData(array.getJsonObject(n));
                });
                if (b.capture.size > b.capture.data.length / 2) b.capture.clear();
            }
            final long total = THREADS.getCurrentThreadCpuTime() - cpu0;
            final long alloc = THREADS.getCurrentThreadAllocatedBytes() - alloc0;
            System.out.printf("%-34s %8d B  %8.2f µs  %8.2f µs  %8.2f µs  %9.0f B%n", "in: " + m[0], text.length(),
                parse / 1_000.0 / ops, dispatch / 1_000.0 / ops, total / 1_000.0 / ops, (double) alloc / ops);
        }
        b.ctx.destroy();
    }

    private static JsonObject parse(final Bench b, final String text) {
        try (final JsonReader reader = b.ctx.getJsonProvider().createReader(new StringReader(text))) {
            return reader.readObject();
        }
    }
}
