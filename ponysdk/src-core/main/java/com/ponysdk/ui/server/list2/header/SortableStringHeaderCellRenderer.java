
package com.ponysdk.ui.server.list2.header;

import com.ponysdk.core.query.CriterionField;

public class SortableStringHeaderCellRenderer extends SortableHeader {

    public SortableStringHeaderCellRenderer(final String caption, final CriterionField criterionField) {
        super(new StringHeaderCellRenderer(caption), criterionField);
    }

}
