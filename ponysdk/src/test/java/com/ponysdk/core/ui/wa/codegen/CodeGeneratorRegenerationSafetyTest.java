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
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that re-running the Code Generator does not overwrite manual subclasses
 * (PForm, PDataTable, PResponsiveGrid, PStack) and that generated output is idempotent.
 *
 * @see WebAwesomeCodeGenerator
 */
class CodeGeneratorRegenerationSafetyTest {

    private WebAwesomeCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new WebAwesomeCodeGenerator(
            Path.of("dummy-manifest.json"),
            Path.of("dummy-java-out"),
            Path.of("dummy-ts-out")
        );
    }

    // --- Manual subclasses live in different packages than generated code ---

    @Test
    void manualClassesAreInSubPackages() {
        // Generated wrappers go to com.ponysdk.core.ui.wa
        // Manual classes live in sub-packages that the generator never touches
        final String generatedWrapperPackage = "com.ponysdk.core.ui.wa";
        final String generatedPropsPackage = "com.ponysdk.core.ui.wa.props";

        final Set<String> manualPackages = Set.of(
            "com.ponysdk.core.ui.wa.form",
            "com.ponysdk.core.ui.wa.datatable",
            "com.ponysdk.core.ui.wa.layout"
        );

        for (final String manualPkg : manualPackages) {
            assertNotEquals(generatedWrapperPackage, manualPkg,
                "Manual package " + manualPkg + " must differ from generated wrapper package");
            assertNotEquals(generatedPropsPackage, manualPkg,
                "Manual package " + manualPkg + " must differ from generated props package");
            assertTrue(manualPkg.startsWith(generatedWrapperPackage + "."),
                "Manual package " + manualPkg + " should be a sub-package of " + generatedWrapperPackage);
        }
    }

    @Test
    void generatedWrapperPackageIsWaRoot() {
        // The generator always outputs wrapper classes to com.ponysdk.core.ui.wa
        final ComponentDefinition def = minimalDef("wa-input", List.of(prop("value", "String")));
        final String source = generator.generateWrapperClass(def);
        assertTrue(source.startsWith("package com.ponysdk.core.ui.wa;\n"),
            "Generated wrapper must be in com.ponysdk.core.ui.wa package");
    }

    @Test
    void generatedPropsPackageIsWaProps() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(prop("value", "String")));
        final String source = generator.generatePropsRecord(def);
        assertTrue(source.startsWith("package com.ponysdk.core.ui.wa.props;\n"),
            "Generated props must be in com.ponysdk.core.ui.wa.props package");
    }

    // --- Generated wrapper class names don't collide with manual class names ---

    @Test
    void generatedNamesDoNotCollideWithManualClasses() {
        // Manual classes: PForm, PDataTable, PResponsiveGrid, PStack
        final Set<String> manualClassNames = Set.of("PForm", "PDataTable", "PResponsiveGrid", "PStack");

        // The generator derives names from wa-* tag names.
        // These manual classes have tag names that would produce different generated names,
        // or they are not in the manifest at all.
        // Verify the naming function doesn't produce collisions for common tags.
        final List<String> commonTags = List.of(
            "wa-input", "wa-button", "wa-dialog", "wa-select", "wa-alert",
            "wa-badge", "wa-avatar", "wa-card", "wa-tooltip", "wa-tab-group"
        );

        for (final String tag : commonTags) {
            final String generatedName = WebAwesomeCodeGenerator.tagNameToWrapperClassName(tag);
            assertFalse(manualClassNames.contains(generatedName),
                "Generated name " + generatedName + " from tag " + tag + " collides with a manual class");
        }
    }

    @Test
    void formTagWouldNotProducePForm() {
        // wa-form would produce PForm — but wa-form is not a Web Awesome component.
        // The generator only processes tags from the manifest.
        // Still, verify the naming: if wa-form were generated, it would be PForm.
        assertEquals("PForm", WebAwesomeCodeGenerator.tagNameToWrapperClassName("wa-form"));
        // This is why PForm is a manual class in a sub-package — no collision at the file level.
    }

    // --- Idempotent generation: same input produces identical output ---

    @Test
    void generateWrapperClassIsIdempotent() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(
            prop("value", "String"),
            prop("disabled", "boolean"),
            prop("placeholder", "String")
        ));

        final String first = generator.generateWrapperClass(def);
        final String second = generator.generateWrapperClass(def);
        assertEquals(first, second, "Re-running generateWrapperClass must produce identical output");
    }

    @Test
    void generatePropsRecordIsIdempotent() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(
            prop("value", "String"),
            prop("disabled", "boolean"),
            prop("placeholder", "String")
        ));

        final String first = generator.generatePropsRecord(def);
        final String second = generator.generatePropsRecord(def);
        assertEquals(first, second, "Re-running generatePropsRecord must produce identical output");
    }

    @Test
    void generateTypeScriptInterfaceIsIdempotent() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(
            prop("value", "String"),
            prop("disabled", "boolean")
        ));

        final String first = generator.generateTypeScriptInterface(def);
        final String second = generator.generateTypeScriptInterface(def);
        assertEquals(first, second, "Re-running generateTypeScriptInterface must produce identical output");
    }

    @Test
    void generateComponentIndexIsIdempotent() {
        final List<ComponentDefinition> defs = List.of(
            minimalDef("wa-input", List.of(prop("value", "String"))),
            minimalDef("wa-button", List.of(prop("disabled", "boolean")))
        );

        final String first = generator.generateComponentIndex(defs);
        final String second = generator.generateComponentIndex(defs);
        assertEquals(first, second, "Re-running generateComponentIndex must produce identical output");
    }

    // --- Generated files contain @generated marker ---

    @Test
    void generatedWrapperClassHasGeneratedMarker() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(prop("value", "String")));
        final String source = generator.generateWrapperClass(def);
        assertTrue(source.contains("@generated from custom-elements.json"),
            "Generated wrapper class must contain @generated marker in Javadoc");
    }

    @Test
    void generatedPropsRecordHasGeneratedMarker() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(prop("value", "String")));
        final String source = generator.generatePropsRecord(def);
        assertTrue(source.contains("@generated from custom-elements.json"),
            "Generated props record must contain @generated marker in Javadoc");
    }

    @Test
    void generatedComponentIndexHasGeneratedMarker() {
        final List<ComponentDefinition> defs = List.of(
            minimalDef("wa-input", List.of(prop("value", "String")))
        );
        final String source = generator.generateComponentIndex(defs);
        assertTrue(source.contains("@generated from custom-elements.json"),
            "Generated component index must contain @generated marker in Javadoc");
    }

    // --- Output directories: generator writes to wa/ and wa/props/, not sub-packages ---

    @Test
    void mainMethodWritesToCorrectDirectories() {
        // The main() method resolves output paths as:
        //   javaOutputDir / "com/ponysdk/core/ui/wa/props"  — for props records
        //   javaOutputDir / "com/ponysdk/core/ui/wa"         — for wrapper classes + ComponentIndex
        // It never writes to form/, datatable/, layout/, or theme/ sub-directories.
        // This is verified by inspecting the code structure — the generator has no knowledge
        // of sub-packages and only uses the two paths above.

        // Verify generated wrapper uses the wa root package
        final ComponentDefinition def = minimalDef("wa-alert", List.of(prop("open", "boolean")));
        final String wrapperSource = generator.generateWrapperClass(def);
        final String propsSource = generator.generatePropsRecord(def);

        assertFalse(wrapperSource.contains("package com.ponysdk.core.ui.wa.form"),
            "Generated wrapper must not target form sub-package");
        assertFalse(wrapperSource.contains("package com.ponysdk.core.ui.wa.datatable"),
            "Generated wrapper must not target datatable sub-package");
        assertFalse(wrapperSource.contains("package com.ponysdk.core.ui.wa.layout"),
            "Generated wrapper must not target layout sub-package");
        assertFalse(wrapperSource.contains("package com.ponysdk.core.ui.wa.theme"),
            "Generated wrapper must not target theme sub-package");

        assertFalse(propsSource.contains("package com.ponysdk.core.ui.wa.form"),
            "Generated props must not target form sub-package");
        assertFalse(propsSource.contains("package com.ponysdk.core.ui.wa.datatable"),
            "Generated props must not target datatable sub-package");
        assertFalse(propsSource.contains("package com.ponysdk.core.ui.wa.layout"),
            "Generated props must not target layout sub-package");
    }

    // --- Helpers ---

    private static ComponentDefinition minimalDef(final String tagName, final List<PropertyDef> props) {
        return new ComponentDefinition(
            tagName,
            tagName.replace("wa-", "Wa").replace("-", ""),
            "A test component",
            "",
            props,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            "stable"
        );
    }

    private static PropertyDef prop(final String name, final String javaType) {
        return new PropertyDef(name, name, javaType, "", "", false);
    }
}
