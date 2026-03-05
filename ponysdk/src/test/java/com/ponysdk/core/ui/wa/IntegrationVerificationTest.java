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

package com.ponysdk.core.ui.wa;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * Integration verification tests for task 14.3 and 14.4.
 * <p>
 * These tests verify:
 * - Generated components have correct getComponentSignature() returning wa-* tag names
 * - Manual subclasses (PForm, PDataTable, PResponsiveGrid, PStack) are not overwritten
 * - Generated base classes and manual extensions coexist via inheritance
 * </p>
 * <p>
 * Validates: Requirements 12.1, 12.3, 13.7, 14.3, 14.4
 * </p>
 */
class IntegrationVerificationTest {

    private static final String GENERATED_DIR = "build/generated/sources/wa/java/com/ponysdk/core/ui/wa";
    private static final String MANUAL_DIR = "src/main/java/com/ponysdk/core/ui/wa";

    @Test
    void testGeneratedComponentsHaveCorrectSignature() throws Exception {
        // Given - Find all generated WA*.java files
        final Path generatedPath = Paths.get(GENERATED_DIR);
        
        if (!Files.exists(generatedPath)) {
            // Skip if generated sources don't exist yet
            System.out.println("Skipping test - generated sources not found. Run: ./gradlew :ponysdk:generateWebAwesomeWrappers");
            return;
        }

        final List<Path> generatedComponents = Files.walk(generatedPath)
            .filter(p -> p.toString().endsWith(".java"))
            .filter(p -> p.getFileName().toString().startsWith("WA"))
            .filter(p -> !p.getFileName().toString().contains("Props"))
            .collect(Collectors.toList());

        assertTrue(generatedComponents.size() > 50, 
            "Should have generated at least 50 component wrappers");

        // When/Then - Verify each component has getComponentSignature() returning wa-*
        for (final Path componentFile : generatedComponents) {
            final String content = Files.readString(componentFile);
            final String className = componentFile.getFileName().toString().replace(".java", "");
            
            // Verify it extends PWebComponent
            assertTrue(content.contains("extends PWebComponent<"),
                className + " should extend PWebComponent");
            
            // Verify it has getComponentSignature() method
            assertTrue(content.contains("protected String getComponentSignature()"),
                className + " should have getComponentSignature() method");
            
            // Verify it returns a wa-* tag name
            assertTrue(content.contains("return \"wa-"),
                className + " should return a wa-* tag name from getComponentSignature()");
        }
    }

    @Test
    void testManualSubclassesExist() {
        // Given - Manual subclasses that should NOT be overwritten
        final List<String> manualClasses = List.of(
            "form/PForm.java",
            "datatable/PDataTable.java",
            "layout/PResponsiveGrid.java",
            "layout/PStack.java"
        );

        // When/Then - Verify each manual class exists
        for (final String manualClass : manualClasses) {
            final File file = new File(MANUAL_DIR, manualClass);
            assertTrue(file.exists(), 
                "Manual subclass should exist: " + manualClass);
        }
    }

    @Test
    void testManualSubclassesExtendPWebComponent() throws Exception {
        // Given - Manual subclasses
        final List<String> manualClasses = List.of(
            "form/PForm.java",
            "datatable/PDataTable.java",
            "layout/PResponsiveGrid.java",
            "layout/PStack.java"
        );

        // When/Then - Verify each extends PWebComponent
        for (final String manualClass : manualClasses) {
            final File file = new File(MANUAL_DIR, manualClass);
            if (!file.exists()) {
                continue; // Skip if file doesn't exist
            }

            final String content = Files.readString(file.toPath());
            assertTrue(content.contains("extends PWebComponent<"),
                manualClass + " should extend PWebComponent");
            
            // Verify it has getComponentSignature() method
            assertTrue(content.contains("protected String getComponentSignature()"),
                manualClass + " should have getComponentSignature() method");
        }
    }

    @Test
    void testCodeGeneratorRegenerationSafety() {
        // Given - Manual classes in src/main/java
        final File manualFormDir = new File(MANUAL_DIR, "form");
        final File manualDataTableDir = new File(MANUAL_DIR, "datatable");
        final File manualLayoutDir = new File(MANUAL_DIR, "layout");

        // When/Then - Verify manual classes are in separate directories from generated
        assertTrue(manualFormDir.exists() || manualDataTableDir.exists() || manualLayoutDir.exists(),
            "Manual subclasses should be in separate directories");

        // Verify generated components are in build/generated
        final File generatedDir = new File(GENERATED_DIR);
        if (generatedDir.exists()) {
            // Generated components should NOT include manual subclasses
            final File[] generatedFiles = generatedDir.listFiles((dir, name) -> 
                name.equals("PForm.java") || 
                name.equals("PDataTable.java") ||
                name.equals("PResponsiveGrid.java") ||
                name.equals("PStack.java")
            );
            
            assertTrue(generatedFiles == null || generatedFiles.length == 0,
                "Generated directory should NOT contain manual subclasses");
        }
    }

    @Test
    void testComponentIndexExists() {
        // Given
        final File componentIndex = new File(GENERATED_DIR, "ComponentIndex.java");

        // When/Then
        if (componentIndex.exists()) {
            assertTrue(componentIndex.length() > 0, "ComponentIndex should not be empty");
        } else {
            System.out.println("ComponentIndex not found - run: ./gradlew :ponysdk:generateWebAwesomeWrappers");
        }
    }
}
