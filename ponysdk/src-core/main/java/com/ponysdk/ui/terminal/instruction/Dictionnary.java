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

package com.ponysdk.ui.terminal.instruction;

public interface Dictionnary {

    public interface APPLICATION {

        public static final String KEY = "0";
        public static final String VIEW_ID = "1";
        public static final String INSTRUCTIONS = "2";

        public static final String START = "0";
    }

    public interface TYPE {

        public static final String KEY = "3";

        public static final String CREATE = "0";
        public static final String ADD = "1";
        public static final String UPDATE = "2";
        public static final String REMOVE = "3";
        public static final String GC = "4";
        public static final String CLOSE = "5";
        public static final String HISTORY = "6";
        public static final String ADD_HANDLER = "7";
        public static final String REMOVE_HANDLER = "8";
        public static final String EVENT = "9";

    }

    public interface HISTORY {

        public static final String TOKEN = "1";
        public static final String FIRE_EVENTS = "2";
    }

    public interface HANDLER {

        public static final String KEY = "4";

        public static final String SELECTION_HANDLER = "0";
        public static final String STRING_SELECTION_HANDLER = "1";
        public static final String STRING_VALUE_CHANGE_HANDLER = "2";
        public static final String BOOLEAN_VALUE_CHANGE_HANDLER = "3";
        public static final String COMMAND = "4";
        public static final String BEFORE_SELECTION_HANDLER = "5";
        public static final String DATE_VALUE_CHANGE_HANDLER = "6";
        public static final String STREAM_REQUEST_HANDLER = "7";
        public static final String EMBEDED_STREAM_REQUEST_HANDLER = "8";
        public static final String CHANGE_HANDLER = "9";
        public static final String TIMER = "a";
        public static final String SCHEDULER = "b";
        public static final String HISTORY = "c";
        public static final String POPUP_POSITION_CALLBACK = "d";
        public static final String CLOSE_HANDLER = "e";
        public static final String DOM_HANDLER = "f";
        public static final String SUBMIT_COMPLETE_HANDLER = "g";
        public static final String OPEN_HANDLER = "h";
    }

    public interface WIDGETTYPE {

        public static final String KEY = "5";

    }

    public interface PROPERTY {

        public static final String ROOT = "-1";
        public static final String WIDTH = "-2";
        public static final String WIDGET_WIDTH = "-3";
        public static final String WIDGET_HEIGHT = "-4";
        public static final String STYLE_PROPERTY = "-5";
        public static final String WIDGET_FONT_SIZE = "-6";
        public static final String TEXT = "6";
        public static final String HTML = "7";
        public static final String ITEM_INSERTED = "8";
        public static final String ITEM_REMOVED = "9";
        public static final String ITEM_TEXT = "a";
        public static final String VALUE = "b";
        public static final String INDEX = "c";
        public static final String BORDER_WIDTH = "d";
        public static final String SPACING = "e";
        public static final String CELL_SPACING = "f";
        public static final String CELL_PADDING = "g";
        public static final String DIRECTION = "h";
        public static final String SIZE = "i";
        public static final String ROW = "j";
        public static final String CELL = "k";
        public static final String CLEAR = "l";
        public static final String CLEAR_ROW = "m";
        public static final String INSERT_ROW = "n";
        public static final String TREE_ITEM_POSITION_PATH = "o";
        public static final String WIDGET_VISIBLE = "p";
        public static final String STYLE_KEY = "q";
        public static final String STYLE_VALUE = "r";
        public static final String STYLE_NAME = "s";
        public static final String ADD_STYLE_NAME = "t";
        public static final String REMOVE_STYLE_NAME = "u";
        public static final String IMAGE_URL = "v";
        public static final String NOTIFICATION_CAPTION = "w";
        public static final String NOTIFICATION_MESSAGE = "x";
        public static final String ENSURE_DEBUG_ID = "y";
        public static final String ENABLED = "z";
        public static final String DATE_FORMAT = "A";
        public static final String DATE_FORMAT_PATTERN = "B";
        public static final String POPUP_GLASS_ENABLED = "C";
        public static final String POPUP_CENTER = "D";
        public static final String POPUP_SHOW = "E";
        public static final String POPUP_HIDE = "F";
        public static final String POPUP_CAPTION = "G";
        public static final String POPUP_DRAGGABLE = "H";
        public static final String POPUP_MODAL = "I";
        public static final String POPUP_GLASS_STYLE_NAME = "J";
        public static final String MENU_ITEM_POSITION_PATH = "K";
        public static final String TAB_WIDGET = "L";
        public static final String WIDGET = "M";
        public static final String TAB_TEXT = "N";
        public static final String ANIMATION = "O";
        public static final String WIDGET_TITLE = "P";
        public static final String MENU_BAR_IS_VERTICAL = "Q";
        public static final String SELECTED_INDEX = "R";
        public static final String SELECTED = "S";
        public static final String STREAM_REQUEST_ID = "T";
        public static final String REPEATING_DELAY = "U";
        public static final String FIXDELAY = "V";
        public static final String IMAGE_STREAM_URL = "W";
        public static final String MULTISELECT = "X";
        public static final String CELL_HORIZONTAL_ALIGNMENT = "Y";
        public static final String CELL_VERTICAL_ALIGNMENT = "Z";
        public static final String HORIZONTAL_ALIGNMENT = "00";
        public static final String VERTICAL_ALIGNMENT = "01";
        public static final String CELL_HEIGHT = "02";
        public static final String CELL_WIDTH = "03";
        public static final String WORD_WRAP = "04";
        public static final String NAME = "05";
        public static final String RELOAD = "06";
        public static final String UNIT = "07";
        public static final String POPUP_POSITION_CALLBACK = "09";
        public static final String FOCUSED = "10";
        public static final String POPUP_POSITION = "11";
        public static final String POPUP_POSITION_LEFT = "12";
        public static final String POPUP_POSITION_TOP = "13";
        public static final String VISIBLE_LINES = "14";
        public static final String VISIBLE_ITEM_COUNT = "15";
        public static final String CHARACTER_WIDTH = "16";
        public static final String HORIZONTAL_SCROLL_POSITION = "17";
        public static final String OFFSETWIDTH = "18";
        public static final String OFFSETHEIGHT = "19";
        public static final String CLIENT_WIDTH = "20";
        public static final String CLIENT_HEIGHT = "21";
        public static final String POPUP_AUTO_HIDE = "22";
        public static final String DOM_HANDLER_CODE = "23";
        public static final String ROW_FORMATTER_STYLE_NAME = "24";
        public static final String ROW_FORMATTER_ADD_STYLE_NAME = "25";
        public static final String HTMLTABLE_ROW_STYLE = "26";
        public static final String ROW_FORMATTER_REMOVE_STYLE_NAME = "27";
        public static final String KEY_FILTER = "28";
        public static final String CLIENT_X = "29";
        public static final String CLIENT_Y = "30";
        public static final String SOURCE_ABSOLUTE_LEFT = "31";
        public static final String SOURCE_ABSOLUTE_TOP = "32";
        public static final String SOURCE_OFFSET_HEIGHT = "33";
        public static final String SOURCE_OFFSET_WIDTH = "34";
        public static final String COOKIE = "35";
        public static final String COOKIE_EXPIRE = "36";
        public static final String REFERENCE_SCROLL_PANEL = "37";
        public static final String IMAGE = "38";
        public static final String BEFORE_INDEX = "39";
        public static final String CORRECT_DIMENSION = "40";
        public static final String DISCLOSURE_PANEL_OPEN_IMG = "41";
        public static final String DISCLOSURE_PANEL_CLOSE_IMG = "42";
        public static final String HTMLTABLE_CELL_STYLE = "43";
        public static final String CELL_FORMATTER_ADD_STYLE_NAME = "44";
        public static final String CELL_FORMATTER_REMOVE_STYLE_NAME = "45";
        public static final String COLUMN = "46";
        public static final String FLEXTABLE_CELL_FORMATTER = "47";
        public static final String COLUMN_FORMATTER_COLUMN_WIDTH = "48";
        public static final String COLUMN_FORMATTER_ADD_STYLE_NAME = "49";
        public static final String COLUMN_FORMATTER_REMOVE_STYLE_NAME = "50";
        public static final String SET_COL_SPAN = "51";
        public static final String SET_ROW_SPAN = "52";
        public static final String FILE_NAME = "53";
        public static final String LOADING = "54";
        public static final String END_OF_PROCESSING = "55";
        public static final String CREATE_LINK = "56";
        public static final String INSERT_HORIZONTAL_RULE = "57";
        public static final String ORDERED = "58";
        public static final String UNORDERED = "59";
        public static final String BACK_COLOR = "60";
        public static final String FONT_NAME = "70";
        public static final String FONT_SIZE = "71";
        public static final String FONT_COLOR = "72";
        public static final String JUSTIFICATION = "73";
        public static final String TOGGLE_BOLD = "74";
        public static final String TOGGLE_ITALIC = "75";
        public static final String TOGGLE_SUBSCRIPT = "76";
        public static final String TOGGLE_UNDERLINE = "77";
        public static final String LEFT_INDENT = "78";
        public static final String REDO = "79";
        public static final String REMOVE_FORMAT = "80";
        public static final String REMOVE_LINK = "81";
        public static final String TOGGLE_RIGHT_INDENT = "82";
        public static final String SELECT_ALL = "83";
        public static final String INSERT_HTML = "84";
        public static final String ENABLED_ON_REQUEST = "85";
        public static final String LOADING_ON_REQUEST = "86";
        public static final String LIMIT = "87";
        public static final String SUGGESTION = "88";
        public static final String DISPLAY_STRING = "89";
        public static final String REPLACEMENT_STRING = "90";
        public static final String STATE = "91";
        public static final String IMAGE_TOP = "92";
        public static final String IMAGE_LEFT = "93";
        public static final String ID = "94";
        public static final String TAG = "95";
        public static final String INNER_HTML = "96";
        public static final String INNER_TEXT = "97";
        public static final String VISIBLE_LENGTH = "98";
        public static final String DOM_HANDLER_TYPE = "99";
        public static final String OPEN = "000";
        public static final String START = "001";
        public static final String COMMAND_ID = "002";
        public static final String STOP = "003";
        public static final String EVAL = "004";
        public static final String RESULT = "005";
        public static final String ERROR_MSG = "006";
        public static final String SCROLL_TO = "007";
        public static final String REPAINT = "008";
        public static final String TEXTBOX_ID = "009";
        public static final String CLEAR_INNER_TEXT = "010";
        public static final String HTMLTABLE_COLUMN_STYLE = "011";
        public static final String ITEM_UPDATED = "012";
        public static final String PLACEHOLDER = "013";
        public static final String FIXRATE = "014";
        public static final String OBJECT_ID = "#";
        public static final String PARENT_ID = "##";

    }

}
