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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ValueTypeModelTest {

    /**
     * Test method for {@link com.ponysdk.core.model.ValueTypeModel#fromRawValue(int)}.
     */
    @Test
    public void testFromRawValue() {
        final ValueTypeModel expected = ValueTypeModel.SHORT;
        assertEquals(expected, ValueTypeModel.fromRawValue(expected.getValue()));
    }

    /**
     * Test method for {@link com.ponysdk.core.model.ValueTypeModel#getSize()}.
     */
    @Test
    public void testGetSize() {
        assertEquals(2, ValueTypeModel.SHORT.getSize());
    }

    @Test
    public void acceptsOnlyTheValuesTheEncoderCasts() {
        assertTrue(ValueTypeModel.NULL.accepts("anything"));
        assertTrue(ValueTypeModel.BOOLEAN.accepts(true));
        assertTrue(ValueTypeModel.BYTE.accepts((byte) 1));
        assertTrue(ValueTypeModel.SHORT.accepts((short) 1));
        assertFalse(ValueTypeModel.SHORT.accepts(1));
        assertTrue(ValueTypeModel.INTEGER.accepts(1));
        assertTrue(ValueTypeModel.UINT31.accepts(1));
        assertFalse(ValueTypeModel.INTEGER.accepts(1L));
        assertTrue(ValueTypeModel.LONG.accepts(1L));
        assertFalse(ValueTypeModel.LONG.accepts(1));
        assertTrue(ValueTypeModel.DOUBLE.accepts(1.0));
        assertTrue(ValueTypeModel.FLOAT.accepts(1f));
        assertFalse(ValueTypeModel.FLOAT.accepts(1.0));
        assertTrue(ValueTypeModel.STRING.accepts(null));
        assertTrue(ValueTypeModel.STRING.accepts("s"));
        assertFalse(ValueTypeModel.STRING.accepts(1));
        assertTrue(ValueTypeModel.ARRAY.accepts(new Object[0]));
        assertFalse(ValueTypeModel.ARRAY.accepts(null));
    }

}
