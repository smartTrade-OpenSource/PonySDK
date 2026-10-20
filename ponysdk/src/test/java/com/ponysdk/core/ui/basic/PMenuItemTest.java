/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.json.Json;

import org.junit.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.test.PSuite;

public class PMenuItemTest extends PSuite {

    @Test
    public void anItemIsEnabledUntilDisabled() {
        final PMenuItem item = Element.newPMenuItem("Item", () -> {
        });
        assertTrue(item.isEnabled());
        item.setEnabled(false);
        assertFalse(item.isEnabled());
    }

    @Test
    public void theCommandOfADisabledItemIsNotRunWhateverTheClientSends() {
        final AtomicInteger runs = new AtomicInteger();
        final PMenuItem item = Element.newPMenuItem("Item", runs::incrementAndGet);
        final var command = Json.createObjectBuilder().add(ClientToServerModel.HANDLER_COMMAND.toStringValue(), 0).build();
        item.onClientData(command);
        assertEquals(1, runs.get());
        item.setEnabled(false);
        item.onClientData(command);
        assertEquals(1, runs.get());
    }
}
