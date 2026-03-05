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
 * Definition of a CSS custom property parsed from a Custom Elements Manifest.
 *
 * @param name         the CSS custom property name (e.g. {@code "--button-background"})
 * @param description  human-readable description of the property's effect
 * @param syntax       the CSS syntax (e.g. {@code "<color>"}, {@code "<length>"})
 * @param defaultValue the default value, or {@code null} if none
 */
public record CssPropertyDef(
    String name,
    String description,
    String syntax,
    String defaultValue
) {
    /**
     * Returns the Java constant name for this CSS property.
     * <p>
     * Converts the CSS property name to an uppercase constant name.
     * For example: {@code "--button-background"} → {@code "CSS_BUTTON_BACKGROUND"}
     * </p>
     *
     * @return the constant name
     */
    public String getConstantName() {
        return "CSS_" + name.replaceFirst("^--", "")
            .replaceAll("-", "_")
            .toUpperCase();
    }
}
