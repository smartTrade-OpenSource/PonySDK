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

package com.ponysdk.core.ui.wa.codegen;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WebAwesomeCodeGenerator#generatePropsRecord(ComponentDefinition)}.
 */
class GeneratePropsRecordTest {

    private WebAwesomeCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new WebAwesomeCodeGenerator(
            Path.of("dummy-manifest.json"), Path.of("dummy-java"), Path.of("dummy-ts"));
    }

    @Test
    void tagNameToPropsClassName_simpleTag() {
        assertEquals("InputProps", WebAwesomeCodeGenerator.tagNameToPropsClassName("wa-input"));
    }

    @Test
    void tagNameToPropsClassName_multiPartTag() {
        assertEquals("TabGroupProps", WebAwesomeCodeGenerator.tagNameToPropsClassName("wa-tab-group"));
    }

    @Test
    void kebabToCamelCase_simple() {
        assertEquals("helpText", WebAwesomeCodeGenerator.kebabToCamelCase("help-text"));
    }

    @Test
    void kebabToCamelCase_noHyphen() {
        assertEquals("value", WebAwesomeCodeGenerator.kebabToCamelCase("value"));
    }

    @Test
    void generatePropsRecord_packageAndClassName() {
        final ComponentDefinition def = createDef("wa-input",
            List.of(prop("value", "string", "String")));

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("package com.ponysdk.core.ui.wa.props;"));
        assertTrue(source.contains("public record InputProps("));
    }

    @Test
    void generatePropsRecord_fieldsMatchProperties() {
        final ComponentDefinition def = createDef("wa-button",
            List.of(
                prop("disabled", "boolean", "boolean"),
                prop("variant", "string", "String")
            ));

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("boolean disabled"));
        assertTrue(source.contains("String variant"));
    }

    @Test
    void generatePropsRecord_kebabFieldsConvertedToCamelCase() {
        final ComponentDefinition def = createDef("wa-input",
            List.of(prop("help-text", "string", "String")));

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("String helpText"));
        assertFalse(source.contains("help-text"));
    }

    @Test
    void generatePropsRecord_defaultsMethod() {
        final ComponentDefinition def = createDef("wa-input",
            List.of(
                prop("value", "string", "String"),
                prop("disabled", "boolean", "boolean"),
                prop("count", "number", "double")
            ));

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("public static InputProps defaults()"));
        assertTrue(source.contains("\"\""));
        assertTrue(source.contains("false"));
        assertTrue(source.contains("0.0"));
    }

    @Test
    void generatePropsRecord_withMethods() {
        final ComponentDefinition def = createDef("wa-input",
            List.of(
                prop("value", "string", "String"),
                prop("disabled", "boolean", "boolean")
            ));

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("public InputProps withValue(final String value)"));
        assertTrue(source.contains("public InputProps withDisabled(final boolean disabled)"));
    }

    @Test
    void generatePropsRecord_optionalImportIncludedWhenNeeded() {
        final ComponentDefinition def = createDef("wa-input",
            List.of(prop("pattern", "string | undefined", "Optional<String>")));

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("import java.util.Optional;"));
        assertTrue(source.contains("Optional.empty()"));
    }

    @Test
    void generatePropsRecord_optionalImportOmittedWhenNotNeeded() {
        final ComponentDefinition def = createDef("wa-button",
            List.of(prop("disabled", "boolean", "boolean")));

        final String source = generator.generatePropsRecord(def);

        assertFalse(source.contains("import java.util.Optional;"));
    }

    @Test
    void generatePropsRecord_javadocContainsTagName() {
        final ComponentDefinition def = createDef("wa-tab-group",
            List.of(prop("value", "string", "String")));

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("wa-tab-group"));
        assertTrue(source.contains("@generated"));
        assertTrue(source.contains("public record TabGroupProps("));
    }

    @Test
    void generatePropsRecord_objectTypeDefaultsToNull() {
        final ComponentDefinition def = createDef("wa-input",
            List.of(prop("data", "SomeUnknownType", "Object")));

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("Object data"));
        // defaults() should contain null for Object type
        assertTrue(source.contains("null"));
    }

    @Test
    void generatePropsRecord_withMethodUsesThisAccessorForOtherFields() {
        final ComponentDefinition def = createDef("wa-input",
            List.of(
                prop("value", "string", "String"),
                prop("disabled", "boolean", "boolean")
            ));

        final String source = generator.generatePropsRecord(def);

        // withValue should reference this.disabled()
        assertTrue(source.contains("this.disabled()"));
        // withDisabled should reference this.value()
        assertTrue(source.contains("this.value()"));
    }

    @Test
    void generatePropsRecord_listTypeIncludesImport() {
        final ComponentDefinition def = createDef("wa-select",
            List.of(prop("options", "string[]", "List<String>")));

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("import java.util.List;"));
        assertTrue(source.contains("List<String> options"));
    }

    @Test
    void generatePropsRecord_listTypeDefaultsToEmptyList() {
        final ComponentDefinition def = createDef("wa-select",
            List.of(prop("items", "string[]", "List<String>")));

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("List.of()"));
    }

    @Test
    void generatePropsRecord_multipleTypesHandledCorrectly() {
        final ComponentDefinition def = createDef("wa-input",
            List.of(
                prop("value", "string", "String"),
                prop("count", "number", "double"),
                prop("enabled", "boolean", "boolean"),
                prop("pattern", "string | undefined", "Optional<String>"),
                prop("tags", "string[]", "List<String>")
            ));

        final String source = generator.generatePropsRecord(def);

        // Verify all imports
        assertTrue(source.contains("import java.util.Optional;"));
        assertTrue(source.contains("import java.util.List;"));

        // Verify all fields
        assertTrue(source.contains("String value"));
        assertTrue(source.contains("double count"));
        assertTrue(source.contains("boolean enabled"));
        assertTrue(source.contains("Optional<String> pattern"));
        assertTrue(source.contains("List<String> tags"));

        // Verify defaults
        assertTrue(source.contains("\"\""));
        assertTrue(source.contains("0.0"));
        assertTrue(source.contains("false"));
        assertTrue(source.contains("Optional.empty()"));
        assertTrue(source.contains("List.of()"));
    }

    @Test
    void generatePropsRecord_emptyPropsGeneratesValidRecord() {
        final ComponentDefinition def = createDef("wa-divider", Collections.emptyList());

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("public record DividerProps("));
        assertTrue(source.contains("public static DividerProps defaults()"));
        // Empty record should have empty parameter list
        assertTrue(source.contains("DividerProps(\n)"));
    }

    @Test
    void generatePropsRecord_recordStructureSupportsEqualsAndHashCode() {
        final ComponentDefinition def = createDef("wa-button",
            List.of(
                prop("variant", "string", "String"),
                prop("disabled", "boolean", "boolean")
            ));

        final String source = generator.generatePropsRecord(def);

        // Verify it's a record (records automatically implement equals/hashCode)
        assertTrue(source.contains("public record ButtonProps("));
        // Verify all fields are in the record declaration (required for proper equals/hashCode)
        assertTrue(source.contains("String variant"));
        assertTrue(source.contains("boolean disabled"));
    }

    @Test
    void generatePropsRecord_builderMethodsReturnNewInstance() {
        final ComponentDefinition def = createDef("wa-input",
            List.of(
                prop("value", "string", "String"),
                prop("disabled", "boolean", "boolean")
            ));

        final String source = generator.generatePropsRecord(def);

        // Verify builder methods create new instances
        assertTrue(source.contains("return new InputProps("));
        // Verify withValue creates new instance with updated value
        assertTrue(source.contains("public InputProps withValue(final String value)"));
        assertTrue(source.contains("return new InputProps(\n            value,"));
    }

    @Test
    void generatePropsRecord_defaultsMethodReturnsNewInstance() {
        final ComponentDefinition def = createDef("wa-button",
            List.of(
                prop("variant", "string", "String"),
                prop("size", "string", "String")
            ));

        final String source = generator.generatePropsRecord(def);

        // Verify defaults() creates new instance
        assertTrue(source.contains("public static ButtonProps defaults()"));
        assertTrue(source.contains("return new ButtonProps("));
    }

    @Test
    void generatePropsRecord_allFieldsIncludedInRecordDeclaration() {
        final ComponentDefinition def = createDef("wa-input",
            List.of(
                prop("value", "string", "String"),
                prop("placeholder", "string", "String"),
                prop("disabled", "boolean", "boolean"),
                prop("readonly", "boolean", "boolean")
            ));

        final String source = generator.generatePropsRecord(def);

        // Verify record declaration includes all fields
        assertTrue(source.contains("public record InputProps("));
        assertTrue(source.contains("String value"));
        assertTrue(source.contains("String placeholder"));
        assertTrue(source.contains("boolean disabled"));
        assertTrue(source.contains("boolean readonly"));

        // Verify all fields have builder methods
        assertTrue(source.contains("withValue"));
        assertTrue(source.contains("withPlaceholder"));
        assertTrue(source.contains("withDisabled"));
        assertTrue(source.contains("withReadonly"));
    }

    private static ComponentDefinition createDef(final String tagName, final List<PropertyDef> properties) {
        return new ComponentDefinition(
            tagName, "Wa" + tagName.substring(3), "", "",
            properties,
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(),
            "stable"
        );
    }

    private static PropertyDef prop(final String name, final String jsType, final String javaType) {
        return new PropertyDef(name, jsType, javaType, "", null, false);
    }
}
