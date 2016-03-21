
package com.ponysdk.ui.server.list;

import com.ponysdk.ui.server.basic.IsPWidget;

public class Cell<D, W extends IsPWidget> {

    private W w;
    private D data;
    private int row;
    private int col;

    public void setW(final W w) {
        this.w = w;
    }

    public void setData(final D data) {
        this.data = data;
    }

    public void setRow(final int row) {
        this.row = row;
    }

    public void setCol(final int col) {
        this.col = col;
    }

    public W getW() {
        return w;
    }

    public D getData() {
        return data;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }
}