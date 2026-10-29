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
package com.ponysdk.core.ui.datagrid2.view;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.cell.CellController;
import com.ponysdk.core.ui.datagrid2.cell.ExtendedCell;
import com.ponysdk.core.ui.datagrid2.cell.ExtendedCellController;
import com.ponysdk.core.ui.datagrid2.cell.LabelCell;
import com.ponysdk.core.ui.datagrid2.cell.PrimaryCell;
import com.ponysdk.core.ui.datagrid2.cell.PrimaryCellController;
import com.ponysdk.core.ui.datagrid2.column.ColumnController;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.data.FilterController;
import com.ponysdk.core.ui.datagrid2.datasource.DefaultCacheDataSource;
import com.ponysdk.test.PSuite;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.LogCapture;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.PAddOn;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.datagrid2.DomEvents;
import com.ponysdk.core.ui.datagrid2.column.ColumnActionListener;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition.State;
import com.ponysdk.core.ui.datagrid2.column.DefaultColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnSort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.GeneralSort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfigBuilder;
import com.ponysdk.core.ui.datagrid2.data.DataGridFilter;
import com.ponysdk.core.ui.datagrid2.data.RowAction;
import com.ponysdk.test.ModelWriterForTest;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import org.mockito.Mockito;
import java.util.Collections;
import java.util.Comparator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * Tests for Row#selectRange() and CellControllerImpl#selectRange() in DefaultDataGridView.
 *
 * Both methods implement "Quick Selection" (Shift+Click): selecting or deselecting a contiguous
 * range of rows relative to a previously clicked anchor row (lastClickedRow).
 *
 * Row#selectRange() is triggered by the Shift+Click DOM handler on a row.
 * CellControllerImpl#selectRange() is the programmatic API available to custom cells,
 * with an additional fallback when no anchor exists (toggle + set anchor).
 *
 * Test infrastructure: a real DefaultDataGridView<String, String> is constructed with a minimal
 * adapter. A CapturingColumnDefinition captures the CellController injected into each cell so
 * that tests can call selectRange() / selectRow() programmatically on specific rows.
 * setData() + awaitDraw() populates the row keys before assertions.
 */
public class DefaultDataGridViewTest extends PSuite {

    /** How long to wait for the async draw to complete before failing a test. */
    private static final long DRAW_TIMEOUT_MS = 3_000;

    private DefaultDataGridView<String, String> view;
    private CapturingColumnDefinition column;

    @BeforeEach
    public void setUp() {
        column = new CapturingColumnDefinition();
        view = newView(column);
    }

    // -------------------------------------------------------------------------
    // Row#selectRange() — R tests
    // -------------------------------------------------------------------------

    /**
     * R1 — no-op when lastClickedRow has never been set (no anchor):
     * CellControllerImpl#selectRange() falls back to a toggle-select when no anchor exists,
     * so calling selectRange() without any prior click selects the row (tested as C3).
     * Row#selectRange() itself (called internally) is a no-op, but the CellControllerImpl
     * wrapper intercepts the null-anchor case before reaching it.
     * This test verifies the combined behavior: row gets selected (fallback) not range-selected.
     */
    @Test
    public void rowSelectRange_noAnchor_fallbackSelectsSingleRow() throws Exception {
        setData("a", "b", "c", "d", "e");

        // No prior selectRow() → CellControllerImpl fallback: select row 2, set anchor
        getCellController(2).selectRange();

        // Only the target row is selected, not a range
        assertSelected("c");
        assertNotSelected("a", "b", "d", "e");
    }

    /**
     * R2 — when the attempted anchor targets a null-key row, selectRow() is a no-op so
     * lastClickedRow stays null. A subsequent selectRange() falls back to toggling the target row.
     */
    @Test
    public void rowSelectRange_anchorRowHasNullKey_fallbackSelectsSingleRow() throws Exception {
        // Only 2 rows loaded → rows 2+ have null keys
        setData("a", "b");

        // Row at index 4 has no key → selectRow() guard fires → lastClickedRow stays null
        getCellController(4).selectRow();

        // No anchor → CellControllerImpl fallback: select row 0, set anchor
        getCellController(0).selectRange();

        assertSelected("a");
        assertNotSelected("b");
    }

    /**
     * R3 — forward range: anchor at row 0, Shift+Click at row 2 (not selected) → rows 0–2 selected.
     */
    @Test
    public void rowSelectRange_forwardRange_selectsRows() throws Exception {
        setData("a", "b", "c", "d", "e");

        getCellController(0).selectRow();       // anchor = row 0
        getCellController(2).selectRange();     // Shift+Click row 2 (not selected)

        assertSelected("a", "b", "c");
        assertNotSelected("d", "e");
    }

    /**
     * R4 — backward range: anchor at row 4, Shift+Click at row 1 (not selected) → rows 1–4 selected.
     */
    @Test
    public void rowSelectRange_backwardRange_selectsRows() throws Exception {
        setData("a", "b", "c", "d", "e");

        getCellController(4).selectRow();       // anchor = row 4
        getCellController(1).selectRange();     // Shift+Click row 1 (not selected)

        assertNotSelected("a");
        assertSelected("b", "c", "d", "e");
    }

    /**
     * R5 — single-row range: anchor is row 0, Shift+Click on the same row as target (row 1).
     * The range from=1 to=1 selects only row 1.
     * Note: the anchor row (row 0) must be different from the target to avoid toggling on self.
     */
    @Test
    public void rowSelectRange_singleRow_selectsRow() throws Exception {
        setData("a", "b", "c");

        getCellController(0).selectRow();       // anchor = row 0 ("a")
        getCellController(1).selectRange();     // Shift+Click row 1 ("b", not selected)

        // Range from=0..1 is selected (both "a" and "b")
        assertSelected("a", "b");
        assertNotSelected("c");
    }

    /**
     * R6 — deselection range: when the target row is already selected, the range is unselected.
     * Rows 0–3 pre-selected; anchor = row 0; Shift+Click on row 2 (selected) → rows 0–2 deselected.
     */
    @Test
    public void rowSelectRange_targetAlreadySelected_deselectsRange() throws Exception {
        setData("a", "b", "c", "d", "e");

        // Pre-select rows 0–3 directly via the controller (no lastClickedRow side-effect)
        view.getController().select("a");
        view.getController().select("b");
        view.getController().select("c");
        view.getController().select("d");

        // Set anchor to row 0 ("a", which is selected)
        getCellController(0).selectRow();

        // Shift+Click on row 2 ("c", already selected) → unselectKeys(0..2)
        getCellController(2).selectRange();

        assertNotSelected("a", "b", "c");
        assertSelected("d");
        assertNotSelected("e");
    }

    /**
     * R7 — non-selectable rows inside the range are silently skipped.
     * Row "c" (index 2) is made non-selectable via a custom DataGridSource.
     */
    @Test
    public void rowSelectRange_skipsNonSelectableRows() throws Exception {
        final CapturingColumnDefinition col = new CapturingColumnDefinition();
        final DefaultDataGridView<String, String> v = newView(col, "c");
        setDataOn(v, col, "a", "b", "c", "d", "e");

        getCellControllerFrom(col, 0).selectRow();      // anchor = row 0
        getCellControllerFrom(col, 4).selectRange();    // Shift+Click row 4 (not selected)

        assertSelectedOn(v, "a", "b", "d", "e");
        assertNotSelectedOn(v, "c");
    }

    /**
     * R8 — Shift+Click on an empty row (null key) when a valid anchor exists.
     * CellControllerImpl#selectRange() guards on null key before reaching Row#selectRange,
     * so no selection change occurs. The anchor row "b" remains selected (set by selectRow).
     */
    @Test
    public void rowSelectRange_shiftClickOnEmptyRow_isNoOp() throws Exception {
        setData("a", "b", "c");

        getCellController(1).selectRow();       // anchor = row 1 ("b"), "b" is now selected
        getCellController(4).selectRange();     // row 4 has null key → CellControllerImpl guard fires → no change

        // selectRange() did not alter the state: only "b" is selected (from selectRow)
        assertNotSelected("a", "c");
        assertSelected("b");
    }

    /**
     * R9 — the select/deselect direction is determined by the target row's state, not the anchor.
     * Anchor is row 2 (selected). Target is row 0 (not selected) → range 0–2 is SELECTED.
     */
    @Test
    public void rowSelectRange_directionDeterminedByTargetRow() throws Exception {
        setData("a", "b", "c", "d", "e");

        getCellController(2).selectRow();       // anchor = row 2 (now selected)

        // Shift+Click on row 0 (not selected) → row 0 is the target → selectKeys(0..2)
        getCellController(0).selectRange();

        assertSelected("a", "b", "c");
        assertNotSelected("d", "e");
    }

    // -------------------------------------------------------------------------
    // CellControllerImpl#selectRange() — C tests
    // -------------------------------------------------------------------------

    /**
     * C1 — no-op when the cell's row has a null key (empty slot beyond loaded data).
     */
    @Test
    public void cellSelectRange_nullKey_doesNothing() throws Exception {
        setData("a", "b");

        // Row at index 5 has no data → null key → CellControllerImpl guard fires
        getCellController(5).selectRange();

        assertNotSelected("a", "b");
    }

    /**
     * C2 — no-op when the cell's row key is not selectable.
     */
    @Test
    public void cellSelectRange_notSelectable_doesNothing() throws Exception {
        final CapturingColumnDefinition col = new CapturingColumnDefinition();
        final DefaultDataGridView<String, String> v = newView(col, "b");
        setDataOn(v, col, "a", "b", "c");

        getCellControllerFrom(col, 1).selectRange();    // "b" is not selectable → guard fires

        assertNotSelectedOn(v, "a", "b", "c");
    }

    /**
     * C3 — fallback when no anchor exists and the row is not selected: toggles to selected
     * and sets the row as the new anchor.
     */
    @Test
    public void cellSelectRange_noAnchor_notSelected_selectsAndSetsAnchor() throws Exception {
        setData("a", "b", "c", "d", "e");

        // No prior click → no anchor → fallback path: select row 2, set anchor
        getCellController(2).selectRange();

        assertSelected("c");
        assertNotSelected("a", "b", "d", "e");

        // Anchor is now row 2; a subsequent selectRange() on row 4 should produce range 2–4
        getCellController(4).selectRange();

        assertSelected("c", "d", "e");
        assertNotSelected("a", "b");
    }

    /**
     * C4 — fallback when no anchor exists and the row is already selected: toggles to unselected.
     * Uses a fresh view instance to ensure lastClickedRow is null while "b" is pre-selected
     * directly via the controller (bypassing the anchor update).
     */
    @Test
    public void cellSelectRange_noAnchor_alreadySelected_unselects() throws Exception {
        final CapturingColumnDefinition col = new CapturingColumnDefinition();
        final DefaultDataGridView<String, String> v = newView(col);
        setDataOn(v, col, "a", "b", "c");

        // Select "b" directly via the controller — does not set lastClickedRow
        v.getController().select("b");

        // selectRange() on row 1 ("b"): no anchor → fallback → toggle unselect
        getCellControllerFrom(col, 1).selectRange();

        assertNotSelectedOn(v, "a", "b", "c");
    }

    /**
     * C5 — when a valid anchor exists, delegates to Row#selectRange(), producing the expected range.
     * Same scenario as R3 but triggered through CellControllerImpl.
     */
    @Test
    public void cellSelectRange_withAnchor_delegatesToRowSelectRange() throws Exception {
        setData("a", "b", "c", "d", "e");

        getCellController(0).selectRow();       // anchor = row 0
        getCellController(3).selectRange();     // range 0–3

        assertSelected("a", "b", "c", "d");
        assertNotSelected("e");
    }

    /**
     * C6 — the anchor set by the fallback path is used correctly for a subsequent selectRange().
     * First call: no anchor → row 2 selected, set as anchor.
     * Second call: anchor = row 2, target = row 4 → range 2–4 selected.
     */
    @Test
    public void cellSelectRange_anchorSetByFallback_usedForNextRange() throws Exception {
        setData("a", "b", "c", "d", "e");

        // First call: no anchor, row 2 not selected → toggle select, set anchor to row 2
        getCellController(2).selectRange();
        assertSelected("c");

        // Second call: anchor = row 2, target = row 4 (not selected) → selectKeys(2..4)
        getCellController(4).selectRange();
        assertSelected("c", "d", "e");
        assertNotSelected("a", "b");
    }

    // -------------------------------------------------------------------------
    // Extended mode (cell editor)
    // -------------------------------------------------------------------------

    /** The editor is created when its cell is edited, with the value shown at that moment. */
    @Test
    public void editorIsCreatedOnEditWithTheCurrentValue() throws Exception {
        final LabelColumnDefinition col = new LabelColumnDefinition();
        final DefaultDataGridView<String, String> v = newView(col);
        setDataOn(v, col, "a", "b");
        Assertions.assertNull(editorOf(col.cells.get(1)), "no editor before editing");

        col.controllers.get(1).setExtendedMode();

        final PTextBox editor = editorOf(col.cells.get(1));
        Assertions.assertNotNull(editor, "editor shown once editing");
        Assertions.assertEquals("b", editor.getText());
    }

    /** Leaving the extended mode drops the editor: only cells being edited hold one. */
    @Test
    public void editorIsDroppedWhenEditingEnds() throws Exception {
        final LabelColumnDefinition col = new LabelColumnDefinition();
        final DefaultDataGridView<String, String> v = newView(col);
        setDataOn(v, col, "a", "b");
        col.controllers.get(0).setExtendedMode();
        Assertions.assertNotNull(editorOf(col.cells.get(0)));

        ((ExtendedCellController<String>) col.controllers.get(0)).setPrimaryMode();

        Assertions.assertNull(editorOf(col.cells.get(0)));
        Assertions.assertTrue(col.cells.get(0).asWidget().isVisible(), "the label is shown again");
    }

    /** The PTextBox the editor added to the cell, if any. */
    private static PTextBox editorOf(final LabelCell<String> cell) {
        final PComplexPanel td = (PComplexPanel) cell.asWidget().getParent();
        for (final PWidget w : td) if (w instanceof PTextBox t) return t;
        return null;
    }

    // -------------------------------------------------------------------------
    // Grid structure, rendering, sorting, filtering, configuration and terminal events
    // -------------------------------------------------------------------------

    @Test
    public void setAdapterBuildsHeaderCellsAndRows() {
        final Grid g = new Grid(true);

        Assertions.assertSame(g.adapter, g.grid.getAdapter());
        Assertions.assertEquals(2, g.adapter.headerRows);
        Assertions.assertEquals(2, g.adapter.resizers);
        Assertions.assertEquals(60, g.adapter.createdRows);
        Assertions.assertEquals(30, g.grid.getLiveDataRowCount());
        final PComplexPanel header = g.unpinnedHeader();
        Assertions.assertEquals(2, header.getWidgetCount());
        Assertions.assertEquals("0", header.getWidget(0).getAttribute("data-column-id"));
        Assertions.assertTrue(g.name.getDraggableHeaderElement().asWidget().hasStyleName("pony-grid-draggable-col"));
        Assertions.assertTrue(g.recorder.methods.contains("onColumnAdded"));
        Assertions.assertTrue(g.recorder.methods.contains("listenOnColumnVisibility"));
        Assertions.assertThrows(IllegalStateException.class, () -> g.grid.setAdapter(g.adapter));
    }

    @Test
    public void gridWithoutHeaderCollapsesHeaderRow() {
        final Grid g = new Grid(false);

        Assertions.assertEquals("0px", g.unpinnedHeader().getHeight());
        Assertions.assertEquals("1px", g.unpinnedHeader().getWidget(0).getHeight());
        Assertions.assertEquals(0, g.adapter.headerRows);
    }

    @Test
    public void operationsRequireAnAdapter() {
        final DefaultDataGridView<String, String> noAdapter = new DefaultDataGridView<>();

        Assertions.assertThrows(IllegalStateException.class, noAdapter::getFilteredData);
        Assertions.assertThrows(IllegalStateException.class, noAdapter::getLiveSelectedData);
        Assertions.assertThrows(IllegalStateException.class, noAdapter::getLiveSelectedDataCount);
        Assertions.assertThrows(IllegalStateException.class, () -> noAdapter.getLiveData(0, 1));
        Assertions.assertThrows(IllegalStateException.class, noAdapter::selectAllLiveData);
        Assertions.assertThrows(IllegalStateException.class, noAdapter::unselectAllData);
        Assertions.assertThrows(IllegalStateException.class, () -> noAdapter.setPollingDelayMillis(10));
        Assertions.assertThrows(IllegalStateException.class, noAdapter::getConfig);
        Assertions.assertNull(noAdapter.getAdapter());
    }

    @Test
    public void setDataRendersSortedRowsAndHidesEmptyOnes() throws Exception {
        final Grid g = new Grid(true);

        g.drawAfter(() -> g.grid.getController().setData(List.of("c", "a", "b")));

        Assertions.assertEquals(List.of("a", "b", "c"), g.texts(0));
        Assertions.assertFalse(g.unpinnedRow(3).isVisible());
        Assertions.assertEquals(List.of("a", "b"), new ArrayList<>(g.grid.getLiveData(0, 2)));
        Assertions.assertEquals(List.of("a", "b", "c"), g.grid.getFilteredData().getData());
        Assertions.assertTrue(g.recorder.methods.contains("onDataUpdated"));
        Assertions.assertTrue(g.recorder.methods.contains("hideLoading"));
        Assertions.assertTrue(g.recorder.methods.contains("updateRowHeight"));
        Assertions.assertNull(g.grid.encodeConfigCustomValue("k", "v"));
        Assertions.assertNull(g.grid.decodeConfigCustomValue("k", "v"));
    }

    @Test
    public void rowClickTogglesSelectionAndShiftClickSelectsRange() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "b", "c", "d")));

        DomEvents.click(g.unpinnedRow(1), false);
        Assertions.assertTrue(g.grid.getController().isSelected("b"));
        g.drawAfter(() -> g.grid.getController().updateCurrentRows());
        Assertions.assertTrue(g.unpinnedRow(1).hasAttribute(DefaultDataGridView.SELECTED_ATTRIBUTE));
        Assertions.assertTrue(g.pinnedRow(1).hasAttribute(DefaultDataGridView.SELECTED_ATTRIBUTE));
        Assertions.assertTrue(g.adapter.selectedRows > 0);

        DomEvents.click(g.unpinnedRow(3), true);
        Assertions.assertEquals(List.of("b", "c", "d"), g.grid.getLiveSelectedData().getData());
        Assertions.assertEquals(3, g.grid.getLiveSelectedDataCount());

        DomEvents.click(g.unpinnedRow(1), false);
        Assertions.assertFalse(g.grid.getController().isSelected("b"));
        g.drawAfter(() -> g.grid.getController().updateCurrentRows());
        Assertions.assertFalse(g.unpinnedRow(1).hasAttribute(DefaultDataGridView.SELECTED_ATTRIBUTE));

        DomEvents.click(g.unpinnedRow(10), false);
        Assertions.assertEquals(2, g.grid.getLiveSelectedDataCount());
    }

    @Test
    public void selectAllAndUnselectAllUpdateRows() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "b")));

        g.grid.selectAllLiveData();
        Assertions.assertEquals(2, g.grid.getLiveSelectedDataCount());
        Assertions.assertTrue(g.unpinnedRow(0).hasAttribute(DefaultDataGridView.SELECTED_ATTRIBUTE));

        g.grid.unselectAllData();
        Assertions.assertEquals(0, g.grid.getLiveSelectedDataCount());
        Assertions.assertFalse(g.unpinnedRow(0).hasAttribute(DefaultDataGridView.SELECTED_ATTRIBUTE));
    }

    @Test
    public void generalSortOfViewReordersRows() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "b", "c")));

        g.drawAfter(() -> g.grid.addSort("desc", Comparator.reverseOrder()));
        Assertions.assertEquals(List.of("c", "b", "a"), g.texts(0));

        g.drawAfter(() -> g.grid.clearSort("desc"));
        Assertions.assertEquals(List.of("a", "b", "c"), g.texts(0));
    }

    @Test
    public void generalFiltersOfViewHideRejectedRows() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "b", "c")));

        g.drawAfter(() -> g.grid.setFilter(filter("noB", v -> !v.equals("b"))));
        Assertions.assertEquals(List.of("a", "c"), g.texts(0));
        Assertions.assertTrue(g.recorder.methods.contains("scrollToTop"));

        g.drawAfter(() -> g.grid.clearFilter("noB"));
        Assertions.assertEquals(List.of("a", "b", "c"), g.texts(0));

        g.drawAfter(() -> g.grid.setFilters(List.of(filter("noA", v -> !v.equals("a")), filter("noC", v -> !v.equals("c")))));
        Assertions.assertEquals(List.of("b"), g.texts(0));

        g.drawAfter(() -> g.grid.clearFilters(List.of("noA")));
        Assertions.assertEquals(List.of("a", "b"), g.texts(0));

        g.drawAfter(() -> g.grid.clearFilters());
        Assertions.assertEquals(List.of("a", "b", "c"), g.texts(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void columnControllerSortsAndNotifiesListeners() throws Exception {
        final Grid g = new Grid(true);
        final ColumnActionListener<String> listener = Mockito.mock(ColumnActionListener.class);
        g.grid.addColumnActionListener(g.name, listener);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "bb", "ccc")));

        g.drawAfter(() -> g.name.getController().sort(false));
        Assertions.assertEquals(List.of("ccc", "bb", "a"), g.texts(0));
        Mockito.verify(listener).onSort(false);

        g.drawAfter(() -> g.name.getController().clearSort());
        Assertions.assertEquals(List.of("a", "bb", "ccc"), g.texts(0));
        Mockito.verify(listener).onClearSort();

        g.grid.clearSorts(true);
        Mockito.verify(listener, Mockito.times(2)).onClearSort();
        g.grid.clearSorts(false);
        Mockito.verify(listener, Mockito.times(2)).onClearSort();

        g.grid.removeColumnActionListener(g.name, listener);
        g.grid.removeColumnActionListener(new DefaultColumnDefinition<>("unknown", v -> v, null), listener);
        g.grid.clearSorts(true);
        Mockito.verify(listener, Mockito.times(2)).onClearSort();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void columnControllerFiltersAndNotifiesListeners() throws Exception {
        final Grid g = new Grid(true);
        final ColumnActionListener<String> listener = Mockito.mock(ColumnActionListener.class);
        g.grid.addColumnActionListener(g.name, listener);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "b", "c")));
        final BiPredicate<String, Supplier<Object>> noB = (v, helper) -> !"b".equals(helper.get());

        g.drawAfter(() -> g.name.getController().filter("k", noB, true, false));
        Assertions.assertEquals(List.of("a", "c"), g.texts(0));
        Mockito.verify(listener).onFilter("k", noB, false);

        g.drawAfter(() -> g.name.getController().clearFilter("k"));
        Assertions.assertEquals(List.of("a", "b", "c"), g.texts(0));
        Mockito.verify(listener).onClearFilter("k");

        g.drawAfter(() -> g.name.getController().filter("k", noB, true, false));
        g.drawAfter(() -> g.name.getController().clearFilters());
        Assertions.assertEquals(List.of("a", "b", "c"), g.texts(0));
        Mockito.verify(listener).onClearFilters();

        g.drawAfter(() -> g.name.getController().redraw(true));
        Mockito.verify(listener).onRedraw(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void columnControllerIgnoresUnsupportedActions() {
        final Grid g = new Grid(true, new CapturingColumnDefinition());
        final ColumnDefinition<String> fixed = g.adapter.getColumnDefinitions().get(2);
        final ColumnActionListener<String> listener = Mockito.mock(ColumnActionListener.class);
        g.grid.addColumnActionListener(fixed, listener);

        fixed.getController().sort(true);
        fixed.getController().clearSort();
        fixed.getController().filter("k", (v, h) -> false, true, false);
        fixed.getController().clearFilter("k");
        fixed.getController().clearFilters();

        Mockito.verifyNoInteractions(listener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void columnStateChangesMoveColumnsBetweenTables() {
        final Grid g = new Grid(true);
        final ColumnActionListener<String> listener = Mockito.mock(ColumnActionListener.class);
        g.grid.addColumnActionListener(g.name, listener);

        g.name.getController().setState(State.PINNED_SHOWN);
        Assertions.assertEquals(1, g.pinnedHeader().getWidgetCount());
        Assertions.assertEquals(1, g.unpinnedHeader().getWidgetCount());
        Assertions.assertTrue(g.pinnedHeader().getWidget(0).hasAttribute(DefaultDataGridView.PINNED_ATTRIBUTE));
        Assertions.assertEquals(1, g.pinnedRow(0).getWidgetCount());
        Assertions.assertEquals("Unpin", ((PLabel) g.name.getHeader().getWidget(1)).getText());
        Assertions.assertEquals(List.of("name:PINNED_SHOWN", "len:UNPINNED_SHOWN"), columnStates(g.grid.getConfig()));
        Mockito.verify(listener).onStateChanged(State.PINNED_SHOWN);

        g.name.getController().setState(State.PINNED_SHOWN);
        Mockito.verify(listener, Mockito.times(1)).onStateChanged(State.PINNED_SHOWN);

        g.name.getController().setState(State.UNPINNED_SHOWN);
        Assertions.assertEquals(0, g.pinnedHeader().getWidgetCount());
        Assertions.assertEquals(2, g.unpinnedRow(0).getWidgetCount());
        Assertions.assertFalse(g.unpinnedHeader().getWidget(0).hasAttribute(DefaultDataGridView.PINNED_ATTRIBUTE));

        g.name.getController().setState(State.UNPINNED_HIDDEN);
        Assertions.assertEquals(1, g.unpinnedHeader().getWidgetCount());
        Assertions.assertEquals(1, g.unpinnedRow(0).getWidgetCount());
        Assertions.assertEquals(List.of("len:UNPINNED_SHOWN", "name:UNPINNED_HIDDEN"), columnStates(g.grid.getConfig()));
        Mockito.verify(listener).onClearSort();

        g.name.getController().setState(State.UNPINNED_SHOWN);
        Assertions.assertEquals(2, g.unpinnedHeader().getWidgetCount());
        Assertions.assertEquals(List.of("len:UNPINNED_SHOWN", "name:UNPINNED_SHOWN"), columnStates(g.grid.getConfig()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setConfigAppliesColumnsAndSortsAndNotifiesListeners() throws Exception {
        final Grid g = new Grid(true);
        final ColumnActionListener<String> nameListener = Mockito.mock(ColumnActionListener.class);
        final ColumnActionListener<String> lenListener = Mockito.mock(ColumnActionListener.class);
        g.grid.addColumnActionListener(g.name, nameListener);
        g.grid.addColumnActionListener(g.len, lenListener);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "b", "c")));
        final DataGridConfig<String> config = new DataGridConfigBuilder<String>()
            .addColumnConfig(new ColumnConfig<>("len", State.PINNED_SHOWN, 200))
            .addColumnConfig(new ColumnConfig<>("unknown", State.UNPINNED_SHOWN, 10))
            .addColumnConfig(new ColumnConfig<>("name", null, 90))
            .addSort(new ColumnSort<>("name", false))
            .addSort(new ColumnSort<>("unknown", true))
            .addSort(new GeneralSort<>("general", Comparator.<String> naturalOrder()))
            .addSort(null)
            .build();

        g.grid.setConfig(config);

        Assertions.assertTrue(waitUntil(() -> List.of("c", "b", "a").equals(g.texts(0))));
        final DataGridConfig<String> current = g.grid.getConfig();
        Assertions.assertEquals(List.of("len:PINNED_SHOWN", "name:UNPINNED_SHOWN"), columnStates(current));
        Assertions.assertEquals(200, current.getColumnConfigs().get(0).getWidth());
        Assertions.assertEquals(90, current.getColumnConfigs().get(1).getWidth());
        Assertions.assertEquals("name", ((ColumnSort<String>) current.getSorts().get(0)).getColumnId());
        Mockito.verify(nameListener).onClearSort();
        Mockito.verify(nameListener).onClearFilters();
        Mockito.verify(nameListener).onSort(false);
        Mockito.verify(lenListener).onStateChanged(State.PINNED_SHOWN);
        Assertions.assertTrue(g.recorder.methods.contains("scrollToTop"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setConfigNotifiesResizedColumns() {
        final Grid g = new Grid(true);
        final ColumnActionListener<String> lenListener = Mockito.mock(ColumnActionListener.class);
        g.grid.addColumnActionListener(g.len, lenListener);

        g.grid.setConfig(new DataGridConfigBuilder<String>().addColumnConfig(new ColumnConfig<>("len", State.UNPINNED_SHOWN, 200)).build());

        Mockito.verify(lenListener).onResized(200);
    }

    @Test
    public void setConfigCanHideColumns() {
        final Grid g = new Grid(true);

        g.grid.setConfig(new DataGridConfigBuilder<String>().addColumnConfig(new ColumnConfig<>("name", State.UNPINNED_HIDDEN, 100)).build());

        Assertions.assertEquals(1, g.unpinnedHeader().getWidgetCount());
        Assertions.assertEquals(List.of("len:UNPINNED_SHOWN", "name:UNPINNED_HIDDEN"), columnStates(g.grid.getConfig()));
    }

    @Test
    public void rowActionsAreAppliedAndCancelled() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "b", "c")));
        final AttributeRowAction onlyB = new AttributeRowAction("b");

        g.grid.addRowAction("hl", onlyB);
        Assertions.assertTrue(g.unpinnedRow(1).hasAttribute("hl"));
        Assertions.assertTrue(g.pinnedRow(1).hasAttribute("hl"));
        Assertions.assertFalse(g.unpinnedRow(0).hasAttribute("hl"));

        g.drawAfter(() -> g.grid.getController().setData("0"));
        Assertions.assertTrue(g.unpinnedRow(2).hasAttribute("hl"));
        Assertions.assertFalse(g.unpinnedRow(1).hasAttribute("hl"));

        g.grid.addRowAction("hl", new AttributeRowAction("zz"));
        Assertions.assertFalse(g.unpinnedRow(2).hasAttribute("hl"));

        g.grid.addRowAction("hl", onlyB);
        g.grid.clearRowAction("hl");
        g.grid.clearRowAction("unknown");
        Assertions.assertFalse(g.unpinnedRow(2).hasAttribute("hl"));
    }

    @Test
    public void pausedGridDrawsOnResume() throws Exception {
        final Grid g = new Grid(true);
        g.grid.pause();
        final CountDownLatch latch = new CountDownLatch(1);
        g.grid.addDrawListener(count -> latch.countDown());

        g.grid.getController().setData(List.of("a"));
        Assertions.assertFalse(latch.await(200, TimeUnit.MILLISECONDS));

        g.grid.resume();
        Assertions.assertTrue(latch.await(DRAW_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        Assertions.assertEquals(List.of("a"), g.texts(0));

        g.grid.resume();
    }

    @Test
    public void pollingDrawsPeriodicallyUntilStopped() throws Exception {
        final Grid g = new Grid(true);

        g.grid.setPollingDelayMillis(20);
        g.grid.setPollingDelayMillis(20);
        final int before = g.recorder.count("hideLoading");
        Assertions.assertTrue(waitUntil(() -> g.recorder.count("hideLoading") >= before + 2));

        g.grid.getController().setData("a");
        Assertions.assertTrue(waitUntil(() -> List.of("a").equals(g.texts(0))));

        g.grid.setPollingDelayMillis(0);
        Thread.sleep(100);
        final int stopped = g.recorder.count("hideLoading");
        Thread.sleep(150);
        Assertions.assertEquals(stopped, g.recorder.count("hideLoading"));
    }

    @Test
    public void sourceFailureShowsErrorMessage() throws Exception {
        final DefaultCacheDataSource<String, String> failing = new DefaultCacheDataSource<>() {

            @Override
            public com.ponysdk.core.ui.datagrid2.data.LiveDataView<String> getRows(final int from, final int size) {
                throw new IllegalStateException("boom");
            }
        };
        failing.setFilterController(new FilterController<>());
        final Grid g = new Grid(true, failing);
        g.grid.setExceptionHandler(t -> "failed: " + t.getMessage());

        g.grid.getController().setData("a");

        Assertions.assertTrue(waitUntil(() -> g.errorDiv().isVisible() && g.errorDiv().getWidgetCount() == 1));
        Assertions.assertEquals("failed: boom", ((PElement) g.errorDiv().getWidget(0)).getInnerText());
    }

    @Test
    public void relativeRowCountGrowsAndShrinksRows() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "b")));

        g.drawAfter(() -> g.terminal(Json.createObjectBuilder().add("rc", 40)));
        Assertions.assertEquals(42, g.grid.getLiveDataRowCount());
        Assertions.assertEquals(42, g.unpinnedBody().getWidgetCount());
        Assertions.assertTrue(g.recorder.methods.contains("checkPosition"));

        g.drawAfter(() -> g.terminal(Json.createObjectBuilder().add("rc", 31)));
        Assertions.assertEquals(33, g.grid.getLiveDataRowCount());
        Assertions.assertEquals(33, g.pinnedBody().getWidgetCount());

        final int hides = g.recorder.count("hideLoading");
        g.terminal(Json.createObjectBuilder().add("rc", 33));
        g.terminal(Json.createObjectBuilder().add("rc", 0));
        Assertions.assertTrue(g.recorder.count("hideLoading") >= hides + 2);
        Assertions.assertEquals(List.of("a", "b"), g.texts(0));
    }

    @Test
    public void scrollEventShiftsDisplayedRows() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "b", "c")));

        g.drawAfter(() -> g.terminal(Json.createObjectBuilder().add("row", 1)));

        Assertions.assertEquals(List.of("b", "c"), g.texts(0));

        g.grid.scrollTo(2);
        Assertions.assertTrue(g.recorder.methods.contains("scrollTo"));
    }

    @Test
    public void columnVisibilityEventShowsPendingCells() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a")));
        final PComplexPanel cell = (PComplexPanel) g.unpinnedRow(0).getWidget(0);

        g.terminal(Json.createObjectBuilder().add("col", Json.createArrayBuilder().add(0)).add("cv", Json.createArrayBuilder().add(false)));
        Assertions.assertTrue(cell.hasAttribute(DefaultDataGridView.PENDING_ATTRIBUTE));
        Assertions.assertTrue(cell.getWidget(0).isVisible());
        Assertions.assertFalse(cell.getWidget(1).isVisible());

        g.terminal(Json.createObjectBuilder().add("col", Json.createArrayBuilder().add(0)).add("cv", Json.createArrayBuilder().add(true)));
        Assertions.assertFalse(cell.hasAttribute(DefaultDataGridView.PENDING_ATTRIBUTE));
        Assertions.assertTrue(cell.getWidget(1).isVisible());

        g.grid.setRefreshOnColumnVisibilityChanged(false);
        Assertions.assertEquals(2, g.recorder.count("listenOnColumnVisibility"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void columnResizeEventAppliesClampedWidth() {
        final Grid g = new Grid(true, new CapturingColumnDefinition());
        final ColumnActionListener<String> listener = Mockito.mock(ColumnActionListener.class);
        g.grid.addColumnActionListener(g.name, listener);

        g.terminal(Json.createObjectBuilder().add("col", 0).add("cw", 200));
        Assertions.assertEquals("200px", g.unpinnedHeader().getWidget(0).getWidth());
        Assertions.assertEquals("200px", g.unpinnedRow(0).getWidget(0).getWidth());
        Assertions.assertEquals(200, g.grid.getConfig().getColumnConfigs().get(0).getWidth());
        Mockito.verify(listener).onResized(200);

        g.terminal(Json.createObjectBuilder().add("col", 0).add("cw", 200));
        g.terminal(Json.createObjectBuilder().add("col", 0).add("cw", 10));
        Assertions.assertEquals("50px", g.unpinnedHeader().getWidget(0).getWidth());
        g.terminal(Json.createObjectBuilder().add("col", 0).add("cw", 20));
        Mockito.verify(listener, Mockito.times(2)).onResized(Mockito.anyInt());

        g.terminal(Json.createObjectBuilder().add("col", 2).add("cw", 250));
        Assertions.assertNull(g.unpinnedHeader().getWidget(2).getWidth());
        Assertions.assertEquals(100, g.grid.getConfig().getColumnConfigs().get(2).getWidth());

        g.name.getController().setState(State.PINNED_SHOWN);
        g.terminal(Json.createObjectBuilder().add("col", 0).add("cw", 120));
        Assertions.assertEquals("120px", g.pinnedHeader().getWidget(0).getWidth());
        Assertions.assertEquals("120px", g.pinnedRow(0).getWidth());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void columnMoveEventReordersUnpinnedColumns() {
        final Grid g = new Grid(true, new CapturingColumnDefinition());
        final ColumnActionListener<String> listener = Mockito.mock(ColumnActionListener.class);
        g.grid.addColumnActionListener(g.name, listener);

        g.terminal(Json.createObjectBuilder().add("col", 0).add("to", 1));

        Assertions.assertEquals(List.of("len", "name", "test-column"), columnIds(g.grid.getConfig()));
        Assertions.assertEquals("1", g.unpinnedHeader().getWidget(0).getAttribute("data-column-id"));
        Mockito.verify(listener).onMoved();

        g.terminal(Json.createObjectBuilder().add("col", 0).add("to", 0));
        g.terminal(Json.createObjectBuilder().add("col", 0).add("to", 2));
        g.terminal(Json.createObjectBuilder().add("col", 2).add("to", 0));
        g.len.getController().setState(State.PINNED_SHOWN);
        g.terminal(Json.createObjectBuilder().add("col", 1).add("to", 0));
        g.terminal(Json.createObjectBuilder().add("col", 0).add("to", 1));
        Assertions.assertEquals(List.of("len", "name", "test-column"), columnIds(g.grid.getConfig()));
        Mockito.verify(listener, Mockito.times(1)).onMoved();
    }

    @Test
    public void destroyingTheGridDestroysItsAddon() {
        final Grid g = new Grid(true);
        g.grid.setPollingDelayMillis(1000);

        g.grid.asWidget().onDestroy();

        Assertions.assertTrue(g.recorder.methods.contains("destroy"));
    }

    @Test
    public void forcedExtendedRowsRequestExtendedHeight() throws Exception {
        final Grid g = new Grid(true);
        g.grid.setForceExtended(true);

        g.drawAfter(() -> g.grid.getController().setData(List.of("a")));

        Assertions.assertTrue(g.recorder.methods.contains("updateExtendedRowHeight"));
    }

    @Test
    public void editedRowRequestsExtendedHeightAndCanUpdateValues() throws Exception {
        final LabelColumnDefinition col = new LabelColumnDefinition();
        final Grid g = new Grid(true, col);
        g.drawAfter(() -> g.grid.getController().setData(List.of("b", "c")));
        final ExtendedCellController<String> controller = (ExtendedCellController<String>) col.controllers.get(0);
        col.controllers.get(0).setExtendedMode();
        Assertions.assertTrue(((PComplexPanel) g.unpinnedRow(0).getWidget(0)).hasAttribute(DefaultDataGridView.EXTENDED_ATTRIBUTE));

        g.drawAfter(() -> controller.updateValue("a"));
        Assertions.assertTrue(g.recorder.methods.contains("updateExtendedRowHeight"));
        controller.setPrimaryMode();
        Assertions.assertEquals(List.of("a", "b", "c"), g.texts(0));

        final List<String> updated = new ArrayList<>();
        controller.updateValue(updated::add);
        Assertions.assertEquals(List.of("a"), updated);

        controller.selectRow();
        Assertions.assertTrue(g.grid.getController().isSelected("a"));
        controller.unselectRow();
        Assertions.assertFalse(g.grid.getController().isSelected("a"));
    }

    @Test
    public void cellModesOnEmptyRowAreIgnored() {
        final LabelColumnDefinition col = new LabelColumnDefinition();
        final Grid g = new Grid(true, col);

        col.controllers.get(5).setExtendedMode();
        ((ExtendedCellController<String>) col.controllers.get(5)).setPrimaryMode();

        Assertions.assertFalse(((PComplexPanel) g.unpinnedRow(5).getWidget(0)).hasAttribute(DefaultDataGridView.EXTENDED_ATTRIBUTE));
    }

    @Test
    public void destroyingWithoutPollingNorAdapterIsSafe() {
        final Grid g = new Grid(true);
        g.grid.asWidget().onDestroy();
        Assertions.assertTrue(g.recorder.methods.contains("destroy"));

        final DefaultDataGridView<String, String> noAdapter = new DefaultDataGridView<>();
        noAdapter.asWidget().onDestroy();
        Assertions.assertNull(noAdapter.asWidget().getWindow());
    }

    @Test
    public void aRowWithoutKeyIgnoresClicksAndIsLeftOutOfARange() throws Exception {
        final DefaultCacheDataSource<String, String> source = new DefaultCacheDataSource<>();
        source.setFilterController(new FilterController<>());
        final DefaultDataGridView<String, String> v = new DefaultDataGridView<>(source);
        v.setAdapter(new TestAdapter(List.of(new CapturingColumnDefinition())) {

            @Override
            public String getKey(final String data) {
                return "b".equals(data) ? null : data;
            }
        });
        final CountDownLatch drawn = new CountDownLatch(1);
        v.addDrawListener(count -> drawn.countDown());
        v.getController().setData(List.of("a", "b", "c"));
        Assertions.assertTrue(drawn.await(DRAW_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        final PComplexPanel root = (PComplexPanel) v.asWidget();
        final PComplexPanel body = (PComplexPanel) ((PComplexPanel) ((PComplexPanel) root.getWidget(1)).getWidget(0)).getWidget(1);

        DomEvents.click(body.getWidget(1), false);
        Assertions.assertEquals(0, v.getLiveSelectedDataCount());

        DomEvents.click(body.getWidget(0), false);
        DomEvents.click(body.getWidget(2), true);
        Assertions.assertEquals(List.of("a", "c"), v.getLiveSelectedData().getData());
    }

    @Test
    public void aSortThenTheClearOfAnUnsortedColumnBeforeTheDrawStillDraws() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("c", "a", "b")));
        final CountDownLatch drawn = new CountDownLatch(1);
        g.grid.addDrawListener(count -> drawn.countDown());
        final UIContext context = UIContext.get();

        // Both client events processed before the draw they schedule runs: it waits for the context held here
        context.acquire();
        try {
            g.name.getController().sort(true);
            g.len.getController().clearSort();
        } finally {
            context.release();
            UIContext.setCurrent(context);
        }

        Assertions.assertTrue(drawn.await(DRAW_TIMEOUT_MS, TimeUnit.MILLISECONDS), "the sort is drawn");
        Assertions.assertEquals(List.of("a", "b", "c"), g.texts(0));
    }

    @Test
    public void aDrawOutsideOfAUIContextIsLogged() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a")));
        final UIContext context = UIContext.get();

        UIContext.remove();
        try (LogCapture log = LogCapture.of(DefaultDataGridView.class)) {
            g.grid.refresh();
            Assertions.assertEquals(List.of("ERROR Cannot draw data from data source"), log.events());
        } finally {
            UIContext.setCurrent(context);
        }
    }

    @Test
    public void columnVisibilityEventWithoutChangeDoesNotRedraw() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a")));
        final List<Integer> draws = new ArrayList<>();
        g.grid.addDrawListener(draws::add);

        g.terminal(Json.createObjectBuilder().add("col", Json.createArrayBuilder().add(0).add(1))
            .add("cv", Json.createArrayBuilder().add(true).add(true)));
        Assertions.assertTrue(draws.isEmpty());

        g.terminal(Json.createObjectBuilder().add("col", Json.createArrayBuilder().add(0).add(1))
            .add("cv", Json.createArrayBuilder().add(false).add(false)));
        Assertions.assertEquals(1, draws.size());
        Assertions.assertTrue(((PComplexPanel) g.unpinnedRow(0).getWidget(1)).hasAttribute(DefaultDataGridView.PENDING_ATTRIBUTE));
    }

    @Test
    public void updateRowsWithAnEmptyIntervalOnlyNotifies() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a")));
        final List<Integer> draws = new ArrayList<>();
        g.grid.addDrawListener(draws::add);

        g.grid.onUpdateRows(5, 2);
        g.grid.onUpdateRows(0, 1000);

        Assertions.assertEquals(List.of(30, 30), draws);
        Assertions.assertEquals(List.of("a"), g.texts(0));
    }

    @Test
    public void sourceFailureWithoutHandlerShowsEmptyMessage() throws Exception {
        final DefaultCacheDataSource<String, String> failing = new DefaultCacheDataSource<>() {

            @Override
            public com.ponysdk.core.ui.datagrid2.data.LiveDataView<String> getRows(final int from, final int size) {
                throw new IllegalStateException("boom");
            }
        };
        failing.setFilterController(new FilterController<>());
        final Grid g = new Grid(true, failing);

        g.grid.getController().setData("a");

        Assertions.assertTrue(waitUntil(() -> g.errorDiv().isVisible() && g.errorDiv().getWidgetCount() == 1));
        Assertions.assertEquals("", ((PElement) g.errorDiv().getWidget(0)).getInnerText());
    }

    @Test
    public void renderingFailureIsGivenToTheExceptionHandler() throws Exception {
        final Grid g = new Grid(true);
        final IllegalStateException failure = new IllegalStateException("bad row");
        final List<Throwable> handled = new ArrayList<>();
        g.grid.setExceptionHandler(t -> {
            handled.add(t);
            return "failed: " + t.getMessage();
        });
        g.grid.addRowAction("throwing", new AttributeRowAction("a") {

            @Override
            public boolean testRow(final String v, final int index) {
                throw failure;
            }
        });

        g.grid.getController().setData("a");

        Assertions.assertTrue(waitUntil(() -> g.errorDiv().isVisible() && g.errorDiv().getWidgetCount() == 1));
        Assertions.assertEquals("failed: bad row", ((PElement) g.errorDiv().getWidget(0)).getInnerText());
        Assertions.assertEquals(List.of(failure), handled);
    }

    @Test
    public void renderingFailureWithoutHandlerShowsEmptyMessage() throws Exception {
        final Grid g = new Grid(true);
        g.grid.addRowAction("throwing", new AttributeRowAction("a") {

            @Override
            public boolean testRow(final String v, final int index) {
                throw new IllegalStateException("bad row");
            }
        });

        g.grid.getController().setData("a");

        Assertions.assertTrue(waitUntil(() -> g.errorDiv().isVisible() && g.errorDiv().getWidgetCount() == 1));
        Assertions.assertEquals("", ((PElement) g.errorDiv().getWidget(0)).getInnerText());
    }

    @Test
    public void hiddenByDefaultColumnIsNotDisplayed() {
        final DefaultColumnDefinition<String> hidden = new DefaultColumnDefinition<>("hidden", v -> v, null) {

            @Override
            public State getDefaultState() {
                return State.UNPINNED_HIDDEN;
            }
        };
        final Grid g = new Grid(true, hidden);

        Assertions.assertEquals(2, g.unpinnedHeader().getWidgetCount());
        Assertions.assertEquals(2, g.unpinnedRow(0).getWidgetCount());
        Assertions.assertEquals(List.of("name:UNPINNED_SHOWN", "len:UNPINNED_SHOWN", "hidden:UNPINNED_HIDDEN"),
            columnStates(g.grid.getConfig()));
    }

    @Test
    public void setConfigAppliesTheFirstConfigOfAColumn() {
        final Grid g = new Grid(true);

        g.grid.setConfig(new DataGridConfigBuilder<String>()
            .addColumnConfig(new ColumnConfig<>("len", State.PINNED_SHOWN, 100))
            .addColumnConfig(new ColumnConfig<>("len", State.UNPINNED_HIDDEN, 100))
            .build());

        Assertions.assertEquals(1, g.pinnedHeader().getWidgetCount());
        Assertions.assertEquals(List.of("len:PINNED_SHOWN", "name:UNPINNED_SHOWN"), columnStates(g.grid.getConfig()));
    }

    @Test
    public void refreshOnColumnVisibilitySetBeforeAdapterIsSentWithTheAddon() {
        final RecordingWriter recorder = new RecordingWriter();
        Mockito.doReturn(recorder).when(UIContext.get()).getWriter();
        final DefaultCacheDataSource<String, String> source = new DefaultCacheDataSource<>();
        source.setFilterController(new FilterController<>());
        final DefaultDataGridView<String, String> grid = new DefaultDataGridView<>(source);

        grid.setRefreshOnColumnVisibilityChanged(false);
        Assertions.assertTrue(recorder.methods.isEmpty());

        grid.setAdapter(new TestAdapter(List.of(new CapturingColumnDefinition())));
        PWindow.getMain().add(grid.asWidget());
        Assertions.assertEquals(1, recorder.count("listenOnColumnVisibility"));
    }

    @Test
    public void nonSelectableRowsIgnoreClicksAndSelection() throws Exception {
        final CapturingColumnDefinition col = new CapturingColumnDefinition();
        final DefaultDataGridView<String, String> v = newView(col, "b");
        setDataOn(v, col, "a", "b");
        final PComplexPanel unpinnedBody = (PComplexPanel) ((PComplexPanel) ((PComplexPanel) ((PComplexPanel) v.asWidget())
            .getWidget(1)).getWidget(0)).getWidget(1);

        DomEvents.click(unpinnedBody.getWidget(1), false);
        assertNotSelectedOn(v, "b");

        getCellControllerFrom(col, 1).selectRow();
        assertNotSelectedOn(v, "b");
        getCellControllerFrom(col, 1).unselectRow();
        assertNotSelectedOn(v, "b");
    }

    @Test
    public void shiftClickWithoutAnchorIsNoOp() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "b", "c")));

        DomEvents.click(g.unpinnedRow(1), true);
        Assertions.assertEquals(0, g.grid.getLiveSelectedDataCount());

        // the anchor row becomes empty: a range from it is a no-op
        DomEvents.click(g.unpinnedRow(2), false);
        g.grid.getController().unselect("c");
        g.drawAfter(() -> g.grid.getController().removeData("c"));
        DomEvents.click(g.unpinnedRow(0), true);
        Assertions.assertEquals(0, g.grid.getLiveSelectedDataCount());
    }

    @Test
    public void extendedModeWithoutExtendedCellKeepsThePrimaryCell() throws Exception {
        setData("a");
        final ExtendedCellController<String> controller = (ExtendedCellController<String>) getCellController(0);

        ((PrimaryCellController<String>) getCellController(0)).setExtendedMode();
        getCellController(0).selectRow();
        assertSelected("a");
        controller.setPrimaryMode();
        assertSelected("a");
    }

    @Test
    public void extendedCellFollowsSelectionAndColumnVisibility() throws Exception {
        final LabelColumnDefinition col = new LabelColumnDefinition();
        final Grid g = new Grid(true, col);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a")));
        col.controllers.get(0).setExtendedMode();
        final PComplexPanel cell = (PComplexPanel) g.unpinnedRow(0).getWidget(0);
        Assertions.assertEquals(3, cell.getWidgetCount());
        final PWidget extended = cell.getWidget(2);

        col.controllers.get(0).selectRow();
        Assertions.assertTrue(g.grid.getController().isSelected("a"));
        col.controllers.get(0).unselectRow();
        Assertions.assertFalse(g.grid.getController().isSelected("a"));

        g.terminal(Json.createObjectBuilder().add("col", Json.createArrayBuilder().add(0)).add("cv", Json.createArrayBuilder().add(false)));
        Assertions.assertFalse(extended.isVisible());
        Assertions.assertFalse(cell.hasAttribute(DefaultDataGridView.EXTENDED_ATTRIBUTE));

        g.terminal(Json.createObjectBuilder().add("col", Json.createArrayBuilder().add(0)).add("cv", Json.createArrayBuilder().add(true)));
        Assertions.assertTrue(extended.isVisible());
        Assertions.assertTrue(cell.hasAttribute(DefaultDataGridView.EXTENDED_ATTRIBUTE));
    }

    @Test
    public void hidingAPinnedColumnUnpinsAndHidesIt() {
        final Grid g = new Grid(true);
        g.name.getController().setState(State.PINNED_SHOWN);

        g.name.getController().setState(State.UNPINNED_HIDDEN);

        Assertions.assertEquals(0, g.pinnedHeader().getWidgetCount());
        Assertions.assertEquals(0, g.pinnedRow(0).getWidgetCount());
        Assertions.assertEquals(1, g.unpinnedHeader().getWidgetCount());
        Assertions.assertEquals(1, g.unpinnedRow(0).getWidgetCount());
        Assertions.assertEquals(List.of("len:UNPINNED_SHOWN", "name:UNPINNED_HIDDEN"), columnStates(g.grid.getConfig()));

        g.name.getController().setState(State.PINNED_SHOWN);
        Assertions.assertEquals(1, g.pinnedHeader().getWidgetCount());
        Assertions.assertEquals(1, g.pinnedRow(0).getWidgetCount());
        Assertions.assertEquals(1, g.unpinnedHeader().getWidgetCount());
    }

    @Test
    public void unknownTerminalDataIsIgnored() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a")));
        final int calls = g.recorder.methods.size();

        g.terminal(Json.createObjectBuilder().add("unknown", 1));

        Assertions.assertEquals(calls, g.recorder.methods.size());
        Assertions.assertEquals(List.of("a"), g.texts(0));
    }

    @Test
    public void nonSelectableRowIgnoresClick() throws Exception {
        final DefaultCacheDataSource<String, String> source = new DefaultCacheDataSource<>() {

            @Override
            public boolean isSelectable(final String key) {
                return !"b".equals(key);
            }
        };
        source.setFilterController(new FilterController<>());
        final Grid g = new Grid(true, source);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "b")));

        DomEvents.click(g.unpinnedRow(1), false);
        Assertions.assertFalse(g.grid.getController().isSelected("b"));
        DomEvents.click(g.unpinnedRow(0), false);
        Assertions.assertTrue(g.grid.getController().isSelected("a"));
    }

    @Test
    public void unselectingAnEmptyRowIsNoOp() throws Exception {
        setData("a");
        getCellController(0).selectRow();

        getCellController(4).unselectRow();

        assertSelected("a");
    }

    @Test
    public void extendedModeTransitionsAreIdempotent() throws Exception {
        final LabelColumnDefinition col = new LabelColumnDefinition();
        final Grid g = new Grid(true, col);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a")));
        final PComplexPanel cell = (PComplexPanel) g.unpinnedRow(0).getWidget(0);
        final ExtendedCellController<String> controller = (ExtendedCellController<String>) col.controllers.get(0);

        controller.setPrimaryMode();
        Assertions.assertEquals(2, cell.getWidgetCount());

        col.controllers.get(0).setExtendedMode();
        final PWidget extended = cell.getWidget(2);
        col.controllers.get(0).setExtendedMode();
        Assertions.assertEquals(3, cell.getWidgetCount());
        Assertions.assertSame(extended, cell.getWidget(2));

        controller.setPrimaryMode();
        Assertions.assertEquals(2, cell.getWidgetCount());
        Assertions.assertFalse(cell.hasAttribute(DefaultDataGridView.EXTENDED_ATTRIBUTE));
    }

    private static List<String> columnStates(final DataGridConfig<String> config) {
        final List<String> states = new ArrayList<>();
        config.getColumnConfigs().forEach(c -> states.add(c.getColumnId() + ":" + c.getState()));
        return states;
    }

    private static List<String> columnIds(final DataGridConfig<String> config) {
        final List<String> ids = new ArrayList<>();
        config.getColumnConfigs().forEach(c -> ids.add(c.getColumnId()));
        return ids;
    }

    private static DataGridFilter<String> filter(final String key, final java.util.function.Predicate<String> predicate) {
        final DataGridFilter<String> filter = new DataGridFilter<>();
        filter.setKey(key);
        filter.setId(key);
        filter.setPredicate(predicate);
        filter.setActive(true);
        return filter;
    }

    private static boolean waitUntil(final java.util.function.BooleanSupplier condition) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + DRAW_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) return true;
            Thread.sleep(10);
        }
        return condition.getAsBoolean();
    }

    /** Records the terminal methods called on add-ons, in call order. */
    static final class RecordingWriter extends ModelWriterForTest {

        final List<String> methods = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void write(final ServerToClientModel model, final Object value) {
            if (model == ServerToClientModel.PADDON_METHOD) methods.add((String) value);
            super.write(model, value);
        }

        int count(final String method) {
            synchronized (methods) {
                return (int) methods.stream().filter(method::equals).count();
            }
        }
    }

    /** Sets an attribute on the rows of one value, removes it from the others. */
    private static class AttributeRowAction implements RowAction<String> {

        private final String value;

        AttributeRowAction(final String value) {
            this.value = value;
        }

        @Override
        public boolean testRow(final String v, final int index) {
            return value.equals(v);
        }

        @Override
        public void apply(final IsPWidget row) {
            row.asWidget().setAttribute("hl");
        }

        @Override
        public void cancel(final IsPWidget row) {
            row.asWidget().removeAttribute("hl");
        }

        @Override
        public boolean isActionApplied(final IsPWidget row) {
            return row.asWidget().hasAttribute("hl");
        }
    }

    /** A TestAdapter with a header, counting the callbacks it receives. */
    static class RecordingAdapter extends TestAdapter {

        private final boolean header;
        int headerRows;
        int resizers;
        int createdRows;
        int selectedRows;

        RecordingAdapter(final List<ColumnDefinition<String>> columns, final boolean header) {
            super(columns);
            this.header = header;
        }

        @Override
        public boolean hasHeader() {
            return header;
        }

        @Override
        public void onCreateHeaderRow(final IsPWidget w) {
            headerRows++;
        }

        @Override
        public void onCreateColumnResizer(final IsPWidget w) {
            resizers++;
        }

        @Override
        public void onCreateRow(final IsPWidget w) {
            createdRows++;
        }

        @Override
        public void onSelectRow(final IsPWidget w) {
            selectedRows++;
        }
    }

    /**
     * A grid with a "name" and a "len" {@link DefaultColumnDefinition} (plus optional extra columns), attached to the
     * main window so that its add-on writes to a {@link RecordingWriter}.
     */
    static final class Grid {

        final RecordingWriter recorder = new RecordingWriter();
        final DefaultColumnDefinition<String> name = new DefaultColumnDefinition<>("name", v -> v, (v, s) -> {
        });
        final DefaultColumnDefinition<String> len = new DefaultColumnDefinition<>("len", String::length, (v, s) -> {
        });
        final RecordingAdapter adapter;
        final DefaultDataGridView<String, String> grid;

        @SafeVarargs
        Grid(final boolean header, final ColumnDefinition<String>... extraColumns) {
            this(header, null, extraColumns);
        }

        @SafeVarargs
        Grid(final boolean header, final DefaultCacheDataSource<String, String> source, final ColumnDefinition<String>... extraColumns) {
            Mockito.doReturn(recorder).when(UIContext.get()).getWriter();
            final List<ColumnDefinition<String>> columns = new ArrayList<>();
            if (extraColumns.length == 1 && extraColumns[0] instanceof LabelColumnDefinition) {
                columns.add(extraColumns[0]);
            } else {
                columns.add(name);
                columns.add(len);
                columns.addAll(List.of(extraColumns));
            }
            adapter = new RecordingAdapter(columns, header);
            if (source == null) {
                final DefaultCacheDataSource<String, String> s = new DefaultCacheDataSource<>();
                s.setFilterController(new FilterController<>());
                grid = new DefaultDataGridView<>(s);
            } else {
                grid = new DefaultDataGridView<>(source);
            }
            grid.setAdapter(adapter);
            PWindow.getMain().add(grid.asWidget());
        }

        void drawAfter(final Runnable action) throws InterruptedException {
            GridDraws.drawAfter(grid, action);
        }

        void terminal(final JsonObjectBuilder data) {
            for (final PAddOn addon : grid.asWidget().getAddons()) {
                if (addon.getClass().getSimpleName().equals("Addon")) DomEvents.terminalData(addon, data);
            }
        }

        PComplexPanel root() {
            return (PComplexPanel) grid.asWidget();
        }

        PComplexPanel pinnedHeader() {
            return (PComplexPanel) ((PComplexPanel) root().getWidget(0)).getWidget(0);
        }

        PComplexPanel unpinnedHeader() {
            return (PComplexPanel) ((PComplexPanel) root().getWidget(0)).getWidget(1);
        }

        PComplexPanel pinnedBody() {
            return (PComplexPanel) ((PComplexPanel) ((PComplexPanel) root().getWidget(1)).getWidget(0)).getWidget(0);
        }

        PComplexPanel unpinnedBody() {
            return (PComplexPanel) ((PComplexPanel) ((PComplexPanel) root().getWidget(1)).getWidget(0)).getWidget(1);
        }

        PComplexPanel errorDiv() {
            return (PComplexPanel) ((PComplexPanel) root().getWidget(1)).getWidget(2);
        }

        PComplexPanel pinnedRow(final int index) {
            return (PComplexPanel) pinnedBody().getWidget(index);
        }

        PComplexPanel unpinnedRow(final int index) {
            return (PComplexPanel) unpinnedBody().getWidget(index);
        }

        /** The texts of the visible rows in the given unpinned column. */
        List<String> texts(final int column) {
            final List<String> texts = new ArrayList<>();
            for (final PWidget row : unpinnedBody()) {
                if (!row.isVisible()) continue;
                final PComplexPanel cell = (PComplexPanel) ((PComplexPanel) row).getWidget(column);
                texts.add(((PLabel) cell.getWidget(1)).getText());
            }
            return texts;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Creates a DefaultDataGridView with a single CapturingColumnDefinition and no non-selectable keys. */
    private static DefaultDataGridView<String, String> newView(final CapturingColumnDefinition col,
                                                                final String... nonSelectableKeys) {
        final DefaultCacheDataSource<String, String> source = new DefaultCacheDataSource<>() {
            @Override
            public boolean isSelectable(final String key) {
                for (final String k : nonSelectableKeys) {
                    if (k.equals(key)) return false;
                }
                return true;
            }
        };
        source.setFilterController(new FilterController<>());
        final DefaultDataGridView<String, String> v = new DefaultDataGridView<>(source);
        v.setAdapter(new TestAdapter(List.of(col)));
        return v;
    }

    private void setData(final String... values) throws Exception {
        setDataOn(view, column, values);
    }

    private void setDataOn(final DefaultDataGridView<String, String> v,
                           final CapturingColumnDefinition col,
                           final String... values) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        v.addDrawListener(rowCount -> latch.countDown());
        // Send all data in one batch to produce a single draw event
        v.getController().setData(List.of(values));
        Assertions.assertTrue(latch.await(DRAW_TIMEOUT_MS, TimeUnit.MILLISECONDS), "Timed out waiting for grid draw");
    }

    private CellController<String> getCellController(final int rowIndex) {
        return getCellControllerFrom(column, rowIndex);
    }

    private CellController<String> getCellControllerFrom(final CapturingColumnDefinition col, final int rowIndex) {
        final CellController<String> cc = col.getCellController(rowIndex);
        Assertions.assertNotNull(cc, "No CellController for row index " + rowIndex
                + " — did you call setData() and wait for the draw?");
        return cc;
    }

    private void assertSelected(final String... keys) {
        assertSelectedOn(view, keys);
    }

    private void assertNotSelected(final String... keys) {
        assertNotSelectedOn(view, keys);
    }

    private void assertSelectedOn(final DefaultDataGridView<String, String> v, final String... keys) {
        for (final String key : keys) {
            Assertions.assertTrue(v.getController().isSelected(key), "Expected '" + key + "' to be selected");
        }
    }

    private void assertNotSelectedOn(final DefaultDataGridView<String, String> v, final String... keys) {
        for (final String key : keys) {
            Assertions.assertFalse(v.getController().isSelected(key), "Expected '" + key + "' to not be selected");
        }
    }

    // -------------------------------------------------------------------------
    // Test infrastructure
    // -------------------------------------------------------------------------

    /**
     * A ColumnDefinition that captures the CellController injected into each created cell,
     * indexed by creation order (= row index at creation time).
     * This lets tests call selectRange() / selectRow() programmatically on specific rows.
     */
    static class CapturingColumnDefinition implements ColumnDefinition<String> {

        private final List<CellController<String>> controllers = new ArrayList<>();
        private ColumnController<String> columnController;

        /** Returns the CellController for the cell at position {@code index}, or null if not yet created. */
        CellController<String> getCellController(final int index) {
            return index < controllers.size() ? controllers.get(index) : null;
        }

        @Override
        public PrimaryCell<String> createCell() {
            return new CapturingCell(controllers.size(), controllers);
        }

        @Override
        public Object getRenderingHelper(final String data) {
            return data;
        }

        @Override
        public int compare(final String v1, final Supplier<Object> h1, final String v2, final Supplier<Object> h2) {
            return v1.compareTo(v2);
        }

        @Override
        public IsPWidget getHeader() {
            return Element.newPLabel(getId());
        }

        @Override
        public IsPWidget getDraggableHeaderElement() {
            return null;
        }

        @Override
        public IsPWidget getFooter() {
            return null;
        }

        @Override
        public void setController(final ColumnController<String> controller) {
            this.columnController = controller;
        }

        @Override
        public ColumnController<String> getController() {
            return columnController;
        }

        @Override
        public State getDefaultState() {
            return State.UNPINNED_SHOWN;
        }

        @Override
        public boolean isVisibilitySwitchable() {
            return false;
        }

        @Override
        public boolean isPinnable() {
            return false;
        }

        @Override
        public boolean isFilterable() {
            return false;
        }

        @Override
        public boolean isSortable() {
            return false;
        }

        @Override
        public boolean isResizable() {
            return false;
        }

        @Override
        public String getId() {
            return "test-column";
        }

        @Override
        public int getDefaultWidth() {
            return 100;
        }

        @Override
        public int getMinWidth() {
            return 50;
        }

        @Override
        public int getMaxWidth() {
            return 300;
        }

        @Override
        public String getGroup() {
            return null;
        }

        @Override
        public void onSort(final boolean asc) {
            // nothing to do
        }

        @Override
        public void onClearSort() {
            // nothing to do
        }

        @Override
        public void onFilter(final Object key, final BiPredicate<String, Supplier<Object>> filter, final boolean reinforcing) {
            // nothing to do
        }

        @Override
        public void onClearFilter(final Object key) {
            // nothing to do
        }

        @Override
        public void onClearFilters() {
            // nothing to do
        }

        @Override
        public void onRedraw(final boolean clearRenderingHelpers) {
            // nothing to do
        }

        @Override
        public void onStateChanged(final State state) {
            // nothing to do
        }

        @Override
        public void onResized(final int width) {
            // nothing to do
        }

        @Override
        public void onMoved() {
            // nothing to do
        }
    }

    /** A column of the framework's {@link LabelCell}s (double-click to edit), capturing cells and controllers. */
    static class LabelColumnDefinition extends CapturingColumnDefinition {

        final List<LabelCell<String>> cells = new ArrayList<>();
        final List<PrimaryCellController<String>> controllers = new ArrayList<>();

        @Override
        public PrimaryCell<String> createCell() {
            final int index = cells.size();
            controllers.add(null);
            final LabelCell<String> cell = new LabelCell<>((value, text) -> {}, 100) {
                @Override
                public void setController(final PrimaryCellController<String> controller) {
                    super.setController(controller);
                    controllers.set(index, controller);
                }
            };
            cells.add(cell);
            return cell;
        }
    }

    /**
     * A minimal PrimaryCell that captures its CellController when setController() is called.
     */
    private static class CapturingCell implements PrimaryCell<String> {

        private final int index;
        private final List<CellController<String>> registry;
        private final PLabel widget = Element.newPLabel();
        private final PLabel pendingWidget = Element.newPLabel("...");

        CapturingCell(final int index, final List<CellController<String>> registry) {
            this.index = index;
            this.registry = registry;
            registry.add(null); // placeholder so the list has the right size at this index
        }

        @Override
        public void setController(final PrimaryCellController<String> cellController) {
            registry.set(index, cellController);
        }

        @Override
        public void render(final String data, final Object renderingHelper) {
            widget.setText(data);
        }

        @Override
        public void select() {
            // nothing to do
        }

        @Override
        public void unselect() {
            // nothing to do
        }

        @Override
        public PWidget asWidget() {
            return widget;
        }

        @Override
        public PWidget asPendingWidget() {
            return pendingWidget;
        }

        @Override
        public Optional<ExtendedCell<String>> genExtended() {
            return Optional.empty();
        }
    }

    /**
     * Minimal DataGridAdapter for String key/value pairs.
     */
    static class TestAdapter implements DataGridAdapter<String, String> {

        private final List<ColumnDefinition<String>> columns;

        TestAdapter(final List<ColumnDefinition<String>> columns) {
            this.columns = columns;
        }

        @Override
        public String getKey(final String data) {
            return data;
        }

        @Override
        public int compareDefault(final String v1, final String v2) {
            return v1.compareTo(v2);
        }

        @Override
        public List<ColumnDefinition<String>> getColumnDefinitions() {
            return columns;
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
        public boolean isAscendingSortByInsertionOrder() {
            return true;
        }

        @Override
        public void onCreateHeaderRow(final IsPWidget w) {
            // nothing to do
        }

        @Override
        public void onCreateFooterRow(final IsPWidget w) {
            // nothing to do
        }

        @Override
        public void onCreateRow(final IsPWidget w) {
            // nothing to do
        }

        @Override
        public void onSelectRow(final IsPWidget w) {
            // nothing to do
        }

        @Override
        public void onUnselectRow(final IsPWidget w) {
            // nothing to do
        }

        @Override
        public void onCreateColumnResizer(final IsPWidget w) {
            // nothing to do
        }

        @Override
        public IsPWidget createLoadingDataWidget() {
            return Element.newPLabel();
        }

        @Override
        public boolean isSelectionEnabled() {
            return true;
        }
    }
}
