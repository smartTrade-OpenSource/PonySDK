/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.core.ui.list.refreshable;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.list.DataGridActivity;
import com.ponysdk.core.ui.list.DataGridColumnDescriptor;
import com.ponysdk.core.ui.list.SimpleListView;

import java.util.*;
import java.util.Map.Entry;

/**
 * Extends {@link DataGridActivity} Capable of moving columns and refreshing a
 * set of rows instead of always refreshing the entire grid
 *
 * @param <K>
 * @param <D>
 */
public class RefreshableDataGrid<K, D> extends DataGridActivity<D> {

    private final Map<K, Map<DataGridColumnDescriptor<K, D>, Cell<D, ? extends IsPWidget>>> cells = new HashMap<>();
    private final List<K> keyByIndex = new ArrayList<>();
    private final Map<K, D> valueByKey = new HashMap<>();

    public RefreshableDataGrid(final SimpleListView listView) {
        super(listView);
    }

    @Override
    public void setData(final List<D> data) {
        throw new RuntimeException("Use setData(key, data)");
    }

    @Override
    public void addData(final D data) {
        throw new RuntimeException("Use setData(key, data)");
    }

    @Override
    public void remove(final D data) {
        throw new RuntimeException("Use removeByKey(key)");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setData(final K key, final D data) {
        Map<DataGridColumnDescriptor<K, D>, Cell<D, ? extends IsPWidget>> map = cells.get(key);
        if (map == null) {
            final int row = getRowCount();
            map = new HashMap<>();
            cells.put(key, map);
            keyByIndex.add(key);

            rows.add(data);
            dataCount++;

            valueByKey.put(key, data);

            int col = 0;

            for (final DataGridColumnDescriptor descriptor : columnDescriptors) {
                final Cell cell = new Cell();
                cell.setCol(col++);
                cell.setData(data);
                cell.setRow(row);
                cell.setValue(descriptor.getValueProvider().getValue(data));
                cell.setW(descriptor.getCellRenderer().render(row, cell.getValue()));
                map.put(descriptor, cell);
                view.addWidget(cell.getW(), cell.getCol(), cell.getRow() + 1, 1);
            }
            view.addWidget(new PSimplePanel(), col, row + 1, 1);
        } else {
            final D previousData = valueByKey.remove(key);
            final int previousIndex = rows.indexOf(previousData);
            rows.remove(previousIndex);
            rows.add(previousIndex, data);
            valueByKey.put(key, data);

            for (final DataGridColumnDescriptor descriptor : columnDescriptors) {
                final Object value = descriptor.getValueProvider().getValue(data);
                descriptor.getCellRenderer().update(value, map.get(descriptor));
                map.get(descriptor).setData(data);
                map.get(descriptor).setValue(value);
            }
        }
    }

    public void removeByKey(final K key) {
        final D removed = valueByKey.get(key);
        if (removed != null) {
            super.remove(removed);
        }
    }

    @Override
    public void remove(final int index) {
        super.remove(index);

        final K k = keyByIndex.remove(index);
        if (k != null) {
            valueByKey.remove(k);
            cells.remove(k);
        }

        updateRowIndex(index);
    }

    @Override
    protected void updateRowIndex(final int min) {
        // update model
        for (int i = min; i < rows.size(); i++) {
            final K k = keyByIndex.get(i);
            if (k != null) {
                final Map<DataGridColumnDescriptor<K, D>, Cell<D, ? extends IsPWidget>> cellRow = cells.get(k);
                for (final Entry<DataGridColumnDescriptor<K, D>, Cell<D, ? extends IsPWidget>> entry : cellRow
                        .entrySet()) {
                    entry.getValue().setRow(i);
                }
            }
        }
    }

    @Override
    public void insertRow(final int row, final int column, final int colSpan, final PWidget widget) {
        keyByIndex.add(row, null);
        super.insertRow(row, column, colSpan, widget);
    }

    public void moveRow(final K key, final int beforeIndex) {
        final Map<DataGridColumnDescriptor<K, D>, Cell<D, ? extends IsPWidget>> map = cells.get(key);
        if (map == null) throw new IndexOutOfBoundsException("cell not found");

        final Cell<D, ? extends IsPWidget> cell = map.entrySet().iterator().next().getValue();
        final int realRow = cell.getRow();

        if (realRow == beforeIndex) return;

        view.moveRow(realRow + 1, beforeIndex + 1);

        final D data = valueByKey.get(key);
        final int row = getDataIndex(data);

        keyByIndex.remove(row);
        keyByIndex.add(beforeIndex, key);

        // permutation
        rows.remove(row);
        rows.add(beforeIndex, cell.getData());

        final int min = Math.min(row, beforeIndex);
        updateRowIndex(min);
    }

    public void moveColumn(final int index, final int beforeIndex) {
        throw new RuntimeException("not yet implemented");
    }

    public int getRow(final K key) {
        final Map<DataGridColumnDescriptor<K, D>, Cell<D, ? extends IsPWidget>> map = cells.get(key);
        if (map == null) return -1;
        return map.entrySet().iterator().next().getValue().getRow();
    }

    @SuppressWarnings("unchecked")
    public <W extends IsPWidget> Collection<Cell<D, W>> getColumn(final DataGridColumnDescriptor<D, ?> descriptor) {
        final List<Cell<D, W>> c = new ArrayList<>();
        final Collection<Map<DataGridColumnDescriptor<K, D>, Cell<D, ? extends IsPWidget>>> values = cells
                .values();
        for (final Map<DataGridColumnDescriptor<K, D>, Cell<D, ? extends IsPWidget>> map : values) {
            final Cell<D, W> cell = (Cell<D, W>) map.get(descriptor);
            if (cell != null) c.add(cell);
        }
        return c;
    }

    public D getData(final K key) {
        final Map<DataGridColumnDescriptor<K, D>, Cell<D, ? extends IsPWidget>> map = cells.get(key);
        if (map == null) return null;
        return map.entrySet().iterator().next().getValue().getData();
    }

    @Override
    public void clear() {
        view.clear(1);
        cells.clear();
        keyByIndex.clear();
        valueByKey.clear();
    }

}
