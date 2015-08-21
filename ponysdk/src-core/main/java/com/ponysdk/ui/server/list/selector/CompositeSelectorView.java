
package com.ponysdk.ui.server.list.selector;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.ui.server.basic.PWidget;

public class CompositeSelectorView implements SelectorView {

    private final List<SelectorView> views = new ArrayList<>();

    public CompositeSelectorView(final SelectorView... views) {
        for (final SelectorView selectorView : views) {
            this.views.add(selectorView);
        }
    }

    @Override
    public PWidget asWidget() {
        // TODO
        return views.get(0).asWidget();
    }

    @Override
    public void addSelectorViewListener(final SelectorViewListener selectorViewListener) {
        for (final SelectorView view : views) {
            view.addSelectorViewListener(selectorViewListener);
        }
    }

    @Override
    public void update(final SelectionMode selectionMode, final int numberOfSelectedItems, final int fullSize, final int pageSize) {
        for (final SelectorView view : views) {
            view.update(selectionMode, numberOfSelectedItems, fullSize, pageSize);
        }
    }

}
