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

package com.ponysdk.sample.client.playground;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ponysdk.core.ui.component.PWebComponent;
import com.ponysdk.core.ui.wa.WAButton;
import com.ponysdk.core.ui.wa.WAInput;

/**
 * Unit tests for {@link DefaultComponentScanner}.
 */
public class DefaultComponentScannerTest {

    private DefaultComponentScanner scanner;

    @Before
    public void setUp() {
        scanner = new DefaultComponentScanner();
    }

    @Test
    public void testScanComponents_shouldReturnNonEmptyList() {
        final List<Class<? extends PWebComponent<?>>> components = scanner.scanComponents();

        assertNotNull("Component list should not be null", components);
        assertFalse("Component list should not be empty", components.isEmpty());
    }

    @Test
    public void testScanComponents_shouldContainKnownComponents() {
        final List<Class<? extends PWebComponent<?>>> components = scanner.scanComponents();

        assertTrue("Should contain WAButton", components.contains(WAButton.class));
        assertTrue("Should contain WAInput", components.contains(WAInput.class));
    }

    @Test
    public void testScanComponents_shouldReturnSortedList() {
        final List<Class<? extends PWebComponent<?>>> components = scanner.scanComponents();

        // Verify list is sorted alphabetically by simple name
        for (int i = 0; i < components.size() - 1; i++) {
            final String current = components.get(i).getSimpleName();
            final String next = components.get(i + 1).getSimpleName();
            assertTrue("Components should be sorted alphabetically: " + current + " should come before " + next,
                    current.compareTo(next) <= 0);
        }
    }

    @Test
    public void testScanComponents_allComponentsExtendPWebComponent() {
        final List<Class<? extends PWebComponent<?>>> components = scanner.scanComponents();

        for (final Class<? extends PWebComponent<?>> component : components) {
            assertTrue(component.getSimpleName() + " should extend PWebComponent",
                    PWebComponent.class.isAssignableFrom(component));
        }
    }

    @Test
    public void testExtractComponentName_shouldRemoveWAPrefix() {
        assertEquals("Button", DefaultComponentScanner.extractComponentName(WAButton.class));
        assertEquals("Input", DefaultComponentScanner.extractComponentName(WAInput.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractComponentName_shouldThrowOnNull() {
        DefaultComponentScanner.extractComponentName(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testScanComponents_shouldReturnUnmodifiableList() {
        final List<Class<? extends PWebComponent<?>>> components = scanner.scanComponents();
        components.add(WAButton.class);
    }
}
