
package com.ponysdk.core.ui.datagrid.impl;

import java.util.function.Function;

import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.datagrid.TypedCellRenderer;

public class PLabelCellRenderer<DataType> extends TypedCellRenderer<DataType, PLabel> {

    private Function<DataType, String> transform = from -> String.valueOf(from);

    public PLabelCellRenderer() {
    }

    public PLabelCellRenderer(final Function<DataType, String> transform) {
        this.transform = transform;
    }

    @Override
    public PLabel render(final DataType value) {
        return new PLabel(transform.apply(value));
    }

    @Override
    protected PLabel update0(final DataType value, final PLabel widget) {
        widget.setText(transform.apply(value));
        return widget;
    }

    @Override
    protected void reset0(final PLabel widget) {
        widget.setText("");
    }

}
