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

package com.ponysdk.core.ui.wa.codegen;

/**
 * Represents a single property of a Web Awesome component as parsed
 * from the Custom Elements Manifest.
 *
 * @param name         the property name (e.g. {@code "value"}, {@code "disabled"})
 * @param type         the JavaScript type from the manifest (e.g. {@code "string"}, {@code "boolean"})
 * @param javaType     the mapped Java type (e.g. {@code "String"}, {@code "boolean"}, or enum name like {@code "ButtonVariant"})
 * @param description  human-readable description of the property
 * @param defaultValue the default value as a string, or {@code null} if none
 * @param reflects     whether the property reflects as an HTML attribute
 * @param isEnum       {@code true} if the property type is a generated enum from union literal types
 * @param enumName     the name of the enum if {@code isEnum} is true (e.g. {@code "ButtonVariant"}), {@code null} otherwise
 * @param enumImport   the full import path if {@code isEnum} is true (e.g. {@code "com.ponysdk.core.ui.wa.enums.ButtonVariant"}), {@code null} otherwise
 */
public record PropertyDef(
    String name,
    String type,
    String javaType,
    String description,
    String defaultValue,
    boolean reflects,
    boolean isEnum,
    String enumName,
    String enumImport
) {
    /**
     * Creates a simple PropertyDef without enum metadata.
     * <p>
     * This factory method provides backward compatibility for existing code
     * that doesn't use enum fields.
     * </p>
     *
     * @param name         the property name
     * @param type         the JavaScript type from the manifest
     * @param javaType     the mapped Java type
     * @param description  human-readable description of the property
     * @param defaultValue the default value as a string, or {@code null} if none
     * @param reflects     whether the property reflects as an HTML attribute
     * @return a new PropertyDef with enum fields set to defaults (isEnum=false, enumName=null, enumImport=null)
     */
    public static PropertyDef simple(String name, String type, String javaType, 
                                     String description, String defaultValue, boolean reflects) {
        return new PropertyDef(name, type, javaType, description, defaultValue, reflects, 
                               false, null, null);
    }
}
