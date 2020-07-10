/*
 * Copyright (c) 2019 PonySDK Owners: Luciano Broussal <luciano.broussal AT
 * gmail.com> Mathieu Barbier <mathieu.barbier AT gmail.com> Nicolas Ciaravola
 * <nicolas.ciaravola.pro AT gmail.com>
 *
 * WebSite: http://code.google.com/p/pony-sdk/
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.Sort;

/**
 * @author mbagdouri
 */
public class DataGridConfigBuilder<V> {

    private boolean built = false;
    private final List<Sort<V>> sorts = new ArrayList<>();
    private final List<ColumnConfig<V>> columnConfigs = new ArrayList<>();
    private final Map<String, Object> customValues = new HashMap<>();

    public DataGridConfigBuilder() {
    }

    void checkBuilt() {
        if (built)
            throw new IllegalArgumentException("A " + DataGridConfig.class + " object has already been built using this builder");
    }

    public DataGridConfig<V> build() {
        checkBuilt();
        built = true;
        return new BuilderDataGridConfig<>(sorts, columnConfigs, customValues);
    }

    public DataGridConfigBuilder<V> addSort(final Sort<V> sort) {
        checkBuilt();
        sorts.add(sort);
        return this;
    }

    public DataGridConfigBuilder<V> addColumnConfig(final ColumnConfig<V> config) {
        checkBuilt();
        columnConfigs.add(config);
        return this;
    }

    public DataGridConfigBuilder<V> addCustomValue(final String key, final Object value) {
        checkBuilt();
        customValues.put(key, value);
        return this;
    }

    private static class BuilderDataGridConfig<V> implements DataGridConfig<V> {

        private final List<Sort<V>> sorts;
        private final List<ColumnConfig<V>> columnConfigs;
        private final Map<String, Object> customValues;
        private final Map<String, Object> unmodifiableCustomValues;

        private BuilderDataGridConfig(final List<Sort<V>> sorts, final List<ColumnConfig<V>> columnConfigs,
                final Map<String, Object> customValues) {
            this.sorts = Collections.unmodifiableList(sorts);
            this.columnConfigs = Collections.unmodifiableList(columnConfigs);
            this.customValues = customValues;
            this.unmodifiableCustomValues = Collections.unmodifiableMap(customValues);
        }

        @Override
        public List<Sort<V>> getSorts() {
            return sorts;
        }

        @Override
        public void setCustomValue(final String key, final Object value) {
            customValues.put(key, value);
        }

        @Override
        public Map<String, Object> getReadOnlyCustomValues() {
            return unmodifiableCustomValues;
        }

        @Override
        public List<ColumnConfig<V>> getColumnConfigs() {
            return columnConfigs;
        }

        @Override
        public <T> T getCustomValue(final String key) {
            return (T) customValues.get(key);
        }
    }
}
