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
 * Represents the result of mapping a CEM type to Java.
 * <p>
 * Contains metadata about the Java type including whether imports are needed,
 * whether a record needs to be generated, and the record definition if applicable.
 * </p>
 *
 * @param javaType              the Java type string (e.g. "String", "Optional&lt;String&gt;", "List&lt;Double&gt;")
 * @param needsImport           whether this type requires an import statement
 * @param importPackage         the package to import from (e.g. "java.util" for Optional)
 * @param needsRecordGeneration whether a custom record class needs to be generated
 * @param recordDefinition      the record definition source code if needsRecordGeneration is true
 */
public record TypeMapping(
    String javaType,
    boolean needsImport,
    String importPackage,
    boolean needsRecordGeneration,
    String recordDefinition
) {
    /**
     * Creates a simple type mapping without imports or record generation.
     */
    public static TypeMapping simple(String javaType) {
        return new TypeMapping(javaType, false, null, false, null);
    }

    /**
     * Creates a type mapping that requires an import.
     */
    public static TypeMapping withImport(String javaType, String importPackage) {
        return new TypeMapping(javaType, true, importPackage, false, null);
    }

    /**
     * Creates a type mapping that requires record generation.
     */
    public static TypeMapping withRecordGeneration(String javaType, String recordDefinition) {
        return new TypeMapping(javaType, false, null, true, recordDefinition);
    }

    /**
     * Creates a fallback type mapping with a TODO comment.
     */
    public static TypeMapping fallbackWithTodo(String cemType, String reason) {
        String todoComment = String.format(
            "// TODO: %s type '%s' cannot be automatically mapped. %s Temporarily using Object.",
            reason,
            cemType,
            "Consider using a sealed interface or custom type."
        );
        return new TypeMapping("Object", false, null, false, todoComment);
    }

    /**
     * Returns whether this is a fallback mapping (Object type).
     */
    public boolean isFallback() {
        return "Object".equals(javaType);
    }
}
