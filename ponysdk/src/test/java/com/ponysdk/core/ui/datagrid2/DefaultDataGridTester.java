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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
class DefaultDataGridTester {

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
		void getRows() {
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
			assertNull(dataSource.updateData(15, this::updater));

			// Accepted, not selected
			controller.updateData(4, this::updater);
			assertEquals("updatedValue", controller.getData(4).getValue("a"));
			assertTrue(liveSelectedData.isEmpty() && selectedKeys.isEmpty());

			// Not Accepted, selected
			filterOddData();
			controller.select(5);
			controller.updateData(5, this::updater);
			assertEquals("updatedValue", controller.getData(5).getValue("a"));
			assertTrue(liveSelectedData.isEmpty());
			assertTrue(selectedKeys.contains(5));

			// Accepted, selected
			controller.select(6);
			controller.updateData(6, this::updater);
			assertEquals("updatedValue", controller.getData(6).getValue("a"));
			assertEquals(1, liveSelectedData.size());
			assertTrue(selectedKeys.contains(6));
		}

		private void updater(final MyRow myRow) {
			myRow.putValue("a", "updatedValue");
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

		@Test
		void addSort() {
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
		void clearSorts() {
			controller.clearSorts();
			final Set<Entry<Object, Comparator<DefaultRow<MyRow>>>> sortsEntry = dataSource.getSortsEntry();
			assertTrue(sortsEntry.isEmpty());
		}

		@Test
		void onSortReturnsSortedData() {
			controller.addSort(colDefs.get('a'), false);
			controller.addSort(colDefs.get('b'), true);
			controller.addSort(colDefs.get('a'), true);
			final ViewLiveData<MyRow> viewLiveData = dataSource.getRows(0, 8);
			for (int i = 0; i < 7; i++) {
				assertTrue(Math.abs(viewLiveData.liveData.get(i).getData().id
						- viewLiveData.liveData.get(i + 1).getData().id) == 1);
			}
		}
	}

	@Nested
	@DisplayName("Data Filtering")
	public class DataFiltering {

		@Test
		void addFilter() {
			int oldSize = 0;
			final Map<Object, AbstractFilter<MyRow>> filters = getFilters();
			oldSize = filters.size();
			controller.setFilter("TestDummyFilter", "", (row) -> {
				return false;
			}, true);
			assertTrue(filters.size() - oldSize == 1);
		}

		@Test
		void addNotNullFilter() {
			int oldSize = 0;
			final Map<Object, AbstractFilter<MyRow>> filters = getFilters();
			oldSize = filters.size();
			controller.setFilter("TestDummyFilter", "", (row) -> {
				return true;
			}, false);
			//Add the filter with same id
			controller.setFilter("TestDummyFilter", "", (row) -> {
				return false;
			}, false);
			assertTrue(filters.size() - oldSize == 1);
		}

		@Test
		void clearFilter() {
			final Map<Object, AbstractFilter<MyRow>> filters = getFilters();
			final int beforeAddFilter = filters.size();
			controller.setFilter("TestDummyFilter", "", (row) -> {
				return false;
			}, true);
			final int afterAddFilter = filters.size();
			controller.clearFilter("TestDummyFilter");
			final int afterClearFilter = filters.size();
			assertTrue(afterAddFilter - beforeAddFilter == 1);
			assertTrue(afterAddFilter - afterClearFilter == 1);
		}

		@Test
		void clearFilters() {
			final Map<Object, AbstractFilter<MyRow>> filters = getFilters();
			final int beforeAddFilter = filters.size();
			controller.setFilter("TestDummyFilter1", "", (row) -> {
				return false;
			}, true);
			controller.setFilter("TestDummyFilter2", "", (row) -> {
				return true;
			}, true);
			final int afterAddFilter = filters.size();
			controller.clearFilters();
			final int afterClearFilter = filters.size();
			assertTrue(afterAddFilter - beforeAddFilter == 2);
			assertTrue(afterAddFilter - afterClearFilter == 2);
		}

		@Test
		void clearFiltersColumnn() {
			controller.clearFilters(colDefs.get('a'));
		}

		@Test
		void onFilterReturnFilteredData() {
			filterOddData();
			final ViewLiveData<MyRow> viewLiveData = dataSource.getRows(0, 9);
			for (int i = 0; i < 4; i++) {
				assertTrue(Math.abs(viewLiveData.liveData.get(i).getData().id
						- viewLiveData.liveData.get(i + 1).getData().id) == 2);
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
			// final MyRow myRow = new MyRow(50);
			// myRow.map = null;
			// controller.setData(myRow);
			// final LinkedHashMap<Object, RowAction<V>> rowActions

			final LinkedHashMap<Object, RowAction<MyRow>> rowActions = getRowActions();
			final List<DefaultDataGridView<Integer, MyRow>.Row> rows = getRows();

			final Callable<Boolean> waitForRowsContent = () -> {
				boolean ready = false;
				while (ready) {
					for (final DefaultDataGridView<Integer, MyRow>.Row row : rows)
						if (row == null) {
							ready = false;
							break;
						} else ready = true;
				}
				return ready;
			};

			final ExecutorService executor = Executors.newSingleThreadExecutor();
			final Future<Boolean> future = executor.submit(waitForRowsContent);
			try {
				if (future.get(5, TimeUnit.SECONDS)) {
					gridView.addRowAction("OnOffRowAction", testRowAction);
					assertEquals(1, rowActions.size());
					assertTrue(verifyRowActionCoherence(rowActions, rows));
				}
			} catch (final Exception e) {
				e.printStackTrace();
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
			try {
				int i = 9;
				for (final DefaultDataGridView<Integer, MyRow>.Row row : rows) {
					i--;
					final PComplexPanel unpinnedRow;
					final Field privateField = DefaultDataGridView.Row.class.getDeclaredField("unpinnedRow");
					privateField.setAccessible(true);
					unpinnedRow = (PComplexPanel) privateField.get(row);
					if (testRowAction.testRow(row.getData(), i) != testRowAction.isActionApplied(unpinnedRow))
						return false;
				}
			} catch (final Exception e) {
				e.printStackTrace();
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
			if (row.getId() % 2 == 0) return true;
			return false;
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

}
