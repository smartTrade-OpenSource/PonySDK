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
import org.junit.jupiter.api.Test;

import javax.json.JsonObject;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying that generated Props Records serialize/deserialize
 * correctly via PComponent's existing JSON mechanism (PropsDiffer).
 * <p>
 * Since the Code Generator produces source code as strings (not compiled classes),
 * we define sample Props Records here to test serialization behavior.
 * </p>
 *
 * <p>Validates: Requirements 10.1, 10.2, 10.3, 10.5</p>
 */
public class PropsSerializationTest {

    // ========== Sample Props Records ==========

    /**
     * Sample ButtonProps record with enum fields (Variant, Size).
     */
    public record ButtonProps(
        Variant variant,
        Size size,
        boolean disabled,
        boolean loading,
        String type
    ) {
        public static ButtonProps defaults() {
            return new ButtonProps(Variant.PRIMARY, Size.MEDIUM, false, false, "button");
        }
    }

    /**
     * Sample InputProps record with Optional fields and full validation properties.
     */
    public record InputProps(
        String type,
        String value,
        String label,
        boolean disabled,
        boolean readonly,
        boolean required,
        Optional<Integer> minlength,
        Optional<Integer> maxlength,
        Optional<Double> min,
        Optional<Double> max,
        Optional<String> pattern,
        String customValidity,
        Size size
    ) {
        public static InputProps defaults() {
            return new InputProps("text", "", "", false, false, false,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), "", Size.MEDIUM);
        }
    }

    /**
     * Sample AlertProps record with enum variant and Optional duration.
     */
    public record AlertProps(
        boolean open,
        boolean closable,
        Variant variant,
        Optional<String> duration
    ) {
        public static AlertProps defaults() {
            return new AlertProps(false, false, Variant.PRIMARY, Optional.empty());
        }
    }

    // ========== Tests ==========

    @Test
    void buttonProps_enumsSerializeAsLowercaseStrings() {
        final PropsDiffer<ButtonProps> differ = new PropsDiffer<>();
        final ButtonProps props = new ButtonProps(Variant.DANGER, Size.LARGE, true, false, "submit");

        final JsonObject json = differ.toJson(props);

        assertEquals("danger", json.getString("variant"), "Variant enum should serialize as lowercase string");
        assertEquals("large", json.getString("size"), "Size enum should serialize as lowercase string");
        assertTrue(json.getBoolean("disabled"));
        assertFalse(json.getBoolean("loading"));
        assertEquals("submit", json.getString("type"));
    }

    @Test
    void buttonProps_allVariantsSerializeCorrectly() {
        final PropsDiffer<ButtonProps> differ = new PropsDiffer<>();

        for (final Variant variant : Variant.values()) {
            final ButtonProps props = new ButtonProps(variant, Size.MEDIUM, false, false, "button");
            final JsonObject json = differ.toJson(props);
            assertEquals(variant.getValue(), json.getString("variant"),
                "Variant." + variant.name() + " should serialize as \"" + variant.getValue() + "\"");
        }
    }

    @Test
    void buttonProps_allSizesSerializeCorrectly() {
        final PropsDiffer<ButtonProps> differ = new PropsDiffer<>();

        for (final Size size : Size.values()) {
            final ButtonProps props = new ButtonProps(Variant.PRIMARY, size, false, false, "button");
            final JsonObject json = differ.toJson(props);
            assertEquals(size.getValue(), json.getString("size"),
                "Size." + size.name() + " should serialize as \"" + size.getValue() + "\"");
        }
    }

    @Test
    void buttonProps_roundTrip() {
        final PropsDiffer<ButtonProps> differ = new PropsDiffer<>();
        final ButtonProps original = new ButtonProps(Variant.SUCCESS, Size.SMALL, true, true, "submit");

        final JsonObject json = differ.toJson(original);
        final ButtonProps restored = differ.fromJson(json, ButtonProps.class);

        assertEquals(original, restored, "Round-trip serialization should produce equivalent record");
    }

    @Test
    void inputProps_optionalEmptySerializedAsNull() {
        final PropsDiffer<InputProps> differ = new PropsDiffer<>();
        final InputProps props = InputProps.defaults();

        final JsonObject json = differ.toJson(props);

        assertTrue(json.isNull("minlength"), "Optional.empty() should serialize as null");
        assertTrue(json.isNull("maxlength"), "Optional.empty() should serialize as null");
        assertTrue(json.isNull("min"), "Optional.empty() should serialize as null");
        assertTrue(json.isNull("max"), "Optional.empty() should serialize as null");
        assertTrue(json.isNull("pattern"), "Optional.empty() should serialize as null");
    }

    @Test
    void inputProps_optionalPresentSerializedAsValue() {
        final PropsDiffer<InputProps> differ = new PropsDiffer<>();
        final InputProps props = new InputProps("email", "test@example.com", "Email",
            false, true, true, Optional.of(5), Optional.of(100),
            Optional.of(1.0), Optional.of(200.0), Optional.of(".*@.*"), "Please enter a valid email", Size.LARGE);

        final JsonObject json = differ.toJson(props);

        assertEquals(5, json.getInt("minlength"));
        assertEquals(100, json.getInt("maxlength"));
        assertEquals(1.0, json.getJsonNumber("min").doubleValue(), 0.001);
        assertEquals(200.0, json.getJsonNumber("max").doubleValue(), 0.001);
        assertEquals(".*@.*", json.getString("pattern"));
        assertEquals("Please enter a valid email", json.getString("customValidity"));
        assertEquals("large", json.getString("size"));
        assertFalse(json.getBoolean("disabled"));
        assertTrue(json.getBoolean("readonly"));
        assertTrue(json.getBoolean("required"));
    }

    @Test
    void inputProps_roundTrip() {
        final PropsDiffer<InputProps> differ = new PropsDiffer<>();
        final InputProps original = new InputProps("email", "test@example.com", "Email",
            false, true, true, Optional.of(5), Optional.of(100),
            Optional.of(1.0), Optional.of(200.0), Optional.of(".*@.*"), "Please enter a valid email", Size.LARGE);

        final JsonObject json = differ.toJson(original);
        final InputProps restored = differ.fromJson(json, InputProps.class);

        assertEquals(original, restored, "Round-trip serialization should produce equivalent record");
    }

    @Test
    void inputProps_roundTripWithOptionalEmpty() {
        final PropsDiffer<InputProps> differ = new PropsDiffer<>();
        final InputProps original = InputProps.defaults();

        final JsonObject json = differ.toJson(original);
        final InputProps restored = differ.fromJson(json, InputProps.class);

        assertEquals(original, restored, "Round-trip with Optional.empty() should produce equivalent record");
    }

    @Test
    void alertProps_roundTripWithAllVariants() {
        final PropsDiffer<AlertProps> differ = new PropsDiffer<>();

        for (final Variant variant : Variant.values()) {
            final AlertProps original = new AlertProps(true, true, variant, Optional.of("3000"));
            final JsonObject json = differ.toJson(original);
            final AlertProps restored = differ.fromJson(json, AlertProps.class);

            assertEquals(original, restored,
                "Round-trip for Variant." + variant.name() + " should produce equivalent record");
        }
    }

    @Test
    void alertProps_roundTripWithOptionalEmpty() {
        final PropsDiffer<AlertProps> differ = new PropsDiffer<>();
        final AlertProps original = AlertProps.defaults();

        final JsonObject json = differ.toJson(original);
        final AlertProps restored = differ.fromJson(json, AlertProps.class);

        assertEquals(original, restored);
    }

    @Test
    void defaults_roundTrip() {
        // Verify all defaults() factory methods produce records that survive round-trip
        final PropsDiffer<ButtonProps> buttonDiffer = new PropsDiffer<>();
        assertEquals(ButtonProps.defaults(),
            buttonDiffer.fromJson(buttonDiffer.toJson(ButtonProps.defaults()), ButtonProps.class));

        final PropsDiffer<InputProps> inputDiffer = new PropsDiffer<>();
        assertEquals(InputProps.defaults(),
            inputDiffer.fromJson(inputDiffer.toJson(InputProps.defaults()), InputProps.class));

        final PropsDiffer<AlertProps> alertDiffer = new PropsDiffer<>();
        assertEquals(AlertProps.defaults(),
            alertDiffer.fromJson(alertDiffer.toJson(AlertProps.defaults()), AlertProps.class));
    }
}
