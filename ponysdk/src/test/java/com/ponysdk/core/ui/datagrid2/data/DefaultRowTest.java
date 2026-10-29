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

public class DefaultRowTest {

    @Test
    public void rejectsNullData() {
        Assertions.assertThrows(NullPointerException.class, () -> new DefaultRow<String>(0, null));
    }

    @Test
    public void exposesIdDataAndAcceptance() {
        final DefaultRow<String> row = new DefaultRow<>(3, "a");

        Assertions.assertEquals(3, row.getID());
        Assertions.assertEquals("a", row.getData());
        Assertions.assertFalse(row.isAccepted());

        row.setData("b");
        row.setAcceptance(true);

        Assertions.assertEquals("b", row.getData());
        Assertions.assertTrue(row.isAccepted());
        Assertions.assertEquals("Row [id=3, data=b, accepted=true]", row.toString());
    }

    @Test
    public void equalityIgnoresIdButUsesDataAndAcceptance() {
        final DefaultRow<String> row = new DefaultRow<>(1, "a");
        final DefaultRow<String> sameData = new DefaultRow<>(2, "a");
        final DefaultRow<String> otherData = new DefaultRow<>(1, "b");

        Assertions.assertEquals(row, row);
        Assertions.assertEquals(row, sameData);
        Assertions.assertEquals(row.hashCode(), sameData.hashCode());
        Assertions.assertNotEquals(row, otherData);
        Assertions.assertNotEquals(row, null);
        Assertions.assertNotEquals(row, "a");

        sameData.setAcceptance(true);
        Assertions.assertNotEquals(row, sameData);
        Assertions.assertNotEquals(row.hashCode(), sameData.hashCode());
    }

    @Test
    public void equalityHandlesNullDataSetAfterConstruction() {
        final DefaultRow<String> row = new DefaultRow<>(1, "a");
        final DefaultRow<String> other = new DefaultRow<>(2, "a");
        row.setData(null);

        Assertions.assertNotEquals(row, other);
        Assertions.assertNotEquals(other, row);
        other.setData(null);
        Assertions.assertEquals(row, other);
        Assertions.assertEquals(row.hashCode(), other.hashCode());
    }
}
