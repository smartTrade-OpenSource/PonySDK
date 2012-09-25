
package com.ponysdk.ui.server.list2.selector;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.list2.Resetable;

public class Selector<T> implements IsPWidget, SelectorViewListener, Resetable {

    private final SelectorView selectorView;
    private final List<Selectable<T>> selectableList = new ArrayList<Selectable<T>>();

    public Selector(final SelectorView selectorView) {
        this.selectorView = selectorView;
        selectorView.addSelectorViewListener(this);
    }

    public void registerSelectable(final Selectable<T> selectable) {
        selectableList.add(selectable);
    }

    public List<T> getSelectedData() {
        final List<T> selectedData = new ArrayList<T>();

        for (final Selectable<T> selectable : selectableList) {
            final T selectedDataRow = selectable.getSelectedData();
            if (selectedDataRow != null) selectedData.add(selectedDataRow);
        }

        return selectedData;
    }

    @Override
    public PWidget asWidget() {
        return selectorView.asWidget();
    }

    @Override
    public void onSelectAll() {
        for (final Selectable<T> selectable : selectableList) {
            selectable.select();
        }
    }

    @Override
    public void onUnselectAll() {
        for (final Selectable<T> selectable : selectableList) {
            selectable.unselect();
        }
    }

    @Override
    public void reset() {
        selectableList.clear();
    }

}
