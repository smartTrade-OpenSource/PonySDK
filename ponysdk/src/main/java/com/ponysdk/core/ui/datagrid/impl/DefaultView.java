package com.ponysdk.core.ui.datagrid.impl;

import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.datagrid.View;

public class DefaultView implements View {

    private final PFlexTable table = new PFlexTable();

    @Override
    public PWidget asWidget() {
        return table;
    }

    @Override
    public void setHeader(final int c, final PWidget w) {
        table.setWidget(0, c, w);
    }

    @Override
    public PWidget getHeader(final int c) {
        return table.getWidget(0, c);
    }

    @Override
    public PWidget getCell(final int r, final int c) {
        return table.getWidget(r + 1, c);
    }

    @Override
    public int getRowCount() {
        return table.getRowCount() - 1;
    }

    @Override
    public void setCell(final int r, final int c, final PWidget w) {
        table.setWidget(r + 1, c, w);
    }

}