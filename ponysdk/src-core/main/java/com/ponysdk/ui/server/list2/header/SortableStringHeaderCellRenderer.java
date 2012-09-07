
package com.ponysdk.ui.server.list2.header;

import com.ponysdk.core.query.Criterion;

public class SortableStringHeaderCellRenderer extends SortableHeader {

    public SortableStringHeaderCellRenderer(final String caption, final Criterion criterionField) {
        super(new StringHeaderCellRenderer(caption), criterionField);
    }

}
