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

package com.ponysdk.ui.server.list;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.core.activity.AbstractActivity;
import com.ponysdk.core.event.PEventBus;
import com.ponysdk.core.event.PEventBusAware;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.list.event.RowDeletedEvent;
import com.ponysdk.ui.server.list.event.RowInsertedEvent;

public class SimpleListActivity<T> extends AbstractActivity {

    protected SimpleListView listView;

    private List<ListColumnDescriptor<T, ?>> listFields;

    private final String ID;

    private String debugID;

    private List<T> data;

    private Map<Integer, Integer> subListSizeByFather = new HashMap<Integer, Integer>();

    private final PEventBus eventBus;

    private int colCount;

    public SimpleListActivity(final SimpleListView listView, final List<ListColumnDescriptor<T, ?>> listFields, final PEventBus eventBus) {
        this.ID = null;
        this.listFields = listFields;
        this.listView = listView;
        this.eventBus = eventBus;
        buildHeaders();
    }

    public SimpleListActivity(final String ID, final SimpleListView listView, final List<ListColumnDescriptor<T, ?>> listFields, final PEventBus eventBus) {
        this.ID = ID;
        this.listFields = listFields;
        this.listView = listView;
        this.eventBus = eventBus;
        buildHeaders();
    }

    public String getID() {
        return ID;
    }

    public void addDescriptor(final ListColumnDescriptor<T, ?> customDescriptor) {
        listFields.add(customDescriptor);
        listView.removeCellStyle(0, colCount, PonySDKTheme.FILL_COLUMN);
        listView.setColumns(colCount);
        listView.addWidget(customDescriptor.renderHeader(), colCount, 0);
        final PSimplePanel widget = new PSimplePanel();
        listView.addCellStyle(0, colCount + 1, PonySDKTheme.FILL_COLUMN);

        int rowIndex = 1;
        if (data != null) {
            for (final T t : data) {
                final IsPWidget renderCell = customDescriptor.renderCell(rowIndex, t);
                listView.addWidget(renderCell, colCount, rowIndex);
                listView.removeCellStyle(rowIndex, colCount - 1, PonySDKTheme.FILL_COLUMN);
                listView.addCellStyle(rowIndex, colCount, PonySDKTheme.FILL_COLUMN);
                rowIndex++;
            }
        }
        if (customDescriptor.getHeaderCellRenderer() instanceof PEventBusAware) {
            ((PEventBusAware) customDescriptor.getHeaderCellRenderer()).setEventBus(eventBus);
        }
        colCount++;
        listView.addWidget(widget, colCount, 0);
    }

    private void buildHeaders() {
        colCount = 0;

        listView.setColumns(listFields.size());
        // listView.insertRow(0);

        for (final ListColumnDescriptor<T, ?> field : listFields) {
            if (field.isViewable()) {
                listView.addWidget(field.renderHeader(), colCount, 0);
                if (field.getWidth() != null) {
                    listView.setColumnWidth(colCount, field.getWidth());
                }
                colCount++;
            }
        }
        final PSimplePanel widget = new PSimplePanel();
        listView.addWidget(widget, colCount, 0);
        listView.addCellStyle(0, colCount, PonySDKTheme.FILL_COLUMN);
        listView.addHeaderStyle("pony-ComplexList-ColumnHeader");
    }

    public void rebuild(final List<ListColumnDescriptor<T, ?>> listFields, final List<T> data) {
        reset();
        this.listView.removeRow(0);
        this.listFields = listFields;
        buildHeaders();
        if (data != null) setData(data);
    }

    private void reset() {
        subListSizeByFather.clear();
        listView.clearList();
        data = null;
    }

    public void insertData(final int row, final T data) {
        int col = 0;

        // listView.insertRow(rowCount);
        for (final ListColumnDescriptor<T, ?> field : listFields) {
            if (!field.isViewable()) continue;
            final IsPWidget renderCell = field.renderCell(row, data);

            if (debugID != null) {
                String headerCaption;
                if (field.getHeaderCellRenderer().getCaption() != null) {
                    headerCaption = field.getHeaderCellRenderer().getCaption();
                } else {
                    headerCaption = String.valueOf(col);
                }
                renderCell.asWidget().ensureDebugId(debugID + "[" + row + "][" + headerCaption + "]");
            }
            listView.addWidget(renderCell, col++, row);
        }
        listView.addWidget(new PSimplePanel(), col, row);
        listView.addRowStyle(row, PonySDKTheme.SIMPLELIST_ROW);
    }

    public void setData(final List<T> data) {
        assert listView != null : "Cannot remove field before binding listView";
        reset();
        this.data = data;
        int rowIndex = 1;
        for (final T t : data) {
            int col = 0;
            // listView.insertRow(rowCount);
            for (final ListColumnDescriptor<T, ?> field : listFields) {
                if (!field.isViewable()) continue;
                final IsPWidget renderCell = field.renderCell(rowIndex, t);

                if (debugID != null) {
                    String headerCaption;
                    if (field.getHeaderCellRenderer().getCaption() != null) {
                        headerCaption = field.getHeaderCellRenderer().getCaption();
                    } else {
                        headerCaption = String.valueOf(col);
                    }
                    renderCell.asWidget().ensureDebugId(debugID + "[" + rowIndex + "][" + headerCaption + "]");
                }
                listView.addWidget(renderCell, col++, rowIndex);
            }
            listView.addWidget(new PSimplePanel(), col, rowIndex);
            listView.addRowStyle(rowIndex, PonySDKTheme.SIMPLELIST_ROW);
            rowIndex++;
        }
    }

    public void insertSubList(final int row, final java.util.List<T> datas) {
        if (datas.isEmpty()) return;
        int subRow = row + 1;
        for (final T data : datas) {
            listView.insertRow(subRow); // create a new row after
            listView.addRowStyle(subRow, PonySDKTheme.SIMPLELIST_SUBROW);
            int col = 0;
            for (final ListColumnDescriptor<T, ?> field : listFields) {
                if (!field.isViewable()) continue;
                listView.addWidget(field.renderSubCell(subRow, data), col++, subRow);
            }
            listView.addWidget(new PSimplePanel(), col, subRow++);
        }
        updateSubListOnRowInserted(row, datas.size());
        eventBus.fireEvent(new RowInsertedEvent(this, row, datas.size()));
    }

    public void removeSubList(final int fatherRow) {
        final Integer subListSize = subListSizeByFather.remove(fatherRow);
        if (subListSize != null) {
            for (int i = 1; i <= subListSize; i++) {
                listView.removeRow(fatherRow + 1);
            }
            eventBus.fireEvent(new RowDeletedEvent(this, fatherRow, subListSize));
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

    public void selectRow(final int row) {
        listView.selectRow(row);
    }

    public void unSelectRow(final int row) {
        listView.unSelectRow(row);
    }

    public List<T> getData() {
        return data;
    }

    @Override
    public void start(final PAcceptsOneWidget container) {
        container.setWidget(listView);
    }

    public void ensureDebugId(final String debugID) {
        this.debugID = debugID;
    }
}
