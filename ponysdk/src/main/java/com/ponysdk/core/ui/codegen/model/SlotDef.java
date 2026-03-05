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

/**
 * Definition of a component slot parsed from a Custom Elements Manifest.
 *
 * @param name        the slot name (e.g. {@code "prefix"}, {@code "suffix"}), or empty string for the default slot
 * @param description human-readable description of the slot's purpose
 */
public record SlotDef(
    String name,
    String description
) {
    /**
     * Returns whether this is the default slot.
     * <p>
     * The default slot has no name (null or empty string).
     * </p>
     *
     * @return {@code true} if this is the default slot
     */
    public boolean isDefaultSlot() {
        return name == null || name.isEmpty();
    }

    /**
     * Returns the Java method name for this slot.
     * <p>
     * Converts the slot name to a camelCase method name prefixed with "add".
     * For the default slot, returns {@code "addContent"}.
     * For example: {@code "prefix"} → {@code "addPrefix"}
     * </p>
     *
     * @return the slot method name
     */
    public String getSlotMethodName() {
        if (isDefaultSlot()) {
            return "addContent";
        }
        return "add" + capitalize(kebabToCamelCase(name));
    }

    /**
     * Converts kebab-case to camelCase.
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
