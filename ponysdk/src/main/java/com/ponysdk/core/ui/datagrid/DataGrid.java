package com.ponysdk.core.ui.datagrid;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.datagrid.impl.DefaultView;

import java.util.*;
import java.util.function.Function;

public class DataGrid<T> implements IsPWidget {

    private final View view;
    private final List<ColumnDescriptor<T>> columns = new ArrayList<>();
    private DataGridTreeSet<T> rows;

    public DataGrid() {
        this(new DefaultView());
    }

    public DataGrid(final Comparator<T> comparator) {
        this(new DefaultView(), comparator);
    }

    public DataGrid(final View view) {
        this(view, (Comparator<T>) Comparator.naturalOrder());
    }

    public DataGrid(final View view, final Comparator<T> comparator) {
        this.view = view;
        this.rows = new DataGridTreeSet<>(comparator);
    }

    @Override
    public PWidget asWidget() {
        return view.asWidget();
    }

    public void addColumnDescriptor(final ColumnDescriptor<T> column) {
        if (columns.add(column)) {
            view.addHeader().add(column.getHeaderRenderer().render());

            int r = 0;
            for (T e : rows.asSet()) {
                view.addCell(r++).add(column.getCellRenderer().render(e));
            }
        }
    }

    public void removeColumn(final ColumnDescriptor<T> column) {
        /**if (columns.isEmpty()) return;

         final int c = columns.indexOf(column);

         if (c != -1) {
         int r = 0;

         final int size = columns.size() - 1;

         columns.remove(c);

         for (int i = c; i < columns.size(); i++) {
         final ColumnDescriptor<T> currentColumn = columns.get(i);
         drawHeader(i, currentColumn);
         for (final T data : rows.asSet()) {
         drawCell(r++, i, currentColumn, data);
         }
         r = 0;
         }

         resetColumn(size, column);
         }**/
    }

    public void put(final T data, final Function<T, T> merge) {
        if (rows.contains(data)) {
            rows.put(merge.apply(data), this::drawCell);
        } else {

        }
    }

    public void remove(final T data) {
        if (rows.remove(data, this::drawCell)) {
            resetRow(rows.size());
        }
    }

    private void drawCell(Integer i, T d) {
        int c = 0;
        for (final ColumnDescriptor<T> column : columns) {
            drawCell(i, c++, column, d);
        }
    }

    public List<ColumnDescriptor<T>> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    private void resetColumn(final int c, final ColumnDescriptor<T> column) {
        final PWidget header = view.getHeader(c);
        if (header != null) header.removeFromParent();

        for (int r = 0; r < view.getRowCount(); r++) {
            column.getCellRenderer().reset(view.getCell(r, c));
        }
    }

    private void resetRow(final int r) {
        int c = 0;
        for (final ColumnDescriptor<T> column : columns) {
            column.getCellRenderer().reset(view.getCell(r, c++));
        }
    }

    public void clear() {
        rows.clear();
        final int rowCount = view.getRowCount();
        for (int i = rowCount - 1; i >= 0; i--) {
            resetRow(i);
        }
    }

}
