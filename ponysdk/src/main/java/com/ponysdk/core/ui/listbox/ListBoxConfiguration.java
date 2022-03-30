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
    private boolean groupEnabled;

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
    public ListBoxConfiguration enableMultiSelectionClear(final String clearLabel) {
        super.enableMultiSelectionClear(clearLabel);
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
