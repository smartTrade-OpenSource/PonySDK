/*
 * Copyright (c) 2019 PonySDK
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.server.concurrent.PScheduler.UIRunnable;
import com.ponysdk.core.server.service.query.PResultSet;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.cell.Cell;
import com.ponysdk.core.ui.datagrid2.cell.ExtendedCell;
import com.ponysdk.core.ui.datagrid2.cell.ExtendedCellController;
import com.ponysdk.core.ui.datagrid2.cell.PrimaryCell;
import com.ponysdk.core.ui.datagrid2.cell.PrimaryCellController;
import com.ponysdk.core.ui.datagrid2.column.ColumnActionListener;
import com.ponysdk.core.ui.datagrid2.column.ColumnController;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition.State;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnSort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.GeneralSort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.Sort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfigBuilder;
import com.ponysdk.core.ui.datagrid2.controller.DataGridController;
import com.ponysdk.core.ui.datagrid2.controller.DataGridControllerListener;
import com.ponysdk.core.ui.datagrid2.controller.DataGridControllerWrapper;
import com.ponysdk.core.ui.datagrid2.controller.DefaultDataGridController;
import com.ponysdk.core.ui.datagrid2.data.DataGridFilter;
import com.ponysdk.core.ui.datagrid2.data.DefaultRow;
import com.ponysdk.core.ui.datagrid2.data.LiveDataView;
import com.ponysdk.core.ui.datagrid2.data.RowAction;
import com.ponysdk.core.ui.datagrid2.datasource.DataGridSource;
import com.ponysdk.core.ui.datagrid2.datasource.DefaultCacheDataSource;
import com.ponysdk.core.util.Pair;
import com.ponysdk.core.util.SetUtils;

/**
 * @author mbagdouri
 */
public final class DefaultDataGridView<K, V> implements DataGridView<K, V>, DataGridControllerListener<V> {

    public static final String ROW_HOVER_LINK = "row-hover-link";

    // Addon
    private static final String ADDON_ROW_KEY = "row";
    private static final String ADDON_ROW_COUNT_KEY = "rc";
    private static final String ADDON_COLUMN_ID = "col";
    private static final String ADDON_COLUMN_WIDTH = "cw";
    private static final String ADDON_COLUMN_VISIBILITY = "cv";
    private static final String ADDON_COLUMN_TO = "to";

    // HTML Attributes
    /**
     * Set on a row widget when it is selected
     */
    public static final String SELECTED_ATTRIBUTE = "pony-selected";
    /**
     * Set on a cell (header, body, and footer) widget when its column is pinned
     */
    public static final String PINNED_ATTRIBUTE = "pony-pinned";
    /**
     * Set on a cell widget of the body of the grid when it is in pending state
     */
    public static final String PENDING_ATTRIBUTE = "pony-pending";
    /**
     * Set on a cell widget of the body of the grid when it is in extended mode
     */
    public static final String EXTENDED_ATTRIBUTE = "pony-extended";

    private static final Logger log = LoggerFactory.getLogger(DefaultDataGridView.class);
    // State
    private static final int MIN_RELATIVE_ROW_COUNT = 30;
    private int columnViewSequence = 0;
    private final PComplexPanel root = Element.newDiv();
    private final PComplexPanel loadingDataDiv;
    private final PComplexPanel errorMsgDiv;
    private final PinnedTable pinnedTable;
    private final UnpinnedTable unpinnedTable;
    private Addon addon;
    private final List<Row> rows = new ArrayList<>();
    private final DataGridController<K, V> controller;
    private DataGridAdapter<K, V> adapter;
    private long pollingDelayMillis;
    private UIRunnable delayedDrawRunnable;
    private int from = Integer.MAX_VALUE;
    private int to = 0;
    private final Set<DrawListener> drawListeners = SetUtils.newArraySet();
    private final Map<ColumnDefinition<V>, ColumnView> columnViews = new HashMap<>();
    private final DataGridControllerWrapper<K, V> controllerWrapper;
    private final LinkedHashMap<Object, RowAction<V>> rowActions = new LinkedHashMap<>();
    private int firstRowIndex;
    private final Map<Integer, Integer> sorts = new HashMap<>();
    private final Set<Integer> filters = new HashSet<>();
    private Function<Throwable, String> exceptionHandler;

    private boolean shouldDraw = true;
    private boolean refreshOnColumnVisibilityChanged = true;
    private boolean drawOnResume;
    private boolean forceExtended;

    public DefaultDataGridView() {
        this(new DefaultCacheDataSource<>());
    }

    public DefaultDataGridView(final DataGridSource<K, V> dataSource) {

        new HideScrollBarAddon(root);
        controller = new DefaultDataGridController<>(dataSource);
        controller.setListener(this);
        controllerWrapper = new DataGridControllerWrapper<>(//
            controller.get(), //
            this::onDataUpdated, //
            this::onDataRemoved);

        root.addStyleName("pony-grid");
        root.setStyleProperty("display", "flex");
        root.setStyleProperty("flex-direction", "column");
        root.setStyleProperty("position", "relative");

        final PComplexPanel headerPinnedDiv = Element.newDiv();
        final PComplexPanel headerUnpinnedDiv = Element.newDiv();
        final PComplexPanel headerDiv = prepareHeaderDiv(headerPinnedDiv, headerUnpinnedDiv);
        root.add(headerDiv);

        final PComplexPanel bodyPinnedDiv = Element.newDiv();
        final PComplexPanel bodyUnpinnedDiv = Element.newDiv();
        final PComplexPanel subBodyDiv = prepareSubBodyDiv(bodyPinnedDiv, bodyUnpinnedDiv);
        loadingDataDiv = prepareLoadingDataDiv();
        errorMsgDiv = prepareErrorMsgDiv();

        final PComplexPanel bodyDiv = prepareBodyDiv(subBodyDiv);
        root.add(bodyDiv);

        final PComplexPanel footerPinnedDiv = Element.newDiv();
        final PComplexPanel footerUnpinnedDiv = Element.newDiv();
        final PComplexPanel footerDiv = prepareFooterDiv(footerPinnedDiv, footerUnpinnedDiv);
        root.add(footerDiv);

        pinnedTable = new PinnedTable(headerPinnedDiv, bodyPinnedDiv, footerPinnedDiv);
        unpinnedTable = new UnpinnedTable(headerUnpinnedDiv, bodyUnpinnedDiv, footerUnpinnedDiv);

        root.addDestroyListener(e -> onDestroy());
    }

    private void onDestroy() {
        if (delayedDrawRunnable != null) {
            delayedDrawRunnable.cancel();
        }
        if (addon != null) {
            addon.destroy();
        }
    }

    @Override
    public DataGridAdapter<K, V> getAdapter() {
        return adapter;
    }

    private PComplexPanel prepareBodyDiv(final PComplexPanel subBodyDiv) {
        final PComplexPanel bodyDiv = Element.newDiv();
        bodyDiv.setStyleProperty("flex", "auto");
        bodyDiv.setStyleProperty("overflow-x", "hidden");
        bodyDiv.addStyleName("pony-grid-body");
        bodyDiv.add(subBodyDiv);
        bodyDiv.add(loadingDataDiv);
        bodyDiv.add(errorMsgDiv);
        return bodyDiv;
    }

    private PComplexPanel prepareSubBodyDiv(final PComplexPanel bodyPinnedDiv, final PComplexPanel bodyUnpinnedDiv) {
        final PComplexPanel subBodyDiv = Element.newDiv();
        subBodyDiv.setStyleProperty("display", "flex");
        subBodyDiv.addStyleName("pony-grid-sub-body");
        subBodyDiv.add(bodyPinnedDiv);
        subBodyDiv.add(bodyUnpinnedDiv);
        return subBodyDiv;
    }

    private static PComplexPanel prepareFooterDiv(final PComplexPanel footerPinnedDiv, final PComplexPanel footerUnpinnedDiv) {
        final PComplexPanel footerDiv = Element.newDiv();
        footerDiv.addStyleName("pony-grid-footer");
        footerDiv.setStyleProperty("flex", "initial");
        footerDiv.setStyleProperty("display", "flex");
        footerDiv.add(footerPinnedDiv);
        footerDiv.add(footerUnpinnedDiv);
        return footerDiv;
    }

    private static PComplexPanel prepareHeaderDiv(final PComplexPanel headerPinnedDiv, final PComplexPanel headerUnpinnedDiv) {
        final PComplexPanel headerDiv = Element.newDiv();
        headerDiv.setStyleProperty("flex", "initial");
        headerDiv.setStyleProperty("display", "flex");
        headerDiv.addStyleName("pony-grid-header");
        headerDiv.add(headerPinnedDiv);
        headerDiv.add(headerUnpinnedDiv);
        return headerDiv;
    }

    private static PComplexPanel prepareLoadingDataDiv() {
        final PComplexPanel loadingDataDiv = Element.newDiv();
        loadingDataDiv.addStyleName("pony-grid-loading-data");
        return loadingDataDiv;
    }

    private static PComplexPanel prepareErrorMsgDiv() {
        final PComplexPanel result = Element.newDiv();
        result.addStyleName("pony-grid-error-msg");
        result.setStyleProperty("position", "absolute");
        result.setStyleProperty("top", "0px");
        result.setStyleProperty("left", "0px");
        result.setStyleProperty("width", "100%");
        result.setStyleProperty("height", "100%");
        return result;
    }

    @Override
    public PWidget asWidget() {
        return root;
    }

    @Override
    public DataGridController<K, V> getController() {
        return controllerWrapper;
    }

    @Override
    public void scrollTo(final int index) {
        addon.scrollTo(index, controller.getRowCount());
    }

    private void onScroll(final int row) {
        hideErrorMessage();
        firstRowIndex = row;
        refresh();
    }

    private void onRelativeRowCountUpdated(int relRowCount) {
        if (relRowCount == 0) {
            hideLoadingDataView();
            return;
        }
        final int mod = relRowCount % 3;
        relRowCount = Math.max(mod == 0 ? relRowCount : relRowCount - mod + 3, MIN_RELATIVE_ROW_COUNT);
        if (this.rows.size() == relRowCount) {
            hideLoadingDataView();
            return;
        }
        hideErrorMessage();
        if (this.rows.size() < relRowCount) {
            for (int i = this.rows.size(); i < relRowCount; i++) {
                rows.add(new Row(i));
            }
        } else do {
            rows.get(rows.size() - 1).destroy();
        } while (rows.size() > relRowCount);
        refresh();
        addon.checkPosition();
    }

    private void onColumnVisibilityChanged(final JsonArray columns, final JsonArray visiblity) {
        boolean changed = false;
        for (int i = 0; i < columns.size(); i++) {
            final int column = columns.getInt(i);
            final boolean visible = visiblity.getBoolean(i);
            final ColumnDefinition<V> c = adapter.getColumnDefinitions().get(column);
            final ColumnView columnView = getColumnView(c);
            changed = changed || columnView.visible != visible;
            columnView.visible = visible;
        }
        if (changed) {
            onUpdateRows(0, controller.getRowCount());
        }
    }

    private void onColumnResized(final int column, final int width) {
        final ColumnDefinition<V> c = adapter.getColumnDefinitions().get(column);
        if (!c.isResizable()) return;
        final ColumnView columnView = getColumnView(c);
        if (columnView.width == width) return;
        final int oldWidth = columnView.width;
        columnView.width = Math.min(Math.max(width, columnView.column.getMinWidth()), columnView.column.getMaxWidth());
        if (columnView.width == oldWidth) return;
        int index = pinnedTable.columns.indexOf(columnView);
        final String widthAsString = columnView.getWidthAsString();
        if (index >= 0) {
            pinnedTable.width += columnView.width - oldWidth;
            pinnedTable.refreshWidth();
            pinnedTable.header.getWidget(index).setWidth(widthAsString);
            final String rowWidth = pinnedTable.width + "px";
            for (final Row row : rows) {
                row.pinnedRow.getWidget(index).setWidth(widthAsString);
                row.pinnedRow.setWidth(rowWidth);
            }
            pinnedTable.footer.getWidget(index).setWidth(widthAsString);
        } else {
            unpinnedTable.width += columnView.width - oldWidth;
            unpinnedTable.refreshWidth();
            index = unpinnedTable.columns.indexOf(columnView);
            unpinnedTable.header.getWidget(index).setWidth(widthAsString);
            final String rowWidth = unpinnedTable.width + "px";
            for (final Row row : rows) {
                row.unpinnedRow.getWidget(index).setWidth(widthAsString);
                row.unpinnedRow.setWidth(rowWidth);
            }
            unpinnedTable.footer.getWidget(index).setWidth(widthAsString);
        }
        columnView.notifyResizedListeners();
    }

    private void onColumnMoved(final int from, final int to) {
        if (from == to) return;
        final ColumnDefinition<V> fromCol = adapter.getColumnDefinitions().get(from);
        if (fromCol.getDraggableHeaderElement() == null) return;
        final ColumnView fromColumnView = getColumnView(fromCol);
        final int fromIndex = unpinnedTable.columns.indexOf(fromColumnView);
        if (fromIndex < 0) return;
        final ColumnDefinition<V> toCol = adapter.getColumnDefinitions().get(to);
        if (toCol.getDraggableHeaderElement() == null) return;
        final ColumnView toColumnView = getColumnView(toCol);
        final int toIndex = unpinnedTable.columns.indexOf(toColumnView);
        if (toIndex < 0) return;
        unpinnedTable.columns.remove(fromIndex);
        unpinnedTable.columns.add(toIndex, fromColumnView);
        final PWidget header = unpinnedTable.header.getWidget(fromIndex);
        unpinnedTable.header.remove(fromIndex);
        unpinnedTable.header.insert(header, toIndex);
        final PWidget footer = unpinnedTable.footer.getWidget(fromIndex);
        unpinnedTable.footer.remove(fromIndex);
        unpinnedTable.footer.insert(footer, toIndex);
        for (final Row row : rows) {
            row.unpinnedCells.add(toIndex, row.unpinnedCells.remove(fromIndex));
            final PWidget widget = row.unpinnedRow.getWidget(fromIndex);
            row.unpinnedRow.remove(fromIndex);
            row.unpinnedRow.insert(widget, toIndex);
        }
        fromColumnView.notifyMovedListeners();
    }

    @Override
    public void refresh() {
        hideErrorMessage();
        updateInterval(0, this.controller.getRowCount());
        draw();
    }

    private void updateInterval(final int from, final int to) {
        if (from > to) return;
        this.from = Math.min(this.from, from);
        this.to = Math.max(this.to, to);
    }

    @Override
    public void onUpdateRows(final int from, final int to) {
        updateInterval(from, to);

        try {
            for (int i = from; i < to && i < rows.size(); i++) {
                updateRow(rows.get(i), rows.get(i).getData());
            }
        } finally {
            for (final DrawListener drawListener : drawListeners) {
                drawListener.onDraw(rows.size());
            }
        }
    }

    @Override
    public int getLiveDataRowCount() {
        return rows.size();
    }

    private void draw() {
        try {
            if (!shouldDraw) {
                drawOnResume = true;
                return;
            }
            if (from > to) {
                hideLoadingDataView();
                return;
            }
            final int size = unpinnedTable.body.getWidgetCount();
            final int start = Math.max(0, from - firstRowIndex);
            final DataGridSnapshot viewStateSnapshot = new DataGridSnapshot(firstRowIndex, size, start, sorts, filters);
            final Consumer<Pair<DefaultDataGridController<K, V>.DataSrcResult, Throwable>> consumer = PScheduler
                .delegate(this::updateView);
            controller.prepareLiveDataOnScreen(firstRowIndex, size, viewStateSnapshot, consumer);
        } catch (final Exception e) {
            log.error("Cannot draw data from data source", e);
        }
    }

    private void updateView(final Pair<DefaultDataGridController<K, V>.DataSrcResult, Throwable> result) {
        if (result.getSecond() != null) {
            showErrorMessage(exceptionHandler == null ? "" : exceptionHandler.apply(result.getSecond()));
        } else {
            final LiveDataView<V> resultLiveData = result.getFirst().liveDataView;
            try {
                for (int i = result.getFirst().start; i < rows.size(); i++) {
                    updateRow(rows.get(i), resultLiveData);
                }
                addon.onDataUpdated(resultLiveData.getAbsoluteRowCount(), rows.size(), result.getFirst().firstRowIndex);
            } catch (final Exception e) {
                log.error("Problem occured while updating the view for result {}, row nb {}", result.getFirst(), rows.size(), e);
                showErrorMessage(exceptionHandler == null ? "" : exceptionHandler.apply(result.getSecond()));
            } finally {
                from = Integer.MAX_VALUE;
                to = 0;
                for (final DrawListener drawListener : drawListeners) {
                    drawListener.onDraw(resultLiveData.getAbsoluteRowCount());
                }
                hideLoadingDataView();
            }
        }
    }

    private void updateRow(final Row row, final V rowData) {
        if (rowData == null) {
            row.hide();
            row.key = null;
            row.data = null;
            log.debug("No data for row {}, actual row size is {}", row, rows.size());
            return;
        }

        final boolean dataHasChanged = row.getData() != rowData;
        final boolean mustUpdateRowHeight = (row.extended || !row.isShown()) && rowData != row.getData();

        if (dataHasChanged) {
            row.setData(rowData);
            row.key = adapter.getKey(rowData);
        }
        row.show();
        final boolean selected = controller.isSelected(row.key);

        updateRowCells(row, row.unpinnedCells, unpinnedTable.columns, selected);
        updateRowCells(row, row.pinnedCells, pinnedTable.columns, selected);

        if (dataHasChanged) {
            if (row.extended || forceExtended) {
                addon.updateExtendedRowHeight(row.relativeIndex);
            } else if (mustUpdateRowHeight) {
                addon.updateRowHeight(row.relativeIndex);
            }
        }

        if (selected) {
            adapter.onSelectRow(row.unpinnedRow);
            row.unpinnedRow.setAttribute(SELECTED_ATTRIBUTE);
            adapter.onSelectRow(row.pinnedRow);
            row.pinnedRow.setAttribute(SELECTED_ATTRIBUTE);
        } else {
            adapter.onUnselectRow(row.unpinnedRow);
            row.unpinnedRow.removeAttribute(SELECTED_ATTRIBUTE);
            adapter.onUnselectRow(row.pinnedRow);
            row.pinnedRow.removeAttribute(SELECTED_ATTRIBUTE);
        }

        applyRowActions(row, rowData);
    }

    private void updateRow(final Row row, final LiveDataView<V> result) {
        if (row.getAbsoluteIndex() >= result.getAbsoluteRowCount()) {
            row.hide();
            row.key = null;
            row.data = null;
            return;
        }
        updateRow(row, getRowData(row.getRelativeIndex(), result));
    }

    private V getRowData(final int rowIndex, final LiveDataView<V> result) {
        checkAdapter();
        final List<DefaultRow<V>> liveData = result.getLiveData();
        return rowIndex < liveData.size() ? liveData.get(rowIndex).getData() : null;
    }

    private void applyRowActions(final Row row, final V rowData) {
        for (final RowAction<V> rowAction : rowActions.values()) {
            if (rowAction.testRow(rowData, row.getAbsoluteIndex())) {
                rowAction.apply(row.pinnedRow);
                rowAction.apply(row.unpinnedRow);
            } else {
                rowAction.cancel(row.pinnedRow);
                rowAction.cancel(row.unpinnedRow);
            }
        }
    }

    private void updateRowCells(final Row row, final List<CellManager<V>> cells, final List<ColumnView> columns,
                                final boolean selected) {
        for (int c = 0; c < cells.size(); c++) {
            final CellManager<V> cell = cells.get(c);
            final ColumnView column = columns.get(c);
            updateRowCell(row, cell, column, selected);
        }
    }

    private void updateRowCell(final Row row, final CellManager<V> cell, final ColumnView columnView, final boolean selected) {
        cell.setVisibility(columnView.visible);
        if (columnView.visible) {
            if (cell.extendedMode) row.extended = true;
            controller.renderCell(columnView.column, cell.getCell(), row.data);
        }
        if (selected) cell.select();
        else cell.unselect();
    }

    @Override
    public void setAdapter(final DataGridAdapter<K, V> adapter) {
        if (this.adapter != null) throw new IllegalStateException("DataGridAdapter is already set");
        controller.setAdapter(adapter);
        this.adapter = adapter;
        if (adapter.hasHeader()) {
            adapter.onCreateHeaderRow(pinnedTable.header);
            adapter.onCreateHeaderRow(unpinnedTable.header);
        } else {
            pinnedTable.header.setHeight("0px");
            unpinnedTable.header.setHeight("0px");
        }
        if (adapter.hasFooter()) {
            adapter.onCreateFooterRow(pinnedTable.footer);
            adapter.onCreateFooterRow(unpinnedTable.footer);
        }
        loadingDataDiv.add(adapter.createLoadingDataWidget());
        for (final ColumnDefinition<V> column : adapter.getColumnDefinitions()) {
            final ColumnView columnView = columnViews.computeIfAbsent(column, ColumnView::new);
            column.setController(new SimpleColumnController(columnView));
            addColumnActionListener(column, column);
            if (column.getDefaultState().isShown()) {
                final Table table = column.getDefaultState().isPinned() ? pinnedTable : unpinnedTable;
                table.addColumn(getColumnView(column));
            }
        }
        pinnedTable.refreshWidth();
        unpinnedTable.refreshWidth();
        for (int i = 0; i < MIN_RELATIVE_ROW_COUNT; i++) {
            rows.add(new Row(i));
        }
        addon = new Addon();
        addon.setListenOnColumnVisibility(refreshOnColumnVisibilityChanged);
        for (final ColumnView columnView : columnViews.values()) {
            addon.onColumnAdded(columnView.id, columnView.column.getMinWidth(), columnView.column.getMaxWidth(),
                columnView.state.isPinned());
        }
    }

    private void pin(final ColumnView columnView) {
        final int index = unpinnedTable.columns.indexOf(columnView);
        unpinnedTable.columns.remove(index);
        unpinnedTable.width -= columnView.width;
        pinnedTable.columns.add(columnView);
        pinnedTable.width += columnView.width;
        {
            final PWidget widget = unpinnedTable.header.getWidget(index);
            unpinnedTable.header.remove(index);
            pinnedTable.header.add(widget);
            widget.setAttribute(PINNED_ATTRIBUTE);
        }
        {
            final PWidget widget = unpinnedTable.footer.getWidget(index);
            unpinnedTable.footer.remove(index);
            pinnedTable.footer.add(widget);
            widget.setAttribute(PINNED_ATTRIBUTE);
        }
        pinnedTable.refreshWidth();
        unpinnedTable.refreshWidth();
        final String pinnedRowWidth = pinnedTable.width + "px";
        final String unpinnedRowWidth = unpinnedTable.width + "px";
        for (final Row row : rows) {
            row.pinnedCells.add(row.unpinnedCells.remove(index));
            final PWidget widget = row.unpinnedRow.getWidget(index);
            row.unpinnedRow.remove(index);
            row.pinnedRow.add(widget);
            row.pinnedRow.setWidth(pinnedRowWidth);
            row.unpinnedRow.setWidth(unpinnedRowWidth);
            widget.setAttribute(PINNED_ATTRIBUTE);
        }
    }

    private void unpin(final ColumnView columnView) {
        final int index = pinnedTable.columns.indexOf(columnView);
        pinnedTable.columns.remove(index);
        pinnedTable.width -= columnView.width;
        unpinnedTable.columns.add(0, columnView);
        unpinnedTable.width += columnView.width;
        {
            final PWidget widget = pinnedTable.header.getWidget(index);
            pinnedTable.header.remove(index);
            unpinnedTable.header.insert(widget, 0);
            widget.removeAttribute(PINNED_ATTRIBUTE);
        }
        {
            final PWidget widget = pinnedTable.footer.getWidget(index);
            pinnedTable.footer.remove(index);
            unpinnedTable.footer.insert(widget, 0);
            widget.removeAttribute(PINNED_ATTRIBUTE);
        }
        pinnedTable.refreshWidth();
        unpinnedTable.refreshWidth();
        final String pinnedRowWidth = pinnedTable.width + "px";
        final String unpinnedRowWidth = unpinnedTable.width + "px";
        for (final Row row : rows) {
            row.unpinnedCells.add(0, row.pinnedCells.remove(index));
            final PWidget widget = row.pinnedRow.getWidget(index);
            row.pinnedRow.remove(index);
            row.unpinnedRow.insert(widget, 0);
            row.pinnedRow.setWidth(pinnedRowWidth);
            row.unpinnedRow.setWidth(unpinnedRowWidth);
            widget.removeAttribute(PINNED_ATTRIBUTE);
        }
    }

    private void show(final ColumnView columnView) {
        unpinnedTable.addColumn(columnView);
        unpinnedTable.refreshWidth();
        final String unpinnedRowWidth = unpinnedTable.width + "px";
        for (final Row row : rows) {
            row.addCell(row.unpinnedRow, row.unpinnedCells, columnView);
            row.unpinnedRow.setWidth(unpinnedRowWidth);
        }
        onUpdateRows(0, controller.getRowCount());
    }

    private void hide(final ColumnView columnView) {
        final int index = unpinnedTable.columns.indexOf(columnView);
        unpinnedTable.removeColumn(index);
        unpinnedTable.refreshWidth();
        final String unpinnedRowWidth = unpinnedTable.width + "px";
        for (final Row row : rows) {
            row.unpinnedCells.remove(index);
            row.unpinnedRow.remove(index);
            row.unpinnedRow.setWidth(unpinnedRowWidth);
        }
    }

    @Override
    public void setPollingDelayMillis(final long pollingDelayMillis) {
        checkAdapter();
        if (this.pollingDelayMillis == pollingDelayMillis) return;
        this.pollingDelayMillis = pollingDelayMillis;
        if (delayedDrawRunnable != null) delayedDrawRunnable.cancel();
        if (pollingDelayMillis == 0L) {
            delayedDrawRunnable = null;
            draw();
        } else {
            delayedDrawRunnable = PScheduler.scheduleWithFixedDelay(this::draw, Duration.ZERO, Duration.ofMillis(pollingDelayMillis));
        }
    }

    @Override
    public void setFilter(final DataGridFilter<V> filter) {
        hideErrorMessage();
        filters.add(filter.getKey().hashCode());
        controller.setBound(false);
        controller.setFilter(filter);
        addon.scrollToTop();
        controller.setBound(true);
    }

    @Override
    public void setFilters(final Collection<DataGridFilter<V>> filters) {
        hideErrorMessage();
        filters.forEach(filter -> this.filters.add(filter.getKey().hashCode()));
        controller.setBound(false);
        controller.setFilters(filters);
        addon.scrollToTop();
        controller.setBound(true);
    }

    @Override
    public void clearFilter(final Object key) {
        hideErrorMessage();
        filters.remove(key.hashCode());
        controller.setBound(false);
        controller.clearFilter(key);
        addon.scrollToTop();
        controller.setBound(true);
    }

    @Override
    public void clearFilters(final Collection<Object> keys) {
        hideErrorMessage();
        keys.forEach(key -> filters.remove(key.hashCode()));
        controller.setBound(false);
        controller.clearFilters(keys);
        addon.scrollToTop();
        controller.setBound(true);
    }

    @Override
    public void clearFilters() {
        hideErrorMessage();
        filters.clear();
        controller.setBound(false);
        controller.clearFilters();
        addon.scrollToTop();
        controller.setBound(true);
    }

    @Override
    public void clearSorts(final boolean notify) {
        hideErrorMessage();
        controller.clearSorts();
        if (notify) {
            for (final ColumnView columnView : columnViews.values()) {
                for (final ColumnActionListener<V> listener : columnView.listeners) {
                    listener.onClearSort();
                }
            }
        }
    }

    private void checkAdapter() {
        if (adapter == null) throw new IllegalStateException("A DataGridAdapter) must be set");
    }

    @Override
    public PResultSet<V> getFilteredData() {
        checkAdapter();
        return controller.getFilteredData();
    }

    @Override
    public PResultSet<V> getLiveSelectedData() {
        checkAdapter();
        return controller.getLiveSelectedData();
    }

    @Override
    public int getLiveSelectedDataCount() {
        checkAdapter();
        return controller.getLiveSelectedDataCount();
    }

    @Override
    public Collection<V> getLiveData(final int from, final int dataSize) {
        checkAdapter();
        return controller.getLiveData(from, dataSize);
    }

    @Override
    public void selectAllLiveData() {
        checkAdapter();
        controller.selectAllLiveData();
        onUpdateRows(0, controller.getRowCount());
    }

    @Override
    public void unselectAllData() {
        checkAdapter();
        controller.unselectAllData();
        onUpdateRows(0, controller.getRowCount());
    }

    @Override
    public void addColumnActionListener(final ColumnDefinition<V> column, final ColumnActionListener<V> listener) {
        getColumnView(column).listeners.add(listener);
    }

    @Override
    public void removeColumnActionListener(final ColumnDefinition<V> column, final ColumnActionListener<V> listener) {
        final ColumnView columnView = columnViews.get(column);
        if (columnView == null) return;
        columnView.listeners.remove(listener);
    }

    @Override
    public String encodeConfigCustomValue(final String key, final Object value) {
        return null;
    }

    @Override
    public Object decodeConfigCustomValue(final String key, final String value) {
        return null;
    }

    @Override
    public DataGridConfig<V> getConfig() {
        checkAdapter();
        final DataGridConfigBuilder<V> builder = new DataGridConfigBuilder<>();
        final Set<ColumnView> hiddenColumns = new HashSet<>(columnViews.values());
        for (final ColumnView columnView : pinnedTable.columns) {
            builder.addColumnConfig(new ColumnConfig<>(columnView.column.getId(), columnView.state, columnView.width));
            hiddenColumns.remove(columnView);
        }
        for (final ColumnView columnView : unpinnedTable.columns) {
            builder.addColumnConfig(new ColumnConfig<>(columnView.column.getId(), columnView.state, columnView.width));
            hiddenColumns.remove(columnView);
        }
        for (final ColumnView columnView : hiddenColumns) {
            builder.addColumnConfig(new ColumnConfig<>(columnView.column.getId(), columnView.state, columnView.width));
        }
        controller.enrichConfigBuilder(builder);
        return builder.build();
    }

    private ColumnView getColumnView(final ColumnDefinition<V> column) {
        return columnViews.get(column);
    }

    @Override
    public void setConfig(final DataGridConfig<V> config) {
        checkAdapter();
        hideErrorMessage();
        final int relativeRowCount = rows.size();
        rows.clear();
        pinnedTable.clear();
        unpinnedTable.clear();

        final Map<String, ColumnDefinition<V>> columnById = setColumnConfigs(config);

        pinnedTable.refreshWidth();
        unpinnedTable.refreshWidth();
        this.controller.setConfig(config);
        for (int i = 0; i < relativeRowCount; i++) {
            rows.add(new Row(i));
        }
        refresh();
        addon.scrollToTop();

        notifyListeners(config, columnById);
    }

    private Map<String, ColumnDefinition<V>> setColumnConfigs(final DataGridConfig<V> config) {
        final List<ColumnDefinition<V>> remainingColumns = new ArrayList<>(adapter.getColumnDefinitions());
        final Map<String, ColumnDefinition<V>> columnById = new HashMap<>();
        for (final ColumnDefinition<V> column : adapter.getColumnDefinitions()) {
            columnById.put(column.getId(), column);
        }
        for (final ColumnConfig<V> columnConfig : config.getColumnConfigs()) {
            final ColumnDefinition<V> column = columnById.get(columnConfig.getColumnId());
            if (column == null || !remainingColumns.remove(column)) continue;
            setColumnConfig(column, columnConfig.getState(), columnConfig.getWidth());
        }
        for (final ColumnDefinition<V> column : remainingColumns) {
            setColumnConfig(column, column.getDefaultState(), column.getDefaultWidth());
        }
        return columnById;
    }

    private void notifyListeners(final DataGridConfig<V> config, final Map<String, ColumnDefinition<V>> columnById) {
        for (final ColumnDefinition<V> column : adapter.getColumnDefinitions()) {
            final ColumnView columnView = getColumnView(column);
            for (final ColumnActionListener<V> listener : columnView.listeners) {
                listener.onClearSort();
                listener.onClearFilters();
            }
        }

        for (final Sort<V> sort : config.getSorts()) {
            if (sort == null || sort instanceof GeneralSort) continue;
            final ColumnSort<V> columnSort = (ColumnSort<V>) sort;
            final ColumnDefinition<V> column = columnById.get(columnSort.getColumnId());
            if (column == null) continue;
            final ColumnView columnView = getColumnView(column);
            for (final ColumnActionListener<V> listener : columnView.listeners) {
                listener.onSort(columnSort.isAsc());
            }
        }
    }

    private void setColumnConfig(final ColumnDefinition<V> column, State state, final int width) {
        if (state == null) state = column.getDefaultState();
        final ColumnView columnView = getColumnView(column);
        columnView.width = width;
        if (state.isShown()) {
            final Table table = state.isPinned() ? pinnedTable : unpinnedTable;
            table.addColumn(columnView);
        }
        if (columnView.state != state) {
            columnView.state = state;
            columnView.notifyStateChangedListeners();
        }
        if (columnView.width != width) {
            columnView.width = width;
            columnView.notifyResizedListeners();
        }

    }

    private void hideErrorMessage() {
        errorMsgDiv.setVisible(false);
    }

    private void hideLoadingDataView() {
        addon.hideLoadingPanel();
    }

    private void showErrorMessage(final String msg) {
        errorMsgDiv.setVisible(true);
        errorMsgDiv.clear();
        final PElement div = Element.newDiv();
        div.setInnerText(msg);
        errorMsgDiv.add(div);
        loadingDataDiv.setVisible(false);
        // must be sent immediately
        UIContext.get().flush();
    }

    @Override
    public void addRowAction(final Object key, final RowAction<V> rowAction) {
        final RowAction<V> old = rowActions.put(key, rowAction);
        removeRowAction(old);
        for (final Row row : rows) {
            if (row.key == null) continue;
            final int absoluteIndex = row.getAbsoluteIndex();
            final int relativeIndex = row.getRelativeIndex();
            final V data = rows.get(relativeIndex).getData();
            if (data == null || !rowAction.testRow(data, absoluteIndex)) continue;
            rowAction.apply(row.pinnedRow);
            rowAction.apply(row.unpinnedRow);
        }
    }

    @Override
    public void clearRowAction(final Object key) {
        removeRowAction(rowActions.remove(key));
    }

    private void removeRowAction(final RowAction<V> old) {
        if (old == null) return;
        for (final Row row : rows) {
            if (row.key == null) continue;
            final int absoluteIndex = row.getAbsoluteIndex();
            final int relativeIndex = row.getRelativeIndex();
            final V data = rows.get(relativeIndex).getData();
            if (data == null || !old.testRow(data, absoluteIndex)) continue;
            old.cancel(row.pinnedRow);
            old.cancel(row.unpinnedRow);
        }
    }

    @Override
    public void addDrawListener(final DrawListener drawListener) {
        drawListeners.add(drawListener);
    }

    @Override
    public void removeDrawListener(final DrawListener drawListener) {
        drawListeners.remove(drawListener);
    }

    @Override
    public void addSort(final Object key, final Comparator<V> comparator) {
        hideErrorMessage();
        controller.addSort(key, comparator);
        draw();
    }

    @Override
    public void clearSort(final Object key) {
        hideErrorMessage();
        controller.clearSort(key);
        draw();
    }

    private void onDataUpdated() {
        if (delayedDrawRunnable == null) draw();
    }

    private void onDataRemoved(final K k) {
    }

    @Override
    public void resume() {
        shouldDraw = true;
        if (drawOnResume) {
            drawOnResume = false;
            draw();
        }
    }

    @Override
    public void pause() {
        shouldDraw = false;
    }

    @Override
    public void setExceptionHandler(final Function<Throwable, String> handler) {
        exceptionHandler = handler;
    }

    public void setRefreshOnColumnVisibilityChanged(final boolean refreshOnColumnVisibilityChanged) {
        this.refreshOnColumnVisibilityChanged = refreshOnColumnVisibilityChanged;
        if (addon != null) addon.setListenOnColumnVisibility(refreshOnColumnVisibilityChanged);
    }

    public void setForceExtended(final boolean forceExtended) {
        this.forceExtended = forceExtended;
    }

    public class Row {

        private final int relativeIndex;
        private K key;
        private V data;
        private final PComplexPanel unpinnedRow = Element.newDiv();
        private final PComplexPanel pinnedRow = Element.newDiv();
        private final List<CellManager<V>> unpinnedCells = new ArrayList<>(unpinnedTable.columns.size());
        private final List<CellManager<V>> pinnedCells = new ArrayList<>(pinnedTable.columns.size());
        private boolean extended = false;

        Row(final int index) {
            this.relativeIndex = index;
            init(unpinnedTable, unpinnedRow, unpinnedCells, false);
            init(pinnedTable, pinnedRow, pinnedCells, true);
            hide();
        }

        int getRelativeIndex() {
            return relativeIndex;
        }

        public V getData() {
            return data;
        }

        void setData(final V data) {
            this.data = data;
        }

        void init(final Table table, final PComplexPanel row, final List<CellManager<V>> cells, final boolean pinned) {
            row.addStyleName("pony-grid-row");
            row.setStyleProperty("white-space", "nowrap");
            for (final ColumnView column : table.columns) {
                final PComplexPanel div = addCell(row, cells, column);
                if (pinned) div.setAttribute(PINNED_ATTRIBUTE);
            }
            row.addDomHandler((PClickHandler) event -> {
                if (key == null || !controller.isSelectable(key)) return;
                if (controller.isSelected(key)) {
                    unselect();
                } else {
                    select();
                }
            }, PClickEvent.TYPE);
            adapter.onCreateRow(row);
            table.body.add(row);
            row.setWidth(table.width + "px");
        }

        void destroy() {
            if (relativeIndex != rows.size() - 1) return;
            rows.remove(relativeIndex);
            pinnedTable.body.remove(relativeIndex);
            unpinnedTable.body.remove(relativeIndex);
        }

        private PComplexPanel addCell(final PComplexPanel row, final List<CellManager<V>> cells, final ColumnView columnView) {
            final PComplexPanel div = Element.newDiv();
            div.setWidth(columnView.getWidthAsString());
            div.setStyleProperty("height", "100%");
            div.setStyleProperty("display", "inline-block");
            div.setStyleProperty("vertical-align", "top");

            final CellManager<V> cell = new CellManager<>(columnView.column.createCell());
            cell.setController(new CellControllerImpl(cell, this, columnView));
            cell.setTableCell(div);
            cell.setVisibility(columnView.visible);
            cells.add(cell);
            row.add(div);
            return div;
        }

        int getAbsoluteIndex() {
            return relativeIndex + firstRowIndex;
        }

        boolean isShown() {
            return unpinnedRow.isVisible();
        }

        void show() {
            unpinnedRow.setVisible(true);
            pinnedRow.setVisible(true);
        }

        void hide() {
            unpinnedRow.setVisible(false);
            pinnedRow.setVisible(false);
        }

        void select() {
            if (key == null || !controller.isSelectable(key)) return;
            controller.select(key);
            pinnedCells.forEach(CellManager::select);
            unpinnedCells.forEach(CellManager::select);
        }

        void unselect() {
            if (key == null || !controller.isSelectable(key)) return;
            controller.unselect(key);
            pinnedCells.forEach(CellManager::unselect);
            unpinnedCells.forEach(CellManager::unselect);
        }

        @Override
        public String toString() {
            return "Row [relativeIndex=" + relativeIndex + ", key=" + key + ", data=" + data + ", extended=" + extended + "]";
        }

    }

    private class PinnedTable extends Table {

        PinnedTable(final PComplexPanel header, final PComplexPanel body, final PComplexPanel footer) {
            super(header, body, footer);
            header.setStyleProperty("flex", "initial");
            header.addStyleName("pony-grid-pinned-header");
            body.setStyleProperty("flex", "initial");
            body.setStyleProperty("overflow-x", "unset");
            body.addStyleName("pony-grid-pinned-body");
            footer.setStyleProperty("flex", "initial");
            footer.addStyleName("pony-grid-pinned-footer");
        }

        @Override
        protected PComplexPanel addFooterCell(final DefaultDataGridView<K, V>.ColumnView columnView) {
            final PComplexPanel footer = super.addFooterCell(columnView);
            footer.setAttribute(PINNED_ATTRIBUTE);
            return footer;
        }

        @Override
        protected PComplexPanel addHeaderCell(final DefaultDataGridView<K, V>.ColumnView columnView) {
            final PComplexPanel header = super.addHeaderCell(columnView);
            header.setAttribute(PINNED_ATTRIBUTE);
            return header;
        }

    }

    private class UnpinnedTable extends Table {

        UnpinnedTable(final PComplexPanel header, final PComplexPanel body, final PComplexPanel footer) {
            super(header, body, footer);
            header.setStyleProperty("flex", "auto");
            header.addStyleName("pony-grid-hidden-scrollbar");
            header.addStyleName("pony-grid-unpinned-header");
            header.setStyleProperty("overflow-x", "auto");
            header.setStyleProperty("-ms-overflow-x", "auto");
            header.setStyleProperty("overflow-y", "hidden");
            header.setStyleProperty("-ms-overflow-y", "hidden");
            body.setStyleProperty("flex", "auto");
            body.addStyleName("pony-grid-hidden-scrollbar");
            body.addStyleName("pony-grid-unpinned-body");
            body.setStyleProperty("overflow-x", "auto");
            body.setStyleProperty("-ms-overflow-x", "auto");
            body.setStyleProperty("overflow-y", "hidden");
            body.setStyleProperty("-ms-overflow-y", "hidden");
            footer.setStyleProperty("flex", "auto");
            footer.addStyleName("pony-grid-unpinned-footer");
            footer.setStyleProperty("overflow-x", "auto");
            footer.setStyleProperty("-ms-overflow-x", "auto");
            footer.setStyleProperty("overflow-y", "hidden");
            footer.setStyleProperty("-ms-overflow-y", "hidden");
        }

    }

    private abstract class Table {

        int width = 0;
        final PComplexPanel header;
        final PComplexPanel body;
        final PComplexPanel footer;

        final List<ColumnView> columns = new ArrayList<>();

        Table(final PComplexPanel header, final PComplexPanel body, final PComplexPanel footer) {
            this.header = header;
            this.body = body;
            this.footer = footer;
            header.setStyleProperty("display", "flex");
            footer.setStyleProperty("display", "flex");
            header.setStyleProperty("white-space", "nowrap");
            footer.setStyleProperty("white-space", "nowrap");
        }

        void refreshWidth() {
            final String w = width + "px";
            header.setWidth(w);
            footer.setWidth(w);
        }

        void addColumn(final ColumnView columnView) {
            width += columnView.width;
            columns.add(columnView);

            addHeaderCell(columnView);
            addFooterCell(columnView);

            if (addon != null) addon.onColumnAdded(columnView.id, columnView.column.getMinWidth(), columnView.column.getMaxWidth(),
                columnView.state.isPinned());
        }

        protected PComplexPanel addFooterCell(final ColumnView columnView) {
            final PComplexPanel footerCell = Element.newDiv();
            footerCell.setStyleProperty("flex", "0 0 auto");
            footerCell.setStyleProperty("width", columnView.getWidthAsString());
            footerCell.setStyleProperty("vertical-align", "top");
            footer.add(footerCell);
            if (adapter.hasFooter()) {
                footerCell.add(columnView.column.getFooter());
                footerCell.setStyleProperty("height", "100%");
            } else {
                footerCell.setHeight("1px");
            }
            return footerCell;
        }

        protected PComplexPanel addHeaderCell(final ColumnView columnView) {
            final PComplexPanel headerCell = Element.newDiv();
            headerCell.setStyleProperty("flex", "0 0 auto");
            headerCell.setStyleProperty("position", "relative");
            headerCell.setStyleProperty("width", columnView.getWidthAsString());
            headerCell.setStyleProperty("vertical-align", "top");
            headerCell.setAttribute("data-column-id", Integer.toString(columnView.id));
            header.add(headerCell);
            if (adapter.hasHeader()) {
                headerCell.add(columnView.column.getHeader());
                headerCell.setStyleProperty("height", "100%");
                if (columnView.column.isResizable()) {
                    final PComplexPanel headerResizer = Element.newDiv();
                    headerResizer.addStyleName("pony-grid-col-resizer");
                    headerResizer.setHeight("100%");
                    headerResizer.setStyleProperty("position", "absolute");
                    headerResizer.setStyleProperty("top", "0");
                    headerResizer.setStyleProperty("right", "0");
                    headerResizer.setStyleProperty("cursor", "col-resize");
                    headerResizer.setStyleProperty("user-select", "none");
                    headerResizer.setStyleProperty("-webkit-user-select", "none");
                    headerResizer.setStyleProperty("-moz-user-select", "none");
                    headerResizer.setStyleProperty("-ms-user-select", "none");
                    headerCell.add(headerResizer);
                    adapter.onCreateColumnResizer(headerResizer);
                }
                final IsPWidget headerDraggableElement = columnView.column.getDraggableHeaderElement();
                if (headerDraggableElement != null) {
                    headerDraggableElement.asWidget().addStyleName("pony-grid-draggable-col");
                }
            } else {
                headerCell.setHeight("1px");
            }
            return headerCell;
        }

        void removeColumn(final int index) {
            final ColumnView columnView = columns.remove(index);
            width -= columnView.width;
            header.remove(index);
            footer.remove(index);
        }

        void clear() {
            columns.clear();
            header.clear();
            footer.clear();
            body.clear();
            width = 0;
        }
    }

    private class CellControllerImpl implements PrimaryCellController<V>, ExtendedCellController<V> {

        private final CellManager<V> cell;
        private final Row row;
        private final ColumnView column;

        public CellControllerImpl(final CellManager<V> cell, final DefaultDataGridView<K, V>.Row row, final ColumnView column) {
            this.cell = cell;
            this.row = row;
            this.column = column;
        }

        @Override
        public void setExtendedMode() {
            if (row.key == null) return;
            cell.extendedMode = true;
            updateRowCell(row, cell, column, getController().isSelected(row.key));
        }

        @Override
        public void setPrimaryMode() {
            if (row.key == null) return;
            cell.extendedMode = false;
            updateRowCell(row, cell, column, getController().isSelected(row.key));
        }

        @Override
        public void selectRow() {
            row.select();
        }

        @Override
        public void unselectRow() {
            row.unselect();
        }

        @Override
        public void updateValue(final V newV) {
            getController().setData(newV);
        }

        @Override
        public void updateValue(final Consumer<V> action) {
            getController().updateData(row.key, action);
        }
    }

    private class SimpleColumnController implements ColumnController<V> {

        private final ColumnView columnView;

        SimpleColumnController(final ColumnView columnView) {
            this.columnView = columnView;
        }

        @Override
        public void sort(final boolean asc) {
            if (!columnView.column.isSortable()) return;
            hideErrorMessage();
            final int i = 0;
            sorts.put(i, asc ? 1 : 0);
            controller.setBound(false);
            controller.addSort(columnView.column, asc);
            addon.scrollToTop();
            controller.setBound(true);
            for (final ColumnActionListener<V> listener : columnView.listeners) {
                listener.onSort(asc);
            }
        }

        @Override
        public void clearSort() {
            if (!columnView.column.isSortable()) return;
            hideErrorMessage();
            sorts.clear();
            controller.clearSort(columnView.column);
            for (final ColumnActionListener<V> listener : columnView.listeners) {
                listener.onClearSort();
            }
        }

        @Override
        public void filter(final Object key, final BiPredicate<V, Supplier<Object>> filter, final boolean active,
                           final boolean reinforcing) {
            if (!columnView.column.isFilterable()) return;
            hideErrorMessage();
            controller.setFilter(key, columnView.column, filter, active, reinforcing);
            for (final ColumnActionListener<V> listener : columnView.listeners) {
                listener.onFilter(key, filter, reinforcing);
            }
        }

        @Override
        public void clearFilter(final Object key) {
            if (!columnView.column.isFilterable()) return;
            hideErrorMessage();
            controller.clearFilter(key);
            for (final ColumnActionListener<V> listener : columnView.listeners) {
                listener.onClearFilter(key);
            }
        }

        @Override
        public void redraw(final boolean clearRenderingHelpers) {
            hideErrorMessage();
            if (clearRenderingHelpers) controller.clearRenderingHelpers(columnView.column);
            refresh();
            for (final ColumnActionListener<V> listener : columnView.listeners) {
                listener.onRedraw(clearRenderingHelpers);
            }
        }

        @Override
        public void clearFilters() {
            if (!columnView.column.isFilterable()) return;
            hideErrorMessage();
            controller.clearFilters(columnView.column);
            for (final ColumnActionListener<V> listener : columnView.listeners) {
                listener.onClearFilters();
            }
        }

        @Override
        public void setState(final State state) {
            if (columnView.state == state) return;

            if (state.isShown() && !columnView.state.isShown()) {
                show(columnView);
            } else if (!state.isShown() && columnView.state.isShown()) {
                hide(columnView);
                clearSort();
            }

            if (state.isPinned() && !columnView.state.isPinned()) {
                pin(columnView);
            } else if (!state.isPinned() && columnView.state.isPinned()) {
                unpin(columnView);
            }

            columnView.state = state;
            columnView.notifyStateChangedListeners();
        }

    }

    private class ColumnView {

        private final int id = columnViewSequence++;
        private final ColumnDefinition<V> column;
        private int width;
        private State state;
        private boolean visible = true;
        private final Set<ColumnActionListener<V>> listeners = new HashSet<>();

        public ColumnView(final ColumnDefinition<V> column) {
            super();
            this.column = column;
            this.width = column.getDefaultWidth();
            this.state = column.getDefaultState();
        }

        String getWidthAsString() {
            return width + "px";
        }

        void notifyStateChangedListeners() {
            for (final ColumnActionListener<V> listener : listeners) {
                listener.onStateChanged(state);
            }
        }

        void notifyResizedListeners() {
            for (final ColumnActionListener<V> listener : listeners) {
                listener.onResized(width);
            }
        }

        void notifyMovedListeners() {
            for (final ColumnActionListener<V> listener : listeners) {
                listener.onMoved();
            }
        }
    }

    private static class HideScrollBarAddon extends PAddOnComposite<PComplexPanel> {

        HideScrollBarAddon(final PComplexPanel root) {
            super(root);
        }
    }

    private static class CellManager<V> {

        private final PrimaryCell<V> primary;
        private final ExtendedCell<V> extended;
        private boolean extendedMode;

        public CellManager(final PrimaryCell<V> primary) {
            this.primary = primary;
            this.extended = primary.genExtended().orElse(null);
        }

        public PComplexPanel getTableCell() {
            return (PComplexPanel) primary.asWidget().getParent();
        }

        public void setTableCell(final PComplexPanel td) {
            td.add(primary.asPendingWidget());
            td.add(primary);
            if (extended != null) td.add(extended);
        }

        public void setVisibility(final boolean columnVisible) {
            primary.asPendingWidget().setVisible(!columnVisible);
            primary.asWidget().setVisible(columnVisible && !extendedMode);
            if (extended != null) extended.asWidget().setVisible(columnVisible && extendedMode);

            final PComplexPanel td = getTableCell();
            if (!columnVisible) {
                td.setAttribute(PENDING_ATTRIBUTE);
                td.removeAttribute(EXTENDED_ATTRIBUTE);
            } else {
                td.removeAttribute(PENDING_ATTRIBUTE);
                if (extendedMode) td.setAttribute(EXTENDED_ATTRIBUTE);
                else td.removeAttribute(EXTENDED_ATTRIBUTE);
            }
        }

        public <T extends PrimaryCellController<V> & ExtendedCellController<V>> void setController(final T controller) {
            primary.setController(controller);
            if (extended != null) extended.setController(controller);
        }

        public void select() {
            primary.select();
            if (extended != null) extended.select();
        }

        public void unselect() {
            primary.unselect();
            if (extended != null) extended.unselect();
        }

        public Cell<V, ?> getCell() {
            return extendedMode && extended != null ? extended : primary;
        }
    }

    private class Addon extends PAddOnComposite<PComplexPanel> {

        Addon() {
            super(root);
            setTerminalHandler(e -> {
                final JsonObject json = e.getData();
                if (json.containsKey(ADDON_ROW_KEY)) {
                    onScroll(json.getInt(ADDON_ROW_KEY));
                } else if (json.containsKey(ADDON_ROW_COUNT_KEY)) {
                    onRelativeRowCountUpdated(json.getInt(ADDON_ROW_COUNT_KEY));
                } else if (json.containsKey(ADDON_COLUMN_VISIBILITY)) {
                    onColumnVisibilityChanged(json.getJsonArray(ADDON_COLUMN_ID), json.getJsonArray(ADDON_COLUMN_VISIBILITY));
                } else if (json.containsKey(ADDON_COLUMN_WIDTH)) {
                    onColumnResized(json.getInt(ADDON_COLUMN_ID), //
                        json.getInt(ADDON_COLUMN_WIDTH));
                } else if (json.containsKey(ADDON_COLUMN_TO)) {
                    onColumnMoved(json.getInt(ADDON_COLUMN_ID), json.getInt(ADDON_COLUMN_TO));
                }
            });
        }

        public void setListenOnColumnVisibility(final boolean value) {
            callTerminalMethod("listenOnColumnVisibility", value);
        }

        public void updateRowHeight(final int index) {
            callTerminalMethod("updateRowHeight", index);
        }

        public void updateExtendedRowHeight(final int index) {
            callTerminalMethod("updateExtendedRowHeight", index);
        }

        public void onDataUpdated(final int absRowCount, final int relRowCount, final int firstRowIndex) {
            callTerminalMethod("onDataUpdated", absRowCount, relRowCount, firstRowIndex);
        }

        public void onColumnAdded(final int id, final int colMinWidth, final int colMaxWidth, final boolean pinned) {
            callTerminalMethod("onColumnAdded", id, colMinWidth, colMaxWidth, pinned);
        }

        public void scrollToTop() {
            callTerminalMethod("scrollToTop");
        }

        public void scrollTo(final int index, final int total) {
            callTerminalMethod("scrollTo", index, total);
        }

        public void checkPosition() {
            callTerminalMethod("checkPosition");
        }

        public void showLoadingPanel() {
            callTerminalMethod("showLoading");
        }

        public void hideLoadingPanel() {
            callTerminalMethod("hideLoading");
        }

        @Override
        public void destroy() {
            callTerminalMethod("destroy");
            super.destroy();
        }
    }
}
