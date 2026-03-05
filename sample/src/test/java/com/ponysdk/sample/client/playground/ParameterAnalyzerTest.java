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

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ParameterAnalyzer}.
 */
public class ParameterAnalyzerTest {

    private ParameterAnalyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new ParameterAnalyzer();
    }

    // Test enum for testing
    enum TestEnum {
        VALUE1, VALUE2, VALUE3
    }

    @Test
    public void testDetermineControlType_forString_shouldReturnTextBox() {
        final ParameterInfo param = new ParameterInfo("test", String.class, false, null);
        
        assertEquals(ParameterAnalyzer.ControlType.TEXT_BOX, analyzer.determineControlType(param));
    }

    @Test
    public void testDetermineControlType_forPrimitiveBoolean_shouldReturnCheckBox() {
        final ParameterInfo param = new ParameterInfo("test", boolean.class, false, null);
        
        assertEquals(ParameterAnalyzer.ControlType.CHECK_BOX, analyzer.determineControlType(param));
    }

    @Test
    public void testDetermineControlType_forBooleanWrapper_shouldReturnCheckBox() {
        final ParameterInfo param = new ParameterInfo("test", Boolean.class, false, null);
        
        assertEquals(ParameterAnalyzer.ControlType.CHECK_BOX, analyzer.determineControlType(param));
    }

    @Test
    public void testDetermineControlType_forPrimitiveInt_shouldReturnNumericTextBox() {
        final ParameterInfo param = new ParameterInfo("test", int.class, false, null);
        
        assertEquals(ParameterAnalyzer.ControlType.NUMERIC_TEXT_BOX, analyzer.determineControlType(param));
    }

    @Test
    public void testDetermineControlType_forIntegerWrapper_shouldReturnNumericTextBox() {
        final ParameterInfo param = new ParameterInfo("test", Integer.class, false, null);
        
        assertEquals(ParameterAnalyzer.ControlType.NUMERIC_TEXT_BOX, analyzer.determineControlType(param));
    }

    @Test
    public void testDetermineControlType_forPrimitiveLong_shouldReturnNumericTextBox() {
        final ParameterInfo param = new ParameterInfo("test", long.class, false, null);
        
        assertEquals(ParameterAnalyzer.ControlType.NUMERIC_TEXT_BOX, analyzer.determineControlType(param));
    }

    @Test
    public void testDetermineControlType_forLongWrapper_shouldReturnNumericTextBox() {
        final ParameterInfo param = new ParameterInfo("test", Long.class, false, null);
        
        assertEquals(ParameterAnalyzer.ControlType.NUMERIC_TEXT_BOX, analyzer.determineControlType(param));
    }

    @Test
    public void testDetermineControlType_forEnum_shouldReturnListBox() {
        final ParameterInfo param = new ParameterInfo("test", TestEnum.class, false, null);
        
        assertEquals(ParameterAnalyzer.ControlType.LIST_BOX, analyzer.determineControlType(param));
    }

    @Test
    public void testDetermineControlType_forOptional_shouldReturnOptionalControl() {
        final ParameterInfo param = new ParameterInfo("test", Optional.class, true, String.class);
        
        assertEquals(ParameterAnalyzer.ControlType.OPTIONAL_CONTROL, analyzer.determineControlType(param));
    }

    @Test
    public void testDetermineControlType_forUnsupportedType_shouldReturnUnsupported() {
        final ParameterInfo param = new ParameterInfo("test", Object.class, false, null);
        
        assertEquals(ParameterAnalyzer.ControlType.UNSUPPORTED, analyzer.determineControlType(param));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetermineControlType_withNull_shouldThrowException() {
        analyzer.determineControlType(null);
    }

    @Test
    public void testIsStringType_forString_shouldReturnTrue() {
        final ParameterInfo param = new ParameterInfo("test", String.class, false, null);
        
        assertTrue(analyzer.isStringType(param));
    }

    @Test
    public void testIsStringType_forNonString_shouldReturnFalse() {
        final ParameterInfo param = new ParameterInfo("test", int.class, false, null);
        
        assertFalse(analyzer.isStringType(param));
    }

    @Test
    public void testIsStringType_forOptionalString_shouldReturnFalse() {
        final ParameterInfo param = new ParameterInfo("test", Optional.class, true, String.class);
        
        assertFalse(analyzer.isStringType(param));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsStringType_withNull_shouldThrowException() {
        analyzer.isStringType(null);
    }

    @Test
    public void testIsBooleanType_forPrimitiveBoolean_shouldReturnTrue() {
        final ParameterInfo param = new ParameterInfo("test", boolean.class, false, null);
        
        assertTrue(analyzer.isBooleanType(param));
    }

    @Test
    public void testIsBooleanType_forBooleanWrapper_shouldReturnTrue() {
        final ParameterInfo param = new ParameterInfo("test", Boolean.class, false, null);
        
        assertTrue(analyzer.isBooleanType(param));
    }

    @Test
    public void testIsBooleanType_forNonBoolean_shouldReturnFalse() {
        final ParameterInfo param = new ParameterInfo("test", String.class, false, null);
        
        assertFalse(analyzer.isBooleanType(param));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsBooleanType_withNull_shouldThrowException() {
        analyzer.isBooleanType(null);
    }

    @Test
    public void testIsNumericType_forPrimitiveInt_shouldReturnTrue() {
        final ParameterInfo param = new ParameterInfo("test", int.class, false, null);
        
        assertTrue(analyzer.isNumericType(param));
    }

    @Test
    public void testIsNumericType_forIntegerWrapper_shouldReturnTrue() {
        final ParameterInfo param = new ParameterInfo("test", Integer.class, false, null);
        
        assertTrue(analyzer.isNumericType(param));
    }

    @Test
    public void testIsNumericType_forPrimitiveLong_shouldReturnTrue() {
        final ParameterInfo param = new ParameterInfo("test", long.class, false, null);
        
        assertTrue(analyzer.isNumericType(param));
    }

    @Test
    public void testIsNumericType_forLongWrapper_shouldReturnTrue() {
        final ParameterInfo param = new ParameterInfo("test", Long.class, false, null);
        
        assertTrue(analyzer.isNumericType(param));
    }

    @Test
    public void testIsNumericType_forNonNumeric_shouldReturnFalse() {
        final ParameterInfo param = new ParameterInfo("test", String.class, false, null);
        
        assertFalse(analyzer.isNumericType(param));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsNumericType_withNull_shouldThrowException() {
        analyzer.isNumericType(null);
    }

    @Test
    public void testIsEnumType_forEnum_shouldReturnTrue() {
        final ParameterInfo param = new ParameterInfo("test", TestEnum.class, false, null);
        
        assertTrue(analyzer.isEnumType(param));
    }

    @Test
    public void testIsEnumType_forNonEnum_shouldReturnFalse() {
        final ParameterInfo param = new ParameterInfo("test", String.class, false, null);
        
        assertFalse(analyzer.isEnumType(param));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsEnumType_withNull_shouldThrowException() {
        analyzer.isEnumType(null);
    }

    @Test
    public void testIsOptionalType_forOptional_shouldReturnTrue() {
        final ParameterInfo param = new ParameterInfo("test", Optional.class, true, String.class);
        
        assertTrue(analyzer.isOptionalType(param));
    }

    @Test
    public void testIsOptionalType_forNonOptional_shouldReturnFalse() {
        final ParameterInfo param = new ParameterInfo("test", String.class, false, null);
        
        assertFalse(analyzer.isOptionalType(param));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsOptionalType_withNull_shouldThrowException() {
        analyzer.isOptionalType(null);
    }

    @Test
    public void testGetOptionalWrappedType_forOptional_shouldReturnWrappedType() {
        final ParameterInfo param = new ParameterInfo("test", Optional.class, true, String.class);
        
        assertEquals(String.class, analyzer.getOptionalWrappedType(param));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetOptionalWrappedType_forNonOptional_shouldThrowException() {
        final ParameterInfo param = new ParameterInfo("test", String.class, false, null);
        analyzer.getOptionalWrappedType(param);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetOptionalWrappedType_withNull_shouldThrowException() {
        analyzer.getOptionalWrappedType(null);
    }

    @Test
    public void testIsSupportedType_forSupportedTypes_shouldReturnTrue() {
        assertTrue(analyzer.isSupportedType(new ParameterInfo("test", String.class, false, null)));
        assertTrue(analyzer.isSupportedType(new ParameterInfo("test", boolean.class, false, null)));
        assertTrue(analyzer.isSupportedType(new ParameterInfo("test", int.class, false, null)));
        assertTrue(analyzer.isSupportedType(new ParameterInfo("test", long.class, false, null)));
        assertTrue(analyzer.isSupportedType(new ParameterInfo("test", TestEnum.class, false, null)));
        assertTrue(analyzer.isSupportedType(new ParameterInfo("test", Optional.class, true, String.class)));
    }

    @Test
    public void testIsSupportedType_forUnsupportedType_shouldReturnFalse() {
        final ParameterInfo param = new ParameterInfo("test", Object.class, false, null);
        
        assertFalse(analyzer.isSupportedType(param));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsSupportedType_withNull_shouldThrowException() {
        analyzer.isSupportedType(null);
    }
}
