/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.core.ui.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class RouteParametersTest {

    private final RouteParameters parameters = new RouteParameters("orders/42?page=3&big=9000000000&flag=False&word=x",
        Map.of("id", "42"), Tokens.query("page=3&big=9000000000&flag=False&word=x&id=7"));

    @Test
    public void aValueIsReadFromThePathThenFromTheQuery() {
        assertEquals("42", parameters.get("id"));
        assertEquals("x", parameters.get("word"));
        assertNull(parameters.get("absent"));
        assertEquals("none", parameters.get("absent", "none"));
        assertEquals("x", parameters.get("word", "none"));
        assertEquals("orders/42?page=3&big=9000000000&flag=False&word=x", parameters.token());
        assertEquals(parameters.token(), parameters.toString());
    }

    @Test
    public void anIntegerIsRequiredOrHasAFallback() {
        assertEquals(42, parameters.getInt("id"));
        assertEquals(3, parameters.getInt("page", 1));
        assertEquals(1, parameters.getInt("word", 1));
        assertEquals(1, parameters.getInt("absent", 1));
        assertEquals(1, parameters.getInt("big", 1), "too large for an int");
        assertThrows(IllegalArgumentException.class, () -> parameters.getInt("absent"));
        assertThrows(IllegalArgumentException.class, () -> parameters.getInt("word"));
    }

    @Test
    public void aLongIsRequiredOrHasAFallback() {
        assertEquals(9_000_000_000L, parameters.getLong("big"));
        assertEquals(9_000_000_000L, parameters.getLong("big", 1));
        assertEquals(1L, parameters.getLong("word", 1));
        assertEquals(1L, parameters.getLong("absent", 1));
        assertThrows(IllegalArgumentException.class, () -> parameters.getLong("absent"));
    }

    @Test
    public void aBooleanIsTrueOrFalseInAnyCaseOrTheFallback() {
        assertFalse(parameters.getBoolean("flag", true));
        assertTrue(new RouteParameters("t", Map.of("on", "TRUE"), Map.of()).getBoolean("on", false));
        assertTrue(parameters.getBoolean("word", true));
        assertTrue(parameters.getBoolean("absent", true));
        assertFalse(parameters.getBoolean("absent", false));
    }
}
