/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * A DOM event listened to by name ({@link PWidget#addEventListener}): its name and the fields its
 * {@link PEventOptions} named, as the terminal read them.
 */
public final class PEvent {

    private final PWidget source;
    private final String name;
    private final JsonObject fields;

    PEvent(final PWidget source, final String name, final JsonObject fields) {
        this.source = source;
        this.name = name;
        this.fields = fields;
    }

    public PWidget getSource() {
        return source;
    }

    public String getName() {
        return name;
    }

    /** Every field read, by its path. */
    public JsonObject getFields() {
        return fields;
    }

    /** A string field, or the JSON text of any other value; null when absent or null. */
    public String getString(final String field) {
        final JsonValue value = fields.get(field);
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) return null;
        return value instanceof JsonString string ? string.getString() : value.toString();
    }

    /** A number field; null when absent or not a number. */
    public Double getNumber(final String field) {
        return fields.get(field) instanceof JsonNumber number ? number.doubleValue() : null;
    }

    /** A boolean field; null when absent or not a boolean. */
    public Boolean getBoolean(final String field) {
        final JsonValue value = fields.get(field);
        if (value == null) return null;
        return switch (value.getValueType()) {
            case TRUE -> Boolean.TRUE;
            case FALSE -> Boolean.FALSE;
            default -> null;
        };
    }

    @Override
    public String toString() {
        return "PEvent " + name + " " + fields;
    }
}
