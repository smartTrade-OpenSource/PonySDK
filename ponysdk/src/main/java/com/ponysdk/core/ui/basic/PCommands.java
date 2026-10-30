/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;

/**
 * What the terminal does to widgets when a DOM event happens, without asking the server
 * (docs/decisions/0027-client-commands.md). The commands are named operations, never code: they run in the order
 * declared, before the event is sent when it is. Immutable: each method returns a copy.
 * <p>
 * They change the browser's state only: the server's {@code isVisible()} or style names do not follow
 * ({@link PEventOptions#commands(PCommands)}). A target destroyed since is skipped.
 * </p>
 */
public final class PCommands {

    /** No command. */
    public static final PCommands NONE = new PCommands(List.of());

    /** What DOMTokenList accepts of a class: no empty name, no white space. */
    private static final Pattern TOKEN = Pattern.compile("\\S+");
    /** An attribute name the terminal sets without a parser: letters, digits and the usual punctuation. */
    private static final Pattern ATTRIBUTE = Pattern.compile("[A-Za-z_][-A-Za-z0-9_:.]*");

    private record Command(String operation, int target, String first, String second) {
    }

    private final List<Command> commands;

    private PCommands(final List<Command> commands) {
        this.commands = commands;
    }

    /** The target is shown, as {@link PWidget#setVisible(boolean) setVisible(true)} does. */
    public PCommands show(final IsPWidget target) {
        return with("show", target, null, null);
    }

    /** The target is hidden, as {@link PWidget#setVisible(boolean) setVisible(false)} does. */
    public PCommands hide(final IsPWidget target) {
        return with("hide", target, null, null);
    }

    /** The target is hidden if it shows, shown if it is hidden, in the browser's state. */
    public PCommands toggle(final IsPWidget target) {
        return with("toggle", target, null, null);
    }

    public PCommands addClass(final IsPWidget target, final String name) {
        return with("addClass", target, token(name), null);
    }

    /** The widget's own type class ({@code pony-Button}) stays. */
    public PCommands removeClass(final IsPWidget target, final String name) {
        return with("removeClass", target, token(name), null);
    }

    public PCommands toggleClass(final IsPWidget target, final String name) {
        return with("toggleClass", target, token(name), null);
    }

    /**
     * The value is text: nothing evaluates it. The name is not an event handler's ({@code onclick}...: any name
     * starting with {@code on}), which a page without 'unsafe-inline' would refuse anyway.
     */
    public PCommands setAttribute(final IsPWidget target, final String name, final String value) {
        Objects.requireNonNull(value, "value");
        return with("setAttribute", target, attribute(name), value);
    }

    public PCommands removeAttribute(final IsPWidget target, final String name) {
        return with("removeAttribute", target, attribute(name), null);
    }

    /** The target takes the focus: its field, for a widget that holds one. */
    public PCommands focus(final IsPWidget target) {
        return with("focus", target, null, null);
    }

    /** A {@code CustomEvent} of this name, bubbling, fired on the target: its listeners by name (ADR-0003) run. */
    public PCommands dispatch(final IsPWidget target, final String eventName) {
        return with("dispatch", target, token(eventName), null);
    }

    public boolean isEmpty() {
        return commands.isEmpty();
    }

    private PCommands with(final String operation, final IsPWidget target, final String first, final String second) {
        Objects.requireNonNull(target, "target");
        final List<Command> all = new ArrayList<>(commands.size() + 1);
        all.addAll(commands);
        all.add(new Command(operation, target.asWidget().getID(), first, second));
        return new PCommands(List.copyOf(all));
    }

    private static String token(final String name) {
        Objects.requireNonNull(name, "name");
        if (!TOKEN.matcher(name).matches()) throw new IllegalArgumentException("'" + name + "' is not a name: empty, or with white space");
        return name;
    }

    private static String attribute(final String name) {
        Objects.requireNonNull(name, "name");
        if (!ATTRIBUTE.matcher(name).matches()) throw new IllegalArgumentException("'" + name + "' is not an attribute name");
        if (name.toLowerCase(Locale.ROOT).startsWith("on"))
            throw new IllegalArgumentException("'" + name + "' is an event handler: commands run named operations, not code");
        return name;
    }

    /** The commands in LISTEN_EVENT's JSON: [operation, target id, arguments...] each, in order. */
    JsonArrayBuilder toJson(final JsonBuilderFactory json) {
        final JsonArrayBuilder out = json.createArrayBuilder();
        for (final Command command : commands) {
            final JsonArrayBuilder one = json.createArrayBuilder().add(command.operation).add(command.target);
            if (command.first != null) one.add(command.first);
            if (command.second != null) one.add(command.second);
            out.add(one);
        }
        return out;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof PCommands other && commands.equals(other.commands);
    }

    @Override
    public int hashCode() {
        return commands.hashCode();
    }
}
