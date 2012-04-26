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

package com.ponysdk.ui.server.list2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.core.activity.AbstractActivity;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.list.SimpleListView;

public class SimpleListActivity<T> extends AbstractActivity implements HasPData<T> {

    private final SimpleListView view;

    private PSelectionModel<T> setSelectionModel;

    private final List<DataGridColumnDescriptor<T, ?>> listFields;

    private Map<Integer, Integer> subListSizeByFather = new HashMap<Integer, Integer>();

    private int colCount;

    public SimpleListActivity(final SimpleListView listView, final List<DataGridColumnDescriptor<T, ?>> listFields) {
        this.listFields = listFields;
        this.view = listView;

        this.colCount = 0;

        this.view.setColumns(listFields.size());

        for (final DataGridColumnDescriptor<T, ?> field : listFields) {
            listView.addWidget(field.getHeaderCellRenderer().render(), colCount, 0);
            this.colCount++;
        }
        final PSimplePanel widget = new PSimplePanel();
        listView.addWidget(widget, colCount, 0);
        listView.addCellStyle(0, colCount, PonySDKTheme.FILL_COLUMN);
        listView.addHeaderStyle("pony-ComplexList-ColumnHeader");
    }

    public void insertData(final int row, final T data) {
        int col = 0;

        for (final DataGridColumnDescriptor<T, ?> field : listFields) {
            view.addWidget(field.renderCell(row, data), col++, row);
        }
        view.addWidget(new PSimplePanel(), col, row);
        view.addRowStyle(row, PonySDKTheme.SIMPLELIST_ROW);
    }

    public void insertSubList(final int row, final java.util.List<T> datas) {
        if (datas.isEmpty()) return;
        int subRow = row + 1;
        for (final T data : datas) {
            view.insertRow(subRow); // create a new row after
            view.addRowStyle(subRow, PonySDKTheme.SIMPLELIST_SUBROW);
            int col = 0;
            for (final DataGridColumnDescriptor<T, ?> field : listFields) {
                view.addWidget(field.renderSubCell(subRow, data), col++, subRow);
            }
            view.addWidget(new PSimplePanel(), col, subRow++);
        }
        updateSubListOnRowInserted(row, datas.size());
        // eventBus.fireEvent(new RowInsertedEvent(this, row, datas.size()));
    }

    public void removeSubList(final int fatherRow) {
        final Integer subListSize = subListSizeByFather.remove(fatherRow);
        if (subListSize != null) {
            for (int i = 1; i <= subListSize; i++) {
                view.removeRow(fatherRow + 1);
            }
            // eventBus.fireEvent(new RowDeletedEvent(this, fatherRow, subListSize));
            updateSubListOnRowDeleted(fatherRow, subListSize);
        }
    }

    private void updateSubListOnRowInserted(final int row, final int insertedRowCount) {
        final Map<Integer, Integer> temp = new HashMap<Integer, Integer>();
        for (final Map.Entry<Integer, Integer> entry : subListSizeByFather.entrySet()) {
            final int size = entry.getValue();
            int subRow = entry.getKey();
            if (subRow > row) {
                subRow += insertedRowCount;
            }
            temp.put(subRow, size);
        }
        subListSizeByFather = temp;
        subListSizeByFather.put(row, insertedRowCount);
    }

    private void updateSubListOnRowDeleted(final int row, final int deletedRowCount) {
        final Map<Integer, Integer> temp = new HashMap<Integer, Integer>();
        for (final Map.Entry<Integer, Integer> entry : subListSizeByFather.entrySet()) {
            final int size = entry.getValue();
            int subRow = entry.getKey();
            if (subRow > row + deletedRowCount) {
                subRow -= deletedRowCount;
            }
            temp.put(subRow, size);
        }
        subListSizeByFather = temp;
    }

    public void clear() {
        view.clearList();
    }

    public void selectRow(final int row) {
        view.selectRow(row);
    }

    public void unSelectRow(final int row) {
        view.unSelectRow(row);
    }

    @Override
    public void start(final PAcceptsOneWidget container) {
        container.setWidget(view);
    }

    @Override
    public PSelectionModel<T> getSelectionModel() {
        return setSelectionModel;
    }

    @Override
    public void setSelectionModel(final PSelectionModel<T> selectionModel) {
        this.setSelectionModel = selectionModel;
    }

    @Override
    public T getVisibleItem(final int indexOnPage) {
        return null;
    }

    @Override
    public int getVisibleItemCount() {
        return 0;
    }

    @Override
    public Iterable<T> getVisibleItems() {
        return null;
    }

    @Override
    public void setRowData(final int start, final List<T> data) {
        // int rowIndex = 1;
        // for (final T t : data) {
        // int col = 0;
        // for (final DataGridColumnDescriptor<T, ?> field : listFields) {
        // view.addWidget(field.renderCell(rowIndex, t), col++, rowIndex);
        // }
        // view.addWidget(new PSimplePanel(), col, rowIndex);// Why ?
        // view.addRowStyle(rowIndex, PonySDKTheme.SIMPLELIST_ROW);
        // rowIndex++;
        // }
        int rowIndex = 1;
        for (final T t : data) {
            insertData(rowIndex++, t);
        }
    }

}
