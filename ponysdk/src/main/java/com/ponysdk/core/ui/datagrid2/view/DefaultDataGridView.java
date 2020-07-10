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
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.server.concurrent.PScheduler.UIRunnable;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.cell.Cell;
import com.ponysdk.core.ui.datagrid2.cell.CellController;
import com.ponysdk.core.ui.datagrid2.cell.ExtendedCell;
import com.ponysdk.core.ui.datagrid2.cell.ExtendedCellController;
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
import com.ponysdk.core.ui.datagrid2.controller.DefaultDataGridController;
import com.ponysdk.core.ui.datagrid2.controller.SpyDataGridController;
import com.ponysdk.core.ui.datagrid2.data.RowAction;
import com.ponysdk.core.ui.datagrid2.data.ViewLiveData;
import com.ponysdk.core.ui.datagrid2.datasource.DataGridSource;
import com.ponysdk.core.ui.datagrid2.datasource.DefaultCacheDataSource;
import com.ponysdk.core.util.SetUtils;

/**
 * @author mbagdouri
 */
public final class DefaultDataGridView<K, V> implements DataGridView<K, V> {

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
    /**
     * Set on a row widget when it is hovered
     */
    public static final String HOVERED_ATTRIBUTE = "pony-hovered";

    private static final Logger log = LoggerFactory.getLogger(DefaultDataGridView.class);
    private final int i = 0;
    // State
    private static final int MIN_RELATIVE_ROW_COUNT = 9;
    private int columnViewSequence = 0;
    private final PComplexPanel root = Element.newDiv();
    private final PComplexPanel loadingDataDiv;
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
    private final Collection<V> selectedDataView;
    private final Map<ColumnDefinition<V>, ColumnView> columnViews = new HashMap<>();
    private final DataGridControllerWrapper controllerWrapper;
    private final LinkedHashMap<Object, RowAction<V>> rowActions = new LinkedHashMap<>();
    private int firstRowIndex;
    private final Map<Integer, Integer> sorts = new HashMap<>();
    private final Set<Integer> filters = new HashSet<>();

    public DefaultDataGridView() {
        this(new DefaultCacheDataSource<K, V>());
    }

    public DefaultDataGridView(final DataGridSource<K, V> dataSource) {

        new HideScrollBarAddon(root);
        controller = new DefaultDataGridController<>(dataSource);
        controller.setListener(this::onUpdateRows);
        selectedDataView = controller.getLiveSelectedData();
        controllerWrapper = new DataGridControllerWrapper(controller.get());

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

        final PComplexPanel bodyDiv = prepareBodyDiv(subBodyDiv);
        root.add(bodyDiv);

        final PComplexPanel footerPinnedDiv = Element.newDiv();
        final PComplexPanel footerUnpinnedDiv = Element.newDiv();
        final PComplexPanel footerDiv = prepareFooterDiv(footerPinnedDiv, footerUnpinnedDiv);
        root.add(footerDiv);

        pinnedTable = new PinnedTable(headerPinnedDiv, bodyPinnedDiv, footerPinnedDiv);
        unpinnedTable = new UnpinnedTable(headerUnpinnedDiv, bodyUnpinnedDiv, footerUnpinnedDiv);

    }

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
        loadingDataDiv.setStyleProperty("position", "absolute");
        loadingDataDiv.setStyleProperty("top", "0px");
        loadingDataDiv.setStyleProperty("left", "0px");
        loadingDataDiv.setStyleProperty("width", "100%");
        loadingDataDiv.setStyleProperty("height", "100%");
        return loadingDataDiv;
    }

    @Override
    public PWidget asWidget() {
        return root;
    }

    @Override
    public DataGridController<K, V> getController() {
        return controllerWrapper;
    }

    private void onScroll(final int row) {
        showLoadingDataView();
        firstRowIndex = row;
        onUpdateRows(0, controller.getRowCount());
        draw();
    }

    private void onRelativeRowCountUpdated(int relRowCount) {
        showLoadingDataView();
        final int mod = relRowCount % 3;
        relRowCount = Math.max(mod == 0 ? relRowCount : relRowCount - mod + 3, MIN_RELATIVE_ROW_COUNT);
        if (this.rows.size() == relRowCount) return;
        if (this.rows.size() < relRowCount) {
            for (int i = this.rows.size(); i < relRowCount; i++) {
                rows.add(new Row(i));
            }
        } else do {
            rows.get(rows.size() - 1).destroy();
        } while (rows.size() > relRowCount);
        onUpdateRows(0, controller.getRowCount());
        draw();
        addon.checkPosition();
    }

    private void onColumnVisibilityChanged(final JsonArray columns, final JsonArray visiblity) {
        for (int i = 0; i < columns.size(); i++) {
            final int column = columns.getInt(i);
            final boolean visible = visiblity.getBoolean(i);
            final ColumnDefinition<V> c = adapter.getColumnDefinitions().get(column);
            final ColumnView columnView = getColumnView(c);
            columnView.visible = visible;
        }
        onUpdateRows(0, controller.getRowCount());
        draw();
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
    }

    public void onUpdateRows(final int from, final int to) {
        if (from >= to) return;
        this.from = Math.min(this.from, from);
        this.to = Math.max(this.to, to);
    }

    @Override
    public int getLiveDataRowCount() {
        return rows.size();
    }

    private void draw() {
        try {
            if (from >= to) return;
            final int size = unpinnedTable.body.getWidgetCount();
            final int start = Math.max(0, from - firstRowIndex);
            final DataGridSnapshot viewStateSnapshot = new DataGridSnapshot(firstRowIndex, size, start, sorts, filters);
            final Consumer<DefaultDataGridController<K, V>.DataSrcResult> consumer = PScheduler.delegate(this::updateView);
            controller.prepareLiveDataOnScreen(firstRowIndex, size, viewStateSnapshot, consumer);
        } catch (final Exception e) {
            log.error("Cannot draw data from data source", e);
        }
    }

    private void updateView(final DefaultDataGridController<K, V>.DataSrcResult dataSrcResult) {
        try {
            final ViewLiveData<V> resultLiveData = dataSrcResult.viewLiveData;
            for (int i = dataSrcResult.start; i < rows.size(); i++) {
                updateRow(rows.get(i), resultLiveData);
            }
            addon.onDataUpdated(resultLiveData.absoluteRowCount, rows.size(), dataSrcResult.firstRowIndex);
        } catch (final Exception e) {
            log.error("Problem occured while updating the view", e);
        } finally {
            from = Integer.MAX_VALUE;
            to = 0;
            for (final DrawListener drawListener : drawListeners) {
                drawListener.onDraw();
            }
            hideLoadingDataView();
        }
    }

    private void updateRow(final Row row, final ViewLiveData<V> result) {
        if (row.getAbsoluteIndex() >= result.absoluteRowCount) {
            row.hide();
            row.key = null;
            return;
        }
        final boolean mustUpdateRowHeight = row.extended || !row.isShown();
        row.show();
        final K previousKey = row.key;
        final V rowData = getRowData(row.getRelativeIndex(), result);
        row.setData(rowData);
        row.key = adapter.getKey(rowData);
        final boolean selected = controller.isSelected(row.key);
        row.extended = false;
        updateRowCells(row, row.unpinnedCells, unpinnedTable.columns, selected, previousKey, result);
        updateRowCells(row, row.pinnedCells, pinnedTable.columns, selected, previousKey, result);
        if (row.extended) {
            addon.updateExtendedRowHeight(row.relativeIndex);
        } else if (mustUpdateRowHeight) {
            addon.updateRowHeight(row.relativeIndex);
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

    private V getRowData(final int rowIndex, final ViewLiveData<V> result) {
        checkAdapter();
        final V v = rowIndex < result.liveData.size() ? result.liveData.get(rowIndex).getData() : null;
        return v;
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

    private void updateRowCells(final Row row, final List<Cell<V>> cells, final List<ColumnView> columns, final boolean selected,
                                final K previousKey, final ViewLiveData<V> result) {
        for (int c = 0; c < cells.size(); c++) {
            final Cell<V> cell = cells.get(c);
            final ColumnView column = columns.get(c);
            updateRowCell(row, cell, column, selected, previousKey, result);
        }
    }

    private void updateRowCell(final Row row, final Cell<V> cell, final ColumnView columnView, final boolean selected,
                               final K previousKey, final ViewLiveData<V> result) {
        final PComplexPanel td = (PComplexPanel) cell.asWidget().getParent();
        final ExtendedCellHandler extendedCellHandler = columnView.extendedCells.get(row.key);
        if (!columnView.visible && extendedCellHandler == null) {
            showPendingWidget(cell, selected, td);
        } else {
            td.removeAttribute(PENDING_ATTRIBUTE);
            cell.asPendingWidget().setVisible(false);
            if (extendedCellHandler == null) {
                showCellWidget(row, cell, columnView, selected, previousKey, td, result);
            } else {
                showExtendedCellWidget(row, cell, td, extendedCellHandler, result);
            }
        }

    }

    private void showExtendedCellWidget(final Row row, final Cell<V> cell, final PComplexPanel td,
                                        final ExtendedCellHandler extendedCellHandler, final ViewLiveData<V> result) {
        td.setAttribute(EXTENDED_ATTRIBUTE);
        extendedCellHandler.row = row;
        row.extended = true;
        cell.asWidget().setVisible(false);
        if (extendedCellHandler.cell.asWidget().getParent() != td) {
            if (extendedCellHandler.cell.asWidget().getParent() != null) {
                extendedCellHandler.cell.beforeRemove();
                extendedCellHandler.cell.asWidget().removeFromParent();
            }
            td.add(extendedCellHandler.cell);
            extendedCellHandler.cell.afterAdd();
        }
        controller.setValueOnExtendedCell(row.getRelativeIndex(), extendedCellHandler.cell, result);
    }

    private void showCellWidget(final Row row, final Cell<V> cell, final ColumnView columnView, final boolean selected,
                                final K previousKey, final PComplexPanel td, final ViewLiveData<V> result) {
        td.removeAttribute(EXTENDED_ATTRIBUTE);
        cell.asWidget().setVisible(true);
        controller.renderCell(columnView.column, row.getRelativeIndex(), cell, result);
        if (td.getWidgetCount() > 2) {
            final ExtendedCellHandler previousExtendedCellHandler = columnView.extendedCells.get(previousKey);
            if (previousExtendedCellHandler != null && previousExtendedCellHandler.cell.asWidget().getParent() != null) {
                previousExtendedCellHandler.cell.beforeRemove();
            }
            td.remove(2);
        }
        if (selected) cell.select();
        else cell.unselect();
    }

    private void showPendingWidget(final Cell<V> cell, final boolean selected, final PComplexPanel td) {
        td.setAttribute(PENDING_ATTRIBUTE);
        td.removeAttribute(EXTENDED_ATTRIBUTE);
        cell.asPendingWidget().setVisible(true);
        cell.asWidget().setVisible(false);
        if (td.getWidgetCount() > 2) td.remove(2);
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
        onUpdateRows(0, rows.size());
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
            delayedDrawRunnable = PScheduler.scheduleAtFixedRate(this::draw, Duration.ofMillis(pollingDelayMillis));
        }
    }

    @Override
    public void setFilter(final Object key, final String id, final Predicate<V> filter, final boolean reinforcing) {
        showLoadingDataView();
        filters.add(key.hashCode());
        controller.setFilter(key, id, filter, reinforcing);
        addon.scrollToTop();
        draw();
    }

    @Override
    public void clearFilter(final Object key) {
        showLoadingDataView();
        filters.remove(key.hashCode());
        controller.clearFilter(key);
        addon.scrollToTop();
        draw();
    }

    @Override
    public void clearFilters() {
        showLoadingDataView();
        filters.clear();
        controller.clearFilters();
        addon.scrollToTop();
        draw();
    }

    @Override
    public void clearSorts() {
        showLoadingDataView();
        controller.clearSorts();
        draw();
        for (final ColumnView columnView : columnViews.values()) {
            for (final ColumnActionListener<V> listener : columnView.listeners) {
                listener.onClearSort();
            }
        }
    }

    private void checkAdapter() {
        if (adapter == null) throw new IllegalStateException("A DataGridAdapter) must be set");
    }

    @Override
    public Collection<V> getLiveSelectedData() {
        checkAdapter();
        return selectedDataView;
    }

    @Override
    public void selectAllLiveData() {
        checkAdapter();
        controller.selectAllLiveData();
        for (final DefaultDataGridView<K, V>.Row row : rows) {
            if (row.pinnedRow.isVisible()) row.select();
        }
        for (final ColumnView column : columnViews.values()) {
            for (final Map.Entry<K, ExtendedCellHandler> entry : column.extendedCells.entrySet()) {
                if (controller.isSelected(entry.getKey())) entry.getValue().cell.select();
            }
        }
    }

    @Override
    public void unselectAllData() {
        checkAdapter();
        controller.unselectAllData();
        for (final DefaultDataGridView<K, V>.Row row : rows) {
            row.unselect();
        }
        for (final ColumnView column : columnViews.values()) {
            for (final ExtendedCellHandler handler : column.extendedCells.values()) {
                handler.cell.unselect();
            }
        }
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
        showLoadingDataView();
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
        onUpdateRows(0, controller.getRowCount());
        addon.scrollToTop();
        draw();

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

    private void showLoadingDataView() {
        loadingDataDiv.setVisible(true);
        // must be sent immediately
        Txn.get().flush(); // FIXME use Txn.get() ???
    }

    private void hideLoadingDataView() {
        loadingDataDiv.setVisible(false);
        // must be sent immediately
        Txn.get().flush(); // FIXME use Txn.get() ???
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

    // FIXME : not called when adding a sort
    @Override
    public void addSort(final Object key, final Comparator<V> comparator) {
        showLoadingDataView();
        controller.addSort(key, comparator);
        draw();
    }

    @Override
    public void clearSort(final Object key) {
        showLoadingDataView();
        controller.clearSort(key);
        draw();
    }

    public class Row {

        private final int relativeIndex;
        private K key;
        private V data;
        private final PComplexPanel unpinnedRow = Element.newDiv();
        private final PComplexPanel pinnedRow = Element.newDiv();
        private final List<Cell<V>> unpinnedCells = new ArrayList<>(unpinnedTable.columns.size());
        private final List<Cell<V>> pinnedCells = new ArrayList<>(pinnedTable.columns.size());
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

        void init(final Table table, final PComplexPanel row, final List<Cell<V>> cells, final boolean pinned) {
            row.addStyleName("pony-grid-row");
            row.setStyleProperty("white-space", "nowrap");
            for (final ColumnView column : table.columns) {
                final PComplexPanel div = addCell(row, cells, column);
                if (pinned) div.setAttribute(PINNED_ATTRIBUTE);
            }
            row.addDomHandler((PClickHandler) event -> {
                if (key == null) return;
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

        private PComplexPanel addCell(final PComplexPanel row, final List<Cell<V>> cells, final ColumnView columnView) {
            final PComplexPanel div = Element.newDiv();
            div.setWidth(columnView.getWidthAsString());
            div.setStyleProperty("height", "100%");
            div.setStyleProperty("display", "inline-block");
            div.setStyleProperty("vertical-align", "top");

            final Cell<V> cell = columnView.column.createCell();
            cell.setController(new SimpleCellController(this, columnView));
            div.add(cell);
            cell.asWidget().setVisible(columnView.visible);

            div.add(cell.asPendingWidget());
            cell.asPendingWidget().setVisible(!columnView.visible);

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
            if (key == null) return;
            controller.select(key);
            adapter.onSelectRow(unpinnedRow);
            unpinnedRow.setAttribute(SELECTED_ATTRIBUTE);
            adapter.onSelectRow(pinnedRow);
            pinnedRow.setAttribute(SELECTED_ATTRIBUTE);
            for (final Cell<V> cell : pinnedCells) {
                cell.select();
            }
            for (final Cell<V> cell : unpinnedCells) {
                cell.select();
            }
            if (extended) {
                for (final ColumnView column : columnViews.values()) {
                    final ExtendedCellHandler handler = column.extendedCells.get(key);
                    if (handler == null) continue;
                    handler.cell.select();
                }
            }
        }

        void unselect() {
            if (key == null) return;
            controller.unselect(key);
            adapter.onUnselectRow(unpinnedRow);
            unpinnedRow.removeAttribute(SELECTED_ATTRIBUTE);
            adapter.onUnselectRow(pinnedRow);
            pinnedRow.removeAttribute(SELECTED_ATTRIBUTE);
            for (final Cell<V> cell : pinnedCells) {
                cell.unselect();
            }
            for (final Cell<V> cell : unpinnedCells) {
                cell.unselect();
            }
            if (extended) {
                for (final ColumnView column : columnViews.values()) {
                    final ExtendedCellHandler handler = column.extendedCells.get(key);
                    if (handler == null) continue;
                    handler.cell.unselect();
                }
            }
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

    private class SimpleCellController implements CellController<V> {

        private final Row row;
        private final ColumnView columnView;

        SimpleCellController(final DefaultDataGridView<K, V>.Row row, final ColumnView column) {
            super();
            this.row = row;
            this.columnView = column;
        }

        @Override
        public void extendedMode(final ExtendedCell<V> extendedCell) {
            if (row.key == null) return;
            final ExtendedCellHandler handler = new ExtendedCellHandler(extendedCell, columnView, row);
            columnView.extendedCells.put(row.key, handler);
            extendedCell.setController(handler);

            if (controller.isSelected(row.key)) extendedCell.select();
            else extendedCell.unselect();

            onUpdateRows(row.getAbsoluteIndex(), row.getAbsoluteIndex() + 1);
            draw();
        }

        @Override
        public void selectRow() {
            row.select();
        }

        @Override
        public void unselectRow() {
            row.unselect();
        }

    }

    private class ExtendedCellHandler implements ExtendedCellController<V> {

        private final ExtendedCell<V> cell;
        private final ColumnView column;
        private final K key;
        private Row row;

        ExtendedCellHandler(final ExtendedCell<V> cell, final ColumnView column, final Row row) {
            super();
            this.cell = cell;
            this.key = row.key;
            this.column = column;
            this.row = row;
        }

        @Override
        public void cancelExtendedMode() {
            column.extendedCells.remove(key);
            if (row == null) return;
            onUpdateRows(row.getAbsoluteIndex(), row.getAbsoluteIndex() + 1);
            draw();
        }

        @Override
        public void selectRow() {
            if (row != null) row.select();
        }

        @Override
        public void unselectRow() {
            if (row != null) row.unselect();
        }

        @Override
        public void updateValue(final V newV) {
            getController().setData(newV);
            draw();
        }

        @Override
        public void updateValue(final Consumer<V> action) {
            getController().updateData(key, action);
            draw();
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
            showLoadingDataView();
            sorts.put(i, asc ? 1 : 0);
            controller.addSort(columnView.column, asc);
            addon.scrollToTop();
            draw();
            for (final ColumnActionListener<V> listener : columnView.listeners) {
                listener.onSort(asc);
            }
        }

        @Override
        public void clearSort() {
            if (!columnView.column.isSortable()) return;
            showLoadingDataView();
            sorts.clear();
            controller.clearSort(columnView.column);
            draw();
            for (final ColumnActionListener<V> listener : columnView.listeners) {
                listener.onClearSort();
            }
        }

        @Override
        public void filter(final Object key, final BiPredicate<V, Supplier<Object>> filter, final boolean reinforcing) {
            if (!columnView.column.isFilterable()) return;
            showLoadingDataView();
            controller.setFilter(key, columnView.column, filter, reinforcing);
            draw();
            for (final ColumnActionListener<V> listener : columnView.listeners) {
                listener.onFilter(key, filter, reinforcing);
            }
        }

        @Override
        public void clearFilter(final Object key) {
            if (!columnView.column.isFilterable()) return;
            showLoadingDataView();
            controller.clearFilter(key);
            draw();
            for (final ColumnActionListener<V> listener : columnView.listeners) {
                listener.onClearFilter(key);
            }
        }

        @Override
        public void redraw(final boolean clearRenderingHelpers) {
            showLoadingDataView();
            if (clearRenderingHelpers) controller.clearRenderingHelpers(columnView.column);
            onUpdateRows(0, controller.getRowCount());
            draw();
            for (final ColumnActionListener<V> listener : columnView.listeners) {
                listener.onRedraw(clearRenderingHelpers);
            }
        }

        @Override
        public void clearFilters() {
            if (!columnView.column.isFilterable()) return;
            showLoadingDataView();
            controller.clearFilters(columnView.column);
            draw();
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
                clearFilters();
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
        private final Map<K, ExtendedCellHandler> extendedCells = new HashMap<>();

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
    }

    private class DataGridControllerWrapper extends SpyDataGridController<K, V> {

        DataGridControllerWrapper(final DataGridController<K, V> controller) {
            super(controller);
        }

        @Override
        protected void onDataUpdate() {
            if (delayedDrawRunnable == null) {
                draw();
            }
        }

        @Override
        public V removeData(final K k) {
            final V v = super.removeData(k);
            for (final ColumnView columnView : columnViews.values()) {
                columnView.extendedCells.remove(k);
            }
            return v;
        }
    }

    private static class HideScrollBarAddon extends PAddOnComposite<PComplexPanel> {

        HideScrollBarAddon(final PComplexPanel root) {
            super(root);
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

        public void checkPosition() {
            callTerminalMethod("checkPosition");
        }
    }
}
