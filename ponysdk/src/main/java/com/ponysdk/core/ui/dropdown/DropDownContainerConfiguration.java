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

package com.ponysdk.core.ui.dropdown;

public interface DropDownContainerConfiguration {

    String DEFAULT_TITLE_SEPARATOR = ":";

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

    DropDownContainerConfiguration enableMultiSelectionClear(final String clearLabel);

    boolean isSelectionDisplayed();

    DropDownContainerConfiguration setSelectionDisplayed(final boolean selectionDisplayed);

    DropDownCloseOnClickMode getCloseOnClickMode();

    DropDownContainerConfiguration setCloseOnClickMode(final DropDownCloseOnClickMode closeOnClickMode);

    boolean isClearTitleButtonEnabled();

    DropDownContainerConfiguration disableClearTitleButton();

    boolean isEventOnlyEnabled();

    DropDownContainerConfiguration enabledEventOnly();

    boolean isStopClickEventEnabled();

    DropDownContainerConfiguration enableStopClickEvent();

    boolean isPositionDropRight();

    DropDownContainerConfiguration setPositionDropRight(final boolean dropRight);

    boolean isMultilevelEnabled();

    DropDownContainerConfiguration enableMultilevel();

    enum DropDownCloseOnClickMode {
        /**
         * <b>Default Mode</b><br>
         * Single selection: <b>true</b><br>
         * Multi selection: <b>false</b>
         */
        DEFAULT,
        TRUE,
        FALSE
    }

}
