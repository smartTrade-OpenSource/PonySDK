/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.ui.list.selector;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.list.Resetable;

public class Selector<T> implements SelectorViewListener, SelectableListener, Resetable {

    private final SelectorView selectorView;
    private final List<Selectable<T>> selectableList = new ArrayList<>();

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
        final List<T> selectedData = new ArrayList<>();

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
            case PARTIAL:
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

    public void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
    }
}
