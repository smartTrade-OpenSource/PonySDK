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
        if (WidgetType.ABSOLUTE_PANEL.equals(widgetType)) return new PTAbsolutePanel();
        else if (WidgetType.ADDON.equals(widgetType)) return new PTAddOn();
        else if (WidgetType.ADDON_COMPOSITE.equals(widgetType)) return new PTAddOnComposite();
        else if (WidgetType.ANCHOR.equals(widgetType)) return new PTAnchor();
        else if (WidgetType.BUTTON.equals(widgetType)) return new PTButton();
        else if (WidgetType.CHECKBOX.equals(widgetType)) return new PTCheckBox<>();
        else if (WidgetType.DATEBOX.equals(widgetType)) return new PTDateBox();
        else if (WidgetType.DATEPICKER.equals(widgetType)) return new PTDatePicker();
        else if (WidgetType.DECORATED_POPUP_PANEL.equals(widgetType)) return new PTDecoratedPopupPanel<>();
        else if (WidgetType.DECORATOR_PANEL.equals(widgetType)) return new PTDecoratorPanel();
        else if (WidgetType.DIALOG_BOX.equals(widgetType)) return new PTDialogBox();
        else if (WidgetType.DISCLOSURE_PANEL.equals(widgetType)) return new PTDisclosurePanel();
        else if (WidgetType.DOCK_LAYOUT_PANEL.equals(widgetType)) return new PTDockLayoutPanel<>();
        else if (WidgetType.ELEMENT.equals(widgetType)) return new PTElement();
        else if (WidgetType.FILE_UPLOAD.equals(widgetType)) return new PTFileUpload();
        else if (WidgetType.FLEX_TABLE.equals(widgetType)) return new PTFlexTable();
        else if (WidgetType.FLOW_PANEL.equals(widgetType)) return new PTFlowPanel();
        else if (WidgetType.FOCUS_PANEL.equals(widgetType)) return new PTFocusPanel();
        else if (WidgetType.GRID.equals(widgetType)) return new PTGrid();
        else if (WidgetType.HEADER_PANEL.equals(widgetType)) return new PTHeaderPanel();
        else if (WidgetType.HORIZONTAL_PANEL.equals(widgetType)) return new PTHorizontalPanel();
        else if (WidgetType.HTML.equals(widgetType)) return new PTHTML();
        else if (WidgetType.IMAGE.equals(widgetType)) return new PTImage();
        else if (WidgetType.LABEL.equals(widgetType)) return new PTLabel<>();
        else if (WidgetType.LAYOUT_PANEL.equals(widgetType)) return new PTLayoutPanel();
        else if (WidgetType.LISTBOX.equals(widgetType)) return new PTListBox();
        else if (WidgetType.MENU_BAR.equals(widgetType)) return new PTMenuBar();
        else if (WidgetType.MENU_ITEM.equals(widgetType)) return new PTMenuItem();
        else if (WidgetType.MENU_ITEM_SEPARATOR.equals(widgetType)) return new PTMenuItemSeparator();
        else if (WidgetType.MULTIWORD_SUGGEST_ORACLE.equals(widgetType)) return new PTMultiWordSuggestOracle();
        else if (WidgetType.PASSWORD_TEXTBOX.equals(widgetType)) return new PTPasswordTextBox();
        else if (WidgetType.POPUP_PANEL.equals(widgetType)) return new PTPopupPanel<>();
        else if (WidgetType.PUSH_BUTTON.equals(widgetType)) return new PTPushButton();
        else if (WidgetType.RADIO_BUTTON.equals(widgetType)) return new PTRadioButton();
        else if (WidgetType.RICH_TEXT_AREA.equals(widgetType)) return new PTRichTextArea();
        else if (WidgetType.RICH_TEXT_TOOLBAR.equals(widgetType)) return new PTRichTextToolbar();
        else if (WidgetType.ROOT_LAYOUT_PANEL.equals(widgetType)) return new PTRootLayoutPanel();
        else if (WidgetType.ROOT_PANEL.equals(widgetType)) return new PTRootPanel();
        else if (WidgetType.SCRIPT.equals(widgetType)) return new PTScript();
        else if (WidgetType.SCROLL_PANEL.equals(widgetType)) return new PTScrollPanel();
        else if (WidgetType.SIMPLE_LAYOUT_PANEL.equals(widgetType)) return new PTSimpleLayoutPanel();
        else if (WidgetType.SIMPLE_PANEL.equals(widgetType)) return new PTSimplePanel<>();
        else if (WidgetType.SPLIT_LAYOUT_PANEL.equals(widgetType)) return new PTSplitLayoutPanel();
        else if (WidgetType.STACKLAYOUT_PANEL.equals(widgetType)) return new PTStackLayoutPanel();
        else if (WidgetType.SUGGESTBOX.equals(widgetType)) return new PTSuggestBox();
        else if (WidgetType.TAB_LAYOUT_PANEL.equals(widgetType)) return new PTTabLayoutPanel();
        else if (WidgetType.TAB_PANEL.equals(widgetType)) return new PTTabPanel();
        else if (WidgetType.TEXTBOX.equals(widgetType)) return new PTTextBox();
        else if (WidgetType.TEXT_AREA.equals(widgetType)) return new PTTextArea();
        else if (WidgetType.TREE.equals(widgetType)) return new PTTree();
        else if (WidgetType.TREE_ITEM.equals(widgetType)) return new PTTreeItem();
        else if (WidgetType.VERTICAL_PANEL.equals(widgetType)) return new PTVerticalPanel();
        else if (WidgetType.WINDOW.equals(widgetType)) return new PTWindow();
        else if (WidgetType.BROWSER.equals(widgetType)) return new PTBrowser();
        else if (WidgetType.FRAME.equals(widgetType)) return new PTFrame();
        else log.severe("UIFactory: Client implementation not found, type : " + widgetType);

        return null;
    }
}
