
package com.ponysdk.core.ui.datagrid;

import com.ponysdk.core.ui.basic.IsPWidget;

public abstract class TypedCellRenderer<DataType, WidgetType extends IsPWidget> implements CellRenderer<DataType> {

    @Override
    public IsPWidget update(final DataType value, final IsPWidget current) {
        return update0(value, cast(current));
    }

    protected WidgetType cast(final IsPWidget w) {
        return (WidgetType) w;
    }

    @Override
    public void reset(final IsPWidget widget) {
        reset0(cast(widget));
    }

    @Override
    public abstract WidgetType render(final DataType value);

    protected abstract WidgetType update0(final DataType value, final WidgetType widget);

    protected abstract void reset0(final WidgetType widget);

}
