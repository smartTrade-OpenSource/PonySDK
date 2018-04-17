
package com.ponysdk.core.ui.basic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ponysdk.core.model.WidgetType;

public class DefaultElementFactoryTest extends PSuite {

    private final DefaultElementFactory element = new DefaultElementFactory();

    @Test
    public void testNewPAbsolutePanel() {
        final PObject widget = element.newPAbsolutePanel();
        assertEquals(WidgetType.ABSOLUTE_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPAnchor() {
        final PObject widget = element.newPAnchor();
        assertEquals(WidgetType.ANCHOR, widget.getWidgetType());
    }

    @Test
    public void testNewPButton() {
        final PObject widget = element.newPButton();
        assertEquals(WidgetType.BUTTON, widget.getWidgetType());
    }

    @Test
    public void testNewPCheckBox() {
        final PObject widget = element.newPCheckBox();
        assertEquals(WidgetType.CHECKBOX, widget.getWidgetType());
    }

    @Test
    public void testNewPDateBox() {
        final PObject widget = element.newPDateBox();
        assertEquals(WidgetType.DATEBOX, widget.getWidgetType());
    }

    @Test
    public void testNewPDatePicker() {
        final PObject widget = element.newPDatePicker();
        assertEquals(WidgetType.DATEPICKER, widget.getWidgetType());
    }

    @Test
    public void testNewPDecoratorPanel() {
        final PObject widget = element.newPDecoratorPanel();
        assertEquals(WidgetType.DECORATOR_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPDecoratedPopupPanel() {
        final PObject widget = element.newPDecoratedPopupPanel(false);
        assertEquals(WidgetType.DECORATED_POPUP_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPDialogBox() {
        final PObject widget = element.newPDialogBox();
        assertEquals(WidgetType.DIALOG_BOX, widget.getWidgetType());
    }

    @Test
    public void testNewPDisclosurePanel() {
        final PObject widget = element.newPDisclosurePanel(null);
        assertEquals(WidgetType.DISCLOSURE_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPDockLayoutPanel() {
        final PObject widget = element.newPDockLayoutPanel(null);
        assertEquals(WidgetType.DOCK_LAYOUT_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPElement() {
        final PObject widget = element.newPElement(null);
        assertEquals(WidgetType.ELEMENT, widget.getWidgetType());
    }

    @Test
    public void testNewPFileUpload() {
        final PObject widget = element.newPFileUpload();
        assertEquals(WidgetType.FILE_UPLOAD, widget.getWidgetType());
    }

    @Test
    public void testNewPFlexTable() {
        final PObject widget = element.newPFlexTable();
        assertEquals(WidgetType.FLEX_TABLE, widget.getWidgetType());
    }

    @Test
    public void testNewPFlowPanel() {
        final PObject widget = element.newPFlowPanel();
        assertEquals(WidgetType.FLOW_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPFocusPanel() {
        final PObject widget = element.newPFocusPanel();
        assertEquals(WidgetType.FOCUS_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPGrid() {
        final PObject widget = element.newPGrid();
        assertEquals(WidgetType.GRID, widget.getWidgetType());
    }

    @Test
    public void testNewPHeaderPanel() {
        final PObject widget = element.newPHeaderPanel();
        assertEquals(WidgetType.HEADER_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPHorizontalPanel() {
        final PObject widget = element.newPHorizontalPanel();
        assertEquals(WidgetType.HORIZONTAL_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPHTML() {
        final PObject widget = element.newPHTML();
        assertEquals(WidgetType.HTML, widget.getWidgetType());
    }

    @Test
    public void testNewPImage() {
        final PObject widget = element.newPImage();
        assertEquals(WidgetType.IMAGE, widget.getWidgetType());
    }

    @Test
    public void testNewPLabel() {
        final PObject widget = element.newPLabel();
        assertEquals(WidgetType.LABEL, widget.getWidgetType());
    }

    @Test
    public void testNewPLayoutPanel() {
        final PObject widget = element.newPLayoutPanel();
        assertEquals(WidgetType.LAYOUT_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPListBox() {
        final PObject widget = element.newPListBox();
        assertEquals(WidgetType.LISTBOX, widget.getWidgetType());
    }

    @Test
    public void testNewPMenuBar() {
        final PObject widget = element.newPMenuBar();
        assertEquals(WidgetType.MENU_BAR, widget.getWidgetType());
    }

    @Test
    public void testNewPMenuItem() {
        final PObject widget = element.newPMenuItem(null);
        assertEquals(WidgetType.MENU_ITEM, widget.getWidgetType());
    }

    @Test
    public void testNewPMenuItemSeparator() {
        final PObject widget = element.newPMenuItemSeparator();
        assertEquals(WidgetType.MENU_ITEM_SEPARATOR, widget.getWidgetType());
    }

    @Test
    public void testNewPPasswordTextBox() {
        final PObject widget = element.newPPasswordTextBox();
        assertEquals(WidgetType.PASSWORD_TEXTBOX, widget.getWidgetType());
    }

    @Test
    public void testNewPPopupPanel() {
        final PObject widget = element.newPPopupPanel();
        assertEquals(WidgetType.POPUP_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPPushButton() {
        final PObject widget = element.newPPushButton(null);
        assertEquals(WidgetType.PUSH_BUTTON, widget.getWidgetType());
    }

    @Test
    public void testNewPRadioButton() {
        final PObject widget = element.newPRadioButton();
        assertEquals(WidgetType.RADIO_BUTTON, widget.getWidgetType());
    }

    @Test
    public void testNewPRichTextArea() {
        final PObject widget = element.newPRichTextArea();
        assertEquals(WidgetType.RICH_TEXT_AREA, widget.getWidgetType());
    }

    @Test
    public void testNewPRichTextToolbar() {
        final PObject widget = element.newPRichTextToolbar(null);
        assertEquals(WidgetType.RICH_TEXT_TOOLBAR, widget.getWidgetType());
    }

    @Test
    public void testNewPScrollPanel() {
        final PObject widget = element.newPScrollPanel();
        assertEquals(WidgetType.SCROLL_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPSimpleLayoutPanel() {
        final PObject widget = element.newPSimpleLayoutPanel();
        assertEquals(WidgetType.SIMPLE_LAYOUT_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPSimplePanel() {
        final PObject widget = element.newPSimplePanel();
        assertEquals(WidgetType.SIMPLE_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPSplitLayoutPanel() {
        final PObject widget = element.newPSplitLayoutPanel();
        assertEquals(WidgetType.SPLIT_LAYOUT_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPStackLayoutPanel() {
        final PObject widget = element.newPStackLayoutPanel(null);
        assertEquals(WidgetType.STACKLAYOUT_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPSuggestBox() {
        final PObject widget = element.newPSuggestBox();
        assertEquals(WidgetType.SUGGESTBOX, widget.getWidgetType());
    }

    @Test
    public void testNewPTabLayoutPanel() {
        final PObject widget = element.newPTabLayoutPanel();
        assertEquals(WidgetType.TAB_LAYOUT_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPTabPanel() {
        final PObject widget = element.newPTabPanel();
        assertEquals(WidgetType.TAB_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPTextArea() {
        final PObject widget = element.newPTextArea();
        assertEquals(WidgetType.TEXT_AREA, widget.getWidgetType());
    }

    @Test
    public void testNewPTextBox() {
        final PObject widget = element.newPTextBox();
        assertEquals(WidgetType.TEXTBOX, widget.getWidgetType());
    }

    @Test
    public void testNewPTree() {
        final PObject widget = element.newPTree();
        assertEquals(WidgetType.TREE, widget.getWidgetType());
    }

    @Test
    public void testNewPTreeItem() {
        final PObject widget = element.newPTreeItem();
        assertEquals(WidgetType.TREE_ITEM, widget.getWidgetType());
    }

    @Test
    public void testNewPVerticalPanel() {
        final PObject widget = element.newPVerticalPanel();
        assertEquals(WidgetType.VERTICAL_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPWindowBooleanStringStringString() {
        final PObject widget = element.newPWindow(false, null, null, null);
        assertEquals(WidgetType.WINDOW, widget.getWidgetType());
    }

    @Test
    public void testNewPFrame() {
        final PObject widget = element.newPFrame(null);
        assertEquals(WidgetType.FRAME, widget.getWidgetType());
    }

}
