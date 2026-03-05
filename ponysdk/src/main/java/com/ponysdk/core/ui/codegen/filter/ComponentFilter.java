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

package com.ponysdk.core.ui.codegen.filter;

import com.ponysdk.core.ui.codegen.model.ComponentDefinition;
import com.ponysdk.core.ui.codegen.model.FilterConfig;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Filters components based on include/exclude patterns and skip lists.
 * <p>
 * Supports glob-style patterns for filtering component tag names.
 * </p>
 */
public class ComponentFilter {

    private final List<Pattern> includePatterns;
    private final List<Pattern> excludePatterns;
    private final List<String> skipComponents;

    /**
     * Creates a new component filter from configuration.
     *
     * @param config the filter configuration
     */
    public ComponentFilter(final FilterConfig config) {
        this.includePatterns = config.includePatterns().stream()
            .map(ComponentFilter::globToRegex)
            .map(Pattern::compile)
            .collect(Collectors.toList());
        
        this.excludePatterns = config.excludePatterns().stream()
            .map(ComponentFilter::globToRegex)
            .map(Pattern::compile)
            .collect(Collectors.toList());
        
        this.skipComponents = config.skipComponents();
    }

    /**
     * Filters a list of component definitions.
     *
     * @param components the components to filter
     * @return filtered list of components
     */
    public List<ComponentDefinition> filter(final List<ComponentDefinition> components) {
        return components.stream()
            .filter(this::shouldInclude)
            .collect(Collectors.toList());
    }

    /**
     * Checks if a component should be included based on filter rules.
     *
     * @param component the component to check
     * @return true if component should be included
     */
    public boolean shouldInclude(final ComponentDefinition component) {
        final String tagName = component.tagName();
        
        // Check skip list first
        if (skipComponents.contains(tagName)) {
            return false;
        }
        
        // Check exclude patterns
        for (final Pattern pattern : excludePatterns) {
            if (pattern.matcher(tagName).matches()) {
                return false;
            }
        }
        
        // If no include patterns, include by default
        if (includePatterns.isEmpty()) {
            return true;
        }
        
        // Check include patterns
        for (final Pattern pattern : includePatterns) {
            if (pattern.matcher(tagName).matches()) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Converts a glob pattern to a regex pattern.
     * Supports * (any characters) and ? (single character).
     *
     * @param glob the glob pattern
     * @return the regex pattern
     */
    private static String globToRegex(final String glob) {
        final StringBuilder regex = new StringBuilder("^");
        
        for (int i = 0; i < glob.length(); i++) {
            final char c = glob.charAt(i);
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                case '[':
                case ']':
                case '{':
                case '}':
                case '\\':
                    regex.append('\\').append(c);
                    break;
                default:
                    regex.append(c);
                    break;
            }
        }
        
        regex.append('$');
        return regex.toString();
    }
}
