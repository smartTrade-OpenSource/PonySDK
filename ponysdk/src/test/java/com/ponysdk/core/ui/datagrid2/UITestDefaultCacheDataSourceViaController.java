/*
 * Copyright (c) 2020 PonySDK
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
package com.ponysdk.core.ui.datagrid2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.column.DefaultColumnDefinition;
import com.ponysdk.core.ui.datagrid2.controller.DataGridController;
import com.ponysdk.core.ui.datagrid2.controller.DefaultDataGridController;
import com.ponysdk.core.ui.datagrid2.controller.DefaultDataGridController.RenderingHelpersCache;
import com.ponysdk.core.ui.datagrid2.data.AbstractFilter;
import com.ponysdk.core.ui.datagrid2.data.DefaultRow;
import com.ponysdk.core.ui.datagrid2.data.RowAction;
import com.ponysdk.core.ui.datagrid2.data.ViewLiveData;
import com.ponysdk.core.ui.datagrid2.datasource.AbstractDataSource;
import com.ponysdk.core.ui.datagrid2.datasource.DataGridSource;
import com.ponysdk.core.ui.datagrid2.datasource.DefaultCacheDataSource;
import com.ponysdk.core.ui.datagrid2.view.ColumnVisibilitySelectorDataGridView;
import com.ponysdk.core.ui.datagrid2.view.ConfigSelectorDataGridView;
import com.ponysdk.core.ui.datagrid2.view.DataGridView;
import com.ponysdk.core.ui.datagrid2.view.DefaultDataGridView;
import com.ponysdk.core.ui.datagrid2.view.RowSelectorColumnDataGridView;

/**
 * @author mabbas
 */
class UITestDefaultCacheDataSourceViaController {

	private Map<Character, DefaultColumnDefinition<MyRow>> colDefs;
	private DataGridView<Integer, MyRow> gridView;
	private DataGridController<Integer, MyRow> controller;
	private DataGridSource<Integer, MyRow> dataSource;
	private DataGridView<Integer, MyRow> simpleGridView;

	@BeforeAll
	static void setUpPony() {
		final WebSocket socket = mock(WebSocket.class);
		final TxnContext context = spy(new TxnContext(socket));
		final ApplicationConfiguration configuration = new ApplicationConfiguration();
		UIContext.setCurrent(spy(new UIContext(socket, context, configuration, null)));
	}

	@BeforeEach
	void beforeEach() {
		// This method initialize this class fields
		createDefaultDataGrid();
	}

	@Nested
	@DisplayName("Data Manipulation")
	public class DataManipulation {

		@Test
		void getData() {
			assertNotNull(controller.getData(5));
			assertEquals("a000000005", controller.getData(5).getValue("a"));
		}

		@Test
		@DisplayName("getRows(index, size)")
		void getRowsForSrc() {
			ViewLiveData<MyRow> viewLiveData = dataSource.getRows(0, 2);
			assertEquals(2, viewLiveData.liveData.size());
			viewLiveData = dataSource.getRows(4, 9);
			assertEquals(5, viewLiveData.liveData.size());
		}

		@Test
		void addData() {
			// Accepted
			MyRow myRow = createMyRow(9);
			controller.setData(myRow);
			assertNotNull(controller.getData(9));
			assertNull(dataSource.setData(myRow));

			// Not Accepted
			filterOddData();
			myRow = createMyRow(11);
			assertNull(dataSource.setData(myRow));
		}

		@Test
		void addDataCollection() {
			final List<DefaultRow<MyRow>> rows = getLiveData();
			final List<MyRow> dataCollection = List.of(createMyRow(9), createMyRow(10), createMyRow(11));
			controller.setData(dataCollection);
			assertEquals(9 + 3, rows.size());
		}

		@Test
		void updateData() {
			// Accepted, not selected
			final MyRow v = createMyRow(6);
			v.putValue("a", "updatedValue");
			controller.setData(v);
			assertEquals("updatedValue", controller.getData(6).getValue("a"));
			assertEquals("b000000006", controller.getData(6).getValue("b"));
		}

		@Test
		void updateSelectAcceptData() {
			filterOddData();
			final Collection<MyRow> liveSelectedData = controller.getLiveSelectedData();
			final Set<Integer> selectedKeys = getSelectedKeys();

			// Accepted, selected
			controller.select(6);
			final MyRow v2 = createMyRow(6);
			v2.putValue("a", "updatedValue");
			controller.setData(v2);
			assertEquals("updatedValue", controller.getData(6).getValue("a"));
			assertTrue(!liveSelectedData.isEmpty());
			assertTrue(!selectedKeys.isEmpty());
			for (final MyRow myRow : liveSelectedData) {
				assertEquals(myRow.id, 6);
				assertEquals(myRow.map.get("a"), "updatedValue");
				assertTrue(selectedKeys.contains(6));
			}

			// Not accepted, selected
			controller.select(5);
			final MyRow v1 = createMyRow(5);
			v1.putValue("a", "updatedValue");
			controller.setData(v1);
			assertEquals("updatedValue", controller.getData(5).getValue("a"));

			// Not accepted, not selected
			final MyRow v3 = createMyRow(5);
			v3.putValue("a", "updatedValue");
			controller.setData(v3);
			assertEquals("updatedValue", controller.getData(5).getValue("a"));

		}

		@Test
		void updateDataViaConsumer() {
			final Collection<MyRow> liveSelectedData = controller.getLiveSelectedData();
			final Set<Integer> selectedKeys = getSelectedKeys();

			// null
			assertNull(dataSource.updateData(15, this::updater1));

			// Accepted, not selected
			controller.updateData(4, this::updater1);
			assertEquals("updatedValue", controller.getData(4).getValue("a"));
			assertTrue(liveSelectedData.isEmpty() && selectedKeys.isEmpty());

			// Not Accepted, selected
			filterOddData();
			controller.select(5);
			controller.updateData(5, this::updater1);
			assertEquals("updatedValue", controller.getData(5).getValue("a"));
			assertTrue(liveSelectedData.isEmpty());
			assertTrue(selectedKeys.contains(5));

			// Accepted, selected
			controller.select(6);
			controller.updateData(6, this::updater1);
			assertEquals("updatedValue", controller.getData(6).getValue("a"));
			assertEquals(1, liveSelectedData.size());
			assertTrue(selectedKeys.contains(6));
		}

		@Test
		void updateDataViaConsumersMap() {
			final Map<Integer, Consumer<MyRow>> updaters = Map.of(1, this::updater1, 2, this::updater2);
			// Accepted, not selected
			controller.updateData(updaters);
			assertEquals("updatedValue", controller.getData(1).getValue("a"));
			assertEquals("updatedValue", controller.getData(2).getValue("b"));
			assertFalse("updatedValue".equals(controller.getData(3).getValue("b")));
		}

		private void updater1(final MyRow myRow) {
			myRow.putValue("a", "updatedValue");
		}

		private void updater2(final MyRow myRow) {
			myRow.putValue("b", "updatedValue");
		}

		@Test
		void forEach() {
			dataSource.forEach(this::action);
		}

		private void action(final Integer key, final MyRow value) {
			assertTrue(key != null && value != null);
		}

		@Test
		void updateNotAcceptedData() {
			filterOddData();
			final MyRow v = createMyRow(6);
			v.putValue("a", "updatedValue");
			controller.setData(v);
			assertEquals("updatedValue", controller.getData(6).getValue("a"));
			assertEquals("b000000006", controller.getData(6).getValue("b"));
		}

		@Test
		void removeData() {
			final Collection<MyRow> liveSelectedData = controller.getLiveSelectedData();
			final Set<Integer> selectedKeys = getSelectedKeys();

			// Non existing
			assertEquals(null, controller.removeData(15));

			// Accepted, not selected
			assertEquals(3, controller.removeData(3).getId());
			assertNull(controller.getData(3));
			assertEquals(8, controller.getRowCount());
			assertTrue(selectedKeys.isEmpty());

			// Accepted, selected
			controller.select(5);
			assertEquals(5, controller.removeData(5).getId());
			assertNull(controller.getData(5));
			assertTrue(liveSelectedData.isEmpty());
			assertEquals(7, controller.getRowCount());
			assertTrue(selectedKeys.isEmpty());

			// Not Accepted, not selected
			filterOddData();
			assertEquals(1, controller.removeData(1).getId());
			assertNull(controller.getData(1));
			assertEquals(5, controller.getRowCount());
			assertTrue(selectedKeys.isEmpty());

			// Not Accepted, selected
			controller.select(7);
			assertEquals(7, controller.removeData(7).getId());
			assertNull(controller.getData(7));
			assertTrue(liveSelectedData.isEmpty());
			assertEquals(5, controller.getRowCount());
			assertTrue(selectedKeys.isEmpty());
		}

		@Test
		void getAllRows() {
			assertEquals(9, dataSource.getRows().size());
		}

		@Test
		void getRowCount() {
			assertEquals(9, dataSource.getRowCount());
		}
	}

	@Nested
	@DisplayName("Data selection")
	public class DataSelection {

		@Test
		void selectData() {
			final Collection<MyRow> liveSelectedData = controller.getLiveSelectedData();

			controller.select(5);
			assertTrue(!liveSelectedData.isEmpty());
			for (final MyRow myRow : liveSelectedData) {
				assertTrue(myRow.id == 5);
			}
		}

		@Test
		void unselectData() {
			controller.select(5);
			controller.select(6);
			controller.unselect(6);
			final Collection<MyRow> liveSelectedData = controller.getLiveSelectedData();
			assertTrue(!liveSelectedData.isEmpty());
			for (final MyRow myRow : liveSelectedData) {
				assertTrue(myRow.id == 5);
			}
		}

		@Test
		void selectAllData() {
			controller.selectAllLiveData();
			final Collection<MyRow> liveSelectedData = controller.getLiveSelectedData();
			final int rowCount = dataSource.getRowCount();
			assertTrue(rowCount == liveSelectedData.size());
		}

		@Test
		void unselectAllData() {
			controller.select(5);
			controller.select(6);
			controller.unselectAllData();
			final Collection<MyRow> liveSelectedData = controller.getLiveSelectedData();
			assertTrue(liveSelectedData.size() == 0);
		}
	}

	@Nested
	@DisplayName("Data Sorting")
	public class DataSorting {

		final Comparator<MyRow> comparator = (row1, row2) -> {
			final int id1 = row1.id;
			final int id2 = row2.id;
			if (id1 > id2) return -1;
			else if (id1 < id2) return 1;
			else return 0;
		};

		@Test
		void addColDefSort() {
			controller.addSort(colDefs.get('a'), false);
			final Set<Entry<Object, Comparator<DefaultRow<MyRow>>>> sortsEntry = dataSource.getSortsEntry();
			for (final Entry<Object, Comparator<DefaultRow<MyRow>>> entry : sortsEntry) {
				if (entry.getValue() instanceof DefaultDataGridController.ColumnControllerSort) {
					final DefaultDataGridController<Integer, MyRow>.ColumnControllerSort sort = (DefaultDataGridController<Integer, MyRow>.ColumnControllerSort) entry
						.getValue();
					assertEquals("a", sort.getcolumn().getColDef().getId());
				}
			}
		}

		@Test
		void addComparatorSort() {
			controller.addSort(colDefs.get('a'), comparator);
			final Set<Entry<Object, Comparator<DefaultRow<MyRow>>>> sortsEntry = dataSource.getSortsEntry();
			for (final Entry<Object, Comparator<DefaultRow<MyRow>>> entry : sortsEntry) {
				if (entry.getValue() instanceof DefaultDataGridController.ColumnControllerSort) {
					final DefaultDataGridController<Integer, MyRow>.ColumnControllerSort sort = (DefaultDataGridController<Integer, MyRow>.ColumnControllerSort) entry
						.getValue();
					assertEquals("a", sort.getcolumn().getColDef().getId());
				}
			}
		}

		@Test
		void clearSort() {
			controller.clearSort(colDefs.get('a'));
			final Set<Entry<Object, Comparator<DefaultRow<MyRow>>>> sortsEntry = dataSource.getSortsEntry();
			for (final Entry<Object, Comparator<DefaultRow<MyRow>>> entry : sortsEntry) {
				if (entry.getValue() instanceof DefaultDataGridController.ColumnControllerSort) {
					final DefaultDataGridController<Integer, MyRow>.ColumnControllerSort sort = (DefaultDataGridController<Integer, MyRow>.ColumnControllerSort) entry
						.getValue();
					assertNotEquals("a", sort.getcolumn().getColDef().getId());
				}
			}
		}

		@Test
		void clearSortByObjectKey() {
			final Set<Entry<Object, Comparator<DefaultRow<MyRow>>>> sortsEntry = dataSource.getSortsEntry();
			controller.addSort("key", comparator);
			assertEquals(1, sortsEntry.size());
			controller.clearSort("key");
			assertEquals(0, sortsEntry.size());
		}

		@Test
		void clearSorts() {
			controller.clearSorts();
			final Set<Entry<Object, Comparator<DefaultRow<MyRow>>>> sortsEntry = dataSource.getSortsEntry();
			assertTrue(sortsEntry.isEmpty());
		}

		@Test
		void onColSortReturnsSortedData() {
			controller.addSort(colDefs.get('a'), false);
			controller.addSort(colDefs.get('b'), true);
			controller.addSort(colDefs.get('a'), true);
			final ViewLiveData<MyRow> viewLiveData = dataSource.getRows(0, 9);
			for (int i = 0; i < 8; i++) {
				assertTrue(Math.abs(viewLiveData.liveData.get(i).getData().id
						- viewLiveData.liveData.get(i + 1).getData().id) == 1);
			}
		}

		@Test
		void onComparatorSortReturnsSortedData() {
			controller.addSort(colDefs.get('a'), comparator);
			final ViewLiveData<MyRow> viewLiveData = dataSource.getRows(0, 9);
			for (int i = 0; i < 8; i++) {
				assertTrue(Math.abs(viewLiveData.liveData.get(i).getData().id
						- viewLiveData.liveData.get(i + 1).getData().id) == 1);
			}
		}
	}

	@Nested
	@DisplayName("Data Filtering")
	public class DataFiltering {

		@Test
		void addGeneralFilter() {
			int oldSize = 0;
			final Map<Object, AbstractFilter<MyRow>> filters = getFilters();
			oldSize = filters.size();
			controller.setFilter("TestDummyGeneralFilter", "", (row) -> {
				return false;
			}, true);
			assertTrue(filters.size() - oldSize == 1);
		}

		@Test
		void addColumnFilter() {
			int oldSize = 0;
			final Map<Object, AbstractFilter<MyRow>> filters = getFilters();
			oldSize = filters.size();
			controller.setFilter("TestDummyColumnFilter", colDefs.get('a'), (row, supplier) -> {
				return false;
			}, true);
			assertTrue(filters.size() - oldSize == 1);
		}

		@Test
		void addSimilarIDGeneralFilter() {
			final Map<Object, AbstractFilter<MyRow>> filters = getFilters();
			controller.setFilter("TestDummyGeneralFilter", "", (row) -> {
				return true;
			}, false);
			// Add a filter with the same id
			controller.setFilter("TestDummyGeneralFilter", "", (row) -> {
				return false;
			}, false);
			assertEquals(1, filters.size());
		}

		@Test
		void addSimilarIDColumnFilter() {
			final Map<Object, AbstractFilter<MyRow>> filters = getFilters();
			controller.setFilter("TestDummyColumnFilter", colDefs.get('a'), (row, supplier) -> {
				return false;
			}, true);
			controller.setFilter("TestDummyColumnFilter", colDefs.get('a'), (row, supplier) -> {
				return true;
			}, true);
			assertEquals(1, filters.size());
		}

		@Test
		void clearFilterByID() {
			final Map<Object, AbstractFilter<MyRow>> filters = getFilters();
			controller.setFilter("TestDummyGeneralFilter", "", (row) -> {
				return false;
			}, true);
			controller.setFilter("TestDummyColumnFilter", colDefs.get('a'), (row, supplier) -> {
				return false;
			}, true);
			assertEquals(2, filters.size());
			controller.clearFilter("TestDummyGeneralFilter");
			assertNull(filters.get("TestDummyGeneralFilter"));
			assertEquals(1, filters.size());
			controller.clearFilter("TestDummyColumnFilter");
			assertNull(filters.get("TestDummyColumnFilter"));
			assertEquals(0, filters.size());
		}

		@Test
		void clearGeneralFiltersByColDef() {
			final Map<Object, AbstractFilter<MyRow>> filters = getFilters();
			controller.setFilter("TestDummyGeneralFilter", "", (row) -> {
				return true;
			}, true);
			assertEquals(1, filters.size());
			// This filter has no associated colDef so nothing msut be cleared
			controller.clearFilters(colDefs.get('a'));
			assertEquals(1, filters.size());
		}

		@Test
		void clearColumnFiltersByColDef() {
			final Map<Object, AbstractFilter<MyRow>> filters = getFilters();
			controller.setFilter("TestDummyColumnFilter", colDefs.get('a'), (row, supplier) -> {
				return false;
			}, true);
			assertEquals(1, filters.size());
			controller.clearFilters(colDefs.get('a'));
			assertEquals(0, filters.size());
		}

		@Test
		void clearFilters() {
			final Map<Object, AbstractFilter<MyRow>> filters = getFilters();
			controller.setFilter("TestDummyFilter1", "", (row) -> {
				return false;
			}, true);
			controller.setFilter("TestDummyColumnFilter", colDefs.get('a'), (row, supplier) -> {
				return false;
			}, true);
			assertEquals(2, filters.size());
			controller.clearFilters();
			assertEquals(0, filters.size());
		}

		@Test
		void onGeneralFilterReturnFilteredData() {
			filterOddData();
			final ViewLiveData<MyRow> viewLiveData = dataSource.getRows(0, 9);
			for (int i = 0; i < 4; i++) {
				assertTrue(Math.abs(viewLiveData.liveData.get(i).getData().id
						- viewLiveData.liveData.get(i + 1).getData().id) == 2);
			}
		}

		@Test
		void onColumnFilterReturnFilteredData() {
			controller.setFilter("ColumnFilterColumnA", colDefs.get('a'), (row, supplier) -> {
				return row.getId() % 2 == 0 ? true : false;
			}, true);
			final ViewLiveData<MyRow> viewLiveData = dataSource.getRows(0, 9);
			for (int i = 0; i < 4; i++) {
				final int currentRowId = viewLiveData.liveData.get(i).getData().getId();
				final int nextRowId = viewLiveData.liveData.get(i + 1).getData().getId();
				assertTrue(Math.abs(currentRowId - nextRowId) == 2);
				assertTrue(currentRowId % 2 == 0);
			}
		}
	}

	@Nested
	@DisplayName("Row actions")
	public class RowActionAdder {

		RowAction<MyRow> testRowAction = new RowAction<>() {

			@Override
			public boolean testRow(final MyRow t, final int index) {
				return (index & 1) == 0;
			}

			@Override
			public void cancel(final IsPWidget row) {
				row.asWidget().removeStyleName("unpair-row");
			}

			@Override
			public void apply(final IsPWidget row) {
				row.asWidget().addStyleName("unpair-row");
			}

			@Override
			public boolean isActionApplied(final IsPWidget row) {
				return row.asWidget().hasStyleName("unpair-row");
			}
		};

		@Test
		void addRowAction() {
			final LinkedHashMap<Object, RowAction<MyRow>> rowActions = getRowActions();
			final List<DefaultDataGridView<Integer, MyRow>.Row> rows = getRows();
			if (isReady(waitForListContent(rows, 9))) {
				gridView.addRowAction("OnOffRowAction", testRowAction);
				assertEquals(1, rowActions.size());
				assertTrue(verifyRowActionCoherence(rowActions, rows));
			}
		}

		@Test
		void clearRowAction() {
			gridView.addRowAction("OnOffRowAction", testRowAction);
			gridView.clearRowAction("OnOffRowAction");
			final LinkedHashMap<Object, RowAction<MyRow>> rowActions = getRowActions();
			final List<DefaultDataGridView<Integer, MyRow>.Row> rows = getRows();
			assertEquals(0, rowActions.size());
			assertTrue(verifyRowActionCanceled(rowActions, rows));
		}

		private boolean verifyRowActionCoherence(final LinkedHashMap<Object, RowAction<MyRow>> rowActionsContainer,
				final List<DefaultDataGridView<Integer, MyRow>.Row> rows) {
			for (int i = 8; i >= 0; i--) {
				final PComplexPanel unpinnedRow = getUnpinnedRow(rows.get(i));
				if (testRowAction.testRow(rows.get(i).getData(), i) != testRowAction.isActionApplied(unpinnedRow))
					return false;
			}
			return true;
		}

		private boolean verifyRowActionCanceled(final LinkedHashMap<Object, RowAction<MyRow>> rowActionsContainer,
				final List<DefaultDataGridView<Integer, MyRow>.Row> rows) {
			try {
				for (final DefaultDataGridView<Integer, MyRow>.Row row : rows) {
					final PComplexPanel unpinnedRow;
					final Field privateField = DefaultDataGridView.Row.class.getDeclaredField("unpinnedRow");
					privateField.setAccessible(true);
					unpinnedRow = (PComplexPanel) privateField.get(row);
					if (testRowAction.isActionApplied(unpinnedRow)) return false;
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
			return true;
		}
	}

	@Nested
	@DisplayName("RenderingHelpersCache")
	public class RenderingHelperCache {

		@Test
		void RenderingHelperInitialization() {
			final RenderingHelpersCache<MyRow> renderingHelpersCache = getRenderingHelpersCache();
			final List<DefaultDataGridView<Integer, MyRow>.Row> rows = getRows();
			if (isReady(waitForListContent(rows, 9))) {
				assertEquals(9, renderingHelpersCache.size());
			}
		}

		@Test
		void RenderingHelperClear() {
			final RenderingHelpersCache<MyRow> renderingHelpersCache = getRenderingHelpersCache();
			final List<DefaultDataGridView<Integer, MyRow>.Row> rows = getRows();
			// clearRenderingHelpers(..) method is called by the update method
			if (isReady(waitForListContent(rows, 9))) {
				assertEquals(9, renderingHelpersCache.size());
				controller.updateData(4, (x) -> {
				});
				controller.updateData(5, (x) -> {
				});
				assertEquals(7, renderingHelpersCache.size());
			}
		}

		// @Test
		// void RenderingHelperAddData() {
		// final RenderingHelpersCache<MyRow> renderingHelpersCache =
		// getRenderingHelpersCache();
		// final List<DefaultDataGridView<Integer, MyRow>.Row> rows = getRows();
		// controller.setData(createMyRow(9));
		// if (isReady(waitForListContent(rows, 10))) {
		// assertEquals(10, renderingHelpersCache.size());
		// }
		// }

		@Test
		void RenderingHelperClearData() {
			final RenderingHelpersCache<MyRow> renderingHelpersCache = getRenderingHelpersCache();
			final List<DefaultDataGridView<Integer, MyRow>.Row> rows = getRows();
			if (isReady(waitForListContent(rows, 9))) {
				assertEquals(9, renderingHelpersCache.size());
			}
			controller.removeData(8);
			controller.removeData(7);
			if (isReady(waitForListContent(rows, 7))) {
				assertEquals(7, renderingHelpersCache.size());
			}
		}

		@RepeatedTest(20)
		@Test
		void RenderingHelperClearColDef() {
			final RenderingHelpersCache<MyRow> renderingHelpersCache = getRenderingHelpersCache();
			final List<DefaultDataGridView<Integer, MyRow>.Row> rows = getRows();
			if (isReady(waitForMapContent(renderingHelpersCache, 9))) {
				assertEquals(9, renderingHelpersCache.size());
			}
			controller.clearRenderingHelpers(colDefs.get('a'));
			for (final Object[] o : renderingHelpersCache.values()) {
				assertNull(o[1]);
				assertEquals('b', ((String) o[2]).charAt(0));
			}
		}
	}

	// FIXME
	@Nested
	@DisplayName("Configuration")
	public class Configuration {

	}

	private void createDefaultDataGrid() {
		colDefs = new HashMap<>();
		dataSource = new DefaultCacheDataSource<>();
		simpleGridView = new DefaultDataGridView<>(dataSource);
		final ColumnVisibilitySelectorDataGridView<Integer, MyRow> columnVisibilitySelectorDataGridView = new ColumnVisibilitySelectorDataGridView<>(
			simpleGridView);
		final RowSelectorColumnDataGridView<Integer, MyRow> rowSelectorColumnDataGridView = new RowSelectorColumnDataGridView<>(
			columnVisibilitySelectorDataGridView);
		final ConfigSelectorDataGridView<Integer, MyRow> configSelectorDataGridView = new ConfigSelectorDataGridView<>(
			rowSelectorColumnDataGridView, "DEFAULT");
		gridView = configSelectorDataGridView;
		gridView.setAdapter(new DataGridAdapter<Integer, MyRow>() {

			private final List<ColumnDefinition<MyRow>> columns = new ArrayList<>();

			{
				for (char c = 'a'; c <= 'b'; c++) {
					final String ss = c + "";
					final DefaultColumnDefinition<MyRow> colDef = new DefaultColumnDefinition<>(ss, v -> v.getValue(ss),
						(v, s) -> {
							v.putValue(ss, s);
						});
					colDefs.put(c, colDef);
					columns.add(colDef);
				}
			}

			@Override
			public void onUnselectRow(final IsPWidget rowWidget) {
				rowWidget.asWidget().removeStyleName("selected-row");
			}

			@Override
			public void onSelectRow(final IsPWidget rowWidget) {
				rowWidget.asWidget().addStyleName("selected-row");
			}

			@Override
			public boolean isAscendingSortByInsertionOrder() {
				return false;
			}

			@Override
			public Integer getKey(final MyRow v) {
				return v.id;
			}

			@Override
			public List<ColumnDefinition<MyRow>> getColumnDefinitions() {
				return columns;
			}

			@Override
			public int compareDefault(final MyRow v1, final MyRow v2) {
				return 0;
			}

			@Override
			public void onCreateHeaderRow(final IsPWidget rowWidget) {
				rowWidget.asWidget().getParent().asWidget().setStyleProperty("background", "aliceblue");
			}

			@Override
			public void onCreateFooterRow(final IsPWidget rowWidget) {
				rowWidget.asWidget().getParent().asWidget().setStyleProperty("background", "aliceblue");
			}

			@Override
			public void onCreateRow(final IsPWidget rowWidget) {
			}

			@Override
			public boolean hasHeader() {
				return true;
			}

			@Override
			public boolean hasFooter() {
				return false;
			}

			@Override
			public IsPWidget createLoadingDataWidget() {
				final PComplexPanel div = Element.newDiv();
				div.setWidth("100%");
				div.setHeight("100%");
				div.setStyleProperty("background-color", "#FFFFFF7F");
				return div;
			}

			@Override
			public void onCreateColumnResizer(final IsPWidget resizer) {
			}
		});

		gridView.setPollingDelayMillis(250L);
		controller = ((DefaultDataGridView<Integer, MyRow>) simpleGridView).getController();
		gridView.asWidget().setHeight("950px");
		gridView.asWidget().setWidth("1900px");
		gridView.asWidget().setStyleProperty("resize", "both");
		gridView.asWidget().setStyleProperty("overflow", "hidden");

		PWindow.getMain().add(gridView);
		PWindow.getMain().add(configSelectorDataGridView.getDecoratorWidget());

		controller.setBound(false);
		for (int i = 0; i < 9; i++) {
			controller.setData(createMyRow(i));
		}
		controller.setBound(true);
	}

	private static class MyRow {

		private final int id;
		private Map<String, String> map;
		private final String format;

		public MyRow(final int id) {
			super();
			this.id = id;
			format = String.format("%09d", id);
		}

		public int getId() {
			return id;
		}

		public void putValue(final String key, final String value) {
			if (map == null) map = new ConcurrentHashMap<>();
			map.put(key, value);
		}

		public String getValue(final String key) {
			if (map != null) {
				final String v = map.get(key);
				if (v != null) return v;
			}
			return key + format;
		}

		@Override
		public MyRow clone() {
			final MyRow model = new MyRow(id);
			if (map == null) return model;
			model.map = new HashMap<>(map);
			return model;
		}

		@Override
		public String toString() {
			return "SampleModel [id=" + id + "]";
		}
	}

	private static MyRow createMyRow(final int index) {
		return new MyRow(index);
	}

	private void filterOddData() {
		controller.setFilter(0, "", (row) -> {
			return row.getId() % 2 == 0 ? true : false;
		}, true);
	}

	// private Field accessPrivateField(final Class<?> cls, final String
	// varName) {
	// final cls var = cls.getDeclaredConstructor().newInstance();
	// final Field field = AbstractDataSource.class.getDeclaredField(varName);
	// field.setAccessible(true);
	// var = (cls) field.get(gridView);
	// return var;
	// }

	private Set<Integer> getSelectedKeys() {
		Set<Integer> selectedKeys = new HashSet<>();
		try {
			final Field privateField = AbstractDataSource.class.getDeclaredField("selectedKeys");
			privateField.setAccessible(true);
			selectedKeys = (Set<Integer>) privateField.get(dataSource);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return selectedKeys;
	}

	private Map<Object, AbstractFilter<MyRow>> getFilters() {
		Map<Object, AbstractFilter<MyRow>> filtersContainer = new HashMap<>();
		try {
			final Field filters = AbstractDataSource.class.getDeclaredField("filters");
			filters.setAccessible(true);
			filtersContainer = (Map<Object, AbstractFilter<MyRow>>) filters.get(dataSource);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return filtersContainer;
	}

	private LinkedHashMap<Object, RowAction<MyRow>> getRowActions() {
		LinkedHashMap<Object, RowAction<MyRow>> rowActionsContainer = new LinkedHashMap<>();
		try {
			final Field privateField = DefaultDataGridView.class.getDeclaredField("rowActions");
			privateField.setAccessible(true);
			rowActionsContainer = (LinkedHashMap<Object, RowAction<MyRow>>) privateField.get(simpleGridView);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return rowActionsContainer;
	}

	private List<DefaultDataGridView<Integer, MyRow>.Row> getRows() {
		List<DefaultDataGridView<Integer, MyRow>.Row> rows = new ArrayList<>();
		try {
			final Field privateField = DefaultDataGridView.class.getDeclaredField("rows");
			privateField.setAccessible(true);
			rows = (List<DefaultDataGridView<Integer, MyRow>.Row>) privateField.get(simpleGridView);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return rows;
	}

	private List<DefaultRow<MyRow>> getLiveData() {
		List<DefaultRow<MyRow>> liveData = new ArrayList<>();
		try {
			final Field privateField = DefaultCacheDataSource.class.getDeclaredField("liveData");
			privateField.setAccessible(true);
			liveData = (List<DefaultRow<MyRow>>) privateField.get(dataSource);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return liveData;
	}

	private PComplexPanel getUnpinnedRow(final DefaultDataGridView.Row row) {
		PComplexPanel unpinnedRow = new PComplexPanel() {

			@Override
			protected WidgetType getWidgetType() {
				return null;
			}
		};

		try {
			final Field privateField = DefaultDataGridView.Row.class.getDeclaredField("unpinnedRow");
			privateField.setAccessible(true);
			unpinnedRow = (PComplexPanel) privateField.get(row);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return unpinnedRow;
	}

	private RenderingHelpersCache<MyRow> getRenderingHelpersCache() {
		RenderingHelpersCache<MyRow> renderingHelpersCache = new RenderingHelpersCache<>();
		try {
			final Field privateField = AbstractDataSource.class.getDeclaredField("renderingHelpersCache");
			privateField.setAccessible(true);
			renderingHelpersCache = (RenderingHelpersCache<MyRow>) privateField.get(dataSource);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return renderingHelpersCache;
	}

	@SuppressWarnings("finally")
	private int getViewRowKey(final List<DefaultDataGridView<Integer, MyRow>.Row> list, final int i) {
		int key = -1;
		try {
			final Field privateField = DefaultDataGridView.Row.class.getDeclaredField("key");
			privateField.setAccessible(true);
			key = (int) privateField.get(list.get(i));
		} catch (final Exception e) {
			// Do nothing since we expect here null pointer
		} finally {
			return key;
		}
	}

	private Future<Boolean> waitForListContent(final List<DefaultDataGridView<Integer, MyRow>.Row> list,
			final int size) {
		final Callable<Boolean> waitForListContent = () -> {
			final boolean ready = false;
			outerloop: while (!ready) {
				for (int i = 0; i < size; i++) {
					if (getViewRowKey(list, i) < 0) {
						continue outerloop;
					}
				}
				return true;
			}
		};

		final ExecutorService executor = Executors.newSingleThreadExecutor();
		return executor.submit(waitForListContent);
	}

	private Future<Boolean> waitForMapContent(final RenderingHelpersCache<MyRow> cache, final int size) {
		final Callable<Boolean> waitForMapContent = () -> {
			final boolean ready = false;
			outerloop: while (!ready) {
				if (cache.size() == 9) {
					for (final Object[] o : cache.values()) {
						if (o[1] == null || o[2] == null || size != cache.size()) {
							continue outerloop;
						}
					}
					return true;
				} else {
					LockSupport.parkNanos(10);
					continue;
				}
			}
		};

		final ExecutorService executor = Executors.newSingleThreadExecutor();
		return executor.submit(waitForMapContent);
	}

	private boolean isReady(final Future<Boolean> future) {
		try {
			if (future.get(5, TimeUnit.SECONDS)) {
				return true;
			}
			return false;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
