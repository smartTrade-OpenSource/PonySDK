/*
 * Copyright (c) 2018 PonySDK
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

package com.ponysdk.core.terminal;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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

public class UIFactoryTest {

    private final UIFactory uiFactory = new UIFactory();

    @Test
    public void testNewPTAbsolutePanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.ABSOLUTE_PANEL);
        assertTrue(widget instanceof PTAbsolutePanel);
    }

    @Test
    public void testNewPTAddOn() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.ADDON);
        assertTrue(widget instanceof PTAddOn);
    }

    @Test
    public void testNewPTAddOnComposite() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.ADDON_COMPOSITE);
        assertTrue(widget instanceof PTAddOnComposite);
    }

    @Test
    public void testNewPTAnchor() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.ANCHOR);
        assertTrue(widget instanceof PTAnchor);
    }

    @Test
    public void testNewPTButton() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.BUTTON);
        assertTrue(widget instanceof PTButton);
    }

    @Test
    public void testNewPTCheckBox() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.CHECKBOX);
        assertTrue(widget instanceof PTCheckBox);
    }

    @Test
    public void testNewPTDateBox() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.DATEBOX);
        assertTrue(widget instanceof PTDateBox);
    }

    @Test
    public void testNewPTDatePicker() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.DATEPICKER);
        assertTrue(widget instanceof PTDatePicker);
    }

    @Test
    public void testNewPTDecoratorPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.DECORATOR_PANEL);
        assertTrue(widget instanceof PTDecoratorPanel);
    }

    @Test
    public void testNewPTDecoratedPopupPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.DECORATED_POPUP_PANEL);
        assertTrue(widget instanceof PTDecoratedPopupPanel);
    }

    @Test
    public void testNewPTDialogBox() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.DIALOG_BOX);
        assertTrue(widget instanceof PTDialogBox);
    }

    @Test
    public void testNewPTDisclosurePanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.DISCLOSURE_PANEL);
        assertTrue(widget instanceof PTDisclosurePanel);
    }

    @Test
    public void testNewPTDockLayoutPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.DOCK_LAYOUT_PANEL);
        assertTrue(widget instanceof PTDockLayoutPanel);
    }

    @Test
    public void testNewPTElement() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.ELEMENT);
        assertTrue(widget instanceof PTElement);
    }

    @Test
    public void testNewPTFileUpload() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.FILE_UPLOAD);
        assertTrue(widget instanceof PTFileUpload);
    }

    @Test
    public void testNewPTFlexTable() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.FLEX_TABLE);
        assertTrue(widget instanceof PTFlexTable);
    }

    @Test
    public void testNewPTFlowPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.FLOW_PANEL);
        assertTrue(widget instanceof PTFlowPanel);
    }

    @Test
    public void testNewPTFocusPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.FOCUS_PANEL);
        assertTrue(widget instanceof PTFocusPanel);
    }

    @Test
    public void testNewPTGrid() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.GRID);
        assertTrue(widget instanceof PTGrid);
    }

    @Test
    public void testNewPTHeaderPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.HEADER_PANEL);
        assertTrue(widget instanceof PTHeaderPanel);
    }

    @Test
    public void testNewPTHorizontalPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.HORIZONTAL_PANEL);
        assertTrue(widget instanceof PTHorizontalPanel);
    }

    @Test
    public void testNewPTHTML() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.HTML);
        assertTrue(widget instanceof PTHTML);
    }

    @Test
    public void testNewPTImage() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.IMAGE);
        assertTrue(widget instanceof PTImage);
    }

    @Test
    public void testNewPTLabel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.LABEL);
        assertTrue(widget instanceof PTLabel);
    }

    @Test
    public void testNewPTLayoutPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.LAYOUT_PANEL);
        assertTrue(widget instanceof PTLayoutPanel);
    }

    @Test
    public void testNewPTListBox() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.LISTBOX);
        assertTrue(widget instanceof PTListBox);
    }

    @Test
    public void testNewPTMenuBar() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.MENU_BAR);
        assertTrue(widget instanceof PTMenuBar);
    }

    @Test
    public void testNewPTMenuItem() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.MENU_ITEM);
        assertTrue(widget instanceof PTMenuItem);
    }

    @Test
    public void testNewPTMenuItemSeparator() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.MENU_ITEM_SEPARATOR);
        assertTrue(widget instanceof PTMenuItemSeparator);
    }

    @Test
    public void testNewPTMultiWordSuggestOracle() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.MULTIWORD_SUGGEST_ORACLE);
        assertTrue(widget instanceof PTMultiWordSuggestOracle);
    }

    @Test
    public void testNewPTPasswordTextBox() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.PASSWORD_TEXTBOX);
        assertTrue(widget instanceof PTPasswordTextBox);
    }

    @Test
    public void testNewPTPopupPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.POPUP_PANEL);
        assertTrue(widget instanceof PTPopupPanel);
    }

    @Test
    public void testNewPTPushButton() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.PUSH_BUTTON);
        assertTrue(widget instanceof PTPushButton);
    }

    @Test
    public void testNewPTRadioButton() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.RADIO_BUTTON);
        assertTrue(widget instanceof PTRadioButton);
    }

    @Test
    public void testNewPTRichTextArea() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.RICH_TEXT_AREA);
        assertTrue(widget instanceof PTRichTextArea);
    }

    @Test
    public void testNewPTRichTextToolbar() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.RICH_TEXT_TOOLBAR);
        assertTrue(widget instanceof PTRichTextToolbar);
    }

    @Test
    public void testNewPTRootLayoutPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.ROOT_LAYOUT_PANEL);
        assertTrue(widget instanceof PTRootLayoutPanel);
    }

    @Test
    public void testNewPTRootPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.ROOT_PANEL);
        assertTrue(widget instanceof PTRootPanel);
    }

    @Test
    public void testNewPTScript() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.SCRIPT);
        assertTrue(widget instanceof PTScript);
    }

    @Test
    public void testNewPTScrollPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.SCROLL_PANEL);
        assertTrue(widget instanceof PTScrollPanel);
    }

    @Test
    public void testNewPTSimpleLayoutPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.SIMPLE_LAYOUT_PANEL);
        assertTrue(widget instanceof PTSimpleLayoutPanel);
    }

    @Test
    public void testNewPTSimplePanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.SIMPLE_PANEL);
        assertTrue(widget instanceof PTSimplePanel);
    }

    @Test
    public void testNewPTSplitLayoutPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.SPLIT_LAYOUT_PANEL);
        assertTrue(widget instanceof PTSplitLayoutPanel);
    }

    @Test
    public void testNewPTStackLayoutPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.STACKLAYOUT_PANEL);
        assertTrue(widget instanceof PTStackLayoutPanel);
    }

    @Test
    public void testNewPTSuggestBox() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.SUGGESTBOX);
        assertTrue(widget instanceof PTSuggestBox);
    }

    @Test
    public void testNewPTTabLayoutPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.TAB_LAYOUT_PANEL);
        assertTrue(widget instanceof PTTabLayoutPanel);
    }

    @Test
    public void testNewPTTabPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.TAB_PANEL);
        assertTrue(widget instanceof PTTabPanel);
    }

    @Test
    public void testNewPTTextBox() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.TEXTBOX);
        assertTrue(widget instanceof PTTextBox);
    }

    @Test
    public void testNewPTTextArea() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.TEXT_AREA);
        assertTrue(widget instanceof PTTextArea);
    }

    @Test
    public void testNewPTTree() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.TREE);
        assertTrue(widget instanceof PTTree);
    }

    @Test
    public void testNewPTTreeItem() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.TREE_ITEM);
        assertTrue(widget instanceof PTTreeItem);
    }

    @Test
    public void testNewPTVerticalPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.VERTICAL_PANEL);
        assertTrue(widget instanceof PTVerticalPanel);
    }

    @Test
    public void testNewPTWindow() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.WINDOW);
        assertTrue(widget instanceof PTWindow);
    }

    @Test
    public void testNewPTBrowser() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.BROWSER);
        assertTrue(widget instanceof PTBrowser);
    }

    @Test
    public void testNewPTFrame() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.FRAME);
        assertTrue(widget instanceof PTFrame);
    }

    @Test
    public void testNewPTNull() {
        final PTObject widget = uiFactory.newUIObject(null);
        assertNull(widget);
    }

}
