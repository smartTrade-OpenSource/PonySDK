/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

/** What a {@link SessionAdmin} caller asks to do with a session, as checked by the {@link SessionAccessPolicy}. */
public enum SessionAction {

    /** See the session: listed by {@link SessionAdmin#sessions()}, readable by {@link SessionAdmin#session(int)}. */
    VIEW,
    /** Close its WebSocket as a network failure would: the client reconnects. */
    DROP,
    /** Refuse, or allow again, its reconnection attempts. */
    HOLD,
    /** Destroy it: the client is told and reloads. */
    DESTROY,
    /** Move it: destroyed as by DESTROY, its client carrying the state its application declares (ADR-0018). */
    MOVE
}
