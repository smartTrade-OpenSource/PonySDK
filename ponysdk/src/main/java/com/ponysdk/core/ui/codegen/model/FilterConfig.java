package com.ponysdk.core.ui.codegen.model;

import java.util.List;

/**
 * Configuration for filtering which components to generate.
 * 
 * @param includePatterns Glob patterns for components to include (e.g., "wa-*", "sl-*")
 * @param excludePatterns Glob patterns for components to exclude (e.g., "*-internal")
 * @param skipComponents Explicit list of component tag names to skip
 */
public record FilterConfig(
    List<String> includePatterns,
    List<String> excludePatterns,
    List<String> skipComponents
) {}
