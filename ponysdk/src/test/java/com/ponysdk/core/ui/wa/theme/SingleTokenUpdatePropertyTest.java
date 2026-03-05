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

package com.ponysdk.core.ui.wa.theme;

import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for single token update producing a minimal CSS patch.
 * <p>
 * <b>Property 4: Single Token Update Produces Minimal CSS Patch</b>
 * </p>
 * <p>
 * For any active theme and any single design token modification, the Theme_Engine
 * SHALL produce a CSS update that modifies exactly one CSS custom property,
 * leaving all other properties unchanged.
 * </p>
 * <p>
 * <b>Validates: Requirements 6.5</b>
 * </p>
 */
@Tag("Feature: ui-library-wrapper, Property 4: Single Token Update Produces Minimal CSS Patch")
public class SingleTokenUpdatePropertyTest {

    /** Known token names from the default themes. */
    private static final String[] TOKEN_NAMES = {
            "primary-color", "secondary-color", "success-color",
            "warning-color", "danger-color", "neutral-color",
            "font-family", "font-size-base", "spacing-unit",
            "border-radius", "shadow-level"
    };

    /**
     * Regex matching a setProperty call: setProperty('--wa-{name}', '{value}')
     * Note: values may contain escaped single quotes (\') so we match until the closing ')
     */
    private static final Pattern SET_PROPERTY_PATTERN =
            Pattern.compile("setProperty\\('(--wa-[\\w-]+)',\\s*'(.*?)'\\)");

    /**
     * Property 4: For any active theme and any single token modification,
     * setToken SHALL inject exactly one JS call containing exactly one setProperty
     * for the correct --wa-{tokenName} variable.
     * <p>
     * <b>Validates: Requirements 6.5</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 4: setToken produces exactly one setProperty call for the correct token")
    void setTokenProducesExactlyOneSetPropertyCall(
            @ForAll("activeTheme") String themeId,
            @ForAll("tokenName") String tokenName,
            @ForAll("tokenValue") String tokenValue
    ) {
        final List<String> capturedJs = new ArrayList<>();
        final ThemeEngine engine = new ThemeEngine(js -> capturedJs.add(js));

        // Apply the theme first (this triggers a full CSS injection)
        engine.applyTheme(themeId);
        capturedJs.clear(); // discard the full theme injection

        // Now update a single token
        engine.setToken(tokenName, tokenValue);

        // Exactly one JS injection should have been made
        assertEquals(1, capturedJs.size(),
                "Expected exactly 1 JS injection for setToken, but got " + capturedJs.size());

        final String injectedJs = capturedJs.get(0);

        // The injected JS should contain exactly one setProperty call
        final Matcher matcher = SET_PROPERTY_PATTERN.matcher(injectedJs);
        assertTrue(matcher.find(),
                "Injected JS should contain a setProperty call but was: " + injectedJs);

        final String varName = matcher.group(1);
        final String varValue = matcher.group(2);

        // Verify the correct CSS variable name
        assertEquals("--wa-" + tokenName, varName,
                "setProperty should target --wa-" + tokenName + " but targeted " + varName);

        // Verify the correct value (injectSingleToken escapes single quotes)
        final String expectedValue = tokenValue.replace("'", "\\'");
        assertEquals(expectedValue, varValue,
                "setProperty value mismatch for token '" + tokenName + "'");

        // Verify no other setProperty calls exist
        assertFalse(matcher.find(),
                "Injected JS should contain only one setProperty call but found more in: " + injectedJs);
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<String> activeTheme() {
        return Arbitraries.of("light", "dark");
    }

    @Provide
    Arbitrary<String> tokenName() {
        return Arbitraries.of(TOKEN_NAMES);
    }

    @Provide
    Arbitrary<String> tokenValue() {
        return Arbitraries.of(
                "#0969da", "#58a6ff", "#1a7f37", "#3fb950",
                "#bf8700", "#cf222e", "#f85149", "#656d76",
                "'Inter', sans-serif", "'Roboto', sans-serif",
                "1rem", "0.5rem", "0.75rem", "1.25rem",
                "0.375rem", "0.5rem", "8px", "12px",
                "0 2px 8px rgba(0,0,0,0.1)", "0 4px 12px rgba(0,0,0,0.2)",
                "16px", "14px", "18px"
        );
    }
}
