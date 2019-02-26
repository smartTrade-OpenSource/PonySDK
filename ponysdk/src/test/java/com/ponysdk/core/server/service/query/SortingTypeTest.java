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

package com.ponysdk.core.server.service.query;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SortingTypeTest {

    /**
     * Test method for
     * {@link com.ponysdk.core.server.service.query.SortingType#isNone(com.ponysdk.core.server.service.query.SortingType)}.
     */
    @Test
    public void testIsNone() {
        assertTrue(SortingType.isNone(SortingType.NONE));
        assertFalse(SortingType.isNone(SortingType.ASCENDING));
        assertFalse(SortingType.isNone(SortingType.DESCENDING));
    }

    /**
     * Test method for
     * {@link com.ponysdk.core.server.service.query.SortingType#isDescending(com.ponysdk.core.server.service.query.SortingType)}.
     */
    @Test
    public void testIsDescending() {
        assertFalse(SortingType.isDescending(SortingType.NONE));
        assertFalse(SortingType.isDescending(SortingType.ASCENDING));
        assertTrue(SortingType.isDescending(SortingType.DESCENDING));
    }

    /**
     * Test method for
     * {@link com.ponysdk.core.server.service.query.SortingType#isAscending(com.ponysdk.core.server.service.query.SortingType)}.
     */
    @Test
    public void testIsAscending() {
        assertFalse(SortingType.isAscending(SortingType.NONE));
        assertTrue(SortingType.isAscending(SortingType.ASCENDING));
        assertFalse(SortingType.isAscending(SortingType.DESCENDING));
    }

}
