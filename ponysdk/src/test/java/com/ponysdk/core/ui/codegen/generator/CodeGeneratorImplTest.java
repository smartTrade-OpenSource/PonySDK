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

package com.ponysdk.core.ui.codegen.generator;

import com.ponysdk.core.ui.codegen.model.ComponentDefinition;
import com.ponysdk.core.ui.codegen.model.MethodDef;
import com.ponysdk.core.ui.codegen.model.ParameterDef;
import com.ponysdk.core.ui.codegen.model.PropertyDef;
import com.ponysdk.core.ui.codegen.model.SlotDef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CodeGeneratorImpl#generateWrapperClass(ComponentDefinition)}.
 */
class CodeGeneratorImplTest {

    private CodeGeneratorImpl generator;

    @BeforeEach
    void setUp() {
        generator = new CodeGeneratorImpl("com.ponysdk.core.ui.webawesome");
    }

    @Test
    void generateWrapperClass_throwsExceptionForNullDefinition() {
        assertThrows(IllegalArgumentException.class, () -> generator.generateWrapperClass(null));
    }

    @Test
    void generateWrapperClass_generatesCorrectPackage() {
        final ComponentDefinition def = createMinimalDef("wa-button");

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("package com.ponysdk.core.ui.webawesome;"));
    }

    @Test
    void generateWrapperClass_importsRequiredClasses() {
        final ComponentDefinition def = createMinimalDef("wa-button");

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("import com.ponysdk.core.ui.component.PWebComponent;"));
    }

    @Test
    void generateWrapperClass_extendsCorrectBaseClass() {
        final ComponentDefinition def = createMinimalDef("wa-button");

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public class PButton extends PWebComponent<ButtonProps>"));
    }

    @Test
    void generateWrapperClass_includesTagNameConstant() {
        final ComponentDefinition def = createMinimalDef("wa-button");

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("private static final String TAG_NAME = \"wa-button\";"));
    }

    @Test
    void generateWrapperClass_includesDeclaredSlotsWhenPresent() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "Detailed description",
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new SlotDef("prefix", "Prefix slot"),
                new SlotDef("suffix", "Suffix slot")
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("import java.util.Set;"));
        assertTrue(source.contains("private static final Set<String> DECLARED_SLOTS = Set.of(\"prefix\", \"suffix\");"));
    }

    @Test
    void generateWrapperClass_doesNotIncludeDeclaredSlotsWhenAbsent() {
        final ComponentDefinition def = createMinimalDef("wa-button");

        final String source = generator.generateWrapperClass(def);

        assertFalse(source.contains("DECLARED_SLOTS"));
        assertFalse(source.contains("import java.util.Set;"));
    }

    @Test
    void generateWrapperClass_includesDefaultConstructor() {
        final ComponentDefinition def = createMinimalDef("wa-button");

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public PButton() {"));
        assertTrue(source.contains("this(ButtonProps.defaults());"));
    }

    @Test
    void generateWrapperClass_includesConstructorWithProps() {
        final ComponentDefinition def = createMinimalDef("wa-button");

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public PButton(final ButtonProps initialProps) {"));
        assertTrue(source.contains("super(initialProps);"));
    }

    @Test
    void generateWrapperClass_constructorCallsSuperWithSlotsWhenPresent() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "Detailed description",
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(new SlotDef("prefix", "Prefix slot")),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("super(initialProps, DECLARED_SLOTS);"));
    }

    @Test
    void generateWrapperClass_includesGetPropsClassOverride() {
        final ComponentDefinition def = createMinimalDef("wa-button");

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("@Override"));
        assertTrue(source.contains("protected Class<ButtonProps> getPropsClass() {"));
        assertTrue(source.contains("return ButtonProps.class;"));
    }

    @Test
    void generateWrapperClass_includesGetComponentSignatureOverride() {
        final ComponentDefinition def = createMinimalDef("wa-button");

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("@Override"));
        assertTrue(source.contains("protected String getComponentSignature() {"));
        assertTrue(source.contains("return TAG_NAME;"));
    }

    @Test
    void generateWrapperClass_includesJavadocWithSummary() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "Detailed description of the button",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("/**"));
        assertTrue(source.contains("* A button component"));
        assertTrue(source.contains("* Detailed description of the button"));
        assertTrue(source.contains("@see ButtonProps"));
    }

    @Test
    void generateWrapperClass_includesExperimentalWarningInJavadoc() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "experimental"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("<strong>Warning:</strong> This component is experimental"));
    }

    @Test
    void generateWrapperClass_includesDeprecatedWarningInJavadoc() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "deprecated"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("<strong>Deprecated:</strong> This component is deprecated"));
    }

    @Test
    void generateWrapperClass_escapesSpecialCharactersInJavadoc() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button with <special> & @characters",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("&lt;special&gt; &amp; &#64;characters"));
    }

    @Test
    void generateWrapperClass_handlesMultiWordComponentNames() {
        final ComponentDefinition def = createMinimalDef("wa-tab-group");

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public class PTabGroup extends PWebComponent<TabGroupProps>"));
        assertTrue(source.contains("private static final String TAG_NAME = \"wa-tab-group\";"));
    }

    @Test
    void generateWrapperClass_includesEventListenerImportsWhenEventsPresent() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            List.of(
                new com.ponysdk.core.ui.codegen.model.EventDef("wa-click", "Emitted when clicked", null, true, false)
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("import java.util.function.Consumer;"));
        assertTrue(source.contains("import javax.json.JsonObject;"));
    }

    @Test
    void generateWrapperClass_doesNotIncludeEventListenerImportsWhenNoEvents() {
        final ComponentDefinition def = createMinimalDef("wa-button");

        final String source = generator.generateWrapperClass(def);

        assertFalse(source.contains("import java.util.function.Consumer;"));
        assertFalse(source.contains("import javax.json.JsonObject;"));
    }

    @Test
    void generateWrapperClass_generatesEventListenerMethods() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            List.of(
                new com.ponysdk.core.ui.codegen.model.EventDef("wa-click", "Emitted when clicked", null, true, false),
                new com.ponysdk.core.ui.codegen.model.EventDef("wa-focus", "Emitted when focused", null, true, false)
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("// ========== GENERATED EVENT HANDLERS =========="));
        assertTrue(source.contains("public void addClickListener(final Consumer<JsonObject> handler)"));
        assertTrue(source.contains("onEvent(\"wa-click\", handler);"));
        assertTrue(source.contains("public void addFocusListener(final Consumer<JsonObject> handler)"));
        assertTrue(source.contains("onEvent(\"wa-focus\", handler);"));
        assertTrue(source.contains("// ========== END GENERATED CODE =========="));
    }

    @Test
    void generateWrapperClass_includesEventDescriptionInJavadoc() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            List.of(
                new com.ponysdk.core.ui.codegen.model.EventDef(
                    "wa-click", 
                    "Emitted when the button is clicked by the user", 
                    null, 
                    true, 
                    false
                )
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("Registers a handler for the 'wa-click' event"));
        assertTrue(source.contains("Emitted when the button is clicked by the user"));
    }

    @Test
    void generateWrapperClass_handlesMultiWordEventNames() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-dialog",
            "Dialog",
            "A dialog component",
            "",
            Collections.emptyList(),
            List.of(
                new com.ponysdk.core.ui.codegen.model.EventDef(
                    "wa-after-show", 
                    "Emitted after the dialog is shown", 
                    null, 
                    true, 
                    false
                )
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public void addAfterShowListener(final Consumer<JsonObject> handler)"));
        assertTrue(source.contains("onEvent(\"wa-after-show\", handler);"));
    }

    @Test
    void generateWrapperClass_includesPComponentImportWhenSlotsPresent() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(new SlotDef("prefix", "Prefix slot")),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("import com.ponysdk.core.ui.component.PComponent;"));
    }

    @Test
    void generateWrapperClass_generatesSlotMethodsForNamedSlots() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new SlotDef("prefix", "Content to place before the button text"),
                new SlotDef("suffix", "Content to place after the button text")
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("// ========== GENERATED SLOT METHODS =========="));
        assertTrue(source.contains("public void addPrefix(final PComponent<?> child)"));
        assertTrue(source.contains("addToSlot(\"prefix\", child);"));
        assertTrue(source.contains("public void addSuffix(final PComponent<?> child)"));
        assertTrue(source.contains("addToSlot(\"suffix\", child);"));
        assertTrue(source.contains("// ========== END GENERATED CODE =========="));
    }

    @Test
    void generateWrapperClass_generatesAddContentMethodForDefaultSlot() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-card",
            "Card",
            "A card component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(new SlotDef("", "Default slot for card content")),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public void addContent(final PComponent<?> child)"));
        assertTrue(source.contains("addToSlot(\"\", child);"));
    }

    @Test
    void generateWrapperClass_includesSlotDescriptionInJavadoc() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(new SlotDef("prefix", "Content to place before the button text")),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("Adds a component to the 'prefix' slot"));
        assertTrue(source.contains("Content to place before the button text"));
    }

    @Test
    void generateWrapperClass_handlesMultiWordSlotNames() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-menu",
            "Menu",
            "A menu component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(new SlotDef("menu-label", "Label for the menu")),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public void addMenuLabel(final PComponent<?> child)"));
        assertTrue(source.contains("addToSlot(\"menu-label\", child);"));
    }

    @Test
    void generateWrapperClass_generatesBothEventListenersAndSlotMethods() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            List.of(
                new com.ponysdk.core.ui.codegen.model.EventDef("wa-click", "Emitted when clicked", null, true, false)
            ),
            List.of(
                new SlotDef("prefix", "Prefix slot")
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        // Verify both sections are present
        assertTrue(source.contains("// ========== GENERATED EVENT HANDLERS =========="));
        assertTrue(source.contains("public void addClickListener(final Consumer<JsonObject> handler)"));
        assertTrue(source.contains("// ========== GENERATED SLOT METHODS =========="));
        assertTrue(source.contains("public void addPrefix(final PComponent<?> child)"));
    }

    private ComponentDefinition createMinimalDef(final String tagName) {
        return new ComponentDefinition(
            tagName,
            "Component",
            "A component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );
    }

    @Test
    void generateWrapperClass_includesCompletableFutureImportForAsyncMethods() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-dialog",
            "Dialog",
            "A dialog component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new MethodDef("show", "Shows the dialog", Collections.emptyList(), "Promise<void>", true)
            ),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("import java.util.concurrent.CompletableFuture;"));
    }

    @Test
    void generateWrapperClass_doesNotIncludeCompletableFutureImportForSyncMethods() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new MethodDef("focus", "Focuses the button", Collections.emptyList(), "void", false)
            ),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertFalse(source.contains("import java.util.concurrent.CompletableFuture;"));
    }

    @Test
    void generateWrapperClass_generatesMethodProxyForVoidMethod() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new MethodDef("focus", "Focuses the button", Collections.emptyList(), "void", false)
            ),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("// ========== GENERATED METHOD PROXIES =========="));
        assertTrue(source.contains("public void focus()"));
        assertTrue(source.contains("callComponentMethod(\"focus\");"));
        assertTrue(source.contains("// ========== END GENERATED CODE =========="));
    }

    @Test
    void generateWrapperClass_generatesMethodProxyWithParameters() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-input",
            "Input",
            "An input component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new MethodDef(
                    "setSelectionRange",
                    "Sets the selection range",
                    List.of(
                        new ParameterDef("start", "number", "int", "Start position"),
                        new ParameterDef("end", "number", "int", "End position")
                    ),
                    "void",
                    false
                )
            ),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public void setSelectionRange(final int start, final int end)"));
        assertTrue(source.contains("callComponentMethod(\"setSelectionRange\", start, end);"));
    }

    @Test
    void generateWrapperClass_generatesAsyncMethodProxyWithCompletableFuture() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-dialog",
            "Dialog",
            "A dialog component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new MethodDef("show", "Shows the dialog", Collections.emptyList(), "Promise<void>", true)
            ),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public CompletableFuture<Void> show()"));
        assertTrue(source.contains("return callComponentMethodAsync(\"show\");"));
    }

    @Test
    void generateWrapperClass_generatesAsyncMethodProxyWithReturnType() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-input",
            "Input",
            "An input component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new MethodDef("getValue", "Gets the current value", Collections.emptyList(), "Promise<string>", true)
            ),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public CompletableFuture<String> getValue()"));
        assertTrue(source.contains("return callComponentMethodAsync(\"getValue\");"));
    }

    @Test
    void generateWrapperClass_includesMethodDescriptionInJavadoc() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new MethodDef("focus", "Focuses the button element", Collections.emptyList(), "void", false)
            ),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("/**"));
        assertTrue(source.contains("* Focuses the button element"));
        assertTrue(source.contains("*/"));
    }

    @Test
    void generateWrapperClass_includesParameterDescriptionsInJavadoc() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-input",
            "Input",
            "An input component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new MethodDef(
                    "setSelectionRange",
                    "Sets the selection range",
                    List.of(
                        new ParameterDef("start", "number", "int", "The start position"),
                        new ParameterDef("end", "number", "int", "The end position")
                    ),
                    "void",
                    false
                )
            ),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("@param start The start position"));
        assertTrue(source.contains("@param end The end position"));
    }

    @Test
    void generateWrapperClass_includesReturnDescriptionForAsyncMethods() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-dialog",
            "Dialog",
            "A dialog component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new MethodDef("show", "Shows the dialog", Collections.emptyList(), "Promise<void>", true)
            ),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("@return future containing the result"));
    }

    @Test
    void generateWrapperClass_generatesMethodProxyWithNonVoidReturnType() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-input",
            "Input",
            "An input component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new MethodDef("checkValidity", "Checks validity", Collections.emptyList(), "boolean", false)
            ),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public boolean checkValidity()"));
        assertTrue(source.contains("return callComponentMethod(\"checkValidity\");"));
        assertTrue(source.contains("@return the result"));
    }

    @Test
    void generateWrapperClass_generatesMultipleMethodProxies() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-input",
            "Input",
            "An input component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new MethodDef("focus", "Focuses the input", Collections.emptyList(), "void", false),
                new MethodDef("blur", "Blurs the input", Collections.emptyList(), "void", false),
                new MethodDef("select", "Selects the text", Collections.emptyList(), "void", false)
            ),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("public void focus()"));
        assertTrue(source.contains("public void blur()"));
        assertTrue(source.contains("public void select()"));
    }

    @Test
    void generateWrapperClass_combinesEventListenersAndSlotMethodsAndMethodProxies() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-input",
            "Input",
            "An input component",
            "",
            Collections.emptyList(),
            List.of(
                new com.ponysdk.core.ui.codegen.model.EventDef("wa-input", "Emitted on input", null, true, false)
            ),
            List.of(
                new SlotDef("prefix", "Prefix slot")
            ),
            List.of(
                new MethodDef("focus", "Focuses the input", Collections.emptyList(), "void", false)
            ),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        // Verify all three sections are present
        assertTrue(source.contains("// ========== GENERATED EVENT HANDLERS =========="));
        assertTrue(source.contains("public void addInputListener(final Consumer<JsonObject> handler)"));
        assertTrue(source.contains("// ========== GENERATED SLOT METHODS =========="));
        assertTrue(source.contains("public void addPrefix(final PComponent<?> child)"));
        assertTrue(source.contains("// ========== GENERATED METHOD PROXIES =========="));
        assertTrue(source.contains("public void focus()"));
    }

    @Test
    void generateWrapperClass_generatesCssPropertyConstants() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new com.ponysdk.core.ui.codegen.model.CssPropertyDef(
                    "--button-background",
                    "Background color of the button",
                    "<color>",
                    "#ffffff"
                ),
                new com.ponysdk.core.ui.codegen.model.CssPropertyDef(
                    "--button-border-radius",
                    "Border radius of the button",
                    "<length>",
                    "4px"
                )
            ),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("private static final String CSS_BUTTON_BACKGROUND = \"--button-background\";"));
        assertTrue(source.contains("private static final String CSS_BUTTON_BORDER_RADIUS = \"--button-border-radius\";"));
    }

    @Test
    void generateWrapperClass_generatesCssPropertySetters() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new com.ponysdk.core.ui.codegen.model.CssPropertyDef(
                    "--button-background",
                    "Background color of the button",
                    "<color>",
                    "#ffffff"
                )
            ),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("// ========== GENERATED CSS PROPERTY SETTERS =========="));
        assertTrue(source.contains("public void setButtonBackground(final String value)"));
        assertTrue(source.contains("setCssProperty(CSS_BUTTON_BACKGROUND, value);"));
        assertTrue(source.contains("// ========== END GENERATED CODE =========="));
    }

    @Test
    void generateWrapperClass_includesCssPropertyDescriptionInJavadoc() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new com.ponysdk.core.ui.codegen.model.CssPropertyDef(
                    "--button-background",
                    "Background color of the button",
                    "<color>",
                    "#ffffff"
                )
            ),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("Sets the '--button-background' CSS custom property"));
        assertTrue(source.contains("Background color of the button"));
        assertTrue(source.contains("Syntax: &lt;color&gt;"));
        assertTrue(source.contains("Default: #ffffff"));
    }

    @Test
    void generateWrapperClass_handlesMultiWordCssPropertyNames() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new com.ponysdk.core.ui.codegen.model.CssPropertyDef(
                    "--button-border-top-left-radius",
                    "Top left border radius",
                    "<length>",
                    "4px"
                )
            ),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("private static final String CSS_BUTTON_BORDER_TOP_LEFT_RADIUS = \"--button-border-top-left-radius\";"));
        assertTrue(source.contains("public void setButtonBorderTopLeftRadius(final String value)"));
    }

    @Test
    void generateWrapperClass_includesManualExtensionPlaceholder() {
        final ComponentDefinition def = createMinimalDef("wa-button");

        final String source = generator.generateWrapperClass(def);

        assertTrue(source.contains("// Manual extensions can be added below this line"));
    }

    @Test
    void generateWrapperClass_combinesAllGeneratedSections() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            List.of(
                new com.ponysdk.core.ui.codegen.model.EventDef("wa-click", "Emitted on click", null, true, false)
            ),
            List.of(
                new SlotDef("prefix", "Prefix slot")
            ),
            List.of(
                new MethodDef("focus", "Focuses the button", Collections.emptyList(), "void", false)
            ),
            List.of(
                new com.ponysdk.core.ui.codegen.model.CssPropertyDef(
                    "--button-background",
                    "Background color",
                    "<color>",
                    "#ffffff"
                )
            ),
            "stable"
        );

        final String source = generator.generateWrapperClass(def);

        // Verify all four sections are present
        assertTrue(source.contains("// ========== GENERATED EVENT HANDLERS =========="));
        assertTrue(source.contains("public void addClickListener(final Consumer<JsonObject> handler)"));
        assertTrue(source.contains("// ========== GENERATED SLOT METHODS =========="));
        assertTrue(source.contains("public void addPrefix(final PComponent<?> child)"));
        assertTrue(source.contains("// ========== GENERATED METHOD PROXIES =========="));
        assertTrue(source.contains("public void focus()"));
        assertTrue(source.contains("// ========== GENERATED CSS PROPERTY SETTERS =========="));
        assertTrue(source.contains("public void setButtonBackground(final String value)"));
        assertTrue(source.contains("// Manual extensions can be added below this line"));
    }

    // ========== Props Record Generation Tests ==========

    @Test
    void generatePropsRecord_throwsExceptionForNullDefinition() {
        assertThrows(IllegalArgumentException.class, () -> generator.generatePropsRecord(null));
    }

    @Test
    void generatePropsRecord_generatesCorrectPackage() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            List.of(
                new PropertyDef("variant", "string", "String", "string", "Button variant", "neutral", false, "public")
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("package com.ponysdk.core.ui.webawesome;"));
    }

    @Test
    void generatePropsRecord_generatesRecordWithProperties() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            List.of(
                new PropertyDef("variant", "string", "String", "string", "Button variant", "neutral", false, "public"),
                new PropertyDef("disabled", "boolean", "boolean", "boolean", "Whether disabled", "false", false, "public")
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("public record ButtonProps("));
        assertTrue(source.contains("String variant"));
        assertTrue(source.contains("boolean disabled"));
    }

    @Test
    void generatePropsRecord_includesJavadocWithDescription() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "Detailed description of the button",
            List.of(
                new PropertyDef("variant", "string", "String", "string", "Button variant", "neutral", false, "public")
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("/**"));
        assertTrue(source.contains("* Props for the Button component"));
        assertTrue(source.contains("* Detailed description of the button"));
        assertTrue(source.contains("@param variant Button variant"));
    }

    @Test
    void generatePropsRecord_generatesDefaultsMethod() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            List.of(
                new PropertyDef("variant", "string", "String", "string", "Button variant", "neutral", false, "public"),
                new PropertyDef("disabled", "boolean", "boolean", "boolean", "Whether disabled", "false", false, "public")
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("public static ButtonProps defaults()"));
        assertTrue(source.contains("return new ButtonProps("));
        assertTrue(source.contains("\"neutral\""));
        assertTrue(source.contains("false"));
    }

    @Test
    void generatePropsRecord_generatesBuilderMethods() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            List.of(
                new PropertyDef("variant", "string", "String", "string", "Button variant", "neutral", false, "public"),
                new PropertyDef("size", "string", "String", "string", "Button size", "medium", false, "public")
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("public ButtonProps withVariant(final String variant)"));
        assertTrue(source.contains("return new ButtonProps("));
        assertTrue(source.contains("variant"));
        assertTrue(source.contains("this.size()"));
        
        assertTrue(source.contains("public ButtonProps withSize(final String size)"));
        assertTrue(source.contains("this.variant()"));
        assertTrue(source.contains("size"));
    }

    @Test
    void generatePropsRecord_handlesOptionalProperties() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            List.of(
                new PropertyDef("variant", "string | undefined", "Optional<String>", "string | undefined", "Button variant", "", false, "public")
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("import java.util.Optional;"));
        assertTrue(source.contains("Optional<String> variant"));
        assertTrue(source.contains("Optional.empty()"));
    }

    @Test
    void generatePropsRecord_handlesListProperties() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-select",
            "Select",
            "A select component",
            "",
            List.of(
                new PropertyDef("options", "string[]", "List<String>", "string[]", "Select options", "", false, "public")
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("import java.util.List;"));
        assertTrue(source.contains("List<String> options"));
        assertTrue(source.contains("List.of()"));
    }

    @Test
    void generatePropsRecord_excludesPrivateProperties() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            List.of(
                new PropertyDef("variant", "string", "String", "string", "Button variant", "neutral", false, "public"),
                new PropertyDef("internalState", "string", "String", "string", "Internal state", "", false, "private")
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("String variant"));
        assertFalse(source.contains("internalState"));
    }

    @Test
    void generatePropsRecord_handlesNumericDefaults() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-rating",
            "Rating",
            "A rating component",
            "",
            List.of(
                new PropertyDef("max", "number", "int", "number", "Maximum rating", "5", false, "public"),
                new PropertyDef("value", "number", "double", "number", "Current value", "0.0", false, "public")
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("int max"));
        assertTrue(source.contains("double value"));
    }

    @Test
    void generatePropsRecord_handlesEmptyProperties() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-divider",
            "Divider",
            "A divider component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("public record DividerProps("));
        assertTrue(source.contains(") {"));
        assertTrue(source.contains("public static DividerProps defaults()"));
    }

    @Test
    void generatePropsRecord_handlesMultiWordPropertyNames() {
        final ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            List.of(
                new PropertyDef("isLoading", "boolean", "boolean", "boolean", "Loading state", "false", false, "public")
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );

        final String source = generator.generatePropsRecord(def);

        assertTrue(source.contains("boolean isLoading"));
        assertTrue(source.contains("public ButtonProps withIsLoading(final boolean isLoading)"));
    }
}
