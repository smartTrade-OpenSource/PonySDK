/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.client;

import static com.ponysdk.sample.client.Ui.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.json.Json;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.fdc3.Fdc3;
import com.ponysdk.sample.perspective.PPerspective;
import com.ponysdk.sample.perspective.PPerspective.Column;
import com.ponysdk.sample.perspective.PerspectiveConfig;

/**
 * FINOS Perspective (docs/decisions/0029-perspective.md): a blotter of 5,000 rows that a feed thread updates 1,000 times
 * a second, grouped, sorted, filtered and charted in the browser, in WebAssembly. The server sends each update as a few
 * bytes of a typed array, conflated per row and per flush.
 */
final class PerspectivePage {

    private static final int ROWS = 5_000;
    private static final int[] RATES = { 100, 1_000, 5_000 };
    private static final String[] SECTORS = { "Banks", "Energy", "Health", "Industrials", "Insurance", "Materials", "Retail", "Software",
            "Telecom", "Utilities" };
    private static final String[] CURRENCIES = { "EUR", "USD", "GBP", "CHF" };

    private PerspectivePage() {
    }

    static PFlowPanel build(final UIContext uiContext, final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Perspective", "FINOS Perspective, a query engine in WebAssembly with a grid and charts: the rows live in the "
            + "browser, where the user groups, pivots, sorts and filters them. The server feeds the table with typed arrays, "
            + "conflated per row. The library (0.8 MB of JavaScript, 4.2 MB of WebAssembly) is served by this application and "
            + "loaded by this page only.");

        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicInteger rate = new AtomicInteger(RATES[1]);

        // region blotter
        // The grid reports the row the user selects (a click selects it); Perspective calls that edit mode SELECT_ROW
        final PerspectiveConfig rowsSelectable = PerspectiveConfig.builder().plugin("Datagrid")
            .pluginConfig(Json.createObjectBuilder().add("edit_mode", "SELECT_ROW").build()).build();
        // The table: its columns, and the one that identifies a row. A float travels as a whole number of its decimals
        final PPerspective blotter = PPerspective.builder("id", Column.integer("id"), Column.string("symbol"),
            Column.string("sector"), Column.string("currency"), Column.floating("bid", 4), Column.floating("ask", 4),
            Column.floating("last", 4), Column.integer("volume"), Column.floating("pnl", 2), Column.datetime("updated"),
            Column.date("tradeDate")).charts(true).config(rowsSelectable).build();
        blotter.asWidget().setHeight("540px");
        // The first load: every row at once
        final double[] mid = new double[ROWS];
        final List<Map<String, Object>> rows = new ArrayList<>(ROWS);
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < ROWS; i++) {
            mid[i] = 20 + random.nextDouble() * 180;
            final Map<String, Object> row = new HashMap<>(16);
            row.put("id", i);
            row.put("symbol", symbol(i));
            row.put("sector", SECTORS[i % SECTORS.length]);
            row.put("currency", CURRENCIES[i % CURRENCIES.length]);
            row.put("bid", mid[i] - 0.01);
            row.put("ask", mid[i] + 0.01);
            row.put("last", mid[i]);
            row.put("volume", random.nextInt(1_000, 500_000));
            row.put("pnl", random.nextGaussian() * 5_000);
            row.put("updated", Instant.now());
            row.put("tradeDate", LocalDate.now().minusDays(i % 5));
            rows.add(row);
        }
        blotter.replace(rows);
        // endregion

        // region feed
        // A thread of the application: each tick moves one row and offers the columns that changed. update() never
        // blocks and may be called from any thread; the ticks of a row between two flushes merge into one
        final AtomicLong ticks = new AtomicLong();
        Thread.ofVirtual().name("showcase-perspective-feed").start(() -> {
            final ThreadLocalRandom feed = ThreadLocalRandom.current();
            long last = System.nanoTime();
            while (running.get()) {
                final long now = System.nanoTime();
                final long perSecond = rate.get();
                final long count = perSecond * (now - last) / 1_000_000_000L; // the rate, whatever the sleep took
                last += count * 1_000_000_000L / perSecond; // what is not yet a whole tick stays for the next turn
                for (long i = 0; i < count; i++) {
                    final int id = feed.nextInt(ROWS);
                    mid[id] = Math.max(1, mid[id] + feed.nextGaussian() * 0.02);
                    final double spread = 0.005 + feed.nextDouble() * 0.01;
                    final Map<String, Object> change = new HashMap<>(8);
                    change.put("id", id);
                    change.put("bid", mid[id] - spread);
                    change.put("ask", mid[id] + spread);
                    change.put("last", mid[id]);
                    change.put("updated", Instant.now());
                    if (feed.nextInt(3) == 0) change.put("volume", feed.nextInt(1_000, 500_000));
                    if (feed.nextBoolean()) change.put("pnl", feed.nextGaussian() * 5_000);
                    if (!blotter.update(change)) return; // the viewer is destroyed
                }
                ticks.addAndGet(count);
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (final InterruptedException e) {
                    return;
                }
            }
        });
        // endregion

        // region config
        // The viewer's configuration from Java: a pivot, and a chart of the same pivot; the user changes it as well
        final PerspectiveConfig pivot = PerspectiveConfig.builder().plugin("Datagrid").groupBy("sector", "currency")
            .columns("bid", "volume", "pnl").aggregate("bid", "avg").aggregate("volume", "sum").aggregate("pnl", "sum")
            .sort("pnl", "desc").build();
        final PerspectiveConfig chart = PerspectiveConfig.builder().plugin("Y Bar").groupBy("sector").columns("pnl")
            .aggregate("pnl", "sum").build();
        // endregion

        final PFlowPanel views = Element.newPFlowPanel();
        views.addStyleName("stream-rates");
        views.add(viewButton("Blotter", () -> {
            blotter.reset();
            blotter.configure(rowsSelectable);
        }), viewButton("Pivot by sector and currency", () -> blotter.configure(pivot)),
            viewButton("Chart: P&L by sector", () -> blotter.configure(chart)));

        final PFlowPanel rates = Element.newPFlowPanel();
        rates.addStyleName("stream-rates");
        final PButton[] rateButtons = new PButton[RATES.length];
        for (int r = 0; r < RATES.length; r++) {
            final int value = RATES[r];
            final PButton button = Element.newPButton(String.format(Locale.US, "%,d updates/s", value));
            button.addStyleName("stream-rate");
            if (value == rate.get()) button.addStyleName("stream-rate-on");
            button.addClickHandler(e -> {
                rate.set(value);
                for (final PButton b : rateButtons) b.removeStyleName("stream-rate-on");
                button.addStyleName("stream-rate-on");
            });
            rateButtons[r] = button;
            rates.add(button);
        }

        final PElement offeredVal = el("perf-value", "—");
        final PElement appliedVal = el("perf-value", "—");
        final PElement flushVal = el("perf-value", "—");
        final PElement wireVal = el("perf-value", "—");
        final PElement perRowVal = el("perf-value", "—");
        final PFlowPanel counters = Element.newPFlowPanel();
        counters.addStyleName("perf-grid");
        counters.add(perfCard(offeredVal, el("perf-unit", "updates/s"), el("perf-label", "Offered by the feed")),
            perfCard(appliedVal, el("perf-unit", "rows/s"), el("perf-label", "Sent after conflation")),
            perfCard(flushVal, el("perf-unit", "batches/s"), el("perf-label", "One per flush")),
            perfCard(wireVal, el("perf-unit", "KB/s"), el("perf-label", "On the wire, whole page")),
            perfCard(perRowVal, el("perf-unit", "B per row"), el("perf-label", "Framing included")));

        // region events
        // What the user does comes back: the configuration after each change, and the row clicked. A click on a row
        // broadcasts its instrument to the other applications of the desktop (FDC3, ADR-0014)
        final PElement changed = el("fdc3-received", "Change the grouping, a sort or the plugin in the viewer: the configuration comes back here.");
        final PElement clicked = el("fdc3-received", "Click a row: its instrument is broadcast on the FDC3 channel.");
        final PElement selected = el("fdc3-received", "Nothing selected.");
        final Fdc3 fdc3 = Fdc3.get();
        final List<HandlerRegistration> registrations = new ArrayList<>();
        registrations.add(fdc3.addStateListener(state -> {
            if (state == Fdc3.State.READY && fdc3.getCurrentChannel() == null) fdc3.joinUserChannel("fdc3.channel.1");
        }));
        blotter.addConfigListener(config -> changed.setInnerText("Configuration: " + config.toJson()));
        blotter.addClickListener(row -> {
            if (!row.containsKey("symbol")) return; // a group's row, or a chart: no instrument
            final String ticker = row.getString("symbol");
            fdc3.broadcast(Fdc3.instrument(ticker, "Instrument " + ticker));
            clicked.setInnerText("Clicked " + ticker + ": broadcast on the FDC3 channel");
        });
        blotter.addSelectListener(row -> selected.setInnerText(row == null ? "Nothing selected." : "Selected: " + row));
        // endregion

        final PFlowPanel card = Element.newPFlowPanel();
        card.addStyleName("spark-card");
        card.add(el("spark-label", "Live blotter, " + String.format(Locale.US, "%,d", ROWS) + " rows"), blotter.asWidget());
        p.add(views, rates, counters, card, changed, clicked, selected);
        p.add(codeToggle("the table and its first load", Source.code(PerspectivePage.class, "blotter")));
        p.add(codeToggle("the feed, from its own thread", Source.code(PerspectivePage.class, "feed")));
        p.add(codeToggle("the viewer's configuration from Java", Source.code(PerspectivePage.class, "config")));
        p.add(codeToggle("what the user does, back on the server", Source.code(PerspectivePage.class, "events")));
        p.add(codeToggle("update(): validated on the caller's thread, merged per row", Source.code(PPerspective.class, "update")));

        p.addDestroyListener(event -> { // the page left: the feed stops, and the browser frees the table
            running.set(false);
            registrations.forEach(HandlerRegistration::removeHandler);
            blotter.destroy();
        });

        final long[] previous = { 0, 0, 0, 0 };
        schedulers.add(PScheduler.scheduleAtFixedRate(() -> {
            if (idle()) return;
            final long offered = ticks.get();
            final long rowsSent = blotter.getApplied();
            final long batches = blotter.getFlushes();
            final long wire = uiContext.getWireBytesSent();
            final long rowsPerSecond = rowsSent - previous[1];
            final long wirePerSecond = wire - previous[3];
            offeredVal.setInnerText(String.format(Locale.US, "%,d", offered - previous[0]));
            appliedVal.setInnerText(String.format(Locale.US, "%,d", rowsPerSecond));
            flushVal.setInnerText(String.valueOf(batches - previous[2]));
            if (wire >= 0 && wirePerSecond >= 0) {
                wireVal.setInnerText(String.format(Locale.US, "%.1f", wirePerSecond / 1024.0));
                if (rowsPerSecond > 0) perRowVal.setInnerText(String.format(Locale.US, "%.0f", (double) wirePerSecond / rowsPerSecond));
            }
            previous[0] = offered;
            previous[1] = rowsSent;
            previous[2] = batches;
            previous[3] = wire;
        }, Duration.ofSeconds(1)));
        return p;
    }

    private static PButton viewButton(final String label, final Runnable action) {
        final PButton button = Element.newPButton(label);
        button.addStyleName("stream-rate");
        button.addClickHandler(e -> action.run());
        return button;
    }

    /** A ticker of four letters, from the row's number: AAAA, AAAB… */
    private static String symbol(final int i) {
        final char[] letters = new char[4];
        for (int c = 3, n = i; c >= 0; c--, n /= 26) letters[c] = (char) ('A' + n % 26);
        return new String(letters);
    }
}
