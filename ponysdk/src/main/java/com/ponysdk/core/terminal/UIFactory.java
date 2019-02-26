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

package com.ponysdk.core.terminal;

import java.util.logging.Logger;

import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.terminal.ui.PTAbsolutePanel;
import com.ponysdk.core.terminal.ui.PTAddOn;
import com.ponysdk.core.terminal.ui.PTAddOnComposite;
import com.ponysdk.core.terminal.ui.PTAnchor;
import com.ponysdk.core.terminal.ui.PTBrowser;
import com.ponysdk.core.terminal.ui.PTButton;
import com.ponysdk.core.terminal.ui.PTCheckBox;
import com.ponysdk.core.terminal.ui.PTDateBox;
import com.ponysdk.core.terminal.ui.PTDatePicker;
import com.ponysdk.core.terminal.ui.PTDecoratedPopupPanel;
import com.ponysdk.core.terminal.ui.PTDecoratorPanel;
import com.ponysdk.core.terminal.ui.PTDialogBox;
import com.ponysdk.core.terminal.ui.PTDisclosurePanel;
import com.ponysdk.core.terminal.ui.PTDockLayoutPanel;
import com.ponysdk.core.terminal.ui.PTElement;
import com.ponysdk.core.terminal.ui.PTFileUpload;
import com.ponysdk.core.terminal.ui.PTFlexTable;
import com.ponysdk.core.terminal.ui.PTFlowPanel;
import com.ponysdk.core.terminal.ui.PTFocusPanel;
import com.ponysdk.core.terminal.ui.PTFrame;
import com.ponysdk.core.terminal.ui.PTFunction;
import com.ponysdk.core.terminal.ui.PTFunctionalLabel;
import com.ponysdk.core.terminal.ui.PTGrid;
import com.ponysdk.core.terminal.ui.PTHTML;
import com.ponysdk.core.terminal.ui.PTHeaderPanel;
import com.ponysdk.core.terminal.ui.PTHorizontalPanel;
import com.ponysdk.core.terminal.ui.PTImage;
import com.ponysdk.core.terminal.ui.PTLabel;
import com.ponysdk.core.terminal.ui.PTLayoutPanel;
import com.ponysdk.core.terminal.ui.PTListBox;
import com.ponysdk.core.terminal.ui.PTMenuBar;
import com.ponysdk.core.terminal.ui.PTMenuItem;
import com.ponysdk.core.terminal.ui.PTMenuItemSeparator;
import com.ponysdk.core.terminal.ui.PTMultiWordSuggestOracle;
import com.ponysdk.core.terminal.ui.PTObject;
import com.ponysdk.core.terminal.ui.PTPasswordTextBox;
import com.ponysdk.core.terminal.ui.PTPopupPanel;
import com.ponysdk.core.terminal.ui.PTPushButton;
import com.ponysdk.core.terminal.ui.PTRadioButton;
import com.ponysdk.core.terminal.ui.PTRichTextArea;
import com.ponysdk.core.terminal.ui.PTRichTextToolbar;
import com.ponysdk.core.terminal.ui.PTRootLayoutPanel;
import com.ponysdk.core.terminal.ui.PTRootPanel;
import com.ponysdk.core.terminal.ui.PTScript;
import com.ponysdk.core.terminal.ui.PTScrollPanel;
import com.ponysdk.core.terminal.ui.PTSimpleLayoutPanel;
import com.ponysdk.core.terminal.ui.PTSimplePanel;
import com.ponysdk.core.terminal.ui.PTSplitLayoutPanel;
import com.ponysdk.core.terminal.ui.PTStackLayoutPanel;
import com.ponysdk.core.terminal.ui.PTSuggestBox;
import com.ponysdk.core.terminal.ui.PTTabLayoutPanel;
import com.ponysdk.core.terminal.ui.PTTabPanel;
import com.ponysdk.core.terminal.ui.PTTextArea;
import com.ponysdk.core.terminal.ui.PTTextBox;
import com.ponysdk.core.terminal.ui.PTTree;
import com.ponysdk.core.terminal.ui.PTTreeItem;
import com.ponysdk.core.terminal.ui.PTVerticalPanel;
import com.ponysdk.core.terminal.ui.PTWindow;

class UIFactory {

    private static final Logger log = Logger.getLogger(UIFactory.class.getName());

    PTObject newUIObject(final WidgetType widgetType) {
        if (WidgetType.FLOW_PANEL == widgetType) return new PTFlowPanel();
        else if (WidgetType.SIMPLE_PANEL == widgetType) return new PTSimplePanel<>();
        else if (WidgetType.LABEL == widgetType) return new PTLabel<>();
        else if (WidgetType.BUTTON == widgetType) return new PTButton();
        else if (WidgetType.CHECKBOX == widgetType) return new PTCheckBox<>();
        else if (WidgetType.DATEBOX == widgetType) return new PTDateBox();
        else if (WidgetType.DATEPICKER == widgetType) return new PTDatePicker();
        else if (WidgetType.ADDON_COMPOSITE == widgetType) return new PTAddOnComposite();
        else if (WidgetType.ADDON == widgetType) return new PTAddOn();
        else if (WidgetType.ELEMENT == widgetType) return new PTElement();
        else if (WidgetType.HTML == widgetType) return new PTHTML();
        else if (WidgetType.IMAGE == widgetType) return new PTImage();
        else if (WidgetType.LISTBOX == widgetType) return new PTListBox();
        else if (WidgetType.DIALOG_BOX == widgetType) return new PTDialogBox();
        else if (WidgetType.PASSWORD_TEXTBOX == widgetType) return new PTPasswordTextBox();
        else if (WidgetType.TEXTBOX == widgetType) return new PTTextBox();
        else if (WidgetType.TEXT_AREA == widgetType) return new PTTextArea();
        else if (WidgetType.ANCHOR == widgetType) return new PTAnchor();
        else if (WidgetType.FILE_UPLOAD == widgetType) return new PTFileUpload();
        else if (WidgetType.FOCUS_PANEL == widgetType) return new PTFocusPanel();
        else if (WidgetType.ABSOLUTE_PANEL == widgetType) return new PTAbsolutePanel();
        else if (WidgetType.FLEX_TABLE == widgetType) return new PTFlexTable();
        else if (WidgetType.GRID == widgetType) return new PTGrid();
        else if (WidgetType.DECORATED_POPUP_PANEL == widgetType) return new PTDecoratedPopupPanel<>();
        else if (WidgetType.DECORATOR_PANEL == widgetType) return new PTDecoratorPanel();
        else if (WidgetType.DISCLOSURE_PANEL == widgetType) return new PTDisclosurePanel();
        else if (WidgetType.DOCK_LAYOUT_PANEL == widgetType) return new PTDockLayoutPanel<>();
        else if (WidgetType.HEADER_PANEL == widgetType) return new PTHeaderPanel();
        else if (WidgetType.WINDOW == widgetType) return new PTWindow();
        else if (WidgetType.ROOT_PANEL == widgetType) return new PTRootPanel();
        else if (WidgetType.MENU_BAR == widgetType) return new PTMenuBar();
        else if (WidgetType.MENU_ITEM == widgetType) return new PTMenuItem();
        else if (WidgetType.MENU_ITEM_SEPARATOR == widgetType) return new PTMenuItemSeparator();
        else if (WidgetType.POPUP_PANEL == widgetType) return new PTPopupPanel<>();
        else if (WidgetType.PUSH_BUTTON == widgetType) return new PTPushButton();
        else if (WidgetType.RADIO_BUTTON == widgetType) return new PTRadioButton();
        else if (WidgetType.SCROLL_PANEL == widgetType) return new PTScrollPanel();
        else if (WidgetType.TAB_PANEL == widgetType) return new PTTabPanel();
        else if (WidgetType.SCRIPT == widgetType) return new PTScript();
        else if (WidgetType.TREE == widgetType) return new PTTree();
        else if (WidgetType.TREE_ITEM == widgetType) return new PTTreeItem();
        else if (WidgetType.BROWSER == widgetType) return new PTBrowser();
        else if (WidgetType.FRAME == widgetType) return new PTFrame();
        else if (WidgetType.HORIZONTAL_PANEL == widgetType) return new PTHorizontalPanel();
        else if (WidgetType.VERTICAL_PANEL == widgetType) return new PTVerticalPanel();
        else if (WidgetType.ROOT_LAYOUT_PANEL == widgetType) return new PTRootLayoutPanel();
        else if (WidgetType.LAYOUT_PANEL == widgetType) return new PTLayoutPanel();
        else if (WidgetType.SIMPLE_LAYOUT_PANEL == widgetType) return new PTSimpleLayoutPanel();
        else if (WidgetType.SPLIT_LAYOUT_PANEL == widgetType) return new PTSplitLayoutPanel();
        else if (WidgetType.STACKLAYOUT_PANEL == widgetType) return new PTStackLayoutPanel();
        else if (WidgetType.TAB_LAYOUT_PANEL == widgetType) return new PTTabLayoutPanel();
        else if (WidgetType.RICH_TEXT_TOOLBAR == widgetType) return new PTRichTextToolbar();
        else if (WidgetType.RICH_TEXT_AREA == widgetType) return new PTRichTextArea();
        else if (WidgetType.SUGGESTBOX == widgetType) return new PTSuggestBox();
        else if (WidgetType.FUNCTIONAL_LABEL == widgetType) return new PTFunctionalLabel();
        else if (WidgetType.FUNCTION == widgetType) return new PTFunction();
        else if (WidgetType.MULTIWORD_SUGGEST_ORACLE == widgetType) return new PTMultiWordSuggestOracle();
        else log.severe("UIFactory: Client implementation not found, type : " + widgetType);

        return null;
    }
}
