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

import com.ponysdk.core.ui.codegen.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CEMParser.
 * <p>
 * Tests parsing valid CEM files, handling optional fields, and error messages for malformed files.
 * Requirements: 1.1, 1.4
 * </p>
 */
public class CEMParserTest {

    private CEMParser parser;

    @BeforeEach
    void setUp() {
        parser = new DefaultCEMParser();
    }

    private Path getResourcePath(String resourceName) throws URISyntaxException {
        return Paths.get(getClass().getClassLoader().getResource(resourceName).toURI());
    }

    // ========== Valid CEM Parsing Tests ==========

    /**
     * Test parsing a valid CEM file with a button component.
     * Validates: Requirement 1.1
     */
    @Test
    void testParseValidButtonCEM() throws Exception {
        Path cemPath = getResourcePath("cem/valid-button.json");
        
        List<ComponentDefinition> components = parser.parse(cemPath);
        
        assertNotNull(components);
        assertEquals(1, components.size());
        
        ComponentDefinition button = components.get(0);
        assertEquals("wa-button", button.tagName());
        assertEquals("Button", button.className());
        assertEquals("Buttons represent actions that are available to the user.", button.summary());
        assertEquals("stable", button.status());
        
        // Verify properties
        assertEquals(3, button.properties().size());
        PropertyDef variantProp = button.properties().get(0);
        assertEquals("variant", variantProp.name());
        assertEquals("string", variantProp.cemType());
        assertEquals("\"neutral\"", variantProp.defaultValue());
        
        // Verify events
        assertEquals(1, button.events().size());
        EventDef clickEvent = button.events().get(0);
        assertEquals("wa-click", clickEvent.name());
        
        // Verify slots
        assertEquals(2, button.slots().size());
        SlotDef prefixSlot = button.slots().get(0);
        assertEquals("prefix", prefixSlot.name());
        
        // Verify methods
        assertEquals(1, button.methods().size());
        MethodDef focusMethod = button.methods().get(0);
        assertEquals("focus", focusMethod.name());
        
        // Verify CSS properties
        assertEquals(1, button.cssProperties().size());
        CssPropertyDef cssProp = button.cssProperties().get(0);
        assertEquals("--button-background", cssProp.name());
    }

    /**
     * Test parsing a CEM file with optional fields omitted.
     * Validates: Requirement 1.1
     */
    @Test
    void testParseWithOptionalFields() throws Exception {
        Path cemPath = getResourcePath("cem/with-optional-fields.json");
        
        List<ComponentDefinition> components = parser.parse(cemPath);
        
        assertNotNull(components);
        assertEquals(1, components.size());
        
        ComponentDefinition component = components.get(0);
        assertEquals("test-minimal", component.tagName());
        assertEquals("MinimalComponent", component.className());
        
        // Optional fields should have defaults
        assertTrue(component.properties().isEmpty());
        assertTrue(component.events().isEmpty());
        assertTrue(component.slots().isEmpty());
        assertTrue(component.methods().isEmpty());
        assertTrue(component.cssProperties().isEmpty());
    }

    /**
     * Test that private components are filtered out.
     * Validates: Requirement 10.5
     */
    @Test
    void testFilterPrivateComponents() throws Exception {
        Path cemPath = getResourcePath("cem/with-private-component.json");
        
        List<ComponentDefinition> components = parser.parse(cemPath);
        
        assertNotNull(components);
        assertEquals(1, components.size());
        
        // Only public component should be present
        ComponentDefinition component = components.get(0);
        assertEquals("test-public", component.tagName());
        assertEquals("PublicComponent", component.className());
    }

    // ========== Error Handling Tests ==========

    /**
     * Test error message for malformed JSON.
     * Validates: Requirement 1.4
     */
    @Test
    void testMalformedJSON() throws Exception {
        Path cemPath = getResourcePath("cem/malformed.json");
        
        CEMParseException exception = assertThrows(CEMParseException.class, () -> {
            parser.parse(cemPath);
        });
        
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Malformed JSON") || 
                   exception.getMessage().contains("Invalid JSON"),
                   "Error message should indicate malformed JSON");
    }

    /**
     * Test error message for missing required 'modules' field.
     * Validates: Requirement 1.4
     */
    @Test
    void testMissingModulesField() throws Exception {
        Path cemPath = getResourcePath("cem/missing-modules.json");
        
        CEMParseException exception = assertThrows(CEMParseException.class, () -> {
            parser.parse(cemPath);
        });
        
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("modules"),
                   "Error message should mention missing 'modules' field");
    }

    // ========== Validation Tests ==========

    /**
     * Test validation of a valid CEM file.
     * Validates: Requirement 1.2
     */
    @Test
    void testValidateValidCEM() throws Exception {
        Path cemPath = getResourcePath("cem/valid-button.json");
        
        ValidationResult result = parser.validate(cemPath);
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    /**
     * Test validation detects missing modules.
     * Validates: Requirement 10.5
     */
    @Test
    void testValidateDetectsMissingModules() throws Exception {
        Path cemPath = getResourcePath("cem/missing-modules.json");
        
        ValidationResult result = parser.validate(cemPath);
        
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.contains("modules")));
    }
}
