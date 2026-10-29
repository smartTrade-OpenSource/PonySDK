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
package com.ponysdk.core.ui.datagrid2.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DataGridFilterTest {

    @Test
    public void dataGridFilterPropertiesAndIdentity() {
        final DataGridFilter<String> filter = new DataGridFilter<>();
        filter.setKey("key");
        filter.setId("id");
        filter.setPredicate("x"::equals);
        filter.setActive(true);
        filter.setReinforcing(true);

        Assertions.assertEquals("key", filter.getKey());
        Assertions.assertEquals("id", filter.getId());
        Assertions.assertTrue(filter.getPredicate().test("x"));
        Assertions.assertTrue(filter.isActive());
        Assertions.assertTrue(filter.isReinforcing());

        final DataGridFilter<String> sameId = new DataGridFilter<>();
        sameId.setId("id");
        final DataGridFilter<String> otherId = new DataGridFilter<>();
        otherId.setId("other");

        Assertions.assertEquals(filter, filter);
        Assertions.assertEquals(filter, sameId);
        Assertions.assertEquals(filter.hashCode(), sameId.hashCode());
        Assertions.assertNotEquals(filter, otherId);
        Assertions.assertNotEquals(filter, null);
        Assertions.assertNotEquals(filter, "id");
    }
}
