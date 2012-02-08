/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

import com.google.gwt.user.client.Window;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.ui.PTAnchor;
import com.ponysdk.ui.terminal.ui.PTButton;
import com.ponysdk.ui.terminal.ui.PTCheckBox;
import com.ponysdk.ui.terminal.ui.PTComposite;
import com.ponysdk.ui.terminal.ui.PTDateBox;
import com.ponysdk.ui.terminal.ui.PTDecoratedPopupPanel;
import com.ponysdk.ui.terminal.ui.PTDialogBox;
import com.ponysdk.ui.terminal.ui.PTDockLayoutPanel;
import com.ponysdk.ui.terminal.ui.PTFileUpload;
import com.ponysdk.ui.terminal.ui.PTFlexTable;
import com.ponysdk.ui.terminal.ui.PTFlowPanel;
import com.ponysdk.ui.terminal.ui.PTHTML;
import com.ponysdk.ui.terminal.ui.PTHorizontalPanel;
import com.ponysdk.ui.terminal.ui.PTImage;
import com.ponysdk.ui.terminal.ui.PTLabel;
import com.ponysdk.ui.terminal.ui.PTListBox;
import com.ponysdk.ui.terminal.ui.PTMenuBar;
import com.ponysdk.ui.terminal.ui.PTMenuItem;
import com.ponysdk.ui.terminal.ui.PTMenuItemSeparator;
import com.ponysdk.ui.terminal.ui.PTObject;
import com.ponysdk.ui.terminal.ui.PTPasswordTextBox;
import com.ponysdk.ui.terminal.ui.PTPopupPanel;
import com.ponysdk.ui.terminal.ui.PTPushButton;
import com.ponysdk.ui.terminal.ui.PTRadioButton;
import com.ponysdk.ui.terminal.ui.PTRichTextArea;
import com.ponysdk.ui.terminal.ui.PTScrollPanel;
import com.ponysdk.ui.terminal.ui.PTSimpleLayoutPanel;
import com.ponysdk.ui.terminal.ui.PTSimplePanel;
import com.ponysdk.ui.terminal.ui.PTSplitLayoutPanel;
import com.ponysdk.ui.terminal.ui.PTStackLayoutPanel;
import com.ponysdk.ui.terminal.ui.PTSuggestBox;
import com.ponysdk.ui.terminal.ui.PTTabLayoutPanel;
import com.ponysdk.ui.terminal.ui.PTTextArea;
import com.ponysdk.ui.terminal.ui.PTTextBox;
import com.ponysdk.ui.terminal.ui.PTTimer;
import com.ponysdk.ui.terminal.ui.PTTree;
import com.ponysdk.ui.terminal.ui.PTTreeItem;
import com.ponysdk.ui.terminal.ui.PTVerticalPanel;

public class UIFactory {

    public PTObject newUIObject(final UIService uiService, final Create create) {
        final WidgetType widgetType = create.getWidgetType();
        switch (widgetType) {
            case BUTTON:
                return new PTButton();
            case ANCHOR:
                return new PTAnchor();
            case LAYOUT_PANEL:
                return new PTButton();
            case TABLAYOUTPANEL:
                return new PTTabLayoutPanel();
            case VERTICAL_PANEL:
                return new PTVerticalPanel();
            case HORIZONTAL_PANEL:
                return new PTHorizontalPanel();
            case DOCK_LAYOUT_PANEL:
                return new PTDockLayoutPanel();
            case SPLIT_LAYOUT_PANEL:
                return new PTSplitLayoutPanel();
            case STACKLAYOUT_PANEL:
                return new PTStackLayoutPanel();
            case LABEL:
                return new PTLabel();
            case HTML:
                return new PTHTML();
            case TEXTBOX:
                return new PTTextBox();
            case PASSWORD_TEXTBOX:
                return new PTPasswordTextBox();
            case TEXT_AREA:
                return new PTTextArea();
            case CHECKBOX:
                return new PTCheckBox();
            case RADIO_BUTTON:
                return new PTRadioButton();
            case PUSH_BUTTON:
                return new PTPushButton();
            case LISTBOX:
                return new PTListBox();
            case SIMPLE_LAYOUT_PANEL:
                return new PTSimpleLayoutPanel();
            case SIMPLE_PANEL:
                return new PTSimplePanel();
            case SCROLL_PANEL:
                return new PTScrollPanel();
            case DATEBOX:
                return new PTDateBox();
            case FLEX_TABLE:
                return new PTFlexTable();
            case IMAGE:
                return new PTImage();
            case FILE_UPLOAD:
                return new PTFileUpload();
            case TREE:
                return new PTTree();
            case TREE_ITEM:
                return new PTTreeItem();
            case MENU_BAR:
                return new PTMenuBar();
            case MENU_ITEM:
                return new PTMenuItem();
            case MENU_ITEM_SEPARATOR:
                return new PTMenuItemSeparator();
            case POPUP_PANEL:
                return new PTPopupPanel();
            case DECORATED_POPUP_PANEL:
                return new PTDecoratedPopupPanel();
            case TIMER:
                return new PTTimer();
            case COMPOSITE:
                return new PTComposite();
            case RICH_TEXT_AREA:
                return new PTRichTextArea();
            case DIALOG_BOX:
                return new PTDialogBox();
            case FLOW_PANEL:
                return new PTFlowPanel();
            case SUGGESTBOX:
                return new PTSuggestBox();
            default:
                Window.alert("UIFactory: Client implementation not found, type : " + create.getWidgetType());
                return null;
        }
    }
}
