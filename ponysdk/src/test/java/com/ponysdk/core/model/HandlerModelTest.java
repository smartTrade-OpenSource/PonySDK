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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HandlerModelTest {

    /**
     * Test method for {@link com.ponysdk.core.model.HandlerModel#fromRawValue(int)}.
     */
    @Test
    public void testFromRawValue() {
        final HandlerModel expected = HandlerModel.HANDLER_DOM_DRAG_LEAVE;
        assertEquals(expected, HandlerModel.fromRawValue(expected.getValue()));
    }

    /**
     * Test method for {@link com.ponysdk.core.model.HandlerModel#isDomHandler()}.
     */
    @Test
    public void testIsDomHandler() {
        assertTrue(HandlerModel.HANDLER_DOM_DRAG_LEAVE.isDomHandler());
        assertFalse(HandlerModel.HANDLER_EMBEDDED_STREAM_REQUEST.isDomHandler());
    }

}
