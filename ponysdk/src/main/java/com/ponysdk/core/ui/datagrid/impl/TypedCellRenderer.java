
package com.ponysdk.core.ui.datagrid.impl;

import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.datagrid.CellRenderer;

public abstract class TypedCellRenderer<DataType, WidgetType extends PWidget> implements CellRenderer<DataType> {

    @Override
    public PWidget update(final DataType value, final PWidget current) {
        return update0(value, cast(current));
    }

    private WidgetType cast(final PWidget w) {
        return (WidgetType) w;
    }

    @Override
    public void reset(final PWidget widget) {
        reset0(cast(widget));
    }

    @Override
    public abstract WidgetType render(final DataType value);

    protected abstract WidgetType update0(final DataType value, final WidgetType widget);

    protected abstract void reset0(final WidgetType widget);

}
