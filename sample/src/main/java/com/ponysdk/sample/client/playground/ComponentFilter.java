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

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for filtering component lists based on search queries.
 * <p>
 * Provides case-insensitive substring matching for component names.
 * </p>
 * <p>
 * Requirements: 2.1, 2.3, 2.4, 2.5, 2.6
 * </p>
 */
public class ComponentFilter {

    /**
     * Private constructor to prevent instantiation.
     */
    private ComponentFilter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Filters a list of component names based on a search query.
     * <p>
     * Returns only components where the component name contains the query
     * as a case-insensitive substring. If the query is null or empty,
     * returns all components.
     * </p>
     *
     * @param components the list of component names to filter, must not be null
     * @param query      the search query, may be null or empty
     * @return a new list containing only matching components
     * @throws IllegalArgumentException if components is null
     */
    public static List<String> filter(final List<String> components, final String query) {
        if (components == null) {
            throw new IllegalArgumentException("components must not be null");
        }

        final List<String> filtered = new ArrayList<>();
        
        for (final String component : components) {
            if (matches(component, query)) {
                filtered.add(component);
            }
        }
        
        return filtered;
    }

    /**
     * Tests if a component name matches a search query.
     * <p>
     * Performs case-insensitive substring matching. Returns true if:
     * <ul>
     *   <li>The query is null or empty/whitespace</li>
     *   <li>The component name contains the query (case-insensitive)</li>
     * </ul>
     * </p>
     *
     * @param componentName the component name to test, must not be null
     * @param query         the search query, may be null or empty
     * @return true if the component matches the query
     * @throws IllegalArgumentException if componentName is null
     */
    public static boolean matches(final String componentName, final String query) {
        if (componentName == null) {
            throw new IllegalArgumentException("componentName must not be null");
        }

        if (query == null || query.trim().isEmpty()) {
            return true;
        }

        return componentName.toLowerCase().contains(query.toLowerCase().trim());
    }
}
