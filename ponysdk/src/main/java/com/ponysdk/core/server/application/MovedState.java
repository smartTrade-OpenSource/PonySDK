/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ponysdk.core.model.ValueTypeModel;

/**
 * What a page carries to the next server when it moves (ADR-0018): names to values of the protocol's own types. The
 * application fills it in {@link UIContext#onMove}, the next server's context reads it from
 * {@link UIContext#getMovedState()} before its entry point starts.
 * <p>
 * Encoded as the protocol encodes a value: the type of {@link ValueTypeModel}, then the value; a name or a string as
 * its UTF-8 length then its bytes.
 */
public final class MovedState {

    private static final byte BYTES = (byte) ValueTypeModel.ARRAY.ordinal();

    private final Map<String, Object> values = new LinkedHashMap<>();

    public MovedState putBoolean(final String name, final boolean value) {
        values.put(name, value);
        return this;
    }

    public MovedState putInt(final String name, final int value) {
        values.put(name, value);
        return this;
    }

    public MovedState putLong(final String name, final long value) {
        values.put(name, value);
        return this;
    }

    public MovedState putDouble(final String name, final double value) {
        values.put(name, value);
        return this;
    }

    /** A null string is not carried. */
    public MovedState putString(final String name, final String value) {
        if (value == null) values.remove(name);
        else values.put(name, value);
        return this;
    }

    /** A null array is not carried. */
    public MovedState putBytes(final String name, final byte[] value) {
        if (value == null) values.remove(name);
        else values.put(name, value.clone());
        return this;
    }

    public boolean has(final String name) {
        return values.containsKey(name);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public boolean getBoolean(final String name, final boolean fallback) {
        return values.get(name) instanceof final Boolean b ? b : fallback;
    }

    public int getInt(final String name, final int fallback) {
        return values.get(name) instanceof final Integer i ? i : fallback;
    }

    public long getLong(final String name, final long fallback) {
        return values.get(name) instanceof final Long l ? l : fallback;
    }

    public double getDouble(final String name, final double fallback) {
        return values.get(name) instanceof final Double d ? d : fallback;
    }

    public String getString(final String name, final String fallback) {
        return values.get(name) instanceof final String s ? s : fallback;
    }

    public byte[] getBytes(final String name) {
        return values.get(name) instanceof final byte[] b ? b.clone() : null;
    }

    /** The value as it was put: a Boolean, Integer, Long, Double, String or byte[] (a copy); null when absent. */
    public Object get(final String name) {
        final Object value = values.get(name);
        return value instanceof final byte[] b ? b.clone() : value;
    }

    byte[] encode() {
        final ByteBuffer buffer = ByteBuffer.allocate(size());
        for (final Map.Entry<String, Object> entry : values.entrySet()) {
            putText(buffer, entry.getKey());
            switch (entry.getValue()) {
                case final Boolean b -> buffer.put((byte) ValueTypeModel.BOOLEAN.ordinal()).put((byte) (b ? 1 : 0));
                case final Integer i -> buffer.put((byte) ValueTypeModel.INTEGER.ordinal()).putInt(i);
                case final Long l -> buffer.put((byte) ValueTypeModel.LONG.ordinal()).putLong(l);
                case final Double d -> buffer.put((byte) ValueTypeModel.DOUBLE.ordinal()).putDouble(d);
                case final String s -> putText(buffer.put((byte) ValueTypeModel.STRING.ordinal()), s);
                default -> {
                    final byte[] bytes = (byte[]) entry.getValue();
                    buffer.put(BYTES).putInt(bytes.length).put(bytes);
                }
            }
        }
        return buffer.array();
    }

    /** @throws IllegalArgumentException for bytes that are not an encoded state */
    static MovedState decode(final ByteBuffer buffer) {
        final MovedState state = new MovedState();
        try {
            while (buffer.hasRemaining()) {
                final String name = getText(buffer);
                final ValueTypeModel type = ValueTypeModel.values()[buffer.get()];
                switch (type) {
                    case BOOLEAN -> state.values.put(name, buffer.get() != 0);
                    case INTEGER -> state.values.put(name, buffer.getInt());
                    case LONG -> state.values.put(name, buffer.getLong());
                    case DOUBLE -> state.values.put(name, buffer.getDouble());
                    case STRING -> state.values.put(name, getText(buffer));
                    case ARRAY -> {
                        final byte[] bytes = new byte[buffer.getInt()];
                        buffer.get(bytes);
                        state.values.put(name, bytes);
                    }
                    default -> throw new IllegalArgumentException("No value of type " + type + " in a moved state");
                }
            }
        } catch (final RuntimeException e) { // past the end, a negative length, an unknown type
            throw new IllegalArgumentException("Not a moved state", e);
        }
        return state;
    }

    private int size() {
        int size = 0;
        for (final Map.Entry<String, Object> entry : values.entrySet()) {
            size += textSize(entry.getKey()) + 1;
            size += switch (entry.getValue()) {
                case Boolean _ -> 1;
                case Integer _ -> 4;
                case Long _, Double _ -> 8;
                case final String s -> textSize(s);
                default -> 4 + ((byte[]) entry.getValue()).length;
            };
        }
        return size;
    }

    private static int textSize(final String text) {
        return 4 + text.getBytes(StandardCharsets.UTF_8).length;
    }

    private static void putText(final ByteBuffer buffer, final String text) {
        final byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(bytes.length).put(bytes);
    }

    private static String getText(final ByteBuffer buffer) {
        final byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
