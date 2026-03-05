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
import com.ponysdk.core.ui.wa.PropsSerializationTest.AlertProps;
import com.ponysdk.core.ui.wa.PropsSerializationTest.ButtonProps;
import com.ponysdk.core.ui.wa.PropsSerializationTest.InputProps;
import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;

import javax.json.JsonObject;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for Props serialization round-trip.
 * <p>
 * Feature: ui-library-wrapper, Property 1: Props Serialization Round-Trip
 * </p>
 * <p>
 * For any valid UI component props record, serializing to JSON then deserializing
 * back SHALL produce an equivalent object, including enum values.
 * </p>
 * <p>
 * <b>Validates: Requirements 10.3, 10.5</b>
 * </p>
 */
@Tag("Feature: ui-library-wrapper, Property 1: Props Serialization Round-Trip")
public class PropsSerializationPropertyTest {

    // ========== Property Tests ==========

    /**
     * For any ButtonProps, serialize → deserialize produces equivalent object.
     * <p>
     * <b>Validates: Requirements 10.3, 10.5</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 1: ButtonProps serialization round-trip")
    void buttonProps_roundTrip(@ForAll("buttonProps") ButtonProps original) {
        final PropsDiffer<ButtonProps> differ = new PropsDiffer<>();

        final JsonObject json = differ.toJson(original);
        final ButtonProps restored = differ.fromJson(json, ButtonProps.class);

        assertEquals(original, restored,
                "ButtonProps round-trip should produce equivalent object");
    }

    /**
     * For any InputProps, serialize → deserialize produces equivalent object.
     * <p>
     * <b>Validates: Requirements 10.3, 10.5</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 1: InputProps serialization round-trip")
    void inputProps_roundTrip(@ForAll("inputProps") InputProps original) {
        final PropsDiffer<InputProps> differ = new PropsDiffer<>();

        final JsonObject json = differ.toJson(original);
        final InputProps restored = differ.fromJson(json, InputProps.class);

        assertEquals(original, restored,
                "InputProps round-trip should produce equivalent object");
    }

    /**
     * For any AlertProps, serialize → deserialize produces equivalent object.
     * <p>
     * <b>Validates: Requirements 10.3, 10.5</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 1: AlertProps serialization round-trip")
    void alertProps_roundTrip(@ForAll("alertProps") AlertProps original) {
        final PropsDiffer<AlertProps> differ = new PropsDiffer<>();

        final JsonObject json = differ.toJson(original);
        final AlertProps restored = differ.fromJson(json, AlertProps.class);

        assertEquals(original, restored,
                "AlertProps round-trip should produce equivalent object");
    }

    /**
     * Enum values always serialize as their lowercase string values.
     * <p>
     * <b>Validates: Requirements 10.5</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 1: Enum values serialize as lowercase strings")
    void enumValues_serializeAsLowercaseStrings(@ForAll("buttonProps") ButtonProps original) {
        final PropsDiffer<ButtonProps> differ = new PropsDiffer<>();

        final JsonObject json = differ.toJson(original);

        assertEquals(original.variant().getValue(), json.getString("variant"),
                "Variant enum should serialize as its lowercase string value");
        assertEquals(original.size().getValue(), json.getString("size"),
                "Size enum should serialize as its lowercase string value");
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<ButtonProps> buttonProps() {
        return Combinators.combine(
                Arbitraries.of(Variant.values()),
                Arbitraries.of(Size.values()),
                Arbitraries.of(true, false),
                Arbitraries.of(true, false),
                Arbitraries.of("button", "submit", "reset")
        ).as(ButtonProps::new);
    }

    @Provide
    Arbitrary<InputProps> inputProps() {
        final Arbitrary<Optional<Integer>> optionalInt = Arbitraries.integers()
                .between(0, 500)
                .optional();

        final Arbitrary<Optional<Double>> optionalDouble = Arbitraries.doubles()
                .between(0.0, 10000.0)
                .optional();

        final Arbitrary<Optional<String>> optionalPattern = Arbitraries.of(".*@.*", "\\d+", "[a-z]+", "^[A-Z].*")
                .optional();

        // jqwik Combinators.combine supports max 8 args, so build in two stages
        return Combinators.combine(
                Arbitraries.of("text", "email", "password", "number", "search", "url", "tel"),
                Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(50),
                Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(30),
                Arbitraries.of(true, false),
                Arbitraries.of(true, false),
                Arbitraries.of(true, false),
                optionalInt,
                optionalInt
        ).flatAs((type, value, label, disabled, readonly, required, minlength, maxlength) ->
                Combinators.combine(
                        optionalDouble,
                        optionalDouble,
                        optionalPattern,
                        Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(100),
                        Arbitraries.of(Size.values())
                ).as((min, max, pattern, customValidity, size) ->
                        new InputProps(type, value, label, disabled, readonly, required,
                                minlength, maxlength, min, max, pattern, customValidity, size)
                )
        );
    }

    @Provide
    Arbitrary<AlertProps> alertProps() {
        final Arbitrary<Optional<String>> optionalDuration = Arbitraries.of("1000", "3000", "5000", "10000")
                .optional();

        return Combinators.combine(
                Arbitraries.of(true, false),
                Arbitraries.of(true, false),
                Arbitraries.of(Variant.values()),
                optionalDuration
        ).as(AlertProps::new);
    }
}
