/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.ponysdk.test.PSuite;

import org.junit.jupiter.api.Test;

public class PSuggestBoxTest extends PSuite {

    /** Its creation names its oracle and its text box (ORACLE, TEXTBOX_ID): both exist on the terminal first. */
    @Test
    public void theOracleAndTheTextBoxAreCreatedWithTheSuggestBox() {
        final PMultiWordSuggestOracle oracle = new PMultiWordSuggestOracle();
        oracle.addAll(List.of("Apple", "Apricot"));
        final PSuggestBox box = Element.newPSuggestBox(oracle);
        assertNotNull(box.getTextBox());
        PWindow.getMain().add(box);
        assertTrue(oracle.isInitialized());
        assertTrue(box.getTextBox().isInitialized());
        assertTrue(box.isInitialized());
        box.setText("Apple");
        assertTrue("Apple".equals(box.getText()), "the text is the text box's");
    }
}
