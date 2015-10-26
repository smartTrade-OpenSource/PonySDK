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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.ponysdk.ui.terminal.addon.attachedpopuppanel.PTAttachedPopupPanel;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.ui.PTAbsolutePanel;
import com.ponysdk.ui.terminal.ui.PTAddOn;
import com.ponysdk.ui.terminal.ui.PTAnchor;
import com.ponysdk.ui.terminal.ui.PTButton;
import com.ponysdk.ui.terminal.ui.PTCheckBox;
import com.ponysdk.ui.terminal.ui.PTDateBox;
import com.ponysdk.ui.terminal.ui.PTDatePicker;
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
import com.ponysdk.ui.terminal.ui.PTHeaderPanel;
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
import com.ponysdk.ui.terminal.ui.PTRadioButton;
import com.ponysdk.ui.terminal.ui.PTRichTextArea;
import com.ponysdk.ui.terminal.ui.PTRichTextToolbar;
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
import com.ponysdk.ui.terminal.ui.PTSuggestBox.PTMultiWordSuggestOracle;
import com.ponysdk.ui.terminal.ui.PTTabLayoutPanel;
import com.ponysdk.ui.terminal.ui.PTTabPanel;
import com.ponysdk.ui.terminal.ui.PTTerminalScheduledCommand;
import com.ponysdk.ui.terminal.ui.PTTextArea;
import com.ponysdk.ui.terminal.ui.PTTextBox;
import com.ponysdk.ui.terminal.ui.PTTree;
import com.ponysdk.ui.terminal.ui.PTTreeItem;
import com.ponysdk.ui.terminal.ui.PTVerticalPanel;
import com.ponysdk.ui.terminal.ui.PTWindow;

public class UIFactory {

    public PTObject newUIObject(final UIService uiService, final PTInstruction create) {
        GWT.log("Create : " + create);
        final int widgetType = create.getInt(Model.WIDGET_TYPE);

        if (WidgetType.ELEMENT.ordinal() == widgetType) { return new PTElement(); }
        if (WidgetType.HTML.ordinal() == widgetType) { return new PTHTML(); }
        if (WidgetType.LABEL.ordinal() == widgetType) { return new PTLabel(); }
        if (WidgetType.BUTTON.ordinal() == widgetType) { return new PTButton(); }
        if (WidgetType.ANCHOR.ordinal() == widgetType) { return new PTAnchor(); }
        if (WidgetType.LISTBOX.ordinal() == widgetType) { return new PTListBox(); }
        if (WidgetType.TEXTBOX.ordinal() == widgetType) { return new PTTextBox(); }
        if (WidgetType.PASSWORD_TEXTBOX.ordinal() == widgetType) { return new PTPasswordTextBox(); }
        if (WidgetType.SCRIPT.ordinal() == widgetType) { return new PTScript(); }
        if (WidgetType.FLOW_PANEL.ordinal() == widgetType) { return new PTFlowPanel(); }
        if (WidgetType.ROOT_LAYOUT_PANEL.ordinal() == widgetType) { return new PTRootLayoutPanel(); }
        if (WidgetType.ROOT_PANEL.ordinal() == widgetType) { return new PTRootPanel(); }
        if (WidgetType.LAYOUT_PANEL.ordinal() == widgetType) { return new PTLayoutPanel(); }
        if (WidgetType.TAB_LAYOUT_PANEL.ordinal() == widgetType) { return new PTTabLayoutPanel(); }
        if (WidgetType.ABSOLUTE_PANEL.ordinal() == widgetType) { return new PTAbsolutePanel(); }
        if (WidgetType.TAB_PANEL.ordinal() == widgetType) { return new PTTabPanel(); }
        if (WidgetType.VERTICAL_PANEL.ordinal() == widgetType) { return new PTVerticalPanel(); }
        if (WidgetType.HORIZONTAL_PANEL.ordinal() == widgetType) { return new PTHorizontalPanel(); }
        if (WidgetType.DOCK_LAYOUT_PANEL.ordinal() == widgetType) { return new PTDockLayoutPanel(); }
        if (WidgetType.SPLIT_LAYOUT_PANEL.ordinal() == widgetType) { return new PTSplitLayoutPanel(); }
        if (WidgetType.STACKLAYOUT_PANEL.ordinal() == widgetType) { return new PTStackLayoutPanel(); }
        if (WidgetType.TEXT_AREA.ordinal() == widgetType) { return new PTTextArea(); }
        if (WidgetType.CHECKBOX.ordinal() == widgetType) { return new PTCheckBox(); }
        if (WidgetType.RADIO_BUTTON.ordinal() == widgetType) { return new PTRadioButton(); }
        if (WidgetType.PUSH_BUTTON.ordinal() == widgetType) { return new PTPushButton(); }
        if (WidgetType.SIMPLE_LAYOUT_PANEL.ordinal() == widgetType) { return new PTSimpleLayoutPanel(); }
        if (WidgetType.SIMPLE_PANEL.ordinal() == widgetType) { return new PTSimplePanel(); }
        if (WidgetType.FOCUS_PANEL.ordinal() == widgetType) { return new PTFocusPanel(); }
        if (WidgetType.SCROLL_PANEL.ordinal() == widgetType) { return new PTScrollPanel(); }
        if (WidgetType.TERMINAL_SCHEDULED_COMMAND.ordinal() == widgetType) { return new PTTerminalScheduledCommand(); }
        if (WidgetType.DATEBOX.ordinal() == widgetType) { return new PTDateBox(); }
        if (WidgetType.DATEPICKER.ordinal() == widgetType) { return new PTDatePicker(); }
        if (WidgetType.FLEX_TABLE.ordinal() == widgetType) { return new PTFlexTable(); }
        if (WidgetType.GRID.ordinal() == widgetType) { return new PTGrid(); }
        if (WidgetType.IMAGE.ordinal() == widgetType) { return new PTImage(); }
        if (WidgetType.FILE_UPLOAD.ordinal() == widgetType) { return new PTFileUpload(); }
        if (WidgetType.TREE.ordinal() == widgetType) { return new PTTree(); }
        if (WidgetType.TREE_ITEM.ordinal() == widgetType) { return new PTTreeItem(); }
        if (WidgetType.MENU_BAR.ordinal() == widgetType) { return new PTMenuBar(); }
        if (WidgetType.MENU_ITEM.ordinal() == widgetType) { return new PTMenuItem(); }
        if (WidgetType.MENU_ITEM_SEPARATOR.ordinal() == widgetType) { return new PTMenuItemSeparator(); }
        if (WidgetType.POPUP_PANEL.ordinal() == widgetType) { return new PTPopupPanel(); }
        if (WidgetType.DECORATED_POPUP_PANEL.ordinal() == widgetType) { return new PTDecoratedPopupPanel(); }
        if (WidgetType.ATTACHED_POPUP_PABEL.ordinal() == widgetType) { return new PTAttachedPopupPanel(); }
        if (WidgetType.SCHEDULER.ordinal() == widgetType) { return new PTScheduler(); }
        if (WidgetType.RICH_TEXT_AREA.ordinal() == widgetType) { return new PTRichTextArea(); }
        if (WidgetType.RICH_TEXT_TOOLBAR.ordinal() == widgetType) { return new PTRichTextToolbar(); }
        if (WidgetType.DIALOG_BOX.ordinal() == widgetType) { return new PTDialogBox(); }
        if (WidgetType.SUGGESTBOX.ordinal() == widgetType) { return new PTSuggestBox(); }
        if (WidgetType.MULTIWORD_SUGGEST_ORACLE.ordinal() == widgetType) { return new PTMultiWordSuggestOracle(); }
        if (WidgetType.DISCLOSURE_PANEL.ordinal() == widgetType) { return new PTDisclosurePanel(); }
        if (WidgetType.DECORATOR_PANEL.ordinal() == widgetType) { return new PTDecoratorPanel(); }
        if (WidgetType.WINDOW.ordinal() == widgetType) { return new PTWindow(); }
        if (WidgetType.HEADER_PANEL.ordinal() == widgetType) { return new PTHeaderPanel(); }
        if (WidgetType.ADDON.ordinal() == widgetType) { return new PTAddOn(); }

        Window.alert("UIFactory: Client implementation not found, type : " + widgetType);

        return null;
    }
}
