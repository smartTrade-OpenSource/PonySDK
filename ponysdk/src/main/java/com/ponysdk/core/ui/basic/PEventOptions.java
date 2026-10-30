/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
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
    public static final PEventOptions NONE = new PEventOptions(List.of(), 0, 0, false, false, false, false, PCommands.NONE);

    /** One factory for every JSON written: each Json.create...() looks the provider up again (20 µs). */
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Map.of());

    private final List<String> fields;
    private final long throttleMillis;
    private final long debounceMillis;
    private final boolean preventDefault;
    private final boolean stopPropagation;
    private final boolean capture;
    private final boolean lock;
    private final PCommands commands;

    private PEventOptions(final List<String> fields, final long throttleMillis, final long debounceMillis,
                          final boolean preventDefault, final boolean stopPropagation, final boolean capture,
                          final boolean lock, final PCommands commands) {
        this.fields = fields;
        this.throttleMillis = throttleMillis;
        this.debounceMillis = debounceMillis;
        this.preventDefault = preventDefault;
        this.stopPropagation = stopPropagation;
        this.capture = capture;
        this.lock = lock;
        this.commands = commands;
    }

    /** The fields read on each event. */
    public static PEventOptions fields(final String... fields) {
        return NONE.withFields(fields);
    }

    public PEventOptions withFields(final String... fields) {
        return new PEventOptions(List.of(fields), throttleMillis, debounceMillis, preventDefault, stopPropagation, capture, lock, commands);
    }

    /** At most one event per period: the first at once, the last of the period at its end. */
    public PEventOptions throttle(final Duration period) {
        return new PEventOptions(fields, period.toMillis(), debounceMillis, preventDefault, stopPropagation, capture, lock, commands);
    }

    /** The last event only, once none came for the delay. */
    public PEventOptions debounce(final Duration delay) {
        return new PEventOptions(fields, throttleMillis, delay.toMillis(), preventDefault, stopPropagation, capture, lock, commands);
    }

    /** The browser's default action is prevented: the listener cannot be passive. */
    public PEventOptions preventDefault() {
        return new PEventOptions(fields, throttleMillis, debounceMillis, true, stopPropagation, capture, lock, commands);
    }

    public PEventOptions stopPropagation() {
        return new PEventOptions(fields, throttleMillis, debounceMillis, preventDefault, true, capture, lock, commands);
    }

    /** Listens in the capture phase: the events of the widget's children come first here. */
    public PEventOptions capture() {
        return new PEventOptions(fields, throttleMillis, debounceMillis, preventDefault, stopPropagation, true, lock, commands);
    }

    /**
     * From an event until the server ran its listeners, the element is busy ({@code aria-busy}) and the terminal
     * sends no other event of this name: a second submit before the answer is dropped, not processed twice. A
     * {@link PFocusWidget}'s click does the same by default ({@link PFocusWidget#setEnabledOnRequest(boolean)}).
     */
    public PEventOptions lock() {
        return new PEventOptions(fields, throttleMillis, debounceMillis, preventDefault, stopPropagation, capture, true, commands);
    }

    /**
     * Commands the terminal runs on each event, in the browser and before the event is sent
     * (docs/decisions/0027-client-commands.md). They are not rate limited: only the sending is. Given again, they
     * replace the previous ones.
     */
    public PEventOptions commands(final PCommands commands) {
        Objects.requireNonNull(commands, "commands");
        return new PEventOptions(fields, throttleMillis, debounceMillis, preventDefault, stopPropagation, capture, lock, commands);
    }

    boolean hasCommands() {
        return !commands.isEmpty();
    }

    public boolean isLock() {
        return lock;
    }

    public List<String> getFields() {
        return fields;
    }

    /** The JSON of {@link com.ponysdk.core.model.ServerToClientModel#LISTEN_EVENT}: the name, then what is set. */
    String toJson(final String name) {
        return toJson(name, true);
    }

    /** With {@code send} false, the terminal runs the commands and sends nothing: no server listener wants the event. */
    String toJson(final String name, final boolean send) {
        final JsonObjectBuilder json = JSON.createObjectBuilder().add("name", name);
        if (!fields.isEmpty()) {
            final JsonArrayBuilder array = JSON.createArrayBuilder();
            fields.forEach(array::add);
            json.add("fields", array);
        }
        if (throttleMillis > 0) json.add("throttle", throttleMillis);
        if (debounceMillis > 0) json.add("debounce", debounceMillis);
        if (preventDefault) json.add("prevent", true);
        if (stopPropagation) json.add("stop", true);
        if (capture) json.add("capture", true);
        if (lock) json.add("lock", true);
        if (hasCommands()) json.add("commands", commands.toJson(JSON));
        if (!send) json.add("send", false);
        return json.build().toString();
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof PEventOptions other && fields.equals(other.fields) && throttleMillis == other.throttleMillis
                && debounceMillis == other.debounceMillis && preventDefault == other.preventDefault
                && stopPropagation == other.stopPropagation && capture == other.capture && lock == other.lock
                && commands.equals(other.commands);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fields, throttleMillis, debounceMillis, preventDefault, stopPropagation, capture, lock, commands);
    }
}
