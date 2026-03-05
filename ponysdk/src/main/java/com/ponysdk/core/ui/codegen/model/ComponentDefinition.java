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

package com.ponysdk.core.ui.codegen.model;

import java.util.List;

/**
 * Complete definition of a web component parsed from a Custom Elements Manifest.
 * This is a library-agnostic model that can represent components from any
 * web component library (Web Awesome, Shoelace, Material Web Components, etc.).
 *
 * @param tagName       the HTML custom element tag (e.g. {@code "wa-button"}, {@code "sl-button"})
 * @param className     the class name from the manifest (e.g. {@code "Button"})
 * @param summary       short description of the component
 * @param description   full documentation extracted from the manifest
 * @param properties    list of the component's properties
 * @param events        list of events emitted by the component
 * @param slots         list of named slots for content composition
 * @param methods       list of public methods exposed by the component
 * @param cssProperties list of CSS custom properties supported by the component
 * @param status        component maturity: {@code "stable"}, {@code "experimental"}, or {@code "deprecated"}
 */
public record ComponentDefinition(
    String tagName,
    String className,
    String summary,
    String description,
    List<PropertyDef> properties,
    List<EventDef> events,
    List<SlotDef> slots,
    List<MethodDef> methods,
    List<CssPropertyDef> cssProperties,
    String status
) {
    /**
     * Returns the Java wrapper class name.
     * <p>
     * Converts the tag name to a PascalCase class name prefixed with "P".
     * For example: {@code "wa-button"} → {@code "PButton"}
     * </p>
     *
     * @return the wrapper class name
     */
    public String getWrapperClassName() {
        // Remove library prefix (e.g., "wa-", "sl-")
        String nameWithoutPrefix = tagName.replaceFirst("^[a-z]+-", "");
        return "P" + capitalize(kebabToCamelCase(nameWithoutPrefix));
    }

    /**
     * Returns the props record class name.
     * <p>
     * Converts the tag name to a PascalCase class name suffixed with "Props".
     * For example: {@code "wa-button"} → {@code "ButtonProps"}
     * </p>
     *
     * @return the props record class name
     */
    public String getPropsClassName() {
        String nameWithoutPrefix = tagName.replaceFirst("^[a-z]+-", "");
        return capitalize(kebabToCamelCase(nameWithoutPrefix)) + "Props";
    }

    /**
     * Returns properties that are not private.
     * <p>
     * Filters out properties marked as private, returning only public and protected properties
     * that should be included in the generated props record.
     * </p>
     *
     * @return list of public properties
     */
    public List<PropertyDef> getPublicProperties() {
        return properties.stream()
            .filter(p -> !"private".equals(p.privacy()))
            .toList();
    }

    /**
     * Converts kebab-case to camelCase.
     * <p>
     * For example: {@code "my-component"} → {@code "myComponent"}
     * </p>
     *
     * @param kebab the kebab-case string
     * @return the camelCase string
     */
    private static String kebabToCamelCase(String kebab) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;

        for (char c : kebab.toCharArray()) {
            if (c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Capitalizes the first character of a string.
     *
     * @param str the string to capitalize
     * @return the capitalized string
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
