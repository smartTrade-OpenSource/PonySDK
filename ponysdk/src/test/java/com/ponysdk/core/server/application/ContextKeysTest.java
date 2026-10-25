/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** {@link ContextKeys}: what proves an embedded terminal's context without the session cookie (ADR-0009). */
public class ContextKeysTest {

    @Test
    public void twoKeysOf128BitsInBase64Url() {
        final String[] keys = ContextKeys.create().encoded().split("\\.");
        assertEquals(2, keys.length, "K.S");
        for (final String key : keys) assertTrue(key.matches("[A-Za-z0-9_-]{22}"), key);
        assertNotEquals(keys[0], keys[1]);
    }

    @Test
    public void eachProvesWhatItIsFor() {
        final ContextKeys keys = ContextKeys.create();
        final String[] values = keys.encoded().split("\\.");
        assertTrue(keys.isKey(values[0]));
        assertTrue(keys.isStreamKey(values[1]));
        assertFalse(keys.isKey(values[1]), "the stream key of a URL is not the key");
        assertFalse(keys.isStreamKey(values[0]));
        assertFalse(keys.isKey(null));
        assertFalse(keys.isKey(values[0].substring(1)));
        assertFalse(ContextKeys.create().isKey(values[0]), "another context's");
    }
}
