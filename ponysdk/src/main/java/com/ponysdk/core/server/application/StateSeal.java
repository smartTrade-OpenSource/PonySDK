/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Seals a {@link MovedState} for the client to carry, and opens it on the next server (ADR-0018): AES-256-GCM under
 * the application's key, its id as additional data, the time of the move inside. The client can neither read nor
 * change it; a state of another application, altered or older than {@link #MAX_AGE} does not open.
 */
final class StateSeal {

    /** From the move to the next server's handshake: the page reloads in a second or two. */
    static final Duration MAX_AGE = Duration.ofMinutes(2);
    /** It travels in the handshake's URL. */
    static final int MAX_TOKEN_LENGTH = 4096;

    private static final byte VERSION = 1;
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;
    private final byte[] applicationId;
    private final Clock clock;

    StateSeal(final byte[] key, final String applicationId, final Clock clock) {
        this.key = new SecretKeySpec(key, "AES");
        this.applicationId = String.valueOf(applicationId).getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    /** @return the token, URL-safe; null when it would be longer than {@link #MAX_TOKEN_LENGTH} */
    String seal(final MovedState state) throws GeneralSecurityException {
        final byte[] values = state.encode();
        final byte[] plain = ByteBuffer.allocate(1 + Long.BYTES + values.length).put(VERSION)
            .putLong(clock.millis()).put(values).array();
        final byte[] nonce = new byte[NONCE_LENGTH];
        RANDOM.nextBytes(nonce);
        final Cipher cipher = cipher(Cipher.ENCRYPT_MODE, nonce);
        final byte[] sealed = cipher.doFinal(plain);
        final byte[] token = ByteBuffer.allocate(NONCE_LENGTH + sealed.length).put(nonce).put(sealed).array();
        final String text = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
        return text.length() > MAX_TOKEN_LENGTH ? null : text;
    }

    /** @throws IllegalArgumentException for a token that does not open: altered, of another key or application, too old */
    MovedState open(final String token) {
        try {
            final byte[] bytes = Base64.getUrlDecoder().decode(token);
            if (bytes.length <= NONCE_LENGTH) throw new IllegalArgumentException("Too short");
            final Cipher cipher = cipher(Cipher.DECRYPT_MODE, java.util.Arrays.copyOf(bytes, NONCE_LENGTH));
            final ByteBuffer plain = ByteBuffer.wrap(cipher.doFinal(bytes, NONCE_LENGTH, bytes.length - NONCE_LENGTH));
            if (plain.get() != VERSION) throw new IllegalArgumentException("Unknown version");
            final long age = clock.millis() - plain.getLong();
            if (age < 0 || age > MAX_AGE.toMillis()) throw new IllegalArgumentException("Moved " + age + " ms ago");
            return MovedState.decode(plain);
        } catch (final GeneralSecurityException e) {
            throw new IllegalArgumentException("Does not open", e);
        }
    }

    private Cipher cipher(final int mode, final byte[] nonce) throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, nonce));
        cipher.updateAAD(applicationId);
        return cipher;
    }
}
