/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

/**
 * Decides, on every {@link SessionAdmin} call, whether a caller may act on a session (ADR-0001). Set with
 * {@link ApplicationConfiguration#setSessionAccessPolicy}; the caller's configuration applies.
 * <p>
 * The policy can read the caller's HTTP session and attributes, typically to check a role. It must be fast and must
 * not take a {@link UIContext} lock.
 */
@FunctionalInterface
public interface SessionAccessPolicy {

    /** The default: a caller acts only on the contexts of its own HTTP session, that is its own tabs and frames. */
    SessionAccessPolicy SAME_HTTP_SESSION = (caller, target, action) -> caller == target
            || caller.getApplication() != null && caller.getApplication() == target.getApplication();

    boolean allows(UIContext caller, UIContext target, SessionAction action);
}
