package com.ponysdk.core.ui.codegen.model;

import java.nio.file.Path;

/**
 * Configuration for a single component library.
 * 
 * @param name Library identifier (e.g., "webawesome", "shoelace")
 * @param cemPath Path to the Custom Elements Manifest JSON file
 * @param javaPackage Base Java package for generated classes (e.g., "com.ponysdk.core.ui.webawesome")
 */
public record LibraryConfig(
    String name,
    Path cemPath,
    String javaPackage
) {}
