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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Performance tests for ComponentFilter.
 * <p>
 * Validates that filtering completes within 100ms for up to 100 components.
 * </p>
 * <p>
 * Requirements: 2.2, 8.1
 * </p>
 */
class ComponentFilterPerformanceTest {

    @Test
    void filterWith58Components_shouldCompleteWithin100ms() {
        // Given: 58 component names (realistic scenario)
        final List<String> components = generateComponentNames(58);

        // Warm up
        ComponentFilter.filter(components, "button");

        // When: Measuring filter execution time
        final long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            ComponentFilter.filter(components, "button");
        }
        final long elapsed = (System.nanoTime() - start) / 1_000_000;

        // Then: 1000 iterations should complete well within 100ms total
        assertThat(elapsed)
            .as("1000 filter iterations on 58 components should complete within 100ms")
            .isLessThan(100);
    }

    @Test
    void filterWith100Components_shouldCompleteWithin100ms() {
        // Given: 100 component names (scalability test)
        final List<String> components = generateComponentNames(100);

        // Warm up
        ComponentFilter.filter(components, "comp");

        // When: Measuring filter execution time
        final long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            ComponentFilter.filter(components, "comp");
        }
        final long elapsed = (System.nanoTime() - start) / 1_000_000;

        // Then: 1000 iterations should complete well within 100ms total
        assertThat(elapsed)
            .as("1000 filter iterations on 100 components should complete within 100ms")
            .isLessThan(100);
    }

    private List<String> generateComponentNames(final int count) {
        final String[] prefixes = {"Button", "Card", "Dialog", "Input", "Select",
            "Checkbox", "Radio", "Slider", "Switch", "Tab", "Badge", "Alert",
            "Avatar", "Breadcrumb", "Carousel", "Dropdown", "Icon", "Menu",
            "Modal", "Progress", "Spinner", "Tag", "Textarea", "Tooltip"};
        final List<String> names = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            names.add("Component" + prefixes[i % prefixes.length] + i);
        }
        return names;
    }
}
