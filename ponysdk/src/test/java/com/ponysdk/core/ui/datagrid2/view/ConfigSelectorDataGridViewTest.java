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

import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.datagrid2.DomEvents;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition.State;
import com.ponysdk.core.ui.datagrid2.column.DefaultColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnSort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.GeneralSort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfigBuilder;
import com.ponysdk.core.ui.datagrid2.data.FilterController;
import com.ponysdk.core.ui.datagrid2.datasource.DefaultCacheDataSource;
import com.ponysdk.core.ui.datagrid2.view.ConfigSelectorDataGridView.ConfigEntry;
import com.ponysdk.core.ui.datagrid2.view.DataGridView.DecodeException;
import com.ponysdk.core.ui.datagrid2.view.DefaultDataGridViewTest.RecordingAdapter;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the named configurations of {@link ConfigSelectorDataGridView}: selection, removal, and their binary encoding.
 */
public class ConfigSelectorDataGridViewTest extends PSuite {

    private static final String DEFAULT = "default";

    private ConfigSelectorDataGridView<String, String> view;

    @BeforeEach
    public void setUp() {
        view = newView(base());
    }

    @Test
    public void defaultEntryIsSelectedAfterSetAdapter() {
        Assertions.assertEquals(1, layout().getWidgetCount());
        Assertions.assertFalse(selectButton(0).isEnabled());
        Assertions.assertEquals(DEFAULT, selectButton(0).getText());
        Assertions.assertEquals(1, ((PComplexPanel) layout().getWidget(0)).getWidgetCount());
        Assertions.assertEquals(List.of(DEFAULT), keys(view.getConfigEntries()));
        Assertions.assertEquals(State.UNPINNED_SHOWN, stateOf(view.getCurrentConfig(), "len"));
    }

    @Test
    public void addConfigEntryRejectsDuplicateKeys() {
        Assertions.assertTrue(view.addConfigEntry("hideLen", hideLen()));
        Assertions.assertFalse(view.addConfigEntry("hideLen", hideLen()));
        Assertions.assertThrows(NullPointerException.class, () -> view.addConfigEntry("null", null));

        Assertions.assertEquals(2, layout().getWidgetCount());
        Assertions.assertEquals("X", ((PButton) ((PComplexPanel) layout().getWidget(1)).getWidget(1)).getText());
        Assertions.assertEquals(List.of(DEFAULT, "hideLen"), keys(view.getConfigEntries()));
    }

    @Test
    public void selectingEntryAppliesItsConfigAndKeepsThePreviousOne() {
        view.addConfigEntry("hideLen", hideLen());

        view.selectConfig("hideLen");

        Assertions.assertEquals(State.UNPINNED_HIDDEN, stateOf(view.getConfig(), "len"));
        Assertions.assertEquals(State.UNPINNED_HIDDEN, stateOf(view.getCurrentConfig(), "len"));
        Assertions.assertTrue(selectButton(0).isEnabled());
        Assertions.assertFalse(selectButton(1).isEnabled());

        view.selectConfig("hideLen");
        view.selectConfig("unknown");
        Assertions.assertEquals(State.UNPINNED_HIDDEN, stateOf(view.getConfig(), "len"));

        PEmulator.click(selectButton(0));
        Assertions.assertEquals(State.UNPINNED_SHOWN, stateOf(view.getConfig(), "len"));
        Assertions.assertFalse(selectButton(0).isEnabled());
    }

    @Test
    public void removingCurrentEntrySelectsDefault() {
        view.addConfigEntry("hideLen", hideLen());
        view.selectConfig("hideLen");

        final DataGridConfig<String> removed = view.removeConfigEntry("hideLen");

        Assertions.assertEquals(State.UNPINNED_HIDDEN, stateOf(removed, "len"));
        Assertions.assertEquals(List.of(DEFAULT), keys(view.getConfigEntries()));
        Assertions.assertEquals(1, layout().getWidgetCount());
        Assertions.assertEquals(State.UNPINNED_SHOWN, stateOf(view.getConfig(), "len"));
        Assertions.assertNull(view.removeConfigEntry("unknown"));
    }

    @Test
    public void removeButtonRemovesEntry() {
        view.addConfigEntry("hideLen", hideLen());

        PEmulator.click(((PComplexPanel) layout().getWidget(1)).getWidget(1));

        Assertions.assertEquals(List.of(DEFAULT), keys(view.getConfigEntries()));
    }

    @Test
    public void defaultEntryCannotBeRemoved() {
        Assertions.assertNotNull(view.removeConfigEntry(DEFAULT));

        Assertions.assertEquals(List.of(DEFAULT), keys(view.getConfigEntries()));
    }

    @Test
    public void removingDefaultEntryKeepsItsWidget() {
        view.removeConfigEntry(DEFAULT);

        Assertions.assertEquals(1, layout().getWidgetCount());
        Assertions.assertEquals(DEFAULT, selectButton(0).getText());
    }

    @Test
    public void setConfigEntriesKeepsDefaultFirst() {
        view.setConfigEntries(List.of(ConfigSelectorDataGridView.e("a", hideLen()), ConfigSelectorDataGridView.e("b", hideLen())));

        Assertions.assertEquals(List.of(DEFAULT, "a", "b"), keys(view.getConfigEntries()));
        Assertions.assertEquals(3, layout().getWidgetCount());
        Assertions.assertEquals(State.UNPINNED_SHOWN, stateOf(view.getConfig(), "len"));
    }

    @Test
    public void setConfigEntriesCanRedefineDefault() {
        view.setConfigEntries(List.of(ConfigSelectorDataGridView.e(DEFAULT, hideLen()), ConfigSelectorDataGridView.e("x", hideLen())));

        Assertions.assertEquals(List.of(DEFAULT, "x"), keys(view.getConfigEntries()));
        Assertions.assertEquals(State.UNPINNED_HIDDEN, stateOf(view.getConfig(), "len"));
    }

    @Test
    public void setConfigEntriesWithNoEntryKeepsDefault() {
        view.setConfigEntries(List.of());

        Assertions.assertEquals(List.of(DEFAULT), keys(view.getConfigEntries()));
    }

    @Test
    public void entriesSurviveEncodeDecode() throws DecodeException {
        final String longKey = "k".repeat(100);
        final DataGridConfig<String> config = new DataGridConfigBuilder<String>()
            .addSort(new ColumnSort<>("name", true))
            .addSort(new GeneralSort<>("general", Comparator.<String> naturalOrder()))
            .addColumnConfig(new ColumnConfig<>("len", State.UNPINNED_HIDDEN, 80))
            .addColumnConfig(new ColumnConfig<>("name", State.PINNED_SHOWN, 120))
            .build();

        final String encoded = view.encodeConfigEntries(List.of(ConfigSelectorDataGridView.e(longKey, config),
            ConfigSelectorDataGridView.e("é", new DataGridConfigBuilder<String>().build())));
        final List<ConfigEntry<String>> decoded = view.decodeConfigEntries(encoded);

        Assertions.assertEquals(List.of(longKey, "é"), keys(decoded));
        final DataGridConfig<String> first = decoded.get(0).getConfig();
        Assertions.assertEquals(2, first.getSorts().size());
        final ColumnSort<String> sort = (ColumnSort<String>) first.getSorts().get(0);
        Assertions.assertEquals("name", sort.getColumnId());
        Assertions.assertTrue(sort.isAsc());
        Assertions.assertNull(first.getSorts().get(1));
        Assertions.assertEquals(State.UNPINNED_HIDDEN, stateOf(first, "len"));
        Assertions.assertEquals(80, first.getColumnConfigs().get(0).getWidth());
        Assertions.assertEquals(State.PINNED_SHOWN, stateOf(first, "name"));
        Assertions.assertTrue(decoded.get(1).getConfig().getColumnConfigs().isEmpty());
    }

    @Test
    public void aColumnStateThisVersionDoesNotKnowIsDecodedAsNone() throws DecodeException {
        final DataGridConfig<String> config = new DataGridConfigBuilder<String>()
            .addColumnConfig(new ColumnConfig<>("name", State.PINNED_SHOWN, 120))
            .build();
        final String encoded = view.encodeConfigEntries(List.of(ConfigSelectorDataGridView.e("c", config)));
        // The same entry written by a version with a state named otherwise
        final String bytes = new String(Base64.getDecoder().decode(encoded), StandardCharsets.ISO_8859_1)
            .replace("PINNED_SHOWN", "PINNED_SHOWX");
        final String unknown = Base64.getEncoder().encodeToString(bytes.getBytes(StandardCharsets.ISO_8859_1));

        final ColumnConfig<String> decoded = view.decodeConfigEntries(unknown).get(0).getConfig().getColumnConfigs().get(0);

        Assertions.assertEquals("name", decoded.getColumnId());
        Assertions.assertNull(decoded.getState());
        Assertions.assertEquals(120, decoded.getWidth());
    }

    @Test
    public void customValuesSurviveEncodeDecode() throws Exception {
        final DefaultDataGridView<String, String> base = base();
        final ConfigSelectorDataGridView<String, String> withFooter = newView(new ColumnFilterFooterDataGridView<>(base));
        final PTextBox footer = (PTextBox) withFooter.getAdapter().getColumnDefinitions().get(0).getFooter();
        DomEvents.stringValueChange(footer, "b");

        final String encoded = withFooter.encodeConfigEntries(List.of(ConfigSelectorDataGridView.e("f", withFooter.getConfig())));
        final List<ConfigEntry<String>> decoded = withFooter.decodeConfigEntries(encoded);

        final Map<Object, String> filters = decoded.get(0).getConfig().getCustomValue(ColumnFilterFooterDataGridView.class.getName());
        Assertions.assertEquals(List.of("b"), new ArrayList<>(filters.values()));
    }

    @Test
    public void customValuesTheViewCannotEncodeAreSkipped() throws DecodeException {
        final DataGridConfig<String> config = new DataGridConfigBuilder<String>().addCustomValue("unknown", 1).build();

        final String encoded = view.encodeConfigEntries(List.of(ConfigSelectorDataGridView.e("c", config)));

        Assertions.assertTrue(view.decodeConfigEntries(encoded).get(0).getConfig().getReadOnlyCustomValues().isEmpty());
    }

    @Test
    public void invalidEncodingIsRejected() {
        Assertions.assertThrows(DecodeException.class, () -> view.decodeConfigEntries("not base64 !"));
        Assertions.assertThrows(DecodeException.class, () -> view.decodeConfigEntries("AAAA"));
        Assertions.assertThrows(DecodeException.class, () -> view.decodeConfigEntries("AAAAAQ=="));
    }

    @Test
    public void operationsRequireAnAdapter() {
        final ConfigSelectorDataGridView<String, String> noAdapter = new ConfigSelectorDataGridView<>(new DefaultDataGridView<>(), DEFAULT);

        Assertions.assertThrows(IllegalStateException.class, () -> noAdapter.setConfigEntries(List.of()));
        Assertions.assertThrows(IllegalStateException.class, () -> noAdapter.decodeConfigEntries(""));
        Assertions.assertThrows(IllegalStateException.class, () -> noAdapter.encodeConfigEntries(List.of()));
        Assertions.assertThrows(IllegalStateException.class, () -> noAdapter.addConfigEntry("a", hideLen()));
        Assertions.assertThrows(IllegalStateException.class, () -> noAdapter.removeConfigEntry("a"));
        Assertions.assertThrows(IllegalStateException.class, noAdapter::getCurrentConfig);
        Assertions.assertThrows(IllegalStateException.class, () -> noAdapter.selectConfig("a"));
        Assertions.assertThrows(IllegalStateException.class, noAdapter::getConfigEntries);
    }

    @Test
    public void simpleConfigEntryKeepsKeyAndConfig() {
        final DataGridConfig<String> config = hideLen();

        final ConfigEntry<String> entry = new ConfigSelectorDataGridView.SimpleConfigEntry<>("k", config);

        Assertions.assertEquals("k", entry.getKey());
        Assertions.assertSame(config, entry.getConfig());
    }

    private static DefaultDataGridView<String, String> base() {
        final DefaultCacheDataSource<String, String> source = new DefaultCacheDataSource<>();
        source.setFilterController(new FilterController<>());
        return new DefaultDataGridView<>(source);
    }

    private static ConfigSelectorDataGridView<String, String> newView(final DataGridView<String, String> inner) {
        final ConfigSelectorDataGridView<String, String> v = new ConfigSelectorDataGridView<>(inner, DEFAULT);
        v.setAdapter(new RecordingAdapter(List.<ColumnDefinition<String>> of(new DefaultColumnDefinition<>("name", s -> s, (s, t) -> {
        }), new DefaultColumnDefinition<>("len", String::length, (s, t) -> {
        })), true));
        return v;
    }

    private static DataGridConfig<String> hideLen() {
        return new DataGridConfigBuilder<String>().addColumnConfig(new ColumnConfig<>("len", State.UNPINNED_HIDDEN, 100)).build();
    }

    private static State stateOf(final DataGridConfig<String> config, final String columnId) {
        for (final ColumnConfig<String> c : config.getColumnConfigs()) {
            if (c.getColumnId().equals(columnId)) return c.getState();
        }
        throw new AssertionError("no column " + columnId);
    }

    private static List<String> keys(final List<ConfigEntry<String>> entries) {
        final List<String> keys = new ArrayList<>();
        entries.forEach(e -> keys.add(e.getKey()));
        return keys;
    }

    private PComplexPanel layout() {
        return (PComplexPanel) view.getDecoratorWidget();
    }

    private PButton selectButton(final int index) {
        return (PButton) ((PComplexPanel) layout().getWidget(index)).getWidget(0);
    }

    @Test
    public void aDescendingSortSurvivesEncodeDecode() throws DecodeException {
        final DataGridConfig<String> config = new DataGridConfigBuilder<String>().addSort(new ColumnSort<>("name", false)).build();

        final String encoded = view.encodeConfigEntries(List.of(ConfigSelectorDataGridView.e("d", config)));

        final ColumnSort<String> sort = (ColumnSort<String>) view.decodeConfigEntries(encoded).get(0).getConfig().getSorts().get(0);
        Assertions.assertFalse(sort.isAsc());
    }

    @Test
    public void aCustomValueAnotherViewEncodedIsSkipped() throws Exception {
        final ConfigSelectorDataGridView<String, String> withFooter = newView(new ColumnFilterFooterDataGridView<>(base()));
        final PTextBox footer = (PTextBox) withFooter.getAdapter().getColumnDefinitions().get(0).getFooter();
        DomEvents.stringValueChange(footer, "b");
        final String encoded = withFooter.encodeConfigEntries(List.of(ConfigSelectorDataGridView.e("f", withFooter.getConfig())));

        Assertions.assertTrue(view.decodeConfigEntries(encoded).get(0).getConfig().getReadOnlyCustomValues().isEmpty());
    }
}
