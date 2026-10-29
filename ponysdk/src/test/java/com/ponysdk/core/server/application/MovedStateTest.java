/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ValueTypeModel;

public class MovedStateTest {

    @Test
    public void eachTypeComesBackFromItsEncoding() {
        final MovedState state = new MovedState().putBoolean("on", true).putInt("qty", -42).putLong("at", 1L << 40)
            .putDouble("px", 1.0845).putString("draft", "Achat 10M EUR/USD à 1,0845 ✓").putBytes("raw", new byte[] { 1, 2, 3 });

        final MovedState back = MovedState.decode(ByteBuffer.wrap(state.encode()));

        assertTrue(back.getBoolean("on", false));
        assertEquals(-42, back.getInt("qty", 0));
        assertEquals(1L << 40, back.getLong("at", 0));
        assertEquals(1.0845, back.getDouble("px", 0));
        assertEquals("Achat 10M EUR/USD à 1,0845 ✓", back.getString("draft", null));
        assertArrayEquals(new byte[] { 1, 2, 3 }, back.getBytes("raw"));
    }

    @Test
    public void aMissingNameOrAnotherTypeGivesTheFallback() {
        final MovedState state = new MovedState().putString("name", "x");

        assertFalse(state.getBoolean("name", false));
        assertEquals(7, state.getInt("name", 7));
        assertEquals(7L, state.getLong("name", 7L));
        assertEquals(7.5, state.getDouble("name", 7.5));
        assertEquals("y", state.getString("missing", "y"));
        assertNull(state.getBytes("name"));
    }

    @Test
    public void aNullIsNotCarriedAndRemovesTheName() {
        final MovedState state = new MovedState().putString("s", "a").putBytes("b", new byte[] { 1 });
        state.putString("s", null).putBytes("b", null);

        assertFalse(state.has("s"));
        assertFalse(state.has("b"));
        assertTrue(state.isEmpty());
    }

    @Test
    public void theBytesPutAndGotAreCopies() {
        final byte[] bytes = { 1 };
        final MovedState state = new MovedState().putBytes("b", bytes);
        bytes[0] = 9;
        state.getBytes("b")[0] = 8;

        assertArrayEquals(new byte[] { 1 }, state.getBytes("b"));
    }

    @Test
    public void bytesThatAreNotAStateAreRefused() {
        final ByteBuffer truncated = ByteBuffer.wrap(new MovedState().putLong("at", 1).encode(), 0, 10);
        final ByteBuffer nullType = ByteBuffer.allocate(6).putInt(1).put((byte) 'n').put((byte) ValueTypeModel.NULL.ordinal()).flip();
        final ByteBuffer unknownType = ByteBuffer.allocate(6).putInt(1).put((byte) 'n').put((byte) 99).flip();

        assertThrows(IllegalArgumentException.class, () -> MovedState.decode(truncated));
        assertThrows(IllegalArgumentException.class, () -> MovedState.decode(nullType));
        assertThrows(IllegalArgumentException.class, () -> MovedState.decode(unknownType));
    }
}
