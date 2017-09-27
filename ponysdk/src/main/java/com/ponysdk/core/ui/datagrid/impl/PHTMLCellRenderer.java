
package com.ponysdk.core.ui.datagrid.impl;

import java.util.function.Function;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PHTML;

public class PHTMLCellRenderer<DataType> extends TypedCellRenderer<DataType, PHTML> {

    private final Function<DataType, String> transform;

    public PHTMLCellRenderer() {
        this(String::valueOf);
    }

    public PHTMLCellRenderer(final Function<DataType, String> transform) {
        this.transform = transform;
    }

    @Override
    public PHTML render(final DataType value) {
        return Element.newPHTML(transform.apply(value));
    }

    @Override
    protected PHTML update0(final DataType value, final PHTML widget) {
        widget.setHTML(transform.apply(value));
        return widget;
    }

    @Override
    protected void reset0(final PHTML widget) {
        if (widget != null) widget.setHTML("");
    }

}
