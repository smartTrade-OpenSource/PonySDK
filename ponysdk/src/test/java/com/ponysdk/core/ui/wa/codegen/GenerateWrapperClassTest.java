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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WebAwesomeCodeGenerator#generateWrapperClass(ComponentDefinition)}
 * and the {@code tagNameToWrapperClassName} helper.
 */
class GenerateWrapperClassTest {

    private WebAwesomeCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new WebAwesomeCodeGenerator(
            Path.of("dummy-manifest.json"),
            Path.of("dummy-java-out"),
            Path.of("dummy-ts-out")
        );
    }

    // --- tagNameToWrapperClassName tests ---

    @Test
    void tagNameToWrapperClassName_simpleTag() {
        assertEquals("PInput", WebAwesomeCodeGenerator.tagNameToWrapperClassName("wa-input"));
    }

    @Test
    void tagNameToWrapperClassName_multiPartTag() {
        assertEquals("PTabGroup", WebAwesomeCodeGenerator.tagNameToWrapperClassName("wa-tab-group"));
    }

    @Test
    void tagNameToWrapperClassName_singleSegment() {
        assertEquals("PButton", WebAwesomeCodeGenerator.tagNameToWrapperClassName("wa-button"));
    }

    @Test
    void tagNameToWrapperClassName_threeSegments() {
        assertEquals("PAnimatedImage", WebAwesomeCodeGenerator.tagNameToWrapperClassName("wa-animated-image"));
    }

    // --- generateWrapperClass: basic structure ---

    @Test
    void generatesCorrectPackageAndImports() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(
            prop("value", "String")
        ));

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("package com.ponysdk.core.ui.wa;"));
        assertTrue(source.contains("import com.ponysdk.core.ui.component.PWebComponent;"));
        assertTrue(source.contains("import com.ponysdk.core.ui.wa.props.InputProps;"));
    }

    @Test
    void generatesClassExtendingPWebComponent() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(
            prop("value", "String")
        ));

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public class PInput extends PWebComponent<InputProps>"));
    }

    @Test
    void generatesDefaultConstructor() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(
            prop("value", "String")
        ));

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public PInput() {"));
        assertTrue(source.contains("super(InputProps.defaults());"));
    }

    @Test
    void generatesPropsConstructor() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(
            prop("value", "String")
        ));

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public PInput(final InputProps initialProps) {"));
        assertTrue(source.contains("super(initialProps);"));
    }

    @Test
    void generatesGetPropsClass() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(
            prop("value", "String")
        ));

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("protected Class<InputProps> getPropsClass()"));
        assertTrue(source.contains("return InputProps.class;"));
    }

    @Test
    void generatesGetComponentSignature() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(
            prop("value", "String")
        ));

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("protected String getComponentSignature()"));
        assertTrue(source.contains("return \"wa-input\";"));
    }

    // --- Convenience setters ---

    @Test
    void generatesSetterForStringProperty() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(
            prop("value", "String")
        ));

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public void setValue(final String value)"));
        assertTrue(source.contains("final InputProps current = getCurrentProps();"));
        assertTrue(source.contains("setProps(current.withValue(value));"));
    }

    @Test
    void generatesSetterForBooleanProperty() {
        final ComponentDefinition def = minimalDef("wa-button", List.of(
            prop("disabled", "boolean")
        ));

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public void setDisabled(final boolean disabled)"));
        assertTrue(source.contains("setProps(current.withDisabled(disabled));"));
    }

    @Test
    void generatesSetterForKebabCaseProperty() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(
            prop("help-text", "String")
        ));

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public void setHelpText(final String helpText)"));
        assertTrue(source.contains("setProps(current.withHelpText(helpText));"));
    }

    @Test
    void generatesMultipleSetters() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(
            prop("value", "String"),
            prop("disabled", "boolean"),
            prop("required", "boolean")
        ));

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public void setValue(final String value)"));
        assertTrue(source.contains("public void setDisabled(final boolean disabled)"));
        assertTrue(source.contains("public void setRequired(final boolean required)"));
    }

    // --- Javadoc ---

    @Test
    void javadocIncludesSummary() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-input", "WaInput", "A text input field.", "",
            List.of(prop("value", "String")),
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("<p>A text input field.</p>"));
    }

    @Test
    void javadocIncludesSlots() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-input", "WaInput", "", "",
            List.of(prop("value", "String")),
            Collections.emptyList(),
            List.of(new SlotDef("prefix", "Content before the input"),
                    new SlotDef("suffix", "Content after the input")),
            Collections.emptyList(), Collections.emptyList(), "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("<h3>Available Slots</h3>"));
        assertTrue(source.contains("{@code prefix} - Content before the input"));
        assertTrue(source.contains("{@code suffix} - Content after the input"));
    }

    @Test
    void javadocIncludesEvents() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-input", "WaInput", "", "",
            List.of(prop("value", "String")),
            List.of(new EventDef("wa-change", "Value changed", null),
                    new EventDef("wa-input", "Input in progress", null)),
            Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("<h3>Events</h3>"));
        assertTrue(source.contains("{@code wa-change} - Value changed"));
        assertTrue(source.contains("{@code wa-input} - Input in progress"));
    }

    @Test
    void javadocIncludesCssParts() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-input", "WaInput", "", "",
            List.of(prop("value", "String")),
            Collections.emptyList(), Collections.emptyList(),
            List.of(new CssPartDef("base", "The base wrapper"),
                    new CssPartDef("input", "The native input")),
            Collections.emptyList(), "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("<h3>CSS Parts</h3>"));
        assertTrue(source.contains("{@code base} - The base wrapper"));
        assertTrue(source.contains("{@code input} - The native input"));
    }

    @Test
    void javadocIncludesGeneratedAnnotation() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(
            prop("value", "String")
        ));

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("@generated from custom-elements.json (Web Awesome)"));
    }

    @Test
    void javadocHandlesDefaultSlot() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button", "WaButton", "", "",
            List.of(prop("disabled", "boolean")),
            Collections.emptyList(),
            List.of(new SlotDef("", "The button content")),
            Collections.emptyList(), Collections.emptyList(), "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("{@code (default)} - The button content"));
    }

    // --- Multi-segment tag names ---

    @Test
    void generatesCorrectClassForMultiSegmentTag() {
        final ComponentDefinition def = minimalDef("wa-tab-group", List.of(
            prop("placement", "String")
        ));

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public class PTabGroup extends PWebComponent<TabGroupProps>"));
        assertTrue(source.contains("return \"wa-tab-group\";"));
    }

    // --- No properties edge case ---

    @Test
    void generatesWrapperWithNoProperties() {
        final ComponentDefinition def = minimalDef("wa-divider", Collections.emptyList());

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public class WADivider extends PWebComponent<DividerProps>"));
        // No setters should be generated
        assertFalse(source.contains("public void set"));
    }

    // --- Helper methods ---

    private static ComponentDefinition minimalDef(final String tagName, final List<PropertyDef> props) {
        return new ComponentDefinition(
            tagName, "Wa" + tagName.substring(3), "", "",
            props,
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), "stable"
        );
    }

    private static PropertyDef prop(final String name, final String javaType) {
        return new PropertyDef(name, name.equals("boolean") ? "boolean" : "string", javaType, "", null, false);
    }
}
