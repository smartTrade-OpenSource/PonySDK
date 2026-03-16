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

package com.ponysdk.sample.client.playground;

import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based and unit tests for {@link LabelFormatter}.
 */
class LabelFormatterTest {

    // ── Property-based tests ──

    // Feature: property-label-improvements, Property 1: Prefix removal
    @Property(tries = 100)
    void prefixRemoval_setPrefix(@ForAll("camelCaseSuffix") String suffix) {
        final String input = "set" + suffix;
        final String result = LabelFormatter.format(input);
        // Result should not start with "Set " when suffix is non-empty
        assertThat(result).doesNotStartWith("Set ");
        // Result should contain the suffix content (first letter of suffix capitalized)
        assertThat(result).startsWith(String.valueOf(Character.toUpperCase(suffix.charAt(0))));
    }

    // Feature: property-label-improvements, Property 1: Prefix removal (is)
    @Property(tries = 100)
    void prefixRemoval_isPrefix(@ForAll("camelCaseSuffix") String suffix) {
        final String input = "is" + suffix;
        final String result = LabelFormatter.format(input);
        assertThat(result).doesNotStartWith("Is ");
        assertThat(result).startsWith(String.valueOf(Character.toUpperCase(suffix.charAt(0))));
    }

    // Feature: property-label-improvements, Property 2: CamelCase splitting
    @Property(tries = 100)
    void camelCaseSplitting_producesCapitalizedWords(@ForAll("camelCaseWord") String input) {
        final String result = LabelFormatter.format(input);
        assertThat(result).isNotEmpty();
        // Each word should start with uppercase
        for (final String word : result.split(" ")) {
            assertThat(word).isNotEmpty();
            assertThat(Character.isUpperCase(word.charAt(0)))
                .as("Word '%s' should start with uppercase in result '%s' from input '%s'", word, result, input)
                .isTrue();
        }
    }

    // Feature: property-label-improvements, Property 3: Idempotence
    @Property(tries = 200)
    void idempotence(@ForAll("alphanumericWord") String input) {
        Assume.that(!input.isEmpty());
        final String once = LabelFormatter.format(input);
        final String twice = LabelFormatter.format(once);
        assertThat(twice).isEqualTo(once);
    }

    // ── Generators ──

    @Provide
    Arbitrary<String> camelCaseSuffix() {
        // Generates strings starting with uppercase letter followed by lowercase letters
        return Arbitraries.strings()
            .withCharRange('A', 'Z').ofMinLength(1).ofMaxLength(1)
            .flatMap(upper -> Arbitraries.strings()
                .withCharRange('a', 'z').ofMinLength(0).ofMaxLength(10)
                .map(lower -> upper + lower));
    }

    @Provide
    Arbitrary<String> camelCaseWord() {
        // Generates camelCase words like "myVariableName"
        final Arbitrary<String> segment = Arbitraries.strings()
            .withCharRange('A', 'Z').ofMinLength(1).ofMaxLength(1)
            .flatMap(upper -> Arbitraries.strings()
                .withCharRange('a', 'z').ofMinLength(1).ofMaxLength(6)
                .map(lower -> upper + lower));

        return segment.list().ofMinSize(1).ofMaxSize(4)
            .map(segments -> {
                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < segments.size(); i++) {
                    final String s = segments.get(i);
                    if (i == 0) {
                        // First segment starts lowercase
                        sb.append(Character.toLowerCase(s.charAt(0))).append(s.substring(1));
                    } else {
                        sb.append(s);
                    }
                }
                return sb.toString();
            });
    }

    @Provide
    Arbitrary<String> alphanumericWord() {
        return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(15);
    }

    // ── Unit tests ──

    @Test
    void emptyString_returnsEmpty() {
        assertThat(LabelFormatter.format("")).isEmpty();
    }

    @Test
    void prefixOnly_set_returnsOriginal() {
        assertThat(LabelFormatter.format("set")).isEqualTo("Set");
    }

    @Test
    void prefixOnly_is_returnsOriginal() {
        assertThat(LabelFormatter.format("is")).isEqualTo("Is");
    }

    @Test
    void null_throwsException() {
        assertThatThrownBy(() -> LabelFormatter.format(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setVariant_returnsVariant() {
        assertThat(LabelFormatter.format("setVariant")).isEqualTo("Variant");
    }

    @Test
    void isDisabled_returnsDisabled() {
        assertThat(LabelFormatter.format("isDisabled")).isEqualTo("Disabled");
    }

    @Test
    void setOuterGlow_returnsOuterGlow() {
        assertThat(LabelFormatter.format("setOuterGlow")).isEqualTo("Outer Glow");
    }

    @Test
    void simpleWord_capitalizesFirst() {
        assertThat(LabelFormatter.format("variant")).isEqualTo("Variant");
    }

    @Test
    void alreadyFormatted_remainsUnchanged() {
        assertThat(LabelFormatter.format("Outer Glow")).isEqualTo("Outer Glow");
    }
}
