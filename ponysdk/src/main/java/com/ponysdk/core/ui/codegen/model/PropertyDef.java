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
 * Definition of a component property parsed from a Custom Elements Manifest.
 *
 * @param name         the property name (e.g. {@code "variant"}, {@code "disabled"})
 * @param cemType      the CEM type string (e.g. {@code "string"}, {@code "boolean"}, {@code "string | undefined"})
 * @param javaType     the mapped Java type (e.g. {@code "String"}, {@code "boolean"}, {@code "Optional<String>"})
 * @param tsType       the mapped TypeScript type (e.g. {@code "string"}, {@code "boolean"}, {@code "string | undefined"})
 * @param description  human-readable description of the property
 * @param defaultValue the default value as a string, or {@code null} if none
 * @param required     whether the property is required
 * @param privacy      the property visibility: {@code "public"}, {@code "protected"}, or {@code "private"}
 */
public record PropertyDef(
    String name,
    String cemType,
    String javaType,
    String tsType,
    String description,
    String defaultValue,
    boolean required,
    String privacy
) {
    /**
     * Returns whether this property should be Optional in Java.
     * <p>
     * A property is optional if it's not required and the Java type doesn't already
     * start with "Optional".
     * </p>
     *
     * @return {@code true} if the property should be wrapped in Optional
     */
    public boolean isOptional() {
        return !required && !javaType.startsWith("Optional");
    }
}
