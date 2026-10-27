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
package com.ponysdk.core.ui.datagrid2.view;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DataGridSnapshotTest {

    private static final Map<Integer, Integer> SORTS = Map.of(1, 2);
    private static final Set<Integer> FILTERS = Set.of(3);

    @Test
    public void equalWhenWindowSortsAndFiltersMatch() {
        final DataGridSnapshot snapshot = new DataGridSnapshot(10, 20, 5, SORTS, FILTERS);

        // the start of the update is not part of the comparison
        Assertions.assertTrue(snapshot.equals(new DataGridSnapshot(10, 20, 0, Map.of(1, 2), Set.of(3))));
        Assertions.assertTrue(snapshot.equals(new DataGridSnapshot(snapshot)));
    }

    @Test
    public void differentWhenAnyPartDiffers() {
        final DataGridSnapshot snapshot = new DataGridSnapshot(10, 20, 5, SORTS, FILTERS);

        Assertions.assertFalse(snapshot.equals(new DataGridSnapshot(11, 20, 5, SORTS, FILTERS)));
        Assertions.assertFalse(snapshot.equals(new DataGridSnapshot(10, 21, 5, SORTS, FILTERS)));
        Assertions.assertFalse(snapshot.equals(new DataGridSnapshot(10, 20, 5, Map.of(), FILTERS)));
        Assertions.assertFalse(snapshot.equals(new DataGridSnapshot(10, 20, 5, SORTS, Set.of())));
    }

    @Test
    public void copyIsIndependentOfTheSource() {
        final DataGridSnapshot source = new DataGridSnapshot(1, 2, 3, new java.util.HashMap<>(SORTS), new java.util.HashSet<>(FILTERS));
        final DataGridSnapshot copy = new DataGridSnapshot(source);

        source.sorts.put(4, 5);
        source.filters.add(6);

        Assertions.assertEquals(1, copy.firstRowIndex);
        Assertions.assertEquals(2, copy.size);
        Assertions.assertEquals(SORTS, copy.sorts);
        Assertions.assertEquals(FILTERS, copy.filters);
        Assertions.assertFalse(source.equals(copy));
    }
}
