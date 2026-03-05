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

package com.ponysdk.core.ui.codegen.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GenerateWebComponentsTask}.
 */
class GenerateWebComponentsTaskTest {

    @TempDir
    Path tempDir;

    private Project project;
    private GenerateWebComponentsTask task;

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .build();
        
        task = project.getTasks().create("generateWebComponents", GenerateWebComponentsTask.class);
    }

    @Test
    void taskShouldHaveCorrectType() {
        assertNotNull(task);
        assertTrue(task instanceof GenerateWebComponentsTask);
    }

    @Test
    void taskShouldHaveConfigFileProperty() {
        assertNotNull(task.getConfigFile());
    }

    @Test
    void taskShouldHaveJavaOutputDirProperty() {
        assertNotNull(task.getJavaOutputDir());
    }

    @Test
    void taskShouldHaveTypescriptOutputDirProperty() {
        assertNotNull(task.getTypescriptOutputDir());
    }

    @Test
    void taskShouldFailWhenConfigFileDoesNotExist() {
        final Path nonExistentConfig = tempDir.resolve("nonexistent.json");
        task.getConfigFile().set(nonExistentConfig.toFile());
        task.getJavaOutputDir().set(tempDir.resolve("java").toFile());
        task.getTypescriptOutputDir().set(tempDir.resolve("ts").toFile());

        assertThrows(GradleException.class, () -> task.generate());
    }

    @Test
    void taskShouldFailWhenConfigFileIsInvalid() throws IOException {
        final Path invalidConfig = tempDir.resolve("invalid.json");
        Files.writeString(invalidConfig, "{ invalid json }");
        
        task.getConfigFile().set(invalidConfig.toFile());
        task.getJavaOutputDir().set(tempDir.resolve("java").toFile());
        task.getTypescriptOutputDir().set(tempDir.resolve("ts").toFile());

        assertThrows(GradleException.class, () -> task.generate());
    }

    @Test
    void taskShouldSucceedWithValidMinimalConfig() throws IOException {
        // Create a minimal valid configuration
        final String config = """
            {
              "libraries": [],
              "output": {
                "javaOutputDir": "%s",
                "typescriptOutputDir": "%s",
                "generateReports": false
              },
              "typeMappings": {
                "customMappings": {}
              },
              "filters": {
                "includePatterns": [],
                "excludePatterns": [],
                "skipComponents": []
              }
            }
            """.formatted(
                tempDir.resolve("java").toString().replace("\\", "\\\\"),
                tempDir.resolve("ts").toString().replace("\\", "\\\\")
            );
        
        final Path configFile = tempDir.resolve("config.json");
        Files.writeString(configFile, config);
        
        task.getConfigFile().set(configFile.toFile());
        task.getJavaOutputDir().set(tempDir.resolve("java").toFile());
        task.getTypescriptOutputDir().set(tempDir.resolve("ts").toFile());

        // Should not throw
        assertDoesNotThrow(() -> task.generate());
    }

    @Test
    void taskShouldCreateOutputDirectories() throws IOException {
        final String config = """
            {
              "libraries": [],
              "output": {
                "javaOutputDir": "%s",
                "typescriptOutputDir": "%s",
                "generateReports": false
              },
              "typeMappings": {
                "customMappings": {}
              },
              "filters": {
                "includePatterns": [],
                "excludePatterns": [],
                "skipComponents": []
              }
            }
            """.formatted(
                tempDir.resolve("java").toString().replace("\\", "\\\\"),
                tempDir.resolve("ts").toString().replace("\\", "\\\\")
            );
        
        final Path configFile = tempDir.resolve("config.json");
        Files.writeString(configFile, config);
        
        final Path javaDir = tempDir.resolve("java");
        final Path tsDir = tempDir.resolve("ts");
        
        task.getConfigFile().set(configFile.toFile());
        task.getJavaOutputDir().set(javaDir.toFile());
        task.getTypescriptOutputDir().set(tsDir.toFile());

        task.generate();

        assertTrue(Files.exists(javaDir), "Java output directory should be created");
        assertTrue(Files.exists(tsDir), "TypeScript output directory should be created");
    }
}
