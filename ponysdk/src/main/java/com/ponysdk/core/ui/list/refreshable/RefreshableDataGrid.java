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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.list.DataGridActivity;
import com.ponysdk.core.ui.list.DataGridColumnDescriptor;
import com.ponysdk.core.ui.list.SimpleListView;

/**
 * Extends {@link DataGridActivity} Capable of moving columns and refreshing a set of rows instead
 * of always refreshing the entire grid
 */
public class RefreshableDataGrid<K, D> extends DataGridActivity<D> {

    protected final Map<Integer, Map<DataGridColumnDescriptor<D, ?>, Cell<D, ? extends IsPWidget>>> cells = new HashMap<>();
    protected final List<K> keyByIndex = new ArrayList<>();

    public RefreshableDataGrid(final SimpleListView listView) {
        super(listView);
    }

    public void setData(final int rowIndex, final K key, final D data) {
        if (!cells.containsKey(rowIndex)) {
            addData(rowIndex, key, data);
        } else {
            updateData(rowIndex, key, data);
        }
    }

    private void addData(final int rowIndex, final K key, final D data) {
        final Map<DataGridColumnDescriptor<D, ?>, Cell<D, ? extends IsPWidget>> map = new HashMap<>();
        cells.put(rowIndex, map);
        keyByIndex.add(key);

        rows.add(data);
        dataCount++;

        int col = 0;

        for (final DataGridColumnDescriptor descriptor : columnDescriptors) {
            final Cell<D, IsPWidget> cell = new Cell<>();
            cell.setData(data);
            final IsPWidget widget = descriptor.getCellRenderer().render(rowIndex, descriptor.getValueProvider().apply(data));
            cell.setWidget(widget);
            map.put(descriptor, cell);
            view.addWidget(cell.getWidget(), col++, rowIndex + 1, 1);
        }
        view.addWidget(Element.newPSimplePanel(), col, rowIndex + 1, 1);
    }

    private void updateData(final int rowIndex, final K key, final D data) {
        final int previousIndex = keyByIndex.indexOf(key);

        if (previousIndex != -1) {
            if (rowIndex == previousIndex) {
                rows.set(previousIndex, data);
                keyByIndex.set(previousIndex, key);
            } else {
                rows.remove(previousIndex);
                keyByIndex.remove(previousIndex);

                if (rowIndex < rows.size()) {
                    rows.set(rowIndex, data);
                    keyByIndex.set(rowIndex, key);
                } else {
                    rows.add(rowIndex, data);
                    keyByIndex.add(rowIndex, key);
                }
            }
        } else {
            if (rowIndex < rows.size()) {
                rows.set(rowIndex, data);
                keyByIndex.set(rowIndex, key);
            } else {
                rows.add(rowIndex, data);
                keyByIndex.add(rowIndex, key);
            }
        }

        final Map<DataGridColumnDescriptor<D, ?>, Cell<D, ? extends IsPWidget>> map = cells.get(rowIndex);
        for (final DataGridColumnDescriptor descriptor : columnDescriptors) {
            final Cell<D, ? extends IsPWidget> current = map.get(descriptor);
            descriptor.getCellRenderer().update(descriptor.getValueProvider().apply(data), current);
            current.setData(data);
        }
    }

    public int getViewRowDataIndex(final D data) {
        return rows.indexOf(data);
    }

    public D getViewData(final int row) {
        final Map<DataGridColumnDescriptor<D, ?>, Cell<D, ? extends IsPWidget>> map = cells.get(row);
        return map != null ? map.values().iterator().next().getData() : null;
    }

    public void moveColumn(final int from, final int to) {
        if (from != to) {
            final DataGridColumnDescriptor<D, ?> object = columnDescriptors.remove(from);
            columnDescriptors.add(to, object);
            view.moveColumn(from, to);
        }
    }

    public int removeByKey(final K key) {
        final int removed = keyByIndex.indexOf(key);
        if (removed != -1) remove(removed);
        return removed;
    }

    @Override
    public void remove(final int rowIndex) {
        super.remove(rowIndex);

        cells.remove(rowIndex);
        keyByIndex.remove(rowIndex);

        // update model
        for (int i = rowIndex; i < rows.size(); i++) {
            final Map<DataGridColumnDescriptor<D, ?>, Cell<D, ? extends IsPWidget>> cellRow = cells.get(i);
            if (cellRow != null) {
                for (final Cell<D, ? extends IsPWidget> entry : cellRow.values()) {
                    entry.setRow(i);
                }
            }
        }
    }

    @Override
    public void clear() {
        view.clear(1);
        keyByIndex.clear();
        cells.clear();
        rows.clear();
    }

    @Override
    public void setData(final int row, final D data) {
        throw new RuntimeException("Use setData(key, data)");
    }

    @Override
    public void setData(final List<D> data) {
        throw new RuntimeException("Use setData(key, data)");
    }

    @Override
    public void remove(final D data) {
        throw new RuntimeException("Use removeByKey(key)");
    }

}
