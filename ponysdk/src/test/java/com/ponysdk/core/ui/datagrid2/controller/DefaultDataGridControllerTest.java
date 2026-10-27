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
package com.ponysdk.core.ui.datagrid2.controller;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.cell.Cell;
import com.ponysdk.core.ui.datagrid2.column.Column;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnSort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.GeneralSort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.Sort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfigBuilder;
import com.ponysdk.core.ui.datagrid2.controller.DefaultDataGridController.RenderingHelpersCache;
import com.ponysdk.core.ui.datagrid2.data.DataGridFilter;
import com.ponysdk.core.ui.datagrid2.data.FilterController;
import com.ponysdk.core.ui.datagrid2.data.LiveDataView;
import com.ponysdk.core.ui.datagrid2.datasource.DefaultCacheDataSource;
import com.ponysdk.core.ui.datagrid2.view.DataGridSnapshot;
import com.ponysdk.core.util.Pair;
import com.ponysdk.test.PSuite;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Tests {@link DefaultDataGridController} on top of a real {@link DefaultCacheDataSource}, with a listener recording
 * the refresh and row update notifications.
 */
public class DefaultDataGridControllerTest extends PSuite {

    private static final long TIMEOUT_SECONDS = 5;

    private DefaultCacheDataSource<String, Item> source;
    private DefaultDataGridController<String, Item> controller;
    private DataGridControllerListener<Item> listener;
    private QtyColumn qtyColumn;
    private QtyColumn otherColumn;
    private TestAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        source = new DefaultCacheDataSource<>();
        source.setFilterController(new FilterController<>());
        controller = new DefaultDataGridController<>(source);
        listener = Mockito.mock(DataGridControllerListener.class);
        controller.setListener(listener);
        qtyColumn = new QtyColumn("qty");
        otherColumn = new QtyColumn("other");
        adapter = new TestAdapter(List.of(qtyColumn, otherColumn));
        controller.setAdapter(adapter);
    }

    @Test
    public void adapterCanOnlyBeSetOnce() {
        Assertions.assertThrows(IllegalStateException.class, () -> controller.setAdapter(adapter));
    }

    @Test
    public void operationsRequireAnAdapter() {
        final DefaultDataGridController<String, Item> noAdapter = new DefaultDataGridController<>(new DefaultCacheDataSource<>());

        Assertions.assertThrows(IllegalStateException.class, () -> noAdapter.addSort(qtyColumn, true));
        Assertions.assertThrows(IllegalStateException.class, () -> noAdapter.addSort("k", Comparator.comparing(i -> i.id)));
        Assertions.assertThrows(IllegalStateException.class, noAdapter::clearSorts);
        Assertions.assertThrows(IllegalStateException.class, noAdapter::clearFilters);
        Assertions.assertThrows(IllegalStateException.class, () -> noAdapter.clearFilter("k"));
        Assertions.assertThrows(IllegalStateException.class,
            () -> noAdapter.setConfig(new DataGridConfigBuilder<Item>().build()));
    }

    @Test
    public void unknownColumnIsRejected() {
        final QtyColumn unknown = new QtyColumn("unknown");

        Assertions.assertThrows(IllegalArgumentException.class, () -> controller.addSort(unknown, true));
        Assertions.assertThrows(IllegalArgumentException.class, () -> controller.clearRenderingHelpers(unknown));
    }

    @Test
    public void setDataNotifiesListenerAndExposesData() {
        controller.setData(new Item("a", 1));

        Mockito.verify(listener).refresh();
        Assertions.assertEquals(1, controller.getRowCount());
        Assertions.assertEquals(1, controller.getData("a").qty);
        Assertions.assertNull(controller.getData("zz"));
        Assertions.assertSame(controller, controller.get());
    }

    @Test
    public void setDataWithSameInstanceDoesNotRefresh() {
        final Item a = new Item("a", 1);
        controller.setData(a);
        Mockito.clearInvocations(listener);

        controller.setData(a);
        controller.setData(List.of(a));

        Mockito.verifyNoInteractions(listener);
    }

    @Test
    public void setDataCollectionRefreshesOnce() {
        controller.setData(List.of(new Item("a", 1), new Item("b", 2), new Item("c", 3)));

        Mockito.verify(listener, Mockito.times(1)).refresh();
        Assertions.assertEquals(List.of("a", "b", "c"), ids(controller.getFilteredData().getData()));
    }

    @Test
    public void unboundControllerDefersRefreshUntilBoundAgain() {
        controller.setBound(false);
        Assertions.assertFalse(controller.getBound());

        controller.setData(new Item("a", 1));
        controller.setData(new Item("b", 2));
        Mockito.verify(listener, Mockito.never()).refresh();

        controller.setBound(false);
        controller.setBound(true);

        Mockito.verify(listener, Mockito.times(1)).refresh();
        Assertions.assertTrue(controller.getBound());
    }

    @Test
    public void rebindingWithoutPendingChangeDoesNotRefresh() {
        controller.setBound(false);
        controller.setBound(true);

        Mockito.verify(listener, Mockito.never()).refresh();
    }

    @Test
    public void listenerIsOptional() {
        controller.setListener(null);

        controller.setData(new Item("a", 1));
        controller.select("a");

        Assertions.assertTrue(controller.isSelected("a"));
    }

    @Test
    public void renderCellComputesRenderingHelperOnce() {
        final Item a = new Item("a", 4);
        controller.setData(a);
        final RecordingCell cell = new RecordingCell();

        controller.renderCell(qtyColumn, cell, a);
        controller.renderCell(qtyColumn, cell, a);

        Assertions.assertEquals(List.of("a=4", "a=4"), cell.rendered);
        Assertions.assertEquals(1, qtyColumn.helperCalls.get());
    }

    @Test
    public void nullRenderingHelperIsCachedToo() {
        final Item a = new Item("a", 4);
        controller.setData(a);
        qtyColumn.nullHelper = true;
        final RecordingCell cell = new RecordingCell();

        controller.renderCell(qtyColumn, cell, a);
        controller.renderCell(qtyColumn, cell, a);

        Assertions.assertEquals(List.of("a=null", "a=null"), cell.rendered);
        Assertions.assertEquals(1, qtyColumn.helperCalls.get());
    }

    @Test
    public void clearRenderingHelpersForcesRecomputation() {
        final Item a = new Item("a", 4);
        controller.setData(a);
        final RecordingCell cell = new RecordingCell();
        controller.renderCell(qtyColumn, cell, a);

        controller.clearRenderingHelpers(qtyColumn);
        controller.renderCell(qtyColumn, cell, a);
        Assertions.assertEquals(2, qtyColumn.helperCalls.get());

        controller.clearRenderingHelper(qtyColumn, "a");
        controller.renderCell(qtyColumn, cell, a);
        Assertions.assertEquals(3, qtyColumn.helperCalls.get());

        controller.clearRenderingHelper(qtyColumn, "zz");
        controller.clearRenderingHelper(null, "a");
        controller.clearRenderingHelper(otherColumn, "a");
        controller.renderCell(qtyColumn, cell, a);
        Assertions.assertEquals(3, qtyColumn.helperCalls.get());
    }

    @Test
    public void clearRenderingHelperWithoutCachedEntryIsNoOp() {
        controller.setData(new Item("a", 4));

        controller.clearRenderingHelper(qtyColumn, "a");
        controller.clearRenderingHelpers(qtyColumn);

        Assertions.assertEquals(0, qtyColumn.helperCalls.get());
    }

    @Test
    public void updateDataRecomputesRenderingHelperAndRefreshes() {
        final Item a = new Item("a", 4);
        controller.setData(a);
        final RecordingCell cell = new RecordingCell();
        controller.renderCell(qtyColumn, cell, a);
        Mockito.clearInvocations(listener);

        controller.updateData("a", item -> item.qty = 9);
        controller.renderCell(qtyColumn, cell, a);

        Mockito.verify(listener).refresh();
        Assertions.assertEquals(List.of("a=4", "a=9"), cell.rendered);
    }

    @Test
    public void updateDataWithMapAppliesEveryUpdater() {
        controller.setData(List.of(new Item("a", 1), new Item("b", 2)));
        Mockito.clearInvocations(listener);
        final Map<String, Consumer<Item>> updaters = new HashMap<>();
        updaters.put("a", item -> item.qty = 10);
        updaters.put("b", item -> item.qty = 20);

        controller.updateData(updaters);

        Mockito.verify(listener, Mockito.times(1)).refresh();
        Assertions.assertEquals(10, controller.getData("a").qty);
        Assertions.assertEquals(20, controller.getData("b").qty);
    }

    @Test
    public void updateDataOfRejectedRowThatStaysRejectedDoesNotRefresh() {
        controller.setFilter("positive", qtyColumn, (item, helper) -> item.qty > 0, true, false);
        controller.setData(new Item("a", -1));
        Mockito.clearInvocations(listener);

        controller.updateData("a", item -> item.qty = -2);
        controller.updateData(Map.of("a", item -> item.qty = -3));

        Mockito.verify(listener, Mockito.never()).refresh();
        Assertions.assertEquals(-3, controller.getData("a").qty);
    }

    @Test
    public void updateDataOfUnknownKeyIsIgnored() {
        Assertions.assertDoesNotThrow(() -> controller.updateData("zz", item -> item.qty = 1));
        Assertions.assertDoesNotThrow(() -> controller.updateData(Map.of("zz", item -> item.qty = 1)));
        Mockito.verify(listener, Mockito.never()).refresh();
    }

    @Test
    public void removeDataReturnsRemovedValueAndRefreshes() {
        controller.setData(List.of(new Item("a", 1), new Item("b", 2)));
        Mockito.clearInvocations(listener);

        Assertions.assertEquals(1, controller.removeData("a").qty);
        Assertions.assertNull(controller.removeData("zz"));

        Mockito.verify(listener, Mockito.times(1)).refresh();
        Assertions.assertEquals(List.of("b"), ids(controller.getFilteredData().getData()));
    }

    @Test
    public void columnSortOrdersByRenderingHelper() {
        controller.setData(List.of(new Item("a", 2), new Item("b", 3), new Item("c", 1)));

        controller.addSort(qtyColumn, true);
        Assertions.assertEquals(List.of("c", "a", "b"), ids(controller.getFilteredData().getData()));

        controller.addSort(qtyColumn, false);
        Assertions.assertEquals(List.of("b", "a", "c"), ids(controller.getFilteredData().getData()));

        controller.clearSort(qtyColumn);
        Assertions.assertEquals(List.of("a", "b", "c"), ids(controller.getFilteredData().getData()));
    }

    @Test
    public void addingSameColumnSortTwiceKeepsOrder() {
        controller.setData(List.of(new Item("a", 2), new Item("b", 1)));
        controller.addSort(qtyColumn, true);
        controller.addSort(qtyColumn, true);

        Assertions.assertEquals(List.of("b", "a"), ids(controller.getFilteredData().getData()));
    }

    @Test
    public void clearingAbsentSortDoesNotRefresh() {
        controller.setData(new Item("a", 1));
        Mockito.clearInvocations(listener);

        controller.clearSort(qtyColumn);
        controller.clearSort("unknown");

        Mockito.verify(listener, Mockito.never()).refresh();
    }

    @Test
    public void generalSortsAndPrimarySort() {
        controller.setData(List.of(new Item("a", 2), new Item("b", 1), new Item("c", 2)));

        controller.addSort("qty", Comparator.comparingInt(i -> i.qty));
        Assertions.assertEquals(List.of("b", "a", "c"), ids(controller.getFilteredData().getData()));

        controller.addPrimarySort("idDesc", Comparator.comparing((Item i) -> i.id).reversed());
        Assertions.assertEquals(List.of("c", "b", "a"), ids(controller.getFilteredData().getData()));

        controller.clearSort("idDesc");
        Assertions.assertEquals(List.of("b", "a", "c"), ids(controller.getFilteredData().getData()));

        controller.clearSorts();
        Assertions.assertEquals(List.of("a", "b", "c"), ids(controller.getFilteredData().getData()));
    }

    @Test
    public void columnFilterUsesRenderingHelper() {
        controller.setData(List.of(new Item("a", 1), new Item("b", 5), new Item("c", 7)));
        Mockito.clearInvocations(listener);

        controller.setFilter("big", qtyColumn, (item, helper) -> (Integer) helper.get() > 4, true, false);

        Assertions.assertEquals(List.of("b", "c"), ids(controller.getFilteredData().getData()));
        Mockito.verify(listener).refresh();

        controller.clearFilter("big");
        Assertions.assertEquals(List.of("a", "b", "c"), ids(controller.getFilteredData().getData()));
    }

    @Test
    public void inactiveColumnFilterAcceptsEverything() {
        controller.setData(List.of(new Item("a", 1), new Item("b", 5)));

        controller.setFilter("none", qtyColumn, (item, helper) -> false, false, false);

        Assertions.assertEquals(List.of("a", "b"), ids(controller.getFilteredData().getData()));
    }

    @Test
    public void clearingUnknownFilterDoesNotRefresh() {
        controller.setData(new Item("a", 1));
        Mockito.clearInvocations(listener);

        controller.clearFilter("unknown");
        controller.clearFilters(List.of("unknown"));

        Mockito.verify(listener, Mockito.never()).refresh();
    }

    @Test
    public void clearFiltersByKeysOfColumnAndAll() {
        controller.setData(List.of(new Item("a", 1), new Item("b", 5), new Item("c", 7)));

        controller.setFilter("big", qtyColumn, (item, helper) -> item.qty > 4, true, false);
        controller.setFilter("odd", otherColumn, (item, helper) -> item.qty % 2 == 1, true, false);
        controller.clearFilters(List.of("big", "unknown"));
        Assertions.assertEquals(List.of("a", "b", "c"), ids(controller.getFilteredData().getData()));

        controller.setFilter("small", qtyColumn, (item, helper) -> item.qty < 6, true, false);
        Assertions.assertEquals(List.of("a", "b"), ids(controller.getFilteredData().getData()));
        controller.clearFilters(qtyColumn);
        Assertions.assertEquals(List.of("a", "b", "c"), ids(controller.getFilteredData().getData()));

        controller.setFilter("small", qtyColumn, (item, helper) -> item.qty < 6, true, false);
        controller.clearFilters();
        Assertions.assertEquals(List.of("a", "b", "c"), ids(controller.getFilteredData().getData()));
    }

    @Test
    public void generalFilters() {
        controller.setData(List.of(new Item("a", 1), new Item("b", 5), new Item("c", 7)));

        controller.setFilter(filter("big", item -> item.qty > 4, true));
        Assertions.assertEquals(List.of("b", "c"), ids(controller.getFilteredData().getData()));

        controller.setFilters(List.of(filter("big", item -> item.qty > 6, true), filter("small", item -> item.qty < 2, false)));
        Assertions.assertEquals(List.of("c"), ids(controller.getFilteredData().getData()));
    }

    @Test
    public void refreshWithSortingAndFilterReappliesFilters() {
        controller.setFilter(filter("big", item -> item.qty > 4, true));
        controller.setData(List.of(new Item("a", 5), new Item("b", 6)));
        controller.getData("a").qty = 1;

        controller.refreshWithSortingAndFilter();

        Assertions.assertEquals(List.of("b"), ids(controller.getFilteredData().getData()));
    }

    @Test
    public void selectionNotifiesRowUpdates() {
        controller.setData(List.of(new Item("a", 1), new Item("b", 2), new Item("c", 3)));

        controller.select("a");
        Mockito.verify(listener).onUpdateRows(0, 1);
        controller.select("zz");
        controller.selectKeys(List.of("b", "c"));
        Mockito.verify(listener).onUpdateRows(1, 3);
        controller.selectKeys(List.of("zz"));

        Assertions.assertEquals(3, controller.getLiveSelectedDataCount());
        Assertions.assertEquals(List.of("a", "b", "c"), ids(controller.getLiveSelectedData().getData()));

        controller.unselect("a");
        controller.unselect("a");
        controller.unselectKeys(List.of("b"));
        controller.unselectKeys(List.of("zz"));
        Assertions.assertEquals(List.of("c"), ids(controller.getLiveSelectedData().getData()));
        Mockito.verify(listener, Mockito.times(4)).onUpdateRows(Mockito.anyInt(), Mockito.anyInt());

        controller.selectAllLiveData();
        Assertions.assertEquals(3, controller.getLiveSelectedDataCount());
        controller.unselectAllData();
        Assertions.assertEquals(0, controller.getLiveSelectedDataCount());
    }

    @Test
    public void selectabilityDependsOnAdapterAndSource() {
        Assertions.assertTrue(controller.isSelectable("a"));

        adapter.selectionEnabled = false;

        Assertions.assertFalse(controller.isSelectable("a"));
    }

    @Test
    public void liveDataWindowAndLastRequestedData() {
        controller.setData(List.of(new Item("a", 1), new Item("b", 2), new Item("c", 3)));

        final Collection<Item> window = controller.getLiveData(1, 5);

        Assertions.assertEquals(List.of("b", "c"), ids(new ArrayList<>(window)));
        Assertions.assertEquals(List.of("b", "c"), ids(controller.getLastRequestedData().getData()));
    }

    @Test
    public void refreshNotificationsAreForwarded() {
        controller.setData(List.of(new Item("a", 1), new Item("b", 2)));
        Mockito.clearInvocations(listener);

        controller.refresh();
        controller.refreshOnNextDraw();
        controller.updateCurrentRows();

        Mockito.verify(listener, Mockito.times(2)).refresh();
        Mockito.verify(listener).onUpdateRows(0, 2);
    }

    @Test
    public void setConfigAppliesSortsAndEnrichConfigBuilderExportsThem() {
        controller.setData(List.of(new Item("a", 2), new Item("b", 3), new Item("c", 1)));
        final Comparator<Item> byId = Comparator.comparing(i -> i.id);
        final List<Sort<Item>> sorts = new ArrayList<>();
        sorts.add(null);
        sorts.add(new ColumnSort<>("unknown", true));
        sorts.add(new ColumnSort<>("qty", false));
        sorts.add(new GeneralSort<>("byId", byId));
        final DataGridConfigBuilder<Item> builder = new DataGridConfigBuilder<>();
        sorts.forEach(builder::addSort);

        controller.setConfig(builder.build());

        Assertions.assertEquals(List.of("b", "a", "c"), ids(controller.getFilteredData().getData()));

        final DataGridConfigBuilder<Item> export = new DataGridConfigBuilder<>();
        controller.enrichConfigBuilder(export);
        final DataGridConfig<Item> exported = export.build();
        Assertions.assertEquals(2, exported.getSorts().size());
        final ColumnSort<Item> columnSort = (ColumnSort<Item>) exported.getSorts().get(0);
        Assertions.assertEquals("qty", columnSort.getColumnId());
        Assertions.assertFalse(columnSort.isAsc());
        final GeneralSort<Item> generalSort = (GeneralSort<Item>) exported.getSorts().get(1);
        Assertions.assertEquals("byId", generalSort.getKey());
        Assertions.assertSame(byId, generalSort.getComparator());
    }

    @Test
    public void setConfigClearsFilters() {
        controller.setData(List.of(new Item("a", 1), new Item("b", 5)));
        controller.setFilter(filter("big", item -> item.qty > 4, true));

        controller.setConfig(new DataGridConfigBuilder<Item>().build());

        Assertions.assertEquals(List.of("a", "b"), ids(controller.getFilteredData().getData()));
    }

    @Test
    public void prepareLiveDataOnScreenDeliversRequestedWindow() throws Exception {
        controller.setData(List.of(new Item("a", 1), new Item("b", 2), new Item("c", 3)));
        final CompletableFuture<Pair<DefaultDataGridController<String, Item>.DataSrcResult, Throwable>> result = new CompletableFuture<>();

        controller.prepareLiveDataOnScreen(1, 2, snapshot(1, 2, 1), result::complete);

        final Pair<DefaultDataGridController<String, Item>.DataSrcResult, Throwable> pair = result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNull(pair.getSecond());
        final DefaultDataGridController<String, Item>.DataSrcResult dataSrcResult = pair.getFirst();
        Assertions.assertEquals(1, dataSrcResult.firstRowIndex);
        Assertions.assertEquals(1, dataSrcResult.start);
        Assertions.assertEquals(3, dataSrcResult.liveDataView.getAbsoluteRowCount());
        final List<Item> rows = new ArrayList<>();
        dataSrcResult.liveDataView.getLiveData().forEach(r -> rows.add(r.getData()));
        Assertions.assertEquals(List.of("b", "c"), ids(rows));
        Assertions.assertTrue(dataSrcResult.toString().startsWith("DataSrcResult [liveDataView="));
    }

    @Test
    public void prepareLiveDataOnScreenReportsSourceFailure() throws Exception {
        final DefaultDataGridController<String, Item> failing = new DefaultDataGridController<>(new FailingSource(false));
        failing.setAdapter(new TestAdapter(List.of(qtyColumn)));
        final CompletableFuture<Pair<DefaultDataGridController<String, Item>.DataSrcResult, Throwable>> result = new CompletableFuture<>();

        failing.prepareLiveDataOnScreen(0, 1, snapshot(0, 1, 0), result::complete);

        final Pair<DefaultDataGridController<String, Item>.DataSrcResult, Throwable> pair = result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNull(pair.getFirst());
        Assertions.assertEquals("boom", pair.getSecond().getMessage());
    }

    @Test
    public void asynchronousSourceDeliversWindow() throws Exception {
        final AsyncSource asyncSource = new AsyncSource();
        asyncSource.setFilterController(new FilterController<>());
        final DefaultDataGridController<String, Item> async = new DefaultDataGridController<>(asyncSource);
        async.setAdapter(new TestAdapter(List.of(qtyColumn)));
        async.setData(List.of(new Item("a", 1), new Item("b", 2)));
        final CompletableFuture<Pair<DefaultDataGridController<String, Item>.DataSrcResult, Throwable>> result = new CompletableFuture<>();

        async.prepareLiveDataOnScreen(0, 2, snapshot(0, 2, 0), result::complete);

        final Pair<DefaultDataGridController<String, Item>.DataSrcResult, Throwable> pair = result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNull(pair.getSecond());
        Assertions.assertEquals(2, pair.getFirst().liveDataView.getLiveData().size());
    }

    @Test
    public void asynchronousSourceReportsFailure() throws Exception {
        final DefaultDataGridController<String, Item> failing = new DefaultDataGridController<>(new FailingSource(true));
        failing.setAdapter(new TestAdapter(List.of(qtyColumn)));
        final CompletableFuture<Pair<DefaultDataGridController<String, Item>.DataSrcResult, Throwable>> result = new CompletableFuture<>();

        failing.prepareLiveDataOnScreen(0, 1, snapshot(0, 1, 0), result::complete);

        final Pair<DefaultDataGridController<String, Item>.DataSrcResult, Throwable> pair = result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNull(pair.getFirst());
        Assertions.assertNotNull(pair.getSecond());
    }

    @Test
    public void renderingHelpersCacheEvictsEldestBeyondCapacity() {
        final RenderingHelpersCache<Integer> cache = new RenderingHelpersCache<>();
        for (int i = 0; i < 513; i++) {
            cache.put(i, new Object[0]);
        }

        Assertions.assertEquals(512, cache.size());
        Assertions.assertFalse(cache.containsKey(0));
        Assertions.assertTrue(cache.containsKey(512));
    }

    @Test
    public void columnControllerSortEquality() {
        final Column<Item> column = new Column<>(0, qtyColumn);
        final DefaultDataGridController<String, Item>.ColumnControllerSort asc = controller.new ColumnControllerSort(column, true);
        final DefaultDataGridController<String, Item>.ColumnControllerSort sameAsc = controller.new ColumnControllerSort(column, true);
        final DefaultDataGridController<String, Item>.ColumnControllerSort desc = controller.new ColumnControllerSort(column, false);

        Assertions.assertTrue(asc.getSortAsc());
        Assertions.assertSame(column, asc.getColumn());
        Assertions.assertEquals(asc, asc);
        Assertions.assertEquals(asc, sameAsc);
        Assertions.assertEquals(asc.hashCode(), sameAsc.hashCode());
        Assertions.assertNotEquals(asc, desc);
        Assertions.assertNotEquals(asc, null);
        Assertions.assertNotEquals(asc, "asc");
    }

    private static DataGridSnapshot snapshot(final int firstRowIndex, final int size, final int start) {
        return new DataGridSnapshot(firstRowIndex, size, start, new HashMap<>(), new HashSet<>());
    }

    private static DataGridFilter<Item> filter(final String key, final java.util.function.Predicate<Item> predicate,
                                               final boolean active) {
        final DataGridFilter<Item> filter = new DataGridFilter<>();
        filter.setKey(key);
        filter.setId(key);
        filter.setPredicate(predicate);
        filter.setActive(active);
        return filter;
    }

    private static List<String> ids(final List<Item> items) {
        final List<String> ids = new ArrayList<>();
        items.forEach(i -> ids.add(i.id));
        return ids;
    }

    static final class Item {

        final String id;
        int qty;

        Item(final String id, final int qty) {
            this.id = id;
            this.qty = qty;
        }
    }

    private static final class RecordingCell implements Cell<Item, com.ponysdk.core.ui.datagrid2.cell.PrimaryCellController<Item>> {

        private final List<String> rendered = new ArrayList<>();

        @Override
        public void setController(final com.ponysdk.core.ui.datagrid2.cell.PrimaryCellController<Item> cellController) {
        }

        @Override
        public void render(final Item data, final Object renderingHelper) {
            rendered.add(data.id + "=" + renderingHelper);
        }

        @Override
        public void select() {
        }

        @Override
        public void unselect() {
        }

        @Override
        public com.ponysdk.core.ui.basic.PWidget asWidget() {
            return null;
        }
    }

    /** A column whose rendering helper is the item quantity, counting how often it is computed. */
    private static final class QtyColumn extends com.ponysdk.core.ui.datagrid2.column.DecoratorColumnDefinition<Item> {

        private final String id;
        private final AtomicInteger helperCalls = new AtomicInteger();
        private boolean nullHelper;

        @SuppressWarnings("unchecked")
        QtyColumn(final String id) {
            super(Mockito.mock(ColumnDefinition.class));
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Object getRenderingHelper(final Item data) {
            helperCalls.incrementAndGet();
            return nullHelper ? null : data.qty;
        }

        @Override
        public int compare(final Item v1, final Supplier<Object> h1, final Item v2, final Supplier<Object> h2) {
            return Integer.compare((Integer) h1.get(), (Integer) h2.get());
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private static final class AsyncSource extends DefaultCacheDataSource<String, Item> {

        @Override
        public boolean shouldBeCalledAsynchronously() {
            return true;
        }
    }

    private static final class FailingSource extends DefaultCacheDataSource<String, Item> {

        private final boolean async;

        FailingSource(final boolean async) {
            this.async = async;
            setFilterController(new FilterController<>());
        }

        @Override
        public LiveDataView<Item> getRows(final int dataSrcRowIndex, final int dataSize) {
            throw new IllegalStateException("boom");
        }

        @Override
        public boolean shouldBeCalledAsynchronously() {
            return async;
        }
    }

    static final class TestAdapter implements DataGridAdapter<String, Item> {

        private final List<ColumnDefinition<Item>> columns;
        private boolean selectionEnabled = true;

        TestAdapter(final List<ColumnDefinition<Item>> columns) {
            this.columns = columns;
        }

        @Override
        public List<ColumnDefinition<Item>> getColumnDefinitions() {
            return columns;
        }

        @Override
        public String getKey(final Item item) {
            return item.id;
        }

        @Override
        public boolean isAscendingSortByInsertionOrder() {
            return true;
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
            return selectionEnabled;
        }
    }
}
