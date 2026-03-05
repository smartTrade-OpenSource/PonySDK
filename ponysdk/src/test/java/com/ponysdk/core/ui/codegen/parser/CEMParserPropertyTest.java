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

package com.ponysdk.core.ui.codegen.parser;

import com.ponysdk.core.ui.codegen.model.ComponentDefinition;
import net.jqwik.api.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for CEMParser.
 * <p>
 * Feature: generic-webcomponent-wrapper
 * </p>
 */
public class CEMParserPropertyTest {

    // ========== Property 1: CEM Parsing Robustness ==========

    /**
     * Property 1: CEM Parsing Robustness
     * <p>
     * **Validates: Requirements 1.1, 1.3, 1.6, 10.1, 10.2**
     * </p>
     * <p>
     * For any valid CEM JSON file with optional fields or unknown extensions,
     * parsing should successfully extract all standard fields and produce a valid
     * ComponentDefinition, ignoring unknown extensions without errors.
     * </p>
     */
    @Property(tries = 100)
    @Label("Feature: generic-webcomponent-wrapper, Property 1: CEM Parsing Robustness")
    void cemParsingRobustness(
            @ForAll("validCEMWithExtensions") JsonObject cem
    ) throws Exception {
        CEMParser parser = new DefaultCEMParser();
        
        // Write CEM to temporary file
        Path tempFile = Files.createTempFile("cem-test", ".json");
        try {
            writeCEMToFile(cem, tempFile);
            
            // Parse should succeed despite unknown extensions
            List<ComponentDefinition> components = parser.parse(tempFile);
            
            assertNotNull(components, "Parser should return non-null list");
            
            // Verify standard fields are extracted
            for (ComponentDefinition def : components) {
                assertNotNull(def.tagName(), "Tag name should be extracted");
                assertNotNull(def.className(), "Class name should be extracted");
                assertNotNull(def.properties(), "Properties list should not be null");
                assertNotNull(def.events(), "Events list should not be null");
                assertNotNull(def.slots(), "Slots list should not be null");
                assertNotNull(def.methods(), "Methods list should not be null");
                assertNotNull(def.cssProperties(), "CSS properties list should not be null");
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    // ========== Property 2: CEM Validation Error Messages ==========

    /**
     * Property 2: CEM Validation Error Messages
     * <p>
     * **Validates: Requirements 1.4, 10.5**
     * </p>
     * <p>
     * For any malformed or invalid CEM file, the parser should return a descriptive
     * error message that identifies the specific validation failure.
     * </p>
     */
    @Property(tries = 100)
    @Label("Feature: generic-webcomponent-wrapper, Property 2: CEM Validation Error Messages")
    void cemValidationErrorMessages(
            @ForAll("malformedCEM") JsonObject cem
    ) throws Exception {
        CEMParser parser = new DefaultCEMParser();
        
        // Write CEM to temporary file
        Path tempFile = Files.createTempFile("cem-test-invalid", ".json");
        try {
            writeCEMToFile(cem, tempFile);
            
            // Parsing should fail with descriptive error
            CEMParseException exception = assertThrows(CEMParseException.class, () -> {
                parser.parse(tempFile);
            });
            
            assertNotNull(exception.getMessage(), "Error message should not be null");
            assertFalse(exception.getMessage().isEmpty(), "Error message should not be empty");
            
            // Error message should be descriptive (contain relevant keywords)
            String message = exception.getMessage().toLowerCase();
            assertTrue(
                message.contains("missing") || 
                message.contains("required") || 
                message.contains("invalid") ||
                message.contains("modules"),
                "Error message should describe the validation failure: " + exception.getMessage()
            );
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    // ========== Arbitraries (Generators) ==========

    /**
     * Generates valid CEM JSON with unknown extensions.
     * Tests that parser ignores unknown fields gracefully.
     */
    @Provide
    Arbitrary<JsonObject> validCEMWithExtensions() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
        ).as((tagPrefix, componentName, extensionKey, extensionValue) -> {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            
            // Standard fields
            builder.add("schemaVersion", "1.0.0");
            
            // Add unknown extension field (should be ignored)
            builder.add("x-" + extensionKey, extensionValue);
            builder.add("vendor-" + extensionKey, extensionValue);
            
            // Modules array with component
            JsonObjectBuilder componentBuilder = Json.createObjectBuilder()
                    .add("kind", "class")
                    .add("name", componentName)
                    .add("tagName", tagPrefix + "-" + componentName.toLowerCase())
                    .add("summary", "Test component")
                    // Add unknown extension in component
                    .add("x-custom-field", "custom-value");
            
            JsonObjectBuilder declarationBuilder = Json.createObjectBuilder()
                    .add("kind", "javascript-module")
                    .add("path", "test.js")
                    .add("declarations", Json.createArrayBuilder()
                            .add(componentBuilder));
            
            builder.add("modules", Json.createArrayBuilder()
                    .add(declarationBuilder));
            
            return builder.build();
        });
    }

    /**
     * Generates malformed CEM JSON (missing required fields).
     * Tests that parser provides descriptive error messages.
     */
    @Provide
    Arbitrary<JsonObject> malformedCEM() {
        return Arbitraries.of(
                // Missing modules array
                Json.createObjectBuilder()
                        .add("schemaVersion", "1.0.0")
                        .add("readme", "Missing modules")
                        .build(),
                
                // Missing schemaVersion
                Json.createObjectBuilder()
                        .add("modules", Json.createArrayBuilder())
                        .build()
        );
    }

    // ========== Helper Methods ==========

    private void writeCEMToFile(JsonObject cem, Path path) throws IOException {
        try (StringWriter stringWriter = new StringWriter();
             JsonWriter jsonWriter = Json.createWriter(stringWriter)) {
            jsonWriter.writeObject(cem);
            Files.writeString(path, stringWriter.toString());
        }
    }
}
