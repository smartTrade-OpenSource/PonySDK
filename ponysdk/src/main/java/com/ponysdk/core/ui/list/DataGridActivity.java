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

package com.ponysdk.core.ui.list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PWidget;

/***
 * A Grid of data that supports paging and columns.
 *
 * @see DataGridColumnDescriptor
 */
public class DataGridActivity<D> implements HasPData<D>, IsPWidget {

    protected final SimpleListView view;
    protected final List<DataGridColumnDescriptor<D, ?>> columnDescriptors = new ArrayList<>();
    protected final List<D> rows = new ArrayList<>();
    private final Map<D, Integer> subListSizeByFather = new HashMap<>();
    protected int dataCount = 0;
    protected int colCount = 0;

    public DataGridActivity(final SimpleListView listView) {
        this.view = listView;
    }

    public void addDataGridColumnDescriptor(final DataGridColumnDescriptor<D, ?> columnDescriptor) {
        columnDescriptors.add(columnDescriptor);
        view.addWidget(columnDescriptor.getHeaderCellRenderer().render(), colCount++, 0, 1);
    }

    public void setData(final int row, final D data) {
        int col = 0;

        for (final DataGridColumnDescriptor<D, ?> field : columnDescriptors) {
            final IsPWidget renderCell = field.renderCell(row, data);
            view.addWidget(renderCell, col++, row + 1, 1);
        }
        view.addWidget(Element.newPSimplePanel(), col, row + 1, 1);
    }

    public void insertSubList(final D fatherData, final List<D> datas) {
        if (datas.isEmpty()) return;

        final int fatherRow = getModelRowIndex(fatherData);
        subListSizeByFather.put(fatherData, datas.size());

        // rows start includes data only
        // views include grid header (consumes 1 line) + data
        // therefore for a given "data", index is n in rows and n+1 in view
        int subRow = fatherRow + 1;
        for (final D data : datas) {
            this.rows.add(subRow, data);
            this.view.insertRow(subRow + 1);
            this.view.addRowStyle(subRow + 1, "pony-SimpleList-SubRow");
            int col = 0;
            for (final DataGridColumnDescriptor<D, ?> field : this.columnDescriptors) {
                this.view.addWidget(field.renderSubCell(subRow + 1, data), col++, subRow + 1, 1);
            }
            this.view.addWidget(Element.newPSimplePanel(), col, subRow + 1, 1);
            subRow++;
        }
    }

    public void removeSubList(final D fatherData) {
        final int fatherRow = getModelRowIndex(fatherData);
        final Integer subListSize = subListSizeByFather.remove(fatherData);

        if (subListSize != null) {
            final int subRow = fatherRow + 1;
            for (int i = 1; i <= subListSize; ++i) {
                this.rows.remove(subRow);
                this.view.removeRow(subRow + 1);
            }
        }
    }

    public void clear() {
        view.clearList();
    }

    public void selectRow(final D data) {
        view.selectRow(getModelRowIndex(data));
    }

    public void unSelectRow(final D data) {
        view.unSelectRow(getModelRowIndex(data));
    }

    public D getRow(final int rowIndex) {
        return rows.get(rowIndex);
    }

    public int getModelRowIndex(final D data) {
        return rows.indexOf(data);
    }

    public int getRowCount() {
        return rows.size();
    }

    public int getVisibleItemCount() {
        return dataCount;
    }

    public Iterable<D> getVisibleItems() {

        return new Iterable<>() {

            private final Iterator<D> rowsIterator = rows.iterator();
            private D next;
            private final Iterator<D> visibleItemIterator = new Iterator<>() {

                @Override
                public boolean hasNext() {
                    while (rowsIterator.hasNext()) {
                        next = rowsIterator.next();
                        if (next != null) return true;
                    }
                    return false;
                }

                @Override
                public D next() {
                    if (hasNext()) return next;
                    else throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    throw new IllegalAccessError("Not implemented");
                }
            };

            @Override
            public Iterator<D> iterator() {
                return visibleItemIterator;
            }
        };
    }

    @Override
    public void setData(final List<D> data) {
        rows.clear();

        rows.addAll(data);
        dataCount = data.size();

        view.clear(1);
        subListSizeByFather.clear();

        int rowIndex = 0;
        for (final D t : data) {
            setData(rowIndex++, t);
        }
    }

    public void remove(final int index) {
        view.removeRow(index + 1);

        final D removed = rows.remove(index);
        if (removed != null) dataCount--;
    }

    protected void remove(final D data) {
        remove(rows.indexOf(data));
    }

    public List<DataGridColumnDescriptor<D, ?>> getColumnDescriptors() {
        return columnDescriptors;
    }

    public SimpleListView getListView() {
        return view;
    }

    @Override
    public PWidget asWidget() {
        return view.asWidget();
    }

    public void insertRow(final int row, final int column, final int colSpan, final PWidget widget) {
        if (row > getRowCount() + 1) throw new IndexOutOfBoundsException("row (" + row + ") > size (" + getRowCount() + ")");

        rows.add(row, null);

        view.insertRow(row + 1);
        view.addWidget(widget, column, row + 1, colSpan);
    }

    public void insertWidget(final int row, final int column, final int colSpan, final PWidget widget) {
        view.addWidget(widget, column, row, colSpan);
    }

}
