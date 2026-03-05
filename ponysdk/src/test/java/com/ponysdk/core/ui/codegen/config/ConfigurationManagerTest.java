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

package com.ponysdk.core.ui.codegen.config;

import com.ponysdk.core.ui.codegen.model.*;
import com.ponysdk.core.ui.codegen.parser.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationManagerTest {
    
    private ConfigurationManager configManager;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        configManager = new DefaultConfigurationManager();
    }
    
    @Test
    void parseValidConfiguration() throws Exception {
        // Create a test CEM file
        Path cemFile = tempDir.resolve("custom-elements.json");
        Files.writeString(cemFile, "{}");
        
        // Create configuration file
        String configJson = """
            {
              "libraries": [
                {
                  "name": "webawesome",
                  "cemPath": "custom-elements.json",
                  "javaPackage": "com.ponysdk.core.ui.webawesome"
                }
              ],
              "output": {
                "javaOutputDir": "src/main/java",
                "typescriptOutputDir": "src/main/typescript",
                "generateReports": true
              },
              "typeMappings": {
                "customMappings": {
                  "HTMLElement": {
                    "javaType": "IsPWidget",
                    "typescriptType": "HTMLElement"
                  }
                }
              },
              "filters": {
                "includePatterns": ["wa-.*"],
                "excludePatterns": [".*-internal"],
                "skipComponents": ["wa-deprecated"]
              }
            }
            """;
        
        Path configFile = tempDir.resolve("config.json");
        Files.writeString(configFile, configJson);
        
        // Parse configuration
        GeneratorConfiguration config = configManager.parse(configFile);
        
        // Verify libraries
        assertNotNull(config.libraries());
        assertEquals(1, config.libraries().size());
        LibraryConfig lib = config.libraries().get(0);
        assertEquals("webawesome", lib.name());
        assertEquals("com.ponysdk.core.ui.webawesome", lib.javaPackage());
        assertTrue(lib.cemPath().toString().endsWith("custom-elements.json"));
        
        // Verify output
        assertNotNull(config.output());
        assertTrue(config.output().javaOutputDir().toString().endsWith("src/main/java"));
        assertTrue(config.output().typescriptOutputDir().toString().endsWith("src/main/typescript"));
        assertTrue(config.output().generateReports());
        
        // Verify type mappings
        assertNotNull(config.typeMappings());
        assertEquals(1, config.typeMappings().customMappings().size());
        CustomTypeMapping mapping = config.typeMappings().customMappings().get("HTMLElement");
        assertEquals("IsPWidget", mapping.javaType());
        assertEquals("HTMLElement", mapping.typescriptType());
        
        // Verify filters
        assertNotNull(config.filters());
        assertEquals(List.of("wa-.*"), config.filters().includePatterns());
        assertEquals(List.of(".*-internal"), config.filters().excludePatterns());
        assertEquals(List.of("wa-deprecated"), config.filters().skipComponents());
    }
    
    @Test
    void parseMinimalConfiguration() throws Exception {
        // Create a test CEM file
        Path cemFile = tempDir.resolve("custom-elements.json");
        Files.writeString(cemFile, "{}");
        
        // Create minimal configuration
        String configJson = """
            {
              "libraries": [
                {
                  "name": "test",
                  "cemPath": "custom-elements.json",
                  "javaPackage": "com.test"
                }
              ],
              "output": {
                "javaOutputDir": "java",
                "typescriptOutputDir": "ts",
                "generateReports": false
              }
            }
            """;
        
        Path configFile = tempDir.resolve("config.json");
        Files.writeString(configFile, configJson);
        
        GeneratorConfiguration config = configManager.parse(configFile);
        
        assertNotNull(config);
        assertEquals(1, config.libraries().size());
        assertNotNull(config.output());
        assertNotNull(config.typeMappings());
        assertNotNull(config.filters());
        assertTrue(config.typeMappings().customMappings().isEmpty());
        assertTrue(config.filters().includePatterns().isEmpty());
    }
    
    @Test
    void parseNonExistentFile() {
        Path nonExistent = tempDir.resolve("nonexistent.json");
        
        ConfigurationException exception = assertThrows(
            ConfigurationException.class,
            () -> configManager.parse(nonExistent)
        );
        
        assertTrue(exception.getMessage().contains("not found"));
    }
    
    @Test
    void parseMalformedJson() throws Exception {
        String malformedJson = "{ invalid json }";
        Path configFile = tempDir.resolve("config.json");
        Files.writeString(configFile, malformedJson);
        
        assertThrows(ConfigurationException.class, () -> configManager.parse(configFile));
    }
    
    @Test
    void serializeConfiguration() throws Exception {
        // Create test configuration
        Path cemPath = tempDir.resolve("test.json");
        Files.writeString(cemPath, "{}");
        
        LibraryConfig lib = new LibraryConfig(
            "testlib",
            cemPath,
            "com.test.lib"
        );
        
        OutputConfig output = new OutputConfig(
            Path.of("java"),
            Path.of("ts"),
            true
        );
        
        TypeMappingConfig typeMappings = new TypeMappingConfig(
            Map.of("Custom", new CustomTypeMapping("JavaCustom", "TsCustom"))
        );
        
        FilterConfig filters = new FilterConfig(
            List.of("test-.*"),
            List.of(".*-skip"),
            List.of("test-old")
        );
        
        GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(lib),
            output,
            typeMappings,
            filters
        );
        
        // Serialize
        Path outputFile = tempDir.resolve("output.json");
        configManager.serialize(config, outputFile);
        
        // Verify file exists and is valid JSON
        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertTrue(content.contains("testlib"));
        assertTrue(content.contains("com.test.lib"));
        assertTrue(content.contains("JavaCustom"));
    }
    
    @Test
    void validateValidConfiguration() throws Exception {
        Path cemFile = tempDir.resolve("test.json");
        Files.writeString(cemFile, "{}");
        
        GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(new LibraryConfig("test", cemFile, "com.test")),
            new OutputConfig(Path.of("java"), Path.of("ts"), true),
            new TypeMappingConfig(Map.of()),
            new FilterConfig(List.of(), List.of(), List.of())
        );
        
        ValidationResult result = configManager.validate(config);
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    @Test
    void validateMissingLibraries() {
        GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(),
            new OutputConfig(Path.of("java"), Path.of("ts"), true),
            new TypeMappingConfig(Map.of()),
            new FilterConfig(List.of(), List.of(), List.of())
        );
        
        ValidationResult result = configManager.validate(config);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("at least one library")));
    }
    
    @Test
    void validateMissingCemFile() {
        Path nonExistent = tempDir.resolve("nonexistent.json");
        
        GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(new LibraryConfig("test", nonExistent, "com.test")),
            new OutputConfig(Path.of("java"), Path.of("ts"), true),
            new TypeMappingConfig(Map.of()),
            new FilterConfig(List.of(), List.of(), List.of())
        );
        
        ValidationResult result = configManager.validate(config);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("CEM file not found")));
    }
    
    @Test
    void validateInvalidJavaPackage() throws Exception {
        Path cemFile = tempDir.resolve("test.json");
        Files.writeString(cemFile, "{}");
        
        GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(new LibraryConfig("test", cemFile, "123invalid")),
            new OutputConfig(Path.of("java"), Path.of("ts"), true),
            new TypeMappingConfig(Map.of()),
            new FilterConfig(List.of(), List.of(), List.of())
        );
        
        ValidationResult result = configManager.validate(config);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("Invalid Java package")));
    }
    
    @Test
    void validateJavaKeywordInPackage() throws Exception {
        Path cemFile = tempDir.resolve("test.json");
        Files.writeString(cemFile, "{}");
        
        GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(new LibraryConfig("test", cemFile, "com.class.test")),
            new OutputConfig(Path.of("java"), Path.of("ts"), true),
            new TypeMappingConfig(Map.of()),
            new FilterConfig(List.of(), List.of(), List.of())
        );
        
        ValidationResult result = configManager.validate(config);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("Invalid Java package")));
    }
    
    @Test
    void validateInvalidRegexPattern() throws Exception {
        Path cemFile = tempDir.resolve("test.json");
        Files.writeString(cemFile, "{}");
        
        GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(new LibraryConfig("test", cemFile, "com.test")),
            new OutputConfig(Path.of("java"), Path.of("ts"), true),
            new TypeMappingConfig(Map.of()),
            new FilterConfig(List.of("[invalid"), List.of(), List.of())
        );
        
        ValidationResult result = configManager.validate(config);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("Invalid include pattern")));
    }
    
    @Test
    void validateMissingOutput() throws Exception {
        Path cemFile = tempDir.resolve("test.json");
        Files.writeString(cemFile, "{}");
        
        GeneratorConfiguration config = new GeneratorConfiguration(
            List.of(new LibraryConfig("test", cemFile, "com.test")),
            null,
            new TypeMappingConfig(Map.of()),
            new FilterConfig(List.of(), List.of(), List.of())
        );
        
        ValidationResult result = configManager.validate(config);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("Output configuration is required")));
    }
    
    @Test
    void roundTripSerialization() throws Exception {
        // Create original configuration
        Path cemFile = tempDir.resolve("test.json");
        Files.writeString(cemFile, "{}");
        
        GeneratorConfiguration original = new GeneratorConfiguration(
            List.of(new LibraryConfig("test", cemFile, "com.test")),
            new OutputConfig(Path.of("java"), Path.of("ts"), true),
            new TypeMappingConfig(Map.of("Custom", new CustomTypeMapping("Java", "Ts"))),
            new FilterConfig(List.of("test-.*"), List.of(".*-skip"), List.of("old"))
        );
        
        // Serialize
        Path serialized = tempDir.resolve("serialized.json");
        configManager.serialize(original, serialized);
        
        // Parse back
        GeneratorConfiguration parsed = configManager.parse(serialized);
        
        // Verify equality
        assertEquals(original.libraries().size(), parsed.libraries().size());
        assertEquals(original.libraries().get(0).name(), parsed.libraries().get(0).name());
        assertEquals(original.libraries().get(0).javaPackage(), parsed.libraries().get(0).javaPackage());
        assertEquals(original.output().generateReports(), parsed.output().generateReports());
        assertEquals(original.typeMappings().customMappings().size(), 
                     parsed.typeMappings().customMappings().size());
        assertEquals(original.filters().includePatterns(), parsed.filters().includePatterns());
    }
}
