package com.ponysdk.ui.server.basic;

import com.ponysdk.ui.model.ServerToClientModel;
import com.ponysdk.ui.server.model.ServerBinaryModel;

public class PFlexCellFormatter extends PCellFormatter {

    public PFlexCellFormatter(final PHTMLTable<PFlexCellFormatter> table) {
        super(table);
    }

    public void setColSpan(final int row, final int column, final int colSpan) {
        table.saveUpdate(new ServerBinaryModel(ServerToClientModel.SET_COL_SPAN, colSpan), new ServerBinaryModel(ServerToClientModel.ROW, row),
                new ServerBinaryModel(ServerToClientModel.COLUMN, column));
    }

    public void setRowSpan(final int row, final int column, final int rowSpan) {
        table.saveUpdate(new ServerBinaryModel(ServerToClientModel.SET_ROW_SPAN, rowSpan), new ServerBinaryModel(ServerToClientModel.ROW, row),
                new ServerBinaryModel(ServerToClientModel.COLUMN, column));
    }
}