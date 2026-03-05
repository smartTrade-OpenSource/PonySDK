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

import com.ponysdk.core.ui.codegen.model.GeneratorConfiguration;
import com.ponysdk.core.ui.codegen.model.LibraryConfig;
import com.ponysdk.core.ui.codegen.parser.ValidationResult;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Default implementation of ConfigurationManager using JSON format.
 */
public class DefaultConfigurationManager implements ConfigurationManager {
    
    @Override
    public GeneratorConfiguration parse(Path configPath) throws ConfigurationException {
        if (!Files.exists(configPath)) {
            throw new ConfigurationException("Configuration file not found: " + configPath);
        }
        
        try {
            String content = Files.readString(configPath);
            JsonReader reader = Json.createReader(new StringReader(content));
            JsonObject json = reader.readObject();
            
            return parseConfiguration(json, configPath.getParent());
        } catch (IOException e) {
            throw new ConfigurationException("Failed to read configuration file: " + configPath, e);
        } catch (Exception e) {
            throw new ConfigurationException("Failed to parse configuration: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void serialize(GeneratorConfiguration config, Path outputPath) throws IOException {
        JsonObject json = serializeConfiguration(config);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory writerFactory = Json.createWriterFactory(properties);
        
        try (JsonWriter writer = writerFactory.createWriter(Files.newBufferedWriter(outputPath))) {
            writer.writeObject(json);
        }
    }
    
    @Override
    public ValidationResult validate(GeneratorConfiguration config) {
        ValidationResult.Builder builder = ValidationResult.builder();
        
        // Validate libraries
        if (config.libraries() == null || config.libraries().isEmpty()) {
            builder.addError("Configuration must contain at least one library");
        } else {
            for (LibraryConfig lib : config.libraries()) {
                validateLibrary(lib, builder);
            }
        }
        
        // Validate output configuration
        if (config.output() == null) {
            builder.addError("Output configuration is required");
        } else {
            if (config.output().javaOutputDir() == null) {
                builder.addError("Java output directory is required");
            }
            if (config.output().typescriptOutputDir() == null) {
                builder.addError("TypeScript output directory is required");
            }
        }
        
        // Validate filter patterns
        if (config.filters() != null) {
            validateFilterPatterns(config.filters().includePatterns(), "include", builder);
            validateFilterPatterns(config.filters().excludePatterns(), "exclude", builder);
        }
        
        return builder.build();
    }
    
    private void validateLibrary(LibraryConfig lib, ValidationResult.Builder builder) {
        if (lib.name() == null || lib.name().isBlank()) {
            builder.addError("Library name is required");
        }
        
        if (lib.cemPath() == null) {
            builder.addError("CEM path is required for library: " + lib.name());
        } else if (!Files.exists(lib.cemPath())) {
            builder.addError("CEM file not found: " + lib.cemPath() + " for library: " + lib.name());
        }
        
        if (lib.javaPackage() == null || lib.javaPackage().isBlank()) {
            builder.addError("Java package is required for library: " + lib.name());
        } else if (!isValidJavaPackage(lib.javaPackage())) {
            builder.addError("Invalid Java package name: " + lib.javaPackage() + 
                " for library: " + lib.name() + 
                ". Example: com.ponysdk.core.ui.webawesome");
        }
    }
    
    private void validateFilterPatterns(java.util.List<String> patterns, String type, ValidationResult.Builder builder) {
        if (patterns == null) return;
        
        for (String pattern : patterns) {
            try {
                Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                builder.addError("Invalid " + type + " pattern: " + pattern + 
                    ". Error: " + e.getMessage() + 
                    ". Example: wa-.*, .*-internal");
            }
        }
    }
    
    private boolean isValidJavaPackage(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return false;
        }
        
        String[] parts = packageName.split("\\.");
        for (String part : parts) {
            if (!isValidJavaIdentifier(part)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean isValidJavaIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }
        
        if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            return false;
        }
        
        for (int i = 1; i < identifier.length(); i++) {
            if (!Character.isJavaIdentifierPart(identifier.charAt(i))) {
                return false;
            }
        }
        
        // Check if it's a reserved keyword
        return !isJavaKeyword(identifier);
    }
    
    private boolean isJavaKeyword(String word) {
        return switch (word) {
            case "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
                 "class", "const", "continue", "default", "do", "double", "else", "enum",
                 "extends", "final", "finally", "float", "for", "goto", "if", "implements",
                 "import", "instanceof", "int", "interface", "long", "native", "new", "package",
                 "private", "protected", "public", "return", "short", "static", "strictfp",
                 "super", "switch", "synchronized", "this", "throw", "throws", "transient",
                 "try", "void", "volatile", "while" -> true;
            default -> false;
        };
    }
    
    private GeneratorConfiguration parseConfiguration(JsonObject json, Path basePath) {
        // Parse libraries
        JsonArray librariesArray = json.getJsonArray("libraries");
        java.util.List<LibraryConfig> libraries = new java.util.ArrayList<>();
        if (librariesArray != null) {
            for (int i = 0; i < librariesArray.size(); i++) {
                JsonObject libJson = librariesArray.getJsonObject(i);
                libraries.add(parseLibrary(libJson, basePath));
            }
        }
        
        // Parse output
        JsonObject outputJson = json.getJsonObject("output");
        com.ponysdk.core.ui.codegen.model.OutputConfig output = null;
        if (outputJson != null) {
            output = parseOutput(outputJson, basePath);
        }
        
        // Parse type mappings
        JsonObject typeMappingsJson = json.getJsonObject("typeMappings");
        com.ponysdk.core.ui.codegen.model.TypeMappingConfig typeMappings = null;
        if (typeMappingsJson != null) {
            typeMappings = parseTypeMappings(typeMappingsJson);
        } else {
            typeMappings = new com.ponysdk.core.ui.codegen.model.TypeMappingConfig(Map.of());
        }
        
        // Parse filters
        JsonObject filtersJson = json.getJsonObject("filters");
        com.ponysdk.core.ui.codegen.model.FilterConfig filters = null;
        if (filtersJson != null) {
            filters = parseFilters(filtersJson);
        } else {
            filters = new com.ponysdk.core.ui.codegen.model.FilterConfig(
                java.util.List.of(), java.util.List.of(), java.util.List.of()
            );
        }
        
        return new GeneratorConfiguration(libraries, output, typeMappings, filters);
    }
    
    private LibraryConfig parseLibrary(JsonObject json, Path basePath) {
        String name = json.getString("name");
        String cemPathStr = json.getString("cemPath");
        Path cemPath = basePath != null ? basePath.resolve(cemPathStr) : Path.of(cemPathStr);
        String javaPackage = json.getString("javaPackage");
        
        return new LibraryConfig(name, cemPath, javaPackage);
    }
    
    private com.ponysdk.core.ui.codegen.model.OutputConfig parseOutput(JsonObject json, Path basePath) {
        String javaOutputDirStr = json.getString("javaOutputDir");
        String tsOutputDirStr = json.getString("typescriptOutputDir");
        boolean generateReports = json.getBoolean("generateReports", true);
        
        Path javaOutputDir = basePath != null ? basePath.resolve(javaOutputDirStr) : Path.of(javaOutputDirStr);
        Path tsOutputDir = basePath != null ? basePath.resolve(tsOutputDirStr) : Path.of(tsOutputDirStr);
        
        return new com.ponysdk.core.ui.codegen.model.OutputConfig(javaOutputDir, tsOutputDir, generateReports);
    }
    
    private com.ponysdk.core.ui.codegen.model.TypeMappingConfig parseTypeMappings(JsonObject json) {
        JsonObject customMappingsJson = json.getJsonObject("customMappings");
        Map<String, com.ponysdk.core.ui.codegen.model.CustomTypeMapping> customMappings = new HashMap<>();
        
        if (customMappingsJson != null) {
            for (String key : customMappingsJson.keySet()) {
                JsonObject mappingJson = customMappingsJson.getJsonObject(key);
                String javaType = mappingJson.getString("javaType");
                String tsType = mappingJson.getString("typescriptType");
                customMappings.put(key, new com.ponysdk.core.ui.codegen.model.CustomTypeMapping(javaType, tsType));
            }
        }
        
        return new com.ponysdk.core.ui.codegen.model.TypeMappingConfig(customMappings);
    }
    
    private com.ponysdk.core.ui.codegen.model.FilterConfig parseFilters(JsonObject json) {
        java.util.List<String> includePatterns = parseStringArray(json.getJsonArray("includePatterns"));
        java.util.List<String> excludePatterns = parseStringArray(json.getJsonArray("excludePatterns"));
        java.util.List<String> skipComponents = parseStringArray(json.getJsonArray("skipComponents"));
        
        return new com.ponysdk.core.ui.codegen.model.FilterConfig(includePatterns, excludePatterns, skipComponents);
    }
    
    private java.util.List<String> parseStringArray(JsonArray array) {
        if (array == null) {
            return java.util.List.of();
        }
        
        java.util.List<String> result = new java.util.ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            result.add(array.getString(i));
        }
        return result;
    }
    
    private JsonObject serializeConfiguration(GeneratorConfiguration config) {
        var builder = Json.createObjectBuilder();
        
        // Serialize libraries
        var librariesBuilder = Json.createArrayBuilder();
        for (LibraryConfig lib : config.libraries()) {
            librariesBuilder.add(serializeLibrary(lib));
        }
        builder.add("libraries", librariesBuilder);
        
        // Serialize output
        builder.add("output", serializeOutput(config.output()));
        
        // Serialize type mappings
        builder.add("typeMappings", serializeTypeMappings(config.typeMappings()));
        
        // Serialize filters
        builder.add("filters", serializeFilters(config.filters()));
        
        return builder.build();
    }
    
    private JsonObject serializeLibrary(LibraryConfig lib) {
        return Json.createObjectBuilder()
            .add("name", lib.name())
            .add("cemPath", lib.cemPath().toString())
            .add("javaPackage", lib.javaPackage())
            .build();
    }
    
    private JsonObject serializeOutput(com.ponysdk.core.ui.codegen.model.OutputConfig output) {
        return Json.createObjectBuilder()
            .add("javaOutputDir", output.javaOutputDir().toString())
            .add("typescriptOutputDir", output.typescriptOutputDir().toString())
            .add("generateReports", output.generateReports())
            .build();
    }
    
    private JsonObject serializeTypeMappings(com.ponysdk.core.ui.codegen.model.TypeMappingConfig typeMappings) {
        var customMappingsBuilder = Json.createObjectBuilder();
        
        for (Map.Entry<String, com.ponysdk.core.ui.codegen.model.CustomTypeMapping> entry : 
                typeMappings.customMappings().entrySet()) {
            customMappingsBuilder.add(entry.getKey(), Json.createObjectBuilder()
                .add("javaType", entry.getValue().javaType())
                .add("typescriptType", entry.getValue().typescriptType())
                .build());
        }
        
        return Json.createObjectBuilder()
            .add("customMappings", customMappingsBuilder)
            .build();
    }
    
    private JsonObject serializeFilters(com.ponysdk.core.ui.codegen.model.FilterConfig filters) {
        var includeBuilder = Json.createArrayBuilder();
        for (String pattern : filters.includePatterns()) {
            includeBuilder.add(pattern);
        }
        
        var excludeBuilder = Json.createArrayBuilder();
        for (String pattern : filters.excludePatterns()) {
            excludeBuilder.add(pattern);
        }
        
        var skipBuilder = Json.createArrayBuilder();
        for (String component : filters.skipComponents()) {
            skipBuilder.add(component);
        }
        
        return Json.createObjectBuilder()
            .add("includePatterns", includeBuilder)
            .add("excludePatterns", excludeBuilder)
            .add("skipComponents", skipBuilder)
            .build();
    }
}
