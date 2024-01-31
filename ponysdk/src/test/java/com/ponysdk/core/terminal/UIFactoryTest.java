package com.ponysdk.core.terminal;

import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.terminal.ui.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UIFactoryTest {

    private final UIFactory uiFactory = new UIFactory();

    @Test
    public void testNewPTAbsolutePanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.ABSOLUTE_PANEL);
        assertInstanceOf(PTAbsolutePanel.class, widget);
    }

    @Test
    public void testNewPTAddOn() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.ADDON);
        assertInstanceOf(PTAddOn.class, widget);
    }

    @Test
    public void testNewPTAddOnComposite() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.ADDON_COMPOSITE);
        assertInstanceOf(PTAddOnComposite.class, widget);
    }

    @Test
    public void testNewPTAnchor() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.ANCHOR);
        assertInstanceOf(PTAnchor.class, widget);
    }

    @Test
    public void testNewPTButton() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.BUTTON);
        assertInstanceOf(PTButton.class, widget);
    }

    @Test
    public void testNewPTCheckBox() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.CHECKBOX);
        assertInstanceOf(PTCheckBox.class, widget);
    }

    @Test
    public void testNewPTDateBox() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.DATEBOX);
        assertInstanceOf(PTDateBox.class, widget);
    }

    @Test
    public void testNewPTDatePicker() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.DATEPICKER);
        assertInstanceOf(PTDatePicker.class, widget);
    }

    @Test
    public void testNewPTDecoratorPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.DECORATOR_PANEL);
        assertInstanceOf(PTDecoratorPanel.class, widget);
    }

    @Test
    public void testNewPTDecoratedPopupPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.DECORATED_POPUP_PANEL);
        assertInstanceOf(PTDecoratedPopupPanel.class, widget);
    }

    @Test
    public void testNewPTDialogBox() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.DIALOG_BOX);
        assertInstanceOf(PTDialogBox.class, widget);
    }

    @Test
    public void testNewPTDisclosurePanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.DISCLOSURE_PANEL);
        assertInstanceOf(PTDisclosurePanel.class, widget);
    }

    @Test
    public void testNewPTDockLayoutPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.DOCK_LAYOUT_PANEL);
        assertInstanceOf(PTDockLayoutPanel.class, widget);
    }

    @Test
    public void testNewPTElement() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.ELEMENT);
        assertInstanceOf(PTElement.class, widget);
    }

    @Test
    public void testNewPTFileUpload() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.FILE_UPLOAD);
        assertInstanceOf(PTFileUpload.class, widget);
    }

    @Test
    public void testNewPTFlexTable() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.FLEX_TABLE);
        assertInstanceOf(PTFlexTable.class, widget);
    }

    @Test
    public void testNewPTFlowPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.FLOW_PANEL);
        assertInstanceOf(PTFlowPanel.class, widget);
    }

    @Test
    public void testNewPTFocusPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.FOCUS_PANEL);
        assertInstanceOf(PTFocusPanel.class, widget);
    }

    @Test
    public void testNewPTGrid() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.GRID);
        assertInstanceOf(PTGrid.class, widget);
    }

    @Test
    public void testNewPTHeaderPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.HEADER_PANEL);
        assertInstanceOf(PTHeaderPanel.class, widget);
    }

    @Test
    public void testNewPTHorizontalPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.HORIZONTAL_PANEL);
        assertInstanceOf(PTHorizontalPanel.class, widget);
    }

    @Test
    public void testNewPTHTML() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.HTML);
        assertInstanceOf(PTHTML.class, widget);
    }

    @Test
    public void testNewPTImage() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.IMAGE);
        assertInstanceOf(PTImage.class, widget);
    }

    @Test
    public void testNewPTLabel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.LABEL);
        assertInstanceOf(PTLabel.class, widget);
    }

    @Test
    public void testNewPTLayoutPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.LAYOUT_PANEL);
        assertInstanceOf(PTLayoutPanel.class, widget);
    }

    @Test
    public void testNewPTListBox() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.LISTBOX);
        assertInstanceOf(PTListBox.class, widget);
    }

    @Test
    public void testNewPTMenuBar() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.MENU_BAR);
        assertInstanceOf(PTMenuBar.class, widget);
    }

    @Test
    public void testNewPTMenuItem() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.MENU_ITEM);
        assertInstanceOf(PTMenuItem.class, widget);
    }

    @Test
    public void testNewPTMenuItemSeparator() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.MENU_ITEM_SEPARATOR);
        assertInstanceOf(PTMenuItemSeparator.class, widget);
    }

    @Test
    public void testNewPTMultiWordSuggestOracle() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.MULTIWORD_SUGGEST_ORACLE);
        assertInstanceOf(PTMultiWordSuggestOracle.class, widget);
    }

    @Test
    public void testNewPTPasswordTextBox() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.PASSWORD_TEXTBOX);
        assertInstanceOf(PTPasswordTextBox.class, widget);
    }

    @Test
    public void testNewPTPopupPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.POPUP_PANEL);
        assertInstanceOf(PTPopupPanel.class, widget);
    }

    @Test
    public void testNewPTPushButton() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.PUSH_BUTTON);
        assertInstanceOf(PTPushButton.class, widget);
    }

    @Test
    public void testNewPTRadioButton() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.RADIO_BUTTON);
        assertInstanceOf(PTRadioButton.class, widget);
    }

    @Test
    public void testNewPTRichTextArea() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.RICH_TEXT_AREA);
        assertInstanceOf(PTRichTextArea.class, widget);
    }

    @Test
    public void testNewPTRichTextToolbar() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.RICH_TEXT_TOOLBAR);
        assertInstanceOf(PTRichTextToolbar.class, widget);
    }

    @Test
    public void testNewPTRootLayoutPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.ROOT_LAYOUT_PANEL);
        assertInstanceOf(PTRootLayoutPanel.class, widget);
    }

    @Test
    public void testNewPTRootPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.ROOT_PANEL);
        assertInstanceOf(PTRootPanel.class, widget);
    }

    @Test
    public void testNewPTScript() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.SCRIPT);
        assertInstanceOf(PTScript.class, widget);
    }

    @Test
    public void testNewPTScrollPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.SCROLL_PANEL);
        assertInstanceOf(PTScrollPanel.class, widget);
    }

    @Test
    public void testNewPTSimpleLayoutPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.SIMPLE_LAYOUT_PANEL);
        assertInstanceOf(PTSimpleLayoutPanel.class, widget);
    }

    @Test
    public void testNewPTSimplePanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.SIMPLE_PANEL);
        assertInstanceOf(PTSimplePanel.class, widget);
    }

    @Test
    public void testNewPTSplitLayoutPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.SPLIT_LAYOUT_PANEL);
        assertInstanceOf(PTSplitLayoutPanel.class, widget);
    }

    @Test
    public void testNewPTStackLayoutPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.STACKLAYOUT_PANEL);
        assertInstanceOf(PTStackLayoutPanel.class, widget);
    }

    @Test
    public void testNewPTSuggestBox() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.SUGGESTBOX);
        assertInstanceOf(PTSuggestBox.class, widget);
    }

    @Test
    public void testNewPTTabLayoutPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.TAB_LAYOUT_PANEL);
        assertInstanceOf(PTTabLayoutPanel.class, widget);
    }

    @Test
    public void testNewPTTabPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.TAB_PANEL);
        assertInstanceOf(PTTabPanel.class, widget);
    }

    @Test
    public void testNewPTTextBox() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.TEXTBOX);
        assertInstanceOf(PTTextBox.class, widget);
    }

    @Test
    public void testNewPTTextArea() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.TEXT_AREA);
        assertInstanceOf(PTTextArea.class, widget);
    }

    @Test
    public void testNewPTTree() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.TREE);
        assertInstanceOf(PTTree.class, widget);
    }

    @Test
    public void testNewPTTreeItem() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.TREE_ITEM);
        assertInstanceOf(PTTreeItem.class, widget);
    }

    @Test
    public void testNewPTVerticalPanel() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.VERTICAL_PANEL);
        assertInstanceOf(PTVerticalPanel.class, widget);
    }

    @Test
    public void testNewPTWindow() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.WINDOW);
        assertInstanceOf(PTWindow.class, widget);
    }

    @Test
    public void testNewPTBrowser() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.BROWSER);
        assertInstanceOf(PTBrowser.class, widget);
    }

    @Test
    public void testNewPTFrame() {
        final PTObject widget = uiFactory.newUIObject(WidgetType.FRAME);
        assertInstanceOf(PTFrame.class, widget);
    }

    @Test
    public void testNewPTNull() {
        final PTObject widget = uiFactory.newUIObject(null);
        assertNull(widget);
    }

}
