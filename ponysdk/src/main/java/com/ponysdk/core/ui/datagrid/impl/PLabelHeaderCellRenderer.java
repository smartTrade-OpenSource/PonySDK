
package com.ponysdk.core.ui.datagrid.impl;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.list.renderer.header.HeaderCellRenderer;

public class PLabelHeaderCellRenderer implements HeaderCellRenderer {

    private final PLabel widget;

    public PLabelHeaderCellRenderer(final String caption) {
        widget = new PLabel(caption);
    }

    @Override
    public IsPWidget render() {
        return widget;
    }

}
