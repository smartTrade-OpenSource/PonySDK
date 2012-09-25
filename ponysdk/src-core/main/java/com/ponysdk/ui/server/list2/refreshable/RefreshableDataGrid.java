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

import com.ponysdk.ui.server.list.SimpleListView;
import com.ponysdk.ui.server.list2.DataGridActivity;
import com.ponysdk.ui.server.list2.DataGridColumnDescriptor;

public class RefreshableDataGrid<K, D> extends DataGridActivity<D> {

    private final Map<K, Map<RefreshableDataGridColumnDescriptor<K, D, ?>, Cell<D, ?>>> cells = new HashMap<K, Map<RefreshableDataGridColumnDescriptor<K, D, ?>, Cell<D, ?>>>();

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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setData(final K key, final D data) {
        Map<RefreshableDataGridColumnDescriptor<K, D, ?>, Cell<D, ?>> map = cells.get(key);
        if (map == null) {
            map = new HashMap<RefreshableDataGridColumnDescriptor<K, D, ?>, Cell<D, ?>>();
            cells.put(key, map);

            final int row = getVisibleItemCount() + 1;
            rows.add(data);
            int col = 0;

            for (final DataGridColumnDescriptor descriptor : columnDescriptors) {
                final RefreshableDataGridColumnDescriptor d = (RefreshableDataGridColumnDescriptor) descriptor;
                final Cell cell = new Cell();
                cell.col = col++;
                cell.data = data;
                cell.row = row;
                cell.value = d.getValueProvider().getValue(data);
                cell.w = d.getCellRenderer().render(row, data, cell.value);
                map.put(d, cell);
                view.addWidget(cell.w, cell.col, cell.row);
            }
        } else {
            for (final DataGridColumnDescriptor<D, ?> descriptor : columnDescriptors) {
                final RefreshableDataGridColumnDescriptor d = (RefreshableDataGridColumnDescriptor) descriptor;
                d.getCellRenderer().update(data, d.getValueProvider().getValue(data), map.get(d));
            }
        }
    }

    @Override
    public void clear() {
        super.clear();
        cells.clear();
    }

}
