/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

public class StateSealTest {

    private static final byte[] KEY = new byte[32];
    private static final Instant NOW = Instant.parse("2026-10-29T21:00:00Z");

    static {
        Arrays.fill(KEY, (byte) 7);
    }

    private static StateSeal seal(final byte[] key, final String application, final Instant at) {
        return new StateSeal(key, application, Clock.fixed(at, ZoneOffset.UTC));
    }

    @Test
    public void aSealedStateOpensOnAnotherServerOfTheApplication() throws Exception {
        final String token = seal(KEY, "app", NOW).seal(new MovedState().putString("draft", "10M EUR/USD"));

        final MovedState opened = seal(KEY, "app", NOW.plusSeconds(3)).open(token);

        assertEquals("10M EUR/USD", opened.getString("draft", null));
        assertTrue(token.matches("[A-Za-z0-9_-]+"), "URL-safe, no padding");
        assertTrue(!token.contains("10M"), "the client cannot read it");
    }

    @Test
    public void twoSealsOfOneStateDiffer() throws Exception {
        final MovedState state = new MovedState().putInt("n", 1);
        assertNotEquals(seal(KEY, "app", NOW).seal(state), seal(KEY, "app", NOW).seal(state));
    }

    @Test
    public void anAlteredTokenDoesNotOpen() throws Exception {
        final byte[] bytes = Base64.getUrlDecoder().decode(seal(KEY, "app", NOW).seal(new MovedState().putInt("n", 1)));
        bytes[bytes.length - 1] ^= 1;
        final String altered = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        assertThrows(IllegalArgumentException.class, () -> seal(KEY, "app", NOW).open(altered));
    }

    @Test
    public void aTokenOfAnotherApplicationOrKeyDoesNotOpen() throws Exception {
        final String token = seal(KEY, "app", NOW).seal(new MovedState().putInt("n", 1));
        final byte[] other = KEY.clone();
        other[0] = 8;

        assertThrows(IllegalArgumentException.class, () -> seal(KEY, "other-app", NOW).open(token));
        assertThrows(IllegalArgumentException.class, () -> seal(other, "app", NOW).open(token));
    }

    @Test
    public void aTokenOlderThanTwoMinutesOrFromTheFutureDoesNotOpen() throws Exception {
        final String token = seal(KEY, "app", NOW).seal(new MovedState().putInt("n", 1));

        seal(KEY, "app", NOW.plus(StateSeal.MAX_AGE)).open(token); // the last instant it opens
        assertThrows(IllegalArgumentException.class, () -> seal(KEY, "app", NOW.plus(StateSeal.MAX_AGE).plusMillis(1)).open(token));
        assertThrows(IllegalArgumentException.class, () -> seal(KEY, "app", NOW.minusMillis(1)).open(token));
    }

    @Test
    public void whatIsNoTokenDoesNotOpen() {
        assertThrows(IllegalArgumentException.class, () -> seal(KEY, "app", NOW).open("not base64!"));
        assertThrows(IllegalArgumentException.class, () -> seal(KEY, "app", NOW).open("AAAA"));
    }

    /** Sealed by a later version of the format, under the same key: refused rather than misread. */
    @Test
    public void aTokenOfAnotherVersionDoesNotOpen() throws Exception {
        final byte[] plain = ByteBuffer.allocate(9).put((byte) 2).putLong(NOW.toEpochMilli()).array();
        final byte[] nonce = new byte[12];
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "AES"), new GCMParameterSpec(128, nonce));
        cipher.updateAAD("app".getBytes(StandardCharsets.UTF_8));
        final byte[] sealed = cipher.doFinal(plain);
        final String token = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteBuffer.allocate(12 + sealed.length).put(nonce).put(sealed).array());

        assertThrows(IllegalArgumentException.class, () -> seal(KEY, "app", NOW).open(token));
    }

    @Test
    public void aStateTooLargeForAUrlIsNotSealed() throws Exception {
        assertNull(seal(KEY, "app", NOW).seal(new MovedState().putBytes("big", new byte[3100])));
    }
}
