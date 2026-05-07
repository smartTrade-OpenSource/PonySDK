package com.ponysdk.core.ui.wa.codegen;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify enum imports are added to wrapper classes.
 */
class WrapperEnumImportTest {

    @Test
    void wrapperClassIncludesEnumImports() {
        // Create a component with an enum property
        PropertyDef enumProp = new PropertyDef(
            "variant",
            "'neutral' | 'brand' | 'success'",
            "ButtonVariant",
            "The button variant",
            "'neutral'",
            false,
            true,  // isEnum
            "ButtonVariant",
            "com.ponysdk.core.ui.wa.enums.ButtonVariant"
        );
        
        PropertyDef stringProp = PropertyDef.simple(
            "label",
            "string",
            "String",
            "The button label",
            null,
            false
        );
        
        ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "WaButton",
            "A button component",
            "",
            List.of(enumProp, stringProp),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );
        
        WebAwesomeCodeGenerator generator = new WebAwesomeCodeGenerator(null, null, null);
        String wrapperCode = generator.generateWrapperClass(def);
        
        // Verify enum import is present
        assertTrue(wrapperCode.contains("import com.ponysdk.core.ui.wa.enums.ButtonVariant;"),
            "Wrapper class should import the enum type. Generated code:\n" + wrapperCode);
        
        // Verify setter uses enum type
        assertTrue(wrapperCode.contains("public void setVariant(final ButtonVariant variant)"),
            "Setter should accept enum type. Generated code:\n" + wrapperCode);
    }
    
    @Test
    void wrapperClassWithoutEnumsHasNoEnumImports() {
        PropertyDef stringProp = PropertyDef.simple(
            "label",
            "string",
            "String",
            "The button label",
            null,
            false
        );
        
        ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "WaButton",
            "A button component",
            "",
            List.of(stringProp),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );
        
        WebAwesomeCodeGenerator generator = new WebAwesomeCodeGenerator(null, null, null);
        String wrapperCode = generator.generateWrapperClass(def);
        
        // Verify no enum imports
        assertFalse(wrapperCode.contains("com.ponysdk.core.ui.wa.enums"),
            "Wrapper class without enums should not have enum imports. Generated code:\n" + wrapperCode);
    }
    
    @Test
    void wrapperClassWithMultipleEnumsHasAllImports() {
        PropertyDef variantProp = new PropertyDef(
            "variant",
            "'neutral' | 'brand'",
            "ButtonVariant",
            "The button variant",
            "'neutral'",
            false,
            true,
            "ButtonVariant",
            "com.ponysdk.core.ui.wa.enums.ButtonVariant"
        );
        
        PropertyDef sizeProp = new PropertyDef(
            "size",
            "'small' | 'medium' | 'large'",
            "ButtonSize",
            "The button size",
            "'medium'",
            false,
            true,
            "ButtonSize",
            "com.ponysdk.core.ui.wa.enums.ButtonSize"
        );
        
        ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "WaButton",
            "A button component",
            "",
            List.of(variantProp, sizeProp),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );
        
        WebAwesomeCodeGenerator generator = new WebAwesomeCodeGenerator(null, null, null);
        String wrapperCode = generator.generateWrapperClass(def);
        
        // Verify both enum imports are present
        assertTrue(wrapperCode.contains("import com.ponysdk.core.ui.wa.enums.ButtonVariant;"),
            "Wrapper class should import ButtonVariant");
        assertTrue(wrapperCode.contains("import com.ponysdk.core.ui.wa.enums.ButtonSize;"),
            "Wrapper class should import ButtonSize");
    }
}
