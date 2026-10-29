/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.chart;

import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.Conflator;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PElement;

/**
 * A time-series chart drawn by the terminal (docs/decisions/0026-chart.md): lines, areas or bars, one or several
 * series, a value axis scaled to the data, a time axis, a legend and a tooltip, the last points kept in a sliding
 * window. The terminal loads its drawing code when the first chart is created.
 * <pre>
 * final PChart chart = PChart.builder(PChart.Type.LINE, "EURUSD", "GBPUSD").title("Prices").decimals(5)
 *     .window(Duration.ofSeconds(60)).build();
 * panel.add(chart);
 * feed.onQuote(quote -&gt; chart.offer(quote.series(), quote.bid())); // any thread, conflated
 * chart.append(time, 1.0851, 1.2702);                                // the UI thread, a point at a given time
 * </pre>
 * A point is a time and one value per series. The values travel as a typed array: the time since the previous point,
 * then each series' change since the previous point, in units of 10<sup>-decimals</sup>: a value that moved a little
 * costs two bytes, one that did not move costs one.
 */
public final class PChart extends PAddOnComposite<PElement> {

    public enum Type {
        LINE, AREA, BAR
    }

    /** Values are sent as whole numbers of 10<sup>-decimals</sup>: the largest a double holds exactly. */
    private static final double MAX_UNITS = 1L << 53;
    private static final int DEFAULT_MAX_POINTS = 3_600;
    private static final Duration DEFAULT_INTERVAL = Duration.ofMillis(100);

    private final int series;
    private final double scale;
    /** The last point sent, which the next one is a change from: the time, and each series in units. */
    private long lastTime;
    private final long[] lastUnits;
    private final long[] nextUnits;
    /** Written by the UI thread, read by anyone. */
    private volatile long rows;

    private final Object feed = new Object();
    private final double[] latest;
    private final boolean[] fresh;
    private long latestTime;
    private final Conflator<Integer, Boolean> conflator;

    private PChart(final Builder builder) {
        super(Element.newDiv(), creationArguments(builder));
        series = builder.names.length;
        scale = Math.pow(10, builder.decimals);
        lastUnits = new long[series];
        nextUnits = new long[series];
        latest = new double[series];
        fresh = new boolean[series];
        conflator = Conflator.<Integer, Boolean> builder(UIContext.get(), (key, ready) -> flush())
            .minInterval(builder.interval).build();
    }

    public static Builder builder(final Type type, final String... series) {
        return new Builder(type, series);
    }

    /**
     * Adds a point: a time (milliseconds since the epoch, not before the previous point's) and one value per series.
     * A NaN leaves its series where it was: no value before its first, its previous value after. On the UI thread.
     *
     * @throws IllegalArgumentException for a time before the previous point's, a number of values other than the
     *             series', or an infinite value
     */
    public void append(final long timeMillis, final double... values) {
        if (values.length != series) throw new IllegalArgumentException(series + " series, " + values.length + " values");
        if (timeMillis < lastTime) throw new IllegalArgumentException("Time " + timeMillis + " is before the previous point's " + lastTime);
        final Object[] point = new Object[series + 1];
        point[0] = timeMillis - lastTime;
        for (int s = 0; s < series; s++) {
            final double value = values[s] * scale;
            if (Double.isNaN(value)) continue;
            if (Math.abs(value) > MAX_UNITS) throw new IllegalArgumentException("Value " + values[s] + " of series " + s + " is out of range");
            nextUnits[s] = Math.round(value);
            point[s + 1] = nextUnits[s] - lastUnits[s];
        }
        for (int s = 0; s < series; s++) if (point[s + 1] != null) lastUnits[s] = nextUnits[s];
        lastTime = timeMillis;
        callTerminalMethod("append", point);
        rows++;
    }

    /**
     * Offers the latest value of a series, from any thread, without waiting: the values offered between two flushes
     * of the chart replace each other, and one point goes to the browser per flush, at most every
     * {@link Builder#interval(Duration) interval}, with the time of the last offer. A series not offered since the
     * previous point keeps its value.
     *
     * @return false once the chart is destroyed: the value is dropped
     */
    public boolean offer(final int series, final double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Value " + value + " is not finite");
        Objects.checkIndex(series, latest.length);
        synchronized (feed) {
            latest[series] = value;
            fresh[series] = true;
            latestTime = System.currentTimeMillis();
        }
        return conflator.offer(0, Boolean.TRUE);
    }

    /** Removes every point: the next one starts the chart again. On the UI thread. */
    public void clear() {
        lastTime = 0;
        Arrays.fill(lastUnits, 0);
        synchronized (feed) {
            Arrays.fill(fresh, false);
        }
        callTerminalMethod("clear");
    }

    /** The points sent to the browser since the creation. */
    public long getRows() {
        return rows;
    }

    @Override
    public void destroy() {
        conflator.close();
        super.destroy();
    }

    @Override
    public void onDestroy() {
        conflator.close();
        super.onDestroy();
    }

    /** One point of the values offered since the last one, on the UI thread. */
    private void flush() {
        final double[] values = new double[series];
        final long time;
        synchronized (feed) {
            for (int s = 0; s < series; s++) {
                values[s] = fresh[s] ? latest[s] : Double.NaN;
                fresh[s] = false;
            }
            time = Math.max(latestTime, lastTime);
        }
        append(time, values);
    }

    private static Object[] creationArguments(final Builder builder) {
        final Object[] args = new Object[6 + builder.names.length];
        args[0] = builder.type.name().toLowerCase(Locale.ROOT);
        args[1] = builder.decimals;
        args[2] = builder.window.toMillis();
        args[3] = builder.maxPoints;
        args[4] = builder.title;
        args[5] = builder.tableRows;
        System.arraycopy(builder.names, 0, args, 6, builder.names.length);
        return args;
    }

    public static final class Builder {

        private final Type type;
        private final String[] names;
        private String title = "Chart";
        private int decimals = 2;
        private Duration window = Duration.ZERO;
        private int maxPoints = DEFAULT_MAX_POINTS;
        private Duration interval = DEFAULT_INTERVAL;
        private int tableRows;

        private Builder(final Type type, final String[] names) {
            this.type = Objects.requireNonNull(type, "type");
            if (names.length == 0) throw new IllegalArgumentException("A chart needs a series");
            for (final String name : names) Objects.requireNonNull(name, "a series' name");
            this.names = names.clone();
        }

        /** The chart's name for a screen reader, which reads it with the last values: "Prices: EURUSD 1.0851 at 10:32:05". */
        public Builder title(final String title) {
            this.title = Objects.requireNonNull(title, "title");
            return this;
        }

        /**
         * The decimals of the values, 0 to 9, 2 by default: what the axis and the tooltip show, and what the wire
         * carries, a value being rounded to it.
         */
        public Builder decimals(final int decimals) {
            if (decimals < 0 || decimals > 9) throw new IllegalArgumentException("Decimals are 0 to 9: " + decimals);
            this.decimals = decimals;
            return this;
        }

        /** Keeps the points of this long before the last one, the time axis showing that long; the last 3,600 points at most. */
        public Builder window(final Duration window) {
            if (window.isZero() || window.isNegative()) throw new IllegalArgumentException("A window is a positive duration: " + window);
            this.window = window;
            return this;
        }

        /** Keeps the last points only, 3,600 by default, 2 to 100,000. */
        public Builder maxPoints(final int maxPoints) {
            if (maxPoints < 2 || maxPoints > 100_000) throw new IllegalArgumentException("Max points are 2 to 100,000: " + maxPoints);
            this.maxPoints = maxPoints;
            return this;
        }

        /** The least time between two points {@link #offer} sends, 100 ms by default. */
        public Builder interval(final Duration interval) {
            if (interval.isNegative()) throw new IllegalArgumentException("A negative interval: " + interval);
            this.interval = interval;
            return this;
        }

        /** Gives a screen reader a table of the last rows, newest first, in addition to the chart's label; 0 (none) by default. */
        public Builder dataTable(final int rows) {
            if (rows < 0 || rows > 100) throw new IllegalArgumentException("Rows are 0 to 100: " + rows);
            this.tableRows = rows;
            return this;
        }

        public PChart build() {
            return new PChart(this);
        }
    }
}
