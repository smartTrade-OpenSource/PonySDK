/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.websocket;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

import com.ponysdk.core.model.ProtocolVersion;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;

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
            ctx = new UIContext(socket, txn, config, null);
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

        // The pending buffer is pooled per thread: one 3 000-update burst (op 0, warm-up included) leaves it grown
        // for every later small transaction on this thread. Last on purpose, it would skew the scenarios after it.
        run("tick: 1 cell after a burst", v, grid, (b, i) -> {
            if (i == 0) for (final PLabel l : cells) for (int p = 0; p < 13; p++) l.setStyleProperty("p" + p, "v");
            cells.get(i % 240).setText("1." + (10_000 + i % 90_000));
        }, 20_000, 50_000);

        if ("heap".startsWith(only) || only.isEmpty()) heap(v);
    }
}
