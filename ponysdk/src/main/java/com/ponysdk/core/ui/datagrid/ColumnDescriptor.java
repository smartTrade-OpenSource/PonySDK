package com.ponysdk.core.ui.datagrid;

public class ColumnDescriptor<D> {

    private HeaderRenderer headerRenderer;
    private CellRenderer<D> cellRenderer;

    HeaderRenderer getHeaderRenderer() {
        return headerRenderer;
    }

    public void setHeaderRenderer(final HeaderRenderer headerCellRender) {
        this.headerRenderer = headerCellRender;
    }

    CellRenderer<D> getCellRenderer() {
        return cellRenderer;
    }

    public void setCellRenderer(final CellRenderer<D> cellRenderer) {
        this.cellRenderer = cellRenderer;
    }

}
