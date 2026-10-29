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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.Test;

import com.ponysdk.test.PSuite;

public class PEventTest extends PSuite {

    @Test
    public void typedAccessorsReadTheFields() {
        final PLabel source = Element.newPLabel();
        final JsonObject fields = Json.createObjectBuilder()
            .add("s", "text")
            .add("n", 1.5)
            .add("t", true)
            .add("f", false)
            .addNull("z")
            .add("o", Json.createObjectBuilder().add("k", 1))
            .build();
        final PEvent event = new PEvent(source, "input", fields);

        assertSame(source, event.getSource());
        assertEquals("input", event.getName());
        assertSame(fields, event.getFields());

        assertEquals("text", event.getString("s"));
        assertEquals("1.5", event.getString("n"));
        assertEquals("{\"k\":1}", event.getString("o"));
        assertNull(event.getString("z"));
        assertNull(event.getString("missing"));

        assertEquals(1.5, event.getNumber("n"));
        assertNull(event.getNumber("s"));
        assertNull(event.getNumber("missing"));

        assertEquals(Boolean.TRUE, event.getBoolean("t"));
        assertEquals(Boolean.FALSE, event.getBoolean("f"));
        assertNull(event.getBoolean("s"));
        assertNull(event.getBoolean("missing"));

        assertEquals("PEvent input " + fields, event.toString());
    }
}
