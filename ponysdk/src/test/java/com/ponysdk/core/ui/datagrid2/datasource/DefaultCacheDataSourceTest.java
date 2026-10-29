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

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.controller.DefaultDataGridController.RenderingHelpersCache;
import com.ponysdk.core.ui.datagrid2.data.AbstractFilter;
import com.ponysdk.core.ui.datagrid2.data.DefaultRow;
import com.ponysdk.core.ui.datagrid2.data.FilterController;
import com.ponysdk.core.ui.datagrid2.data.Interval;
import com.ponysdk.core.ui.datagrid2.data.LiveDataView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests data insertion, update, removal, sorting and filtering of {@link DefaultCacheDataSource}.
 */
public class DefaultCacheDataSourceTest {

    private DefaultCacheDataSource<String, Item> source;
    private FilterController<Item> filterController;
    private RenderingHelpersCache<Item> renderingHelpers;

    @BeforeEach
    public void setUp() {
        source = new DefaultCacheDataSource<>();
        filterController = new FilterController<>();
        renderingHelpers = new RenderingHelpersCache<>();
        source.setFilterController(filterController);
        source.setRenderingHelpersCache(renderingHelpers);
        source.setAdapter(new ItemAdapter(true));
    }

    @Test
    public void setDataKeepsInsertionOrderAndReturnsAffectedInterval() {
        final Interval first = source.setData(new Item("a", 1));
        final Interval second = source.setData(new Item("b", 2));

        Assertions.assertEquals(0, first.from);
        Assertions.assertEquals(1, first.to);
        Assertions.assertEquals(1, second.from);
        Assertions.assertEquals(2, second.to);
        Assertions.assertEquals(List.of("a", "b"), ids(source.getFilteredData().getData()));
        Assertions.assertEquals(2, source.getRowCount());
        Assertions.assertEquals(2, source.getRows().size());
        Assertions.assertEquals("a", source.getRow("a").getData().id);
        Assertions.assertNull(source.getRow("zz"));
        Assertions.assertFalse(source.shouldBeCalledAsynchronously());
        Assertions.assertTrue(source.isSelectable("a"));
    }

    @Test
    public void theSortedRowListsRejectADuplicateAndMissAnAbsentRow() {
        final DefaultRow<Item> a = new DefaultRow<>(0, new Item("a", 1));
        final DefaultRow<Item> b = new DefaultRow<>(1, new Item("b", 2));
        final DefaultRow<Item> c = new DefaultRow<>(2, new Item("c", 3));
        final List<DefaultRow<Item>> rows = new ArrayList<>();
        source.insertRow(rows, a);
        source.insertRow(rows, b);
        source.insertRow(rows, c);

        Assertions.assertThrows(IllegalArgumentException.class, () -> source.insertRow(rows, b));
        Assertions.assertEquals(List.of(a, b, c), rows);

        final DefaultRow<Item> absent = new DefaultRow<>(3, new Item("d", 4));
        Assertions.assertEquals(-1, source.findRowIndex(rows, absent));
        Assertions.assertEquals(-1, source.removeRow(rows, absent));
        Assertions.assertEquals(1, source.removeRow(rows, b));
        Assertions.assertEquals(List.of(a, c), rows);
    }

    @Test
    public void descendingInsertionOrderPutsNewRowsFirst() {
        source = new DefaultCacheDataSource<>();
        source.setFilterController(new FilterController<>());
        source.setRenderingHelpersCache(renderingHelpers);
        source.setAdapter(new ItemAdapter(false));

        source.setData(new Item("a", 1));
        source.setData(new Item("b", 2));
        source.setData(new Item("c", 3));

        Assertions.assertEquals(List.of("c", "b", "a"), ids(source.getFilteredData().getData()));
    }

    @Test
    public void setDataWithSameInstanceIsNoOp() {
        final Item a = new Item("a", 1);
        source.setData(a);

        Assertions.assertNull(source.setData(a));
    }

    @Test
    public void setDataWithNewInstanceReplacesValue() {
        source.setData(new Item("a", 1));
        source.setData(new Item("b", 2));

        final Item newA = new Item("a", 5);
        final Interval interval = source.setData(newA);

        Assertions.assertNotNull(interval);
        Assertions.assertSame(newA, source.getRow("a").getData());
        Assertions.assertEquals(2, source.getRowCount());
    }

    @Test
    public void setDataWithNewInstanceEvictsRenderingHelpersOfReplacedValue() {
        final Item a = new Item("a", 1);
        source.setData(a);
        renderingHelpers.put(a, new Object[] { "x" });

        source.setData(new Item("a", 5));

        Assertions.assertFalse(renderingHelpers.containsKey(a));
    }

    @Test
    public void updateDataEvictsRenderingHelpersOfUpdatedValue() {
        final Item a = new Item("a", 1);
        source.setData(a);
        renderingHelpers.put(a, new Object[] { "x" });

        source.updateData("a", item -> item.qty = 2);

        Assertions.assertFalse(renderingHelpers.containsKey(a));
    }

    @Test
    public void setDataRejectsNull() {
        Assertions.assertThrows(NullPointerException.class, () -> source.setData(null));
    }

    @Test
    public void updateDataMovesRowAccordingToSortAndKeepsSelection() {
        source.addSort("qty", (r1, r2) -> Integer.compare(r1.getData().qty, r2.getData().qty));
        source.setData(new Item("a", 1));
        source.setData(new Item("b", 2));
        source.setData(new Item("c", 3));
        source.select("a");

        final Interval interval = source.updateData("a", item -> item.qty = 10);

        Assertions.assertEquals(0, interval.from);
        Assertions.assertEquals(3, interval.to);
        Assertions.assertEquals(List.of("b", "c", "a"), ids(source.getFilteredData().getData()));
        Assertions.assertTrue(source.isSelected("a"));
        Assertions.assertEquals(List.of("a"), ids(source.getLiveSelectedData().getData()));
    }

    @Test
    public void updateDataMovingRowUpReturnsIntervalFromNewIndex() {
        source.addSort("qty", (r1, r2) -> Integer.compare(r1.getData().qty, r2.getData().qty));
        source.setData(new Item("a", 1));
        source.setData(new Item("b", 2));
        source.setData(new Item("c", 3));

        final Interval interval = source.updateData("c", item -> item.qty = 0);

        Assertions.assertEquals(0, interval.from);
        Assertions.assertEquals(3, interval.to);
        Assertions.assertEquals(List.of("c", "a", "b"), ids(source.getFilteredData().getData()));
    }

    @Test
    public void updateDataOfUnknownKeyReturnsNull() {
        Assertions.assertNull(source.updateData("zz", item -> item.qty = 1));
    }

    @Test
    public void updateDataRejectedByFilterRemovesRowFromLiveData() {
        source.setData(new Item("a", 1));
        source.setData(new Item("b", 2));
        source.select("a");
        source.setFilter("positive", null, false, filter(item -> item.qty > 0, true));

        final Interval interval = source.updateData("a", item -> item.qty = -1);

        Assertions.assertEquals(0, interval.from);
        Assertions.assertEquals(2, interval.to);
        Assertions.assertEquals(List.of("b"), ids(source.getFilteredData().getData()));
        Assertions.assertEquals(0, source.getlLiveSelectedDataCount());
        Assertions.assertTrue(source.isSelected("a"));
        Assertions.assertFalse(source.getRow("a").isAccepted());
    }

    @Test
    public void updateOfRejectedRowReinsertsItWhenAcceptedAgain() {
        source.setFilter("positive", null, false, filter(item -> item.qty > 0, true));
        source.setData(new Item("a", -1));
        source.setData(new Item("b", 2));
        source.select("a");
        Assertions.assertEquals(List.of("b"), ids(source.getFilteredData().getData()));

        Assertions.assertNull(source.updateData("a", item -> item.qty = -2));

        final Interval interval = source.updateData("a", item -> item.qty = 1);

        Assertions.assertEquals(0, interval.from);
        Assertions.assertEquals(2, interval.to);
        Assertions.assertEquals(List.of("a", "b"), ids(source.getFilteredData().getData()));
        Assertions.assertEquals(List.of("a"), ids(source.getLiveSelectedData().getData()));
    }

    @Test
    public void setDataOnRejectedRowWithNewInstance() {
        source.setFilter("positive", null, false, filter(item -> item.qty > 0, true));
        source.setData(new Item("a", -1));

        Assertions.assertNull(source.setData(new Item("a", -3)));
        Assertions.assertNotNull(source.setData(new Item("a", 3)));
        Assertions.assertEquals(List.of("a"), ids(source.getFilteredData().getData()));
    }

    @Test
    public void removeDataRemovesSelectedRow() {
        source.setData(new Item("a", 1));
        source.setData(new Item("b", 2));
        source.select("a");

        final Item removed = source.removeData("a");

        Assertions.assertEquals("a", removed.id);
        Assertions.assertNull(source.getRow("a"));
        Assertions.assertFalse(source.isSelected("a"));
        Assertions.assertEquals(0, source.getlLiveSelectedDataCount());
        Assertions.assertEquals(List.of("b"), ids(source.getFilteredData().getData()));
    }

    @Test
    public void removeDataOfRejectedRowOnlyDropsItFromCache() {
        source.setFilter("positive", null, false, filter(item -> item.qty > 0, true));
        source.setData(new Item("a", -1));

        Assertions.assertEquals(-1, source.removeData("a").qty);
        Assertions.assertTrue(source.getRows().isEmpty());
    }

    @Test
    public void getRowsReturnsRequestedWindowClampedToRowCount() {
        for (int i = 0; i < 5; i++) {
            source.setData(new Item("i" + i, i));
        }

        final LiveDataView<Item> window = source.getRows(1, 2);
        Assertions.assertEquals(5, window.getAbsoluteRowCount());
        Assertions.assertEquals(List.of("i1", "i2"), rowIds(window.getLiveData()));
        Assertions.assertEquals(List.of("i1", "i2"), ids(source.getLastRequestedData().getData()));

        final LiveDataView<Item> clamped = source.getRows(3, 10);
        Assertions.assertEquals(List.of("i3", "i4"), rowIds(clamped.getLiveData()));
    }

    @Test
    public void addPrimarySortTakesPrecedenceOverExistingSorts() {
        source.setData(new Item("a", 2));
        source.setData(new Item("b", 1));
        source.setData(new Item("c", 2));
        source.addSort("qty", (r1, r2) -> Integer.compare(r1.getData().qty, r2.getData().qty));
        Assertions.assertEquals(List.of("b", "a", "c"), ids(source.getFilteredData().getData()));

        source.addPrimarySort("idDesc", (r1, r2) -> r2.getData().id.compareTo(r1.getData().id));

        Assertions.assertEquals(List.of("c", "b", "a"), ids(source.getFilteredData().getData()));
        final List<Object> keys = new ArrayList<>();
        source.getSortsEntry().forEach(e -> keys.add(e.getKey()));
        Assertions.assertEquals(List.of("idDesc", "qty"), keys);
    }

    @Test
    public void clearSortRestoresInsertionOrderAfterResort() {
        source.setData(new Item("a", 2));
        source.setData(new Item("b", 1));
        source.addSort("qty", (r1, r2) -> Integer.compare(r1.getData().qty, r2.getData().qty));

        Assertions.assertFalse(source.clearSort("unknown"));
        Assertions.assertTrue(source.clearSort("qty"));
        source.sort();

        Assertions.assertEquals(List.of("a", "b"), ids(source.getFilteredData().getData()));
    }

    @Test
    public void clearSortsResortsByInsertionOrder() {
        source.setData(new Item("a", 2));
        source.setData(new Item("b", 1));
        source.addSort("qty", (r1, r2) -> Integer.compare(r1.getData().qty, r2.getData().qty));
        source.selectAllLiveData();

        source.clearSorts();

        Assertions.assertEquals(List.of("a", "b"), ids(source.getFilteredData().getData()));
        Assertions.assertEquals(List.of("a", "b"), ids(source.getLiveSelectedData().getData()));
        Assertions.assertTrue(source.getSortsEntry().isEmpty());
    }

    @Test
    public void compareFallsBackToAdapterDefaultComparison() {
        source = new DefaultCacheDataSource<>();
        source.setFilterController(new FilterController<>());
        source.setRenderingHelpersCache(renderingHelpers);
        source.setAdapter(new ItemAdapter(true) {

            @Override
            public int compareDefault(final Item v1, final Item v2) {
                return v1.id.compareTo(v2.id);
            }
        });

        source.setData(new Item("c", 0));
        source.setData(new Item("a", 0));
        source.setData(new Item("b", 0));

        Assertions.assertEquals(List.of("a", "b", "c"), ids(source.getFilteredData().getData()));
    }

    @Test
    public void newFilterRejectsRowsAndUnselectsThemFromLiveSelection() {
        source.setData(new Item("a", 1));
        source.setData(new Item("b", -1));
        source.selectAllLiveData();

        source.setFilter("positive", "id", true, filter(item -> item.qty > 0, true));

        Assertions.assertEquals(List.of("a"), ids(source.getFilteredData().getData()));
        Assertions.assertEquals(List.of("a"), ids(source.getLiveSelectedData().getData()));
    }

    @Test
    public void replacingFilterWithoutReinforcingResetsLiveData() {
        source.setData(new Item("a", 1));
        source.setData(new Item("b", 5));
        source.select("b");
        source.setFilter("f", null, false, filter(item -> item.qty > 2, true));
        Assertions.assertEquals(List.of("b"), ids(source.getFilteredData().getData()));

        source.setFilter("f", null, false, filter(item -> item.qty < 2, true));

        Assertions.assertEquals(List.of("a"), ids(source.getFilteredData().getData()));
        Assertions.assertEquals(0, source.getlLiveSelectedDataCount());

        source.setFilter("f", null, false, filter(item -> true, true));
        Assertions.assertEquals(List.of("a", "b"), ids(source.getFilteredData().getData()));
        Assertions.assertEquals(List.of("b"), ids(source.getLiveSelectedData().getData()));
    }

    @Test
    public void inactiveFiltersAcceptEverything() {
        source.setFilter("f", null, false, filter(item -> false, false));

        source.setData(new Item("a", 1));

        Assertions.assertEquals(List.of("a"), ids(source.getFilteredData().getData()));
    }

    @Test
    public void filtersOfSameGroupAreCombinedWithOr() {
        filterController.registerFilterInGroup("g", "small");
        filterController.registerFilterInGroup("g", "big");
        source.setFilter("small", null, false, filter(item -> item.qty < 2, true));
        source.setFilter("big", null, false, filter(item -> item.qty > 8, true));
        source.setFilter("odd", null, false, filter(item -> item.qty % 2 == 1, true));

        for (int i = 0; i < 10; i++) {
            source.setData(new Item("i" + i, i));
        }

        Assertions.assertEquals(List.of("i1", "i9"), ids(source.getFilteredData().getData()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void clearFiltersRestoreRowsAfterReset() {
        final ColumnDefinition<Item> column = Mockito.mock(ColumnDefinition.class);
        source.setData(new Item("a", 1));
        source.setData(new Item("b", -1));
        source.setFilter("f", null, true, filter(item -> item.qty > 0, true));
        source.setFilter("onColumn", null, true, new ItemFilter(item -> item.qty > 0, true, column));

        Assertions.assertFalse(source.clearFilter("unknown"));
        Assertions.assertTrue(source.clearFilter("f"));
        source.resetLiveData();
        Assertions.assertEquals(List.of("a"), ids(source.getFilteredData().getData()));

        source.clearFilters(column);
        source.resetLiveData();
        Assertions.assertEquals(List.of("a", "b"), ids(source.getFilteredData().getData()));

        source.setFilter("f", null, true, filter(item -> item.qty > 0, true));
        source.clearFilters();
        source.resetLiveData();
        Assertions.assertEquals(List.of("a", "b"), ids(source.getFilteredData().getData()));
    }

    @Test
    public void selectionOfUnknownOrRejectedRowsReturnsNull() {
        source.setFilter("f", null, true, filter(item -> item.qty > 0, true));
        source.setData(new Item("a", 1));
        source.setData(new Item("b", -1));

        Assertions.assertNull(source.select("zz"));
        Assertions.assertNull(source.select("b"));
        Assertions.assertTrue(source.isSelected("b"));
        Assertions.assertNull(source.unselect("zz"));
        Assertions.assertNull(source.unselect("b"));
        Assertions.assertNotNull(source.select("a"));
        Assertions.assertNull(source.select("a"));
        final Interval unselected = source.unselect("a");
        Assertions.assertEquals(0, unselected.from);
        Assertions.assertEquals(1, unselected.to);
        Assertions.assertNull(source.unselect("a"));
    }

    @Test
    public void forEachVisitsEveryCachedValue() {
        source.setFilter("f", null, true, filter(item -> item.qty > 0, true));
        source.setData(new Item("a", 1));
        source.setData(new Item("b", -1));
        final Map<String, Integer> visited = new HashMap<>();

        source.forEach((k, v) -> visited.put(k, v.qty));

        Assertions.assertEquals(Map.of("a", 1, "b", -1), visited);
        Assertions.assertTrue(source.toString().contains("a"));
    }

    private static List<String> ids(final List<Item> items) {
        final List<String> ids = new ArrayList<>();
        items.forEach(i -> ids.add(i.id));
        return ids;
    }

    private static List<String> rowIds(final List<DefaultRow<Item>> rows) {
        final List<String> ids = new ArrayList<>();
        rows.forEach(r -> ids.add(r.getData().id));
        return ids;
    }

    private static AbstractFilter<Item> filter(final Predicate<Item> predicate, final boolean active) {
        return new ItemFilter(predicate, active, null);
    }

    static final class Item {

        final String id;
        int qty;

        Item(final String id, final int qty) {
            this.id = id;
            this.qty = qty;
        }

        @Override
        public String toString() {
            return id + ":" + qty;
        }
    }

    private static final class ItemFilter implements AbstractFilter<Item> {

        private final Predicate<Item> predicate;
        private final boolean active;
        private final ColumnDefinition<Item> column;

        ItemFilter(final Predicate<Item> predicate, final boolean active, final ColumnDefinition<Item> column) {
            this.predicate = predicate;
            this.active = active;
            this.column = column;
        }

        @Override
        public boolean test(final DefaultRow<Item> row) {
            return predicate.test(row.getData());
        }

        @Override
        public ColumnDefinition<Item> getColumnDefinition() {
            return column;
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }

    private static class ItemAdapter implements DataGridAdapter<String, Item> {

        private final boolean ascending;

        ItemAdapter(final boolean ascending) {
            this.ascending = ascending;
        }

        @Override
        public List<ColumnDefinition<Item>> getColumnDefinitions() {
            return List.of();
        }

        @Override
        public String getKey(final Item item) {
            return item.id;
        }

        @Override
        public boolean isAscendingSortByInsertionOrder() {
            return ascending;
        }

        @Override
        public int compareDefault(final Item v1, final Item v2) {
            return 0;
        }

        @Override
        public void onCreateHeaderRow(final IsPWidget rowWidget) {
        }

        @Override
        public void onCreateFooterRow(final IsPWidget rowWidget) {
        }

        @Override
        public void onCreateRow(final IsPWidget rowWidget) {
        }

        @Override
        public void onSelectRow(final IsPWidget rowWidget) {
        }

        @Override
        public void onUnselectRow(final IsPWidget rowWidget) {
        }

        @Override
        public void onCreateColumnResizer(final IsPWidget resizer) {
        }

        @Override
        public IsPWidget createLoadingDataWidget() {
            return null;
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
        public boolean isSelectionEnabled() {
            return true;
        }
    }

    @Test
    public void withoutFilterControllerEveryRowIsAccepted() {
        final DefaultCacheDataSource<String, Item> plain = new DefaultCacheDataSource<>();
        plain.setRenderingHelpersCache(renderingHelpers);
        plain.setAdapter(new ItemAdapter(true));
        plain.setData(new Item("a", 1));
        Assertions.assertEquals(1, plain.getRowCount());
    }

    @Test
    public void aReplacedSelectedValueStaysSelected() {
        source.setData(new Item("a", 1));
        source.setData(new Item("b", 2));
        source.select("a");
        source.setData(new Item("a", 5));
        source.setData(new Item("b", 6));
        Assertions.assertTrue(source.isSelected("a"));
        Assertions.assertFalse(source.isSelected("b"));
    }

    @Test
    public void aReinforcedFilterIsAppliedToEveryRow() {
        source.setData(new Item("a", 1));
        source.setData(new Item("b", 2));
        source.setFilter("f", null, false, filter(item -> item.qty > 0, true));
        source.setFilter("f", null, true, filter(item -> item.qty > 1, true));
        Assertions.assertEquals(List.of("b"), ids(source.getFilteredData().getData()));
    }
}
