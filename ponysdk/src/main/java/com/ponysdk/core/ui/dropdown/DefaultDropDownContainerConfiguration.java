/*============================================================================
 *
 * Copyright (c) 2000-2021 Smart Trade Technologies. All Rights Reserved.
 *
 * This software is the proprietary information of Smart Trade Technologies
 * Use is subject to license terms. Duplication or distribution prohibited.
 *
 *============================================================================*/

package com.ponysdk.core.ui.dropdown;

public class DefaultDropDownContainerConfiguration implements DropDownContainerConfiguration {

    private boolean titleDisplayed = true;
    private boolean titlePlaceHolder = true;
    private String title;
    private String titleSeparator = DropDownContainerConfiguration.DEFAULT_TITLE_SEPARATOR;
    private String placeholder;
    private String allLabel;
    private String clearLabel;
    private boolean selectionDisplayed = true;
    private DropDownCloseOnClickMode closeOnClickMode = DropDownCloseOnClickMode.DEFAULT;
    private boolean clearTitleButtonEnabled = true;

    public DefaultDropDownContainerConfiguration() {
        super();
    }

    @Override
    public boolean isTitleDisplayed() {
        return titleDisplayed;
    }

    @Override
    public DropDownContainerConfiguration setTitleDisplayed(final boolean titleDisplayed) {
        this.titleDisplayed = titleDisplayed;
        return this;
    }

    @Override
    public boolean isTitlePlaceHolder() {
        return titlePlaceHolder;
    }

    @Override
    public DropDownContainerConfiguration disableTitlePlaceHolder() {
        this.titlePlaceHolder = false;
        return this;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public DropDownContainerConfiguration setTitle(final String title) {
        this.title = title;
        return this;
    }

    @Override
    public String getTitleSeparator() {
        return titleSeparator;
    }

    @Override
    public DropDownContainerConfiguration setTitleSeparator(final String titleSeparator) {
        this.titleSeparator = titleSeparator;
        return this;
    }

    @Override
    public String getPlaceholder() {
        return placeholder;
    }

    @Override
    public DropDownContainerConfiguration setPlaceholder(final String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Override
    public String getAllLabel() {
        return allLabel;
    }

    @Override
    public DropDownContainerConfiguration setAllLabel(final String allLabel) {
        this.allLabel = allLabel;
        return this;
    }

    @Override
    public String getClearLabel() {
        return clearLabel;
    }

    @Override
    public DropDownContainerConfiguration setClearLabel(final String clearLabel) {
        this.clearLabel = clearLabel;
        return this;
    }

    @Override
    public boolean isSelectionDisplayed() {
        return selectionDisplayed;
    }

    @Override
    public DropDownContainerConfiguration setSelectionDisplayed(final boolean selectionDisplayed) {
        this.selectionDisplayed = selectionDisplayed;
        return this;
    }

    @Override
    public DropDownCloseOnClickMode getCloseOnClickMode() {
        return closeOnClickMode;
    }

    @Override
    public DropDownContainerConfiguration setCloseOnClickMode(final DropDownCloseOnClickMode closeOnClickMode) {
        this.closeOnClickMode = closeOnClickMode;
        return this;
    }

    @Override
    public boolean isClearTitleButtonEnabled() {
        return clearTitleButtonEnabled;
    }

    @Override
    public DropDownContainerConfiguration disableClearTitleButton() {
        this.clearTitleButtonEnabled = false;
        return this;
    }
}
