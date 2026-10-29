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

import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.PAddOn;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.datagrid2.DomEvents;
import com.ponysdk.core.ui.datagrid2.cell.PrimaryCell;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition.State;
import com.ponysdk.core.ui.datagrid2.column.DefaultColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfigBuilder;
import com.ponysdk.core.ui.datagrid2.data.FilterController;
import com.ponysdk.core.ui.datagrid2.datasource.DefaultCacheDataSource;
import com.ponysdk.core.ui.datagrid2.view.DefaultDataGridViewTest.RecordingAdapter;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;
import jakarta.json.Json;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the check box column that {@link RowSelectorColumnDataGridView} pins first: its header selects or unselects
 * every row and reflects the selection, its cells reflect the selection of their row.
 */
public class RowSelectorColumnDataGridViewTest extends PSuite {

    private static final String COLUMN_ID = RowSelectorColumnDataGridView.RowSelectorColumnDefinition.class.getName();

    private DefaultDataGridView<String, String> base;
    private RowSelectorColumnDataGridView<String, String> view;

    private void newView(final boolean footer) throws Exception {
        final DefaultCacheDataSource<String, String> source = new DefaultCacheDataSource<>();
        source.setFilterController(new FilterController<>());
        base = new DefaultDataGridView<>(source);
        view = new RowSelectorColumnDataGridView<>(base);
        final DataGridView<String, String> outer = footer ? new ColumnFilterFooterDataGridView<>(view) : view;
        final List<ColumnDefinition<String>> columns = List.of(new DefaultColumnDefinition<>("name", v -> v, (v, s) -> {
        }));
        outer.setAdapter(new RecordingAdapter(columns, true));
        drawAfter(() -> view.getController().setData(List.of("a", "b", "c")));
    }

    @Test
    public void selectorColumnIsPinnedFirst() throws Exception {
        newView(false);
        final ColumnDefinition<String> selector = selector();

        Assertions.assertEquals(COLUMN_ID, selector.getId());
        Assertions.assertEquals(State.PINNED_SHOWN, selector.getDefaultState());
        Assertions.assertEquals(COLUMN_ID, view.getConfig().getColumnConfigs().get(0).getColumnId());
        Assertions.assertEquals(20, selector.getDefaultWidth());
        Assertions.assertEquals(20, selector.getMinWidth());
        Assertions.assertEquals(20, selector.getMaxWidth());
        Assertions.assertFalse(selector.isVisibilitySwitchable());
        Assertions.assertFalse(selector.isPinnable());
        Assertions.assertFalse(selector.isFilterable());
        Assertions.assertFalse(selector.isSortable());
        Assertions.assertFalse(selector.isResizable());
        Assertions.assertNull(selector.getRenderingHelper("a"));
        Assertions.assertEquals(0, selector.compare("a", () -> null, "b", () -> null));
        Assertions.assertNull(selector.getDraggableHeaderElement());
        Assertions.assertNull(selector.getGroup());
        Assertions.assertNull(selector.getFooter());
        Assertions.assertNotNull(selector.getController());
        final PrimaryCell<String> cell = selector.createCell();
        Assertions.assertTrue(cell.asWidget() instanceof PCheckBox);
        Assertions.assertNotNull(cell.asPendingWidget());
    }

    /** A check box has no text here: a screen reader hears what the input is named, in the application's language. */
    @Test
    public void theCheckBoxesAreNamedForAScreenReader() throws Exception {
        final CapturingModelWriter writer = CapturingModelWriter.install();
        try {
            newView(false);
            PWindow.getMain().add(base.asWidget());
            final List<Object> names = writer.valuesAfter(ServerToClientModel.PUT_INPUT_ATTRIBUTE_KEY, "aria-label");
            Assertions.assertEquals("Select all rows", names.get(0));
            Assertions.assertTrue(names.size() > 3, "one per row drawn: " + names.size());
            Assertions.assertTrue(names.stream().skip(1).allMatch("Select row"::equals), names.toString());

            writer.clear();
            final RowSelectorColumnDataGridView<String, String> french = new RowSelectorColumnDataGridView<>(
                new DefaultDataGridView<>(new DefaultCacheDataSource<>())) {
                @Override
                protected String selectAllLabel() {
                    return "Tout s\u00e9lectionner";
                }

                @Override
                protected String selectRowLabel() {
                    return "S\u00e9lectionner la ligne";
                }
            };
            french.setAdapter(new RecordingAdapter(List.of(), true));
            final ColumnDefinition<String> selector = french.getAdapter().getColumnDefinitions().get(0);
            PWindow.getMain().add(selector.getHeader().asWidget());
            PWindow.getMain().add(selector.createCell().asWidget());
            Assertions.assertEquals(List.of("Tout s\u00e9lectionner", "S\u00e9lectionner la ligne"),
                writer.valuesAfter(ServerToClientModel.PUT_INPUT_ATTRIBUTE_KEY, "aria-label"));
        } finally {
            writer.uninstall();
        }
    }

    @Test
    public void headerCheckBoxSelectsAndUnselectsAllRows() throws Exception {
        newView(false);
        final PCheckBox header = (PCheckBox) selector().getHeader();

        DomEvents.booleanValueChange(header, true);
        Assertions.assertEquals(3, view.getLiveSelectedDataCount());
        Assertions.assertEquals(PCheckBoxState.CHECKED, header.getState());
        Assertions.assertTrue(rowCheckBox(1).getValue());

        DomEvents.booleanValueChange(header, false);
        Assertions.assertEquals(0, view.getLiveSelectedDataCount());
        Assertions.assertEquals(PCheckBoxState.UNCHECKED, header.getState());
        Assertions.assertFalse(rowCheckBox(1).getValue());
        Assertions.assertTrue(rowCheckBox(1).isEnabled());
    }

    @Test
    public void headerShowsPartialSelection() throws Exception {
        newView(false);
        final PCheckBox header = (PCheckBox) selector().getHeader();

        view.getController().select("b");

        Assertions.assertEquals(PCheckBoxState.INDETERMINATE, header.getState());
    }

    @Test
    public void programmaticSelectionIsShownOnItsRow() throws Exception {
        newView(false);

        view.getController().select("b");

        Assertions.assertTrue(rowCheckBox(1).getValue());
        Assertions.assertFalse(rowCheckBox(0).getValue());
    }

    @Test
    public void selectorCellsAreDisabledWhenSelectionIsDisabled() throws Exception {
        final DefaultCacheDataSource<String, String> source = new DefaultCacheDataSource<>();
        source.setFilterController(new FilterController<>());
        base = new DefaultDataGridView<>(source);
        view = new RowSelectorColumnDataGridView<>(base);
        view.setAdapter(new RecordingAdapter(List.of(new DefaultColumnDefinition<>("name", v -> v, (v, s) -> {
        })), true) {

            @Override
            public boolean isSelectionEnabled() {
                return false;
            }
        });

        drawAfter(() -> view.getController().setData(List.of("a")));

        Assertions.assertFalse(rowCheckBox(0).isEnabled());
    }

    @Test
    public void footerIsAHiddenCheckBoxWhenAdapterHasFooter() throws Exception {
        newView(true);

        Assertions.assertTrue(selector().getFooter() instanceof PCheckBox);
        Assertions.assertSame(selector(), base.getAdapter().getColumnDefinitions().get(0));
    }

    @Test
    public void rowSelectorCanWrapColumnFilterFooter() {
        final DefaultCacheDataSource<String, String> source = new DefaultCacheDataSource<>();
        source.setFilterController(new FilterController<>());
        final RowSelectorColumnDataGridView<String, String> selector = new RowSelectorColumnDataGridView<>(
            new ColumnFilterFooterDataGridView<>(new DefaultDataGridView<>(source)));

        Assertions.assertDoesNotThrow(() -> selector.setAdapter(new RecordingAdapter(List.of(new DefaultColumnDefinition<>("name", v -> v, (v, s) -> {
        })), true)));
    }

    @Test
    public void setConfigKeepsSelectorColumnFirst() throws Exception {
        newView(false);

        view.setConfig(new DataGridConfigBuilder<String>().addColumnConfig(new ColumnConfig<>("name", State.PINNED_SHOWN, 80)).build());
        Assertions.assertEquals(List.of(COLUMN_ID, "name"), columnIds(view.getConfig()));

        view.setConfig(new DataGridConfigBuilder<String>()
            .addColumnConfig(new ColumnConfig<>("name", State.PINNED_SHOWN, 80))
            .addColumnConfig(new ColumnConfig<>(COLUMN_ID, State.PINNED_SHOWN, 20))
            .build());
        Assertions.assertEquals(List.of(COLUMN_ID, "name"), columnIds(view.getConfig()));

        view.setConfig(view.getConfig());
        Assertions.assertEquals(List.of(COLUMN_ID, "name"), columnIds(view.getConfig()));
    }

    @Test
    public void setConfigBeforeAdapterIsDelegated() {
        final RowSelectorColumnDataGridView<String, String> noAdapter = new RowSelectorColumnDataGridView<>(new DefaultDataGridView<>());

        Assertions.assertThrows(IllegalStateException.class, () -> noAdapter.setConfig(new DataGridConfigBuilder<String>().build()));
    }

    @Test
    public void columnActionsOnOtherColumnsDoNotChangeSelection() throws Exception {
        newView(false);
        final ColumnDefinition<String> name = view.getAdapter().getColumnDefinitions().get(1);
        view.getController().select("a");

        drawAfter(() -> name.getController().sort(false));
        name.getController().filter("k", (v, h) -> true, true, false);
        name.getController().clearFilter("k");
        name.getController().clearFilters();
        name.getController().redraw(true);
        name.getController().setState(State.PINNED_SHOWN);
        name.getController().clearSort();

        Assertions.assertEquals(List.of("a"), view.getLiveSelectedData().getData());
    }

    @Test
    public void movingAnotherColumnKeepsTheSelectorFirstAndTheSelection() throws Exception {
        final DefaultCacheDataSource<String, String> source = new DefaultCacheDataSource<>();
        source.setFilterController(new FilterController<>());
        base = new DefaultDataGridView<>(source);
        view = new RowSelectorColumnDataGridView<>(base);
        view.setAdapter(new RecordingAdapter(List.of(new DefaultColumnDefinition<>("name", v -> v, (v, s) -> {
        }), new DefaultColumnDefinition<>("len", String::length, (v, s) -> {
        })), true));
        drawAfter(() -> view.getController().setData(List.of("a", "b")));
        view.getController().select("a");

        for (final PAddOn addon : base.asWidget().getAddons()) {
            if (addon.getClass().getSimpleName().equals("Addon")) DomEvents.terminalData(addon, Json.createObjectBuilder().add("col", 1).add("to", 2));
        }

        Assertions.assertEquals(List.of(COLUMN_ID, "len", "name"), columnIds(view.getConfig()));
        Assertions.assertEquals(List.of("a"), view.getLiveSelectedData().getData());
        Assertions.assertTrue(rowCheckBox(0).getValue());
    }

    @Test
    public void gridWithoutHeaderHasNoHeaderCheckBox() throws Exception {
        final DefaultCacheDataSource<String, String> source = new DefaultCacheDataSource<>();
        source.setFilterController(new FilterController<>());
        base = new DefaultDataGridView<>(source);
        view = new RowSelectorColumnDataGridView<>(base);
        view.setAdapter(new RecordingAdapter(List.of(new DefaultColumnDefinition<>("name", v -> v, (v, s) -> {
        })), false));

        drawAfter(() -> view.getController().setData(List.of("a", "b")));

        Assertions.assertNull(selector().getHeader());
        Assertions.assertFalse(rowCheckBox(0).getValue());
        drawAfter(() -> view.getController().select("a"));
        Assertions.assertEquals(1, view.getLiveSelectedDataCount());
    }

    @Test
    public void setConfigWithoutColumnsKeepsSelectorColumnFirst() throws Exception {
        newView(false);

        view.setConfig(new DataGridConfigBuilder<String>().build());

        Assertions.assertEquals(List.of(COLUMN_ID, "name"), columnIds(view.getConfig()));
    }

    private ColumnDefinition<String> selector() {
        return view.getAdapter().getColumnDefinitions().get(0);
    }

    /** The selector check box of a row: first pinned cell, after its pending widget. */
    private PCheckBox rowCheckBox(final int row) {
        final PComplexPanel root = (PComplexPanel) base.asWidget();
        final PComplexPanel pinnedBody = (PComplexPanel) ((PComplexPanel) ((PComplexPanel) root.getWidget(1)).getWidget(0)).getWidget(0);
        final PComplexPanel cell = (PComplexPanel) ((PComplexPanel) pinnedBody.getWidget(row)).getWidget(0);
        return (PCheckBox) cell.getWidget(1);
    }

    private static List<String> columnIds(final DataGridConfig<String> config) {
        final List<String> ids = new ArrayList<>();
        config.getColumnConfigs().forEach(c -> ids.add(c.getColumnId()));
        return ids;
    }

    private void drawAfter(final Runnable action) throws InterruptedException {
        GridDraws.drawAfter(base, action);
    }
}
