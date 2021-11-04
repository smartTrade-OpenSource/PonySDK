/*============================================================================
 *
 * Copyright (c) 2000-2021 Smart Trade Technologies. All Rights Reserved.
 *
 * This software is the proprietary information of Smart Trade Technologies
 * Use is subject to license terms. Duplication or distribution prohibited.
 *
 *============================================================================*/

package com.ponysdk.core.ui.listbox;

import com.ponysdk.core.ui.dropdown.DefaultDropDownContainerConfiguration;

public final class ListBoxConfiguration extends DefaultDropDownContainerConfiguration {

    private boolean searchEnabled = true;
    private String noMatchesLabel; // TODO missing
    private boolean multiSelectionEnabled;
    private boolean sortingEnabled = true;
    private boolean selectionSortingEnabled = true;
    private Integer selectionLimit;
    private Integer displaySelectionLimit;
    private String displaySelectionLabel;
    private boolean groupEnabled = false;

    public String getNoMatchesLabel() {
        return noMatchesLabel;
    }

    public boolean isSearchEnabled() {
        return searchEnabled;
    }

    public ListBoxConfiguration disableSearch() {
        this.searchEnabled = false;
        return this;
    }

    public ListBoxConfiguration setNoMatchesLabel(final String noMatchesLabel) {
        this.noMatchesLabel = noMatchesLabel;
        return this;
    }

    public boolean isMultiSelectionEnabled() {
        return multiSelectionEnabled;
    }

    public ListBoxConfiguration enableMultiSelection() {
        this.multiSelectionEnabled = true;
        return this;
    }

    public boolean isSortingEnabled() {
        return sortingEnabled;
    }

    public ListBoxConfiguration disableSorting() {
        this.sortingEnabled = false;
        return this;
    }

    public boolean isSelectionSortingEnabled() {
        return selectionSortingEnabled;
    }

    public ListBoxConfiguration disableSelectionSortingEnabled() {
        this.selectionSortingEnabled = false;
        return this;
    }

    public Integer getSelectionLimit() {
        return selectionLimit;
    }

    public ListBoxConfiguration setSelectionLimit(final Integer selectionLimit) {
        this.selectionLimit = selectionLimit;
        return this;
    }

    public Integer getDisplaySelectionLimit() {
        return displaySelectionLimit;
    }

    public ListBoxConfiguration setDisplaySelectionLimit(final Integer displaySelectionLimit) {
        this.displaySelectionLimit = displaySelectionLimit;
        return this;
    }

    public String getDisplaySelectionLabel() {
        return displaySelectionLabel;
    }

    public ListBoxConfiguration setDisplaySelectionLabel(final String displaySelectionLabel) {
        this.displaySelectionLabel = displaySelectionLabel;
        return this;
    }

    public boolean isGroupEnabled() {
        return groupEnabled;
    }

    public ListBoxConfiguration enableGroup() {
        this.groupEnabled = true;
        return this;
    }

    @Override
    public ListBoxConfiguration setTitleDisplayed(final boolean titleDisplayed) {
        super.setTitleDisplayed(titleDisplayed);
        return this;
    }

    @Override
    public ListBoxConfiguration disableTitlePlaceHolder() {
        super.disableTitlePlaceHolder();
        return this;
    }

    @Override
    public ListBoxConfiguration setTitle(final String title) {
        super.setTitle(title);
        return this;
    }

    @Override
    public ListBoxConfiguration setPlaceholder(final String placeholder) {
        super.setPlaceholder(placeholder);
        return this;
    }

    @Override
    public ListBoxConfiguration setAllLabel(final String allLabel) {
        super.setAllLabel(allLabel);
        return this;
    }

    @Override
    public ListBoxConfiguration setClearLabel(final String clearLabel) {
        super.setClearLabel(clearLabel);
        return this;
    }

    @Override
    public ListBoxConfiguration setSelectionDisplayed(final boolean selectionDisplayed) {
        super.setSelectionDisplayed(selectionDisplayed);
        return this;
    }

    @Override
    public ListBoxConfiguration setCloseOnClickMode(final DropDownCloseOnClickMode closeOnClickMode) {
        super.setCloseOnClickMode(closeOnClickMode);
        return this;
    }

    @Override
    public ListBoxConfiguration disableClearTitleButton() {
        super.disableClearTitleButton();
        return this;
    }
}
