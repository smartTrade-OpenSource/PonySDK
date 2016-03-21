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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PWidget;

/***
 * A Grid of data that supports paging and columns.
 * 
 * @see ColumnDescriptor
 */
public class Grid<K, D> implements HasPData<D>, IsPWidget {

    protected final List<ColumnDescriptor<D, ?>> columnDescriptors = new ArrayList<>();

    protected final Map<K, Map<ColumnDescriptor<D, ?>, Cell<D, ?>>> cells = new HashMap<>();

    protected final List<K> keyByIndex = new ArrayList<>();
    protected final Map<K, D> valueByKey = new HashMap<>();
    protected final SimpleListView view;

    private PSelectionModel<D> setSelectionModel;

    private int colCount = 0;
    protected final List<D> rows = new ArrayList<>();
    protected int dataCount = 0;

    public Grid(final SimpleListView listView) {
        this.view = listView;
    }

    public void addColumnDescriptor(final ColumnDescriptor<D, ?> columnDescriptor) {
        columnDescriptors.add(columnDescriptor);
        view.addWidget(columnDescriptor.getHeaderCellRenderer().render(), colCount++, 0, 1);
    }

    public void setData(final int row, final D data) {
        int col = 0;

        for (final ColumnDescriptor<D, ?> field : columnDescriptors) {
            final IsPWidget renderCell = field.renderCell(data);
            view.addWidget(renderCell, col++, row + 1, 1);
        }
        view.addWidget(new PSimplePanel(), col, row + 1, 1);
    }

    public void insertRow(final int row, final int column, final int colSpan, final PWidget widget) {
        keyByIndex.add(row, null);

        if (row > getRowCount() + 1) throw new IndexOutOfBoundsException("row (" + row + ") > size (" + getRowCount() + ")");

        rows.add(row, null);

        view.insertRow((row + 1));
        view.addWidget(widget, column, (row + 1), colSpan);
        updateRowIndex(row);
    }

    public void insertWidget(final int row, final int column, final int colSpan, final PWidget widget) {
        view.addWidget(widget, column, row, colSpan);
    }

    public void selectRow(final D data) {
        view.selectRow(getDataIndex(data));
    }

    public void unSelectRow(final D data) {
        view.unSelectRow(getDataIndex(data));
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

    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getVisibleItemCount() {
        return dataCount;
    }

    @Override
    public Iterable<D> getVisibleItems() {

        return new Iterable<D>() {

            private D next;
            private final Iterator<D> rowsIterator = rows.iterator();

            private final Iterator<D> visibleItemIterator = new Iterator<D>() {

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
                    return next;
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

    public int getDataIndex(final D data) {
        return rows.indexOf(data);
    }

    @Override
    public void setData(final List<D> data) {
        rows.clear();

        rows.addAll(data);
        dataCount = data.size();

        view.clear(1);

        int rowIndex = 0;
        for (final D t : data) {
            setData(rowIndex++, t);
        }
    }

    public void addData(final D data) {
        insertData(getVisibleItemCount(), data);
    }

    public void insertData(final int index, final D data) {
        rows.add(index, data);
        dataCount++;

        view.insertRow(index + 1);
        setData(index, data);
    }

    public void remove(final int index) {
        view.removeRow(index + 1);

        final D removed = rows.remove(index);
        if (removed != null) dataCount--;

        final K k = keyByIndex.remove(index);
        if (k != null) {
            valueByKey.remove(k);
            cells.remove(k);
        }

        updateRowIndex(index);
    }

    public void remove(final D data) {
        remove(rows.indexOf(data));
    }

    @Override
    public PWidget asWidget() {
        return view.asWidget();
    }

    public List<ColumnDescriptor<D, ?>> getColumnDescriptors() {
        return columnDescriptors;
    }

    public SimpleListView getListView() {
        return view;
    }

    public void setData(final K key, final D data) {
        Map<ColumnDescriptor<D, ?>, Cell<D, ?>> map = cells.get(key);
        if (map == null) {
            final int row = getRowCount();
            map = new HashMap<>();
            cells.put(key, map);
            keyByIndex.add(key);

            rows.add(data);
            dataCount++;

            valueByKey.put(key, data);

            int col = 0;

            for (final ColumnDescriptor<D, ?> descriptor : columnDescriptors) {
                final Cell<D, ?> cell = new Cell<D, ?>();
                cell.setCol(col++);
                cell.setData(data);
                cell.setRow(row);
                cell.setW(descriptor.getCellRenderer().render(cell.getData()));
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

            for (final ColumnDescriptor<D, ?> column : columnDescriptors) {
                final Cell<D, ?> cell = map.get(column);
                column.getCellRenderer().update(data, cell);
                map.get(column).setData(data);
            }
        }
    }

    public void removeByKey(final K key) {
        final D removed = valueByKey.get(key);
        if (removed != null) {
            remove(removed);
        }
    }

    protected void updateRowIndex(final int min) {
        for (int i = min; i < rows.size(); i++) {
            final K k = keyByIndex.get(i);
            if (k != null) {
                final Map<ColumnDescriptor<D, ?>, Cell<D, ?>> cellRow = cells.get(k);
                final Iterator<Entry<ColumnDescriptor<D, ?>, Cell<D, ?>>> iter = cellRow.entrySet().iterator();
                final int realRow = i;
                while (iter.hasNext()) {
                    final Entry<ColumnDescriptor<D, ?>, Cell<D, ?>> entry = iter.next();
                    entry.getValue().setRow(realRow);
                }
            }
        }
    }

    public void moveRow(final K key, final int beforeIndex) {
        final Map<ColumnDescriptor<D, ?>, Cell<D, ?>> map = cells.get(key);
        if (map == null) throw new IndexOutOfBoundsException("cell not found");

        final Cell<D, ?> cell = map.entrySet().iterator().next().getValue();
        final int realRow = cell.getRow();

        if (realRow == beforeIndex) return;

        view.moveRow((realRow + 1), (beforeIndex + 1));

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

    public int getRow(final K key) {
        final Map<ColumnDescriptor<D, ?>, Cell<D, ?>> map = cells.get(key);
        if (map == null) return -1;
        return map.entrySet().iterator().next().getValue().getRow();
    }

    @SuppressWarnings("unchecked")
    public <W extends IsPWidget> Collection<Cell<D, W>> getColumn(final ColumnDescriptor<D, W> descriptor) {
        final List<Cell<D, W>> c = new ArrayList<>();
        final Collection<Map<ColumnDescriptor<D, ?>, Cell<D, ?>>> values = cells.values();
        for (final Map<ColumnDescriptor<D, ?>, Cell<D, ?>> map : values) {
            final Cell<D, W> cell = (Cell<D, W>) map.get(descriptor);
            if (cell != null) c.add(cell);
        }
        return c;
    }

    public D getData(final K key) {
        final Map<ColumnDescriptor<D, ?>, Cell<D, ?>> map = cells.get(key);
        if (map == null) return null;
        return map.entrySet().iterator().next().getValue().getData();
    }

    public void clear() {
        view.clear(1);
        cells.clear();
        keyByIndex.clear();
        valueByKey.clear();
    }

}
