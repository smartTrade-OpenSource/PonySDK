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

import com.ponysdk.core.ui.basic.PScript;
import com.ponysdk.core.ui.basic.PWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Server-side theme engine that manages design tokens and injects CSS custom properties
 * on the client's {@code :root} element via {@link PScript}.
 * <p>
 * Provides default light and dark themes out of the box, supports custom theme creation
 * by extending a base theme, hot theme switching, and individual token updates.
 * </p>
 */
public class ThemeEngine {

    private static final Logger log = LoggerFactory.getLogger(ThemeEngine.class);

    private static final String CSS_VAR_PREFIX = "--wa-";

    private final Map<String, ThemeDefinition> themes = new LinkedHashMap<>();
    private String activeThemeId = "light";

    /**
     * Functional interface for injecting CSS on the client.
     * Default implementation uses {@link PScript#execute(PWindow, String)}.
     * Can be replaced for testing.
     */
    @FunctionalInterface
    public interface CssInjector {
        void inject(String javaScript);
    }

    private CssInjector cssInjector;

    /**
     * Creates a ThemeEngine with default light and dark themes,
     * using PScript for client-side CSS injection.
     */
    public ThemeEngine() {
        this(js -> PScript.execute(PWindow.getMain(), js));
    }

    /**
     * Creates a ThemeEngine with default light and dark themes
     * and a custom CSS injector (useful for testing).
     *
     * @param cssInjector strategy for injecting CSS on the client
     */
    public ThemeEngine(final CssInjector cssInjector) {
        this.cssInjector = cssInjector;
        registerDefaultThemes();
    }

    private void registerDefaultThemes() {
        themes.put("light", createLightTheme());
        themes.put("dark", createDarkTheme());
    }

    static ThemeDefinition createLightTheme() {
        final Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("primary-color", "#0969da");
        tokens.put("secondary-color", "#6e7781");
        tokens.put("success-color", "#1a7f37");
        tokens.put("warning-color", "#bf8700");
        tokens.put("danger-color", "#cf222e");
        tokens.put("neutral-color", "#656d76");
        tokens.put("font-family", "'Inter', sans-serif");
        tokens.put("font-size-base", "1rem");
        tokens.put("spacing-unit", "1rem");
        tokens.put("border-radius", "0.375rem");
        tokens.put("shadow-level", "0 2px 8px rgba(0,0,0,0.1)");
        return new ThemeDefinition("light", "Light", Map.copyOf(tokens));
    }

    static ThemeDefinition createDarkTheme() {
        final Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("primary-color", "#58a6ff");
        tokens.put("secondary-color", "#8b949e");
        tokens.put("success-color", "#3fb950");
        tokens.put("warning-color", "#d29922");
        tokens.put("danger-color", "#f85149");
        tokens.put("neutral-color", "#8b949e");
        tokens.put("font-family", "'Inter', sans-serif");
        tokens.put("font-size-base", "1rem");
        tokens.put("spacing-unit", "1rem");
        tokens.put("border-radius", "0.375rem");
        tokens.put("shadow-level", "0 2px 8px rgba(0,0,0,0.3)");
        return new ThemeDefinition("dark", "Dark", Map.copyOf(tokens));
    }

    /**
     * Applies the specified theme by generating CSS custom property declarations
     * and injecting them on the client's {@code :root} element.
     *
     * @param themeId the theme identifier (e.g. "light", "dark")
     * @throws IllegalArgumentException if the theme is not registered
     */
    public void applyTheme(final String themeId) {
        final ThemeDefinition theme = themes.get(themeId);
        if (theme == null) {
            throw new IllegalArgumentException("Unknown theme: " + themeId);
        }
        activeThemeId = themeId;
        final String css = generateCss(theme);
        injectFullCss(css);
    }

    /**
     * Modifies a single design token in the active theme and sends a minimal
     * CSS update to the client (only the changed variable).
     *
     * @param tokenName the token name (e.g. "primary-color")
     * @param value     the new CSS value
     * @throws IllegalStateException if no theme is active
     */
    public void setToken(final String tokenName, final String value) {
        final ThemeDefinition active = themes.get(activeThemeId);
        if (active == null) {
            throw new IllegalStateException("No active theme");
        }

        // Build updated tokens map
        final Map<String, String> updatedTokens = new LinkedHashMap<>(active.tokens());
        updatedTokens.put(tokenName, value);
        themes.put(activeThemeId, new ThemeDefinition(active.id(), active.name(), Map.copyOf(updatedTokens)));

        // Send minimal single-property update
        injectSingleToken(tokenName, value);
    }

    /**
     * Creates a custom theme by merging a base theme's tokens with the provided overrides.
     *
     * @param id          unique identifier for the new theme
     * @param baseThemeId the base theme to extend
     * @param overrides   token overrides (keys that exist in the base are replaced, new keys are added)
     * @throws IllegalArgumentException if the base theme is not registered
     */
    public void createCustomTheme(final String id, final String baseThemeId, final Map<String, String> overrides) {
        final ThemeDefinition base = themes.get(baseThemeId);
        if (base == null) {
            throw new IllegalArgumentException("Unknown base theme: " + baseThemeId);
        }

        final Map<String, String> merged = new LinkedHashMap<>(base.tokens());
        merged.putAll(overrides);
        themes.put(id, new ThemeDefinition(id, id, Map.copyOf(merged)));
    }

    /**
     * Toggles between light and dark themes.
     * If the active theme is "light", switches to "dark" and vice versa.
     * If the active theme is neither, switches to "light".
     */
    public void toggleDarkMode() {
        final String target = "dark".equals(activeThemeId) ? "light" : "dark";
        applyTheme(target);
    }

    // ========== CSS Generation ==========

    /**
     * Generates a full CSS string with all tokens as {@code --wa-*} custom properties on {@code :root}.
     *
     * @param theme the theme definition
     * @return CSS string like {@code :root { --wa-primary-color: #0969da; ... }}
     */
    String generateCss(final ThemeDefinition theme) {
        final String declarations = theme.tokens().entrySet().stream()
                .map(e -> "  " + CSS_VAR_PREFIX + e.getKey() + ": " + e.getValue() + ";")
                .collect(Collectors.joining("\n"));
        return ":root {\n" + declarations + "\n}";
    }

    /**
     * Generates a CSS variable name from a token name.
     *
     * @param tokenName the token name (e.g. "primary-color")
     * @return the CSS variable name (e.g. "--wa-primary-color")
     */
    String toCssVarName(final String tokenName) {
        return CSS_VAR_PREFIX + tokenName;
    }

    // ========== Client-side injection ==========

    private void injectFullCss(final String css) {
        // Use a <style> element with a known ID so we can replace it on theme switch
        final String escapedCss = css.replace("'", "\\'").replace("\n", "\\n");
        final String js = "(function() {"
                + "var el = document.getElementById('wa-theme-style');"
                + "if (!el) { el = document.createElement('style'); el.id = 'wa-theme-style'; document.head.appendChild(el); }"
                + "el.textContent = '" + escapedCss + "';"
                + "})();";
        cssInjector.inject(js);
    }

    private void injectSingleToken(final String tokenName, final String value) {
        final String varName = toCssVarName(tokenName);
        final String escapedValue = value.replace("'", "\\'");
        final String js = "document.documentElement.style.setProperty('" + varName + "', '" + escapedValue + "');";
        cssInjector.inject(js);
    }

    // ========== Accessors ==========

    /**
     * Returns the currently active theme ID.
     */
    public String getActiveThemeId() {
        return activeThemeId;
    }

    /**
     * Returns the theme definition for the given ID, or null if not found.
     */
    public ThemeDefinition getTheme(final String themeId) {
        return themes.get(themeId);
    }

    /**
     * Returns all registered theme IDs.
     */
    public java.util.Set<String> getThemeIds() {
        return java.util.Collections.unmodifiableSet(themes.keySet());
    }

    /**
     * Sets the CSS injector strategy (useful for testing).
     */
    public void setCssInjector(final CssInjector cssInjector) {
        this.cssInjector = cssInjector;
    }
}
