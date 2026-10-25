/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * The keys of an embedded terminal's context (docs/decisions/0009-embedding-limits.md), what proves the context when
 * the session cookie does not come: a page of another site. K proves it in a header or a WebSocket subprotocol; S
 * only for a pending stream fetched by its URL, which takes no header. 128 random bits each.
 */
public final class ContextKeys {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final byte[] key;
    private final byte[] streamKey;

    private ContextKeys(final byte[] key, final byte[] streamKey) {
        this.key = key;
        this.streamKey = streamKey;
    }

    public static ContextKeys create() {
        return new ContextKeys(random(), random());
    }

    private static byte[] random() {
        final byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return ENCODER.encode(bytes);
    }

    /** K, in constant time. */
    public boolean isKey(final String candidate) {
        return matches(key, candidate);
    }

    /** S, in constant time. */
    public boolean isStreamKey(final String candidate) {
        return matches(streamKey, candidate);
    }

    private static boolean matches(final byte[] expected, final String candidate) {
        return candidate != null && MessageDigest.isEqual(expected, candidate.getBytes(StandardCharsets.US_ASCII));
    }

    /** As CONTEXT_KEYS carries them: K.S (base64url has no dot). */
    public String encoded() {
        return new String(key, StandardCharsets.US_ASCII) + '.' + new String(streamKey, StandardCharsets.US_ASCII);
    }
}
