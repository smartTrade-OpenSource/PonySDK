package com.ponysdk.core.ui.codegen.model;

/**
 * Custom type mapping override for CEM types.
 * 
 * @param javaType Java type to use instead of default mapping
 * @param typescriptType TypeScript type to use instead of default mapping
 */
public record CustomTypeMapping(
    String javaType,
    String typescriptType
) {}
