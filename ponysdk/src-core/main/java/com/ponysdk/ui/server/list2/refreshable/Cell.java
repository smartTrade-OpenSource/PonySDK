
package com.ponysdk.ui.server.list2.refreshable;

import com.ponysdk.ui.server.basic.IsPWidget;

public class Cell<D, W extends IsPWidget> {

    W w;
    D data;
    Object value;
    int row;
    int col;

    public W getW() {
        return w;
    }

    public D getData() {
        return data;
    }

    public Object getValue() {
        return value;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }
}