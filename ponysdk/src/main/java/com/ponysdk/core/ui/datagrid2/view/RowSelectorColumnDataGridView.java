/*
 * Copyright (c) 2019 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.adapter.DecoratorDataGridAdapter;
import com.ponysdk.core.ui.datagrid2.cell.Cell;
import com.ponysdk.core.ui.datagrid2.cell.CellController;
import com.ponysdk.core.ui.datagrid2.column.ColumnController;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnConfig;
import com.ponysdk.core.ui.datagrid2.config.DecoratorDataGridConfig;

/**
 * @author mbagdouri
 */
public class RowSelectorColumnDataGridView<K, V> extends DecoratorDataGridView<K, V> {

    private static final String COLUMN_ID = RowSelectorColumnDataGridView.RowSelectorColumnDefinition.class.getName();

    private RowSelectorColumnDataGridAdapter decoratorAdapter;

    public RowSelectorColumnDataGridView(final DataGridView<K, V> view) {
        super(view);
    }

    @Override
    public void setAdapter(final DataGridAdapter<K, V> adapter) {
        decoratorAdapter = new RowSelectorColumnDataGridAdapter(adapter);
        super.setAdapter(decoratorAdapter);
        decoratorAdapter.addColumnActionListeners();
        addDrawListener(() -> decoratorAdapter.rowSelectorColumn.refreshHeader());
    }

    @Override
    public void setConfig(final DataGridConfig<V> config) {
        if (decoratorAdapter == null
                || config.getColumnConfigs().size() > 0 && config.getColumnConfigs().get(0).getColumnId().equals(COLUMN_ID)) {
            super.setConfig(config);
            return;
        }
        final List<ColumnConfig<V>> copy = new ArrayList<>(config.getColumnConfigs());
        final Iterator<ColumnConfig<V>> iterator = copy.iterator();
        ColumnConfig<V> columnConfig = null;
        while (iterator.hasNext()) {
            final ColumnConfig<V> c = iterator.next();
            if (c.getColumnId().equals(COLUMN_ID)) {
                iterator.remove();
                columnConfig = c;
                break;
            }
        }
        if (columnConfig == null) columnConfig = new ColumnConfig<>(decoratorAdapter.columns.get(0));
        copy.add(0, columnConfig);
        final List<ColumnConfig<V>> cc = Collections.unmodifiableList(copy);
        super.setConfig(new DecoratorDataGridConfig<>(config) {

            @Override
            public List<ColumnConfig<V>> getColumnConfigs() {
                return cc;
            }

        });
    }

    private class RowSelectorColumnDataGridAdapter extends DecoratorDataGridAdapter<K, V> {

        private final List<ColumnDefinition<V>> columns;
        private final RowSelectorColumnDefinition rowSelectorColumn;

        RowSelectorColumnDataGridAdapter(final DataGridAdapter<K, V> adapter) {
            super(adapter);
            final List<ColumnDefinition<V>> c = adapter.getColumnDefinitions();
            columns = new ArrayList<>(c.size() + 1);
            rowSelectorColumn = new RowSelectorColumnDefinition(adapter);
            columns.add(rowSelectorColumn);
            columns.addAll(c);
        }

        @Override
        public List<ColumnDefinition<V>> getColumnDefinitions() {
            return columns;
        }

        private void addColumnActionListeners() {
            for (final ColumnDefinition<V> column : getColumnDefinitions()) {
                addColumnActionListener(column, rowSelectorColumn);
            }
        }

    }

    private class RowSelectorColumnDefinition implements ColumnDefinition<V> {

        private final PCheckBox header;
        private final PCheckBox footer;

        private ColumnController<V> columnController;

        RowSelectorColumnDefinition(final DataGridAdapter<K, V> adapter) {
            header = adapter.hasHeader() ? Element.newPCheckBox() : null;
            if (header != null) {
                header.addValueChangeHandler(this::onValueChange);
            }

            footer = adapter.hasFooter() ? Element.newPCheckBox() : null;
            if (footer != null) {
                footer.setStyleProperty("visibility", "hidden");
            }
        }

        private void refreshHeader() {
            if (header == null) return;
            final int selected = getView().getLiveSelectedData().size();
            if (selected == 0) header.setState(PCheckBoxState.UNCHECKED);
            // FIXME : make sure of the getLiveData().size that is no more
            // else if (selected == getView().getLiveData().size())
            // header.setState(PCheckBoxState.CHECKED);
            else if (selected == getView().getLiveDataRowCount()) header.setState(PCheckBoxState.CHECKED);
            else header.setState(PCheckBoxState.INDETERMINATE);
        }

        @Override
        public void onSort(final boolean asc) {
        }

        @Override
        public void onClearSort() {
        }

        @Override
        public void onFilter(final Object key, final BiPredicate<V, Supplier<Object>> filter, final boolean reinforcing) {
        }

        @Override
        public void onClearFilter(final Object key) {
        }

        @Override
        public void onClearFilters() {
        }

        @Override
        public void onRedraw(final boolean clearRenderingHelpers) {
        }

        @Override
        public IsPWidget getHeader() {
            return header;
        }

        @Override
        public IsPWidget getFooter() {
            return footer;
        }

        @Override
        public Cell<V> createCell() {
            return new RowSelectorColumnCell();
        }

        @Override
        public Object getRenderingHelper(final V data) {
            return null;
        }

        @Override
        public int compare(final V v1, final Supplier<Object> renderingHelper1, final V v2, final Supplier<Object> renderingHelper2) {
            return 0;
        }

        @Override
        public void setController(final ColumnController<V> columnController) {
            this.columnController = columnController;
        }

        @Override
        public ColumnController<V> getController() {
            return columnController;
        }

        @Override
        public boolean isVisibilitySwitchable() {
            return false;
        }

        @Override
        public boolean isPinSwitchable() {
            return false;
        }

        @Override
        public boolean isFilterable() {
            return false;
        }

        @Override
        public String getId() {
            return COLUMN_ID;
        }

        @Override
        public int getDefaultWidth() {
            return 20;
        }

        @Override
        public int getMinWidth() {
            return 20;
        }

        @Override
        public int getMaxWidth() {
            return 20;
        }

        @Override
        public void onStateChanged(final State state) {
        }

        @Override
        public State getDefaultState() {
            return State.PINNED_SHOWN;
        }

        @Override
        public boolean isSortable() {
            return false;
        }

        @Override
        public void onResized(final int width) {
        }

        @Override
        public boolean isResizable() {
            return false;
        }

        private void onValueChange(final PValueChangeEvent<Boolean> event) {
            if (event.getData()) {
                getView().selectAllLiveData();
            } else {
                getView().unselectAllData();
            }
            refreshHeader();
        }

        private class RowSelectorColumnCell implements Cell<V> {

            private final PCheckBox checkBox = Element.newPCheckBox();
            private final PWidget pending = Element.newSpan();

            @Override
            public PWidget asWidget() {
                return checkBox;
            }

            @Override
            public void render(final V data, final Object renderingHelper) {
            }

            @Override
            public void setController(final CellController<V> cellController) {
            }

            @Override
            public void select() {
                checkBox.setValue(true);
                refreshHeader();
            }

            @Override
            public void unselect() {
                checkBox.setValue(false);
                refreshHeader();
            }

            @Override
            public PWidget asPendingWidget() {
                return pending;
            }
        }

        @Override
        public IsPWidget getDraggableHeaderElement() {
            return null;
        }

    }

}