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

package com.ponysdk.core.model;

public enum WidgetType {

    ROOT_LAYOUT_PANEL,
    ROOT_PANEL,
    DOCK_LAYOUT_PANEL,
    ABSOLUTE_PANEL,
    SPLIT_LAYOUT_PANEL,
    SIMPLE_PANEL,
    TAB_PANEL,
    SCROLL_PANEL,
    FLOW_PANEL,
    VERTICAL_PANEL,
    HORIZONTAL_PANEL,
    BUTTON,
    LABEL,
    FLEX_TABLE,
    GRID,
    TREE,
    TREE_ITEM,
    COMPOSITE,
    DATEBOX,
    DATEPICKER,
    TEXTBOX,
    PASSWORD_TEXTBOX,
    LISTBOX,
    IMAGE,
    ANCHOR,
    CHECKBOX,
    MENU_BAR,
    MENU_ITEM,
    MENU_ITEM_SEPARATOR,
    TAB_LAYOUT_PANEL,
    LAYOUT_PANEL,
    SIMPLE_LAYOUT_PANEL,
    STACKLAYOUT_PANEL,
    HTML,
    RADIO_BUTTON,
    TEXT_AREA,
    POPUP_PANEL,
    FILE_UPLOAD,
    PUSH_BUTTON,
    ADDON,
    ADDON_COMPOSITE,
    RICH_TEXT_AREA,
    RICH_TEXT_TOOLBAR,
    DISCLOSURE_PANEL,
    DECORATED_POPUP_PANEL,
    DIALOG_BOX,
    DECORATOR_PANEL,
    ELEMENT,
    FOCUS_PANEL,
    SCRIPT,
    WINDOW,
    FRAME,
    BROWSER,
    HEADER_PANEL,
    SUGGESTBOX,
    MULTIWORD_SUGGEST_ORACLE,
    FUNCTION,
    FUNCTIONAL_LABEL;

    private static final WidgetType[] VALUES = WidgetType.values();

    private WidgetType() {
    }

    public final byte getValue() {
        return (byte) ordinal();
    }

    public static WidgetType fromRawValue(final int rawValue) {
        return VALUES[rawValue];
    }

}
