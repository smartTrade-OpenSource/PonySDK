package com.ponysdk.ui.server.basic;

import com.ponysdk.ui.model.ServerToClientModel;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

public class PCellFormatter {

    protected final PHTMLTable<? extends PCellFormatter> table;

    public PCellFormatter(final PHTMLTable<? extends PCellFormatter> table) {
        this.table = table;
    }

    public void addStyleName(final int row, final int column, final String styleName) {
        table.saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.CELL_FORMATTER_ADD_STYLE_NAME, styleName);
            writer.writeModel(ServerToClientModel.ROW, row);
            writer.writeModel(ServerToClientModel.COLUMN, column);
        });
    }

    public void removeStyleName(final int row, final int column, final String styleName) {
        table.saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.CELL_FORMATTER_REMOVE_STYLE_NAME, styleName);
            writer.writeModel(ServerToClientModel.ROW, row);
            writer.writeModel(ServerToClientModel.COLUMN, column);
        });
    }

    public void setStyleName(final int row, final int column, final String styleName) {
        table.saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.CELL_FORMATTER_SET_STYLE_NAME, styleName);
            writer.writeModel(ServerToClientModel.ROW, row);
            writer.writeModel(ServerToClientModel.COLUMN, column);
        });
    }

    public void setVerticalAlignment(final int row, final int column, final PVerticalAlignment align) {
        table.saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.VERTICAL_ALIGNMENT, align.getValue());
            writer.writeModel(ServerToClientModel.ROW, row);
            writer.writeModel(ServerToClientModel.COLUMN, column);
        });
    }

    public void setHorizontalAlignment(final int row, final int column, final PHorizontalAlignment align) {
        table.saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.HORIZONTAL_ALIGNMENT, align.getValue());
            writer.writeModel(ServerToClientModel.ROW, row);
            writer.writeModel(ServerToClientModel.COLUMN, column);
        });
    }
}