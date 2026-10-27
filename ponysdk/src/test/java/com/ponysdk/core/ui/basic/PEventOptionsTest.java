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

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ponysdk.test.PSuite;

public class PEventOptionsTest extends PSuite {

    @Test
    public void noneSendsOnlyTheName() {
        assertEquals("{\"name\":\"click\"}", PEventOptions.NONE.toJson("click"));
        assertEquals(List.of(), PEventOptions.NONE.getFields());
    }

    @Test
    public void everyOptionIsSent() {
        final PEventOptions options = PEventOptions.fields("value", "key")
            .throttle(Duration.ofMillis(100))
            .debounce(Duration.ofMillis(50))
            .preventDefault()
            .stopPropagation()
            .capture();
        assertEquals(List.of("value", "key"), options.getFields());
        assertEquals("{\"name\":\"input\",\"fields\":[\"value\",\"key\"],\"throttle\":100,\"debounce\":50,\"prevent\":true,"
                + "\"stop\":true,\"capture\":true}", options.toJson("input"));
    }

    @Test
    public void optionsAreValues() {
        final PEventOptions a = PEventOptions.fields("x").preventDefault();
        final PEventOptions b = PEventOptions.NONE.preventDefault().withFields("x");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, a.capture());
        assertNotEquals(a, a.stopPropagation());
        assertNotEquals(a, a.throttle(Duration.ofSeconds(1)));
        assertNotEquals(a, a.debounce(Duration.ofSeconds(1)));
        assertNotEquals(a, PEventOptions.fields("y").preventDefault());
        assertNotEquals(a, "x");
    }
}
