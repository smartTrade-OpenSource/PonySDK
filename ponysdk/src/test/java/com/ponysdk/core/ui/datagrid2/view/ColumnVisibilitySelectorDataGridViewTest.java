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

import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.datagrid2.DomEvents;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition.State;
import com.ponysdk.core.ui.datagrid2.column.DefaultColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.data.FilterController;
import com.ponysdk.core.ui.datagrid2.datasource.DefaultCacheDataSource;
import com.ponysdk.core.ui.datagrid2.view.DefaultDataGridViewTest.CapturingColumnDefinition;
import com.ponysdk.core.ui.datagrid2.view.DefaultDataGridViewTest.RecordingAdapter;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the column visibility selector: a check box per switchable column, applied, cancelled or restored to defaults.
 */
public class ColumnVisibilitySelectorDataGridViewTest extends PSuite {

    private ColumnVisibilitySelectorDataGridView<String, String> view;
    private DefaultColumnDefinition<String> name;
    private DefaultColumnDefinition<String> len;
    private final List<String> hooks = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        hooks.clear();
        final DefaultCacheDataSource<String, String> source = new DefaultCacheDataSource<>();
        source.setFilterController(new FilterController<>());
        view = new ColumnVisibilitySelectorDataGridView<>(new DefaultDataGridView<>(source)) {

            @Override
            protected void onApply() {
                hooks.add("apply");
            }

            @Override
            protected void onCancel() {
                hooks.add("cancel");
            }

            @Override
            protected void onRestore() {
                hooks.add("restore");
            }
        };
        name = new DefaultColumnDefinition<>("name", v -> v, (v, s) -> {
        });
        len = new DefaultColumnDefinition<>("len", String::length, (v, s) -> {
        });
        view.setAdapter(new RecordingAdapter(List.<ColumnDefinition<String>> of(name, len, new CapturingColumnDefinition()), true));
    }

    @Test
    public void layoutListsSwitchableColumnsSortedById() {
        final PComplexPanel columns = columnsWidget();

        Assertions.assertEquals(2, columns.getWidgetCount());
        Assertions.assertEquals("len", checkBox(0).getText());
        Assertions.assertEquals("name", checkBox(1).getText());
        Assertions.assertTrue(checkBox(0).getValue());
        Assertions.assertTrue(checkBox(1).isEnabled());
        Assertions.assertEquals("Apply", ((com.ponysdk.core.ui.basic.PButton) layout().getWidget(1)).getText());
    }

    @Test
    public void applyHidesUncheckedColumns() {
        DomEvents.booleanValueChange(checkBox(1), false);

        PEmulator.click(applyButton());

        Assertions.assertEquals(List.of("apply"), hooks);
        Assertions.assertEquals(State.UNPINNED_HIDDEN, stateOf("name"));
        Assertions.assertFalse(checkBox(1).getValue());

        DomEvents.booleanValueChange(checkBox(1), true);
        PEmulator.click(applyButton());
        Assertions.assertEquals(State.UNPINNED_SHOWN, stateOf("name"));
    }

    @Test
    public void applyWithoutChangeKeepsStates() {
        DomEvents.booleanValueChange(checkBox(1), false);
        DomEvents.booleanValueChange(checkBox(1), true);

        PEmulator.click(applyButton());

        Assertions.assertEquals(State.UNPINNED_SHOWN, stateOf("name"));
    }

    @Test
    public void cancelRevertsCheckBoxes() {
        DomEvents.booleanValueChange(checkBox(1), false);

        PEmulator.click(layout().getWidget(2));

        Assertions.assertEquals(List.of("cancel"), hooks);
        Assertions.assertTrue(checkBox(1).getValue());
        PEmulator.click(applyButton());
        Assertions.assertEquals(State.UNPINNED_SHOWN, stateOf("name"));
    }

    @Test
    public void restoreChecksColumnsShownByDefault() {
        DomEvents.booleanValueChange(checkBox(1), false);
        PEmulator.click(applyButton());
        DomEvents.booleanValueChange(checkBox(0), false);

        PEmulator.click(layout().getWidget(3));

        Assertions.assertEquals(List.of("apply", "restore"), hooks);
        Assertions.assertTrue(checkBox(0).getValue());
        Assertions.assertTrue(checkBox(1).getValue());
        PEmulator.click(applyButton());
        Assertions.assertEquals(State.UNPINNED_SHOWN, stateOf("name"));
        Assertions.assertEquals(State.UNPINNED_SHOWN, stateOf("len"));
    }

    @Test
    public void pinnedColumnCannotBeSwitchedNorRestored() {
        name.getController().setState(State.PINNED_SHOWN);
        Assertions.assertFalse(checkBox(1).isEnabled());

        PEmulator.click(layout().getWidget(3));

        Assertions.assertEquals(State.PINNED_SHOWN, stateOf("name"));
    }

    @Test
    public void searchShowsOnlyMatchingColumns() {
        DomEvents.stringValueChange(searchBox(), " NA ");

        Assertions.assertFalse(columnsWidget().getWidget(0).isVisible());
        Assertions.assertTrue(columnsWidget().getWidget(1).isVisible());

        DomEvents.stringValueChange(searchBox(), "");
        Assertions.assertTrue(columnsWidget().getWidget(0).isVisible());
    }

    @Test
    public void columnActionsOtherThanStateLeaveCheckBoxesUnchanged() {
        name.getController().sort(true);
        name.getController().clearSort();
        name.getController().filter("k", (v, h) -> true, true, false);
        name.getController().clearFilter("k");
        name.getController().clearFilters();
        name.getController().redraw(false);
        view.getView().getController().setData("a");

        Assertions.assertTrue(checkBox(1).getValue());
        Assertions.assertTrue(checkBox(1).isEnabled());
    }

    private State stateOf(final String columnId) {
        for (final DataGridConfig.ColumnConfig<String> config : view.getConfig().getColumnConfigs()) {
            if (config.getColumnId().equals(columnId)) return config.getState();
        }
        throw new AssertionError("no column " + columnId);
    }

    private PComplexPanel layout() {
        return (PComplexPanel) view.getDecoratorWidget();
    }

    private PTextBox searchBox() {
        return (PTextBox) layout().getWidget(0);
    }

    private PWidget applyButton() {
        return layout().getWidget(1);
    }

    private PComplexPanel columnsWidget() {
        return (PComplexPanel) layout().getWidget(4);
    }

    private PCheckBox checkBox(final int index) {
        return (PCheckBox) ((PComplexPanel) columnsWidget().getWidget(index)).getWidget(0);
    }
}
