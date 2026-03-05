package com.ponysdk.core.ui.codegen.model;

import java.util.Map;

/**
 * Configuration for custom type mappings.
 * 
 * @param customMappings Map of CEM type patterns to custom type mappings
 */
public record TypeMappingConfig(
    Map<String, CustomTypeMapping> customMappings
) {}
