
package com.ponysdk.ui.server.list2.selector;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.list.SelectionMode;
import com.ponysdk.ui.server.list2.Resetable;

public class Selector<T> implements SelectorActionViewListener, SelectorInfoViewListener, SelectableListener, Resetable {

    private final SelectorActionView selectorActionView;
    private final SelectorInfoView selectorInfoView;
    private final List<Selectable<T>> selectableList = new ArrayList<Selectable<T>>();

    private int pageSize = 20;
    private int numberOfSelectedItems = 0;
    private int fullSize = 0;

    private SelectionMode selectionMode = SelectionMode.NONE;

    public Selector(final SelectorActionView selectorActionView, final SelectorInfoView selectorInfoView) {
        this.selectorActionView = selectorActionView;
        this.selectorInfoView = selectorInfoView;

        selectorActionView.addSelectorActionViewListener(this);
        selectorInfoView.addSelectorInfoViewListener(this);
    }

    public Selector(final SelectorActionView selectorActionView, final SelectorInfoView selectorInfoView, final int pageSize) {
        this.selectorActionView = selectorActionView;
        this.selectorInfoView = selectorInfoView;
        this.pageSize = pageSize;

        selectorActionView.addSelectorActionViewListener(this);
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

    public PWidget getActionView() {
        return selectorActionView.asWidget();
    }

    public PWidget getInfoView() {
        return selectorInfoView.asWidget();
    }

    @Override
    public void onSelectAllVisible() {
        for (final Selectable<T> selectable : selectableList) {
            selectable.select();
        }
        numberOfSelectedItems = selectableList.size();
        checkNumberOfSelectedItems();

    }

    @Override
    public void onUnselectAllVisible() {
        for (final Selectable<T> selectable : selectableList) {
            selectable.unselect();
        }
        numberOfSelectedItems = 0;
        selectionMode = SelectionMode.NONE;
    }

    @Override
    public void onFullSelection() {
        selectionMode = SelectionMode.FULL;
        numberOfSelectedItems = fullSize;
        selectorInfoView.showClearSelectionOption(numberOfSelectedItems);
    }

    @Override
    public void onClearFullSelection() {
        selectorInfoView.hide();
        onUnselectAllVisible();
    }

    @Override
    public void onSelect() {
        numberOfSelectedItems++;
        selectionMode = SelectionMode.PARTIAL;
        checkNumberOfSelectedItems();
    }

    private void checkNumberOfSelectedItems() {
        if (numberOfSelectedItems == pageSize && pageSize != 0 && pageSize != fullSize) {
            selectorInfoView.showSelectAllOption(numberOfSelectedItems, fullSize);
        } else if (numberOfSelectedItems == fullSize) {
            selectorInfoView.showAllSelected(numberOfSelectedItems);
        } else if (numberOfSelectedItems == 0) {
            selectionMode = SelectionMode.NONE;
        }
    }

    @Override
    public void onUnselect() {
        numberOfSelectedItems--;
        selectorInfoView.hide();
        checkNumberOfSelectedItems();
    }

    @Override
    public void reset() {
        selectableList.clear();
        numberOfSelectedItems = 0;
        selectionMode = SelectionMode.NONE;
        selectorInfoView.hide();
    }

    public void setFullSize(final int fullSize) {
        this.fullSize = fullSize;
    }

    public SelectionMode getSelectionMode() {
        return selectionMode;
    }

}
