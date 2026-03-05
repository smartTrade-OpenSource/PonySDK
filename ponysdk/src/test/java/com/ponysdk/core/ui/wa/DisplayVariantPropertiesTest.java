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
 * Unit tests verifying that display component wrappers (PAlert, PBadge, PTag)
 * include a {@code variant} field accepting Variant enum values.
 *
 * <p>Validates: Requirements 2.1, 2.3</p>
 */
public class DisplayVariantPropertiesTest {

    // ========== Code Generator Enrichment Tests ==========

    @Test
    void enrichment_addsVariantToDisplayComponent() {
        final ComponentDefinition bare = new ComponentDefinition(
            "wa-alert", "WaAlert", "Alert component", "",
            List.of(new PropertyDef("open", "boolean", "boolean", "Whether the alert is open", "false", true)),
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), "stable"
        );

        final ComponentDefinition enriched = WebAwesomeCodeGenerator.enrichDisplayComponentProperties(bare);

        final Set<String> propNames = enriched.properties().stream()
            .map(PropertyDef::name)
            .collect(Collectors.toSet());

        assertTrue(propNames.contains("variant"),
            "Display component should have 'variant' property after enrichment");
        assertTrue(propNames.contains("open"),
            "Original 'open' property should be preserved");
    }

    @Test
    void enrichment_preservesExistingVariantProp() {
        final List<PropertyDef> existing = new ArrayList<>();
        existing.add(new PropertyDef("open", "boolean", "boolean", "Whether open", "false", true));
        existing.add(new PropertyDef("variant", "string", "String", "Visual variant", "\"primary\"", false));

        final ComponentDefinition bare = new ComponentDefinition(
            "wa-badge", "WaBadge", "Badge component", "",
            existing, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), "stable"
        );

        final ComponentDefinition enriched = WebAwesomeCodeGenerator.enrichDisplayComponentProperties(bare);

        final long variantCount = enriched.properties().stream()
            .filter(p -> "variant".equals(p.name()))
            .count();
        assertEquals(1, variantCount, "Existing 'variant' property should not be duplicated");
    }

    @Test
    void enrichment_doesNotModifyNonDisplayComponents() {
        final ComponentDefinition button = new ComponentDefinition(
            "wa-button", "WaButton", "Button component", "",
            List.of(new PropertyDef("disabled", "boolean", "boolean", "Whether disabled", "false", true)),
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), "stable"
        );

        final ComponentDefinition result = WebAwesomeCodeGenerator.enrichDisplayComponentProperties(button);

        assertSame(button, result, "Non-display components should not be enriched");
        assertEquals(1, result.properties().size());
    }

    @Test
    void enrichment_worksForDisplayComponents() {
        // Test with a few known display components
        final String[] displayComponents = {"wa-alert", "wa-badge", "wa-tag"};
        
        for (final String tagName : displayComponents) {
            final ComponentDefinition bare = new ComponentDefinition(
                tagName, "Wa" + tagName.substring(3), tagName + " component", "",
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), "stable"
            );

            final ComponentDefinition enriched = WebAwesomeCodeGenerator.enrichDisplayComponentProperties(bare);

            final Set<String> propNames = enriched.properties().stream()
                .map(PropertyDef::name)
                .collect(Collectors.toSet());

            assertTrue(propNames.contains("variant"),
                tagName + " should have 'variant' property after enrichment");
        }
    }

    // ========== Generated Props Record Validation Tests ==========

    @Test
    void generatedPropsRecord_containsVariantField() {
        final ComponentDefinition alertDef = new ComponentDefinition(
            "wa-alert", "WaAlert", "Alert component", "",
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), "stable"
        );

        final ComponentDefinition enriched = WebAwesomeCodeGenerator.enrichDisplayComponentProperties(alertDef);
        final WebAwesomeCodeGenerator generator = new WebAwesomeCodeGenerator(
            java.nio.file.Path.of("dummy"), java.nio.file.Path.of("dummy"), java.nio.file.Path.of("dummy")
        );
        final String propsSource = generator.generatePropsRecord(enriched);

        assertTrue(propsSource.contains("String variant"),
            "Generated record should contain 'variant' field of type String");
    }

    @Test
    void generatedWrapperClass_containsVariantSetter() {
        final ComponentDefinition tagDef = new ComponentDefinition(
            "wa-tag", "WaTag", "Tag component", "",
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), "stable"
        );

        final ComponentDefinition enriched = WebAwesomeCodeGenerator.enrichDisplayComponentProperties(tagDef);
        final WebAwesomeCodeGenerator generator = new WebAwesomeCodeGenerator(
            java.nio.file.Path.of("dummy"), java.nio.file.Path.of("dummy"), java.nio.file.Path.of("dummy")
        );
        final String wrapperSource = generator.generateWrapperClass(enriched);

        assertTrue(wrapperSource.contains("setVariant"),
            "Wrapper should have setVariant setter");
    }

    // ========== Sample AlertProps Serialization with Variant ==========

    @Test
    void alertProps_variantSerializesAsLowercaseString() {
        final PropsDiffer<PropsSerializationTest.AlertProps> differ = new PropsDiffer<>();

        for (final Variant variant : Variant.values()) {
            final PropsSerializationTest.AlertProps props =
                new PropsSerializationTest.AlertProps(true, true, variant, Optional.of("3000"));
            final JsonObject json = differ.toJson(props);

            assertEquals(variant.getValue(), json.getString("variant"),
                "Variant." + variant.name() + " should serialize as \"" + variant.getValue() + "\"");
        }
    }

    @Test
    void alertProps_variantRoundTrip() {
        final PropsDiffer<PropsSerializationTest.AlertProps> differ = new PropsDiffer<>();

        for (final Variant variant : Variant.values()) {
            final PropsSerializationTest.AlertProps original =
                new PropsSerializationTest.AlertProps(true, false, variant, Optional.empty());
            final JsonObject json = differ.toJson(original);
            final PropsSerializationTest.AlertProps restored =
                differ.fromJson(json, PropsSerializationTest.AlertProps.class);

            assertEquals(original, restored,
                "Round-trip for Variant." + variant.name() + " should produce equivalent record");
        }
    }
}
