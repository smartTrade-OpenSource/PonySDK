/*
 * Copyright (c) 2018 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class ArrayValueModelTest {

    /**
     * Test method for {@link com.ponysdk.core.model.ArrayValueModel#fromRawValue(int)}.
     */
    @Test
    public void testFromRawValue() {
        final ArrayValueModel expected = ArrayValueModel.DOUBLE;
        assertEquals(expected, ArrayValueModel.fromRawValue(expected.getValue()));
    }

    /**
     * Test method for {@link com.ponysdk.core.model.ArrayValueModel#isDynamicSize()}.
     */
    @Test
    public void testIsDynamicSize() {
        assertFalse(ArrayValueModel.DOUBLE.isDynamicSize());
        assertTrue(ArrayValueModel.STRING_UTF8_UINT8_LENGTH.isDynamicSize());
    }

    /**
     * Test method for {@link com.ponysdk.core.model.ArrayValueModel#isDynamicSize()}.
     */
    @Test
    public void testMinSize() {
        assertEquals(0, ArrayValueModel.BOOLEAN_FALSE.getMinSize());
        assertEquals(0, ArrayValueModel.BOOLEAN_FALSE.getMinSize());
        assertEquals(0, ArrayValueModel.NULL.getMinSize());
        assertEquals(1, ArrayValueModel.STRING_UTF8_UINT8_LENGTH.getMinSize());
        assertEquals(2, ArrayValueModel.STRING_UTF8_UINT16_LENGTH.getMinSize());
        assertEquals(4, ArrayValueModel.STRING_UTF8_UINT32_LENGTH.getMinSize());
        assertEquals(1, ArrayValueModel.STRING_ASCII_UINT8_LENGTH.getMinSize());
        assertEquals(2, ArrayValueModel.STRING_ASCII_UINT16_LENGTH.getMinSize());
        assertEquals(1, ArrayValueModel.BYTE.getMinSize());
        assertEquals(2, ArrayValueModel.SHORT.getMinSize());
        assertEquals(4, ArrayValueModel.INTEGER.getMinSize());
        assertEquals(4, ArrayValueModel.FLOAT.getMinSize());
        assertEquals(8, ArrayValueModel.DOUBLE.getMinSize());
        assertEquals(8, ArrayValueModel.LONG.getMinSize());
    }
}
