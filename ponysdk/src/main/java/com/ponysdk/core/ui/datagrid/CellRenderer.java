package com.ponysdk.core.ui.datagrid;

import com.ponysdk.core.ui.basic.IsPWidget;

public interface CellRenderer<DataType> {

    IsPWidget render(final DataType value);

    IsPWidget update(final DataType value, IsPWidget current);

    void reset(IsPWidget widget);

}
