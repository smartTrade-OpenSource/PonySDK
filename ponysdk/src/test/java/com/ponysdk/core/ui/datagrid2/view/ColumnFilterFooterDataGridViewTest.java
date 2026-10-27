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
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.datagrid2.DomEvents;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition.State;
import com.ponysdk.core.ui.datagrid2.column.DefaultColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfigBuilder;
import com.ponysdk.core.ui.datagrid2.data.FilterController;
import com.ponysdk.core.ui.datagrid2.datasource.DefaultCacheDataSource;
import com.ponysdk.core.ui.datagrid2.view.DataGridView.DecodeException;
import com.ponysdk.core.ui.datagrid2.view.DefaultDataGridViewTest.CapturingColumnDefinition;
import com.ponysdk.core.ui.datagrid2.view.DefaultDataGridViewTest.RecordingAdapter;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.test.PSuite;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests the text box filters that {@link ColumnFilterFooterDataGridView} adds in the footer of each column without one.
 */
public class ColumnFilterFooterDataGridViewTest extends PSuite {

    private static final String CONFIG_KEY = ColumnFilterFooterDataGridView.class.getName();
    private static final long TIMEOUT_MS = 3_000;

    private DefaultDataGridView<String, String> base;
    private ColumnFilterFooterDataGridView<String, String> view;
    private final PLabel ownFooter = Element.newPLabel("own");

    @BeforeEach
    public void setUp() throws Exception {
        final DefaultCacheDataSource<String, String> source = new DefaultCacheDataSource<>();
        source.setFilterController(new FilterController<>());
        base = new DefaultDataGridView<>(source);
        view = new ColumnFilterFooterDataGridView<>(base);
        final ColumnDefinition<String> name = new DefaultColumnDefinition<>("name", v -> v, (v, s) -> {
        });
        final ColumnDefinition<String> withFooter = new DefaultColumnDefinition<>("withFooter", v -> v, (v, s) -> {
        }) {

            @Override
            public IsPWidget getFooter() {
                return ownFooter;
            }
        };
        view.setAdapter(new RecordingAdapter(List.of(name, withFooter, new CapturingColumnDefinition()), true));
        drawAfter(() -> view.getController().setData(List.of("apple", "banana", "cherry")));
    }

    @Test
    public void columnsWithoutFooterGetAFilterTextBox() {
        Assertions.assertTrue(view.getAdapter().hasFooter());
        final List<ColumnDefinition<String>> columns = view.getAdapter().getColumnDefinitions();

        Assertions.assertTrue(columns.get(0).getFooter() instanceof PTextBox);
        Assertions.assertSame(ownFooter, columns.get(1).getFooter());
        final PTextBox fixedFooter = (PTextBox) columns.get(2).getFooter();
        Assertions.assertFalse(fixedFooter.isEnabled());
        Assertions.assertEquals("112.0px", ((PTextBox) columns.get(0).getFooter()).getWidth());
    }

    @Test
    public void typingInFooterFiltersRowsAndReenablesFooterAfterDraw() throws Exception {
        final PTextBox footer = footer();

        type(" AN ");

        Assertions.assertEquals(List.of("banana"), view.getFilteredData().getData());

        type("ana");
        Assertions.assertEquals(List.of("banana"), view.getFilteredData().getData());

        type("");
        Assertions.assertEquals(List.of("apple", "banana", "cherry"), view.getFilteredData().getData());
        Assertions.assertEquals("", footer.getText());
    }

    @Test
    public void configCarriesFooterFilters() throws Exception {
        type("err");

        final DataGridConfig<String> config = view.getConfig();
        final Map<Object, String> filters = config.getCustomValue(CONFIG_KEY);

        Assertions.assertEquals(1, filters.size());
        Assertions.assertEquals("err", filters.values().iterator().next());

        final String encoded = view.encodeConfigCustomValue(CONFIG_KEY, filters);
        Assertions.assertEquals("name\u0000err\u0000", encoded);
        Assertions.assertEquals(filters, view.decodeConfigCustomValue(CONFIG_KEY, encoded));
    }

    @Test
    public void configWithoutFilterHasNoCustomValue() {
        Assertions.assertNull(view.getConfig().getCustomValue(CONFIG_KEY));
    }

    @Test
    public void encodingEscapesSeparators() throws Exception {
        type("a\u0000b");
        final Map<Object, String> filters = view.getConfig().getCustomValue(CONFIG_KEY);

        final String encoded = view.encodeConfigCustomValue(CONFIG_KEY, filters);

        Assertions.assertEquals("name\u0000a\u0000\u0000b\u0000", encoded);
        final Map<Object, String> decoded = (Map<Object, String>) view.decodeConfigCustomValue(CONFIG_KEY, encoded);
        Assertions.assertEquals("a\u0000b", decoded.values().iterator().next());
    }

    @Test
    public void decodingIgnoresUnknownColumns() throws DecodeException {
        final Map<?, ?> decoded = (Map<?, ?>) view.decodeConfigCustomValue(CONFIG_KEY, "unknown\u0000x\u0000");

        Assertions.assertTrue(decoded.isEmpty());
    }

    @Test
    public void otherCustomValuesAreLeftToWrappedView() throws DecodeException {
        Assertions.assertNull(view.encodeConfigCustomValue("other", "v"));
        Assertions.assertNull(view.decodeConfigCustomValue("other", "v"));
    }

    @Test
    public void setConfigRestoresFooterFilters() throws Exception {
        type("err");
        final DataGridConfig<String> config = view.getConfig();
        type("");
        Assertions.assertEquals(3, view.getFilteredData().getData().size());

        view.setConfig(config);

        Assertions.assertEquals("err", footer().getText());
        Assertions.assertEquals(List.of("cherry"), view.getFilteredData().getData());
    }

    @Test
    public void setConfigWithoutFiltersClearsFooters() throws Exception {
        type("err");

        view.setConfig(new DataGridConfigBuilder<String>().build());

        Assertions.assertEquals("", footer().getText());
        Assertions.assertEquals(3, view.getFilteredData().getData().size());
    }

    @Test
    public void hidingColumnClearsItsFooter() throws Exception {
        type("err");
        final ColumnDefinition<String> name = view.getAdapter().getColumnDefinitions().get(0);

        name.getController().setState(State.UNPINNED_HIDDEN);

        Assertions.assertEquals("", footer().getText());
        name.getController().setState(State.UNPINNED_SHOWN);
        name.onMoved();
        Assertions.assertEquals("", footer().getText());
    }

    @Test
    public void hidingColumnClearsItsFilter() throws Exception {
        type("err");
        Assertions.assertEquals(List.of("cherry"), view.getFilteredData().getData());

        view.getAdapter().getColumnDefinitions().get(0).getController().setState(State.UNPINNED_HIDDEN);

        Assertions.assertEquals(List.of("apple", "banana", "cherry"), view.getFilteredData().getData());
    }

    @Test
    public void hiddenColumnFilterIsNotRestoredByConfig() throws Exception {
        type("err");
        final DataGridConfig<String> config = view.getConfig();
        view.getAdapter().getColumnDefinitions().get(0).getController().setState(State.UNPINNED_HIDDEN);

        view.setConfig(new DataGridConfigBuilder<String>()
            .addColumnConfig(new DataGridConfig.ColumnConfig<>("name", State.UNPINNED_HIDDEN, 100))
            .addCustomValue(CONFIG_KEY, config.getCustomValue(CONFIG_KEY))
            .build());

        Assertions.assertEquals("", footer().getText());
    }

    @Test
    public void clearFilterOfAnotherKeyKeepsFooterText() throws Exception {
        type("err");
        final ColumnDefinition<String> name = view.getAdapter().getColumnDefinitions().get(0);

        name.onClearFilter("other");

        Assertions.assertEquals("err", footer().getText());
    }

    @Test
    public void customValuesRequireAnAdapter() {
        final ColumnFilterFooterDataGridView<String, String> noAdapter = new ColumnFilterFooterDataGridView<>(new DefaultDataGridView<>());

        Assertions.assertThrows(IllegalStateException.class, () -> noAdapter.encodeConfigCustomValue(CONFIG_KEY, Map.of()));
        Assertions.assertThrows(IllegalStateException.class, () -> noAdapter.decodeConfigCustomValue(CONFIG_KEY, ""));
    }

    @Test
    public void hidingColumnWithoutFilterKeepsData() {
        final ColumnDefinition<String> name = view.getAdapter().getColumnDefinitions().get(0);

        name.getController().setState(State.UNPINNED_HIDDEN);

        Assertions.assertEquals("", footer().getText());
        Assertions.assertEquals(List.of("apple", "banana", "cherry"), view.getFilteredData().getData());
    }

    @Test
    public void adapterCannotBeSetTwice() {
        final DataGridAdapter<String, String> adapter = view.getAdapter();
        Assertions.assertThrows(IllegalStateException.class, () -> view.setAdapter(adapter));
        Assertions.assertSame(adapter, view.getAdapter());
    }

    /** Types in the "name" footer, then waits for the draw that enables the footer again. */
    private void type(final String text) throws Exception {
        drawAfter(() -> DomEvents.stringValueChange(footer(), text));
        final long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (!footer().isEnabled() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Assertions.assertTrue(footer().isEnabled(), "footer enabled again after the draw");
    }

    private PTextBox footer() {
        return (PTextBox) view.getAdapter().getColumnDefinitions().get(0).getFooter();
    }

    private void drawAfter(final ThrowingRunnable action) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final DataGridView.DrawListener listener = count -> latch.countDown();
        base.addDrawListener(listener);
        try {
            action.run();
            Assertions.assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "Timed out waiting for grid draw");
        } finally {
            base.removeDrawListener(listener);
        }
    }

    private interface ThrowingRunnable {

        void run() throws Exception;
    }
}
