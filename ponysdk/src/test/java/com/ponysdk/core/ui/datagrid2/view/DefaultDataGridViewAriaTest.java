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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.datagrid2.DomEvents;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition.State;
import com.ponysdk.core.ui.datagrid2.column.DefaultColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnSort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfigBuilder;
import com.ponysdk.core.ui.datagrid2.data.FilterController;
import com.ponysdk.core.ui.datagrid2.datasource.DefaultCacheDataSource;
import com.ponysdk.core.ui.datagrid2.view.DefaultDataGridViewTest.Grid;
import com.ponysdk.core.ui.datagrid2.view.DefaultDataGridViewTest.TestAdapter;
import com.ponysdk.test.ModelWriterForTest;
import com.ponysdk.test.PSuite;

import jakarta.json.Json;

/**
 * docs/decisions/0025-datagrid-accessibility.md: what the server tells a screen reader of a grid. A row is two
 * elements, the one of the pinned columns being the row and the other presentational; the cells, and the link that
 * makes the two one row, are laid by the terminal, not here.
 */
public class DefaultDataGridViewAriaTest extends PSuite {

    private static List<String> rows(final int count) {
        final List<String> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) rows.add(String.format("row%03d", i));
        return rows;
    }

    @Test
    public void theGridAndItsContainersAreMarkedForAScreenReader() {
        final Grid g = new Grid(true);

        Assertions.assertEquals("grid", g.root().getAttribute("role"));
        Assertions.assertEquals("true", g.root().getAttribute("aria-multiselectable"));
        for (final PWidget container : List.of(g.root().getWidget(0), g.root().getWidget(1), g.root().getWidget(2),
            ((PComplexPanel) g.root().getWidget(1)).getWidget(0), g.pinnedBody(), g.unpinnedBody(), g.unpinnedHeader())) {
            Assertions.assertEquals("presentation", container.getAttribute("role"), container.getStyleName());
        }
        Assertions.assertEquals("row", g.pinnedHeader().getAttribute("role"));
        Assertions.assertEquals("1", g.pinnedHeader().getAttribute("aria-rowindex"));
        Assertions.assertEquals("row", g.pinnedRow(0).getAttribute("role"), "the pinned half is the row");
        Assertions.assertEquals("presentation", g.unpinnedRow(0).getAttribute("role"), "the other half is no row");
        Assertions.assertEquals("presentation", footer(g.root(), 0).getAttribute("role"), "no footer, no row");
        Assertions.assertEquals("presentation", footer(g.root(), 1).getAttribute("role"));
    }

    @Test
    public void aGridWithoutHeaderHasNoHeaderRow() throws Exception {
        final Grid g = new Grid(false);
        g.drawAfter(() -> g.grid.getController().setData(rows(3)));

        Assertions.assertEquals("presentation", g.pinnedHeader().getAttribute("role"));
        Assertions.assertNull(g.pinnedHeader().getAttribute("aria-rowindex"));
        Assertions.assertEquals("3", g.root().getAttribute("aria-rowcount"));
        Assertions.assertEquals("1", g.pinnedRow(0).getAttribute("aria-rowindex"), "the data starts at the first row");
        Assertions.assertEquals("3", g.pinnedRow(2).getAttribute("aria-rowindex"));

        g.drawAfter(() -> g.name.getController().sort(true));
        Assertions.assertNull(g.unpinnedHeader().getWidget(0).getAttribute("aria-sort"), "no header, no sort to say");
    }

    @Test
    public void rowIndexAndCountAreThoseOfTheLiveDataWhileTheRowsAreVirtualised() throws Exception {
        final Grid g = new Grid(true);
        Assertions.assertEquals("1", g.root().getAttribute("aria-rowcount"), "the header, before any data");

        g.drawAfter(() -> g.grid.getController().setData(rows(100)));

        Assertions.assertEquals("101", g.root().getAttribute("aria-rowcount"), "the live data and the header, not the 30 rows drawn");
        Assertions.assertEquals("2", g.pinnedRow(0).getAttribute("aria-rowindex"));
        Assertions.assertEquals("31", g.pinnedRow(29).getAttribute("aria-rowindex"));

        g.drawAfter(() -> g.terminal(Json.createObjectBuilder().add("row", 40)));

        Assertions.assertEquals("42", g.pinnedRow(0).getAttribute("aria-rowindex"), "row 40 of the data is the 42nd of the grid");
        Assertions.assertEquals("71", g.pinnedRow(29).getAttribute("aria-rowindex"));
        Assertions.assertEquals("101", g.root().getAttribute("aria-rowcount"));
        Assertions.assertNull(g.unpinnedRow(0).getAttribute("aria-rowindex"), "the row is the pinned half");
    }

    @Test
    public void theCountFollowsTheDataAndTheFooterComesLast() throws Exception {
        final Footed f = new Footed(true, true);
        Assertions.assertEquals("2", f.root().getAttribute("aria-rowcount"), "a header and a footer");
        Assertions.assertEquals("row", footer(f.root(), 0).getAttribute("role"));

        f.drawAfter(() -> f.grid.getController().setData(rows(5)));
        Assertions.assertEquals("7", f.root().getAttribute("aria-rowcount"));
        Assertions.assertEquals("7", footer(f.root(), 0).getAttribute("aria-rowindex"), "after the 5 rows of data and the header");

        f.drawAfter(() -> f.grid.getController().setData(rows(8)));
        Assertions.assertEquals("10", f.root().getAttribute("aria-rowcount"));
        Assertions.assertEquals("10", footer(f.root(), 0).getAttribute("aria-rowindex"));
        Assertions.assertEquals("presentation", footer(f.root(), 1).getAttribute("role"));
    }

    @Test
    public void theIndexIsSentWhenTheRowsMoveAndTheSelectionWhenItChanges() throws Exception {
        final Footed f = new Footed(true, false);
        f.drawAfter(() -> f.grid.getController().setData(rows(5)));
        final int sent = f.attributes.count("aria-rowindex") + f.attributes.count("aria-selected") + f.attributes.count("aria-rowcount");
        Assertions.assertTrue(sent > 0);

        // a tick: the rows are drawn again, nothing of the announced state changed
        f.grid.onUpdateRows(0, 30);
        Assertions.assertEquals(sent, f.attributes.count("aria-rowindex") + f.attributes.count("aria-selected") + f.attributes.count("aria-rowcount"),
            "an unchanged row costs nothing");

        final int indexes = f.attributes.count("aria-rowindex");
        f.drawAfter(() -> f.terminal(Json.createObjectBuilder().add("row", 2)));
        Assertions.assertEquals(indexes + 3, f.attributes.count("aria-rowindex"), "the 3 rows left with data moved, the others were hidden");
    }

    @Test
    public void aRowSaysWhetherItIsSelected() throws Exception {
        final DefaultCacheDataSource<String, String> source = new DefaultCacheDataSource<>() {
            @Override
            public boolean isSelectable(final String key) {
                return !key.equals("c");
            }
        };
        source.setFilterController(new FilterController<>());
        final Grid g = new Grid(true, source);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "b", "c")));

        Assertions.assertEquals("false", g.pinnedRow(0).getAttribute("aria-selected"));
        Assertions.assertNull(g.pinnedRow(2).getAttribute("aria-selected"), "a row that cannot be selected is no choice");

        DomEvents.click(g.unpinnedRow(1), false);
        Assertions.assertEquals("true", g.pinnedRow(1).getAttribute("aria-selected"));
        Assertions.assertEquals("false", g.pinnedRow(0).getAttribute("aria-selected"));
        Assertions.assertNull(g.unpinnedRow(1).getAttribute("aria-selected"), "the pinned half is the row");

        DomEvents.click(g.pinnedRow(1), false);
        Assertions.assertEquals("false", g.pinnedRow(1).getAttribute("aria-selected"));

        DomEvents.click(g.unpinnedRow(0), false);
        DomEvents.click(g.unpinnedRow(1), true);
        Assertions.assertEquals("true", g.pinnedRow(0).getAttribute("aria-selected"), "a range selects its rows");
        Assertions.assertEquals("true", g.pinnedRow(1).getAttribute("aria-selected"));
    }

    @Test
    public void withoutSelectionNothingIsSelectable() throws Exception {
        final Footed f = new Footed(false, false);
        f.drawAfter(() -> f.grid.getController().setData(rows(3)));

        Assertions.assertNull(f.root().getAttribute("aria-multiselectable"));
        Assertions.assertNull(f.pinnedRow(0).getAttribute("aria-selected"));
    }

    @Test
    public void theSortOfAColumnIsOnItsHeaderCell() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "bb", "ccc")));
        final PWidget name = g.unpinnedHeader().getWidget(0);
        final PWidget len = g.unpinnedHeader().getWidget(1);
        Assertions.assertNull(name.getAttribute("aria-sort"));

        g.drawAfter(() -> g.name.getController().sort(true));
        Assertions.assertEquals("ascending", name.getAttribute("aria-sort"));
        Assertions.assertNull(len.getAttribute("aria-sort"));

        g.drawAfter(() -> g.name.getController().sort(false));
        Assertions.assertEquals("descending", name.getAttribute("aria-sort"));

        g.drawAfter(() -> g.name.getController().clearSort());
        Assertions.assertNull(name.getAttribute("aria-sort"));

        g.drawAfter(() -> g.len.getController().sort(true));
        Assertions.assertEquals("ascending", len.getAttribute("aria-sort"));
        g.grid.clearSorts(false);
        Assertions.assertNull(len.getAttribute("aria-sort"), "cleared, the columns told or not");
    }

    @Test
    public void theEnterKeyOnAHeaderSortsItsColumnAsAClickOnItsNameDoes() throws Exception {
        final Grid g = new Grid(true, new DefaultDataGridViewTest.CapturingColumnDefinition());
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "bb", "ccc")));
        final PWidget name = g.unpinnedHeader().getWidget(0);

        g.drawAfter(() -> g.terminal(Json.createObjectBuilder().add("col", 0).add("sort", 1)));
        Assertions.assertEquals("ascending", name.getAttribute("aria-sort"));
        Assertions.assertEquals(List.of("a", "bb", "ccc"), g.texts(0));

        g.drawAfter(() -> g.terminal(Json.createObjectBuilder().add("col", 0).add("sort", 1)));
        Assertions.assertEquals("descending", name.getAttribute("aria-sort"));
        Assertions.assertEquals(List.of("ccc", "bb", "a"), g.texts(0));

        g.drawAfter(() -> g.terminal(Json.createObjectBuilder().add("col", 0).add("sort", 1)));
        Assertions.assertEquals("ascending", name.getAttribute("aria-sort"));

        g.terminal(Json.createObjectBuilder().add("col", 2).add("sort", 1)); // a column that cannot be sorted
        Assertions.assertNull(g.unpinnedHeader().getWidget(2).getAttribute("aria-sort"));
        Assertions.assertEquals("ascending", name.getAttribute("aria-sort"));
    }

    @Test
    public void aConfigSortsTheNewHeaderCells() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "b")));
        g.drawAfter(() -> g.name.getController().sort(true));

        g.grid.setConfig(new DataGridConfigBuilder<String>()
            .addColumnConfig(new ColumnConfig<>("name", State.UNPINNED_SHOWN, 100))
            .addColumnConfig(new ColumnConfig<>("len", State.UNPINNED_SHOWN, 100))
            .addSort(new ColumnSort<>("len", false))
            .build());

        Assertions.assertNull(g.unpinnedHeader().getWidget(0).getAttribute("aria-sort"));
        Assertions.assertEquals("descending", g.unpinnedHeader().getWidget(1).getAttribute("aria-sort"));
    }

    @Test
    public void aHiddenColumnIsNotInTheGridAndASortedOneLosesItsSort() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(List.of("a", "b")));
        Assertions.assertEquals("2", g.root().getAttribute("aria-colcount"));
        g.drawAfter(() -> g.name.getController().sort(true));

        g.name.getController().setState(State.UNPINNED_HIDDEN);
        Assertions.assertEquals("1", g.root().getAttribute("aria-colcount"));
        Assertions.assertEquals(1, g.unpinnedHeader().getWidgetCount());

        g.name.getController().setState(State.UNPINNED_SHOWN);
        Assertions.assertEquals("2", g.root().getAttribute("aria-colcount"));
        Assertions.assertNull(g.unpinnedHeader().getWidget(1).getAttribute("aria-sort"), "the sort went with the hiding");

        g.grid.setConfig(new DataGridConfigBuilder<String>().addColumnConfig(new ColumnConfig<>("len", State.UNPINNED_HIDDEN, 100)).build());
        Assertions.assertEquals("1", g.root().getAttribute("aria-colcount"));
    }

    @Test
    public void pinningACounterKeepsTheColumnsCountedAndAsksTheTerminalToLayTheCellsAgain() {
        final Grid g = new Grid(true, new DefaultDataGridViewTest.CapturingColumnDefinition());
        final int laid = g.recorder.count("updateAria");
        Assertions.assertTrue(laid >= 1, "laid once the grid is built");

        g.name.getController().setState(State.PINNED_SHOWN);
        Assertions.assertEquals("3", g.root().getAttribute("aria-colcount"), "pinned columns are columns of the grid");
        Assertions.assertEquals(laid + 1, g.recorder.count("updateAria"));

        g.name.getController().setState(State.UNPINNED_SHOWN);
        Assertions.assertEquals(laid + 2, g.recorder.count("updateAria"));

        g.name.getController().setState(State.UNPINNED_HIDDEN);
        Assertions.assertEquals(laid + 3, g.recorder.count("updateAria"));

        g.name.getController().setState(State.UNPINNED_SHOWN);
        Assertions.assertEquals(laid + 4, g.recorder.count("updateAria"));

        g.terminal(Json.createObjectBuilder().add("col", 0).add("to", 1));
        Assertions.assertTrue(g.recorder.count("updateAria") > laid + 4, "a column moved");
    }

    @Test
    public void growingTheRowsAndSettingAConfigAskTheTerminalToLayTheCellsAgain() throws Exception {
        final Grid g = new Grid(true);
        g.drawAfter(() -> g.grid.getController().setData(rows(3)));
        final int laid = g.recorder.count("updateAria");

        g.drawAfter(() -> g.terminal(Json.createObjectBuilder().add("rc", 40)));
        Assertions.assertTrue(g.recorder.count("updateAria") > laid, "rows added");
        final int rows = g.recorder.count("updateAria");

        g.grid.setConfig(new DataGridConfigBuilder<String>().build());
        Assertions.assertTrue(g.recorder.count("updateAria") > rows, "a configuration");
    }

    /** The pinned (0) or the unpinned (1) half of the footer of the grid. */
    private static PComplexPanel footer(final PComplexPanel root, final int half) {
        return (PComplexPanel) ((PComplexPanel) root.getWidget(2)).getWidget(half);
    }

    /** A grid with a header, a footer if asked and the selection as asked; its writer records the attributes written. */
    private static final class Footed {

        final AttributeWriter attributes = new AttributeWriter();
        final DefaultDataGridView<String, String> grid;

        Footed(final boolean selection, final boolean footer) {
            final DefaultColumnDefinition<String> name = new DefaultColumnDefinition<>("name", v -> v, (v, s) -> {
            }) {
                @Override
                public IsPWidget getFooter() {
                    return Element.newPLabel("footer");
                }
            };
            final DefaultCacheDataSource<String, String> source = new DefaultCacheDataSource<>();
            source.setFilterController(new FilterController<>());
            Mockito.doReturn(attributes).when(UIContext.get()).getWriter();
            grid = new DefaultDataGridView<>(source);
            grid.setAdapter(new TestAdapter(List.<ColumnDefinition<String>> of(name)) {
                @Override
                public boolean hasHeader() {
                    return true;
                }

                @Override
                public boolean hasFooter() {
                    return footer;
                }

                @Override
                public boolean isSelectionEnabled() {
                    return selection;
                }
            });
            PWindow.getMain().add(grid.asWidget());
        }

        PComplexPanel root() {
            return (PComplexPanel) grid.asWidget();
        }

        PComplexPanel pinnedRow(final int index) {
            return (PComplexPanel) ((PComplexPanel) ((PComplexPanel) ((PComplexPanel) root().getWidget(1)).getWidget(0)).getWidget(0)).getWidget(index);
        }

        void terminal(final jakarta.json.JsonObjectBuilder data) {
            grid.asWidget().getAddons().forEach(addon -> {
                if (addon.getClass().getSimpleName().equals("Addon")) DomEvents.terminalData(addon, data);
            });
        }

        void drawAfter(final Runnable action) throws InterruptedException {
            GridDraws.drawAfter(grid, action);
        }
    }

    /** The names of the attributes the server wrote, in order. */
    private static final class AttributeWriter extends ModelWriterForTest {

        final List<String> names = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void write(final ServerToClientModel model, final Object value) {
            if (model == ServerToClientModel.PUT_ATTRIBUTE_KEY) names.add((String) value);
            super.write(model, value);
        }

        int count(final String name) {
            synchronized (names) {
                return (int) names.stream().filter(name::equals).count();
            }
        }
    }
}
