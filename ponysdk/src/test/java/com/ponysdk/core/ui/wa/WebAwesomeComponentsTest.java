/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.ui.wa;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WebAwesomeComponents} initialization and registration.
 * <p>
 * Validates: Requirements 12.3, 13.8, 14.1
 * </p>
 */
class WebAwesomeComponentsTest {

    @BeforeEach
    void setUp() {
        // Reset state before each test
        if (WebAwesomeComponents.isInitialized()) {
            WebAwesomeComponents.resetForTesting();
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        if (WebAwesomeComponents.isInitialized()) {
            WebAwesomeComponents.resetForTesting();
        }
    }

    @Test
    void testInitialize_registersAllComponents() {
        // When
        WebAwesomeComponents.initialize();

        // Then
        assertTrue(WebAwesomeComponents.isInitialized());
        final ComponentRegistrar registrar = WebAwesomeComponents.getRegistrar();
        
        // Verify all 57 components are registered
        assertEquals(57, registrar.size(), "Should register all 57 Web Awesome components");
    }

    @Test
    void testInitialize_isIdempotent() {
        // When
        WebAwesomeComponents.initialize();
        final int firstSize = WebAwesomeComponents.getRegistrar().size();
        
        WebAwesomeComponents.initialize(); // Call again
        final int secondSize = WebAwesomeComponents.getRegistrar().size();

        // Then
        assertEquals(firstSize, secondSize, "Multiple initializations should not change registration count");
    }

    @Test
    void testGetRegistrar_beforeInitialize_throwsException() {
        // When/Then
        assertThrows(IllegalStateException.class, WebAwesomeComponents::getRegistrar,
            "Should throw when accessing registrar before initialization");
    }

    @Test
    void testIsRegistered_knownComponents() {
        // Given
        WebAwesomeComponents.initialize();

        // When/Then - Test representative components
        assertTrue(WebAwesomeComponents.isRegistered("wa-button"));
        assertTrue(WebAwesomeComponents.isRegistered("wa-input"));
        assertTrue(WebAwesomeComponents.isRegistered("wa-dialog"));
        assertTrue(WebAwesomeComponents.isRegistered("wa-select"));
        assertTrue(WebAwesomeComponents.isRegistered("wa-checkbox"));
    }

    @Test
    void testIsRegistered_unknownComponent() {
        // Given
        WebAwesomeComponents.initialize();

        // When/Then
        assertFalse(WebAwesomeComponents.isRegistered("wa-nonexistent"));
        assertFalse(WebAwesomeComponents.isRegistered("unknown-component"));
    }

    @Test
    void testComponentLookup_returnsCorrectMetadata() {
        // Given
        WebAwesomeComponents.initialize();
        final ComponentRegistrar registrar = WebAwesomeComponents.getRegistrar();

        // When
        final var buttonEntry = registrar.lookup("wa-button");

        // Then
        assertTrue(buttonEntry.isPresent());
        assertEquals("wa-button", buttonEntry.get().tagName());
        assertEquals("com.ponysdk.core.ui.wa.WAButton", buttonEntry.get().wrapperClassName());
        assertEquals("com.ponysdk.core.ui.wa.props.ButtonProps", buttonEntry.get().propsClassName());
        assertEquals("stable", buttonEntry.get().status());
    }

    @Test
    void testAllComponentsHaveCorrectTagNamePrefix() {
        // Given
        WebAwesomeComponents.initialize();
        final ComponentRegistrar registrar = WebAwesomeComponents.getRegistrar();

        // When/Then
        registrar.getAll().keySet().forEach(tagName ->
            assertTrue(tagName.startsWith("wa-"), 
                "All Web Awesome components should have 'wa-' prefix: " + tagName)
        );
    }

    @Test
    void testComponentStatusBreakdown() {
        // Given
        WebAwesomeComponents.initialize();
        final ComponentRegistrar registrar = WebAwesomeComponents.getRegistrar();

        // When
        final long stable = registrar.getAll().values().stream()
            .filter(e -> "stable".equals(e.status()))
            .count();
        final long experimental = registrar.getAll().values().stream()
            .filter(e -> "experimental".equals(e.status()))
            .count();

        // Then
        assertTrue(stable > 0, "Should have stable components");
        assertTrue(stable + experimental == registrar.size(), 
            "All components should be either stable or experimental");
    }

    @Test
    void testRepresentativeComponentsAreRegistered() {
        // Given
        WebAwesomeComponents.initialize();
        final ComponentRegistrar registrar = WebAwesomeComponents.getRegistrar();

        // When/Then - Verify key components from each category
        
        // Input components
        assertTrue(registrar.isRegistered("wa-input"));
        assertTrue(registrar.isRegistered("wa-textarea"));
        assertTrue(registrar.isRegistered("wa-select"));
        assertTrue(registrar.isRegistered("wa-checkbox"));
        assertTrue(registrar.isRegistered("wa-radio-group"));
        assertTrue(registrar.isRegistered("wa-switch"));
        assertTrue(registrar.isRegistered("wa-slider"));
        assertTrue(registrar.isRegistered("wa-color-picker"));
        assertTrue(registrar.isRegistered("wa-rating"));

        // Display components
        assertTrue(registrar.isRegistered("wa-badge"));
        assertTrue(registrar.isRegistered("wa-avatar"));
        assertTrue(registrar.isRegistered("wa-card"));
        assertTrue(registrar.isRegistered("wa-tag"));
        assertTrue(registrar.isRegistered("wa-progress-bar"));
        assertTrue(registrar.isRegistered("wa-progress-ring"));
        assertTrue(registrar.isRegistered("wa-spinner"));
        assertTrue(registrar.isRegistered("wa-skeleton"));
        assertTrue(registrar.isRegistered("wa-tooltip"));
        assertTrue(registrar.isRegistered("wa-icon"));

        // Navigation components
        assertTrue(registrar.isRegistered("wa-breadcrumb"));
        assertTrue(registrar.isRegistered("wa-tab-group"));
        assertTrue(registrar.isRegistered("wa-drawer"));
        assertTrue(registrar.isRegistered("wa-dropdown"));

        // Overlay components
        assertTrue(registrar.isRegistered("wa-dialog"));
        assertTrue(registrar.isRegistered("wa-popup"));
    }
}
