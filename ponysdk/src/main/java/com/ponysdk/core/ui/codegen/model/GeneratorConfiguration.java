package com.ponysdk.core.ui.codegen.model;

import java.util.List;

/**
 * Top-level configuration for the web component wrapper generator.
 * 
 * @param libraries List of component libraries to process
 * @param output Output directory configuration
 * @param typeMappings Custom type mapping configuration
 * @param filters Component filtering configuration
 */
public record GeneratorConfiguration(
    List<LibraryConfig> libraries,
    OutputConfig output,
    TypeMappingConfig typeMappings,
    FilterConfig filters
) {}
