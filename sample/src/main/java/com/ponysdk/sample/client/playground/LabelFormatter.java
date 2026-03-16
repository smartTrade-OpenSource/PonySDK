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
 * Transforms Java method names into human-readable labels.
 * <p>
 * Stateless utility class. Thread-safe.
 * </p>
 * <p>
 * Rules:
 * <ul>
 *   <li>Removes "set" or "is" prefix if present (keeps original if removal yields empty)</li>
 *   <li>Splits camelCase words with spaces</li>
 *   <li>Capitalizes the first letter of each word</li>
 *   <li>Returns empty string for empty input</li>
 *   <li>Idempotent: format(format(x)) == format(x)</li>
 * </ul>
 * </p>
 */
public final class LabelFormatter {

    private LabelFormatter() {
        // Utility class
    }

    /**
     * Formats a Java method name into a human-readable label.
     *
     * @param methodName the method name to format, must not be null
     * @return the formatted label
     * @throws IllegalArgumentException if methodName is null
     */
    public static String format(final String methodName) {
        if (methodName == null) {
            throw new IllegalArgumentException("methodName must not be null");
        }
        if (methodName.isEmpty()) {
            return "";
        }

        // Strip prefix
        String stripped = stripPrefix(methodName);

        // Split camelCase and capitalize
        return splitCamelCase(stripped);
    }

    private static String stripPrefix(final String name) {
        if (name.startsWith("set") && name.length() > 3 && Character.isUpperCase(name.charAt(3))) {
            return name.substring(3);
        }
        if (name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2))) {
            return name.substring(2);
        }
        return name;
    }

    private static String splitCamelCase(final String name) {
        if (name.isEmpty()) {
            return "";
        }

        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && name.charAt(i - 1) != ' '
                && !Character.isUpperCase(name.charAt(i - 1))) {
                result.append(' ');
            }
            result.append(i == 0 ? Character.toUpperCase(c) : c);
        }
        return result.toString();
    }
}
