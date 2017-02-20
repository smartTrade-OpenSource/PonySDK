package com.ponysdk.core.ui.datagrid;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PWidget;

import java.util.*;
import java.util.function.Function;

public class DataGrid<DataType extends Comparable<DataType>> implements IsPWidget {

    private final View view;
    private final List<ColumnDescriptor<DataType>> columns = new ArrayList<>();
    private final TreeSet<Decorator> rows = new TreeSet<>(new DefaultComparator());

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
        if (columns.add(column)) {
            drawHeader(columns.size() - 1, column);
        }
    }

    @Override
    public PWidget asWidget() {
        return view.asWidget();
    }

    public void setData(final DataType data) {
        final Decorator d = new Decorator(data);
        if (rows.add(d)) {
            draw(d);
        } else {
            update(d);
        }
    }

    private void drawHeader(int c, final ColumnDescriptor<DataType> column) {
        final IsPWidget w = column.getHeaderCellRenderer().render();
        view.setHeader(c, w);
    }

    private void update(final Decorator d) {
        final int r = rows.headSet(d).size();
        int c = 0;
        for (final ColumnDescriptor<DataType> column : columns) {
            drawCell(r, c++, column, d.data);
        }
    }

    private void draw(final Decorator from) {
        if (from == null) return;
        final int index = rows.headSet(from).size();
        final SortedSet<Decorator> tail = rows.tailSet(from);
        int r = index;
        int c = 0;

        for (final ColumnDescriptor<DataType> column : columns) {
            for (final Decorator w : tail) {
                drawCell(r++, c, column, w.data);
            }
            c++;
            r = index;
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
            int r = 0;

            int size = columns.size() - 1;

            columns.remove(c);

            for (int i = c; i < columns.size(); i++) {
                final ColumnDescriptor<DataType> currentColumn = columns.get(i);
                drawHeader(i, currentColumn);
                for (final Decorator d : rows) {
                    drawCell(r++, c, currentColumn, d.data);
                }
                r = 0;
            }

            resetColumn(size, column);
        }
    }

    public void removeData(final DataType data) {
        final Decorator w = new Decorator(data);
        final Decorator higher = rows.higher(w);
        if (rows.remove(w)) {
            draw(higher);
            resetRow(rows.size());
        }
    }

    private void resetColumn(final Integer c, final ColumnDescriptor<DataType> column) {
        PWidget header = view.getHeader(c);
        if (header != null) header.removeFromParent();

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

    private class Decorator {

        private Object key;
        private DataType data;

        private Decorator(final DataType data) {
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

    private class DefaultComparator implements Comparator<Decorator> {

        @Override
        public int compare(final Decorator d1, final Decorator d2) {
            return d1.data.compareTo(d2.data);
        }

    }

}
