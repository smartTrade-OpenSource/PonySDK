
package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.WidgetType;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ElementTest extends PSuite {

    @BeforeClass
    public static void init() {
        Element.setElementFactory(new DefaultElementFactory());
    }

    @Test
    public void testNewPAbsolutePanel() {
        final PObject widget = Element.newPAbsolutePanel();
        assertEquals(WidgetType.ABSOLUTE_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPAnchor() {
        final PObject widget = Element.newPAnchor();
        assertEquals(WidgetType.ANCHOR, widget.getWidgetType());
    }

    @Test
    public void testNewPButton() {
        final PObject widget = Element.newPButton();
        assertEquals(WidgetType.BUTTON, widget.getWidgetType());
    }

    @Test
    public void testNewPCheckBox() {
        final PObject widget = Element.newPCheckBox();
        assertEquals(WidgetType.CHECKBOX, widget.getWidgetType());
    }

    @Test
    public void testNewPDateBox() {
        final PObject widget = Element.newPDateBox();
        assertEquals(WidgetType.DATEBOX, widget.getWidgetType());
    }

    @Test
    public void testNewPDatePicker() {
        final PObject widget = Element.newPDatePicker();
        assertEquals(WidgetType.DATEPICKER, widget.getWidgetType());
    }

    @Test
    public void testNewPDecoratorPanel() {
        final PObject widget = Element.newPDecoratorPanel();
        assertEquals(WidgetType.DECORATOR_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPDecoratedPopupPanel() {
        final PObject widget = Element.newPDecoratedPopupPanel(false);
        assertEquals(WidgetType.DECORATED_POPUP_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPDialogBox() {
        final PObject widget = Element.newPDialogBox();
        assertEquals(WidgetType.DIALOG_BOX, widget.getWidgetType());
    }

    @Test
    public void testNewPDisclosurePanel() {
        final PObject widget = Element.newPDisclosurePanel(null);
        assertEquals(WidgetType.DISCLOSURE_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPDockLayoutPanel() {
        final PObject widget = Element.newPDockLayoutPanel(null);
        assertEquals(WidgetType.DOCK_LAYOUT_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPElement() {
        final PObject widget = Element.newPElement(null);
        assertEquals(WidgetType.ELEMENT, widget.getWidgetType());
    }

    @Test
    public void testNewPFileUpload() {
        final PObject widget = Element.newPFileUpload();
        assertEquals(WidgetType.FILE_UPLOAD, widget.getWidgetType());
    }

    @Test
    public void testNewPFlexTable() {
        final PObject widget = Element.newPFlexTable();
        assertEquals(WidgetType.FLEX_TABLE, widget.getWidgetType());
    }

    @Test
    public void testNewPFlowPanel() {
        final PObject widget = Element.newPFlowPanel();
        assertEquals(WidgetType.FLOW_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPFocusPanel() {
        final PObject widget = Element.newPFocusPanel();
        assertEquals(WidgetType.FOCUS_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPGrid() {
        final PObject widget = Element.newPGrid();
        assertEquals(WidgetType.GRID, widget.getWidgetType());
    }

    @Test
    public void testNewPHeaderPanel() {
        final PObject widget = Element.newPHeaderPanel();
        assertEquals(WidgetType.HEADER_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPHorizontalPanel() {
        final PObject widget = Element.newPHorizontalPanel();
        assertEquals(WidgetType.HORIZONTAL_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPHTML() {
        final PObject widget = Element.newPHTML();
        assertEquals(WidgetType.HTML, widget.getWidgetType());
    }

    @Test
    public void testNewPImage() {
        final PObject widget = Element.newPImage();
        assertEquals(WidgetType.IMAGE, widget.getWidgetType());
    }

    @Test
    public void testNewPLabel() {
        final PObject widget = Element.newPLabel();
        assertEquals(WidgetType.LABEL, widget.getWidgetType());
    }

    @Test
    public void testNewPLayoutPanel() {
        final PObject widget = Element.newPLayoutPanel();
        assertEquals(WidgetType.LAYOUT_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPListBox() {
        final PObject widget = Element.newPListBox();
        assertEquals(WidgetType.LISTBOX, widget.getWidgetType());
    }

    @Test
    public void testNewPMenuBar() {
        final PObject widget = Element.newPMenuBar();
        assertEquals(WidgetType.MENU_BAR, widget.getWidgetType());
    }

    @Test
    public void testNewPMenuItem() {
        final PObject widget = Element.newPMenuItem(null);
        assertEquals(WidgetType.MENU_ITEM, widget.getWidgetType());
    }

    @Test
    public void testNewPMenuItemSeparator() {
        final PObject widget = Element.newPMenuItemSeparator();
        assertEquals(WidgetType.MENU_ITEM_SEPARATOR, widget.getWidgetType());
    }

    @Test
    public void testNewPPasswordTextBox() {
        final PObject widget = Element.newPPasswordTextBox();
        assertEquals(WidgetType.PASSWORD_TEXTBOX, widget.getWidgetType());
    }

    @Test
    public void testNewPPopupPanel() {
        final PObject widget = Element.newPPopupPanel();
        assertEquals(WidgetType.POPUP_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPPushButton() {
        final PObject widget = Element.newPPushButton(null);
        assertEquals(WidgetType.PUSH_BUTTON, widget.getWidgetType());
    }

    @Test
    public void testNewPRadioButton() {
        final PObject widget = Element.newPRadioButton();
        assertEquals(WidgetType.RADIO_BUTTON, widget.getWidgetType());
    }

    @Test
    public void testNewPRichTextArea() {
        final PObject widget = Element.newPRichTextArea();
        assertEquals(WidgetType.RICH_TEXT_AREA, widget.getWidgetType());
    }

    @Test
    public void testNewPRichTextToolbar() {
        final PObject widget = Element.newPRichTextToolbar(null);
        assertEquals(WidgetType.RICH_TEXT_TOOLBAR, widget.getWidgetType());
    }

    @Test
    public void testNewPScrollPanel() {
        final PObject widget = Element.newPScrollPanel();
        assertEquals(WidgetType.SCROLL_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPSimpleLayoutPanel() {
        final PObject widget = Element.newPSimpleLayoutPanel();
        assertEquals(WidgetType.SIMPLE_LAYOUT_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPSimplePanel() {
        final PObject widget = Element.newPSimplePanel();
        assertEquals(WidgetType.SIMPLE_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPSplitLayoutPanel() {
        final PObject widget = Element.newPSplitLayoutPanel();
        assertEquals(WidgetType.SPLIT_LAYOUT_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPStackLayoutPanel() {
        final PObject widget = Element.newPStackLayoutPanel(null);
        assertEquals(WidgetType.STACKLAYOUT_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPSuggestBox() {
        final PObject widget = Element.newPSuggestBox();
        assertEquals(WidgetType.SUGGESTBOX, widget.getWidgetType());
    }

    @Test
    public void testNewPTabLayoutPanel() {
        final PObject widget = Element.newPTabLayoutPanel();
        assertEquals(WidgetType.TAB_LAYOUT_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPTabPanel() {
        final PObject widget = Element.newPTabPanel();
        assertEquals(WidgetType.TAB_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPTextArea() {
        final PObject widget = Element.newPTextArea();
        assertEquals(WidgetType.TEXT_AREA, widget.getWidgetType());
    }

    @Test
    public void testNewPTextBox() {
        final PObject widget = Element.newPTextBox();
        assertEquals(WidgetType.TEXTBOX, widget.getWidgetType());
    }

    @Test
    public void testNewPTree() {
        final PObject widget = Element.newPTree();
        assertEquals(WidgetType.TREE, widget.getWidgetType());
    }

    @Test
    public void testNewPTreeItem() {
        final PObject widget = Element.newPTreeItem();
        assertEquals(WidgetType.TREE_ITEM, widget.getWidgetType());
    }

    @Test
    public void testNewPVerticalPanel() {
        final PObject widget = Element.newPVerticalPanel();
        assertEquals(WidgetType.VERTICAL_PANEL, widget.getWidgetType());
    }

    @Test
    public void testNewPWindow() {
        final PObject widget = Element.newPWindow(false, null, null, null);
        assertEquals(WidgetType.WINDOW, widget.getWidgetType());
    }

    @Test
    public void testNewPFrame() {
        final PObject widget = Element.newPFrame(null);
        assertEquals(WidgetType.FRAME, widget.getWidgetType());
    }

}
