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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for Theme CSS generation.
 * <p>
 * <b>Property 3: Theme CSS Generation</b>
 * </p>
 * <p>
 * For any valid ThemeDefinition containing N design tokens, the generated CSS output
 * SHALL contain exactly N CSS custom property declarations ({@code --wa-*}), and each
 * token name SHALL map to a {@code --wa-{tokenName}} CSS variable with the correct value.
 * </p>
 * <p>
 * <b>Validates: Requirements 6.3</b>
 * </p>
 */
@Tag("Feature: ui-library-wrapper, Property 3: Theme CSS Generation")
public class ThemeCssPropertyTest {

    /** Regex matching a single CSS custom property declaration: --wa-{name}: {value}; */
    private static final Pattern CSS_VAR_PATTERN = Pattern.compile("--wa-([\\w-]+):\\s*(.+?);");

    /**
     * Property 3: For any valid ThemeDefinition with N tokens, generateCss SHALL produce
     * exactly N --wa-* declarations, each with the correct token name and value.
     * <p>
     * <b>Validates: Requirements 6.3</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 3: CSS output contains exactly N --wa-* declarations with correct values")
    void cssContainsExactlyNDeclarationsWithCorrectValues(
            @ForAll("themeDefinition") ThemeDefinition theme
    ) {
        final ThemeEngine engine = new ThemeEngine(js -> {});
        final String css = engine.generateCss(theme);
        final Map<String, String> tokens = theme.tokens();
        final int expectedCount = tokens.size();

        // Extract all --wa-* declarations from the generated CSS
        final Matcher matcher = CSS_VAR_PATTERN.matcher(css);
        int actualCount = 0;
        while (matcher.find()) {
            actualCount++;
            final String varName = matcher.group(1);
            final String varValue = matcher.group(2);

            assertTrue(tokens.containsKey(varName),
                    "CSS contains --wa-" + varName + " but token '" + varName + "' is not in the theme definition. "
                            + "Theme tokens: " + tokens.keySet());

            assertEquals(tokens.get(varName), varValue,
                    "Value mismatch for token '" + varName + "'");
        }

        assertEquals(expectedCount, actualCount,
                "Expected " + expectedCount + " --wa-* declarations but found " + actualCount
                        + ". Theme tokens: " + tokens.keySet());
    }

    /**
     * Property 3 (structural): The CSS output SHALL be wrapped in a :root { ... } block.
     * <p>
     * <b>Validates: Requirements 6.3</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 3: CSS output is wrapped in :root block")
    void cssIsWrappedInRootBlock(
            @ForAll("themeDefinition") ThemeDefinition theme
    ) {
        final ThemeEngine engine = new ThemeEngine(js -> {});
        final String css = engine.generateCss(theme);

        assertTrue(css.startsWith(":root {"),
                "CSS should start with ':root {' but was: " + css.substring(0, Math.min(css.length(), 30)));
        assertTrue(css.endsWith("}"),
                "CSS should end with '}' but was: " + css.substring(Math.max(0, css.length() - 10)));
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<ThemeDefinition> themeDefinition() {
        final Arbitrary<Map.Entry<String, String>> tokenEntry = Combinators.combine(
                Arbitraries.of("primary-color", "secondary-color", "success-color",
                        "warning-color", "danger-color", "neutral-color",
                        "font-family", "font-size-base", "spacing-unit",
                        "border-radius", "shadow-level"),
                Arbitraries.of("#0969da", "#6e7781", "#1a7f37", "#bf8700", "#cf222e",
                        "'Inter', sans-serif", "1rem", "0.375rem",
                        "0 2px 8px rgba(0,0,0,0.1)", "16px", "0.5rem")
        ).as(Map::entry);

        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
                Arbitraries.strings().ofMaxLength(50),
                tokenEntry.list().ofMinSize(0).ofMaxSize(11)
                        .map(entries -> entries.stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                        (a, b) -> b)))
        ).as(ThemeDefinition::new);
    }
}
