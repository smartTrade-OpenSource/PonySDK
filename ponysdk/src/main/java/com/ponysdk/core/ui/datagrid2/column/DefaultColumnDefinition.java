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

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.datagrid2.cell.Cell;
import com.ponysdk.core.ui.datagrid2.cell.LabelCell;

/**
 * @author mbagdouri
 */
public class DefaultColumnDefinition<V> implements ColumnDefinition<V> {

    private final String columnName;
    private final Function<V, Object> columnValueFn;
    private final BiConsumer<V, String> columnEditFn;
    private final PComplexPanel header = Element.newDiv();
    private final PLabel columnNameLabel = Element.newPLabel();
    private final PLabel pinLabel = Element.newPLabel();
    private final PLabel hideLabel = Element.newPLabel();
    private ColumnController<V> columnController;
    private Boolean sort;
    private State state = getDefaultState();

    public DefaultColumnDefinition(final String columnName, final Function<V, Object> columnValueFn,
            final BiConsumer<V, String> columnEditFn) {
        super();
        this.columnName = columnName;
        this.columnValueFn = columnValueFn;
        this.columnEditFn = columnEditFn;
        initHeader();
    }

    private void initHeader() {
        columnNameLabel.setText(columnName);
        columnNameLabel.addClickHandler(e -> {
            if (columnController == null) return;
            columnController.sort(sort == null || !sort);
        });
        this.header.add(columnNameLabel);

        if (isPinSwitchable()) {
            this.pinLabel.setText(state.isPinned() ? "[unpin me]" : "[pin me]");
            this.pinLabel.addClickHandler(e -> {
                if (columnController == null) return;
                columnController.setState(state.isPinned() ? state.onUnpin() : state.onPin());
            });
        }
        this.header.add(pinLabel);

        if (isVisibilitySwitchable()) {
            this.hideLabel.setText("[hide me]");
            this.hideLabel.addClickHandler(e -> {
                if (columnController == null) return;
                columnController.setState(state.isShown() ? state.onHide() : state.onShow());
            });
        }
        this.header.add(hideLabel);
    }

    @Override
    public PComplexPanel getHeader() {
        return header;
    }

    @Override
    public Cell<V> createCell() {
        return new LabelCell<>(columnEditFn, (int) (getDefaultWidth() * 0.8));
    }

    @Override
    public Object getRenderingHelper(final V data) {
        return columnValueFn.apply(data).toString();
    }

    @Override
    public int compare(final V v1, final Supplier<Object> renderingHelper1, final V v2, final Supplier<Object> renderingHelper2) {
        return renderingHelper1.get().toString().compareTo(renderingHelper2.get().toString());
    }

    @Override
    public String toString() {
        return "SimpleColumnDefinition [columnName=" + columnName + "]";
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
    public IsPWidget getFooter() {
        return null;
    }

    @Override
    public boolean isVisibilitySwitchable() {
        return true;
    }

    @Override
    public String getId() {
        return columnName;
    }

    @Override
    public void onSort(final boolean asc) {
        sort = asc;

    }

    @Override
    public void onClearSort() {
        sort = null;
    }

    @Override
    public void onFilter(final Object key, final BiPredicate<V, Supplier<Object>> filter, final boolean reinforcing) {
    }

    @Override
    public void onClearFilter(final Object key) {
        if (key != this) return;
        onClearFilters();
    }

    @Override
    public void onClearFilters() {
    }

    @Override
    public void onRedraw(final boolean clearRenderingHelpers) {
    }

    @Override
    public boolean isPinSwitchable() {
        return true;
    }

    @Override
    public boolean isFilterable() {
        return true;
    }

    @Override
    public int getDefaultWidth() {
        return 140;
    }

    @Override
    public void onStateChanged(final State state) {
        this.state = state;
        pinLabel.setText(state.isPinned() ? "[unpin me]" : "[pin me]");
    }

    @Override
    public State getDefaultState() {
        return State.UNPINNED_SHOWN;
    }

    @Override
    public boolean isSortable() {
        return true;
    }

    @Override
    public void onResized(final int width) {
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public int getMinWidth() {
        return 50;
    }

    @Override
    public int getMaxWidth() {
        return Integer.MAX_VALUE;
    }

    @Override
    public IsPWidget getDraggableHeaderElement() {
        return columnNameLabel;
    }

}