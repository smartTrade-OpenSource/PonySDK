/*
 * Copyright (c) 2019 PonySDK
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

package com.ponysdk.core.ui.datagrid2.column;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.adapter.DecoratorDataGridAdapter;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.view.DataGridView;
import com.ponysdk.core.ui.datagrid2.view.DecoratorDataGridView;
import com.ponysdk.core.util.StringUtils;

/**
 * @author mbagdouri
 */
public class ColumnFilterFooterDataGridView<K, V> extends DecoratorDataGridView<K, V> {

    private static final char SEPARATOR = '\u0000';
    private static final String SEPARATOR_STR = String.valueOf(SEPARATOR);
    private static final String TWO_SEPARATORS = SEPARATOR_STR + SEPARATOR_STR;
    private static final String CONFIG_KEY = ColumnFilterFooterDataGridView.class.getName();

    private PTextBox disabledFooter;

    private ColumnFilterFooterDataGridAdapter adapter;

    public ColumnFilterFooterDataGridView(final DataGridView<K, V> view) {
        super(view);
    }

    @Override
    public void setAdapter(final DataGridAdapter<K, V> adapter) {
        if (this.adapter == null) this.adapter = new ColumnFilterFooterDataGridAdapter(adapter);
        super.setAdapter(this.adapter);
        addDrawListener(this::enableFooter);
    }

    private void enableFooter() {
        if (disabledFooter == null) return;
        disabledFooter.setEnabled(true);
        disabledFooter.focus();
        // must be sent immediately
        Txn.get().flush(); // FIXME use Txn.get() ???
        disabledFooter = null;
    }

    private void checkAdapter() {
        if (adapter == null) throw new IllegalStateException("No " + DataGridAdapter.class + " has been set yet");
    }

    @Override
    public void setConfig(final DataGridConfig<V> config) {
        super.setConfig(config);
        final Map<FilterFooterColumn, String> filters = config.getCustomValue(CONFIG_KEY);
        if (filters == null) return;
        for (final Map.Entry<FilterFooterColumn, String> entry : filters.entrySet()) {
            if (entry.getKey().state.isShown()) {
                entry.getKey().footer.setText(entry.getValue());
                entry.getKey().filter(entry.getValue());
            }
        }
    }

    @Override
    public DataGridConfig<V> getConfig() {
        final DataGridConfig<V> config = super.getConfig();
        final Map<FilterFooterColumn, String> filters = new HashMap<>();
        for (final ColumnDefinition<V> column : adapter.filterFooterColumns.values()) {
            final FilterFooterColumn filterFooterColumn = (FilterFooterColumn) column;
            if (filterFooterColumn.filter.isEmpty()) continue;
            filters.put(filterFooterColumn, filterFooterColumn.filter);
        }
        if (filters.isEmpty()) return config;
        config.setCustomValue(CONFIG_KEY, filters);
        return config;
    }

    @Override
    public String encodeConfigCustomValue(final String key, final Object value) {
        if (!CONFIG_KEY.equals(key)) return super.encodeConfigCustomValue(key, value);
        checkAdapter();
        final Map<FilterFooterColumn, String> filters = (Map<FilterFooterColumn, String>) value;
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<FilterFooterColumn, String> entry : filters.entrySet()) {
            final String k = entry.getKey().getId().replace(SEPARATOR_STR, TWO_SEPARATORS);
            final String v = entry.getValue().replace(SEPARATOR_STR, TWO_SEPARATORS);
            sb.append(k).append(SEPARATOR).append(v).append(SEPARATOR);
        }
        return sb.toString();
    }

    @Override
    public Object decodeConfigCustomValue(final String key, final String value) throws DecodeException {
        if (!CONFIG_KEY.equals(key)) return super.decodeConfigCustomValue(key, value);
        checkAdapter();
        final Map<FilterFooterColumn, String> filters = new HashMap<>();
        String k = null;
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c != SEPARATOR) continue;
            if (i + 1 < value.length() && value.charAt(i + 1) == SEPARATOR) {
                i++;
                continue;
            }
            if (k == null) k = value.substring(start, i).replace(TWO_SEPARATORS, SEPARATOR_STR);
            else {
                final String v = value.substring(start, i).replace(TWO_SEPARATORS, SEPARATOR_STR);
                final FilterFooterColumn column = adapter.filterFooterColumns.get(k);
                if (column != null) {
                    filters.put(column, v);
                }
                k = null;
            }
            start = i + 1;
        }
        return filters;
    }

    private class ColumnFilterFooterDataGridAdapter extends DecoratorDataGridAdapter<K, V> {

        private final List<ColumnDefinition<V>> columns;
        private final Map<String, FilterFooterColumn> filterFooterColumns = new HashMap<>();

        private ColumnFilterFooterDataGridAdapter(final DataGridAdapter<K, V> adapter) {
            super(adapter);
            final List<ColumnDefinition<V>> c = adapter.getColumnDefinitions();
            columns = new ArrayList<>(c.size());
            for (final ColumnDefinition<V> column : c) {
                if (column.getFooter() != null) {
                    columns.add(column);
                } else {
                    final FilterFooterColumn filterFooterColumn = new FilterFooterColumn(column);
                    columns.add(filterFooterColumn);
                    filterFooterColumns.put(filterFooterColumn.getId(), filterFooterColumn);
                }
            }
        }

        @Override
        public List<ColumnDefinition<V>> getColumnDefinitions() {
            return columns;
        }

        @Override
        public boolean hasFooter() {
            return true;
        }
    }

    protected void onCreateFooterWidget(final ColumnDefinition<V> column, final PTextBox footer) {
        footer.setWidth(column.getDefaultWidth() * 0.8 + "px");
    }

    private class FilterFooterColumn extends DecoratorColumnDefinition<V> {

        private final PTextBox footer = Element.newPTextBox();
        private State state = getDefaultState();
        private String filter = "";

        private FilterFooterColumn(final ColumnDefinition<V> column) {
            super(column);
            if (isFilterable()) {
                this.footer.addValueChangeHandler(this::onValueChange);

                // only to receive value change events on every key
                this.footer.addKeyUpHandler(e -> {
                    // do nothing
                });
            } else {
                this.footer.setEnabled(false);
            }
            onCreateFooterWidget(column, footer);
        }

        @Override
        public IsPWidget getFooter() {
            return footer;
        }

        private void onValueChange(final PValueChangeEvent<String> event) {
            footer.setEnabled(false);
            // must be sent immediately
            Txn.get().flush(); // FIXME use Txn.get() ???
            disabledFooter = footer;
            filter(event.getData());
        }

        private void filter(final String f) {
            final String newFilter = f.trim();
            final boolean reinforcing = newFilter.contains(filter);
            filter = newFilter;
            final ColumnController<V> grip = getController();
            if (grip == null) return;
            if (filter.isEmpty()) grip.clearFilter(FilterFooterColumn.this);
            else grip.filter(FilterFooterColumn.this,
                (data, helper) -> StringUtils.containsIgnoreCase(helper.get().toString(), newFilter), reinforcing);
        }

        @Override
        public void onClearFilter(final Object key) {
            if (key == FilterFooterColumn.this) {
                footer.setText(filter = "");
                return;
            }
            super.onClearFilter(key);
        }

        @Override
        public void onClearFilters() {
            footer.setText(filter = "");
            super.onClearFilters();
        }

        @Override
        public void onStateChanged(final State state) {
            this.state = state;
            if (!state.isShown()) {
                footer.setText(filter = "");
            }
            super.onStateChanged(state);
        }

    }
}