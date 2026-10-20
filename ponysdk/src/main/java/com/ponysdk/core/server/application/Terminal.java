/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

/**
 * The terminal the bootstrap page loads (docs/decisions/0002-javascript-terminal.md). Both speak the same protocol;
 * the GWT one goes away once the TypeScript one passes the behaviour oracle.
 */
public enum Terminal {

    /** {@code ponyterminal/ponyterminal.nocache.js}, compiled by GWT. The default. */
    GWT,

    /** {@code ponyterminal/terminal.js}, written in TypeScript on native elements. */
    TYPESCRIPT
}
