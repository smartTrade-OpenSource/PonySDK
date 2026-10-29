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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class TemplateTest {

    @Test
    public void theEmptyTemplateMatchesTheEmptyPathOnly() {
        final Template root = Template.parse("");
        assertEquals(Map.of(), root.match(Tokens.segments("")));
        assertEquals(Map.of(), root.match(Tokens.segments("/")));
        assertNull(root.match(Tokens.segments("orders")));
        assertEquals("", root.shape());
        assertEquals("", root.key());
    }

    @Test
    public void aTemplateGivesItsParametersByName() {
        final Template template = Template.parse("orders/:id/lines/:line");
        assertEquals(Map.of("id", "42", "line", "a b"), template.match(Tokens.segments("orders/42/lines/a%20b")));
        assertNull(template.match(Tokens.segments("orders/42/lines")));
        assertNull(template.match(Tokens.segments("orders/42/rows/3")));
        assertEquals("/orders/:/lines/:", template.shape());
        assertEquals("0101", template.key());
    }

    @Test
    public void aLiteralIsComparedDecoded() {
        assertEquals(Map.of(), Template.parse("a b").match(Tokens.segments("a%20b")));
    }

    @Test
    public void expandingFillsAndEncodesTheParametersAndNotesWhichItUsed() {
        final Template template = Template.parse("orders/:id/edit");
        final Set<String> used = new HashSet<>();
        assertEquals("orders/a%20b%2Fc/edit", template.expand(Map.of("id", "a b/c", "page", 2), used));
        assertEquals(Set.of("id"), used);
        assertEquals("a%20b", Template.parse("a b").expand(Map.of(), used));
    }

    @Test
    public void expandingNeedsAValueForEachParameter() {
        assertThrows(IllegalArgumentException.class, () -> Template.parse(":id").expand(Map.of(), new HashSet<>()));
    }

    @Test
    public void aBadTemplateIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Template.parse("orders?x=1"));
        assertThrows(IllegalArgumentException.class, () -> Template.parse("orders/:"));
        assertThrows(IllegalArgumentException.class, () -> Template.parse(":id/x/:id"));
    }
}
