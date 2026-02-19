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

package com.ponysdk.core.ui.component;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import javax.json.JsonArray;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for PropsDiffer.
 * <p>
 * Feature: pcomponent, Property 2: Props Diff Round-Trip
 * </p>
 */
public class PropsDifferPropertyTest {

    // ========== Test Record Types ==========

    /**
     * Simple test record with primitive types.
     */
    public record SimpleProps(
            String name,
            int count,
            double value,
            boolean enabled
    ) {}

    /**
     * Nested test record for testing hierarchical props.
     */
    public record NestedProps(
            String title,
            SimpleProps inner
    ) {}

    /**
     * Test record with Optional field.
     */
    public record OptionalProps(
            String required,
            Optional<String> optional,
            int number
    ) {}

    /**
     * Test record with List field.
     */
    public record ListProps(
            String name,
            List<String> items
    ) {}

    /**
     * Complex test record combining multiple field types.
     */
    public record ComplexProps(
            String title,
            int count,
            double ratio,
            boolean active,
            Optional<String> description,
            List<String> tags,
            SimpleProps nested
    ) {}

    // ========== Property Tests ==========

    // ========== Property 1: Props Serialization Round-Trip Tests ==========

    /**
     * Property 1: Props Serialization Round-Trip - SimpleProps
     * <p>
     * **Validates: Requirements 3.5**
     * </p>
     * <p>
     * For any valid props object (Java Record), serializing to JSON then deserializing
     * back to a props object SHALL produce an equivalent object.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 1: Props Serialization Round-Trip - SimpleProps")
    void propsSerializationRoundTrip_SimpleProps(
            @ForAll("simpleProps") SimpleProps original
    ) {
        PropsDiffer<SimpleProps> differ = new PropsDiffer<>();

        // Serialize to JSON
        javax.json.JsonObject json = differ.toJson(original);

        // Deserialize back to props
        SimpleProps result = differ.fromJson(json, SimpleProps.class);

        assertEquals(original, result,
                "Serializing to JSON then deserializing should produce equivalent object");
    }

    /**
     * Property 1: Props Serialization Round-Trip - NestedProps
     * <p>
     * **Validates: Requirements 3.5**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 1: Props Serialization Round-Trip - NestedProps")
    void propsSerializationRoundTrip_NestedProps(
            @ForAll("nestedProps") NestedProps original
    ) {
        PropsDiffer<NestedProps> differ = new PropsDiffer<>();

        // Serialize to JSON
        javax.json.JsonObject json = differ.toJson(original);

        // Deserialize back to props
        NestedProps result = differ.fromJson(json, NestedProps.class);

        assertEquals(original, result,
                "Serializing nested props to JSON then deserializing should produce equivalent object");
    }

    /**
     * Property 1: Props Serialization Round-Trip - OptionalProps
     * <p>
     * **Validates: Requirements 3.5**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 1: Props Serialization Round-Trip - OptionalProps")
    void propsSerializationRoundTrip_OptionalProps(
            @ForAll("optionalProps") OptionalProps original
    ) {
        PropsDiffer<OptionalProps> differ = new PropsDiffer<>();

        // Serialize to JSON
        javax.json.JsonObject json = differ.toJson(original);

        // Deserialize back to props
        OptionalProps result = differ.fromJson(json, OptionalProps.class);

        assertEquals(original, result,
                "Serializing optional props to JSON then deserializing should produce equivalent object");
    }

    /**
     * Property 1: Props Serialization Round-Trip - ListProps
     * <p>
     * **Validates: Requirements 3.5**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 1: Props Serialization Round-Trip - ListProps")
    void propsSerializationRoundTrip_ListProps(
            @ForAll("listProps") ListProps original
    ) {
        PropsDiffer<ListProps> differ = new PropsDiffer<>();

        // Serialize to JSON
        javax.json.JsonObject json = differ.toJson(original);

        // Deserialize back to props
        ListProps result = differ.fromJson(json, ListProps.class);

        assertEquals(original, result,
                "Serializing list props to JSON then deserializing should produce equivalent object");
    }

    /**
     * Property 1: Props Serialization Round-Trip - ComplexProps
     * <p>
     * **Validates: Requirements 3.5**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 1: Props Serialization Round-Trip - ComplexProps")
    void propsSerializationRoundTrip_ComplexProps(
            @ForAll("complexProps") ComplexProps original
    ) {
        PropsDiffer<ComplexProps> differ = new PropsDiffer<>();

        // Serialize to JSON
        javax.json.JsonObject json = differ.toJson(original);

        // Deserialize back to props
        ComplexProps result = differ.fromJson(json, ComplexProps.class);

        assertEquals(original, result,
                "Serializing complex props to JSON then deserializing should produce equivalent object");
    }

    // ========== Property 2: Props Diff Round-Trip Tests ==========

    /**
     * Property 2: Props Diff Round-Trip
     * <p>
     * **Validates: Requirements 1.3, 1.4, 3.1, 6.4**
     * </p>
     * <p>
     * For any two valid props objects (previous and current), computing the JSON Patch diff
     * from previous to current, then applying that patch to previous, SHALL produce an object
     * equivalent to current.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 2: Props Diff Round-Trip - SimpleProps")
    void propsDiffRoundTrip_SimpleProps(
            @ForAll("simpleProps") SimpleProps previous,
            @ForAll("simpleProps") SimpleProps current
    ) {
        PropsDiffer<SimpleProps> differ = new PropsDiffer<>();

        Optional<JsonArray> patchOpt = differ.computeDiff(previous, current);

        if (patchOpt.isPresent()) {
            // Apply patch to previous
            SimpleProps result = differ.applyPatch(previous, patchOpt.get(), SimpleProps.class);
            assertEquals(current, result,
                    "Applying diff patch to previous should produce current");
        } else {
            // No patch means either previous == current or previous is null
            // Since we're providing non-null previous, it means they're equal
            assertEquals(previous, current,
                    "Empty patch should mean previous equals current");
        }
    }

    /**
     * Property 2: Props Diff Round-Trip with nested records.
     * <p>
     * **Validates: Requirements 1.3, 1.4, 3.1, 6.4**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 2: Props Diff Round-Trip - NestedProps")
    void propsDiffRoundTrip_NestedProps(
            @ForAll("nestedProps") NestedProps previous,
            @ForAll("nestedProps") NestedProps current
    ) {
        PropsDiffer<NestedProps> differ = new PropsDiffer<>();

        Optional<JsonArray> patchOpt = differ.computeDiff(previous, current);

        if (patchOpt.isPresent()) {
            NestedProps result = differ.applyPatch(previous, patchOpt.get(), NestedProps.class);
            assertEquals(current, result,
                    "Applying diff patch to previous should produce current");
        } else {
            assertEquals(previous, current,
                    "Empty patch should mean previous equals current");
        }
    }

    /**
     * Property 2: Props Diff Round-Trip with Optional fields.
     * <p>
     * **Validates: Requirements 1.3, 1.4, 3.1, 6.4**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 2: Props Diff Round-Trip - OptionalProps")
    void propsDiffRoundTrip_OptionalProps(
            @ForAll("optionalProps") OptionalProps previous,
            @ForAll("optionalProps") OptionalProps current
    ) {
        PropsDiffer<OptionalProps> differ = new PropsDiffer<>();

        Optional<JsonArray> patchOpt = differ.computeDiff(previous, current);

        if (patchOpt.isPresent()) {
            OptionalProps result = differ.applyPatch(previous, patchOpt.get(), OptionalProps.class);
            assertEquals(current, result,
                    "Applying diff patch to previous should produce current");
        } else {
            assertEquals(previous, current,
                    "Empty patch should mean previous equals current");
        }
    }

    /**
     * Property 2: Props Diff Round-Trip with List fields.
     * <p>
     * **Validates: Requirements 1.3, 1.4, 3.1, 6.4**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 2: Props Diff Round-Trip - ListProps")
    void propsDiffRoundTrip_ListProps(
            @ForAll("listProps") ListProps previous,
            @ForAll("listProps") ListProps current
    ) {
        PropsDiffer<ListProps> differ = new PropsDiffer<>();

        Optional<JsonArray> patchOpt = differ.computeDiff(previous, current);

        if (patchOpt.isPresent()) {
            ListProps result = differ.applyPatch(previous, patchOpt.get(), ListProps.class);
            assertEquals(current, result,
                    "Applying diff patch to previous should produce current");
        } else {
            assertEquals(previous, current,
                    "Empty patch should mean previous equals current");
        }
    }

    /**
     * Property 2: Props Diff Round-Trip with complex props combining all field types.
     * <p>
     * **Validates: Requirements 1.3, 1.4, 3.1, 6.4**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 2: Props Diff Round-Trip - ComplexProps")
    void propsDiffRoundTrip_ComplexProps(
            @ForAll("complexProps") ComplexProps previous,
            @ForAll("complexProps") ComplexProps current
    ) {
        PropsDiffer<ComplexProps> differ = new PropsDiffer<>();

        Optional<JsonArray> patchOpt = differ.computeDiff(previous, current);

        if (patchOpt.isPresent()) {
            ComplexProps result = differ.applyPatch(previous, patchOpt.get(), ComplexProps.class);
            assertEquals(current, result,
                    "Applying diff patch to previous should produce current");
        } else {
            assertEquals(previous, current,
                    "Empty patch should mean previous equals current");
        }
    }

    // ========== Property 3: No-Change Detection Tests ==========

    /**
     * Property 3: No-Change Detection - SimpleProps
     * <p>
     * **Validates: Requirements 3.2**
     * </p>
     * <p>
     * For any props object, when computeDiff is called with an identical props object
     * for both previous and current, THE PropsDiffer SHALL return Optional.empty()
     * (no patch generated), indicating no update message should be sent.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 3: No-Change Detection - SimpleProps")
    void noChangeDetection_SimpleProps(
            @ForAll("simpleProps") SimpleProps props
    ) {
        PropsDiffer<SimpleProps> differ = new PropsDiffer<>();

        // Call computeDiff with the same object for both previous and current
        Optional<JsonArray> patchOpt = differ.computeDiff(props, props);

        assertTrue(patchOpt.isEmpty(),
                "When props are identical, computeDiff should return empty (no update needed)");
    }

    /**
     * Property 3: No-Change Detection - NestedProps
     * <p>
     * **Validates: Requirements 3.2**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 3: No-Change Detection - NestedProps")
    void noChangeDetection_NestedProps(
            @ForAll("nestedProps") NestedProps props
    ) {
        PropsDiffer<NestedProps> differ = new PropsDiffer<>();

        Optional<JsonArray> patchOpt = differ.computeDiff(props, props);

        assertTrue(patchOpt.isEmpty(),
                "When nested props are identical, computeDiff should return empty (no update needed)");
    }

    /**
     * Property 3: No-Change Detection - OptionalProps
     * <p>
     * **Validates: Requirements 3.2**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 3: No-Change Detection - OptionalProps")
    void noChangeDetection_OptionalProps(
            @ForAll("optionalProps") OptionalProps props
    ) {
        PropsDiffer<OptionalProps> differ = new PropsDiffer<>();

        Optional<JsonArray> patchOpt = differ.computeDiff(props, props);

        assertTrue(patchOpt.isEmpty(),
                "When optional props are identical, computeDiff should return empty (no update needed)");
    }

    /**
     * Property 3: No-Change Detection - ListProps
     * <p>
     * **Validates: Requirements 3.2**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 3: No-Change Detection - ListProps")
    void noChangeDetection_ListProps(
            @ForAll("listProps") ListProps props
    ) {
        PropsDiffer<ListProps> differ = new PropsDiffer<>();

        Optional<JsonArray> patchOpt = differ.computeDiff(props, props);

        assertTrue(patchOpt.isEmpty(),
                "When list props are identical, computeDiff should return empty (no update needed)");
    }

    /**
     * Property 3: No-Change Detection - ComplexProps
     * <p>
     * **Validates: Requirements 3.2**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 3: No-Change Detection - ComplexProps")
    void noChangeDetection_ComplexProps(
            @ForAll("complexProps") ComplexProps props
    ) {
        PropsDiffer<ComplexProps> differ = new PropsDiffer<>();

        Optional<JsonArray> patchOpt = differ.computeDiff(props, props);

        assertTrue(patchOpt.isEmpty(),
                "When complex props are identical, computeDiff should return empty (no update needed)");
    }

    /**
     * Property 3: No-Change Detection - Equivalent but different instances
     * <p>
     * **Validates: Requirements 3.2**
     * </p>
     * <p>
     * This test verifies that no-change detection works even when comparing
     * two different object instances that have equal values.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 3: No-Change Detection - Equivalent instances")
    void noChangeDetection_EquivalentInstances(
            @ForAll("simpleProps") SimpleProps props
    ) {
        PropsDiffer<SimpleProps> differ = new PropsDiffer<>();

        // Create a new instance with the same values
        SimpleProps copy = new SimpleProps(props.name(), props.count(), props.value(), props.enabled());

        // Verify they are equal but not the same instance
        assertEquals(props, copy, "Copy should be equal to original");

        Optional<JsonArray> patchOpt = differ.computeDiff(props, copy);

        assertTrue(patchOpt.isEmpty(),
                "When props are equivalent (but different instances), computeDiff should return empty");
    }

    /**
     * Property 3: No-Change Detection - Binary diff mode
     * <p>
     * **Validates: Requirements 3.2**
     * </p>
     * <p>
     * This test verifies that no-change detection also works for binary diff mode,
     * returning an empty byte array when props are identical.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 3: No-Change Detection - Binary diff")
    void noChangeDetection_BinaryDiff(
            @ForAll("simpleProps") SimpleProps props
    ) {
        PropsDiffer<SimpleProps> differ = new PropsDiffer<>();

        byte[] binaryDiff = differ.computeBinaryDiff(props, props);

        assertEquals(0, binaryDiff.length,
                "When props are identical, computeBinaryDiff should return empty byte array");
    }

    // ========== Arbitraries (Generators) ==========

    @Provide
    Arbitrary<SimpleProps> simpleProps() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(50),
                Arbitraries.integers().between(-1000, 1000),
                Arbitraries.doubles().between(-1000.0, 1000.0)
                        .filter(d -> !Double.isNaN(d) && !Double.isInfinite(d)),
                Arbitraries.of(true, false)
        ).as(SimpleProps::new);
    }

    @Provide
    Arbitrary<NestedProps> nestedProps() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(50),
                simpleProps()
        ).as(NestedProps::new);
    }

    @Provide
    Arbitrary<OptionalProps> optionalProps() {
        Arbitrary<Optional<String>> optionalString = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(30)
                .optional();

        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
                optionalString,
                Arbitraries.integers().between(-100, 100)
        ).as(OptionalProps::new);
    }

    @Provide
    Arbitrary<ListProps> listProps() {
        Arbitrary<List<String>> stringList = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20)
                .list()
                .ofMinSize(0)
                .ofMaxSize(10);

        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
                stringList
        ).as(ListProps::new);
    }

    @Provide
    Arbitrary<ComplexProps> complexProps() {
        Arbitrary<Optional<String>> optionalDesc = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(30)
                .optional();

        Arbitrary<List<String>> tagList = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(15)
                .list()
                .ofMinSize(0)
                .ofMaxSize(5);

        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(50),
                Arbitraries.integers().between(-100, 100),
                Arbitraries.doubles().between(-100.0, 100.0)
                        .filter(d -> !Double.isNaN(d) && !Double.isInfinite(d)),
                Arbitraries.of(true, false),
                optionalDesc,
                tagList,
                simpleProps()
        ).as(ComplexProps::new);
    }
}
