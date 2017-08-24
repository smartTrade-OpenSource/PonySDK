package com.ponysdk.core.ui.datagrid;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PWidget;

public interface View extends IsPWidget {

    void setHeader(int c, PWidget w);

    void setCell(int r, int c, PWidget w);

    int getRowCount();

    PWidget getHeader(int r);

    PWidget getCell(int r, int c);

}