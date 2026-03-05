package com.ponysdk.core.ui.codegen.model;

import java.nio.file.Path;

/**
 * Configuration for code generation output directories.
 * 
 * @param javaOutputDir Directory for generated Java source files
 * @param typescriptOutputDir Directory for generated TypeScript files
 * @param generateReports Whether to generate detailed generation reports
 */
public record OutputConfig(
    Path javaOutputDir,
    Path typescriptOutputDir,
    boolean generateReports
) {}
