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

package com.ponysdk.ui.server.list2.refreshable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.list.SimpleListView;
import com.ponysdk.ui.server.list2.DataGridActivity;
import com.ponysdk.ui.server.list2.DataGridColumnDescriptor;

public class RefreshableDataGrid<K, D> extends DataGridActivity<D> {

    protected final Map<K, Map<RefreshableDataGridColumnDescriptor<K, D, ?>, Cell<D, ?>>> cells = new HashMap<K, Map<RefreshableDataGridColumnDescriptor<K, D, ?>, Cell<D, ?>>>();
    protected final Map<K, D> valueByKey = new HashMap<K, D>();

    public RefreshableDataGrid(final SimpleListView listView) {
        super(listView);
    }

    @Override
    public void addDataGridColumnDescriptor(final DataGridColumnDescriptor<D, ?> columnDescriptor) {
        throw new IllegalArgumentException("use RefreshableDataGridColumnDescriptor instead of DataGridColumnDescriptor");
    }

    public void addDataGridColumnDescriptor(final RefreshableDataGridColumnDescriptor<D, ?, ?> columnDescriptor) {
        super.addDataGridColumnDescriptor(columnDescriptor);
    }

    @Override
    public void setData(final List<D> data) {
        throw new RuntimeException("Use setData(key, data)");
    }

    @Override
    public void remove(final D data) {
        throw new RuntimeException("Use removeByKey(key)");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setData(final K key, final D data) {
        Map<RefreshableDataGridColumnDescriptor<K, D, ?>, Cell<D, ?>> map = cells.get(key);
        if (map == null) {
            map = new HashMap<RefreshableDataGridColumnDescriptor<K, D, ?>, Cell<D, ?>>();
            cells.put(key, map);

            final int row = getVisibleItemCount();
            rows.add(data);
            valueByKey.put(key, data);

            int col = 0;

            for (final DataGridColumnDescriptor descriptor : columnDescriptors) {
                final RefreshableDataGridColumnDescriptor d = (RefreshableDataGridColumnDescriptor) descriptor;
                final Cell cell = new Cell();
                cell.col = col++;
                cell.data = data;
                cell.row = row;
                cell.value = d.getValueProvider().getValue(data);
                cell.w = d.getCellRenderer().render(row, cell.value);
                map.put(d, cell);
                view.addWidget(cell.w, cell.col, cell.row + 1);
            }
            view.addWidget(new PSimplePanel(), col, row + 1);
            view.addRowStyle(row + 1, PonySDKTheme.SIMPLELIST_ROW);

        } else {
            for (final DataGridColumnDescriptor<D, ?> descriptor : columnDescriptors) {
                final RefreshableDataGridColumnDescriptor d = (RefreshableDataGridColumnDescriptor) descriptor;
                final Object value = d.getValueProvider().getValue(data);
                d.getCellRenderer().update(value, map.get(d));
                map.get(d).data = data;
                map.get(d).value = value;
            }
        }
    }

    public void removeByKey(final K key) {
        final D removed = valueByKey.remove(key);
        if (removed != null) {
            cells.remove(key);
            super.remove(removed);
        }
    }

    public int getRow(final K key) {
        final Map<RefreshableDataGridColumnDescriptor<K, D, ?>, Cell<D, ?>> map = cells.get(key);
        if (map == null) return -1;
        return map.entrySet().iterator().next().getValue().row;
    }

    public D getData(final K key) {
        final Map<RefreshableDataGridColumnDescriptor<K, D, ?>, Cell<D, ?>> map = cells.get(key);
        if (map == null) return null;
        return map.entrySet().iterator().next().getValue().data;
    }

    @Override
    public void clear() {
        view.clear(1);
        cells.clear();
    }

}
