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

package com.ponysdk.ui.terminal;

public class PropertyKey {

    public static final PropertyKey ROOT = new PropertyKey("ROOT");

    public static final PropertyKey WIDTH = new PropertyKey("WIDTH");

    public static final PropertyKey WIDGET_WIDTH = new PropertyKey("WIDGET_WIDTH");

    public static final PropertyKey WIDGET_HEIGHT = new PropertyKey("WIDGET_HEIGHT");

    public static final PropertyKey STYLE_PROPERTY = new PropertyKey("STYLE_PROPERTY");

    public static final PropertyKey WIDGET_FONT_SIZE = new PropertyKey("WIDGET_FONT_SIZE");

    public static final PropertyKey TEXT = new PropertyKey("TEXT");

    public static final PropertyKey HTML = new PropertyKey("HTML");

    public static final PropertyKey ITEM_INSERTED = new PropertyKey("ITEM_INSERTED");

    public static final PropertyKey ITEM_TEXT = new PropertyKey("ITEM_TEXT");

    public static final PropertyKey VALUE = new PropertyKey("VALUE");

    public static final PropertyKey INDEX = new PropertyKey("INDEX");

    public static final PropertyKey BORDER_WIDTH = new PropertyKey("BORDER_WIDTH");

    public static final PropertyKey SPACING = new PropertyKey("SPACING");

    public static final PropertyKey CELL_SPACING = new PropertyKey("CELL_SPACING");

    public static final PropertyKey CELL_PADDING = new PropertyKey("CELL_PADDING");

    public static final PropertyKey DIRECTION = new PropertyKey("DIRECTION");

    public static final PropertyKey SIZE = new PropertyKey("SIZE");

    public static final PropertyKey ROW = new PropertyKey("ROW");

    public static final PropertyKey CELL = new PropertyKey("CELL");

    public static final PropertyKey CLEAR = new PropertyKey("CLEAR");

    public static final PropertyKey CLEAR_ROW = new PropertyKey("CLEAR_ROW");

    public static final PropertyKey INSERT_ROW = new PropertyKey("INSERT_ROW");

    public static final PropertyKey TREE_ITEM_POSITION_PATH = new PropertyKey("TREE_ITEM_POSITION_PATH");

    public static final PropertyKey WIDGET_VISIBLE = new PropertyKey("WIDGET_VISIBLE");

    public static final PropertyKey STYLE_KEY = new PropertyKey("STYLE_KEY");

    public static final PropertyKey STYLE_VALUE = new PropertyKey("STYLE_VALUE");

    public static final PropertyKey STYLE_NAME = new PropertyKey("STYLE_NAME");

    public static final PropertyKey ADD_STYLE_NAME = new PropertyKey("ADD_STYLE_NAME");

    public static final PropertyKey REMOVE_STYLE_NAME = new PropertyKey("REMOVE_STYLE_NAME");

    public static final PropertyKey IMAGE_URL = new PropertyKey("IMAGE_URL");

    public static final PropertyKey NOTIFICATION_CAPTION = new PropertyKey("NOTIFICATION_CAPTION");

    public static final PropertyKey NOTIFICATION_MESSAGE = new PropertyKey("NOTIFICATION_MESSAGE");

    public static final PropertyKey ENSURE_DEBUG_ID = new PropertyKey("ENSURE_DEBUG_ID");

    public static final PropertyKey ENABLED = new PropertyKey("ENABLED");

    public static final PropertyKey DATE_FORMAT = new PropertyKey("DATE_FORMAT");

    public static final PropertyKey DATE_FORMAT_PATTERN = new PropertyKey("DATE_FORMAT_PATTERN");

    public static final PropertyKey POPUP_GLASS_ENABLED = new PropertyKey("POPUP_GLASS_ENABLED");

    public static final PropertyKey POPUP_CENTER = new PropertyKey("POPUP_CENTER");

    public static final PropertyKey POPUP_SHOW = new PropertyKey("POPUP_SHOW");

    public static final PropertyKey POPUP_HIDE = new PropertyKey("POPUP_HIDE");

    public static final PropertyKey POPUP_TEXT = new PropertyKey("POPUP_TEXT");

    public static final PropertyKey POPUP_GLASS_STYLE_NAME = new PropertyKey("POPUP_GLASS_STYLE_NAME");

    public static final PropertyKey MENU_ITEM_POSITION_PATH = new PropertyKey("MENU_ITEM_POSITION_PATH");

    public static final PropertyKey TAB_WIDGET = new PropertyKey("TAB_WIDGET");

    public static final PropertyKey WIDGET = new PropertyKey("WIDGET");

    public static final PropertyKey TAB_TEXT = new PropertyKey("TAB_TEXT");

    public static final PropertyKey ANIMATION = new PropertyKey("ANIMATION");

    public static final PropertyKey WIDGET_TITLE = new PropertyKey("WIDGET_TITLE");

    public static final PropertyKey DIALOG_BOX_CLOSE_WIDGET = new PropertyKey("DIALOG_BOX_CLOSE_WIDGET");

    public static final PropertyKey MENU_BAR_IS_VERTICAL = new PropertyKey("MENU_BAR_IS_VERTICAL");

    public static final PropertyKey DIALOG_BOX_CLOSABLE = new PropertyKey("DIALOG_BOX_CLOSABLE");

    public static final PropertyKey SELECTED_INDEX = new PropertyKey("SELECTED_INDEX");

    public static final PropertyKey SELECTED = new PropertyKey("SELECTED");

    public static final PropertyKey STREAM_REQUEST_ID = new PropertyKey("STREAM_REQUEST_ID");

    public static final PropertyKey DELAY = new PropertyKey("DELAY");

    public static final PropertyKey IMAGE_STREAM_URL = new PropertyKey("IMAGE_STREAM_URL");

    public static final PropertyKey MULTISELECT = new PropertyKey("MULTISELECT");

    public static final PropertyKey CELL_HORIZONTAL_ALIGNMENT = new PropertyKey("CELL_HORIZONTAL_ALIGNMENT");

    public static final PropertyKey CELL_VERTICAL_ALIGNMENT = new PropertyKey("CELL_VERTICAL_ALIGNMENT");

    public static final PropertyKey HORIZONTAL_ALIGNMENT = new PropertyKey("HORIZONTAL_ALIGNMENT");

    public static final PropertyKey VERTICAL_ALIGNMENT = new PropertyKey("VERTICAL_ALIGNMENT");

    public static final PropertyKey CELL_HEIGHT = new PropertyKey("CELL_HEIGHT");

    public static final PropertyKey CELL_WIDTH = new PropertyKey("CELL_WIDTH");

    public static final PropertyKey WORD_WRAP = new PropertyKey("WORD_WRAP");

    public static final PropertyKey NAME = new PropertyKey("NAME");

    public static final PropertyKey RELOAD = new PropertyKey("RELOAD");

    public static final PropertyKey UNIT = new PropertyKey("UNIT");

    public static final PropertyKey POPUP_POSITION_CALLBACK = new PropertyKey("POPUP_POSITION_CALLBACK");

    public static final PropertyKey FOCUSED = new PropertyKey("FOCUSED");

    public static final PropertyKey POPUP_POSITION = new PropertyKey("POPUP_POSITION");

    public static final PropertyKey POPUP_POSITION_LEFT = new PropertyKey("POPUP_POSITION_LEFT");

    public static final PropertyKey POPUP_POSITION_TOP = new PropertyKey("POPUP_POSITION_TOP");

    public static final PropertyKey VISIBLE_LINES = new PropertyKey("VISIBLE_LINES");

    public static final PropertyKey CHARACTER_WIDTH = new PropertyKey("CHARACTER_WIDTH");

    public static final PropertyKey HORIZONTAL_SCROLL_POSITION = new PropertyKey("HORIZONTAL_SCROLL_POSITION");

    public static final PropertyKey OFFSETWIDTH = new PropertyKey("OFFSETWIDTH");

    public static final PropertyKey OFFSETHEIGHT = new PropertyKey("OFFSETHEIGHT");

    public static final PropertyKey CLIENT_WIDTH = new PropertyKey("CLIENT_WIDTH");

    public static final PropertyKey CLIENT_HEIGHT = new PropertyKey("CLIENT_HEIGHT");

    public static final PropertyKey POPUP_AUTO_HIDE = new PropertyKey("POPUP_AUTO_HIDE");

    public static final PropertyKey DOM_HANDLER = new PropertyKey("DOM_HANDLER");

    public static final PropertyKey ROW_FORMATTER_STYLE_NAME = new PropertyKey("ROW_FORMATTER_STYLE_NAME");

    public static final PropertyKey ROW_FORMATTER_ADD_STYLE_NAME = new PropertyKey("ROW_FORMATTER_ADD_STYLE_NAME");

    public static final PropertyKey HTMLTABLE_ROW_STYLE = new PropertyKey("HTMLTABLE_ROW_STYLE");

    public static final PropertyKey ROW_FORMATTER_REMOVE_STYLE_NAME = new PropertyKey("ROW_FORMATTER_REMOVE_STYLE_NAME");

    public static final PropertyKey KEY_FILTER = new PropertyKey("KEY_FILTER");

    public static final PropertyKey CLIENT_X = new PropertyKey("CLIENT_X");

    public static final PropertyKey CLIENT_Y = new PropertyKey("CLIENT_Y");

    public static final PropertyKey SOURCE_ABSOLUTE_LEFT = new PropertyKey("SOURCE_ABSOLUTE_LEFT");

    public static final PropertyKey SOURCE_ABSOLUTE_TOP = new PropertyKey("SOURCE_ABSOLUTE_TOP");

    public static final PropertyKey SOURCE_OFFSET_HEIGHT = new PropertyKey("SOURCE_OFFSET_HEIGHT");

    public static final PropertyKey SOURCE_OFFSET_WIDTH = new PropertyKey("SOURCE_OFFSET_WIDTH");

    public static final PropertyKey COOKIE = new PropertyKey("COOKIE");

    public static final PropertyKey COOKIE_EXPIRE = new PropertyKey("COOKIE_EXPIRE");

    public static final PropertyKey REFERENCE_SCROLL_PANEL = new PropertyKey("REFERENCE_SCROLL_PANEL");

    public static final PropertyKey IMAGE = new PropertyKey("IMAGE");

    public static final PropertyKey BEFORE_INDEX = new PropertyKey("BEFORE_INDEX");

    public static final PropertyKey CORRECT_DIMENSION = new PropertyKey("CORRECT_DIMENSION");

    public static final PropertyKey DISCLOSURE_PANEL_OPEN_IMG = new PropertyKey("DISCLOSURE_PANEL_OPEN_IMG");

    public static final PropertyKey DISCLOSURE_PANEL_CLOSE_IMG = new PropertyKey("DISCLOSURE_PANEL_CLOSE_IMG");

    public static final PropertyKey HTMLTABLE_CELL_STYLE = new PropertyKey("HTMLTABLE_CELL_STYLE");

    public static final PropertyKey CELL_FORMATTER_ADD_STYLE_NAME = new PropertyKey("CELL_FORMATTER_ADD_STYLE_NAME");

    public static final PropertyKey CELL_FORMATTER_REMOVE_STYLE_NAME = new PropertyKey("CELL_FORMATTER_REMOVE_STYLE_NAME");

    public static final PropertyKey COLUMN = new PropertyKey("COLUMN");

    public static final PropertyKey FLEXTABLE_CELL_FORMATTER = new PropertyKey("FLEXTABLE_CELL_FORMATTER");

    public static final PropertyKey COLUMN_FORMATTER_COLUMN_WIDTH = new PropertyKey("COLUMN_FORMATTER_COLUMN_WIDTH");

    public static final PropertyKey COLUMN_FORMATTER_ADD_STYLE_NAME = new PropertyKey("COLUMN_FORMATTER_ADD_STYLE_NAME");

    public static final PropertyKey COLUMN_FORMATTER_REMOVE_STYLE_NAME = new PropertyKey("COLUMN_FORMATTER_REMOVE_STYLE_NAME");

    public static final PropertyKey SET_COL_SPAN = new PropertyKey("SET_COL_SPAN");

    public static final PropertyKey SET_ROW_SPAN = new PropertyKey("SET_ROW_SPAN");

    public static final PropertyKey FILE_NAME = new PropertyKey("FILE_NAME");

    public static final PropertyKey LOADING = new PropertyKey("LOADING");

    public static final PropertyKey END_OF_PROCESSING = new PropertyKey("END_OF_PROCESSING");

	public static final PropertyKey OPEN = new PropertyKey("OPEN");

    private final String key;

    public PropertyKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object obj) {
        return key.equalsIgnoreCase(((PropertyKey) obj).getKey());
    }

    @Override
    public String toString() {
        return "PropertyKey [Key=" + key + "]";
    }

}
