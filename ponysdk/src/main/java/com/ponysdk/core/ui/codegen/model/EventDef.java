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
 * Definition of a component event parsed from a Custom Elements Manifest.
 *
 * @param name        the event name (e.g. {@code "wa-change"}, {@code "sl-input"})
 * @param description human-readable description of when the event fires
 * @param detailType  the type of the event's detail payload, or {@code null} if none
 * @param bubbles     whether the event bubbles up through the DOM
 * @param cancelable  whether the event can be canceled
 */
public record EventDef(
    String name,
    String description,
    String detailType,
    boolean bubbles,
    boolean cancelable
) {
    /**
     * Returns the Java method name for this event listener.
     * <p>
     * Converts the event name to a camelCase method name prefixed with "add" and suffixed with "Listener".
     * For example: {@code "wa-click"} → {@code "addClickListener"}
     * </p>
     *
     * @return the listener method name
     */
    public String getListenerMethodName() {
        // Remove library prefix (e.g., "wa-", "sl-")
        String eventName = name.replaceFirst("^[a-z]+-", "");
        return "add" + capitalize(kebabToCamelCase(eventName)) + "Listener";
    }

    /**
     * Returns whether this event needs a custom detail class.
     * <p>
     * A custom detail class is needed if the event has a non-empty detail type
     * that is not void or an empty object.
     * </p>
     *
     * @return {@code true} if a custom detail class should be generated
     */
    public boolean needsDetailClass() {
        return detailType != null && !detailType.isEmpty() &&
               !detailType.equals("void") && !detailType.equals("{}");
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
