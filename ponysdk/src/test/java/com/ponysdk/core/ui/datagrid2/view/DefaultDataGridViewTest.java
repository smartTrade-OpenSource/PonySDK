package com.ponysdk.core.ui.datagrid2.view;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.cell.CellController;
import com.ponysdk.core.ui.datagrid2.cell.ExtendedCell;
import com.ponysdk.core.ui.datagrid2.cell.PrimaryCell;
import com.ponysdk.core.ui.datagrid2.cell.PrimaryCellController;
import com.ponysdk.core.ui.datagrid2.column.ColumnController;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.data.FilterController;
import com.ponysdk.core.ui.datagrid2.datasource.DefaultCacheDataSource;
import com.ponysdk.test.PSuite;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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

    @Before
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
        Assert.assertTrue("Timed out waiting for grid draw", latch.await(DRAW_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private CellController<String> getCellController(final int rowIndex) {
        return getCellControllerFrom(column, rowIndex);
    }

    private CellController<String> getCellControllerFrom(final CapturingColumnDefinition col, final int rowIndex) {
        final CellController<String> cc = col.getCellController(rowIndex);
        Assert.assertNotNull("No CellController for row index " + rowIndex
                + " — did you call setData() and wait for the draw?", cc);
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
            Assert.assertTrue("Expected '" + key + "' to be selected", v.getController().isSelected(key));
        }
    }

    private void assertNotSelectedOn(final DefaultDataGridView<String, String> v, final String... keys) {
        for (final String key : keys) {
            Assert.assertFalse("Expected '" + key + "' to not be selected", v.getController().isSelected(key));
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
