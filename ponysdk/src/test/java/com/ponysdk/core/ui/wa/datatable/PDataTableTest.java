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

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.test.ModelWriterForTest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PDataTable}.
 *
 * <p>Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 8.6</p>
 */
class PDataTableTest {

    private PDataTable table;

    @BeforeEach
    void setUp() {
        final WebSocket socket = Mockito.mock(WebSocket.class);
        final ServletUpgradeRequest request = Mockito.mock(ServletUpgradeRequest.class);
        final TxnContext context = Mockito.spy(new TxnContext(socket));
        final ModelWriter modelWriter = new ModelWriterForTest();
        final Application application = Mockito.mock(Application.class);
        context.setApplication(application);
        final ApplicationConfiguration configuration = Mockito.mock(ApplicationConfiguration.class);
        Txn.get().begin(context);
        final UIContext uiContext = Mockito.spy(new UIContext(socket, context, configuration, request));
        Mockito.when(uiContext.getWriter()).thenReturn(modelWriter);
        UIContext.setCurrent(uiContext);

        table = new PDataTable();
    }

    @AfterEach
    void tearDown() {
        Txn.get().commit();
    }

    // ========== Construction & Defaults ==========

    @Test
    void defaultConstructor_createsWithDefaults() {
        final DataTableProps props = table.getCurrentProps();
        assertNotNull(props);
        assertTrue(props.columns().isEmpty());
        assertTrue(props.data().isEmpty());
        assertEquals(0, props.page());
        assertEquals(25, props.pageSize());
        assertEquals(0, props.totalRows());
        assertEquals("", props.sortField());
        assertEquals("asc", props.sortDirection());
        assertTrue(props.selectedRows().isEmpty());
        assertFalse(props.virtualScroll());
    }

    @Test
    void customConstructor_usesProvidedProps() {
        final var cols = List.of(ColumnDef.of("name", "Name"));
        final var props = DataTableProps.defaults().withColumns(cols).withPageSize(50);
        final var customTable = new PDataTable(props);
        assertEquals(cols, customTable.getCurrentProps().columns());
        assertEquals(50, customTable.getCurrentProps().pageSize());
    }

    @Test
    void getComponentSignature_returnsWaDataTable() {
        assertEquals("wa-data-table", table.getComponentSignature());
    }

    @Test
    void getPropsClass_returnsDataTablePropsClass() {
        assertEquals(DataTableProps.class, table.getPropsClass());
    }

    // ========== setColumns ==========

    @Test
    void setColumns_updatesProps() {
        final var cols = List.of(
            ColumnDef.of("id", "ID"),
            ColumnDef.sortable("name", "Name")
        );
        table.setColumns(cols);
        assertEquals(cols, table.getCurrentProps().columns());
    }

    @Test
    void setColumns_nullThrows() {
        assertThrows(NullPointerException.class, () -> table.setColumns(null));
    }

    // ========== setData ==========

    @Test
    void setData_updatesPropsAndTotalRows() {
        final var rows = List.of(
            Map.<String, Object>of("id", "1", "name", "Alice"),
            Map.<String, Object>of("id", "2", "name", "Bob")
        );
        table.setData(rows);
        assertEquals(rows, table.getCurrentProps().data());
        assertEquals(2, table.getCurrentProps().totalRows());
    }

    @Test
    void setData_nullThrows() {
        assertThrows(NullPointerException.class, () -> table.setData(null));
    }

    @Test
    void setData_emptyList() {
        table.setData(List.of());
        assertTrue(table.getCurrentProps().data().isEmpty());
        assertEquals(0, table.getCurrentProps().totalRows());
    }

    // ========== Selection ==========

    @Test
    void selectRow_addsSingleRow() {
        table.selectRow("row-1");
        assertTrue(table.getCurrentProps().selectedRows().contains("row-1"));
    }

    @Test
    void selectRow_multipleRows() {
        table.selectRow("row-1");
        table.selectRow("row-2");
        final Set<String> selected = table.getCurrentProps().selectedRows();
        assertEquals(Set.of("row-1", "row-2"), selected);
    }

    @Test
    void selectRow_duplicateIsIdempotent() {
        table.selectRow("row-1");
        table.selectRow("row-1");
        assertEquals(1, table.getCurrentProps().selectedRows().size());
    }

    @Test
    void deselectRow_removesRow() {
        table.selectRow("row-1");
        table.selectRow("row-2");
        table.deselectRow("row-1");
        assertEquals(Set.of("row-2"), table.getCurrentProps().selectedRows());
    }

    @Test
    void deselectRow_nonExistentIsNoOp() {
        table.selectRow("row-1");
        table.deselectRow("row-99");
        assertEquals(Set.of("row-1"), table.getCurrentProps().selectedRows());
    }

    @Test
    void clearSelection_removesAll() {
        table.selectRow("row-1");
        table.selectRow("row-2");
        table.clearSelection();
        assertTrue(table.getCurrentProps().selectedRows().isEmpty());
    }

    @Test
    void selectRow_nullThrows() {
        assertThrows(NullPointerException.class, () -> table.selectRow(null));
    }

    @Test
    void deselectRow_nullThrows() {
        assertThrows(NullPointerException.class, () -> table.deselectRow(null));
    }

    // ========== Pagination ==========

    @Test
    void setPage_updatesProps() {
        table.setPage(3);
        assertEquals(3, table.getCurrentProps().page());
    }

    @Test
    void setPageSize_updatesProps() {
        table.setPageSize(100);
        assertEquals(100, table.getCurrentProps().pageSize());
    }

    // ========== Virtual Scroll ==========

    @Test
    void setVirtualScroll_enablesVirtualScroll() {
        table.setVirtualScroll(true);
        assertTrue(table.getCurrentProps().virtualScroll());
    }

    @Test
    void setVirtualScroll_disablesVirtualScroll() {
        table.setVirtualScroll(true);
        table.setVirtualScroll(false);
        assertFalse(table.getCurrentProps().virtualScroll());
    }

    // ========== Event Handlers ==========

    @Test
    void onSort_nullThrows() {
        assertThrows(NullPointerException.class, () -> table.onSort(null));
    }

    @Test
    void onPageChange_nullThrows() {
        assertThrows(NullPointerException.class, () -> table.onPageChange(null));
    }

    @Test
    void onSelectionChange_nullThrows() {
        assertThrows(NullPointerException.class, () -> table.onSelectionChange(null));
    }

    @Test
    void onSort_registersHandler() {
        // Just verify it doesn't throw — actual event dispatch is integration-tested
        table.onSort(event -> {});
    }

    @Test
    void onPageChange_registersHandler() {
        table.onPageChange(page -> {});
    }

    @Test
    void onSelectionChange_registersHandler() {
        table.onSelectionChange(selected -> {});
    }

    // ========== ColumnDef ==========

    @Test
    void columnDef_ofFactory() {
        final var col = ColumnDef.of("email", "Email");
        assertEquals("email", col.field());
        assertEquals("Email", col.header());
        assertEquals("string", col.type());
        assertFalse(col.sortable());
        assertFalse(col.filterable());
        assertTrue(col.width().isEmpty());
    }

    @Test
    void columnDef_sortableFactory() {
        final var col = ColumnDef.sortable("name", "Name");
        assertTrue(col.sortable());
    }

    @Test
    void columnDef_fullConstructor() {
        final var col = new ColumnDef("age", "Age", "number", true, true, Optional.of(120));
        assertEquals("age", col.field());
        assertEquals("number", col.type());
        assertTrue(col.sortable());
        assertTrue(col.filterable());
        assertEquals(Optional.of(120), col.width());
    }

    // ========== DataTableProps withX ==========

    @Test
    void dataTableProps_withSortField() {
        final var props = DataTableProps.defaults().withSortField("name").withSortDirection("desc");
        assertEquals("name", props.sortField());
        assertEquals("desc", props.sortDirection());
    }

    // ========== SortEvent ==========

    @Test
    void sortEvent_record() {
        final var event = new SortEvent("name", "desc");
        assertEquals("name", event.field());
        assertEquals("desc", event.direction());
    }

    // ========== Combined Operations ==========

    @Test
    void setData_preservesOtherProps() {
        table.setColumns(List.of(ColumnDef.of("id", "ID")));
        table.setPage(2);
        table.setPageSize(10);
        table.selectRow("row-1");
        table.setVirtualScroll(true);

        table.setData(List.of(Map.of("id", "1")));

        // Verify other props are preserved
        assertEquals(1, table.getCurrentProps().columns().size());
        assertEquals(2, table.getCurrentProps().page());
        assertEquals(10, table.getCurrentProps().pageSize());
        assertTrue(table.getCurrentProps().selectedRows().contains("row-1"));
        assertTrue(table.getCurrentProps().virtualScroll());
    }
}
