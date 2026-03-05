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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ThemeEngine}.
 * Uses a no-op CSS injector to avoid PonySDK runtime dependencies.
 */
class ThemeEngineTest {

    private ThemeEngine engine;
    private final List<String> injectedJs = new ArrayList<>();

    @BeforeEach
    void setUp() {
        injectedJs.clear();
        engine = new ThemeEngine(injectedJs::add);
    }

    // ========== Default themes ==========

    @Test
    void defaultThemes_areLightAndDark() {
        final Set<String> ids = engine.getThemeIds();
        assertTrue(ids.contains("light"), "Should contain light theme");
        assertTrue(ids.contains("dark"), "Should contain dark theme");
    }

    @Test
    void defaultActiveTheme_isLight() {
        assertEquals("light", engine.getActiveThemeId());
    }

    @Test
    void lightTheme_containsAllRequiredTokens() {
        assertRequiredTokens(engine.getTheme("light"));
    }

    @Test
    void darkTheme_containsAllRequiredTokens() {
        assertRequiredTokens(engine.getTheme("dark"));
    }

    @Test
    void lightTheme_hasExpectedValues() {
        final ThemeDefinition light = engine.getTheme("light");
        assertEquals("#0969da", light.tokens().get("primary-color"));
        assertEquals("#6e7781", light.tokens().get("secondary-color"));
        assertEquals("#1a7f37", light.tokens().get("success-color"));
        assertEquals("#bf8700", light.tokens().get("warning-color"));
        assertEquals("#cf222e", light.tokens().get("danger-color"));
        assertEquals("#656d76", light.tokens().get("neutral-color"));
        assertEquals("'Inter', sans-serif", light.tokens().get("font-family"));
        assertEquals("1rem", light.tokens().get("font-size-base"));
        assertEquals("1rem", light.tokens().get("spacing-unit"));
        assertEquals("0.375rem", light.tokens().get("border-radius"));
        assertEquals("0 2px 8px rgba(0,0,0,0.1)", light.tokens().get("shadow-level"));
    }

    @Test
    void darkTheme_hasDarkAppropriateColors() {
        final ThemeDefinition dark = engine.getTheme("dark");
        assertEquals("#58a6ff", dark.tokens().get("primary-color"));
        assertEquals("#f85149", dark.tokens().get("danger-color"));
        assertEquals("#3fb950", dark.tokens().get("success-color"));
    }

    // ========== applyTheme ==========

    @Test
    void applyTheme_setsActiveThemeId() {
        engine.applyTheme("dark");
        assertEquals("dark", engine.getActiveThemeId());
    }

    @Test
    void applyTheme_injectsCssWithAllTokens() {
        engine.applyTheme("light");
        assertEquals(1, injectedJs.size());

        final String js = injectedJs.get(0);
        // The injected JS should contain the CSS with all --wa-* variables
        assertTrue(js.contains("--wa-primary-color"), "Should contain --wa-primary-color");
        assertTrue(js.contains("--wa-font-family"), "Should contain --wa-font-family");
        assertTrue(js.contains(":root"), "Should contain :root selector");
    }

    @Test
    void applyTheme_unknownTheme_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> engine.applyTheme("nonexistent"));
    }

    // ========== generateCss ==========

    @Test
    void generateCss_producesCorrectFormat() {
        final ThemeDefinition theme = new ThemeDefinition("test", "Test",
                Map.of("primary-color", "#ff0000", "font-size-base", "16px"));
        final String css = engine.generateCss(theme);

        assertTrue(css.startsWith(":root {"));
        assertTrue(css.endsWith("}"));
        assertTrue(css.contains("--wa-primary-color: #ff0000;"));
        assertTrue(css.contains("--wa-font-size-base: 16px;"));
    }

    @Test
    void generateCss_tokenCount_matchesThemeTokens() {
        final ThemeDefinition light = engine.getTheme("light");
        final String css = engine.generateCss(light);
        final long varCount = css.lines()
                .filter(line -> line.trim().startsWith("--wa-"))
                .count();
        assertEquals(light.tokens().size(), varCount);
    }

    // ========== setToken ==========

    @Test
    void setToken_updatesActiveThemeToken() {
        engine.setToken("primary-color", "#ff0000");
        assertEquals("#ff0000", engine.getTheme("light").tokens().get("primary-color"));
    }

    @Test
    void setToken_injectsSinglePropertyUpdate() {
        engine.setToken("primary-color", "#ff0000");
        assertEquals(1, injectedJs.size());

        final String js = injectedJs.get(0);
        assertTrue(js.contains("--wa-primary-color"));
        assertTrue(js.contains("#ff0000"));
        assertTrue(js.contains("setProperty"));
    }

    @Test
    void setToken_addsNewToken() {
        engine.setToken("custom-token", "42px");
        assertEquals("42px", engine.getTheme("light").tokens().get("custom-token"));
    }

    // ========== createCustomTheme ==========

    @Test
    void createCustomTheme_mergesBaseWithOverrides() {
        engine.createCustomTheme("brand", "light", Map.of("primary-color", "#1a73e8"));

        final ThemeDefinition brand = engine.getTheme("brand");
        assertNotNull(brand);
        assertEquals("#1a73e8", brand.tokens().get("primary-color"));
        // Non-overridden tokens should come from the base
        assertEquals("#6e7781", brand.tokens().get("secondary-color"));
    }

    @Test
    void createCustomTheme_retainsAllBaseTokens() {
        final ThemeDefinition base = engine.getTheme("light");
        engine.createCustomTheme("custom", "light", Map.of("primary-color", "#000"));

        final ThemeDefinition custom = engine.getTheme("custom");
        assertEquals(base.tokens().size(), custom.tokens().size());
    }

    @Test
    void createCustomTheme_unknownBase_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.createCustomTheme("x", "nonexistent", Map.of()));
    }

    @Test
    void createCustomTheme_canAddExtraTokens() {
        final ThemeDefinition base = engine.getTheme("light");
        engine.createCustomTheme("extended", "light", Map.of("custom-new-token", "10px"));

        final ThemeDefinition extended = engine.getTheme("extended");
        assertEquals(base.tokens().size() + 1, extended.tokens().size());
        assertEquals("10px", extended.tokens().get("custom-new-token"));
    }

    // ========== toggleDarkMode ==========

    @Test
    void toggleDarkMode_switchesFromLightToDark() {
        assertEquals("light", engine.getActiveThemeId());
        engine.toggleDarkMode();
        assertEquals("dark", engine.getActiveThemeId());
    }

    @Test
    void toggleDarkMode_switchesFromDarkToLight() {
        engine.applyTheme("dark");
        injectedJs.clear();
        engine.toggleDarkMode();
        assertEquals("light", engine.getActiveThemeId());
    }

    @Test
    void toggleDarkMode_fromCustomTheme_switchesToDark() {
        engine.createCustomTheme("brand", "light", Map.of());
        engine.applyTheme("brand");
        injectedJs.clear();
        // "brand" is not "dark", so toggleDarkMode switches to "dark"
        engine.toggleDarkMode();
        assertEquals("dark", engine.getActiveThemeId());
    }

    @Test
    void toggleDarkMode_injectsCss() {
        engine.toggleDarkMode();
        assertFalse(injectedJs.isEmpty(), "toggleDarkMode should inject CSS");
    }

    // ========== toCssVarName ==========

    @Test
    void toCssVarName_prependsPrefix() {
        assertEquals("--wa-primary-color", engine.toCssVarName("primary-color"));
        assertEquals("--wa-font-family", engine.toCssVarName("font-family"));
    }

    // ========== Helper ==========

    private void assertRequiredTokens(final ThemeDefinition theme) {
        assertNotNull(theme);
        final Map<String, String> tokens = theme.tokens();
        final List<String> required = List.of(
                "primary-color", "secondary-color", "success-color", "warning-color",
                "danger-color", "neutral-color", "font-family", "font-size-base",
                "spacing-unit", "border-radius", "shadow-level"
        );
        for (final String token : required) {
            assertTrue(tokens.containsKey(token), "Theme '" + theme.id() + "' missing required token: " + token);
            assertNotNull(tokens.get(token), "Token '" + token + "' should not be null");
            assertFalse(tokens.get(token).isBlank(), "Token '" + token + "' should not be blank");
        }
    }
}
