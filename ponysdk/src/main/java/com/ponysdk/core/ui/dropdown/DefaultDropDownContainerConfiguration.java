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
    private boolean eventOnlyEnabled;
    private boolean stopClickEvent;
    private DropDownPosition position = DropDownPosition.INSIDE;
    private boolean multilevelEnabled;

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
    public DropDownContainerConfiguration enableMultiSelectionClear(final String clearLabel) {
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

    public boolean isEventOnlyEnabled(){
        return this.eventOnlyEnabled;
    }

    public DropDownContainerConfiguration enabledEventOnly(){
        this.eventOnlyEnabled = true;
        return this;
    }

    @Override
    public boolean isStopClickEventEnabled() {
        return this.stopClickEvent;
    }

    @Override
    public DropDownContainerConfiguration enableStopClickEvent() {
        this.stopClickEvent = true;
        return this;
    }

    @Override
    public DropDownPosition getPosition() {
        return position;
    }

    @Override
    public DropDownContainerConfiguration setPosition(DropDownPosition position) {
        this.position = position;
        return this;
    }

    @Override
    public boolean isMultilevelEnabled() {
        return multilevelEnabled;
    }

    @Override
    public DropDownContainerConfiguration enableMultilevel() {
        this.multilevelEnabled = true;
        return this;
    }
}
