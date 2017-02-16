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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PWidget;

public class DataGrid<DataType> implements IsPWidget {

    private final int pageSize = Integer.MAX_VALUE;

    private final PFlexTable table;
    private final Set<ColumnDescriptor<DataType>> columnDescriptors = new HashSet<>();
    private final Map<Integer, ColumnDescriptor<DataType>> descriptorsByColumn = new TreeMap<>();
    private final Map<Integer, DataType> rows = new TreeMap<>();
    private final Map<Object, Integer> rowByKey = new HashMap<>();

    private Function<DataType, ? extends Object> keyProvider = Function.identity();

    public DataGrid() {
        this.table = new PFlexTable();
    }

    public DataGrid(final Function<DataType, Object> keyProvider) {
        this();
        this.keyProvider = keyProvider;
    }

    public void addColumnDescriptor(final ColumnDescriptor<DataType> columnDescriptor) {
        if (columnDescriptors.add(columnDescriptor)) {
            descriptorsByColumn.put(columnDescriptors.size() - 1, columnDescriptor);
            drawHeader(columnDescriptor);
        }
    }

    @Override
    public PWidget asWidget() {
        return table.asWidget();
    }

    public void setData(final DataType data) {
        if (rows.size() < pageSize) {
            final Object key = keyProvider.apply(data);
            Integer row = rowByKey.get(key);

            if (row == null) {
                row = rows.size();
                rowByKey.put(key, row);
            }

            rows.put(row, data);

            draw(row, data);
        }
    }

    private void drawHeader(final ColumnDescriptor<DataType> descriptor) {
        final IsPWidget w = descriptor.getHeaderCellRenderer().render();
        table.setWidget(0, columnDescriptors.size() - 1, w);
    }

    private void draw(final int rowIndex, final DataType data) {
        int columnIndex = 0;
        for (final ColumnDescriptor<DataType> descriptor : columnDescriptors) {
            final PWidget w = table.getWidget(rowIndex, columnIndex);
            if (w == null) {
                table.setWidget(rowIndex, columnIndex, descriptor.getCellRenderer().render(data));
            } else {
                descriptor.getCellRenderer().update(data, w);
            }
            columnIndex++;
        }
    }

    public void removeData(final DataType data) {
        final Integer row = rowByKey.remove(keyProvider.apply(data));

        if (row == null) return;

        rows.remove(row);

        for (int i = row; i < rows.size(); i++) {
            draw(i, rows.get(row));
        }

        resetRow(rows.size());
    }

    private void resetColumn(final Integer column) {
        final ColumnDescriptor<DataType> descriptor = descriptorsByColumn.get(column);
        for (int row = 0; row < rows.size(); row++) {
            final PWidget widget = table.getWidget(row, column);
            descriptor.getCellRenderer().reset(widget);
        }
    }

    private void resetRow(final Integer row) {
        for (int column = 0; column < columnDescriptors.size(); column++) {
            final ColumnDescriptor<DataType> descriptor = descriptorsByColumn.get(column);
            final PWidget widget = table.getWidget(row, column);
            descriptor.getCellRenderer().reset(widget);
        }
    }

    public void moveColumn(final int from, final int to) {
        if (from != to) {
            final ColumnDescriptor<DataType> object = descriptorsByColumn.remove(from);
            //            descriptorsByColumn.add(to, object);
            //            table.moveColumn(from, to);
        }
    }

}
