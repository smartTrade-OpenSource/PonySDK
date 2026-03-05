package com.ponysdk.core.ui.codegen.model;

/**
 * Result of mapping a CEM type to Java, including import and generation metadata.
 * 
 * @param javaType The Java type string (e.g., "String", "List&lt;Double&gt;", "ButtonSize")
 * @param needsImport Whether this type requires an import statement
 * @param importPackage The package to import from (null if needsImport is false)
 * @param needsRecordGeneration Whether a record class needs to be generated for this type
 * @param recordDefinition The record definition source code (null if needsRecordGeneration is false)
 */
public record TypeMapping(
    String javaType,
    boolean needsImport,
    String importPackage,
    boolean needsRecordGeneration,
    String recordDefinition
) {}
