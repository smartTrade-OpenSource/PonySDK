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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.list.SimpleListView;

public class DataGridActivity<D> implements HasPData<D>, IsPWidget {

    private final SimpleListView view;
    private PSelectionModel<D> setSelectionModel;
    private final List<DataGridColumnDescriptor<D, ?>> columnDescriptors = new ArrayList<DataGridColumnDescriptor<D, ?>>();
    private Map<Integer, Integer> subListSizeByFather = new HashMap<Integer, Integer>();

    private int colCount = 0;
    private final List<D> rows = new ArrayList<D>();

    public DataGridActivity(final SimpleListView listView) {
        this.view = listView;
        this.view.asWidget().addStyleName(PonySDKTheme.COMPLEXLIST);

    }

    public void addDataGridColumnDescriptor(final DataGridColumnDescriptor<D, ?> columnDescriptor) {
        columnDescriptors.add(columnDescriptor);

        // this.view.setColumns(columnDescriptors.size());

        view.addWidget(columnDescriptor.getHeaderCellRenderer().render(), colCount++, 0);

        addFillColumn();
    }

    protected void addFillColumn() {
        final PSimplePanel widget = new PSimplePanel();
        view.addWidget(widget, colCount, 0);
        view.addCellStyle(0, colCount, PonySDKTheme.FILL_COLUMN);
        view.addHeaderStyle(PonySDKTheme.COMPLEXLIST_COLUMNHEADER_COMPLEX);
    }

    public void setData(final int row, final D data) {
        int col = 0;

        for (final DataGridColumnDescriptor<D, ?> field : columnDescriptors) {
            view.addWidget(field.renderCell(row, data), col++, row);
        }
        view.addWidget(new PSimplePanel(), col, row);
        view.addRowStyle(row, PonySDKTheme.SIMPLELIST_ROW);
    }

    public void insertSubList(final int row, final List<D> datas) {
        if (datas.isEmpty()) return;
        int subRow = row + 1;
        for (final D data : datas) {
            view.insertRow(subRow); // create a new row after
            view.addRowStyle(subRow, PonySDKTheme.SIMPLELIST_SUBROW);
            int col = 0;
            for (final DataGridColumnDescriptor<D, ?> field : columnDescriptors) {
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
    public PSelectionModel<D> getSelectionModel() {
        return setSelectionModel;
    }

    @Override
    public void setSelectionModel(final PSelectionModel<D> selectionModel) {
        this.setSelectionModel = selectionModel;
    }

    @Override
    public D getVisibleItem(final int indexOnPage) {
        return rows.get(indexOnPage);
    }

    @Override
    public int getVisibleItemCount() {
        return rows.size();
    }

    @Override
    public Iterable<D> getVisibleItems() {
        return rows;
    }

    @Override
    public void setData(final List<D> data) {
        rows.clear();
        rows.addAll(data);

        view.clear(1);

        int rowIndex = 1;
        for (final D t : data) {
            setData(rowIndex++, t);
        }
    }

    public void addData(final D data) {
        insertData(getVisibleItemCount() + 1, data);
    }

    public void insertData(final int index, final D data) {
        rows.add(index, data);

        view.insertRow(index + 1);
        setData(index + 1, data);
    }

    public void remove(final int index) {
        view.removeRow(index + 1);
        rows.remove(index);
    }

    public void remove(final D data) {
        remove(rows.indexOf(data));
    }

    @Override
    public PWidget asWidget() {
        return view.asWidget();
    }

}
