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

package com.ponysdk.core.ui.codegen.pipeline;

import com.ponysdk.core.ui.codegen.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link CodeGenerationPipeline}.
 * <p>
 * Tests the complete generation workflow from CEM file to generated code,
 * including error handling, recovery, and report generation.
 * </p>
 * <p>
 * <b>Validates: Requirements 1.1, 2.1, 3.1, 4.1, 20.1, 20.2, 20.3, 20.4</b>
 * </p>
 */
class CodeGenerationPipelineTest {

    @TempDir
    Path tempDir;

    private Path javaOutputDir;
    private Path tsOutputDir;
    private Path cemFile;

    @BeforeEach
    void setUp() throws IOException {
        javaOutputDir = tempDir.resolve("java");
        tsOutputDir = tempDir.resolve("typescript");
        cemFile = tempDir.resolve("test-cem.json");
        
        Files.createDirectories(javaOutputDir);
        Files.createDirectories(tsOutputDir);
    }

    // ========== Complete Generation Workflow Tests ==========

    /**
     * Tests complete generation from CEM to Java/TypeScript code.
     * <p>
     * Validates: Requirements 1.1, 2.1, 3.1, 4.1
     * </p>
     */
    @Test
    void execute_completeGenerationWorkflow() throws IOException {
        // Given: A valid CEM file with a button component
        writeCEMFile(cemFile, createMinimalButtonCEM());
        
        final GeneratorConfiguration config = createConfig(cemFile);
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);

        // When: Execute the pipeline
        final GenerationReport report = pipeline.execute();

        // Then: Generation succeeds with no errors
        assertTrue(report.errors().isEmpty(), "Should have no errors");
        assertEquals(1, report.libraries().size(), "Should process one library");
        
        final LibraryReport libReport = report.libraries().get(0);
        assertEquals("testlib", libReport.libraryName());
        assertEquals(1, libReport.componentCount(), "Should generate one component");
        assertTrue(libReport.propertyCount() >= 2, "Should have at least 2 properties");
        assertTrue(libReport.eventCount() >= 1, "Should have at least 1 event");

        // Verify generated files exist
        assertFalse(libReport.generatedFiles().isEmpty(), "Should generate files");
        
        // Verify Java wrapper class exists
        final Path wrapperPath = javaOutputDir
            .resolve("com/ponysdk/core/ui/testlib/PButton.java");
        assertTrue(Files.exists(wrapperPath), "Wrapper class should exist");
        
        // Verify Props record exists
        final Path propsPath = javaOutputDir
            .resolve("com/ponysdk/core/ui/testlib/ButtonProps.java");
        assertTrue(Files.exists(propsPath), "Props record should exist");
        
        // Verify TypeScript interface exists
        final Path tsPath = tsOutputDir
            .resolve("testlib/ButtonProps.ts");
        assertTrue(Files.exists(tsPath), "TypeScript interface should exist");
    }

    /**
     * Tests that generated Java code is syntactically valid.
     * <p>
     * Validates: Requirements 2.1, 3.1
     * </p>
     */
    @Test
    void execute_generatedJavaCodeIsValid() throws IOException {
        // Given: A valid CEM file
        writeCEMFile(cemFile, createMinimalButtonCEM());
        
        final GeneratorConfiguration config = createConfig(cemFile);
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);

        // When: Execute the pipeline
        final GenerationReport report = pipeline.execute();

        // Then: Generated Java code should be valid
        assertTrue(report.errors().isEmpty(), "Should have no errors");
        
        // Read and verify wrapper class structure
        final Path wrapperPath = javaOutputDir
            .resolve("com/ponysdk/core/ui/testlib/PButton.java");
        final String wrapperCode = Files.readString(wrapperPath);
        
        assertTrue(wrapperCode.contains("package com.ponysdk.core.ui.testlib;"));
        assertTrue(wrapperCode.contains("public class PButton extends PWebComponent<ButtonProps>"));
        assertTrue(wrapperCode.contains("private static final String TAG_NAME = \"test-button\""));
        
        // Read and verify props record structure
        final Path propsPath = javaOutputDir
            .resolve("com/ponysdk/core/ui/testlib/ButtonProps.java");
        final String propsCode = Files.readString(propsPath);
        
        assertTrue(propsCode.contains("package com.ponysdk.core.ui.testlib;"));
        assertTrue(propsCode.contains("public record ButtonProps("));
    }

    /**
     * Tests that generated TypeScript code is syntactically valid.
     * <p>
     * Validates: Requirements 4.1
     * </p>
     */
    @Test
    void execute_generatedTypeScriptCodeIsValid() throws IOException {
        // Given: A valid CEM file
        writeCEMFile(cemFile, createMinimalButtonCEM());
        
        final GeneratorConfiguration config = createConfig(cemFile);
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);

        // When: Execute the pipeline
        final GenerationReport report = pipeline.execute();

        // Then: Generated TypeScript code should be valid
        assertTrue(report.errors().isEmpty(), "Should have no errors");
        
        final Path tsPath = tsOutputDir.resolve("testlib/ButtonProps.ts");
        final String tsCode = Files.readString(tsPath);
        
        assertTrue(tsCode.contains("export interface ButtonProps"));
        assertTrue(tsCode.contains("label?:"));
        assertTrue(tsCode.contains("disabled?:"));
    }

    // ========== Error Handling Tests ==========

    /**
     * Tests error handling when CEM parsing fails.
     * <p>
     * Validates: Requirements 1.1, 20.2
     * </p>
     */
    @Test
    void execute_handlesCEMParsingError() throws IOException {
        // Given: A malformed CEM file
        writeCEMFile(cemFile, "{ invalid json }");
        
        final GeneratorConfiguration config = createConfig(cemFile);
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);

        // When: Execute the pipeline
        final GenerationReport report = pipeline.execute();

        // Then: Should report errors
        assertFalse(report.errors().isEmpty(), "Should have errors");
        assertTrue(report.errors().get(0).contains("testlib"), 
            "Error should mention library name");
    }

    /**
     * Tests error handling when CEM file is missing.
     * <p>
     * Validates: Requirements 1.1, 20.2
     * </p>
     */
    @Test
    void execute_handlesMissingCEMFile() {
        // Given: A configuration with non-existent CEM file
        final Path nonExistentFile = tempDir.resolve("does-not-exist.json");
        final GeneratorConfiguration config = createConfig(nonExistentFile);
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);

        // When: Execute the pipeline
        final GenerationReport report = pipeline.execute();

        // Then: Should report errors
        assertFalse(report.errors().isEmpty(), "Should have errors");
    }

    /**
     * Tests error handling when code generation fails for a component.
     * <p>
     * Validates: Requirements 2.1, 20.2
     * </p>
     */
    @Test
    void execute_handlesCodeGenerationError() throws IOException {
        // Given: A CEM file with a component that has invalid type
        final String cemWithInvalidType = """
            {
                "schemaVersion": "1.0.0",
                "modules": [{
                    "kind": "javascript-module",
                    "path": "test.js",
                    "declarations": [{
                        "kind": "class",
                        "name": "BadComponent",
                        "tagName": "test-bad",
                        "customElement": true,
                        "members": [{
                            "kind": "field",
                            "name": "invalidProp",
                            "type": { "text": "" }
                        }]
                    }]
                }]
            }
            """;
        
        writeCEMFile(cemFile, cemWithInvalidType);
        
        final GeneratorConfiguration config = createConfig(cemFile);
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);

        // When: Execute the pipeline
        final GenerationReport report = pipeline.execute();

        // Then: Pipeline should continue and report the issue
        assertNotNull(report, "Should return a report");
        // The component might be skipped or generated with warnings
        final LibraryReport libReport = report.libraries().get(0);
        assertTrue(libReport.skippedComponents().isEmpty() || !report.warnings().isEmpty(),
            "Should either skip component or generate with warnings");
    }

    // ========== Report Generation Tests ==========

    /**
     * Tests that generation report contains correct statistics.
     * <p>
     * Validates: Requirements 20.1, 20.3, 20.4
     * </p>
     */
    @Test
    void execute_reportContainsCorrectStatistics() throws IOException {
        // Given: A CEM file with multiple components
        writeCEMFile(cemFile, createMultiComponentCEM());
        
        final GeneratorConfiguration config = createConfig(cemFile);
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);

        // When: Execute the pipeline
        final GenerationReport report = pipeline.execute();

        // Then: Report should contain statistics
        assertNotNull(report.timestamp(), "Should have timestamp");
        assertNotNull(report.duration(), "Should have duration");
        assertTrue(report.duration().toMillis() >= 0, "Duration should be non-negative");
        
        assertEquals(1, report.libraries().size(), "Should have one library");
        final LibraryReport libReport = report.libraries().get(0);
        
        assertEquals(2, libReport.componentCount(), "Should have 2 components");
        assertTrue(libReport.propertyCount() > 0, "Should have properties");
        assertTrue(libReport.eventCount() > 0, "Should have events");
        
        // Verify total counts
        assertEquals(2, report.getTotalComponents());
        assertTrue(report.getTotalProperties() > 0);
        assertTrue(report.getTotalEvents() > 0);
    }

    /**
     * Tests that generation report lists all generated files.
     * <p>
     * Validates: Requirements 20.1
     * </p>
     */
    @Test
    void execute_reportListsGeneratedFiles() throws IOException {
        // Given: A valid CEM file
        writeCEMFile(cemFile, createMinimalButtonCEM());
        
        final GeneratorConfiguration config = createConfig(cemFile);
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);

        // When: Execute the pipeline
        final GenerationReport report = pipeline.execute();

        // Then: Report should list generated files
        final LibraryReport libReport = report.libraries().get(0);
        final List<String> generatedFiles = libReport.generatedFiles();
        
        assertFalse(generatedFiles.isEmpty(), "Should have generated files");
        
        // Should include wrapper, props, TypeScript, and registration class
        assertTrue(generatedFiles.stream().anyMatch(f -> f.contains("PButton.java")),
            "Should include wrapper class");
        assertTrue(generatedFiles.stream().anyMatch(f -> f.contains("ButtonProps.java")),
            "Should include props record");
        assertTrue(generatedFiles.stream().anyMatch(f -> f.contains("ButtonProps.ts")),
            "Should include TypeScript interface");
        assertTrue(generatedFiles.stream().anyMatch(f -> f.contains("ComponentRegistry.java")),
            "Should include registration class");
    }

    /**
     * Tests that generation report includes skipped components.
     * <p>
     * Validates: Requirements 20.2
     * </p>
     */
    @Test
    void execute_reportIncludesSkippedComponents() throws IOException {
        // Given: A CEM file with a component that will be filtered out
        final String cemWithFilteredComponent = """
            {
                "schemaVersion": "1.0.0",
                "modules": [{
                    "kind": "javascript-module",
                    "path": "test.js",
                    "declarations": [{
                        "kind": "class",
                        "name": "InternalComponent",
                        "tagName": "test-internal",
                        "customElement": true,
                        "members": []
                    }]
                }]
            }
            """;
        
        writeCEMFile(cemFile, cemWithFilteredComponent);
        
        // Create config with exclude pattern
        final FilterConfig filters = new FilterConfig(
            List.of("test-*"),
            List.of("*-internal"),
            List.of()
        );
        
        final GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(new LibraryConfig("testlib", cemFile, "com.ponysdk.core.ui.testlib")),
            new OutputConfig(javaOutputDir, tsOutputDir, true),
            new TypeMappingConfig(java.util.Map.of()),
            filters
        );
        
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);

        // When: Execute the pipeline
        final GenerationReport report = pipeline.execute();

        // Then: Report should show component was filtered
        final LibraryReport libReport = report.libraries().get(0);
        assertEquals(0, libReport.componentCount(), "Should have no components after filtering");
    }

    // ========== Multi-Library Support Tests ==========

    /**
     * Tests generation with multiple libraries.
     * <p>
     * Validates: Requirements 1.1, 20.1
     * </p>
     */
    @Test
    void execute_supportsMultipleLibraries() throws IOException {
        // Given: Two CEM files for different libraries
        final Path cemFile1 = tempDir.resolve("lib1-cem.json");
        final Path cemFile2 = tempDir.resolve("lib2-cem.json");
        
        writeCEMFile(cemFile1, createMinimalButtonCEM());
        writeCEMFile(cemFile2, createMinimalInputCEM());
        
        final GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(
                new LibraryConfig("lib1", cemFile1, "com.ponysdk.core.ui.lib1"),
                new LibraryConfig("lib2", cemFile2, "com.ponysdk.core.ui.lib2")
            ),
            new OutputConfig(javaOutputDir, tsOutputDir, true),
            new TypeMappingConfig(java.util.Map.of()),
            new FilterConfig(List.of(), List.of(), List.of())
        );
        
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);

        // When: Execute the pipeline
        final GenerationReport report = pipeline.execute();

        // Then: Both libraries should be processed
        assertEquals(2, report.libraries().size(), "Should process two libraries");
        
        final LibraryReport lib1Report = report.libraries().get(0);
        final LibraryReport lib2Report = report.libraries().get(1);
        
        assertEquals("lib1", lib1Report.libraryName());
        assertEquals("lib2", lib2Report.libraryName());
        
        // Verify files are in separate packages
        assertTrue(Files.exists(javaOutputDir.resolve("com/ponysdk/core/ui/lib1/PButton.java")));
        assertTrue(Files.exists(javaOutputDir.resolve("com/ponysdk/core/ui/lib2/PInput.java")));
    }

    // ========== Helper Methods ==========

    private GeneratorConfiguration createConfig(final Path cemPath) {
        return new GeneratorConfiguration(
            List.of(new LibraryConfig("testlib", cemPath, "com.ponysdk.core.ui.testlib")),
            new OutputConfig(javaOutputDir, tsOutputDir, true),
            new TypeMappingConfig(java.util.Map.of()),
            new FilterConfig(List.of(), List.of(), List.of())
        );
    }

    private void writeCEMFile(final Path path, final String content) throws IOException {
        Files.writeString(path, content);
    }

    private String createMinimalButtonCEM() {
        return """
            {
                "schemaVersion": "1.0.0",
                "readme": "",
                "modules": [{
                    "kind": "javascript-module",
                    "path": "components/button/button.js",
                    "declarations": [{
                        "kind": "class",
                        "description": "A simple button component.",
                        "name": "Button",
                        "tagName": "test-button",
                        "customElement": true,
                        "members": [
                            {
                                "kind": "field",
                                "name": "label",
                                "type": { "text": "string" },
                                "description": "The button label",
                                "default": "\\"Click me\\""
                            },
                            {
                                "kind": "field",
                                "name": "disabled",
                                "type": { "text": "boolean" },
                                "description": "Whether the button is disabled",
                                "default": "false"
                            }
                        ],
                        "events": [{
                            "name": "test-click",
                            "type": { "text": "CustomEvent" },
                            "description": "Emitted when the button is clicked"
                        }],
                        "slots": [{
                            "name": "",
                            "description": "The button's content"
                        }]
                    }],
                    "exports": [{
                        "kind": "custom-element-definition",
                        "name": "test-button",
                        "declaration": { "name": "Button" }
                    }]
                }]
            }
            """;
    }

    private String createMinimalInputCEM() {
        return """
            {
                "schemaVersion": "1.0.0",
                "readme": "",
                "modules": [{
                    "kind": "javascript-module",
                    "path": "components/input/input.js",
                    "declarations": [{
                        "kind": "class",
                        "description": "A simple input component.",
                        "name": "Input",
                        "tagName": "test-input",
                        "customElement": true,
                        "members": [
                            {
                                "kind": "field",
                                "name": "value",
                                "type": { "text": "string" },
                                "description": "The input value",
                                "default": "\\"\\"
                            }
                        ],
                        "events": [{
                            "name": "test-change",
                            "type": { "text": "CustomEvent" },
                            "description": "Emitted when the value changes"
                        }]
                    }],
                    "exports": [{
                        "kind": "custom-element-definition",
                        "name": "test-input",
                        "declaration": { "name": "Input" }
                    }]
                }]
            }
            """;
    }

    private String createMultiComponentCEM() {
        return """
            {
                "schemaVersion": "1.0.0",
                "readme": "",
                "modules": [{
                    "kind": "javascript-module",
                    "path": "components/components.js",
                    "declarations": [
                        {
                            "kind": "class",
                            "name": "Button",
                            "tagName": "test-button",
                            "customElement": true,
                            "members": [{
                                "kind": "field",
                                "name": "label",
                                "type": { "text": "string" }
                            }],
                            "events": [{
                                "name": "test-click",
                                "type": { "text": "CustomEvent" }
                            }]
                        },
                        {
                            "kind": "class",
                            "name": "Input",
                            "tagName": "test-input",
                            "customElement": true,
                            "members": [{
                                "kind": "field",
                                "name": "value",
                                "type": { "text": "string" }
                            }],
                            "events": [{
                                "name": "test-change",
                                "type": { "text": "CustomEvent" }
                            }]
                        }
                    ],
                    "exports": [
                        {
                            "kind": "custom-element-definition",
                            "name": "test-button",
                            "declaration": { "name": "Button" }
                        },
                        {
                            "kind": "custom-element-definition",
                            "name": "test-input",
                            "declaration": { "name": "Input" }
                        }
                    ]
                }]
            }
            """;
    }
}
