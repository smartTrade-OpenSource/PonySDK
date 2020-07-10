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

import java.util.function.BiPredicate;
import java.util.function.Supplier;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.datagrid2.cell.Cell;

/**
 * @author mbagdouri
 */
public class DecoratorColumnDefinition<V> implements ColumnDefinition<V> {

    private final ColumnDefinition<V> column;

    protected DecoratorColumnDefinition(final ColumnDefinition<V> column) {
        super();
        this.column = column;
    }

    protected ColumnDefinition<V> getColumn() {
        return column;
    }

    @Override
    public void onSort(final boolean asc) {
        column.onSort(asc);
    }

    @Override
    public void onClearSort() {
        column.onClearSort();
    }

    @Override
    public void onFilter(final Object key, final BiPredicate<V, Supplier<Object>> filter, final boolean reinforcing) {
        column.onFilter(key, filter, reinforcing);
    }

    @Override
    public void onClearFilter(final Object key) {
        column.onClearFilter(key);
    }

    @Override
    public void onClearFilters() {
        column.onClearFilters();
    }

    @Override
    public void onRedraw(final boolean clearRenderingHelpers) {
        column.onRedraw(clearRenderingHelpers);
    }

    @Override
    public void onStateChanged(final State state) {
        column.onStateChanged(state);
    }

    @Override
    public IsPWidget getHeader() {
        return column.getHeader();
    }

    @Override
    public IsPWidget getFooter() {
        return column.getFooter();
    }

    @Override
    public Cell<V> createCell() {
        return column.createCell();
    }

    @Override
    public Object getRenderingHelper(final V data) {
        return column.getRenderingHelper(data);
    }

    @Override
    public int compare(final V v1, final Supplier<Object> renderingHelper1, final V v2, final Supplier<Object> renderingHelper2) {
        return column.compare(v1, renderingHelper1, v2, renderingHelper2);
    }

    @Override
    public State getDefaultState() {
        return column.getDefaultState();
    }

    @Override
    public boolean isVisibilitySwitchable() {
        return column.isVisibilitySwitchable();
    }

    @Override
    public boolean isPinSwitchable() {
        return column.isPinSwitchable();
    }

    @Override
    public boolean isFilterable() {
        return column.isFilterable();
    }

    @Override
    public String getId() {
        return column.getId();
    }

    @Override
    public int getDefaultWidth() {
        return column.getDefaultWidth();
    }

    @Override
    public void setController(final ColumnController<V> columnController) {
        column.setController(columnController);
    }

    @Override
    public ColumnController<V> getController() {
        return column.getController();
    }

    @Override
    public boolean isSortable() {
        return column.isSortable();
    }

    @Override
    public void onResized(final int width) {
        column.onResized(width);
    }

    @Override
    public boolean isResizable() {
        return column.isResizable();
    }

    @Override
    public int getMinWidth() {
        return column.getMinWidth();
    }

    @Override
    public int getMaxWidth() {
        return column.getMaxWidth();
    }

    @Override
    public IsPWidget getDraggableHeaderElement() {
        return column.getDraggableHeaderElement();
    }

}
