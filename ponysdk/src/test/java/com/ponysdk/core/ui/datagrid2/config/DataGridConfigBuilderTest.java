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
package com.ponysdk.core.ui.datagrid2.config;

import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition.State;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnSort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.GeneralSort;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DataGridConfigBuilderTest {

    @Test
    public void buildKeepsSortsColumnConfigsAndCustomValues() {
        final Comparator<String> comparator = Comparator.naturalOrder();
        final DataGridConfig<String> config = new DataGridConfigBuilder<String>()
            .addSort(new ColumnSort<>("name", true))
            .addSort(new GeneralSort<>("key", comparator))
            .addColumnConfig(new ColumnConfig<>("name", State.PINNED_SHOWN, 120))
            .addCustomValue("custom", 42)
            .build();

        final List<DataGridConfig.Sort<String>> sorts = config.getSorts();
        Assertions.assertEquals(2, sorts.size());
        final ColumnSort<String> columnSort = (ColumnSort<String>) sorts.get(0);
        Assertions.assertEquals("name", columnSort.getColumnId());
        Assertions.assertTrue(columnSort.isAsc());
        final GeneralSort<String> generalSort = (GeneralSort<String>) sorts.get(1);
        Assertions.assertEquals("key", generalSort.getKey());
        Assertions.assertSame(comparator, generalSort.getComparator());

        final ColumnConfig<String> columnConfig = config.getColumnConfigs().get(0);
        Assertions.assertEquals("name", columnConfig.getColumnId());
        Assertions.assertEquals(State.PINNED_SHOWN, columnConfig.getState());
        Assertions.assertEquals(120, columnConfig.getWidth());

        Assertions.assertEquals(Integer.valueOf(42), config.getCustomValue("custom"));
        Assertions.assertEquals(Map.of("custom", 42), config.getReadOnlyCustomValues());
    }

    @Test
    public void builtConfigListsAreReadOnly() {
        final DataGridConfig<String> config = new DataGridConfigBuilder<String>().build();

        Assertions.assertThrows(UnsupportedOperationException.class, () -> config.getSorts().add(new ColumnSort<>("a", true)));
        Assertions.assertThrows(UnsupportedOperationException.class,
            () -> config.getColumnConfigs().add(new ColumnConfig<>("a", State.UNPINNED_SHOWN, 10)));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> config.getReadOnlyCustomValues().put("a", 1));
    }

    @Test
    public void setCustomValueIsVisibleThroughReadOnlyView() {
        final DataGridConfig<String> config = new DataGridConfigBuilder<String>().build();

        config.setCustomValue("k", "v");

        Assertions.assertEquals("v", config.getCustomValue("k"));
        Assertions.assertEquals("v", config.getReadOnlyCustomValues().get("k"));
    }

    @Test
    public void builderCannotBeUsedAfterBuild() {
        final DataGridConfigBuilder<String> builder = new DataGridConfigBuilder<>();
        builder.build();

        Assertions.assertThrows(IllegalArgumentException.class, builder::build);
        Assertions.assertThrows(IllegalArgumentException.class, () -> builder.addSort(new ColumnSort<>("a", true)));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> builder.addColumnConfig(new ColumnConfig<>("a", State.UNPINNED_SHOWN, 10)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> builder.addCustomValue("a", 1));
    }

    @Test
    public void columnConfigFromDefinitionUsesDefaults() {
        @SuppressWarnings("unchecked")
        final ColumnDefinition<String> column = Mockito.mock(ColumnDefinition.class);
        Mockito.when(column.getId()).thenReturn("col");
        Mockito.when(column.getDefaultState()).thenReturn(State.UNPINNED_HIDDEN);
        Mockito.when(column.getDefaultWidth()).thenReturn(77);

        final ColumnConfig<String> config = new ColumnConfig<>(column);

        Assertions.assertEquals("col", config.getColumnId());
        Assertions.assertEquals(State.UNPINNED_HIDDEN, config.getState());
        Assertions.assertEquals(77, config.getWidth());
    }

    @Test
    public void decoratorDelegatesToWrappedConfig() {
        final DataGridConfig<String> wrapped = new DataGridConfigBuilder<String>()
            .addSort(new ColumnSort<>("name", false))
            .addColumnConfig(new ColumnConfig<>("name", State.UNPINNED_SHOWN, 50))
            .addCustomValue("a", "b")
            .build();
        final DataGridConfig<String> decorator = new DecoratorDataGridConfig<>(wrapped) {
        };

        Assertions.assertSame(wrapped.getSorts(), decorator.getSorts());
        Assertions.assertSame(wrapped.getColumnConfigs(), decorator.getColumnConfigs());
        Assertions.assertEquals("b", decorator.getCustomValue("a"));

        decorator.setCustomValue("c", "d");

        Assertions.assertEquals("d", wrapped.getCustomValue("c"));
        Assertions.assertEquals(Map.of("a", "b", "c", "d"), decorator.getReadOnlyCustomValues());
    }
}
