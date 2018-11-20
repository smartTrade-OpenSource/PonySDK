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

package com.ponysdk.core.server.context;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PSuite;

public class PObjectCacheTest extends PSuite {

    private PObjectCache cache;

    @Before
    public void setUp() {
        cache = new PObjectCache();
    }

    /**
     * Test method for {@link com.ponysdk.core.server.context.PObjectCache#add(com.ponysdk.core.ui.basic.PObject)}.
     */
    @Test
    public void testAdd() {
        final PObject uiObject1 = Element.newButton();
        final int uiObjectId1 = uiObject1.getID();

        cache.add(uiObject1);
        assertEquals(uiObject1, cache.get(uiObjectId1));
    }

}
