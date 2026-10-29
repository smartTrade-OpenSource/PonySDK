/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.websocket;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

/** {@link Subprotocols}: what an embedded terminal offers in the upgrade, out of the URL (ADR-0009). */
public class SubprotocolsTest {

    @Test
    public void theHeaderIsAListOfProtocols() {
        final String header = "ponysdk, ponysdk.key.K3y-_x, other";
        assertTrue(Subprotocols.offered(header));
        assertEquals("K3y-_x", Subprotocols.key(header));
        assertNull(Subprotocols.credential(header));
        assertFalse(Subprotocols.offered("ponysdk.key.x"));
        assertFalse(Subprotocols.offered(null));
        assertNull(Subprotocols.key(null));
        assertNull(Subprotocols.key("ponysdk.key."), "empty");
    }

    @Test
    public void theCredentialIsBase64UrlOfItsUtf8() {
        final String credential = "eyJhbGciOi.payload.sig é";
        final String header = "ponysdk,ponysdk.credential."
            + Base64.getUrlEncoder().withoutPadding().encodeToString(credential.getBytes(StandardCharsets.UTF_8));
        assertEquals(credential, Subprotocols.credential(header));
        assertNull(Subprotocols.credential("ponysdk, ponysdk.credential.***"), "not base64url");
    }
}
