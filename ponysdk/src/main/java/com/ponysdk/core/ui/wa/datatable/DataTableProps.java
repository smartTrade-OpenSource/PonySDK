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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Props record for {@link PDataTable}.
 *
 * @param columns       the column definitions
 * @param data          the row data (each row is a map of field name to value)
 * @param page          the current page number (0-based)
 * @param pageSize      the number of rows per page
 * @param totalRows     the total number of rows in the dataset
 * @param sortField     the currently sorted field name (empty if none)
 * @param sortDirection the sort direction ("asc" or "desc")
 * @param selectedRows  the set of selected row identifiers
 * @param virtualScroll whether virtual scrolling is enabled
 */
public record DataTableProps(
    List<ColumnDef> columns,
    List<Map<String, Object>> data,
    int page,
    int pageSize,
    int totalRows,
    String sortField,
    String sortDirection,
    Set<String> selectedRows,
    boolean virtualScroll
) {

    /**
     * Creates a DataTableProps with sensible defaults.
     */
    public static DataTableProps defaults() {
        return new DataTableProps(
            List.of(),
            List.of(),
            0,
            25,
            0,
            "",
            "asc",
            Set.of(),
            false
        );
    }

    public DataTableProps withColumns(final List<ColumnDef> columns) {
        return new DataTableProps(columns, data, page, pageSize, totalRows, sortField, sortDirection, selectedRows, virtualScroll);
    }

    public DataTableProps withData(final List<Map<String, Object>> data) {
        return new DataTableProps(columns, data, page, pageSize, totalRows, sortField, sortDirection, selectedRows, virtualScroll);
    }

    public DataTableProps withPage(final int page) {
        return new DataTableProps(columns, data, page, pageSize, totalRows, sortField, sortDirection, selectedRows, virtualScroll);
    }

    public DataTableProps withPageSize(final int pageSize) {
        return new DataTableProps(columns, data, page, pageSize, totalRows, sortField, sortDirection, selectedRows, virtualScroll);
    }

    public DataTableProps withTotalRows(final int totalRows) {
        return new DataTableProps(columns, data, page, pageSize, totalRows, sortField, sortDirection, selectedRows, virtualScroll);
    }

    public DataTableProps withSortField(final String sortField) {
        return new DataTableProps(columns, data, page, pageSize, totalRows, sortField, sortDirection, selectedRows, virtualScroll);
    }

    public DataTableProps withSortDirection(final String sortDirection) {
        return new DataTableProps(columns, data, page, pageSize, totalRows, sortField, sortDirection, selectedRows, virtualScroll);
    }

    public DataTableProps withSelectedRows(final Set<String> selectedRows) {
        return new DataTableProps(columns, data, page, pageSize, totalRows, sortField, sortDirection, selectedRows, virtualScroll);
    }

    public DataTableProps withVirtualScroll(final boolean virtualScroll) {
        return new DataTableProps(columns, data, page, pageSize, totalRows, sortField, sortDirection, selectedRows, virtualScroll);
    }
}
