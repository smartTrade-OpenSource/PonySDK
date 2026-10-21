/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic.event;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

/**
 * What the terminal sends of a DOM event listened to by name (docs/decisions/0003-protocol-v4-listeners.md): the
 * fields read on the event, and the rate it sends them at. Immutable: each method returns a copy.
 * <p>
 * A field is a path on the event: {@code key}, {@code clientX}, {@code deltaY}, {@code target.value},
 * {@code detail}. A value that is not a string, a number, a boolean or null comes as JSON text.
 * </p>
 */
public final class PEventOptions {

    /** No field, every event. */
    public static final PEventOptions NONE = new PEventOptions(List.of(), 0, 0, false, false, false);

    private final List<String> fields;
    private final long throttleMillis;
    private final long debounceMillis;
    private final boolean preventDefault;
    private final boolean stopPropagation;
    private final boolean capture;

    private PEventOptions(final List<String> fields, final long throttleMillis, final long debounceMillis,
                          final boolean preventDefault, final boolean stopPropagation, final boolean capture) {
        this.fields = fields;
        this.throttleMillis = throttleMillis;
        this.debounceMillis = debounceMillis;
        this.preventDefault = preventDefault;
        this.stopPropagation = stopPropagation;
        this.capture = capture;
    }

    /** The fields read on each event. */
    public static PEventOptions fields(final String... fields) {
        return NONE.withFields(fields);
    }

    public PEventOptions withFields(final String... fields) {
        return new PEventOptions(List.of(fields), throttleMillis, debounceMillis, preventDefault, stopPropagation, capture);
    }

    /** At most one event per period: the first at once, the last of the period at its end. */
    public PEventOptions throttle(final Duration period) {
        return new PEventOptions(fields, period.toMillis(), debounceMillis, preventDefault, stopPropagation, capture);
    }

    /** The last event only, once none came for the delay. */
    public PEventOptions debounce(final Duration delay) {
        return new PEventOptions(fields, throttleMillis, delay.toMillis(), preventDefault, stopPropagation, capture);
    }

    /** The browser's default action is prevented: the listener cannot be passive. */
    public PEventOptions preventDefault() {
        return new PEventOptions(fields, throttleMillis, debounceMillis, true, stopPropagation, capture);
    }

    public PEventOptions stopPropagation() {
        return new PEventOptions(fields, throttleMillis, debounceMillis, preventDefault, true, capture);
    }

    /** Listens in the capture phase: the events of the widget's children come first here. */
    public PEventOptions capture() {
        return new PEventOptions(fields, throttleMillis, debounceMillis, preventDefault, stopPropagation, true);
    }

    public List<String> getFields() {
        return fields;
    }

    /** The JSON of {@link com.ponysdk.core.model.ServerToClientModel#LISTEN_EVENT}: the name, then what is set. */
    public String toJson(final String name) {
        final JsonObjectBuilder json = Json.createObjectBuilder().add("name", name);
        if (!fields.isEmpty()) {
            final JsonArrayBuilder array = Json.createArrayBuilder();
            fields.forEach(array::add);
            json.add("fields", array);
        }
        if (throttleMillis > 0) json.add("throttle", throttleMillis);
        if (debounceMillis > 0) json.add("debounce", debounceMillis);
        if (preventDefault) json.add("prevent", true);
        if (stopPropagation) json.add("stop", true);
        if (capture) json.add("capture", true);
        return json.build().toString();
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof PEventOptions other && fields.equals(other.fields) && throttleMillis == other.throttleMillis
                && debounceMillis == other.debounceMillis && preventDefault == other.preventDefault
                && stopPropagation == other.stopPropagation && capture == other.capture;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fields, throttleMillis, debounceMillis, preventDefault, stopPropagation, capture);
    }
}
