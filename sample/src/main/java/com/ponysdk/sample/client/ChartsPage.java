/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.client;

import static com.ponysdk.sample.client.Ui.*;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.chart.PChart;

/**
 * Charts (docs/decisions/0026-chart.md): the terminal draws what the server sends as typed arrays. A multi-series line
 * fed by a thread at 1,000 ticks a second and conflated, an area and bars fed from the UI thread.
 */
final class ChartsPage {

    private static final String[] PAIRS = { "EURUSD", "GBPUSD", "AUDUSD", "USDCAD" };
    private static final int[] RATES = { 1_000, 10_000 };

    private ChartsPage() {
    }

    static PFlowPanel build(final UIContext uiContext, final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Charts", "Lines, areas and bars drawn by the terminal on a canvas, from typed arrays: a point costs a few "
            + "bytes, and a feed of 1,000 ticks a second sends ten points a second. The drawing code is loaded when the first "
            + "chart of a page is created.");

        final AtomicBoolean running = new AtomicBoolean(true);

        // region prices
        // Four rebased prices over the last 60 seconds, a point at most every 100 ms: the ticks of the feed in between
        // replace each other, so the browser gets ten points a second whatever the feed's rate
        final PChart prices = PChart.builder(PChart.Type.LINE, PAIRS).title("Rebased prices, start = 100")
            .decimals(3).window(Duration.ofSeconds(60)).dataTable(10).build();
        // The feed's thread offers each tick; offer() never blocks, whatever the browser's pace
        final AtomicInteger rate = new AtomicInteger(RATES[0]);
        final AtomicLong ticks = new AtomicLong();
        Thread.ofVirtual().name("showcase-chart-feed").start(() -> {
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            final double[] level = { 100, 100, 100, 100 };
            long last = System.nanoTime();
            while (running.get()) {
                final long now = System.nanoTime();
                final long count = rate.get() * (now - last) / 1_000_000_000L; // the rate, whatever the sleep took
                last = now;
                for (long i = 0; i < count; i++) {
                    final int pair = random.nextInt(level.length);
                    level[pair] += random.nextGaussian() * 0.01 + (100 - level[pair]) * 0.0005;
                    if (!prices.offer(pair, level[pair])) return; // the chart is destroyed
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
        prices.asWidget().addStyleName("showcase-chart");
        prices.asWidget().setHeight("320px");

        final PFlowPanel rates = Element.newPFlowPanel();
        rates.addStyleName("stream-rates");
        final PButton[] rateButtons = new PButton[RATES.length];
        for (int r = 0; r < RATES.length; r++) {
            final int value = RATES[r];
            final PButton button = Element.newPButton(String.format(Locale.US, "%,d ticks/s", value));
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
        final PButton clear = Element.newPButton("Clear");
        clear.addStyleName("stream-rate");
        clear.addClickHandler(e -> prices.clear());
        rates.add(clear);

        final PElement ticksVal = el("perf-value", "—");
        final PElement pointsVal = el("perf-value", "—");
        final PElement wireVal = el("perf-value", "—");
        final PElement perPointVal = el("perf-value", "—");
        final PFlowPanel counters = Element.newPFlowPanel();
        counters.addStyleName("perf-grid");
        counters.add(perfCard(ticksVal, el("perf-unit", "ticks/s"), el("perf-label", "Offered by the feed")),
            perfCard(pointsVal, el("perf-unit", "points/s"), el("perf-label", "Sent to the chart")),
            perfCard(wireVal, el("perf-unit", "KB/s"), el("perf-label", "On the wire, whole page")),
            perfCard(perPointVal, el("perf-unit", "B per point"), el("perf-label", "4 series, framing included")));

        final PFlowPanel main = Element.newPFlowPanel();
        main.addStyleName("spark-card");
        main.add(el("spark-label", "Live prices, last 60 seconds"), prices.asWidget());
        p.add(rates, counters, main);
        p.add(codeToggle("the live chart and its feed", Source.code(ChartsPage.class, "prices")));

        final PChart pnl = area(schedulers);
        final PChart flow = bars(schedulers);
        p.addDestroyListener(event -> { // the page left: the feed stops, and the browser frees its charts
            running.set(false);
            prices.destroy();
            pnl.destroy();
            flow.destroy();
        });
        pnl.asWidget().setHeight("220px");
        flow.asWidget().setHeight("220px");

        final PFlowPanel grid = Element.newPFlowPanel();
        grid.addStyleName("chart-grid");
        final PFlowPanel areaCard = Element.newPFlowPanel();
        areaCard.addStyleName("spark-card");
        areaCard.add(el("spark-label", "Area: desk P&L, last 2 minutes"), pnl.asWidget());
        final PFlowPanel barCard = Element.newPFlowPanel();
        barCard.addStyleName("spark-card");
        barCard.add(el("spark-label", "Bars: executions per second, last 60 seconds"), flow.asWidget());
        grid.add(areaCard, barCard);
        p.add(sectionTitle("Area and bars"), grid);
        p.add(codeToggle("the area chart", Source.code(ChartsPage.class, "area")));
        p.add(codeToggle("the bar chart", Source.code(ChartsPage.class, "bars")));

        final long[] previous = { 0, 0, 0 };
        schedulers.add(PScheduler.scheduleAtFixedRate(() -> {
            if (idle()) return;
            final long offered = ticks.get();
            final long rows = prices.getRows();
            final long wire = uiContext.getWireBytesSent();
            final long ticksPerSecond = offered - previous[0];
            final long pointsPerSecond = rows - previous[1];
            final long wirePerSecond = wire - previous[2];
            previous[0] = offered;
            previous[1] = rows;
            previous[2] = wire;
            ticksVal.setInnerText(String.format(Locale.US, "%,d", ticksPerSecond));
            pointsVal.setInnerText(String.valueOf(pointsPerSecond));
            if (wire >= 0 && wirePerSecond >= 0) {
                wireVal.setInnerText(String.format(Locale.US, "%.1f", wirePerSecond / 1024.0));
                if (pointsPerSecond > 0) perPointVal.setInnerText(String.format(Locale.US, "%.0f", (double) wirePerSecond / pointsPerSecond));
            }
        }, Duration.ofSeconds(1)));
        return p;
    }

    private static PChart area(final List<PScheduler.UIRunnable> schedulers) {
        // region area
        // A point at a given time, from the UI thread: the P&L of a desk over the last two minutes
        final PChart pnl = PChart.builder(PChart.Type.AREA, "P&L").title("Desk P&L, EUR").decimals(0)
            .window(Duration.ofMinutes(2)).build();
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final double[] value = { 0 };
        final long now = System.currentTimeMillis();
        for (int i = 120; i > 0; i--) { // the history
            value[0] += random.nextGaussian() * 800;
            pnl.append(now - i * 1_000L, value[0]);
        }
        schedulers.add(PScheduler.scheduleAtFixedRate(() -> {
            if (idle()) return;
            value[0] += ThreadLocalRandom.current().nextGaussian() * 800;
            pnl.append(System.currentTimeMillis(), value[0]);
        }, Duration.ofSeconds(1)));
        return pnl;
        // endregion
    }

    private static PChart bars(final List<PScheduler.UIRunnable> schedulers) {
        // region bars
        // Bars, two series side by side, a point a second
        final PChart flow = PChart.builder(PChart.Type.BAR, "Buys", "Sells").title("Executions per second")
            .decimals(0).window(Duration.ofSeconds(60)).build();
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final long now = System.currentTimeMillis();
        for (int i = 60; i > 0; i--) flow.append(now - i * 1_000L, random.nextInt(40), random.nextInt(40));
        schedulers.add(PScheduler.scheduleAtFixedRate(() -> {
            if (idle()) return;
            final ThreadLocalRandom ticker = ThreadLocalRandom.current();
            flow.append(System.currentTimeMillis(), ticker.nextInt(40), ticker.nextInt(40));
        }, Duration.ofSeconds(1)));
        return flow;
        // endregion
    }
}
