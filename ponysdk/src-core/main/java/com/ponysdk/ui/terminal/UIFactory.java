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

import com.google.gwt.user.client.Window;
import com.ponysdk.ui.terminal.Dictionnary.WIDGETTYPE;
import com.ponysdk.ui.terminal.addon.attachedpopuppanel.PTAttachedPopupPanel;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.ui.PTAbsolutePanel;
import com.ponysdk.ui.terminal.ui.PTAnchor;
import com.ponysdk.ui.terminal.ui.PTButton;
import com.ponysdk.ui.terminal.ui.PTCheckBox;
import com.ponysdk.ui.terminal.ui.PTDateBox;
import com.ponysdk.ui.terminal.ui.PTDecoratedPopupPanel;
import com.ponysdk.ui.terminal.ui.PTDecoratorPanel;
import com.ponysdk.ui.terminal.ui.PTDialogBox;
import com.ponysdk.ui.terminal.ui.PTDisclosurePanel;
import com.ponysdk.ui.terminal.ui.PTDockLayoutPanel;
import com.ponysdk.ui.terminal.ui.PTElement;
import com.ponysdk.ui.terminal.ui.PTFileUpload;
import com.ponysdk.ui.terminal.ui.PTFlexTable;
import com.ponysdk.ui.terminal.ui.PTFlowPanel;
import com.ponysdk.ui.terminal.ui.PTFocusPanel;
import com.ponysdk.ui.terminal.ui.PTGrid;
import com.ponysdk.ui.terminal.ui.PTHTML;
import com.ponysdk.ui.terminal.ui.PTHorizontalPanel;
import com.ponysdk.ui.terminal.ui.PTImage;
import com.ponysdk.ui.terminal.ui.PTLabel;
import com.ponysdk.ui.terminal.ui.PTLayoutPanel;
import com.ponysdk.ui.terminal.ui.PTListBox;
import com.ponysdk.ui.terminal.ui.PTMenuBar;
import com.ponysdk.ui.terminal.ui.PTMenuItem;
import com.ponysdk.ui.terminal.ui.PTMenuItemSeparator;
import com.ponysdk.ui.terminal.ui.PTObject;
import com.ponysdk.ui.terminal.ui.PTPasswordTextBox;
import com.ponysdk.ui.terminal.ui.PTPopupPanel;
import com.ponysdk.ui.terminal.ui.PTPushButton;
import com.ponysdk.ui.terminal.ui.PTPusher;
import com.ponysdk.ui.terminal.ui.PTRadioButton;
import com.ponysdk.ui.terminal.ui.PTRichTextArea;
import com.ponysdk.ui.terminal.ui.PTRootLayoutPanel;
import com.ponysdk.ui.terminal.ui.PTRootPanel;
import com.ponysdk.ui.terminal.ui.PTScheduler;
import com.ponysdk.ui.terminal.ui.PTScript;
import com.ponysdk.ui.terminal.ui.PTScrollPanel;
import com.ponysdk.ui.terminal.ui.PTSimpleLayoutPanel;
import com.ponysdk.ui.terminal.ui.PTSimplePanel;
import com.ponysdk.ui.terminal.ui.PTSplitLayoutPanel;
import com.ponysdk.ui.terminal.ui.PTStackLayoutPanel;
import com.ponysdk.ui.terminal.ui.PTSuggestBox;
import com.ponysdk.ui.terminal.ui.PTTabLayoutPanel;
import com.ponysdk.ui.terminal.ui.PTTabPanel;
import com.ponysdk.ui.terminal.ui.PTTextArea;
import com.ponysdk.ui.terminal.ui.PTTextBox;
import com.ponysdk.ui.terminal.ui.PTTree;
import com.ponysdk.ui.terminal.ui.PTTreeItem;
import com.ponysdk.ui.terminal.ui.PTVerticalPanel;

public class UIFactory {

    public PTObject newUIObject(final UIService uiService, final PTInstruction create) {
        final WidgetType widgetType = WidgetType.values()[create.getInt(WIDGETTYPE.KEY)];

        if (WidgetType.BUTTON.equals(widgetType)) { return new PTButton(); }
        if (WidgetType.ANCHOR.equals(widgetType)) { return new PTAnchor(); }
        if (WidgetType.ROOT_LAYOUT_PANEL.equals(widgetType)) { return new PTRootLayoutPanel(); }
        if (WidgetType.ROOT_PANEL.equals(widgetType)) { return new PTRootPanel(); }
        if (WidgetType.LAYOUT_PANEL.equals(widgetType)) { return new PTLayoutPanel(); }
        if (WidgetType.TAB_LAYOUT_PANEL.equals(widgetType)) { return new PTTabLayoutPanel(); }
        if (WidgetType.ABSOLUTE_PANEL.equals(widgetType)) { return new PTAbsolutePanel(); }
        if (WidgetType.TAB_PANEL.equals(widgetType)) { return new PTTabPanel(); }
        if (WidgetType.VERTICAL_PANEL.equals(widgetType)) { return new PTVerticalPanel(); }
        if (WidgetType.HORIZONTAL_PANEL.equals(widgetType)) { return new PTHorizontalPanel(); }
        if (WidgetType.DOCK_LAYOUT_PANEL.equals(widgetType)) { return new PTDockLayoutPanel(); }
        if (WidgetType.SPLIT_LAYOUT_PANEL.equals(widgetType)) { return new PTSplitLayoutPanel(); }
        if (WidgetType.STACKLAYOUT_PANEL.equals(widgetType)) { return new PTStackLayoutPanel(); }
        if (WidgetType.LABEL.equals(widgetType)) { return new PTLabel(); }
        if (WidgetType.HTML.equals(widgetType)) { return new PTHTML(); }
        if (WidgetType.TEXTBOX.equals(widgetType)) { return new PTTextBox(); }
        if (WidgetType.PASSWORD_TEXTBOX.equals(widgetType)) { return new PTPasswordTextBox(); }
        if (WidgetType.TEXT_AREA.equals(widgetType)) { return new PTTextArea(); }
        if (WidgetType.CHECKBOX.equals(widgetType)) { return new PTCheckBox(); }
        if (WidgetType.RADIO_BUTTON.equals(widgetType)) { return new PTRadioButton(); }
        if (WidgetType.PUSH_BUTTON.equals(widgetType)) { return new PTPushButton(); }
        if (WidgetType.LISTBOX.equals(widgetType)) { return new PTListBox(); }
        if (WidgetType.SIMPLE_LAYOUT_PANEL.equals(widgetType)) { return new PTSimpleLayoutPanel(); }
        if (WidgetType.SIMPLE_PANEL.equals(widgetType)) { return new PTSimplePanel(); }
        if (WidgetType.FOCUS_PANEL.equals(widgetType)) { return new PTFocusPanel(); }
        if (WidgetType.SCROLL_PANEL.equals(widgetType)) { return new PTScrollPanel(); }
        if (WidgetType.DATEBOX.equals(widgetType)) { return new PTDateBox(); }
        if (WidgetType.FLEX_TABLE.equals(widgetType)) { return new PTFlexTable(); }
        if (WidgetType.GRID.equals(widgetType)) { return new PTGrid(); }
        if (WidgetType.IMAGE.equals(widgetType)) { return new PTImage(); }
        if (WidgetType.FILE_UPLOAD.equals(widgetType)) { return new PTFileUpload(); }
        if (WidgetType.TREE.equals(widgetType)) { return new PTTree(); }
        if (WidgetType.TREE_ITEM.equals(widgetType)) { return new PTTreeItem(); }
        if (WidgetType.MENU_BAR.equals(widgetType)) { return new PTMenuBar(); }
        if (WidgetType.MENU_ITEM.equals(widgetType)) { return new PTMenuItem(); }
        if (WidgetType.MENU_ITEM_SEPARATOR.equals(widgetType)) { return new PTMenuItemSeparator(); }
        if (WidgetType.POPUP_PANEL.equals(widgetType)) { return new PTPopupPanel(); }
        if (WidgetType.DECORATED_POPUP_PANEL.equals(widgetType)) { return new PTDecoratedPopupPanel(); }
        if (WidgetType.ATTACHED_POPUP_PABEL.equals(widgetType)) { return new PTAttachedPopupPanel(); }
        if (WidgetType.SCHEDULER.equals(widgetType)) { return new PTScheduler(); }
        if (WidgetType.RICH_TEXT_AREA.equals(widgetType)) { return new PTRichTextArea(); }
        if (WidgetType.DIALOG_BOX.equals(widgetType)) { return new PTDialogBox(); }
        if (WidgetType.FLOW_PANEL.equals(widgetType)) { return new PTFlowPanel(); }
        if (WidgetType.SUGGESTBOX.equals(widgetType)) { return new PTSuggestBox(); }
        if (WidgetType.DISCLOSURE_PANEL.equals(widgetType)) { return new PTDisclosurePanel(); }
        if (WidgetType.DECORATOR_PANEL.equals(widgetType)) { return new PTDecoratorPanel(); }
        if (WidgetType.ELEMENT.equals(widgetType)) { return new PTElement(); }
        if (WidgetType.SCRIPT.equals(widgetType)) { return new PTScript(); }
        if (WidgetType.PUSHER.equals(widgetType)) { return new PTPusher(); }
        Window.alert("UIFactory: Client implementation not found, type : " + widgetType);

        return null;
    }
}
