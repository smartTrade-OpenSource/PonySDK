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

public enum ServerToClientModel {

    HEARTBEAT(ValueTypeModel.NULL),
    PING_SERVER(ValueTypeModel.LONG),
    CREATE_CONTEXT(ValueTypeModel.INTEGER),
    DESTROY_CONTEXT(ValueTypeModel.NULL),
    END(ValueTypeModel.NULL),
    OPTION_FORMFIELD_TABULATION(ValueTypeModel.BOOLEAN),

    TYPE_CREATE(ValueTypeModel.INTEGER),
    TYPE_UPDATE(ValueTypeModel.INTEGER),
    TYPE_ADD(ValueTypeModel.INTEGER),
    TYPE_REMOVE(ValueTypeModel.INTEGER),
    TYPE_ADD_HANDLER(ValueTypeModel.INTEGER),
    TYPE_REMOVE_HANDLER(ValueTypeModel.NULL),
    TYPE_HISTORY(ValueTypeModel.STRING),
    TYPE_GC(ValueTypeModel.INTEGER),

    NATIVE(ValueTypeModel.JSON_OBJECT),
    DATE(ValueTypeModel.LONG),
    VALUE(ValueTypeModel.STRING_UTF8),
    PARENT_OBJECT_ID(ValueTypeModel.INTEGER),
    PUT_STYLE_KEY(ValueTypeModel.STRING),
    STYLE_VALUE(ValueTypeModel.STRING),
    REMOVE_STYLE_KEY(ValueTypeModel.STRING),
    PUT_PROPERTY_KEY(ValueTypeModel.STRING),
    PROPERTY_VALUE(ValueTypeModel.STRING_UTF8),
    PUT_ATTRIBUTE_KEY(ValueTypeModel.STRING),
    ATTRIBUTE_VALUE(ValueTypeModel.STRING_UTF8),
    REMOVE_ATTRIBUTE_KEY(ValueTypeModel.STRING),
    ADD_STYLE_NAME(ValueTypeModel.STRING),
    REMOVE_STYLE_NAME(ValueTypeModel.STRING),
    HREF(ValueTypeModel.STRING_UTF8),
    TEXT(ValueTypeModel.STRING_UTF8),
    HTML(ValueTypeModel.STRING_UTF8),
    WIDGET_ID(ValueTypeModel.INTEGER),
    WIDGET(ValueTypeModel.NULL),
    VALUE_CHECKBOX(ValueTypeModel.BYTE),
    WORD_WRAP(ValueTypeModel.BOOLEAN),
    CLEAR(ValueTypeModel.NULL),
    CLEAR_ROW(ValueTypeModel.INTEGER),
    INSERT_ROW(ValueTypeModel.INTEGER),
    CELL_PADDING(ValueTypeModel.INTEGER),
    CELL_SPACING(ValueTypeModel.INTEGER),
    HORIZONTAL_ALIGNMENT(ValueTypeModel.BYTE),
    WIDGET_HORIZONTAL_ALIGNMENT(ValueTypeModel.BYTE),
    VERTICAL(ValueTypeModel.BOOLEAN),
    VERTICAL_ALIGNMENT(ValueTypeModel.BYTE),
    WIDGET_VERTICAL_ALIGNMENT(ValueTypeModel.BYTE),
    WIDGET_HIDDEN(ValueTypeModel.BOOLEAN),
    LEFT(ValueTypeModel.DOUBLE),
    RIGHT(ValueTypeModel.DOUBLE),
    WIDTH(ValueTypeModel.DOUBLE),
    TOP(ValueTypeModel.DOUBLE),
    BOTTOM(ValueTypeModel.DOUBLE),
    HEIGHT(ValueTypeModel.DOUBLE),
    UNIT(ValueTypeModel.BYTE),
    ANIMATE(ValueTypeModel.INTEGER),
    ANIMATION_DURATION(ValueTypeModel.INTEGER),
    OPEN(ValueTypeModel.NULL),
    CLOSE(ValueTypeModel.NULL),
    BIND(ValueTypeModel.STRING),
    CELL_HEIGHT(ValueTypeModel.STRING),
    CELL_WIDTH(ValueTypeModel.STRING),
    INDEX(ValueTypeModel.INTEGER),
    ADD_COOKIE(ValueTypeModel.STRING),
    REMOVE_COOKIE(ValueTypeModel.STRING),
    COOKIE_EXPIRE(ValueTypeModel.LONG),
    COOKIE_DOMAIN(ValueTypeModel.STRING),
    COOKIE_PATH(ValueTypeModel.STRING_UTF8),
    COOKIE_SECURE(ValueTypeModel.NULL),
    TIME(ValueTypeModel.LONG),
    ANIMATION(ValueTypeModel.BOOLEAN),
    CURSOR_POSITION(ValueTypeModel.INTEGER),
    SELECTION_RANGE_START(ValueTypeModel.INTEGER),
    SELECTION_RANGE_LENGTH(ValueTypeModel.INTEGER),
    MAX_LENGTH(ValueTypeModel.INTEGER),
    VISIBLE_LENGTH(ValueTypeModel.INTEGER),
    REGEX_FILTER(ValueTypeModel.STRING_UTF8),
    MASK(ValueTypeModel.STRING_UTF8),
    VISIBILITY(ValueTypeModel.BOOLEAN),
    REPLACEMENT_STRING(ValueTypeModel.STRING_UTF8),
    DATE_FORMAT_PATTERN(ValueTypeModel.STRING_UTF8),
    KEEP_DAY_TIME_NEEDED(ValueTypeModel.NULL),
    TAG(ValueTypeModel.STRING),
    INNER_HTML(ValueTypeModel.STRING_UTF8),
    INNER_TEXT(ValueTypeModel.STRING_UTF8),
    PICKER(ValueTypeModel.INTEGER),
    ROW(ValueTypeModel.INTEGER),
    ROW_FORMATTER_ADD_STYLE_NAME(ValueTypeModel.STRING),
    ROW_FORMATTER_REMOVE_STYLE_NAME(ValueTypeModel.STRING),
    ROW_FORMATTER_SET_STYLE_NAME(ValueTypeModel.STRING),
    COLUMN(ValueTypeModel.INTEGER),
    CELL_FORMATTER_ADD_STYLE_NAME(ValueTypeModel.STRING),
    CELL_FORMATTER_REMOVE_STYLE_NAME(ValueTypeModel.STRING),
    CELL_FORMATTER_SET_STYLE_NAME(ValueTypeModel.STRING),
    COLUMN_FORMATTER_COLUMN_WIDTH(ValueTypeModel.STRING),
    COLUMN_FORMATTER_ADD_STYLE_NAME(ValueTypeModel.STRING),
    COLUMN_FORMATTER_REMOVE_STYLE_NAME(ValueTypeModel.STRING),
    COLUMN_FORMATTER_SET_STYLE_NAME(ValueTypeModel.STRING),
    PLACEHOLDER(ValueTypeModel.STRING_UTF8),
    DATE_ENABLED(ValueTypeModel.STRING_UTF8),
    ENABLED(ValueTypeModel.BOOLEAN),
    ADD_DATE_STYLE(ValueTypeModel.STRING_UTF8),
    STYLE_NAME(ValueTypeModel.STRING),
    REMOVE_DATE_STYLE(ValueTypeModel.STRING_UTF8),
    YEAR_ARROWS_VISIBLE(ValueTypeModel.BOOLEAN),
    POPUP_CAPTION(ValueTypeModel.STRING_UTF8),
    WIDGET_VISIBLE(ValueTypeModel.BOOLEAN),
    IMAGE_WIDTH(ValueTypeModel.INTEGER),
    IMAGE_HEIGHT(ValueTypeModel.INTEGER),
    WIDGET_WIDTH(ValueTypeModel.STRING),
    WIDGET_HEIGHT(ValueTypeModel.STRING),
    WIDGET_TITLE(ValueTypeModel.STRING_UTF8),
    STYLE_PRIMARY_NAME(ValueTypeModel.STRING),
    ENSURE_DEBUG_ID(ValueTypeModel.STRING),
    ITEM_INSERTED(ValueTypeModel.STRING_UTF8),
    ITEM_UPDATED(ValueTypeModel.STRING_UTF8),
    ITEM_REMOVED(ValueTypeModel.INTEGER),
    SELECTED(ValueTypeModel.BOOLEAN),
    SELECTED_INDEX(ValueTypeModel.INTEGER),
    MULTISELECT(ValueTypeModel.BOOLEAN),
    VISIBLE_ITEM_COUNT(ValueTypeModel.INTEGER),
    ITEM_ADD(ValueTypeModel.STRING_UTF8),
    ITEM_GROUP(ValueTypeModel.STRING_UTF8),
    POPUP_AUTO_HIDE(ValueTypeModel.BOOLEAN),
    MODAL(ValueTypeModel.BOOLEAN),
    POPUP_GLASS_ENABLED(ValueTypeModel.BOOLEAN),
    DRAGGABLE(ValueTypeModel.BOOLEAN),
    CENTER(ValueTypeModel.NULL),
    POSITION_LEFT(ValueTypeModel.INTEGER),
    POSITION_TOP(ValueTypeModel.INTEGER),
    FIXDELAY(ValueTypeModel.LONG),
    MIN_SIZE(ValueTypeModel.INTEGER),
    SNAP_CLOSED_SIZE(ValueTypeModel.INTEGER),
    TOGGLE_DISPLAY_ALLOWED(ValueTypeModel.BOOLEAN),
    IMAGE_URL(ValueTypeModel.STRING_UTF8),
    INSERT_HORIZONTAL_RULE(ValueTypeModel.NULL),
    INSERT_HTML(ValueTypeModel.STRING_UTF8),
    ORDERED(ValueTypeModel.NULL),
    UNORDERED(ValueTypeModel.NULL),
    BACK_COLOR(ValueTypeModel.STRING),
    FONT_NAME(ValueTypeModel.STRING),
    FONT_SIZE(ValueTypeModel.BYTE),
    FONT_COLOR(ValueTypeModel.STRING),
    JUSTIFICATION(ValueTypeModel.BYTE),
    TOGGLE_BOLD(ValueTypeModel.NULL),
    TOGGLE_ITALIC(ValueTypeModel.NULL),
    TOGGLE_SUBSCRIPT(ValueTypeModel.NULL),
    TOGGLE_UNDERLINE(ValueTypeModel.NULL),
    LEFT_INDENT(ValueTypeModel.NULL),
    REDO(ValueTypeModel.NULL),
    REMOVE_FORMAT(ValueTypeModel.NULL),
    REMOVE_LINK(ValueTypeModel.NULL),
    TOGGLE_RIGHT_INDENT(ValueTypeModel.NULL),
    SELECT_ALL(ValueTypeModel.NULL),
    COMMAND_ID(ValueTypeModel.LONG),
    ORACLE(ValueTypeModel.INTEGER),
    STREAM_REQUEST_ID(ValueTypeModel.INTEGER),
    TREE_ROOT(ValueTypeModel.INTEGER),
    FACTORY(ValueTypeModel.STRING),
    DIRECTION(ValueTypeModel.BYTE),
    SIZE(ValueTypeModel.DOUBLE),
    WIDGET_SIZE(ValueTypeModel.DOUBLE),
    RESIZE(ValueTypeModel.NULL),
    BEFORE_INDEX(ValueTypeModel.INTEGER),
    TAB_WIDGET(ValueTypeModel.INTEGER),
    TAB_TEXT(ValueTypeModel.STRING_UTF8),
    ENABLED_ON_REQUEST(ValueTypeModel.BOOLEAN),
    TABINDEX(ValueTypeModel.INTEGER),
    END_OF_PROCESSING(ValueTypeModel.NULL),
    FOCUS(ValueTypeModel.BOOLEAN),
    HANDLER_TYPE(ValueTypeModel.BYTE),
    DOM_HANDLER_CODE(ValueTypeModel.BYTE),
    HISTORY_FIRE_EVENTS(ValueTypeModel.BOOLEAN),
    LOADING_ON_REQUEST(ValueTypeModel.BOOLEAN),
    SET_COL_SPAN(ValueTypeModel.INTEGER),
    SET_ROW_SPAN(ValueTypeModel.INTEGER),
    KEY_FILTER(ValueTypeModel.JSON_OBJECT),
    POPUP_POSITION_AND_SHOW(ValueTypeModel.NULL),
    ROOT_ID(ValueTypeModel.STRING),
    WINDOW_ID(ValueTypeModel.INTEGER),
    FRAME_ID(ValueTypeModel.INTEGER),
    EVAL(ValueTypeModel.STRING_UTF8),
    VISIBLE_LINES(ValueTypeModel.INTEGER),
    CHARACTER_WIDTH(ValueTypeModel.INTEGER),
    HORIZONTAL_SCROLL_POSITION(ValueTypeModel.INTEGER),
    VERTICAL_SCROLL_POSITION(ValueTypeModel.INTEGER),
    SCROLL_TO(ValueTypeModel.INTEGER),
    LIMIT(ValueTypeModel.INTEGER),
    SUGGESTION(ValueTypeModel.STRING_UTF8),
    FEATURES(ValueTypeModel.STRING_UTF8),
    RELATIVE(ValueTypeModel.BOOLEAN),
    URL(ValueTypeModel.STRING_UTF8),
    WIDGET_TYPE(ValueTypeModel.BYTE),
    PREVENT_EVENT(ValueTypeModel.INTEGER),
    STOP_EVENT(ValueTypeModel.INTEGER),
    BORDER_WIDTH(ValueTypeModel.INTEGER),
    SPACING(ValueTypeModel.INTEGER),
    NAME(ValueTypeModel.STRING_UTF8),
    WIDGET_FULL_SIZE(ValueTypeModel.NULL),
    WINDOW_TITLE(ValueTypeModel.STRING_UTF8),
    WINDOW_LOCATION_REPLACE(ValueTypeModel.STRING_UTF8),
    RESIZE_BY_X(ValueTypeModel.DOUBLE),
    RESIZE_BY_Y(ValueTypeModel.DOUBLE),
    RESIZE_TO_WIDTH(ValueTypeModel.INTEGER),
    RESIZE_TO_HEIGHT(ValueTypeModel.INTEGER),
    MOVE_BY_X(ValueTypeModel.DOUBLE),
    MOVE_BY_Y(ValueTypeModel.DOUBLE),
    MOVE_TO_X(ValueTypeModel.DOUBLE),
    MOVE_TO_Y(ValueTypeModel.DOUBLE),
    PRINT(ValueTypeModel.NULL),
    PADDON_CREATION(ValueTypeModel.JSON_OBJECT),
    PADDON_METHOD(ValueTypeModel.STRING),
    PADDON_ARGUMENTS(ValueTypeModel.JSON_OBJECT),
    DESTROY(ValueTypeModel.NULL),

    // Old, useless or not used
    POPUP_GLASS_STYLE_NAME(ValueTypeModel.INTEGER),
    DISCLOSURE_PANEL_OPEN_IMG(ValueTypeModel.INTEGER),
    DISCLOSURE_PANEL_CLOSE_IMG(ValueTypeModel.INTEGER),
    SUGGESTIONS(ValueTypeModel.STRING_UTF8),
    DEFAULT_SUGGESTIONS(ValueTypeModel.STRING_UTF8),
    TEXTBOX_ID(ValueTypeModel.INTEGER);

    private static final ServerToClientModel[] VALUES = ServerToClientModel.values();

    private final ValueTypeModel type;

    private ServerToClientModel(final ValueTypeModel size) {
        this.type = size;
    }

    public final short getValue() {
        return (short) ordinal();
    }

    public final ValueTypeModel getTypeModel() {
        return type;
    }

    public static ServerToClientModel fromRawValue(final short rawValue) {
        return VALUES[rawValue];
    }

}
