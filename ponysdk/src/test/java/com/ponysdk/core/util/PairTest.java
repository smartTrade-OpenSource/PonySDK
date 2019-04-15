/*
 * Copyright (c) 2019 PonySDK
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

package com.ponysdk.core.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PairTest {

    private Object o1;
    private Object o2;
    private Pair<Object, Object> pair1;

    @Before
    public void setUp() {
        o1 = new Object();
        o2 = new Object();
        pair1 = new Pair<>(o1, o2);
        assertNotNull(pair1.toString());
    }

    /**
     * Test method for {@link com.ponysdk.core.util.Pair#getFirst()}.
     */
    @Test
    public void testGetFirst() {
        assertEquals(o1, new Pair<>(o1, null).getFirst());
    }

    /**
     * Test method for {@link com.ponysdk.core.util.Pair#getSecond()}.
     */
    @Test
    public void testGetSecond() {
        assertEquals(o2, new Pair<>(null, o2).getSecond());
    }

    /**
     * Test method for {@link com.ponysdk.core.util.Pair#equals(java.lang.Object)}.
     */
    @Test
    public void testEquals() {
        assertTrue(pair1.equals(pair1));

        assertTrue(pair1.equals(new Pair<>(o1, o2)));
        assertEquals(pair1.hashCode(), new Pair<>(o1, o2).hashCode());

        assertTrue(new Pair<>(null, o2).equals(new Pair<>(null, o2)));
        assertEquals(new Pair<>(null, o2).hashCode(), new Pair<>(null, o2).hashCode());

        assertTrue(new Pair<>(o1, null).equals(new Pair<>(o1, null)));
        assertEquals(new Pair<>(o1, null).hashCode(), new Pair<>(o1, null).hashCode());
    }

    /**
     * Test method for {@link com.ponysdk.core.util.Pair#equals(java.lang.Object)}.
     */
    @Test
    public void testNotEquals() {
        assertFalse(pair1.equals(null));
        assertFalse(pair1.equals(new Object()));
        assertFalse(pair1.equals(new Pair<>(null, o2)));
        assertFalse(pair1.equals(new Pair<>(o1, null)));
        assertFalse(new Pair<>(null, o2).equals(pair1));
        assertFalse(new Pair<>(o1, null).equals(pair1));
    }

}
