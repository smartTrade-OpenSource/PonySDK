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

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition.State;
import com.ponysdk.core.ui.datagrid2.view.DataGridView;

/**
 * {@code DataGridConfig} is a configuration of the {@link DataGridView} that
 * can be changed multiple times on the view
 *
 * @author mbagdouri
 */
public interface DataGridConfig<V> {

    public abstract List<Sort<V>> getSorts();

    public abstract List<ColumnConfig<V>> getColumnConfigs();

    public abstract Map<String, Object> getReadOnlyCustomValues();

    public abstract <T> T getCustomValue(String key);

    public abstract void setCustomValue(String key, Object value);

    public static class ColumnConfig<V> {

        private final String columnId;
        private final State state;
        private final int width;

        public ColumnConfig(final String columnId, final State state, final int width) {
            super();
            this.columnId = columnId;
            this.state = state;
            this.width = width;
        }

        public ColumnConfig(final ColumnDefinition<V> column) {
            this(column.getId(), column.getDefaultState(), column.getDefaultWidth());
        }

        public int getWidth() {
            return width;
        }

        public String getColumnId() {
            return columnId;
        }

        public State getState() {
            return state;
        }

    }

    public static class Sort<V> {

        private Sort() {

        }

    }

    public final static class ColumnSort<V> extends Sort<V> {

        private final String columnId;
        private final boolean asc;

        public ColumnSort(final String columnId, final boolean asc) {
            super();
            this.columnId = columnId;
            this.asc = asc;
        }

        public String getColumnId() {
            return columnId;
        }

        public boolean isAsc() {
            return asc;
        }

    }

    public final static class GeneralSort<V> extends Sort<V> {

        private final Object key;
        private final Comparator<V> comparator;

        public GeneralSort(final Object key, final Comparator<V> comparator) {
            super();
            this.key = key;
            this.comparator = comparator;
        }

        public Object getKey() {
            return key;
        }

        public Comparator<V> getComparator() {
            return comparator;
        }

    }
}
