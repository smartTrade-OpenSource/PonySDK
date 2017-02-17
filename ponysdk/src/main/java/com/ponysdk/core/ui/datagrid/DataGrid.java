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
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PWidget;

public class DataGrid<DataType extends Comparable<DataType>> implements IsPWidget {

    private final View view;
    private final List<ColumnDescriptor<DataType>> columns = new ArrayList<>();
    private final TreeSet<W> rows = new TreeSet<>(new DefaultComparator());

    private Function<DataType, ? extends Object> keyProvider;

    public DataGrid() {
        this(new DefaultView(), Function.identity());
    }

    public DataGrid(final Function<DataType, Object> keyProvider) {
        this(new DefaultView(), keyProvider);
    }

    public DataGrid(final View view, final Function<DataType, ? extends Object> keyProvider) {
        this.view = view;
        this.keyProvider = keyProvider;
    }

    public void addColumnDescriptor(final ColumnDescriptor<DataType> column) {
        if (!columns.add(column)) {
            drawHeader(column);
        }
    }

    @Override
    public PWidget asWidget() {
        return view.asWidget();
    }

    public void setData(final DataType data) {
        final W w = new W(data);
        if (rows.add(w)) {
            draw(w);
        }
    }

    private void drawHeader(final ColumnDescriptor<DataType> column) {
        final IsPWidget w = column.getHeaderCellRenderer().render();
        view.setHeader(columns.size() - 1, w);
    }

    private void draw(final W from) {
        if (from == null) return;
        int r = rows.headSet(from).size() + 1;
        int c = 0;

        for (final ColumnDescriptor<DataType> column : columns) {
            for (final W w : rows.tailSet(from, false)) {
                drawCell(r++, c, column, w.data);
            }
            c++;
        }
    }

    private void drawCell(final int r, final int c, final ColumnDescriptor<DataType> column, final DataType data) {
        final PWidget w = view.getCell(r, c);
        if (w == null) {
            view.setCell(r, c, column.getCellRenderer().render(data));
        } else {
            column.getCellRenderer().update(data, w);
        }
    }

    public void removeColumn(final ColumnDescriptor<DataType> column) {
        if (columns.isEmpty()) return;

        final int c = columns.indexOf(column);

        if (c != -1) {
            final ColumnDescriptor<DataType> last = columns.get(columns.size());
            int r = 0;
            columns.remove(c);

            for (int i = c; i < columns.size(); i++) {
                for (final W w : rows) {
                    drawCell(r, c, columns.get(i), w.data);
                }
                r = 0;
            }

            resetColumn(columns.size(), last);
        }

    }

    public void removeData(final DataType data) {
        final W w = new W(data);
        final W higher = rows.higher(w);
        if (rows.remove(w)) {
            draw(higher);
            resetRow(rows.size());
        }
    }

    private void resetColumn(final Integer c, final ColumnDescriptor<DataType> column) {
        for (int r = 0; r < view.getRowCount(); r++) {
            column.getCellRenderer().reset(view.getCell(r, c));
        }
    }

    private void resetRow(final Integer r) {
        int c = 0;
        for (final ColumnDescriptor<DataType> column : columns) {
            column.getCellRenderer().reset(view.getCell(r, c++));
        }
    }

    private class W {

        private Object key;
        private DataType data;

        private W(final DataType data) {
            setData(data);
        }

        private void setData(final DataType data) {
            this.data = data;
            this.key = keyProvider.apply(data);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return key.equals(obj);
        }

    }

    private class DefaultComparator implements Comparator<W> {

        @Override
        public int compare(final W o1, final W o2) {
            return o1.data.compareTo(o2.data);
        }

    }

    public interface View extends IsPWidget {

        void setHeader(int c, IsPWidget w);

        void setCell(int r, int c, IsPWidget w);

        int getRowCount();

        PWidget getCell(int r, int c);

    }

    public static class DefaultView implements View {

        private final PFlexTable table = new PFlexTable();

        @Override
        public PWidget asWidget() {
            return table;
        }

        @Override
        public void setHeader(final int c, final IsPWidget w) {
            table.setWidget(0, c, w);
        }

        @Override
        public PWidget getCell(final int r, final int c) {
            return table.getWidget(r + 1, c);
        }

        @Override
        public int getRowCount() {
            return table.getRowCount() - 1;
        }

        @Override
        public void setCell(final int r, final int c, final IsPWidget w) {
            table.setWidget(r, c, w);
        }

    }

    //    public void moveColumn(final int from, final int to) {
    //        if (from != to) {
    //            final ColumnDescriptor<DataType> object = descriptorsByColumn.remove(from);
    //            //            descriptorsByColumn.add(to, object);
    //            //            table.moveColumn(from, to);
    //        }
    //    }

}
