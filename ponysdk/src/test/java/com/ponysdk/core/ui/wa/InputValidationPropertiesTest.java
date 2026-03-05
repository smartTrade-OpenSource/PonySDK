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

import com.ponysdk.core.ui.component.PropsDiffer;
import com.ponysdk.core.ui.wa.PropsSerializationTest.InputProps;
import com.ponysdk.core.ui.wa.codegen.ComponentDefinition;
import com.ponysdk.core.ui.wa.codegen.PropertyDef;
import com.ponysdk.core.ui.wa.codegen.WebAwesomeCodeGenerator;
import org.junit.jupiter.api.Test;

import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying that input component wrappers expose all required
 * validation properties: required, minLength, maxLength, min, max, pattern,
 * customValidity, disabled, and readonly.
 *
 * <p>Validates: Requirements 1.1, 1.4, 1.6</p>
 */
public class InputValidationPropertiesTest {

    private static final Set<String> REQUIRED_VALIDATION_PROPS = Set.of(
        "required", "minlength", "maxlength", "min", "max",
        "pattern", "customValidity", "disabled", "readonly"
    );

    // ========== Code Generator Enrichment Tests ==========

    @Test
    void enrichment_addsValidationPropsToInputComponent() {
        final ComponentDefinition bare = new ComponentDefinition(
            "wa-input", "WaInput", "Input component", "",
            List.of(new PropertyDef("value", "string", "String", "The value", null, false)),
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), "stable"
        );

        // Create a mock JsonObject with formAssociated field
        final JsonObject mockDecl = javax.json.Json.createObjectBuilder()
            .add("members", javax.json.Json.createArrayBuilder()
                .add(javax.json.Json.createObjectBuilder()
                    .add("kind", "field")
                    .add("name", "formAssociated")
                    .add("static", true)))
            .build();

        final ComponentDefinition enriched = WebAwesomeCodeGenerator.enrichInputComponentProperties(bare, mockDecl);

        final Set<String> propNames = enriched.properties().stream()
            .map(PropertyDef::name)
            .collect(Collectors.toSet());

        for (final String required : REQUIRED_VALIDATION_PROPS) {
            assertTrue(propNames.contains(required),
                "Input component should have validation property: " + required);
        }
        // Original property preserved
        assertTrue(propNames.contains("value"), "Original 'value' property should be preserved");
    }

    @Test
    void enrichment_preservesExistingValidationProps() {
        final List<PropertyDef> existing = new ArrayList<>();
        existing.add(new PropertyDef("value", "string", "String", "The value", null, false));
        existing.add(new PropertyDef("disabled", "boolean", "boolean", "Whether disabled", "false", true));
        existing.add(new PropertyDef("required", "boolean", "boolean", "Whether required", "false", true));

        final ComponentDefinition bare = new ComponentDefinition(
            "wa-textarea", "WaTextarea", "Textarea component", "",
            existing, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), "stable"
        );

        // Create a mock JsonObject with formAssociated field
        final JsonObject mockDecl = javax.json.Json.createObjectBuilder()
            .add("members", javax.json.Json.createArrayBuilder()
                .add(javax.json.Json.createObjectBuilder()
                    .add("kind", "field")
                    .add("name", "formAssociated")
                    .add("static", true)))
            .build();

        final ComponentDefinition enriched = WebAwesomeCodeGenerator.enrichInputComponentProperties(bare, mockDecl);

        // Count occurrences of 'disabled' — should be exactly 1 (not duplicated)
        final long disabledCount = enriched.properties().stream()
            .filter(p -> "disabled".equals(p.name()))
            .count();
        assertEquals(1, disabledCount, "Existing 'disabled' property should not be duplicated");

        final long requiredCount = enriched.properties().stream()
            .filter(p -> "required".equals(p.name()))
            .count();
        assertEquals(1, requiredCount, "Existing 'required' property should not be duplicated");

        // But missing ones should be added
        final Set<String> propNames = enriched.properties().stream()
            .map(PropertyDef::name)
            .collect(Collectors.toSet());
        assertTrue(propNames.contains("minlength"));
        assertTrue(propNames.contains("maxlength"));
        assertTrue(propNames.contains("min"));
        assertTrue(propNames.contains("max"));
        assertTrue(propNames.contains("pattern"));
        assertTrue(propNames.contains("customValidity"));
        assertTrue(propNames.contains("readonly"));
    }

    @Test
    void enrichment_doesNotModifyNonInputComponents() {
        final ComponentDefinition button = new ComponentDefinition(
            "wa-button", "WaButton", "Button component", "",
            List.of(new PropertyDef("disabled", "boolean", "boolean", "Whether disabled", "false", true)),
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), "stable"
        );

        // Create a mock JsonObject without formAssociated field
        final JsonObject mockDecl = javax.json.Json.createObjectBuilder()
            .add("members", javax.json.Json.createArrayBuilder())
            .build();

        final ComponentDefinition result = WebAwesomeCodeGenerator.enrichInputComponentProperties(button, mockDecl);

        // Should be the same instance — no enrichment for non-input components
        assertSame(button, result, "Non-input components should not be enriched");
        assertEquals(1, result.properties().size());
    }

    @Test
    void enrichment_worksForFormAssociatedComponents() {
        // Test with a few known form-associated components
        final String[] formComponents = {"wa-input", "wa-textarea", "wa-select"};
        
        for (final String tagName : formComponents) {
            final ComponentDefinition bare = new ComponentDefinition(
                tagName, "Wa" + tagName.substring(3), tagName + " component", "",
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), "stable"
            );

            // Create a mock JsonObject with formAssociated field
            final JsonObject mockDecl = javax.json.Json.createObjectBuilder()
                .add("members", javax.json.Json.createArrayBuilder()
                    .add(javax.json.Json.createObjectBuilder()
                        .add("kind", "field")
                        .add("name", "formAssociated")
                        .add("static", true)))
                .build();

            final ComponentDefinition enriched = WebAwesomeCodeGenerator.enrichInputComponentProperties(bare, mockDecl);

            final Set<String> propNames = enriched.properties().stream()
                .map(PropertyDef::name)
                .collect(Collectors.toSet());

            for (final String required : REQUIRED_VALIDATION_PROPS) {
                assertTrue(propNames.contains(required),
                    tagName + " should have validation property: " + required);
            }
        }
    }

    // ========== Generated Props Record Validation Tests ==========

    @Test
    void generatedPropsRecord_containsValidationFields() {
        final ComponentDefinition inputDef = new ComponentDefinition(
            "wa-input", "WaInput", "Input component", "",
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), "stable"
        );

        // Create a mock JsonObject with formAssociated field
        final JsonObject mockDecl = javax.json.Json.createObjectBuilder()
            .add("members", javax.json.Json.createArrayBuilder()
                .add(javax.json.Json.createObjectBuilder()
                    .add("kind", "field")
                    .add("name", "formAssociated")
                    .add("static", true)))
            .build();

        final ComponentDefinition enriched = WebAwesomeCodeGenerator.enrichInputComponentProperties(inputDef, mockDecl);
        final WebAwesomeCodeGenerator generator = new WebAwesomeCodeGenerator(
            java.nio.file.Path.of("dummy"), java.nio.file.Path.of("dummy"), java.nio.file.Path.of("dummy")
        );
        final String propsSource = generator.generatePropsRecord(enriched);

        // Verify all validation fields appear in the generated record
        assertTrue(propsSource.contains("boolean required"), "Generated record should contain 'required' field");
        assertTrue(propsSource.contains("double minlength"), "Generated record should contain 'minlength' field");
        assertTrue(propsSource.contains("double maxlength"), "Generated record should contain 'maxlength' field");
        assertTrue(propsSource.contains("double min"), "Generated record should contain 'min' field");
        assertTrue(propsSource.contains("double max"), "Generated record should contain 'max' field");
        assertTrue(propsSource.contains("String pattern"), "Generated record should contain 'pattern' field");
        assertTrue(propsSource.contains("String customValidity"), "Generated record should contain 'customValidity' field");
        assertTrue(propsSource.contains("boolean disabled"), "Generated record should contain 'disabled' field");
        assertTrue(propsSource.contains("boolean readonly"), "Generated record should contain 'readonly' field");
    }

    @Test
    void generatedWrapperClass_containsValidationSetters() {
        final ComponentDefinition inputDef = new ComponentDefinition(
            "wa-input", "WaInput", "Input component", "",
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), "stable"
        );

        // Create a mock JsonObject with formAssociated field
        final JsonObject mockDecl = javax.json.Json.createObjectBuilder()
            .add("members", javax.json.Json.createArrayBuilder()
                .add(javax.json.Json.createObjectBuilder()
                    .add("kind", "field")
                    .add("name", "formAssociated")
                    .add("static", true)))
            .build();

        final ComponentDefinition enriched = WebAwesomeCodeGenerator.enrichInputComponentProperties(inputDef, mockDecl);
        final WebAwesomeCodeGenerator generator = new WebAwesomeCodeGenerator(
            java.nio.file.Path.of("dummy"), java.nio.file.Path.of("dummy"), java.nio.file.Path.of("dummy")
        );
        final String wrapperSource = generator.generateWrapperClass(enriched);

        assertTrue(wrapperSource.contains("setRequired"), "Wrapper should have setRequired setter");
        assertTrue(wrapperSource.contains("setMinlength"), "Wrapper should have setMinlength setter");
        assertTrue(wrapperSource.contains("setMaxlength"), "Wrapper should have setMaxlength setter");
        assertTrue(wrapperSource.contains("setMin"), "Wrapper should have setMin setter");
        assertTrue(wrapperSource.contains("setMax"), "Wrapper should have setMax setter");
        assertTrue(wrapperSource.contains("setPattern"), "Wrapper should have setPattern setter");
        assertTrue(wrapperSource.contains("setCustomValidity"), "Wrapper should have setCustomValidity setter");
        assertTrue(wrapperSource.contains("setDisabled"), "Wrapper should have setDisabled setter");
        assertTrue(wrapperSource.contains("setReadonly"), "Wrapper should have setReadonly setter");
    }

    // ========== Sample InputProps Serialization Tests ==========

    @Test
    void inputProps_validationFieldsSerializeCorrectly() {
        final PropsDiffer<InputProps> differ = new PropsDiffer<>();
        final InputProps props = new InputProps("text", "hello", "Name",
            true, true, true,
            Optional.of(3), Optional.of(50),
            Optional.of(0.0), Optional.of(100.0),
            Optional.of("[a-z]+"), "Custom error message", Size.MEDIUM);

        final JsonObject json = differ.toJson(props);

        assertTrue(json.getBoolean("disabled"), "disabled should be true");
        assertTrue(json.getBoolean("readonly"), "readonly should be true");
        assertTrue(json.getBoolean("required"), "required should be true");
        assertEquals(3, json.getInt("minlength"));
        assertEquals(50, json.getInt("maxlength"));
        assertEquals(0.0, json.getJsonNumber("min").doubleValue(), 0.001);
        assertEquals(100.0, json.getJsonNumber("max").doubleValue(), 0.001);
        assertEquals("[a-z]+", json.getString("pattern"));
        assertEquals("Custom error message", json.getString("customValidity"));
    }

    @Test
    void inputProps_validationFieldsRoundTrip() {
        final PropsDiffer<InputProps> differ = new PropsDiffer<>();
        final InputProps original = new InputProps("number", "42", "Age",
            false, true, true,
            Optional.of(1), Optional.of(3),
            Optional.of(0.0), Optional.of(150.0),
            Optional.of("\\d+"), "", Size.SMALL);

        final JsonObject json = differ.toJson(original);
        final InputProps restored = differ.fromJson(json, InputProps.class);

        assertEquals(original, restored, "Round-trip with validation fields should produce equivalent record");
    }

    @Test
    void inputProps_disabledAndReadonlyDefaults() {
        final InputProps defaults = InputProps.defaults();

        assertFalse(defaults.disabled(), "disabled should default to false");
        assertFalse(defaults.readonly(), "readonly should default to false");
        assertFalse(defaults.required(), "required should default to false");
        assertEquals("", defaults.customValidity(), "customValidity should default to empty string");
    }
}
