
package com.ponysdk.ui.server.list2.selector;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.list.SelectionMode;
import com.ponysdk.ui.server.list2.Resetable;

public class Selector<T> implements SelectorViewListener, SelectableListener, Resetable {

    private final SelectorView selectorView;
    private final List<Selectable<T>> selectableList = new ArrayList<Selectable<T>>();

    private int pageSize;
    private int numberOfSelectedItems = 0;
    private int fullSize = 0;

    private SelectionMode selectionMode = SelectionMode.NONE;

    public Selector(final SelectorView selectorView) {
        this(selectorView, 20);
    }

    public Selector(final SelectorView selectorView, final int pageSize) {
        this.selectorView = selectorView;
        this.pageSize = pageSize;
        this.selectorView.addSelectorViewListener(this);
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

    public PWidget getSelectorView() {
        return selectorView.asWidget();
    }

    @Override
    public void onSelectionChange(final SelectionMode mode) {
        switch (mode) {
            case NONE:
                for (final Selectable<T> selectable : selectableList) {
                    selectable.unselect();
                }
                numberOfSelectedItems = 0;
                break;
            case PAGE:
                for (final Selectable<T> selectable : selectableList) {
                    selectable.select();
                }
                numberOfSelectedItems = selectableList.size();
                break;
            case FULL:
                for (final Selectable<T> selectable : selectableList) {
                    selectable.select();
                }
                numberOfSelectedItems = fullSize;
                break;
            default:
                break;
        }

        selectionMode = mode;
        checkNumberOfSelectedItems();
    }

    @Override
    public void onSelect() {
        numberOfSelectedItems++;
        selectionMode = SelectionMode.PARTIAL;
        checkNumberOfSelectedItems();
    }

    @Override
    public void onUnselect() {
        if (numberOfSelectedItems > pageSize) numberOfSelectedItems = pageSize;
        numberOfSelectedItems--;
        selectionMode = SelectionMode.PARTIAL;
        checkNumberOfSelectedItems();
    }

    private void checkNumberOfSelectedItems() {
        if (numberOfSelectedItems > pageSize) {
            selectionMode = SelectionMode.FULL;
        } else if (numberOfSelectedItems == 0) {
            selectionMode = SelectionMode.NONE;
        } else {
            if (numberOfSelectedItems == fullSize) selectionMode = SelectionMode.FULL;
            else if (numberOfSelectedItems == pageSize) selectionMode = SelectionMode.PAGE;
            else selectionMode = SelectionMode.PARTIAL;
        }

        selectorView.update(selectionMode, numberOfSelectedItems, fullSize, pageSize);
    }

    @Override
    public void reset() {
        selectableList.clear();
        numberOfSelectedItems = 0;
        selectionMode = SelectionMode.NONE;
        selectorView.update(selectionMode, numberOfSelectedItems, fullSize, pageSize);
    }

    public void setFullSize(final int fullSize) {
        this.fullSize = fullSize;
    }

    public SelectionMode getSelectionMode() {
        return selectionMode;
    }

}
