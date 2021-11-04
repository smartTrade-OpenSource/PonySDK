/*============================================================================
 *
 * Copyright (c) 2000-2021 Smart Trade Technologies. All Rights Reserved.
 *
 * This software is the proprietary information of Smart Trade Technologies
 * Use is subject to license terms. Duplication or distribution prohibited.
 *
 *============================================================================*/

package com.ponysdk.core.ui.dropdown;

public interface DropDownContainerConfiguration {

    public enum DropDownCloseOnClickMode {
        /**
         * <b>Default Mode</b><br>
         * Single selection: <b>true</b><br>
         * Multi selection: <b>false</b>
         */
        DEFAULT,
        TRUE,
        FALSE;
    }

    public final static String DEFAULT_TITLE_SEPARATOR = ":";

    boolean isTitleDisplayed();

    DropDownContainerConfiguration setTitleDisplayed(final boolean titleDisplayed);

    boolean isTitlePlaceHolder();

    DropDownContainerConfiguration disableTitlePlaceHolder();

    String getTitle();

    DropDownContainerConfiguration setTitle(final String title);

    String getTitleSeparator();

    DropDownContainerConfiguration setTitleSeparator(final String titleSeparator);

    String getPlaceholder();

    DropDownContainerConfiguration setPlaceholder(final String placeholder);

    String getAllLabel();

    DropDownContainerConfiguration setAllLabel(final String allLabel);

    String getClearLabel();

    DropDownContainerConfiguration setClearLabel(final String clearLabel);

    boolean isSelectionDisplayed();

    DropDownContainerConfiguration setSelectionDisplayed(final boolean selectionDisplayed);

    DropDownCloseOnClickMode getCloseOnClickMode();

    DropDownContainerConfiguration setCloseOnClickMode(final DropDownCloseOnClickMode closeOnClickMode);

    boolean isClearTitleButtonEnabled();

    DropDownContainerConfiguration disableClearTitleButton();
}
