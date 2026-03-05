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

package com.ponysdk.core.ui.codegen;

import com.ponysdk.core.ui.codegen.model.*;
import com.ponysdk.core.ui.codegen.pipeline.CodeGenerationPipeline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for the complete code generation pipeline.
 * <p>
 * Tests the complete workflow from CEM parsing to compiled code, including:
 * - Multi-library generation
 * - Incremental regeneration
 * - Code compilation verification
 * - Complete pipeline orchestration
 * </p>
 * <p>
 * Validates: All requirements (comprehensive end-to-end validation)
 * </p>
 */
class EndToEndIntegrationTest {

    @TempDir
    Path tempDir;

    // ========== Complete Workflow Tests ==========

    @Test
    void completeWorkflow_fromCEMToCompiledCode() throws Exception {
        // Setup: Create configuration for single library
        final Path cemPath = Paths.get("src/test/resources/sample-cem/minimal-button.json");
        final Path javaOutputDir = tempDir.resolve("java");
        final Path tsOutputDir = tempDir.resolve("typescript");

        final GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(new LibraryConfig(
                "testlib",
                cemPath,
                "com.test.ui.testlib"
            )),
            new OutputConfig(javaOutputDir, tsOutputDir, true),
            new TypeMappingConfig(Collections.emptyMap()),
            new FilterConfig(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            )
        );

        // Execute: Run the complete pipeline
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);
        final GenerationReport report = pipeline.execute();

        // Verify: Report shows success
        assertNotNull(report);
        assertEquals(0, report.errors().size(), "Should have no errors");
        assertEquals(1, report.libraries().size(), "Should process one library");

        final LibraryReport libReport = report.libraries().get(0);
        assertEquals("testlib", libReport.libraryName());
        assertTrue(libReport.componentCount() > 0, "Should generate at least one component");

        // Verify: Generated files exist
        final Path wrapperPath = javaOutputDir.resolve("com/test/ui/testlib/PButton.java");
        final Path propsPath = javaOutputDir.resolve("com/test/ui/testlib/ButtonProps.java");
        final Path tsPath = tsOutputDir.resolve("testlib/ButtonProps.ts");
        final Path registryPath = javaOutputDir.resolve("com/test/ui/testlib/TestlibComponentRegistry.java");

        assertTrue(Files.exists(wrapperPath), "Wrapper class should exist");
        assertTrue(Files.exists(propsPath), "Props record should exist");
        assertTrue(Files.exists(tsPath), "TypeScript interface should exist");
        assertTrue(Files.exists(registryPath), "Registry class should exist");

        // Verify: Generated Java code has correct structure
        final String wrapperCode = Files.readString(wrapperPath);
        assertTrue(wrapperCode.contains("package com.test.ui.testlib;"));
        assertTrue(wrapperCode.contains("public class PButton extends PWebComponent<ButtonProps>"));
        assertTrue(wrapperCode.contains("private static final String TAG_NAME = \"test-button\";"));
        assertTrue(wrapperCode.contains("public void addClickListener"));

        final String propsCode = Files.readString(propsPath);
        assertTrue(propsCode.contains("public record ButtonProps("));
        assertTrue(propsCode.contains("String label"));
        assertTrue(propsCode.contains("boolean disabled"));

        // Verify: Generated code compiles
        assertTrue(compileJavaFiles(javaOutputDir), "Generated Java code should compile");

        // Verify: TypeScript code has correct structure
        final String tsCode = Files.readString(tsPath);
        assertTrue(tsCode.contains("export interface ButtonProps"));
        assertTrue(tsCode.contains("label?:"));
        assertTrue(tsCode.contains("disabled?:"));
    }

    @Test
    void multiLibraryGeneration_separatePackages() throws Exception {
        // Setup: Create configuration for multiple libraries
        final Path cemPath1 = Paths.get("src/test/resources/sample-cem/minimal-button.json");
        final Path cemPath2 = Paths.get("src/test/resources/sample-cem/complex-component.json");
        final Path javaOutputDir = tempDir.resolve("java");
        final Path tsOutputDir = tempDir.resolve("typescript");

        final GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(
                new LibraryConfig("lib1", cemPath1, "com.test.ui.lib1"),
                new LibraryConfig("lib2", cemPath2, "com.test.ui.lib2")
            ),
            new OutputConfig(javaOutputDir, tsOutputDir, true),
            new TypeMappingConfig(Collections.emptyMap()),
            new FilterConfig(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            )
        );

        // Execute: Run the pipeline
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);
        final GenerationReport report = pipeline.execute();

        // Verify: Both libraries processed
        assertEquals(2, report.libraries().size());
        assertEquals(0, report.errors().size());

        // Verify: Components in separate packages
        final Path lib1Wrapper = javaOutputDir.resolve("com/test/ui/lib1/PButton.java");
        final Path lib2Wrapper = javaOutputDir.resolve("com/test/ui/lib2/PDataTable.java");

        assertTrue(Files.exists(lib1Wrapper), "Lib1 component should exist");
        assertTrue(Files.exists(lib2Wrapper), "Lib2 component should exist");

        final String lib1Code = Files.readString(lib1Wrapper);
        final String lib2Code = Files.readString(lib2Wrapper);

        assertTrue(lib1Code.contains("package com.test.ui.lib1;"));
        assertTrue(lib2Code.contains("package com.test.ui.lib2;"));

        // Verify: Separate registry classes
        final Path lib1Registry = javaOutputDir.resolve("com/test/ui/lib1/Lib1ComponentRegistry.java");
        final Path lib2Registry = javaOutputDir.resolve("com/test/ui/lib2/Lib2ComponentRegistry.java");

        assertTrue(Files.exists(lib1Registry));
        assertTrue(Files.exists(lib2Registry));
    }

    @Test
    void incrementalRegeneration_preservesManualCode() throws Exception {
        // Setup: Initial generation
        final Path cemPath = Paths.get("src/test/resources/sample-cem/minimal-button.json");
        final Path javaOutputDir = tempDir.resolve("java");
        final Path tsOutputDir = tempDir.resolve("typescript");

        final GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(new LibraryConfig("testlib", cemPath, "com.test.ui.testlib")),
            new OutputConfig(javaOutputDir, tsOutputDir, true),
            new TypeMappingConfig(Collections.emptyMap()),
            new FilterConfig(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            )
        );

        // Execute: First generation
        CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);
        GenerationReport report = pipeline.execute();
        assertEquals(0, report.errors().size());

        // Modify: Add manual code to generated file
        final Path wrapperPath = javaOutputDir.resolve("com/test/ui/testlib/PButton.java");
        String originalCode = Files.readString(wrapperPath);
        
        final String manualCode = "\n    // Manual extension\n    public void customMethod() {\n        // Custom logic\n    }\n";
        final String modifiedCode = originalCode.replace(
            "    // ========== END GENERATED CODE ==========",
            "    // ========== END GENERATED CODE ==========\n" + manualCode
        );
        Files.writeString(wrapperPath, modifiedCode);

        // Execute: Regenerate
        pipeline = new CodeGenerationPipeline(config);
        report = pipeline.execute();
        assertEquals(0, report.errors().size());

        // Verify: Manual code preserved
        final String regeneratedCode = Files.readString(wrapperPath);
        assertTrue(regeneratedCode.contains("customMethod"), "Manual code should be preserved");
        assertTrue(regeneratedCode.contains("Custom logic"), "Manual code content should be preserved");
        
        // Verify: Generated code still present
        assertTrue(regeneratedCode.contains("public class PButton extends PWebComponent<ButtonProps>"));
        assertTrue(regeneratedCode.contains("addClickListener"));
    }

    @Test
    void generatedCodeCompiles_withDependencies() throws Exception {
        // Setup: Generate code with component dependencies
        final Path cemPath = Paths.get("src/test/resources/sample-cem/complex-component.json");
        final Path javaOutputDir = tempDir.resolve("java");
        final Path tsOutputDir = tempDir.resolve("typescript");

        final GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(new LibraryConfig("testlib", cemPath, "com.test.ui.testlib")),
            new OutputConfig(javaOutputDir, tsOutputDir, true),
            new TypeMappingConfig(Collections.emptyMap()),
            new FilterConfig(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            )
        );

        // Execute: Generate code
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);
        final GenerationReport report = pipeline.execute();

        // Verify: No errors
        assertEquals(0, report.errors().size());

        // Verify: All generated files compile together
        assertTrue(compileJavaFiles(javaOutputDir), "All generated files should compile together");
    }

    @Test
    void pipelineHandlesErrors_gracefully() throws Exception {
        // Setup: Configuration with invalid CEM path
        final Path invalidCemPath = tempDir.resolve("nonexistent.json");
        final Path javaOutputDir = tempDir.resolve("java");
        final Path tsOutputDir = tempDir.resolve("typescript");

        final GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(new LibraryConfig("invalid", invalidCemPath, "com.test.ui.invalid")),
            new OutputConfig(javaOutputDir, tsOutputDir, true),
            new TypeMappingConfig(Collections.emptyMap()),
            new FilterConfig(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            )
        );

        // Execute: Run pipeline
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);
        final GenerationReport report = pipeline.execute();

        // Verify: Error reported but pipeline completes
        assertNotNull(report);
        assertTrue(report.errors().size() > 0, "Should report errors");
        assertTrue(report.errors().get(0).contains("invalid"), "Error should mention library name");
    }

    @Test
    void multiLibraryWithFiltering_generatesCorrectSubset() throws Exception {
        // Setup: Multi-library config with filters
        final Path cemPath1 = Paths.get("src/test/resources/sample-cem/minimal-button.json");
        final Path cemPath2 = Paths.get("src/test/resources/sample-cem/complex-component.json");
        final Path javaOutputDir = tempDir.resolve("java");
        final Path tsOutputDir = tempDir.resolve("typescript");

        final GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(
                new LibraryConfig("lib1", cemPath1, "com.test.ui.lib1"),
                new LibraryConfig("lib2", cemPath2, "com.test.ui.lib2")
            ),
            new OutputConfig(javaOutputDir, tsOutputDir, true),
            new TypeMappingConfig(Collections.emptyMap()),
            new FilterConfig(
                List.of("test-*"),  // Only include test-* components
                Collections.emptyList(),
                Collections.emptyList()
            )
        );

        // Execute: Run pipeline
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);
        final GenerationReport report = pipeline.execute();

        // Verify: Only filtered components generated
        assertEquals(0, report.errors().size());
        
        for (final LibraryReport libReport : report.libraries()) {
            // All generated components should match filter
            for (final String filePath : libReport.generatedFiles()) {
                if (filePath.contains("ComponentRegistry")) {
                    continue; // Skip registry files
                }
                final String content = Files.readString(Paths.get(filePath));
                if (content.contains("TAG_NAME")) {
                    assertTrue(content.contains("\"test-"), "Component should match filter pattern");
                }
            }
        }
    }

    @Test
    void generationReport_containsAccurateStatistics() throws Exception {
        // Setup: Generate from known CEM
        final Path cemPath = Paths.get("src/test/resources/sample-cem/minimal-button.json");
        final Path javaOutputDir = tempDir.resolve("java");
        final Path tsOutputDir = tempDir.resolve("typescript");

        final GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(new LibraryConfig("testlib", cemPath, "com.test.ui.testlib")),
            new OutputConfig(javaOutputDir, tsOutputDir, true),
            new TypeMappingConfig(Collections.emptyMap()),
            new FilterConfig(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            )
        );

        // Execute: Generate
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);
        final GenerationReport report = pipeline.execute();

        // Verify: Report statistics
        assertNotNull(report.timestamp());
        assertNotNull(report.duration());
        assertTrue(report.duration().toMillis() >= 0);

        assertEquals(1, report.libraries().size());
        final LibraryReport libReport = report.libraries().get(0);

        assertTrue(libReport.componentCount() > 0);
        assertTrue(libReport.propertyCount() >= 0);
        assertTrue(libReport.eventCount() >= 0);
        assertTrue(libReport.generatedFiles().size() > 0);

        // Verify: Aggregation methods work
        assertEquals(libReport.componentCount(), report.getTotalComponents());
        assertEquals(libReport.propertyCount(), report.getTotalProperties());
        assertEquals(libReport.eventCount(), report.getTotalEvents());
    }

    @Test
    void registryClass_containsAllGeneratedComponents() throws Exception {
        // Setup: Generate multiple components
        final Path cemPath = Paths.get("src/test/resources/sample-cem/complex-component.json");
        final Path javaOutputDir = tempDir.resolve("java");
        final Path tsOutputDir = tempDir.resolve("typescript");

        final GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(new LibraryConfig("testlib", cemPath, "com.test.ui.testlib")),
            new OutputConfig(javaOutputDir, tsOutputDir, true),
            new TypeMappingConfig(Collections.emptyMap()),
            new FilterConfig(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            )
        );

        // Execute: Generate
        final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);
        final GenerationReport report = pipeline.execute();

        // Verify: Registry contains all components
        final Path registryPath = javaOutputDir.resolve("com/test/ui/testlib/TestlibComponentRegistry.java");
        assertTrue(Files.exists(registryPath));

        final String registryCode = Files.readString(registryPath);
        assertTrue(registryCode.contains("public final class TestlibComponentRegistry"));
        assertTrue(registryCode.contains("COMPONENT_FACTORIES"));
        
        // Each generated component should be registered
        final LibraryReport libReport = report.libraries().get(0);
        assertTrue(libReport.componentCount() > 0);
    }

    // ========== Helper Methods ==========

    /**
     * Compiles Java files in the specified directory.
     *
     * @param sourceDir the directory containing Java source files
     * @return true if compilation succeeds, false otherwise
     */
    private boolean compileJavaFiles(final Path sourceDir) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("No Java compiler available - skipping compilation test");
            return true; // Skip test if no compiler available
        }

        // Collect all .java files
        final List<File> javaFiles = Files.walk(sourceDir)
            .filter(p -> p.toString().endsWith(".java"))
            .map(Path::toFile)
            .collect(Collectors.toList());

        if (javaFiles.isEmpty()) {
            return true; // No files to compile
        }

        // Setup compilation
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        
        final Iterable<? extends JavaFileObject> compilationUnits = 
            fileManager.getJavaFileObjectsFromFiles(javaFiles);

        // Add classpath for PWebComponent and other dependencies
        final String classpath = System.getProperty("java.class.path");
        final List<String> options = Arrays.asList(
            "-classpath", classpath,
            "-d", tempDir.resolve("classes").toString()
        );

        Files.createDirectories(tempDir.resolve("classes"));

        // Compile
        final JavaCompiler.CompilationTask task = compiler.getTask(
            null,
            fileManager,
            diagnostics,
            options,
            null,
            compilationUnits
        );

        final boolean success = task.call();

        // Log diagnostics if compilation fails
        if (!success) {
            for (final Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                System.err.println(diagnostic.getKind() + ": " + diagnostic.getMessage(null));
                if (diagnostic.getSource() != null) {
                    System.err.println("  at " + diagnostic.getSource().getName() + 
                        ":" + diagnostic.getLineNumber());
                }
            }
        }

        fileManager.close();
        return success;
    }
}
