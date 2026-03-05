/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.ui.wa.datatable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import javax.json.JsonObject;

import com.ponysdk.core.ui.component.PWebComponent;

/**
 * Server-side wrapper for a data table component ({@code <wa-data-table>}).
 * <p>
 * Supports typed columns, server-side sorting, pagination, single/multiple row selection,
 * and virtual scrolling for large datasets (&gt; 1000 rows).
 * </p>
 * <p>
 * Data updates go through the normal {@link PWebComponent} props mechanism,
 * which uses {@link com.ponysdk.core.ui.component.PropsDiffer} to produce
 * JSON Patch for modified rows only.
 * </p>
 *
 * <h3>Events</h3>
 * <ul>
 *   <li>{@code wa-sort} — column sort requested (field + direction)</li>
 *   <li>{@code wa-page-change} — pagination page change</li>
 *   <li>{@code wa-selection-change} — row selection changed</li>
 * </ul>
 *
 * @see DataTableProps
 * @see ColumnDef
 * @see SortEvent
 */
public class PDataTable extends PWebComponent<DataTableProps> {

    private static final String EVENT_SORT = "wa-sort";
    private static final String EVENT_PAGE_CHANGE = "wa-page-change";
    private static final String EVENT_SELECTION_CHANGE = "wa-selection-change";

    public PDataTable() {
        super(DataTableProps.defaults());
    }

    public PDataTable(final DataTableProps initialProps) {
        super(initialProps);
    }

    @Override
    protected Class<DataTableProps> getPropsClass() {
        return DataTableProps.class;
    }

    @Override
    protected String getComponentSignature() {
        return "wa-data-table";
    }

    // ========== Data & Columns ==========

    /**
     * Updates the table data. The new data flows through the normal PWebComponent
     * props mechanism, which uses PropsDiffer to produce JSON Patch for modified rows only.
     *
     * @param rows the row data, each row is a map of field name to value
     * @throws NullPointerException if rows is null
     */
    public void setData(final List<Map<String, Object>> rows) {
        Objects.requireNonNull(rows, "Rows must not be null");
        setProps(getCurrentProps().withData(rows).withTotalRows(rows.size()));
    }

    /**
     * Sets the column definitions for the table.
     *
     * @param columns the column definitions
     * @throws NullPointerException if columns is null
     */
    public void setColumns(final List<ColumnDef> columns) {
        Objects.requireNonNull(columns, "Columns must not be null");
        setProps(getCurrentProps().withColumns(columns));
    }

    // ========== Event Handlers ==========

    /**
     * Registers a handler for sort events.
     * <p>
     * When a user clicks a sortable column header, the handler receives a {@link SortEvent}
     * with the field name and direction.
     * </p>
     *
     * @param handler the sort event handler
     * @throws NullPointerException if handler is null
     */
    public void onSort(final Consumer<SortEvent> handler) {
        Objects.requireNonNull(handler, "Sort handler must not be null");
        onEvent(EVENT_SORT, payload -> {
            final String field = payload.getString("field", "");
            final String direction = payload.getString("direction", "asc");
            handler.accept(new SortEvent(field, direction));
        });
    }

    /**
     * Registers a handler for page change events.
     *
     * @param handler the page change handler, receives the new page number
     * @throws NullPointerException if handler is null
     */
    public void onPageChange(final Consumer<Integer> handler) {
        Objects.requireNonNull(handler, "Page change handler must not be null");
        onEvent(EVENT_PAGE_CHANGE, payload -> {
            final int page = payload.getInt("page", 0);
            handler.accept(page);
        });
    }

    /**
     * Registers a handler for selection change events.
     *
     * @param handler the selection change handler, receives the set of selected row IDs
     * @throws NullPointerException if handler is null
     */
    public void onSelectionChange(final Consumer<Set<String>> handler) {
        Objects.requireNonNull(handler, "Selection change handler must not be null");
        onEvent(EVENT_SELECTION_CHANGE, payload -> {
            final Set<String> selected = new HashSet<>();
            if (payload.containsKey("selectedRows")) {
                final var arr = payload.getJsonArray("selectedRows");
                for (int i = 0; i < arr.size(); i++) {
                    selected.add(arr.getString(i));
                }
            }
            handler.accept(selected);
        });
    }

    // ========== Selection Management ==========

    /**
     * Selects a row by its identifier.
     *
     * @param rowId the row identifier to select
     * @throws NullPointerException if rowId is null
     */
    public void selectRow(final String rowId) {
        Objects.requireNonNull(rowId, "Row ID must not be null");
        final Set<String> updated = new HashSet<>(getCurrentProps().selectedRows());
        updated.add(rowId);
        setProps(getCurrentProps().withSelectedRows(updated));
    }

    /**
     * Deselects a row by its identifier.
     *
     * @param rowId the row identifier to deselect
     * @throws NullPointerException if rowId is null
     */
    public void deselectRow(final String rowId) {
        Objects.requireNonNull(rowId, "Row ID must not be null");
        final Set<String> updated = new HashSet<>(getCurrentProps().selectedRows());
        updated.remove(rowId);
        setProps(getCurrentProps().withSelectedRows(updated));
    }

    /**
     * Clears all row selections.
     */
    public void clearSelection() {
        setProps(getCurrentProps().withSelectedRows(Set.of()));
    }

    // ========== Pagination ==========

    /**
     * Sets the current page number.
     *
     * @param page the page number (0-based)
     */
    public void setPage(final int page) {
        setProps(getCurrentProps().withPage(page));
    }

    /**
     * Sets the number of rows per page.
     *
     * @param pageSize the page size
     */
    public void setPageSize(final int pageSize) {
        setProps(getCurrentProps().withPageSize(pageSize));
    }

    // ========== Virtual Scroll ==========

    /**
     * Enables or disables virtual scrolling.
     * <p>
     * Virtual scrolling is recommended for datasets exceeding 1000 rows.
     * </p>
     *
     * @param enabled true to enable virtual scrolling
     */
    public void setVirtualScroll(final boolean enabled) {
        setProps(getCurrentProps().withVirtualScroll(enabled));
    }
}
