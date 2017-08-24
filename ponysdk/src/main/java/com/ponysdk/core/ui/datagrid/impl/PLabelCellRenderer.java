package com.ponysdk.core.ui.datagrid.impl;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;

import java.util.function.Function;

public class PLabelCellRenderer<DataType> extends TypedCellRenderer<DataType, PLabel> {

    private final Function<DataType, String> transform;

    public PLabelCellRenderer() {
        this(String::valueOf);
    }

    public PLabelCellRenderer(final Function<DataType, String> transform) {
        this.transform = transform;
    }

    @Override
    public PLabel render(final DataType value) {
        return Element.newPLabel(transform.apply(value));
    }

    @Override
    protected PLabel update0(final DataType value, final PLabel widget) {
        widget.setText(transform.apply(value));
        return widget;
    }

    @Override
    protected void reset0(final PLabel widget) {
        if (widget != null)
            widget.setText("");
    }

}
