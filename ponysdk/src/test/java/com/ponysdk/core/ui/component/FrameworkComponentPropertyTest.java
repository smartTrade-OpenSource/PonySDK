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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for Framework Adapter Instantiation.
 * <p>
 * Feature: pcomponent, Property 7: Framework Adapter Instantiation
 * </p>
 * <p>
 * **Validates: Requirements 2.5, 6.3**
 * </p>
 * <p>
 * This test validates that for any valid component creation message with a framework type
 * (React, Vue, Svelte, WebComponent), the ComponentRegistry SHALL instantiate an adapter
 * of the corresponding type.
 * </p>
 * <p>
 * Since PComponent requires UIContext for instantiation (which is not available in unit tests),
 * we test the framework type system at the protocol level:
 * <ul>
 *   <li>FrameworkType enum correctly maps to byte values for protocol serialization</li>
 *   <li>Byte values correctly round-trip back to FrameworkType enum values</li>
 *   <li>All four framework types (REACT, VUE, SVELTE, WEB_COMPONENT) are supported</li>
 *   <li>Framework type byte values are unique and non-overlapping</li>
 * </ul>
 * </p>
 */
public class FrameworkComponentPropertyTest {

    // ========== Property 7: Framework Adapter Instantiation Tests ==========

    /**
     * Property 7: Framework Type Byte Value Round-Trip
     * <p>
     * **Validates: Requirements 2.5, 6.3**
     * </p>
     * <p>
     * For any FrameworkType, converting to byte value and back SHALL produce
     * the same FrameworkType (protocol serialization correctness).
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 7: Framework Type Byte Value Round-Trip")
    void frameworkTypeByteValueRoundTrip(
            @ForAll("frameworkType") FrameworkType original
    ) {
        // Convert to byte value (as would be sent over protocol)
        byte byteValue = original.getValue();

        // Convert back from byte value (as would be received on client)
        FrameworkType restored = FrameworkType.fromRawValue(byteValue);

        assertEquals(original, restored,
                "Converting FrameworkType to byte and back should produce the same type");
    }

    /**
     * Property 7: Framework Type Byte Values Are Unique
     * <p>
     * **Validates: Requirements 2.5, 6.3**
     * </p>
     * <p>
     * All FrameworkType enum values SHALL have unique byte values to ensure
     * correct adapter instantiation on the client side.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 7: Framework Type Byte Values Are Unique")
    void frameworkTypeByteValuesAreUnique(
            @ForAll("frameworkTypePair") FrameworkType[] pair
    ) {
        FrameworkType first = pair[0];
        FrameworkType second = pair[1];

        if (first != second) {
            assertNotEquals(first.getValue(), second.getValue(),
                    "Different FrameworkTypes should have different byte values: " + first + " vs " + second);
        }
    }

    /**
     * Property 7: Framework Type Byte Value Is Non-Negative
     * <p>
     * **Validates: Requirements 2.5, 6.3**
     * </p>
     * <p>
     * All FrameworkType byte values SHALL be non-negative to ensure
     * correct protocol encoding.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 7: Framework Type Byte Value Is Non-Negative")
    void frameworkTypeByteValueIsNonNegative(
            @ForAll("frameworkType") FrameworkType frameworkType
    ) {
        byte byteValue = frameworkType.getValue();

        assertTrue(byteValue >= 0,
                "FrameworkType byte value should be non-negative: " + frameworkType + " = " + byteValue);
    }

    /**
     * Property 7: Framework Type Ordinal Matches Byte Value
     * <p>
     * **Validates: Requirements 2.5, 6.3**
     * </p>
     * <p>
     * The FrameworkType byte value SHALL equal its ordinal value,
     * ensuring consistent protocol encoding.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 7: Framework Type Ordinal Matches Byte Value")
    void frameworkTypeOrdinalMatchesByteValue(
            @ForAll("frameworkType") FrameworkType frameworkType
    ) {
        assertEquals((byte) frameworkType.ordinal(), frameworkType.getValue(),
                "FrameworkType byte value should equal its ordinal");
    }

    /**
     * Property 7: All Framework Types Are Covered
     * <p>
     * **Validates: Requirements 2.5, 6.3**
     * </p>
     * <p>
     * The FrameworkType enum SHALL contain exactly the four required framework types:
     * REACT, VUE, SVELTE, and WEB_COMPONENT.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 7: All Framework Types Are Covered")
    void allFrameworkTypesAreCovered(
            @ForAll("frameworkType") FrameworkType frameworkType
    ) {
        Set<FrameworkType> expectedTypes = new HashSet<>(Arrays.asList(
                FrameworkType.REACT,
                FrameworkType.VUE,
                FrameworkType.SVELTE,
                FrameworkType.WEB_COMPONENT
        ));

        assertTrue(expectedTypes.contains(frameworkType),
                "FrameworkType should be one of the expected types: " + frameworkType);

        // Also verify the total count
        assertEquals(4, FrameworkType.values().length,
                "There should be exactly 4 framework types");
    }

    /**
     * Property 7: Framework Type fromRawValue Handles All Valid Values
     * <p>
     * **Validates: Requirements 2.5, 6.3**
     * </p>
     * <p>
     * For any valid byte value (0 to 3), fromRawValue SHALL return a valid FrameworkType.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 7: Framework Type fromRawValue Handles All Valid Values")
    void frameworkTypeFromRawValueHandlesAllValidValues(
            @ForAll("validByteValue") int byteValue
    ) {
        FrameworkType result = FrameworkType.fromRawValue(byteValue);

        assertNotNull(result, "fromRawValue should return a non-null FrameworkType for valid byte value: " + byteValue);
        assertEquals(byteValue, result.getValue(),
                "Returned FrameworkType should have the same byte value");
    }

    /**
     * Property 7: React Framework Type Mapping
     * <p>
     * **Validates: Requirements 2.5, 6.3**
     * </p>
     * <p>
     * PReactComponent specifies REACT as the target framework.
     * The REACT FrameworkType SHALL have byte value 0.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 7: React Framework Type Mapping")
    void reactFrameworkTypeMapping() {
        assertEquals(0, FrameworkType.REACT.getValue(),
                "REACT should have byte value 0");
        assertEquals(FrameworkType.REACT, FrameworkType.fromRawValue(0),
                "Byte value 0 should map to REACT");
    }

    /**
     * Property 7: Vue Framework Type Mapping
     * <p>
     * **Validates: Requirements 2.5, 6.3**
     * </p>
     * <p>
     * PVueComponent specifies VUE as the target framework.
     * The VUE FrameworkType SHALL have byte value 1.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 7: Vue Framework Type Mapping")
    void vueFrameworkTypeMapping() {
        assertEquals(1, FrameworkType.VUE.getValue(),
                "VUE should have byte value 1");
        assertEquals(FrameworkType.VUE, FrameworkType.fromRawValue(1),
                "Byte value 1 should map to VUE");
    }

    /**
     * Property 7: Svelte Framework Type Mapping
     * <p>
     * **Validates: Requirements 2.5, 6.3**
     * </p>
     * <p>
     * PSvelteComponent specifies SVELTE as the target framework.
     * The SVELTE FrameworkType SHALL have byte value 2.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 7: Svelte Framework Type Mapping")
    void svelteFrameworkTypeMapping() {
        assertEquals(2, FrameworkType.SVELTE.getValue(),
                "SVELTE should have byte value 2");
        assertEquals(FrameworkType.SVELTE, FrameworkType.fromRawValue(2),
                "Byte value 2 should map to SVELTE");
    }

    /**
     * Property 7: WebComponent Framework Type Mapping
     * <p>
     * **Validates: Requirements 2.5, 6.3**
     * </p>
     * <p>
     * PWebComponent specifies WEB_COMPONENT as the target framework.
     * The WEB_COMPONENT FrameworkType SHALL have byte value 3.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 7: WebComponent Framework Type Mapping")
    void webComponentFrameworkTypeMapping() {
        assertEquals(3, FrameworkType.WEB_COMPONENT.getValue(),
                "WEB_COMPONENT should have byte value 3");
        assertEquals(FrameworkType.WEB_COMPONENT, FrameworkType.fromRawValue(3),
                "Byte value 3 should map to WEB_COMPONENT");
    }

    /**
     * Property 7: Framework Type Consistency Across Multiple Accesses
     * <p>
     * **Validates: Requirements 2.5, 6.3**
     * </p>
     * <p>
     * Accessing the same FrameworkType multiple times SHALL always return
     * the same byte value (immutability).
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 7: Framework Type Consistency Across Multiple Accesses")
    void frameworkTypeConsistencyAcrossMultipleAccesses(
            @ForAll("frameworkType") FrameworkType frameworkType,
            @ForAll("accessCount") int accessCount
    ) {
        byte firstValue = frameworkType.getValue();

        for (int i = 0; i < accessCount; i++) {
            assertEquals(firstValue, frameworkType.getValue(),
                    "FrameworkType byte value should be consistent across multiple accesses");
        }
    }

    // ========== Arbitraries (Generators) ==========

    /**
     * Generates arbitrary FrameworkType enum values.
     */
    @Provide
    Arbitrary<FrameworkType> frameworkType() {
        return Arbitraries.of(FrameworkType.values());
    }

    /**
     * Generates pairs of FrameworkType values for uniqueness testing.
     */
    @Provide
    Arbitrary<FrameworkType[]> frameworkTypePair() {
        return Combinators.combine(
                Arbitraries.of(FrameworkType.values()),
                Arbitraries.of(FrameworkType.values())
        ).as((a, b) -> new FrameworkType[]{a, b});
    }

    /**
     * Generates valid byte values for FrameworkType (0-3).
     */
    @Provide
    Arbitrary<Integer> validByteValue() {
        return Arbitraries.integers().between(0, 3);
    }

    /**
     * Generates access counts for consistency testing.
     */
    @Provide
    Arbitrary<Integer> accessCount() {
        return Arbitraries.integers().between(1, 10);
    }
}
