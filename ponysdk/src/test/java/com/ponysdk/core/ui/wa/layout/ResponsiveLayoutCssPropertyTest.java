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

package com.ponysdk.core.ui.wa.layout;

import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for responsive layout CSS generation.
 * <p>
 * <b>Property 12: Responsive Layout CSS Generation</b>
 * </p>
 * <p>
 * For any PResponsiveGrid configuration with breakpoints for mobile (0-599px),
 * tablet (600-1023px), and desktop (1024px+), the generated CSS SHALL contain
 * the correct media queries and column definitions for each breakpoint. For any
 * component with conditional display settings (e.g., hideOnMobile=true), the CSS
 * SHALL include a media query that hides the component at the matching breakpoint.
 * </p>
 * <p>
 * <b>Validates: Requirements 4.2, 4.3, 4.5</b>
 * </p>
 */
@Tag("Feature: ui-library-wrapper, Property 12: Responsive Layout CSS Generation")
public class ResponsiveLayoutCssPropertyTest {

    /** Regex matching a media query block: @media (...) { ... } */
    private static final Pattern MEDIA_QUERY_PATTERN = Pattern.compile(
            "@media\\s+\\(([^)]+)\\)\\s*\\{([^}]+)\\}",
            Pattern.DOTALL
    );

    /** Regex matching grid-template-columns declaration */
    private static final Pattern GRID_COLUMNS_PATTERN = Pattern.compile(
            "grid-template-columns:\\s*repeat\\((\\d+),\\s*1fr\\)"
    );

    /** Regex matching gap declaration */
    private static final Pattern GAP_PATTERN = Pattern.compile(
            "gap:\\s*([^;]+)"
    );

    /** Regex matching display: none declaration */
    private static final Pattern DISPLAY_NONE_PATTERN = Pattern.compile(
            "display:\\s*none"
    );

    /**
     * Property 12: For any ResponsiveGridProps with breakpoints, the CSS SHALL contain
     * correct media queries with column definitions for each breakpoint.
     * <p>
     * <b>Validates: Requirements 4.2, 4.3</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 12: CSS contains correct media queries for each breakpoint")
    void cssContainsCorrectMediaQueriesForBreakpoints(
            @ForAll("responsiveGridProps") ResponsiveGridProps props
    ) {
        final String gridId = "test-grid-" + System.nanoTime();
        final ResponsiveLayoutCssGenerator generator = new ResponsiveLayoutCssGenerator();
        final String css = generator.generateCSS(gridId, props);

        // Verify base grid styles (no media query)
        assertTrue(css.contains("display: grid"),
                "CSS should contain base 'display: grid' declaration");
        assertTrue(css.contains("grid-template-columns: repeat(" + props.columns() + ", 1fr)"),
                "CSS should contain base grid-template-columns with " + props.columns() + " columns");
        assertTrue(css.contains("gap: " + props.gap()),
                "CSS should contain base gap: " + props.gap());

        // Verify media queries for each breakpoint
        for (Map.Entry<String, BreakpointConfig> entry : props.breakpoints().entrySet()) {
            final BreakpointConfig config = entry.getValue();
            final String expectedMediaQuery = "min-width: " + config.minWidth() + "px";

            assertTrue(css.contains(expectedMediaQuery),
                    "CSS should contain media query for breakpoint '" + entry.getKey()
                            + "' with min-width: " + config.minWidth() + "px");

            // Extract the media query block and verify its content
            final Pattern specificMediaPattern = Pattern.compile(
                    "@media\\s+\\(min-width:\\s*" + config.minWidth() + "px\\)\\s*\\{([^}]+)\\}",
                    Pattern.DOTALL
            );
            final Matcher matcher = specificMediaPattern.matcher(css);
            if (matcher.find()) {
                final String mediaContent = matcher.group(1);
                assertTrue(mediaContent.contains("grid-template-columns: repeat(" + config.columns() + ", 1fr)"),
                        "Media query for breakpoint '" + entry.getKey() + "' should set columns to " + config.columns());
                assertTrue(mediaContent.contains("gap: " + config.gap()),
                        "Media query for breakpoint '" + entry.getKey() + "' should set gap to " + config.gap());
            } else {
                fail("Could not find media query block for breakpoint '" + entry.getKey()
                        + "' with min-width: " + config.minWidth() + "px");
            }
        }
    }

    /**
     * Property 12: For any ResponsiveGridProps with hideOnMobile=true, the CSS SHALL
     * contain a media query that hides the component on mobile breakpoint (max-width: 599px).
     * <p>
     * <b>Validates: Requirements 4.5</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 12: CSS hides component on mobile when hideOnMobile=true")
    void cssHidesComponentOnMobileWhenFlagSet(
            @ForAll("responsiveGridPropsWithHideOnMobile") ResponsiveGridProps props
    ) {
        final String gridId = "test-grid-" + System.nanoTime();
        final ResponsiveLayoutCssGenerator generator = new ResponsiveLayoutCssGenerator();
        final String css = generator.generateCSS(gridId, props);

        assertTrue(props.hideOnMobile(), "Test precondition: hideOnMobile should be true");

        // Verify mobile hide media query exists
        final Pattern mobileHidePattern = Pattern.compile(
                "@media\\s+\\(max-width:\\s*599px\\)\\s*\\{([^}]+)\\}",
                Pattern.DOTALL
        );
        final Matcher matcher = mobileHidePattern.matcher(css);
        assertTrue(matcher.find(),
                "CSS should contain @media (max-width: 599px) block when hideOnMobile=true");

        final String mediaContent = matcher.group(1);
        assertTrue(DISPLAY_NONE_PATTERN.matcher(mediaContent).find(),
                "Mobile media query should contain 'display: none' when hideOnMobile=true");
    }

    /**
     * Property 12: For any ResponsiveGridProps with hideOnTablet=true, the CSS SHALL
     * contain a media query that hides the component on tablet breakpoint (600-1023px).
     * <p>
     * <b>Validates: Requirements 4.5</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 12: CSS hides component on tablet when hideOnTablet=true")
    void cssHidesComponentOnTabletWhenFlagSet(
            @ForAll("responsiveGridPropsWithHideOnTablet") ResponsiveGridProps props
    ) {
        final String gridId = "test-grid-" + System.nanoTime();
        final ResponsiveLayoutCssGenerator generator = new ResponsiveLayoutCssGenerator();
        final String css = generator.generateCSS(gridId, props);

        assertTrue(props.hideOnTablet(), "Test precondition: hideOnTablet should be true");

        // Verify tablet hide media query exists
        final Pattern tabletHidePattern = Pattern.compile(
                "@media\\s+\\(min-width:\\s*600px\\)\\s+and\\s+\\(max-width:\\s*1023px\\)\\s*\\{([^}]+)\\}",
                Pattern.DOTALL
        );
        final Matcher matcher = tabletHidePattern.matcher(css);
        assertTrue(matcher.find(),
                "CSS should contain @media (min-width: 600px) and (max-width: 1023px) block when hideOnTablet=true");

        final String mediaContent = matcher.group(1);
        assertTrue(DISPLAY_NONE_PATTERN.matcher(mediaContent).find(),
                "Tablet media query should contain 'display: none' when hideOnTablet=true");
    }

    /**
     * Property 12: For any ResponsiveGridProps with hideOnDesktop=true, the CSS SHALL
     * contain a media query that hides the component on desktop breakpoint (1024px+).
     * <p>
     * <b>Validates: Requirements 4.5</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 12: CSS hides component on desktop when hideOnDesktop=true")
    void cssHidesComponentOnDesktopWhenFlagSet(
            @ForAll("responsiveGridPropsWithHideOnDesktop") ResponsiveGridProps props
    ) {
        final String gridId = "test-grid-" + System.nanoTime();
        final ResponsiveLayoutCssGenerator generator = new ResponsiveLayoutCssGenerator();
        final String css = generator.generateCSS(gridId, props);

        assertTrue(props.hideOnDesktop(), "Test precondition: hideOnDesktop should be true");

        // Verify desktop hide media query exists
        final Pattern desktopHidePattern = Pattern.compile(
                "@media\\s+\\(min-width:\\s*1024px\\)\\s*\\{([^}]+)\\}",
                Pattern.DOTALL
        );
        final Matcher matcher = desktopHidePattern.matcher(css);
        assertTrue(matcher.find(),
                "CSS should contain @media (min-width: 1024px) block when hideOnDesktop=true");

        final String mediaContent = matcher.group(1);
        assertTrue(DISPLAY_NONE_PATTERN.matcher(mediaContent).find(),
                "Desktop media query should contain 'display: none' when hideOnDesktop=true");
    }

    /**
     * Property 12: For any ResponsiveGridProps with no conditional display flags set,
     * the CSS SHALL NOT contain any display: none declarations.
     * <p>
     * <b>Validates: Requirements 4.5</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 12: CSS does not hide component when no hide flags are set")
    void cssDoesNotHideComponentWhenNoFlagsSet(
            @ForAll("responsiveGridPropsWithNoHideFlags") ResponsiveGridProps props
    ) {
        final String gridId = "test-grid-" + System.nanoTime();
        final ResponsiveLayoutCssGenerator generator = new ResponsiveLayoutCssGenerator();
        final String css = generator.generateCSS(gridId, props);

        assertFalse(props.hideOnMobile(), "Test precondition: hideOnMobile should be false");
        assertFalse(props.hideOnTablet(), "Test precondition: hideOnTablet should be false");
        assertFalse(props.hideOnDesktop(), "Test precondition: hideOnDesktop should be false");

        // Verify no display: none declarations exist
        assertFalse(DISPLAY_NONE_PATTERN.matcher(css).find(),
                "CSS should not contain 'display: none' when no hide flags are set");
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<ResponsiveGridProps> responsiveGridProps() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 24),
                Arbitraries.of("0.5rem", "1rem", "1.5rem", "2rem", "16px", "24px"),
                breakpointsMap(),
                Arbitraries.of(true, false),
                Arbitraries.of(true, false),
                Arbitraries.of(true, false)
        ).as(ResponsiveGridProps::new);
    }

    @Provide
    Arbitrary<ResponsiveGridProps> responsiveGridPropsWithHideOnMobile() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 24),
                Arbitraries.of("0.5rem", "1rem", "1.5rem", "2rem"),
                breakpointsMap(),
                Arbitraries.just(true),  // hideOnMobile = true
                Arbitraries.of(true, false),
                Arbitraries.of(true, false)
        ).as(ResponsiveGridProps::new);
    }

    @Provide
    Arbitrary<ResponsiveGridProps> responsiveGridPropsWithHideOnTablet() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 24),
                Arbitraries.of("0.5rem", "1rem", "1.5rem", "2rem"),
                breakpointsMap(),
                Arbitraries.of(true, false),
                Arbitraries.just(true),  // hideOnTablet = true
                Arbitraries.of(true, false)
        ).as(ResponsiveGridProps::new);
    }

    @Provide
    Arbitrary<ResponsiveGridProps> responsiveGridPropsWithHideOnDesktop() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 24),
                Arbitraries.of("0.5rem", "1rem", "1.5rem", "2rem"),
                breakpointsMap(),
                Arbitraries.of(true, false),
                Arbitraries.of(true, false),
                Arbitraries.just(true)  // hideOnDesktop = true
        ).as(ResponsiveGridProps::new);
    }

    @Provide
    Arbitrary<ResponsiveGridProps> responsiveGridPropsWithNoHideFlags() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 24),
                Arbitraries.of("0.5rem", "1rem", "1.5rem", "2rem"),
                breakpointsMap(),
                Arbitraries.just(false),  // hideOnMobile = false
                Arbitraries.just(false),  // hideOnTablet = false
                Arbitraries.just(false)   // hideOnDesktop = false
        ).as(ResponsiveGridProps::new);
    }

    @Provide
    Arbitrary<Map<String, BreakpointConfig>> breakpointsMap() {
        final Arbitrary<Map.Entry<String, BreakpointConfig>> breakpointEntry = Combinators.combine(
                Arbitraries.of("mobile", "tablet", "desktop"),
                breakpointConfig()
        ).as(Map::entry);

        return breakpointEntry.list().ofMinSize(0).ofMaxSize(3)
                .map(entries -> entries.stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (a, b) -> b)));
    }

    @Provide
    Arbitrary<BreakpointConfig> breakpointConfig() {
        return Combinators.combine(
                Arbitraries.integers().between(0, 2000),
                Arbitraries.integers().between(1, 24),
                Arbitraries.of("0.5rem", "1rem", "1.5rem", "2rem", "16px", "24px")
        ).as(BreakpointConfig::new);
    }
}
