/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

/**
 * Read-only snapshot of a session, as {@link SessionAdmin} shows it. It never carries the HTTP session, whose id
 * would let the reader hijack it, nor the user, which belongs to the application (ADR-0001).
 *
 * @param id               the {@link UIContext} id
 * @param state            connected, suspended (its client is away) or destroyed
 * @param latencyMs        the last measured roundtrip latency
 * @param reconnectionHeld whether an administrator refuses its reconnection
 * @param suspension       while {@link State#SUSPENDED}, what the context records for its client; null otherwise
 */
public record SessionInfo(int id, State state, double latencyMs, boolean reconnectionHeld, Suspension suspension) {

    public enum State {
        CONNECTED, SUSPENDED, DESTROYED
    }

    /**
     * What a suspended context records until its client comes back.
     *
     * @param since      when the suspension started, epoch milliseconds
     * @param timeoutMs  the reconnection window: the context is destroyed when it expires
     * @param written    instructions written since the suspension started
     * @param held       instructions held now, once compacted: they are replayed when the client comes back
     * @param maxEntries the cap on held instructions: the context is destroyed beyond it
     */
    public record Suspension(long since, long timeoutMs, long written, int held, int maxEntries) {
    }
}
