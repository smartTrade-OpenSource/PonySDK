/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

import com.ponysdk.core.ui.datagrid.impl.PLabelCellRenderer;
import com.ponysdk.core.ui.datagrid.impl.PLabelHeaderRenderer;

import java.util.function.Function;

public class ColumnDescriptor<D> {

    private HeaderRenderer headerRenderer;
    private CellRenderer<D> cellRenderer;

    public HeaderRenderer getHeaderRenderer() {
        return headerRenderer;
    }

    public void setHeaderRenderer(final HeaderRenderer headerCellRender) {
        this.headerRenderer = headerCellRender;
    }

    public CellRenderer<D> getCellRenderer() {
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
