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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based and unit tests for {@link TypeHintResolver}.
 */
class TypeHintResolverTest {

    // Test enum for property-based tests
    private enum TestVariant { PRIMARY, SECONDARY }
    private enum TestSize { SMALL, MEDIUM, LARGE }

    private static final Map<Class<?>, String> EXPECTED_HINTS = Map.of(
        String.class, "String",
        boolean.class, "boolean",
        Boolean.class, "boolean",
        int.class, "number",
        Integer.class, "number",
        long.class, "number",
        Long.class, "number"
    );

    // Feature: property-label-improvements, Property 4: Type hint resolution consistency
    @Property(tries = 100)
    void typeHintResolution_supportedTypes(@ForAll("supportedParameterInfo") ParameterInfo param) {
        final String result = TypeHintResolver.resolve(param);
        assertThat(result).isNotNull().isNotEmpty();

        final Class<?> type = param.type();
        if (EXPECTED_HINTS.containsKey(type)) {
            assertThat(result).isEqualTo(EXPECTED_HINTS.get(type));
        } else if (type.isEnum()) {
            assertThat(result).isEqualTo(type.getSimpleName());
        }
    }

    @Provide
    Arbitrary<ParameterInfo> supportedParameterInfo() {
        final List<Class<?>> types = List.of(
            String.class, boolean.class, Boolean.class,
            int.class, Integer.class, long.class, Long.class,
            TestVariant.class, TestSize.class
        );
        return Arbitraries.of(types)
            .map(type -> new ParameterInfo("param", type, false, null));
    }

    // ── Unit tests ──

    @Test
    void null_throwsException() {
        assertThatThrownBy(() -> TypeHintResolver.resolve(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownType_returnsUnknown() {
        final ParameterInfo param = new ParameterInfo("param", Object.class, false, null);
        assertThat(TypeHintResolver.resolve(param)).isEqualTo("unknown");
    }

    @Test
    void stringType_returnsString() {
        final ParameterInfo param = new ParameterInfo("param", String.class, false, null);
        assertThat(TypeHintResolver.resolve(param)).isEqualTo("String");
    }

    @Test
    void booleanPrimitive_returnsBoolean() {
        final ParameterInfo param = new ParameterInfo("param", boolean.class, false, null);
        assertThat(TypeHintResolver.resolve(param)).isEqualTo("boolean");
    }

    @Test
    void enumType_returnsSimpleName() {
        final ParameterInfo param = new ParameterInfo("param", TestVariant.class, false, null);
        assertThat(TypeHintResolver.resolve(param)).isEqualTo("TestVariant");
    }

    @Test
    void intType_returnsNumber() {
        final ParameterInfo param = new ParameterInfo("param", int.class, false, null);
        assertThat(TypeHintResolver.resolve(param)).isEqualTo("number");
    }
}
