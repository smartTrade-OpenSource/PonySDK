
package com.ponysdk.core.ui.datagrid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.datagrid.impl.DefaultView;

public class DataGrid<T> implements IsPWidget {

    private final View view;
    private final List<ColumnDescriptor<T>> columns = new ArrayList<>();

    private DataGridTreeSet<T> rows;

    public DataGrid() {
        this(new DefaultView(), Function.identity());
    }

    public DataGrid(final Comparator<T> comparator) {
        this(new DefaultView(), Function.identity(), comparator);
    }

    public DataGrid(final Function<T, ?> keyProvider) {
        this(new DefaultView(), keyProvider);
    }

    public DataGrid(final View view, final Function<T, ?> keyProvider) {
        this(view, keyProvider, (Comparator<T>) Comparator.naturalOrder());
    }

    public DataGrid(final View view, final Function<T, ?> keyProvider, final Comparator<T> comparator) {
        this.view = view;
        this.rows = new DataGridTreeSet<>(comparator, keyProvider);
    }

    @Override
    public PWidget asWidget() {
        return view.asWidget();
    }

    public void addColumnDescriptor(final ColumnDescriptor<T> column) {
        if (columns.add(column)) {
            int r = 0;
            final int c = columns.size() - 1;

            drawHeader(c, column);

            for (final T w : rows) {
                drawCell(r++, c, column, w);
            }
        }
    }

    public Collection<T> getData() {
        return Collections.unmodifiableSet(rows);
    }

    public void addData(final Collection<T> data) {
        data.forEach(this::addData);
    }

    public Consumer<T> getConsumer() {
        return data -> this.addData(data);
    }

    public void addData(final T data) {
        if (rows.containsData(data)) {
            final int indexBefore = rows.getPosition(data);
            if (indexBefore != -1) {
                rows.remove(data);
                rows.add(data);
                final int indexAfter = rows.headSet(data).size();
                if (indexBefore == indexAfter) {
                    update(indexAfter, data);
                } else {
                    draw(Math.min(indexBefore, indexAfter), data);
                }
            } else {
                //Update
            }
        } else {
            rows.add(data);
            draw(rows.headSet(data).size(), data);
        }
    }

    public void removeColumn(final ColumnDescriptor<T> column) {
        if (columns.isEmpty()) return;

        final int c = columns.indexOf(column);

        if (c != -1) {
            int r = 0;

            final int size = columns.size() - 1;

            columns.remove(c);

            for (int i = c; i < columns.size(); i++) {
                final ColumnDescriptor<T> currentColumn = columns.get(i);
                drawHeader(i, currentColumn);
                for (final T data : rows) {
                    drawCell(r++, i, currentColumn, data);
                }
                r = 0;
            }

            resetColumn(size, column);
        }
    }

    public void removeData(final T data) {
        final T higher = rows.higher(data);
        final int index = rows.headSet(data).size();
        if (rows.remove(data)) {
            draw(index, higher);
            resetRow(rows.size());
        }
    }

    public List<ColumnDescriptor<T>> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    private void drawHeader(final int c, final ColumnDescriptor<T> column) {
        view.setHeader(c, column.getHeaderRenderer().render());
    }

    public void update(final T data, final Function<T, T> merge) {
        final int indexBefore = rows.getPosition(data);
        if (indexBefore != -1) {
            rows.remove(data);
            rows.add(merge.apply(data));
            final int indexAfter = rows.getPosition(data);
            if (indexBefore == indexAfter) {
                update(indexAfter, data);
            } else {
                draw(Math.min(indexBefore, indexAfter), data);
            }
        } else {
            rows.add(data);
            draw(rows.headSet(data).size(), data);
        }

    }

    private void update(final int r, final T data) {
        int c = 0;
        for (final ColumnDescriptor<T> column : columns) {
            drawCell(r, c++, column, data);
        }
    }

    private void draw(final int fromRow, final T from) {
        if (from == null) return;
        final SortedSet<T> tail = rows.tailSet(from);
        int r = fromRow;
        int c = 0;

        for (final T w : tail) {
            for (final ColumnDescriptor<T> column : columns) {
                drawCell(r, c++, column, w);
            }
            c = 0;
            r++;
        }
    }

    private void drawCell(final int r, final int c, final ColumnDescriptor<T> column, final T data) {
        PWidget w = view.getCell(r, c);

        if (w == null) {
            w = column.getCellRenderer().render(data);
            view.setCell(r, c, w);
        } else {
            column.getCellRenderer().update(data, w);
        }
    }

    private void resetColumn(final Integer c, final ColumnDescriptor<T> column) {
        final PWidget header = view.getHeader(c);
        if (header != null) header.removeFromParent();

        for (int r = 0; r < view.getRowCount(); r++) {
            column.getCellRenderer().reset(view.getCell(r, c));
        }
    }

    private void resetRow(final Integer r) {
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
