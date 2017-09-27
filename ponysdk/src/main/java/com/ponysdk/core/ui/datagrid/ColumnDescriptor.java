
package com.ponysdk.core.ui.datagrid;

import java.util.function.Function;

import com.ponysdk.core.ui.datagrid.impl.PLabelCellRenderer;
import com.ponysdk.core.ui.datagrid.impl.PLabelHeaderRenderer;

public class ColumnDescriptor<D> {

    private HeaderRenderer headerRenderer;
    private CellRenderer<D> cellRenderer;

    HeaderRenderer getHeaderRenderer() {
        return headerRenderer;
    }

    public void setHeaderRenderer(final HeaderRenderer headerCellRender) {
        this.headerRenderer = headerCellRender;
    }

    CellRenderer<D> getCellRenderer() {
        return cellRenderer;
    }

    public void setCellRenderer(final CellRenderer<D> cellRenderer) {
        this.cellRenderer = cellRenderer;
    }

    public static <D> ColumnDescriptor<D> newDefault(final String caption, final Function<D, String> transform) {
        final ColumnDescriptor<D> descriptor = new ColumnDescriptor<>();
        descriptor.setHeaderRenderer(new PLabelHeaderRenderer(caption));
        descriptor.setCellRenderer(new PLabelCellRenderer<>(transform));
        return descriptor;
    }

    public static <D, V> ColumnDescriptor<D> newDefault(final String caption, final Function<D, V> transform1,
                                                        final Function<V, String> transform2) {
        final ColumnDescriptor<D> descriptor = new ColumnDescriptor<>();
        descriptor.setHeaderRenderer(new PLabelHeaderRenderer(caption));

        final Function<D, String> chainFunction = t -> {
            final V tf1 = transform1.apply(t);
            if (tf1 == null) return null;
            return transform2.apply(tf1);
        };

        descriptor.setCellRenderer(new PLabelCellRenderer<>(chainFunction));
        return descriptor;
    }

    public static <D, V, W> ColumnDescriptor<D> newDefault(final String caption, final Function<D, V> transform1,
                                                           final Function<V, W> transform2, final Function<W, String> transform3) {
        final ColumnDescriptor<D> descriptor = new ColumnDescriptor<>();
        descriptor.setHeaderRenderer(new PLabelHeaderRenderer(caption));

        final Function<D, String> chainFunction = t -> {
            final V tf1 = transform1.apply(t);
            if (tf1 == null) return null;
            final W tf2 = transform2.apply(tf1);
            if (tf2 == null) return null;
            return transform3.apply(tf2);
        };

        descriptor.setCellRenderer(new PLabelCellRenderer<>(chainFunction));
        return descriptor;
    }
}
