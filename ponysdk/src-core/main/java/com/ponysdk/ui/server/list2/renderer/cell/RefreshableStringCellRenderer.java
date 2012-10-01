
package com.ponysdk.ui.server.list2.renderer.cell;

import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.list2.refreshable.Cell;
import com.ponysdk.ui.server.list2.refreshable.RefreshableCellRenderer;

public class RefreshableStringCellRenderer<D, V> implements RefreshableCellRenderer<D, V, PHTML> {

    @Override
    public PHTML render(final int row, final D data, final V value) {
        return new PHTML((value == null ? "-" : value.toString()));
    }

    @Override
    public void update(final D data, final V value, final Cell<D, PHTML> previous) {
        if (value == previous.getValue()) return;
        previous.getW().setText((value == null ? "-" : value.toString()));
    }

}
