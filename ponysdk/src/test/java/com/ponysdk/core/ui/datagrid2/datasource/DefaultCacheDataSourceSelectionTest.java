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

package com.ponysdk.core.ui.datagrid2.datasource;

import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.data.FilterController;
import com.ponysdk.core.ui.datagrid2.data.Interval;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

/**
 * Tests for selectKeys/unselectKeys batch operations on DefaultCacheDataSource.
 */
public class DefaultCacheDataSourceSelectionTest {

    private DefaultCacheDataSource<String, String> dataSource;

    @BeforeEach
    public void setUp() {
        dataSource = new DefaultCacheDataSource<>();
        dataSource.setFilterController(new FilterController<>());
        dataSource.setAdapter(new TestStringDataGridAdapter());
        // Add test data
        dataSource.setData("a");
        dataSource.setData("b");
        dataSource.setData("c");
        dataSource.setData("d");
        dataSource.setData("e");
    }

    @Test
    public void testSelectKeysSelectsMultipleRows() {
        final Interval interval = dataSource.selectKeys(List.of("a", "c", "e"));

        Assertions.assertNotNull(interval);
        Assertions.assertTrue(dataSource.isSelected("a"));
        Assertions.assertFalse(dataSource.isSelected("b"));
        Assertions.assertTrue(dataSource.isSelected("c"));
        Assertions.assertFalse(dataSource.isSelected("d"));
        Assertions.assertTrue(dataSource.isSelected("e"));
    }

    @Test
    public void testSelectKeysReturnsNullWhenNoKeysExist() {
        final Interval interval = dataSource.selectKeys(List.of("x", "y", "z"));

        Assertions.assertNull(interval);
    }

    @Test
    public void testSelectKeysReturnsNullForEmptyCollection() {
        final Interval interval = dataSource.selectKeys(Collections.emptyList());

        Assertions.assertNull(interval);
    }

    @Test
    public void testSelectKeysDoesNotDoubleSelect() {
        dataSource.select("a");
        Assertions.assertTrue(dataSource.isSelected("a"));

        final Interval interval = dataSource.selectKeys(List.of("a", "b"));

        Assertions.assertNotNull(interval);
        Assertions.assertTrue(dataSource.isSelected("a"));
        Assertions.assertTrue(dataSource.isSelected("b"));
        Assertions.assertEquals(2, dataSource.getlLiveSelectedDataCount());
    }

    @Test
    public void testUnselectKeysUnselectsMultipleRows() {
        dataSource.selectKeys(List.of("a", "b", "c", "d"));

        final Interval interval = dataSource.unselectKeys(List.of("b", "d"));

        Assertions.assertNotNull(interval);
        Assertions.assertTrue(dataSource.isSelected("a"));
        Assertions.assertFalse(dataSource.isSelected("b"));
        Assertions.assertTrue(dataSource.isSelected("c"));
        Assertions.assertFalse(dataSource.isSelected("d"));
    }

    @Test
    public void testUnselectKeysReturnsNullWhenNoneSelected() {
        final Interval interval = dataSource.unselectKeys(List.of("a", "b"));

        Assertions.assertNull(interval);
    }

    @Test
    public void testUnselectKeysReturnsNullForEmptyCollection() {
        dataSource.selectKeys(List.of("a", "b"));

        final Interval interval = dataSource.unselectKeys(Collections.emptyList());

        Assertions.assertNull(interval);
    }

    @Test
    public void testSelectKeysThenUnselectAllThenShiftClickScenario() {
        // Simulates: click row a (select), shift+click row d (selectKeys a-d), click elsewhere (unselectAll)
        dataSource.select("a");
        final Interval rangeInterval = dataSource.selectKeys(List.of("a", "b", "c", "d"));

        Assertions.assertNotNull(rangeInterval);
        Assertions.assertEquals(4, dataSource.getlLiveSelectedDataCount());

        dataSource.unselectAllData();
        Assertions.assertEquals(0, dataSource.getlLiveSelectedDataCount());
        Assertions.assertFalse(dataSource.isSelected("a"));
    }

    @Test
    public void testSelectKeysOnlySelectsExistingKeys() {
        // Mix of existing and non-existing keys
        final Interval interval = dataSource.selectKeys(List.of("a", "x", "c", "y"));

        Assertions.assertNotNull(interval);
        Assertions.assertTrue(dataSource.isSelected("a"));
        Assertions.assertTrue(dataSource.isSelected("c"));
        Assertions.assertFalse(dataSource.isSelected("x"));
        Assertions.assertEquals(2, dataSource.getlLiveSelectedDataCount());
    }

    @Test
    public void testSelectKeysIntervalCoversAllAffectedRows() {
        // "a" and "e" are the rows at positions 0 and 4 of the live data
        final Interval interval = dataSource.selectKeys(List.of("a", "e"));

        Assertions.assertNotNull(interval);
        // Interval covers the rows to redraw, from row 0 to row 4 inclusive
        Assertions.assertEquals(0, interval.from);
        Assertions.assertEquals(5, interval.to);
    }

    @Test
    public void testUnselectKeysIntervalCoversAllAffectedRows() {
        dataSource.selectKeys(List.of("a", "b", "c", "d", "e"));

        final Interval interval = dataSource.unselectKeys(List.of("a", "e"));

        Assertions.assertNotNull(interval);
        Assertions.assertTrue(interval.from <= interval.to);
        Assertions.assertEquals(3, dataSource.getlLiveSelectedDataCount());
    }

    @Test
    public void testSelectKeysAfterSelectAllLiveData() {
        // selectAllLiveData selects everything — selectKeys should have no additional effect
        dataSource.selectAllLiveData();

        Assertions.assertTrue(dataSource.isSelected("a"));
        Assertions.assertTrue(dataSource.isSelected("e"));
    }

    @Test
    public void testConsecutiveSelectKeysAccumulates() {
        // First shift-click: select a-c
        dataSource.selectKeys(List.of("a", "b", "c"));
        Assertions.assertEquals(3, dataSource.getlLiveSelectedDataCount());

        // Second shift-click: extend to d-e (without clearing previous)
        dataSource.selectKeys(List.of("d", "e"));
        Assertions.assertEquals(5, dataSource.getlLiveSelectedDataCount());
        Assertions.assertTrue(dataSource.isSelected("a"));
        Assertions.assertTrue(dataSource.isSelected("e"));
    }

    @Test
    public void testUnselectKeysPartialOverlap() {
        // Select a, b, c
        dataSource.selectKeys(List.of("a", "b", "c"));

        // Unselect b, d — only b is actually unselected, d was not selected
        final Interval interval = dataSource.unselectKeys(List.of("b", "d"));

        Assertions.assertNotNull(interval);
        Assertions.assertTrue(dataSource.isSelected("a"));
        Assertions.assertFalse(dataSource.isSelected("b"));
        Assertions.assertTrue(dataSource.isSelected("c"));
        Assertions.assertFalse(dataSource.isSelected("d"));
        Assertions.assertEquals(2, dataSource.getlLiveSelectedDataCount());
    }

    @Test
    public void testSelectKeysThenRemoveDataKeepsRemainingSelection() {
        dataSource.selectKeys(List.of("a", "b", "c"));

        dataSource.removeData("b");

        Assertions.assertTrue(dataSource.isSelected("a"));
        Assertions.assertFalse(dataSource.isSelected("b"));
        Assertions.assertTrue(dataSource.isSelected("c"));
        Assertions.assertEquals(2, dataSource.getlLiveSelectedDataCount());
    }

    @Test
    public void testSelectKeysFollowedByIndividualUnselect() {
        dataSource.selectKeys(List.of("a", "b", "c", "d"));

        dataSource.unselect("b");
        dataSource.unselect("d");

        Assertions.assertTrue(dataSource.isSelected("a"));
        Assertions.assertFalse(dataSource.isSelected("b"));
        Assertions.assertTrue(dataSource.isSelected("c"));
        Assertions.assertFalse(dataSource.isSelected("d"));
        Assertions.assertEquals(2, dataSource.getlLiveSelectedDataCount());
    }

    @Test
    public void testIndividualSelectFollowedByUnselectKeys() {
        dataSource.select("a");
        dataSource.select("b");
        dataSource.select("c");

        final Interval interval = dataSource.unselectKeys(List.of("a", "b", "c"));

        Assertions.assertNotNull(interval);
        Assertions.assertEquals(0, dataSource.getlLiveSelectedDataCount());
    }

    @Test
    public void testSelectKeysAllAlreadySelected() {
        dataSource.selectKeys(List.of("a", "b", "c"));

        // select(k) returns null when k is already selected → selectKeys returns null when no state changes
        final Interval interval = dataSource.selectKeys(List.of("a", "b", "c"));

        Assertions.assertNull(interval);
        Assertions.assertEquals(3, dataSource.getlLiveSelectedDataCount());
    }

    @Test
    public void testSelectKeysSingleElement() {
        final Interval interval = dataSource.selectKeys(List.of("c"));

        Assertions.assertNotNull(interval);
        Assertions.assertTrue(dataSource.isSelected("c"));
        Assertions.assertEquals(1, dataSource.getlLiveSelectedDataCount());
    }

    @Test
    public void testUnselectKeysSingleElement() {
        dataSource.select("c");

        final Interval interval = dataSource.unselectKeys(List.of("c"));

        Assertions.assertNotNull(interval);
        Assertions.assertFalse(dataSource.isSelected("c"));
        Assertions.assertEquals(0, dataSource.getlLiveSelectedDataCount());
    }

    @Test
    public void testSelectKeysAfterUnselectAll() {
        dataSource.selectKeys(List.of("a", "b", "c"));
        dataSource.unselectAllData();

        final Interval interval = dataSource.selectKeys(List.of("d", "e"));

        Assertions.assertNotNull(interval);
        Assertions.assertFalse(dataSource.isSelected("a"));
        Assertions.assertTrue(dataSource.isSelected("d"));
        Assertions.assertTrue(dataSource.isSelected("e"));
        Assertions.assertEquals(2, dataSource.getlLiveSelectedDataCount());
    }

    @Test
    public void testSelectKeysOnFilteredOutRowsReturnsNull() {
        // Set a filter that rejects "b" and "d"
        dataSource.setFilter("testFilter", "testFilter", false, new com.ponysdk.core.ui.datagrid2.data.AbstractFilter<>() {
            @Override
            public boolean test(final com.ponysdk.core.ui.datagrid2.data.DefaultRow<String> row) {
                final String v = row.getData();
                return !"b".equals(v) && !"d".equals(v);
            }

            @Override
            public boolean isActive() {
                return true;
            }

            @Override
            public com.ponysdk.core.ui.datagrid2.column.ColumnDefinition<String> getColumnDefinition() {
                return null;
            }
        });

        // "b" and "d" are filtered out — selecting them should return null
        final Interval interval = dataSource.selectKeys(List.of("b", "d"));
        Assertions.assertNull(interval);

        // "a" and "c" are still visible — selecting them should work
        final Interval interval2 = dataSource.selectKeys(List.of("a", "c"));
        Assertions.assertNotNull(interval2);
        Assertions.assertTrue(dataSource.isSelected("a"));
        Assertions.assertTrue(dataSource.isSelected("c"));
    }

    /**
     * Reusable minimal DataGridAdapter for String key/value tests.
     */
    private static class TestStringDataGridAdapter implements DataGridAdapter<String, String> {

        @Override
        public String getKey(final String data) {
            return data;
        }

        @Override
        public int compareDefault(final String v1, final String v2) {
            return v1.compareTo(v2);
        }

        @Override
        public List<com.ponysdk.core.ui.datagrid2.column.ColumnDefinition<String>> getColumnDefinitions() {
            return Collections.emptyList();
        }

        @Override
        public boolean hasHeader() {
            return false;
        }

        @Override
        public boolean hasFooter() {
            return false;
        }

        @Override
        public boolean isAscendingSortByInsertionOrder() {
            return true;
        }

        @Override
        public void onCreateHeaderRow(final com.ponysdk.core.ui.basic.IsPWidget w) {
        }

        @Override
        public void onCreateFooterRow(final com.ponysdk.core.ui.basic.IsPWidget w) {
        }

        @Override
        public void onCreateRow(final com.ponysdk.core.ui.basic.IsPWidget w) {
        }

        @Override
        public void onSelectRow(final com.ponysdk.core.ui.basic.IsPWidget w) {
        }

        @Override
        public void onUnselectRow(final com.ponysdk.core.ui.basic.IsPWidget w) {
        }

        @Override
        public void onCreateColumnResizer(final com.ponysdk.core.ui.basic.IsPWidget w) {
        }

        @Override
        public com.ponysdk.core.ui.basic.IsPWidget createLoadingDataWidget() {
            return null;
        }

        @Override
        public boolean isSelectionEnabled() {
            return true;
        }
    }

    // --- selectRange simulation: first click then shift-click ---

    @Test
    public void testSelectRangeSimulation_firstClickThenShiftClick() {
        // Simulates: user clicks "b" (anchor), then Shift+clicks "d"
        // Expected: range "b", "c", "d" selected
        dataSource.select("b");
        Assertions.assertEquals(1, dataSource.getlLiveSelectedDataCount());

        final Interval rangeInterval = dataSource.selectKeys(List.of("b", "c", "d"));

        Assertions.assertNotNull(rangeInterval);
        Assertions.assertFalse(dataSource.isSelected("a"));
        Assertions.assertTrue(dataSource.isSelected("b"));
        Assertions.assertTrue(dataSource.isSelected("c"));
        Assertions.assertTrue(dataSource.isSelected("d"));
        Assertions.assertFalse(dataSource.isSelected("e"));
        Assertions.assertEquals(3, dataSource.getlLiveSelectedDataCount());
    }

    @Test
    public void testSelectRangeSimulation_unselectRange() {
        // Simulates: range a-c selected, user Shift+clicks "b" to unselect range b-c
        dataSource.selectKeys(List.of("a", "b", "c"));

        // Shift+click on "b" while "a" (anchor) was already selected — unselect b-c range
        final Interval interval = dataSource.unselectKeys(List.of("b", "c"));

        Assertions.assertNotNull(interval);
        Assertions.assertTrue(dataSource.isSelected("a"));
        Assertions.assertFalse(dataSource.isSelected("b"));
        Assertions.assertFalse(dataSource.isSelected("c"));
        Assertions.assertEquals(1, dataSource.getlLiveSelectedDataCount());
    }

    @Test
    public void testSelectRangeSimulation_reverseRange() {
        // Simulates: user clicks "d" (anchor), then Shift+clicks "b" (upward range)
        dataSource.select("d");
        final Interval rangeInterval = dataSource.selectKeys(List.of("b", "c", "d"));

        Assertions.assertNotNull(rangeInterval);
        Assertions.assertFalse(dataSource.isSelected("a"));
        Assertions.assertTrue(dataSource.isSelected("b"));
        Assertions.assertTrue(dataSource.isSelected("c"));
        Assertions.assertTrue(dataSource.isSelected("d"));
        Assertions.assertFalse(dataSource.isSelected("e"));
    }

    @Test
    public void testSelectRangeSimulation_extendExistingRange() {
        // First range: b-c, then extend to b-e
        dataSource.selectKeys(List.of("b", "c"));
        Assertions.assertEquals(2, dataSource.getlLiveSelectedDataCount());

        dataSource.selectKeys(List.of("b", "c", "d", "e"));
        Assertions.assertEquals(4, dataSource.getlLiveSelectedDataCount());
        Assertions.assertTrue(dataSource.isSelected("b"));
        Assertions.assertTrue(dataSource.isSelected("e"));
    }
}
