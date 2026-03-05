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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Generates CSS for responsive grid layouts with media queries per breakpoint,
 * and conditional display rules for hiding components at specific breakpoints.
 * <p>
 * This is the server-side equivalent of the TypeScript ResponsiveGridRenderer.
 * It generates CSS that will be injected into the client-side document.
 * </p>
 * <p>
 * Requirements: 4.2 - 12-column grid with configurable breakpoints<br>
 * Requirements: 4.3 - Reorganize layout when crossing a breakpoint<br>
 * Requirements: 4.5 - Conditional display based on active breakpoint<br>
 * Requirements: 4.6 - hideOnMobile/hideOnTablet/hideOnDesktop support
 * </p>
 */
public class ResponsiveLayoutCssGenerator {

    /**
     * Generate a complete CSS string for a responsive grid identified by {@code gridId}.
     * <p>
     * The output includes:
     * <ul>
     *   <li>A base grid rule using the default columns/gap</li>
     *   <li>A {@code @media} rule for each breakpoint overriding columns/gap</li>
     *   <li>Conditional display {@code @media} rules for hideOnMobile/hideOnTablet/hideOnDesktop</li>
     * </ul>
     * </p>
     *
     * @param gridId unique identifier for this grid instance
     * @param props  responsive grid properties
     * @return complete CSS string with all rules and media queries
     */
    public String generateCSS(final String gridId, final ResponsiveGridProps props) {
        final String selector = "[data-grid-id=\"" + gridId + "\"]";
        final List<String> parts = new ArrayList<>();

        // Base grid styles (no media query)
        parts.add(generateBaseGridStyles(selector, props));

        // Media queries for each breakpoint, sorted by minWidth ascending
        final List<Map.Entry<String, BreakpointConfig>> sortedBreakpoints = props.breakpoints().entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().minWidth()))
                .toList();

        for (final Map.Entry<String, BreakpointConfig> entry : sortedBreakpoints) {
            parts.add(generateBreakpointMediaQuery(selector, entry.getValue()));
        }

        // Conditional display media queries
        parts.addAll(generateConditionalDisplayCSS(selector, props));

        return String.join("\n\n", parts);
    }

    /**
     * Generate the base grid styles without media queries.
     */
    private String generateBaseGridStyles(final String selector, final ResponsiveGridProps props) {
        return selector + " {\n" +
                "  display: grid;\n" +
                "  grid-template-columns: repeat(" + props.columns() + ", 1fr);\n" +
                "  gap: " + props.gap() + ";\n" +
                "}";
    }

    /**
     * Generate a single {@code @media} block for a breakpoint configuration.
     */
    private String generateBreakpointMediaQuery(final String selector, final BreakpointConfig config) {
        return "@media (min-width: " + config.minWidth() + "px) {\n" +
                "  " + selector + " {\n" +
                "    grid-template-columns: repeat(" + config.columns() + ", 1fr);\n" +
                "    gap: " + config.gap() + ";\n" +
                "  }\n" +
                "}";
    }

    /**
     * Generate media queries that hide the grid at matching breakpoints.
     */
    private List<String> generateConditionalDisplayCSS(final String selector, final ResponsiveGridProps props) {
        final List<String> rules = new ArrayList<>();

        if (props.hideOnMobile()) {
            rules.add(
                    "@media (max-width: 599px) {\n" +
                            "  " + selector + " {\n" +
                            "    display: none;\n" +
                            "  }\n" +
                            "}"
            );
        }

        if (props.hideOnTablet()) {
            rules.add(
                    "@media (min-width: 600px) and (max-width: 1023px) {\n" +
                            "  " + selector + " {\n" +
                            "    display: none;\n" +
                            "  }\n" +
                            "}"
            );
        }

        if (props.hideOnDesktop()) {
            rules.add(
                    "@media (min-width: 1024px) {\n" +
                            "  " + selector + " {\n" +
                            "    display: none;\n" +
                            "  }\n" +
                            "}"
            );
        }

        return rules;
    }
}
