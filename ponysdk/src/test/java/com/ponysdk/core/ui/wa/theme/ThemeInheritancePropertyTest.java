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

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for theme inheritance via {@link ThemeEngine#createCustomTheme}.
 * <p>
 * <b>Property 5: Theme Inheritance</b>
 * </p>
 * <p>
 * For any base theme with N tokens and any set of K override tokens (where K &le; N),
 * creating a custom theme SHALL produce a theme containing all N tokens, where the K
 * overridden tokens have the new values and the remaining N-K tokens retain the base
 * theme values.
 * </p>
 * <p>
 * <b>Validates: Requirements 6.6</b>
 * </p>
 */
@Tag("Feature: ui-library-wrapper, Property 5: Theme Inheritance")
public class ThemeInheritancePropertyTest {

    /** All token names present in the default light/dark themes. */
    private static final List<String> ALL_TOKEN_NAMES = List.of(
            "primary-color", "secondary-color", "success-color",
            "warning-color", "danger-color", "neutral-color",
            "font-family", "font-size-base", "spacing-unit",
            "border-radius", "shadow-level"
    );

    /** Override values distinct from both light and dark defaults. */
    private static final String[] OVERRIDE_VALUES = {
            "#ff0000", "#00ff00", "#0000ff", "#abcdef",
            "'Helvetica', sans-serif", "2rem", "1.5rem",
            "10px", "0 8px 24px rgba(0,0,0,0.5)", "20px", "0.125rem"
    };

    /**
     * Property 5: For any base theme (light or dark) and any subset of K token overrides
     * (K &le; N), the custom theme SHALL contain all N tokens with K overridden and N-K
     * retained from the base.
     * <p>
     * <b>Validates: Requirements 6.6</b>
     * </p>
     */
    @Property(tries = 200)
    @Label("Property 5: Custom theme has all N tokens with K overridden and N-K retained from base")
    void customThemeHasAllTokensWithCorrectOverrides(
            @ForAll("baseThemeId") String baseThemeId,
            @ForAll("tokenOverrides") Map<String, String> overrides
    ) {
        final ThemeEngine engine = new ThemeEngine(js -> {});

        final ThemeDefinition baseTheme = engine.getTheme(baseThemeId);
        assertNotNull(baseTheme, "Base theme '" + baseThemeId + "' should exist");

        final Map<String, String> baseTokens = baseTheme.tokens();
        final int n = baseTokens.size();

        // Create the custom theme
        final String customId = "custom-" + UUID.randomUUID();
        engine.createCustomTheme(customId, baseThemeId, overrides);

        final ThemeDefinition customTheme = engine.getTheme(customId);
        assertNotNull(customTheme, "Custom theme should be registered");

        final Map<String, String> customTokens = customTheme.tokens();

        // The custom theme must have at least all N base tokens
        // (it may also have extra tokens if overrides contain keys not in base)
        for (final Map.Entry<String, String> baseEntry : baseTokens.entrySet()) {
            final String tokenName = baseEntry.getKey();
            assertTrue(customTokens.containsKey(tokenName),
                    "Custom theme should contain base token '" + tokenName + "'");

            if (overrides.containsKey(tokenName)) {
                // Overridden token: should have the override value
                assertEquals(overrides.get(tokenName), customTokens.get(tokenName),
                        "Overridden token '" + tokenName + "' should have the override value");
            } else {
                // Retained token: should have the base value
                assertEquals(baseEntry.getValue(), customTokens.get(tokenName),
                        "Non-overridden token '" + tokenName + "' should retain the base value");
            }
        }
    }

    /**
     * Property 5 (count): The custom theme token count SHALL be at least N (base token count).
     * <p>
     * <b>Validates: Requirements 6.6</b>
     * </p>
     */
    @Property(tries = 200)
    @Label("Property 5: Custom theme token count is at least N (base count)")
    void customThemeTokenCountIsAtLeastBaseCount(
            @ForAll("baseThemeId") String baseThemeId,
            @ForAll("tokenOverrides") Map<String, String> overrides
    ) {
        final ThemeEngine engine = new ThemeEngine(js -> {});

        final ThemeDefinition baseTheme = engine.getTheme(baseThemeId);
        final int n = baseTheme.tokens().size();

        final String customId = "custom-" + UUID.randomUUID();
        engine.createCustomTheme(customId, baseThemeId, overrides);

        final ThemeDefinition customTheme = engine.getTheme(customId);
        assertTrue(customTheme.tokens().size() >= n,
                "Custom theme should have at least " + n + " tokens (base count) but had "
                        + customTheme.tokens().size());
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<String> baseThemeId() {
        return Arbitraries.of("light", "dark");
    }

    @Provide
    Arbitrary<Map<String, String>> tokenOverrides() {
        // Generate a random subset of token names with override values
        final Arbitrary<List<String>> subsetKeys = Arbitraries.of(ALL_TOKEN_NAMES)
                .list().ofMinSize(0).ofMaxSize(ALL_TOKEN_NAMES.size())
                .map(list -> list.stream().distinct().collect(Collectors.toList()));

        return subsetKeys.flatMap(keys -> {
            if (keys.isEmpty()) {
                return Arbitraries.just(Map.of());
            }
            return Arbitraries.of(OVERRIDE_VALUES)
                    .list().ofSize(keys.size())
                    .map(values -> {
                        final Map<String, String> map = new LinkedHashMap<>();
                        for (int i = 0; i < keys.size(); i++) {
                            map.put(keys.get(i), values.get(i));
                        }
                        return map;
                    });
        });
    }
}
