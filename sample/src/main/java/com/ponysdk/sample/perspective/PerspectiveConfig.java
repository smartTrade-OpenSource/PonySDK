/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.perspective;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * What a Perspective viewer shows of its table: the plugin (a grid or a chart), the grouping, the columns, the sort, the
 * filter, the aggregates and the plugin's own settings. A field left null is not changed when the configuration is applied
 * ({@link PPerspective#configure}); a user's change comes back with every field set.
 * <pre>
 * PerspectiveConfig.builder().plugin("Datagrid").groupBy("sector").columns("symbol", "pnl")
 *     .aggregate("pnl", "sum").sort("pnl", "desc").filter("sector", "==", "Banks").build();
 * </pre>
 * Perspective's names are used as they are: plugins ("Datagrid", "Y Line", "Y Bar", "Treemap"…), sort directions
 * ("asc", "desc"), filter operators ("==", "&gt;", "contains", "in"…), aggregates ("sum", "avg", "last"…).
 */
public record PerspectiveConfig(String plugin, List<String> groupBy, List<String> splitBy, List<String> columns, List<Sort> sort,
        List<Filter> filter, Map<String, String> aggregates, JsonObject pluginConfig) {

    /** A column and its direction. */
    public record Sort(String column, String direction) {
    }

    /** A column, an operator and the value it is compared to (a string, a number, a boolean, a list of them, or null). */
    public record Filter(String column, String operator, Object value) {
    }

    public PerspectiveConfig {
        groupBy = copy(groupBy);
        splitBy = copy(splitBy);
        columns = copy(columns);
        sort = copy(sort);
        filter = copy(filter);
        aggregates = aggregates == null ? null : Map.copyOf(aggregates);
    }

    private static <T> List<T> copy(final List<T> list) {
        return list == null ? null : java.util.Collections.unmodifiableList(new ArrayList<>(list)); // columns may hold a null
    }

    public static Builder builder() {
        return new Builder();
    }

    /** In Perspective's JSON, the fields that are set only. */
    public JsonObject toJson() {
        final JsonObjectBuilder json = Json.createObjectBuilder();
        if (plugin != null) json.add("plugin", plugin);
        if (groupBy != null) json.add("group_by", strings(groupBy));
        if (splitBy != null) json.add("split_by", strings(splitBy));
        if (columns != null) json.add("columns", strings(columns));
        if (sort != null) {
            final JsonArrayBuilder array = Json.createArrayBuilder();
            sort.forEach(s -> array.add(Json.createArrayBuilder().add(s.column()).add(s.direction())));
            json.add("sort", array);
        }
        if (filter != null) {
            final JsonArrayBuilder array = Json.createArrayBuilder();
            filter.forEach(f -> array.add(Json.createArrayBuilder().add(f.column()).add(f.operator()).add(value(f.value()))));
            json.add("filter", array);
        }
        if (aggregates != null) {
            final JsonObjectBuilder object = Json.createObjectBuilder();
            aggregates.forEach(object::add);
            json.add("aggregates", object);
        }
        if (pluginConfig != null) json.add("plugin_config", pluginConfig);
        return json.build();
    }

    /** The configuration a viewer reports: what {@link #toJson()} writes. An aggregate that is not a name is left out. */
    public static PerspectiveConfig fromJson(final JsonObject json) {
        List<Sort> sort = null;
        if (json.get("sort") instanceof final JsonArray array) {
            sort = new ArrayList<>(array.size());
            for (final JsonValue s : array) sort.add(new Sort(s.asJsonArray().getString(0), s.asJsonArray().getString(1)));
        }
        List<Filter> filter = null;
        if (json.get("filter") instanceof final JsonArray array) {
            filter = new ArrayList<>(array.size());
            for (final JsonValue f : array) {
                final JsonArray triple = f.asJsonArray();
                filter.add(new Filter(triple.getString(0), triple.getString(1), object(triple.get(2))));
            }
        }
        Map<String, String> aggregates = null;
        if (json.get("aggregates") instanceof final JsonObject object) {
            aggregates = new LinkedHashMap<>();
            for (final Map.Entry<String, JsonValue> entry : object.entrySet()) {
                if (entry.getValue() instanceof final JsonString name) aggregates.put(entry.getKey(), name.getString());
            }
        }
        return new PerspectiveConfig(json.getString("plugin", null), strings(json, "group_by"), strings(json, "split_by"),
            strings(json, "columns"), sort, filter, aggregates, json.get("plugin_config") instanceof final JsonObject settings ? settings : null);
    }

    private static List<String> strings(final JsonObject json, final String key) {
        if (!(json.get(key) instanceof final JsonArray array)) return null;
        final List<String> list = new ArrayList<>(array.size());
        for (final JsonValue value : array) list.add(value instanceof final JsonString string ? string.getString() : null);
        return list;
    }

    private static JsonArrayBuilder strings(final List<String> list) {
        final JsonArrayBuilder array = Json.createArrayBuilder();
        for (final String value : list) {
            if (value == null) array.addNull();
            else array.add(value);
        }
        return array;
    }

    private static JsonValue value(final Object value) {
        return switch (value) {
            case null -> JsonValue.NULL;
            case final String string -> Json.createValue(string);
            case final Boolean bool -> bool ? JsonValue.TRUE : JsonValue.FALSE;
            case final Integer number -> Json.createValue(number);
            case final Long number -> Json.createValue(number);
            case final Number number -> Json.createValue(number.doubleValue());
            case final List<?> list -> {
                final JsonArrayBuilder array = Json.createArrayBuilder();
                list.forEach(item -> array.add(value(item)));
                yield array.build();
            }
            default -> throw new IllegalArgumentException("A filter value is a string, a number, a boolean, a list or null: " + value);
        };
    }

    private static Object object(final JsonValue value) {
        return switch (value.getValueType()) {
            case STRING -> ((JsonString) value).getString();
            case NUMBER -> ((JsonNumber) value).isIntegral() ? (Object) ((JsonNumber) value).longValue() : ((JsonNumber) value).doubleValue();
            case TRUE -> Boolean.TRUE;
            case FALSE -> Boolean.FALSE;
            case ARRAY -> value.asJsonArray().stream().map(PerspectiveConfig::object).toList();
            default -> null;
        };
    }

    public static final class Builder {

        private String plugin;
        private List<String> groupBy;
        private List<String> splitBy;
        private List<String> columns;
        private List<Sort> sort;
        private List<Filter> filter;
        private Map<String, String> aggregates;
        private JsonObject pluginConfig;

        private Builder() {
        }

        /** The visualization: "Datagrid", or a chart: "Y Line", "Y Bar", "Treemap"… */
        public Builder plugin(final String plugin) {
            this.plugin = Objects.requireNonNull(plugin, "plugin");
            return this;
        }

        /** The columns the rows are grouped by, outermost first. */
        public Builder groupBy(final String... columns) {
            this.groupBy = List.of(columns);
            return this;
        }

        /** The columns whose values become columns of their own. */
        public Builder splitBy(final String... columns) {
            this.splitBy = List.of(columns);
            return this;
        }

        /** The columns shown, in order. */
        public Builder columns(final String... columns) {
            this.columns = List.of(columns);
            return this;
        }

        /** Adds a sort: the column and "asc" or "desc". */
        public Builder sort(final String column, final String direction) {
            if (sort == null) sort = new ArrayList<>();
            sort.add(new Sort(column, direction));
            return this;
        }

        /** Adds a filter: the column, an operator ("==", "&gt;", "contains"…) and the value; they all apply. */
        public Builder filter(final String column, final String operator, final Object value) {
            if (filter == null) filter = new ArrayList<>();
            filter.add(new Filter(column, operator, value));
            return this;
        }

        /** The aggregate of a column when rows are grouped: "sum", "avg", "last"… */
        public Builder aggregate(final String column, final String aggregate) {
            if (aggregates == null) aggregates = new LinkedHashMap<>();
            aggregates.put(column, aggregate);
            return this;
        }

        /** The plugin's own settings, as Perspective names them: {@code {"edit_mode": "SELECT_ROW"}} makes the grid report selections. */
        public Builder pluginConfig(final JsonObject pluginConfig) {
            this.pluginConfig = Objects.requireNonNull(pluginConfig, "pluginConfig");
            return this;
        }

        public PerspectiveConfig build() {
            return new PerspectiveConfig(plugin, groupBy, splitBy, columns, sort, filter, aggregates, pluginConfig);
        }
    }
}
