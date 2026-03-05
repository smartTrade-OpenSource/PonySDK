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
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.Tag;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for row selection tracking.
 * <p>
 * <b>Property 9: Row Selection Tracking</b>
 * </p>
 * <p>
 * For any sequence of select/deselect/clear operations, the selected row IDs
 * maintained by PDataTable SHALL exactly match the expected set after applying
 * all operations in order.
 * </p>
 * <p>
 * <b>Validates: Requirements 8.5</b>
 * </p>
 */
@Tag("Feature: ui-library-wrapper, Property 9: Row Selection Tracking")
public class RowSelectionPropertyTest {

    @BeforeProperty
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
    }

    @AfterProperty
    void tearDown() {
        Txn.get().commit();
    }

    // ========== Operations Model ==========

    enum OpType { SELECT, DESELECT, CLEAR }

    record SelectionOp(OpType type, String rowId) {}

    // ========== Property Tests ==========

    /**
     * For any sequence of select/deselect/clear operations applied to a PDataTable,
     * the resulting selectedRows set exactly matches what you'd get by applying the
     * same operations to a reference HashSet.
     * <p>
     * <b>Validates: Requirements 8.5</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 9: Row selection tracking matches reference set")
    void selectionTrackingMatchesReferenceSet(
            @ForAll("selectionOperations") List<SelectionOp> operations) {

        final PDataTable table = new PDataTable();
        final Set<String> reference = new HashSet<>();

        for (final SelectionOp op : operations) {
            switch (op.type) {
                case SELECT -> {
                    table.selectRow(op.rowId);
                    reference.add(op.rowId);
                }
                case DESELECT -> {
                    table.deselectRow(op.rowId);
                    reference.remove(op.rowId);
                }
                case CLEAR -> {
                    table.clearSelection();
                    reference.clear();
                }
            }
        }

        assertEquals(reference, table.getCurrentProps().selectedRows(),
                "After " + operations.size() + " operations, selectedRows should match reference set");
    }

    /**
     * Selecting an already-selected row is idempotent — the set does not change.
     * <p>
     * <b>Validates: Requirements 8.5</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 9: selectRow is idempotent")
    void selectRowIsIdempotent(@ForAll("rowId") String rowId) {

        final PDataTable table = new PDataTable();
        table.selectRow(rowId);
        final Set<String> afterFirst = Set.copyOf(table.getCurrentProps().selectedRows());

        table.selectRow(rowId);
        final Set<String> afterSecond = Set.copyOf(table.getCurrentProps().selectedRows());

        assertEquals(afterFirst, afterSecond,
                "Selecting '" + rowId + "' twice should produce the same set");
        assertEquals(1, afterSecond.size());
    }

    /**
     * Deselecting a row that is not selected is a no-op.
     * <p>
     * <b>Validates: Requirements 8.5</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 9: deselectRow of non-selected row is no-op")
    void deselectNonSelectedIsNoOp(
            @ForAll("rowId") String selectedId,
            @ForAll("rowId") String otherId) {

        Assume.that(!selectedId.equals(otherId));

        final PDataTable table = new PDataTable();
        table.selectRow(selectedId);
        final Set<String> before = Set.copyOf(table.getCurrentProps().selectedRows());

        table.deselectRow(otherId);
        final Set<String> after = Set.copyOf(table.getCurrentProps().selectedRows());

        assertEquals(before, after,
                "Deselecting non-selected row '" + otherId + "' should not change the set");
    }

    /**
     * clearSelection always results in an empty set, regardless of prior state.
     * <p>
     * <b>Validates: Requirements 8.5</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 9: clearSelection always empties the set")
    void clearSelectionAlwaysEmpties(@ForAll("rowIds") List<String> rowIds) {

        final PDataTable table = new PDataTable();
        for (final String id : rowIds) {
            table.selectRow(id);
        }

        table.clearSelection();

        assertTrue(table.getCurrentProps().selectedRows().isEmpty(),
                "After clearSelection, selectedRows should be empty");
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<String> rowId() {
        return Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(10)
                .map(s -> "row-" + s);
    }

    @Provide
    Arbitrary<List<String>> rowIds() {
        return rowId().list().ofMinSize(1).ofMaxSize(20);
    }

    @Provide
    Arbitrary<List<SelectionOp>> selectionOperations() {
        final Arbitrary<String> ids = rowId();
        final Arbitrary<SelectionOp> op = Combinators.combine(
                Arbitraries.of(OpType.values()),
                ids
        ).as(SelectionOp::new);
        return op.list().ofMinSize(1).ofMaxSize(50);
    }
}
