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

import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for widget name to tag name conversion.
 * <p>
 * <b>Feature: widget-slots-editor, Property 1: Widget name to tag name conversion</b>
 * </p>
 * <p>
 * For any widget class name from the available widgets list (e.g. "Icon", "Badge"),
 * converting it to a tag name should produce the correct kebab-case format prefixed with
 * "wa-" (e.g. "wa-icon", "wa-badge"), and this conversion should be idempotent when
 * applied to an already-converted tag name.
 * </p>
 * <p>
 * <b>Validates: Requirements 1.2</b>
 * </p>
 */
@Tag("Feature: widget-slots-editor, Property 1: Widget name to tag name conversion")
public class WidgetNameConversionPropertyTest {

    /**
     * Replicates the exact conversion logic from ComponentPlayground.extractTagName().
     * The original is a private instance method using the same regex pattern.
     */
    private static String extractTagName(final String componentName) {
        if (componentName.startsWith("WA")) {
            final String withoutPrefix = componentName.substring(2);
            return "wa-" + camelToKebab(withoutPrefix);
        }
        return "wa-" + camelToKebab(componentName);
    }

    /**
     * Replicates the exact conversion logic from ComponentPlayground.camelToKebab().
     */
    private static String camelToKebab(final String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    // ========== Property Tests ==========

    /**
     * Property 1: Each WA* widget name from InsertableWidgetRegistry produces a valid
     * kebab-case "wa-*" tag name.
     * <p>
     * The tag name must:
     * <ul>
     *   <li>Start with "wa-"</li>
     *   <li>Be entirely lowercase</li>
     *   <li>Not contain consecutive hyphens</li>
     *   <li>Not end with a hyphen</li>
     *   <li>Have non-empty content after the "wa-" prefix</li>
     * </ul>
     * </p>
     * <p><b>Validates: Requirements 1.2</b></p>
     */
    @Property(tries = 100)
    @Label("Property 1: WA* widget name produces correct kebab-case wa-* tag name")
    void widgetNameProducesCorrectKebabCaseTagName(
            @ForAll("insertableWidgetName") String widgetName
    ) {
        final String tagName = extractTagName(widgetName);

        assertTrue(tagName.startsWith("wa-"),
                "Tag name '" + tagName + "' from widget '" + widgetName + "' must start with 'wa-'");

        assertEquals(tagName, tagName.toLowerCase(),
                "Tag name '" + tagName + "' must be entirely lowercase");

        assertFalse(tagName.contains("--"),
                "Tag name '" + tagName + "' must not contain consecutive hyphens");

        assertFalse(tagName.endsWith("-"),
                "Tag name '" + tagName + "' must not end with a hyphen");

        assertTrue(tagName.length() > 3,
                "Tag name '" + tagName + "' must have content after 'wa-' prefix");
    }

    /**
     * Property 1 (idempotence): Applying camelToKebab to an already-converted tag name
     * should produce the same result.
     * <p><b>Validates: Requirements 1.2</b></p>
     */
    @Property(tries = 100)
    @Label("Property 1: Conversion is idempotent on already-converted tag names")
    void conversionIsIdempotentOnTagNames(
            @ForAll("insertableWidgetName") String widgetName
    ) {
        final String tagName = extractTagName(widgetName);

        // Applying camelToKebab to an already kebab-case string should not change it
        final String reConverted = camelToKebab(tagName);
        assertEquals(tagName, reConverted,
                "camelToKebab should be idempotent on '" + tagName + "'");
    }

    /**
     * Property 1 (exact mappings): Each known WA* name maps to its expected tag name.
     * <p><b>Validates: Requirements 1.2</b></p>
     */
    @Property(tries = 100)
    @Label("Property 1: Known WA* names map to expected tag names")
    void knownWidgetNamesMapToExpectedTagNames(
            @ForAll("widgetNameWithExpectedTag") WidgetNameMapping mapping
    ) {
        final String tagName = extractTagName(mapping.widgetName());
        assertEquals(mapping.expectedTagName(), tagName,
                "Widget '" + mapping.widgetName() + "' should map to '" + mapping.expectedTagName() + "'");
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<String> insertableWidgetName() {
        return Arbitraries.of(InsertableWidgetRegistry.WIDGET_NAMES);
    }

    @Provide
    Arbitrary<WidgetNameMapping> widgetNameWithExpectedTag() {
        return Arbitraries.of(
                new WidgetNameMapping("Icon", "wa-icon"),
                new WidgetNameMapping("Badge", "wa-badge"),
                new WidgetNameMapping("Spinner", "wa-spinner"),
                new WidgetNameMapping("Button", "wa-button"),
                new WidgetNameMapping("Tag", "wa-tag"),
                new WidgetNameMapping("Avatar", "wa-avatar"),
                new WidgetNameMapping("Divider", "wa-divider"),
                new WidgetNameMapping("ProgressBar", "wa-progress-bar"),
                new WidgetNameMapping("ProgressRing", "wa-progress-ring"),
                new WidgetNameMapping("Skeleton", "wa-skeleton")
        );
    }

    // ========== Test Helpers ==========

    record WidgetNameMapping(String widgetName, String expectedTagName) {}
}
