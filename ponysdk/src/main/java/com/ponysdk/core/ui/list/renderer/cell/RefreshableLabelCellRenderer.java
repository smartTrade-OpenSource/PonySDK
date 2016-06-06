
package com.ponysdk.core.ui.list.renderer.cell;

import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.list.refreshable.Cell;

public class RefreshableLabelCellRenderer<V> implements CellRenderer<V, PHTML> {

    @Override
    public PHTML render(final int row, final V value) {
        return new PHTML(value == null ? "-" : value.toString());
    }

    @Override
    public void update(final V value, final Cell<V, PHTML> previous) {
        previous.getW().setText(value == null ? "-" : value.toString());
    }

}
