/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.websocket;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * The WebSocket subprotocols an embedded terminal offers (docs/decisions/0009-embedding-limits.md): {@code ponysdk},
 * which the server accepts, and what a browser cannot put in a header of the upgrade otherwise, kept out of the URL
 * and so out of the access logs: its context's key on a reconnection, {@code ponysdk.key.<K>}, its host's credential
 * on a start, {@code ponysdk.credential.<base64url of its UTF-8>}.
 */
public final class Subprotocols {

    public static final String PONYSDK = "ponysdk";
    public static final String HEADER = "Sec-WebSocket-Protocol";
    private static final String KEY = "ponysdk.key.";
    private static final String CREDENTIAL = "ponysdk.credential.";

    private Subprotocols() {
    }

    /** Whether the header offers {@code ponysdk}. */
    public static boolean offered(final String header) {
        if (header == null) return false;
        for (final String protocol : header.split(",")) {
            if (PONYSDK.equals(protocol.trim())) return true;
        }
        return false;
    }

    /** The context key a reconnection offers, or null. */
    public static String key(final String header) {
        return value(header, KEY);
    }

    /** The host's credential, or null when none or not base64url. */
    public static String credential(final String header) {
        final String encoded = value(header, CREDENTIAL);
        if (encoded == null) return null;
        try {
            return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    private static String value(final String header, final String prefix) {
        if (header == null) return null;
        for (final String protocol : header.split(",")) {
            final String trimmed = protocol.trim();
            if (trimmed.startsWith(prefix) && trimmed.length() > prefix.length()) return trimmed.substring(prefix.length());
        }
        return null;
    }
}
