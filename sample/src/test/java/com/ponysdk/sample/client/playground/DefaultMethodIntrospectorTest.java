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

import static org.junit.Assert.*;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.ponysdk.core.ui.wa.WAButton;

/**
 * Unit tests for {@link DefaultMethodIntrospector}.
 */
public class DefaultMethodIntrospectorTest {

    private DefaultMethodIntrospector introspector;

    @Before
    public void setUp() {
        introspector = new DefaultMethodIntrospector();
    }

    @Test
    public void testDiscoverSetters_shouldReturnNonEmptyList() {
        final List<MethodSignature> setters = introspector.discoverSetters(WAButton.class);

        assertNotNull("Setter list should not be null", setters);
        assertFalse("Setter list should not be empty for WAButton", setters.isEmpty());
    }

    @Test
    public void testDiscoverSetters_shouldOnlyIncludeSetterMethods() {
        final List<MethodSignature> setters = introspector.discoverSetters(WAButton.class);

        for (final MethodSignature signature : setters) {
            assertTrue("Method " + signature.methodName() + " should start with 'set'",
                    signature.methodName().startsWith("set"));
        }
    }

    @Test
    public void testDiscoverSetters_shouldFilterMethodsWithMoreThan3Parameters() {
        final List<MethodSignature> setters = introspector.discoverSetters(WAButton.class);

        for (final MethodSignature signature : setters) {
            assertTrue("Method " + signature.methodName() + " should have 3 or fewer parameters",
                    signature.parameters().size() <= 3);
        }
    }

    @Test
    public void testDiscoverSetters_shouldReturnSortedList() {
        final List<MethodSignature> setters = introspector.discoverSetters(WAButton.class);

        // Verify list is sorted alphabetically by method name
        for (int i = 0; i < setters.size() - 1; i++) {
            final String current = setters.get(i).methodName();
            final String next = setters.get(i + 1).methodName();
            assertTrue("Methods should be sorted alphabetically: " + current + " should come before " + next,
                    current.compareTo(next) <= 0);
        }
    }

    @Test
    public void testDiscoverSetters_shouldExtractCompleteMethodSignature() {
        final List<MethodSignature> setters = introspector.discoverSetters(WAButton.class);

        assertFalse("Should have at least one setter", setters.isEmpty());

        for (final MethodSignature signature : setters) {
            assertNotNull("Method should not be null", signature.method());
            assertNotNull("Method name should not be null", signature.methodName());
            assertNotNull("Return type should not be null", signature.returnType());
            assertNotNull("Parameters list should not be null", signature.parameters());
        }
    }

    @Test
    public void testDiscoverSetters_shouldExtractParameterInformation() {
        final List<MethodSignature> setters = introspector.discoverSetters(WAButton.class);

        // Find a setter with parameters
        MethodSignature setterWithParams = null;
        for (MethodSignature s : setters) {
            if (!s.parameters().isEmpty()) {
                setterWithParams = s;
                break;
            }
        }
        assertNotNull("Should have at least one setter with parameters", setterWithParams);

        for (final ParameterInfo param : setterWithParams.parameters()) {
            assertNotNull("Parameter name should not be null", param.name());
            assertNotNull("Parameter type should not be null", param.type());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDiscoverSetters_shouldThrowOnNull() {
        introspector.discoverSetters(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDiscoverSetters_shouldReturnUnmodifiableList() {
        final List<MethodSignature> setters = introspector.discoverSetters(WAButton.class);
        setters.add(null);
    }

    @Test
    public void testDiscoverSetters_shouldDetectOptionalParameters() {
        // Create a test class with Optional parameters
        final List<MethodSignature> setters = introspector.discoverSetters(TestComponentWithOptional.class);

        MethodSignature optionalSetter = null;
        for (MethodSignature s : setters) {
            if (s.methodName().equals("setOptionalValue")) {
                optionalSetter = s;
                break;
            }
        }
        assertNotNull("Should find setOptionalValue method", optionalSetter);

        assertEquals(1, optionalSetter.parameters().size());
        final ParameterInfo param = optionalSetter.parameters().get(0);
        assertTrue("Parameter should be detected as Optional", param.isOptional());
        assertNotNull("Optional wrapped type should not be null", param.optionalWrappedType());
    }

    /**
     * Test component class with Optional parameters for testing.
     */
    public static class TestComponentWithOptional {
        public void setOptionalValue(final Optional<String> value) {
            // Test method
        }

        public void setRegularValue(final String value) {
            // Test method
        }

        public void getNonSetter() {
            // Should not be discovered
        }

        private void setPrivateMethod(final String value) {
            // Should not be discovered (not public)
        }

        public void setTooManyParameters(final String a, final String b, final String c, final String d) {
            // Should not be discovered (more than 3 parameters)
        }
    }
}
