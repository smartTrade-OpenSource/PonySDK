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
 * Unit tests for {@link DefaultTypeConverter}.
 */
public class DefaultTypeConverterTest {

    private DefaultTypeConverter converter;

    private enum TestEnum {
        VALUE_ONE, VALUE_TWO, VALUE_THREE
    }

    @Before
    public void setUp() {
        converter = new DefaultTypeConverter();
    }

    // String conversion tests
    @Test
    public void testConvertString_shouldPassThrough() throws ConversionException {
        assertEquals("hello", converter.convert("hello", String.class));
        assertEquals("world", converter.convert("world", String.class));
    }

    @Test
    public void testConvertString_shouldHandleNull() throws ConversionException {
        assertNull(converter.convert(null, String.class));
    }

    @Test
    public void testConvertString_shouldHandleEmpty() throws ConversionException {
        assertNull(converter.convert("", String.class));
        assertNull(converter.convert("   ", String.class));
    }

    // Boolean conversion tests
    @Test
    public void testConvertBoolean_shouldParseTrue() throws ConversionException {
        assertTrue(converter.convert("true", Boolean.class));
        assertTrue(converter.convert("TRUE", Boolean.class));
        assertTrue(converter.convert("True", Boolean.class));
        assertTrue(converter.convert("  true  ", Boolean.class));
    }

    @Test
    public void testConvertBoolean_shouldParseFalse() throws ConversionException {
        assertFalse(converter.convert("false", Boolean.class));
        assertFalse(converter.convert("FALSE", Boolean.class));
        assertFalse(converter.convert("False", Boolean.class));
        assertFalse(converter.convert("  false  ", Boolean.class));
    }

    @Test(expected = ConversionException.class)
    public void testConvertBoolean_shouldThrowOnInvalidValue() throws ConversionException {
        converter.convert("maybe", Boolean.class);
    }

    @Test
    public void testConvertBooleanPrimitive_shouldWork() throws ConversionException {
        assertTrue(converter.convert("true", boolean.class));
        assertFalse(converter.convert("false", boolean.class));
    }

    @Test(expected = ConversionException.class)
    public void testConvertBooleanPrimitive_shouldThrowOnNull() throws ConversionException {
        converter.convert(null, boolean.class);
    }

    // Integer conversion tests
    @Test
    public void testConvertInteger_shouldParseValidNumbers() throws ConversionException {
        assertEquals(Integer.valueOf(42), converter.convert("42", Integer.class));
        assertEquals(Integer.valueOf(-100), converter.convert("-100", Integer.class));
        assertEquals(Integer.valueOf(0), converter.convert("0", Integer.class));
        assertEquals(Integer.valueOf(123), converter.convert("  123  ", Integer.class));
    }

    @Test(expected = ConversionException.class)
    public void testConvertInteger_shouldThrowOnInvalidValue() throws ConversionException {
        converter.convert("abc", Integer.class);
    }

    @Test(expected = ConversionException.class)
    public void testConvertInteger_shouldThrowOnDecimal() throws ConversionException {
        converter.convert("12.34", Integer.class);
    }

    @Test
    public void testConvertIntegerPrimitive_shouldWork() throws ConversionException {
        assertEquals(Integer.valueOf(42), converter.convert("42", int.class));
    }

    @Test(expected = ConversionException.class)
    public void testConvertIntegerPrimitive_shouldThrowOnNull() throws ConversionException {
        converter.convert(null, int.class);
    }

    // Long conversion tests
    @Test
    public void testConvertLong_shouldParseValidNumbers() throws ConversionException {
        assertEquals(Long.valueOf(42L), converter.convert("42", Long.class));
        assertEquals(Long.valueOf(-100L), converter.convert("-100", Long.class));
        assertEquals(Long.valueOf(9223372036854775807L), converter.convert("9223372036854775807", Long.class));
        assertEquals(Long.valueOf(123L), converter.convert("  123  ", Long.class));
    }

    @Test(expected = ConversionException.class)
    public void testConvertLong_shouldThrowOnInvalidValue() throws ConversionException {
        converter.convert("xyz", Long.class);
    }

    @Test
    public void testConvertLongPrimitive_shouldWork() throws ConversionException {
        assertEquals(Long.valueOf(42L), converter.convert("42", long.class));
    }

    @Test(expected = ConversionException.class)
    public void testConvertLongPrimitive_shouldThrowOnNull() throws ConversionException {
        converter.convert(null, long.class);
    }

    // Enum conversion tests
    @Test
    public void testConvertEnum_shouldFindConstantByName() throws ConversionException {
        assertEquals(TestEnum.VALUE_ONE, converter.convert("VALUE_ONE", TestEnum.class));
        assertEquals(TestEnum.VALUE_TWO, converter.convert("VALUE_TWO", TestEnum.class));
        assertEquals(TestEnum.VALUE_THREE, converter.convert("VALUE_THREE", TestEnum.class));
    }

    @Test
    public void testConvertEnum_shouldHandleWhitespace() throws ConversionException {
        assertEquals(TestEnum.VALUE_ONE, converter.convert("  VALUE_ONE  ", TestEnum.class));
    }

    @Test(expected = ConversionException.class)
    public void testConvertEnum_shouldThrowOnInvalidConstant() throws ConversionException {
        converter.convert("INVALID", TestEnum.class);
    }

    @Test(expected = ConversionException.class)
    public void testConvertEnum_shouldBeCaseSensitive() throws ConversionException {
        converter.convert("value_one", TestEnum.class);
    }

    // Optional conversion tests
    @Test
    public void testConvertOptional_shouldWrapNonEmptyValue() throws ConversionException {
        final Optional<String> result = converter.convert("hello", Optional.class);
        assertTrue(result.isPresent());
        assertEquals("hello", result.get());
    }

    @Test
    public void testConvertOptional_shouldReturnEmptyForNull() throws ConversionException {
        final Optional<String> result = converter.convert(null, Optional.class);
        assertFalse(result.isPresent());
    }

    @Test
    public void testConvertOptional_shouldReturnEmptyForEmptyString() throws ConversionException {
        Optional<String> result = converter.convert("", Optional.class);
        assertFalse(result.isPresent());

        result = converter.convert("   ", Optional.class);
        assertFalse(result.isPresent());
    }

    // Unsupported type tests
    @Test(expected = ConversionException.class)
    public void testConvertUnsupportedType_shouldThrow() throws ConversionException {
        converter.convert("value", Object.class);
    }

    // Null target type test
    @Test(expected = ConversionException.class)
    public void testConvertNullTargetType_shouldThrow() throws ConversionException {
        converter.convert("value", null);
    }
}
