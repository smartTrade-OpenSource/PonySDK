package com.ponysdk.core.ui.datagrid;

import com.ponysdk.core.ui.basic.PWidget;

public interface CellRenderer<DataType> {

    PWidget render(final DataType value);

    PWidget update(final DataType value, PWidget current);

    void reset(PWidget widget);

}
