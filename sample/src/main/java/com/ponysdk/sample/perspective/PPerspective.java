/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.perspective;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import jakarta.json.JsonObject;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.Conflator;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;

/**
 * A FINOS Perspective viewer (docs/decisions/0029-perspective.md): a grid or a chart over a table that lives in the
 * browser, in WebAssembly, where the user groups, pivots, sorts and filters it. The server feeds the table; the
 * library is loaded from the application's own files, by the page that has a viewer.
 * <pre>
 * final PPerspective blotter = PPerspective.builder("id", Column.integer("id"), Column.string("symbol"), Column.floating("bid", 5)).build();
 * panel.add(blotter);
 * blotter.replace(rows);                                      // the first load
 * feed.onQuote(q -&gt; blotter.update(Map.of("id", q.id(), "bid", q.bid()))); // any thread, conflated
 * </pre>
 * <h2>The wire</h2>
 * A flush of the conflator is one call, a typed array: the flags (bit 0: replace the table's rows), the time every
 * datetime of the batch is relative to (only when the table has a datetime column), the number of keys to remove and the
 * keys, the number of rows, then for each row its key, the mask of the columns it carries (bit <i>i</i> for the
 * <i>i</i>-th column that is not the key) and their values. A column left out of a row is not changed in the table.
 * A value is the smallest of byte, short, int and long the protocol writes: a float is a whole number of
 * 10<sup>-decimals</sup>, a datetime the milliseconds since the batch's time, a date a day number, a string an
 * entry of the string dictionary after its first use. Perspective itself takes rows as objects, so the terminal builds
 * those from the array.
 */
public final class PPerspective extends PAddOnComposite<PElement> {

    /** The types of Perspective's columns; an integer is 32 bits. */
    public enum Type {
        INTEGER, FLOAT, STRING, BOOLEAN, DATE, DATETIME
    }

    /** The colours of the viewer: Perspective's "Pro" theme, light or dark, or the page's colour scheme. */
    public enum Theme {
        AUTO, LIGHT, DARK
    }

    /**
     * A column of the table. {@code decimals} is that of a float, which travels as a whole number of 10<sup>-decimals</sup>
     * and is rounded to it.
     */
    public record Column(String name, Type type, int decimals) {

        public Column {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
            if (name.isEmpty()) throw new IllegalArgumentException("A column needs a name");
            if (decimals < 0 || decimals > 9) throw new IllegalArgumentException("Decimals are 0 to 9: " + decimals);
        }

        public static Column integer(final String name) {
            return new Column(name, Type.INTEGER, 0);
        }

        public static Column floating(final String name, final int decimals) {
            return new Column(name, Type.FLOAT, decimals);
        }

        public static Column string(final String name) {
            return new Column(name, Type.STRING, 0);
        }

        public static Column bool(final String name) {
            return new Column(name, Type.BOOLEAN, 0);
        }

        /** A day, as a {@link LocalDate}. */
        public static Column date(final String name) {
            return new Column(name, Type.DATE, 0);
        }

        /** A time, as an {@link Instant}, a {@link Date} or milliseconds since the epoch. */
        public static Column datetime(final String name) {
            return new Column(name, Type.DATETIME, 0);
        }
    }

    /** The columns, the key included: the mask of a row has a bit for each column but the key. */
    private static final int MAX_COLUMNS = 32;
    /** Values in one call: the terminal passes them to a function as arguments, which engines limit. */
    private static final int MAX_VALUES_PER_CALL = 16_384;
    private static final int REPLACE = 1;
    private static final Duration DEFAULT_INTERVAL = Duration.ofMillis(100);

    private final String index;
    private final Column key;
    private final Column[] cells;
    private final Map<String, Integer> cellOf = new HashMap<>();
    private final boolean hasDatetime;

    private final Object feed = new Object();
    /** Guarded by feed: what the next flush sends. */
    private LinkedHashMap<Object, Row> pending = new LinkedHashMap<>();
    private LinkedHashMap<Object, Row> spare = new LinkedHashMap<>();
    private LinkedHashSet<Object> removed = new LinkedHashSet<>();
    private boolean replace;

    private final Conflator<Integer, Boolean> conflator;
    private final LongAdder offered = new LongAdder();
    /** Written by the UI thread, read by anyone. */
    private volatile long applied;
    private volatile long flushes;

    private final List<Consumer<PerspectiveConfig>> configListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<JsonObject>> clickListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<JsonObject>> selectListeners = new CopyOnWriteArrayList<>();

    private PPerspective(final Builder builder) {
        super(Element.newDiv(), creationArguments(builder));
        index = builder.index;
        Column found = null;
        final List<Column> others = new ArrayList<>();
        for (final Column column : builder.columns) {
            if (column.name().equals(index)) found = column;
            else others.add(column);
        }
        key = found;
        cells = others.toArray(Column[]::new);
        for (int c = 0; c < cells.length; c++) cellOf.put(cells[c].name(), c);
        hasDatetime = builder.columns.stream().anyMatch(column -> column.type() == Type.DATETIME);
        conflator = Conflator.<Integer, Boolean> builder(UIContext.get(), (unused, ready) -> flush())
            .minInterval(builder.interval).build();
        setTerminalHandler(event -> onTerminalEvent(event.getData()));
    }

    /**
     * @param index the column that identifies a row: an update of a row with this key changes it, with a new key adds it
     * @param columns at most 32, the index among them
     */
    public static Builder builder(final String index, final Column... columns) {
        return new Builder(index, columns);
    }

    // region update
    /**
     * Changes a row, or adds it: {@code row} holds the index and the columns that changed, by name; the others are left
     * as they are. From any thread, without waiting: the changes of a row offered between two flushes merge, and one
     * batch goes to the browser per flush, at most every {@link Builder#interval(Duration) interval}.
     *
     * @return false once the viewer is destroyed: the row is dropped
     * @throws IllegalArgumentException for a row without its index, a column that is not in the table, or a value that
     *             is not of its column's type
     */
    public boolean update(final Map<String, ?> row) {
        final Row next = new Row(cells.length);
        Object rowKey = null;
        for (final Map.Entry<String, ?> entry : row.entrySet()) {
            if (entry.getKey().equals(index)) {
                rowKey = normalize(key, entry.getValue());
                continue;
            }
            final Integer cell = cellOf.get(entry.getKey());
            if (cell == null) throw new IllegalArgumentException("No column " + entry.getKey());
            next.set(cell, normalize(cells[cell], entry.getValue()));
        }
        if (rowKey == null) throw new IllegalArgumentException("A row needs its index, " + index);
        offered.increment();
        synchronized (feed) {
            final Row current = pending.get(rowKey);
            if (current == null) pending.put(rowKey, next);
            else current.merge(next);
        }
        return conflator.offer(0, Boolean.TRUE);
    }
    // endregion

    /**
     * Removes the rows of these keys, from any thread: a row updated again afterwards comes back with the columns of
     * that update only.
     *
     * @return false once the viewer is destroyed
     */
    public boolean remove(final Collection<?> keys) {
        final List<Object> normalized = new ArrayList<>(keys.size());
        for (final Object rowKey : keys) normalized.add(normalize(key, Objects.requireNonNull(rowKey, "a key")));
        synchronized (feed) {
            for (final Object rowKey : normalized) {
                pending.remove(rowKey);
                if (!replace) removed.add(rowKey);
            }
        }
        return conflator.offer(0, Boolean.TRUE);
    }

    /** Replaces every row of the table with these, from any thread: what was offered before is dropped. */
    public boolean replace(final Collection<? extends Map<String, ?>> rows) {
        final LinkedHashMap<Object, Row> next = new LinkedHashMap<>(Math.max(16, rows.size() * 4 / 3 + 1));
        for (final Map<String, ?> row : rows) {
            final Row cellsOfRow = new Row(cells.length);
            Object rowKey = null;
            for (final Map.Entry<String, ?> entry : row.entrySet()) {
                if (entry.getKey().equals(index)) {
                    rowKey = normalize(key, entry.getValue());
                    continue;
                }
                final Integer cell = cellOf.get(entry.getKey());
                if (cell == null) throw new IllegalArgumentException("No column " + entry.getKey());
                cellsOfRow.set(cell, normalize(cells[cell], entry.getValue()));
            }
            if (rowKey == null) throw new IllegalArgumentException("A row needs its index, " + index);
            final Row previous = next.putIfAbsent(rowKey, cellsOfRow);
            if (previous != null) previous.merge(cellsOfRow);
        }
        offered.add(rows.size());
        synchronized (feed) {
            replace = true;
            removed.clear();
            pending = next;
        }
        return conflator.offer(0, Boolean.TRUE);
    }

    /** Removes every row, from any thread. */
    public boolean clear() {
        synchronized (feed) {
            replace = true;
            removed.clear();
            pending.clear();
        }
        return conflator.offer(0, Boolean.TRUE);
    }

    /**
     * Shows this configuration: the fields that are set replace the viewer's, the others stay. The user's own changes
     * come back through {@link #addConfigListener}, this one does not. On the UI thread.
     */
    public void configure(final PerspectiveConfig configuration) {
        callTerminalMethod("configure", configuration.toJson().toString());
    }

    /** Back to what the table's columns give: the grid, every column, no grouping, sort or filter. On the UI thread. */
    public void reset() {
        callTerminalMethod("reset");
    }

    /** Called, on the UI thread, with the configuration after each change of the user's: a column dragged, a sort, a plugin. */
    public HandlerRegistration addConfigListener(final Consumer<PerspectiveConfig> listener) {
        return register(configListeners, listener);
    }

    /** Called, on the UI thread, with the row (as Perspective gives it: the columns shown) that the user clicked. */
    public HandlerRegistration addClickListener(final Consumer<JsonObject> listener) {
        return register(clickListeners, listener);
    }

    /** Called, on the UI thread, with the row the user selected, or null when the selection is cleared. */
    public HandlerRegistration addSelectListener(final Consumer<JsonObject> listener) {
        return register(selectListeners, listener);
    }

    /** The rows offered since the creation, by {@link #update} and {@link #replace}. */
    public long getOffered() {
        return offered.sum();
    }

    /** The rows sent to the browser since the creation: the others were merged with a later change of their key. */
    public long getApplied() {
        return applied;
    }

    /** The batches sent to the browser, one per flush. */
    public long getFlushes() {
        return flushes;
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

    private static <T> HandlerRegistration register(final List<Consumer<T>> listeners, final Consumer<T> listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    private void onTerminalEvent(final JsonObject event) {
        switch (event.getString("type", "")) {
            case "config" -> {
                final PerspectiveConfig changed = PerspectiveConfig.fromJson(event.getJsonObject("config"));
                configListeners.forEach(listener -> listener.accept(changed));
            }
            case "click" -> clickListeners.forEach(listener -> listener.accept(event.getJsonObject("row")));
            case "select" -> {
                final JsonObject row = event.getBoolean("selected", false) && event.get("row") instanceof final JsonObject json ? json : null;
                selectListeners.forEach(listener -> listener.accept(row));
            }
            default -> { // "ready": the viewer is built
            }
        }
    }

    /** One batch of what was offered since the last, on the UI thread. */
    private void flush() {
        final LinkedHashMap<Object, Row> rows;
        final LinkedHashSet<Object> gone;
        int flags;
        synchronized (feed) {
            rows = pending;
            pending = spare != null ? spare : new LinkedHashMap<>();
            spare = null;
            gone = removed;
            removed = new LinkedHashSet<>();
            flags = replace ? REPLACE : 0;
            replace = false;
        }
        try {
            if (rows.isEmpty() && gone.isEmpty() && flags == 0) return;
            final long base = hasDatetime ? System.currentTimeMillis() : 0;
            final Iterator<Object> keys = gone.iterator();
            final Iterator<Map.Entry<Object, Row>> entries = rows.entrySet().iterator();
            do {
                final List<Object> out = new ArrayList<>();
                out.add(flags);
                if (hasDatetime) out.add(base);
                flags = 0;
                int budget = MAX_VALUES_PER_CALL;
                final int removedAt = out.size();
                out.add(0);
                int removedCount = 0;
                while (keys.hasNext() && budget-- > 0) {
                    out.add(wire(key, keys.next(), base));
                    removedCount++;
                }
                out.set(removedAt, removedCount);
                final int rowsAt = out.size();
                out.add(0);
                int rowCount = 0;
                while (entries.hasNext() && budget > 0) {
                    final Map.Entry<Object, Row> entry = entries.next();
                    final Row row = entry.getValue();
                    out.add(wire(key, entry.getKey(), base));
                    out.add(row.mask);
                    budget -= 2;
                    for (int c = 0, mask = row.mask; mask != 0; c++, mask >>>= 1) {
                        if ((mask & 1) == 0) continue;
                        out.add(wire(cells[c], row.cells[c], base));
                        budget--;
                    }
                    rowCount++;
                }
                out.set(rowsAt, rowCount);
                callTerminalMethod("batch", out.toArray());
                applied += rowCount;
            } while (keys.hasNext() || entries.hasNext());
            flushes++;
        } finally {
            rows.clear();
            synchronized (feed) {
                spare = rows; // the two maps take turns: no allocation per flush
            }
        }
    }

    /** The value of a cell as it is sent: a datetime relative to the batch's time. */
    private static Object wire(final Column column, final Object value, final long base) {
        return column.type() == Type.DATETIME && value != null ? Long.valueOf((Long) value - base) : value;
    }

    /** A cell's value in the form it is stored and sent: null stays null (it sets the cell to null). */
    private static Object normalize(final Column column, final Object value) {
        if (value == null) return null;
        return switch (column.type()) {
            case INTEGER -> {
                if (!(value instanceof final Number number) || number.longValue() != number.doubleValue()
                    || number.longValue() < Integer.MIN_VALUE || number.longValue() > Integer.MAX_VALUE)
                    throw new IllegalArgumentException(column.name() + " is an integer of 32 bits: " + value);
                yield number.intValue();
            }
            case FLOAT -> {
                if (!(value instanceof final Number number) || !Double.isFinite(number.doubleValue()))
                    throw new IllegalArgumentException(column.name() + " is a finite number: " + value);
                final double units = number.doubleValue() * Math.pow(10, column.decimals());
                if (Math.abs(units) > (1L << 53)) throw new IllegalArgumentException(column.name() + " is out of range for "
                    + column.decimals() + " decimals: " + value);
                yield Math.round(units);
            }
            case STRING -> {
                if (!(value instanceof final CharSequence text)) throw new IllegalArgumentException(column.name() + " is a string: " + value);
                yield text.toString();
            }
            case BOOLEAN -> {
                if (!(value instanceof Boolean)) throw new IllegalArgumentException(column.name() + " is a boolean: " + value);
                yield value;
            }
            case DATE -> {
                if (!(value instanceof final LocalDate date)) throw new IllegalArgumentException(column.name() + " is a LocalDate: " + value);
                yield (int) date.toEpochDay();
            }
            case DATETIME -> switch (value) {
                case final Instant instant -> instant.toEpochMilli();
                case final Date date -> date.getTime();
                case final Number number -> number.longValue();
                default -> throw new IllegalArgumentException(column.name() + " is an Instant, a Date or milliseconds: " + value);
            };
        };
    }

    private static Object[] creationArguments(final Builder builder) {
        final Object[] args = new Object[5 + 3 * builder.columns.size()];
        args[0] = builder.index;
        args[1] = builder.theme.name().toLowerCase(Locale.ROOT);
        args[2] = builder.charts;
        args[3] = builder.config == null ? "" : builder.config.toJson().toString();
        args[4] = builder.columns.size();
        int at = 5;
        for (final Column column : builder.columns) {
            args[at++] = column.name();
            args[at++] = column.type().name().toLowerCase(Locale.ROOT);
            args[at++] = column.decimals();
        }
        return args;
    }

    /** What changed in one row: the columns it carries (a bit each) and their values. */
    private static final class Row {

        private final Object[] cells;
        private int mask;

        private Row(final int size) {
            cells = new Object[size];
        }

        private void set(final int cell, final Object value) {
            cells[cell] = value;
            mask |= 1 << cell;
        }

        /** A later change of the same row: its columns replace this one's. */
        private void merge(final Row later) {
            for (int c = 0, bits = later.mask; bits != 0; c++, bits >>>= 1) {
                if ((bits & 1) == 1) cells[c] = later.cells[c];
            }
            mask |= later.mask;
        }
    }

    public static final class Builder {

        private final String index;
        private final List<Column> columns;
        private Theme theme = Theme.AUTO;
        private boolean charts;
        private PerspectiveConfig config;
        private Duration interval = DEFAULT_INTERVAL;

        private Builder(final String index, final Column[] columns) {
            this.index = Objects.requireNonNull(index, "index");
            if (columns.length == 0 || columns.length > MAX_COLUMNS) throw new IllegalArgumentException("A table has 1 to " + MAX_COLUMNS + " columns");
            final Map<String, Column> byName = new LinkedHashMap<>();
            for (final Column column : columns) {
                Objects.requireNonNull(column, "a column");
                if (byName.put(column.name(), column) != null) throw new IllegalArgumentException("Two columns named " + column.name());
            }
            if (!byName.containsKey(index)) throw new IllegalArgumentException("The index " + index + " is not a column");
            this.columns = List.copyOf(byName.values());
        }

        /** The colours: the page's colour scheme by default. */
        public Builder theme(final Theme theme) {
            this.theme = Objects.requireNonNull(theme, "theme");
            return this;
        }

        /** Loads the chart plugins, so that the user (or {@link Builder#config}) can choose a chart: 285 KB more, and a worker. */
        public Builder charts(final boolean charts) {
            this.charts = charts;
            return this;
        }

        /** What the viewer shows at first. */
        public Builder config(final PerspectiveConfig config) {
            this.config = Objects.requireNonNull(config, "config");
            return this;
        }

        /** The least time between two batches, 100 ms by default. */
        public Builder interval(final Duration interval) {
            if (interval.isNegative()) throw new IllegalArgumentException("A negative interval: " + interval);
            this.interval = interval;
            return this;
        }

        public PPerspective build() {
            return new PPerspective(this);
        }
    }
}
