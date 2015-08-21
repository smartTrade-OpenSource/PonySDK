
package com.ponysdk.ui.server.list.renderer.cell;

import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.list.refreshable.Cell;
import com.ponysdk.ui.server.list.refreshable.RefreshableCellRenderer;

public class RefreshableLabelCellRenderer<V> implements RefreshableCellRenderer<V, PHTML> {

    @Override
    public PHTML render(final int row, final V value) {
        return new PHTML((value == null ? "-" : value.toString()));
    }

    @Override
    public void update(final V value, final Cell<V, PHTML> previous) {
        previous.getW().setText((value == null ? "-" : value.toString()));
    }

}
