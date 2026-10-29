/*
 * Copyright (c) 2026 PonySDK
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
package com.ponysdk.core.ui.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ModelEnumsTest {

    @Test
    public void testKeyCodes() {
        assertTrue(PKeyCodes.ESCAPE.equals(27));
        assertFalse(PKeyCodes.ESCAPE.equals(PKeyCodes.TAB.getCode()));
        assertEquals("27", PKeyCodes.ESCAPE.getCodeToString());
        for (final PKeyCodes code : PKeyCodes.values()) {
            assertEquals(code.getCode(), PKeyCodes.fromInt(code.getCode()).getCode());
        }
        assertSame(PKeyCodes.ESCAPE, PKeyCodes.fromInt(27));
        assertSame(PKeyCodes.UNKNOWN, PKeyCodes.fromInt(123456));
    }

    @Test
    public void testEventTypes() {
        assertSame(PEventType.ONCLICK, PEventType.fromInt(0x00001));
        assertNull(PEventType.fromInt(Integer.MIN_VALUE));
        assertTrue(PEventType.ONKEYUP.equals(0x00200));
        assertFalse(PEventType.ONKEYUP.equals(0x00001));
        assertEquals("512", PEventType.ONKEYUP.getCodeToString());
    }
}
