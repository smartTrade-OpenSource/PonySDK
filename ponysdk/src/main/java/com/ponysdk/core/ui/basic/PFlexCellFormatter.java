package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.ServerToClientModel;

public class PFlexCellFormatter extends PCellFormatter {

    public PFlexCellFormatter(final PHTMLTable<PFlexCellFormatter> table) {
        super(table);
    }

    public void setColSpan(final int row, final int column, final int colSpan) {
        table.saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.SET_COL_SPAN, colSpan);
            writer.writeModel(ServerToClientModel.ROW, row);
            writer.writeModel(ServerToClientModel.COLUMN, column);
        });
    }

    public void setRowSpan(final int row, final int column, final int rowSpan) {
        table.saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.SET_ROW_SPAN, rowSpan);
            writer.writeModel(ServerToClientModel.ROW, row);
            writer.writeModel(ServerToClientModel.COLUMN, column);
        });
    }
}