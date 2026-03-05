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

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.test.PSuite;

/**
 * Unit tests for {@link FormGenerator}.
 */
public class FormGeneratorTest extends PSuite {

    private FormGenerator formGenerator;

    @Before
    public void setUp() {
        formGenerator = new FormGenerator();
    }

    @Test
    public void testGenerateControls_withEmptyList_shouldReturnEmptyList() {
        final List<PropertyControl> controls = formGenerator.generateControls(List.of());
        
        assertNotNull(controls);
        assertTrue(controls.isEmpty());
    }

    @Test
    public void testGenerateControls_withSingleMethod_shouldReturnOneControl() throws Exception {
        final Method method = TestComponent.class.getMethod("setTitle", String.class);
        final ParameterInfo param = new ParameterInfo("title", String.class, false, null);
        final MethodSignature signature = new MethodSignature(
            method,
            "setTitle",
            void.class,
            List.of(param)
        );
        
        final List<PropertyControl> controls = formGenerator.generateControls(List.of(signature));
        
        assertNotNull(controls);
        assertEquals(1, controls.size());
    }

    @Test
    public void testGenerateControls_shouldCreateLabelWithMethodName() throws Exception {
        final Method method = TestComponent.class.getMethod("setTitle", String.class);
        final ParameterInfo param = new ParameterInfo("title", String.class, false, null);
        final MethodSignature signature = new MethodSignature(
            method,
            "setTitle",
            void.class,
            List.of(param)
        );
        
        final List<PropertyControl> controls = formGenerator.generateControls(List.of(signature));
        final PropertyControl control = controls.get(0);
        
        assertNotNull(control.label());
        assertEquals("setTitle", control.label().getText());
    }

    @Test
    public void testGenerateControls_shouldCreateAppropriateControl() throws Exception {
        final Method method = TestComponent.class.getMethod("setTitle", String.class);
        final ParameterInfo param = new ParameterInfo("title", String.class, false, null);
        final MethodSignature signature = new MethodSignature(
            method,
            "setTitle",
            void.class,
            List.of(param)
        );
        
        final List<PropertyControl> controls = formGenerator.generateControls(List.of(signature));
        final PropertyControl control = controls.get(0);
        
        assertNotNull(control.control());
        assertTrue(control.control() instanceof PTextBox);
    }

    @Test
    public void testGenerateControls_shouldCreateErrorLabel() throws Exception {
        final Method method = TestComponent.class.getMethod("setTitle", String.class);
        final ParameterInfo param = new ParameterInfo("title", String.class, false, null);
        final MethodSignature signature = new MethodSignature(
            method,
            "setTitle",
            void.class,
            List.of(param)
        );
        
        final List<PropertyControl> controls = formGenerator.generateControls(List.of(signature));
        final PropertyControl control = controls.get(0);
        
        assertNotNull(control.errorLabel());
        assertTrue(control.errorLabel() instanceof PLabel);
    }

    @Test
    public void testGenerateControls_errorLabelShouldBeInitiallyHidden() throws Exception {
        final Method method = TestComponent.class.getMethod("setTitle", String.class);
        final ParameterInfo param = new ParameterInfo("title", String.class, false, null);
        final MethodSignature signature = new MethodSignature(
            method,
            "setTitle",
            void.class,
            List.of(param)
        );
        
        final List<PropertyControl> controls = formGenerator.generateControls(List.of(signature));
        final PropertyControl control = controls.get(0);
        
        assertFalse(control.errorLabel().isVisible());
    }

    @Test
    public void testGenerateControls_shouldIncludeMethodSignature() throws Exception {
        final Method method = TestComponent.class.getMethod("setTitle", String.class);
        final ParameterInfo param = new ParameterInfo("title", String.class, false, null);
        final MethodSignature signature = new MethodSignature(
            method,
            "setTitle",
            void.class,
            List.of(param)
        );
        
        final List<PropertyControl> controls = formGenerator.generateControls(List.of(signature));
        final PropertyControl control = controls.get(0);
        
        assertNotNull(control.method());
        assertEquals(signature, control.method());
    }

    @Test
    public void testGenerateControls_withMultipleMethods_shouldReturnMultipleControls() throws Exception {
        final Method method1 = TestComponent.class.getMethod("setTitle", String.class);
        final Method method2 = TestComponent.class.getMethod("setEnabled", boolean.class);
        
        final ParameterInfo param1 = new ParameterInfo("title", String.class, false, null);
        final ParameterInfo param2 = new ParameterInfo("enabled", boolean.class, false, null);
        
        final MethodSignature signature1 = new MethodSignature(method1, "setTitle", void.class, List.of(param1));
        final MethodSignature signature2 = new MethodSignature(method2, "setEnabled", void.class, List.of(param2));
        
        final List<PropertyControl> controls = formGenerator.generateControls(List.of(signature1, signature2));
        
        assertNotNull(controls);
        assertEquals(2, controls.size());
        assertEquals("setTitle", controls.get(0).label().getText());
        assertEquals("setEnabled", controls.get(1).label().getText());
    }

    @Test
    public void testGenerateControls_withNoParameters_shouldCreateNoParamsLabel() throws Exception {
        final Method method = TestComponent.class.getMethod("reset");
        final MethodSignature signature = new MethodSignature(
            method,
            "reset",
            void.class,
            List.of()
        );
        
        final List<PropertyControl> controls = formGenerator.generateControls(List.of(signature));
        final PropertyControl control = controls.get(0);
        
        assertNotNull(control.control());
        assertTrue(control.control() instanceof PLabel);
        assertEquals("(no parameters)", ((PLabel) control.control()).getText());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateControls_withNullList_shouldThrowException() {
        formGenerator.generateControls(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_withNullFactory_shouldThrowException() {
        new FormGenerator(null);
    }

    // Test component class for reflection
    public static class TestComponent {
        public void setTitle(String title) {}
        public void setEnabled(boolean enabled) {}
        public void reset() {}
    }
}
