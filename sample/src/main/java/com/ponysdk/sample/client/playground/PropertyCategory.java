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

/**
 * Categories for grouping component properties.
 * <p>
 * Display order is defined by enum ordinal:
 * APPEARANCE → CONTENT → LAYOUT → BEHAVIOR → OTHER.
 * </p>
 */
public enum PropertyCategory {

    APPEARANCE("Appearance"),
    CONTENT("Content"),
    LAYOUT("Layout"),
    BEHAVIOR("Behavior"),
    OTHER("Other");

    private final String displayName;

    PropertyCategory(final String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Determines the category for a given parameter type.
     *
     * @param parameterInfo the parameter info, must not be null
     * @return the category, never null
     * @throws IllegalArgumentException if parameterInfo is null
     */
    public static PropertyCategory fromParameterInfo(final ParameterInfo parameterInfo) {
        if (parameterInfo == null) {
            throw new IllegalArgumentException("parameterInfo must not be null");
        }

        final Class<?> type = parameterInfo.type();

        if (type == boolean.class || type == Boolean.class) return BEHAVIOR;
        if (type.isEnum()) return APPEARANCE;
        if (type == String.class) return CONTENT;
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) return LAYOUT;

        return OTHER;
    }
}
