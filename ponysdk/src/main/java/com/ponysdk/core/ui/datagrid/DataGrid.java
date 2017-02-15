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

package com.ponysdk.core.ui.datagrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PWidget;

public abstract class DataGrid<DataType> implements IsPWidget {

    private final int pageSize = Integer.MAX_VALUE;

    private final PFlexTable table;
    private final List<DataGridColumnDescriptor<DataType>> columnDescriptors = new ArrayList<>();
    private final Map<Integer, DataType> rows = new TreeMap<>();
    private final Map<Object, Integer> rowByKey = new HashMap<>();

    public DataGrid() {
        this.table = new PFlexTable();
    }

    public List<DataGridColumnDescriptor<DataType>> getColumnDescriptors() {
        return columnDescriptors;
    }

    public void addDataGridColumnDescriptor(final DataGridColumnDescriptor<DataType> columnDescriptor) {
        columnDescriptors.add(columnDescriptor);
    }

    public int getRowCount() {
        return rows.size();
    }

    @Override
    public PWidget asWidget() {
        return table.asWidget();
    }

    public void setData(final DataType data) {
        if (rows.size() < pageSize) {
            final Object key = getKey(data);
            final int row = rowByKey.get(key);

            if (row == -1) {
                rowByKey.put(row, key);
            } else {
                rows.put(row, data);
            }

            draw(row, data);
        }

    }

    protected abstract Object getKey(DataType data);

    private void draw(final int rowIndex, final DataType data) {
        int columnIndex = 0;
        for (final DataGridColumnDescriptor<DataType> descriptor : columnDescriptors) {
            final PWidget widget = table.getWidget(rowIndex, columnIndex);
            if (widget == null) {
                final IsPWidget newWidget = descriptor.getCellRenderer().render(data);
                table.setWidget(rowIndex, columnIndex, newWidget.asWidget());
            } else {
                descriptor.getCellRenderer().update(data, widget);
            }
            columnIndex++;
        }
    }

    public void removeData(final DataType data) {
        final Object key = getKey(data);
        final int row = keyByIndex.indexOf(key);

        rows.remove(row);
        keyByIndex.remove(key);

        for (int i = row; i < rows.size(); i++) {
            draw(i, getData(i));
        }
        reset(rows.size());
    }

    private DataType getData(final int rowIndex) {
        return rows.get(rowIndex);
    }

    private void reset(final int rowIndex) {
        int columnIndex = 0;
        for (final DataGridColumnDescriptor<DataType> descriptor : columnDescriptors) {
            final PWidget widget = table.getWidget(rowIndex, columnIndex);
            descriptor.getCellRenderer().reset(widget);
            columnIndex++;
        }
    }

    public void moveColumn(final int from, final int to) {
        if (from != to) {
            final DataGridColumnDescriptor<DataType> object = columnDescriptors.remove(from);
            columnDescriptors.add(to, object);
            //            table.moveColumn(from, to);
        }
    }

}
