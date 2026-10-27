
package com.ponysdk.core.ui.basic;

import com.ponysdk.test.PSuite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.formatter.TextFunction;

public class ElementTest extends PSuite {

    @BeforeAll
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

    @Test
    public void textOverloadsKeepTheirArguments() {
        final PAnchor anchor = Element.newPAnchor("text", "http://href");
        assertEquals("text", anchor.getText());
        assertEquals("http://href", anchor.getHref());
        assertEquals("only", Element.newPAnchor("only").getText());
        assertNull(Element.newPAnchor("only").getHref());

        final PButton button = Element.newPButton("t", "<b>h</b>");
        assertEquals("t", button.getText());
        assertEquals("<b>h</b>", button.getHTML());
        assertEquals("t", Element.newPButton("t").getText());

        assertEquals("c", Element.newPCheckBox("c").getText());
        assertEquals("r", Element.newPRadioButton("r").getText());
        assertEquals("l", Element.newPLabel("l").getText());
        assertEquals("x", Element.newPTextBox("x").getText());
        assertEquals("x", Element.newPTextArea("x").getText());
        assertEquals("x", Element.newPPasswordTextBox("x").getText());

        assertEquals("<p/>", Element.newPHTML("<p/>").getHTML());
        final PHTML wrapped = Element.newPHTML("<p/>", true);
        assertTrue(wrapped.isWordWrap());
        assertFalse(Element.newPHTML().isWordWrap());
    }

    @Test
    public void imagesKeepTheirUrl() {
        assertEquals("u", Element.newPImage("u").getUrl());
        assertEquals("u", Element.newPImage("u", 1, 2, 3, 4).getUrl());
        assertTrue(Element.newPImage(new PImage.ClassPathURL("images/disclosure_closed.png")).getUrl().startsWith("data:image/png"));
        final PImage image = Element.newPImage();
        assertEquals(WidgetType.PUSH_BUTTON, Element.newPPushButton(image).getWidgetType());
    }

    @Test
    public void dateBoxOverloadsKeepTheirFormatAndPicker() {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy");
        final PDatePicker picker = Element.newPDatePicker();
        final PDateBox withPicker = Element.newPDateBox(picker, format);
        assertSame(picker, withPicker.getDatePicker());
        assertSame(format, withPicker.getDateFormat());
        assertSame(format, Element.newPDateBox(format).getDateFormat());
        assertSame(format, Element.newPDateBox(format, true).getDateFormat());

        final PDateBox fromFactory = new DefaultElementFactory().newPDateBox(picker, format, true);
        assertSame(picker, fromFactory.getDatePicker());
        assertSame(format, fromFactory.getDateFormat());
    }

    @Test
    public void panelAndMenuOverloads() {
        assertEquals(WidgetType.DIALOG_BOX, Element.newPDialogBox(true).getWidgetType());
        assertEquals(WidgetType.POPUP_PANEL, Element.newPPopupPanel(true).getWidgetType());
        assertEquals(PUnit.PX, Element.newPDockLayoutPanel(PUnit.PX).getUnit());
        assertEquals(WidgetType.STACKLAYOUT_PANEL, Element.newPStackLayoutPanel(PUnit.EM).getWidgetType());
        assertEquals(WidgetType.DISCLOSURE_PANEL, Element.newPDisclosurePanel("header").getWidgetType());
        final PGrid grid = Element.newPGrid(2, 3);
        assertEquals(2, grid.getRows());
        assertEquals(3, grid.getColumns());
        assertEquals(WidgetType.LISTBOX, Element.newPListBox(true).getWidgetType());

        assertTrue(Element.newPMenuBar(true).isVertical());
        assertFalse(Element.newPMenuBar(false).isVertical());
        final PMenuBar subMenu = Element.newPMenuBar();
        assertEquals("a", Element.newPMenuItem("a", false).getText());
        assertSame(subMenu, Element.newPMenuItem("b", subMenu).getSubMenu());
        assertSame(subMenu, Element.newPMenuItem("c", true, subMenu).getSubMenu());
        assertEquals("d", Element.newPMenuItem("d", false, () -> {
        }).getText());
        assertEquals("e", Element.newPMenuItem("e", () -> {
        }).getText());

        final PRichTextArea richTextArea = Element.newPRichTextArea();
        assertSame(richTextArea, Element.newPRichTextToolbar(richTextArea).getRichTextArea());
        final PMultiWordSuggestOracle oracle = new PMultiWordSuggestOracle();
        assertSame(oracle, Element.newPSuggestBox(oracle).getSuggestOracle());
    }

    @Test
    public void treeItemOverloads() {
        final PLabel label = Element.newPLabel("w");
        assertSame(label, Element.newPTreeItem(label).getWidget());
        assertEquals(WidgetType.TREE_ITEM, Element.newPTreeItem("text").getWidgetType());
    }

    @Test
    public void radioAndFunctionalFactories() {
        assertEquals("g", Element.newPRadioButtonGroup("g").getName());
        final PRadioButton radio = Element.newPRadioButton();
        assertNull(Element.newPRadioButtonSelection(List.of(radio)).getValue());

        final TextFunction function = TextFunction.named(args -> "v", "f");
        assertSame(function, Element.newPFunctionalLabel(function).getTextFunction());
        assertEquals("v", Element.newPFunctionalLabel(function, 1).getText());
    }

    @Test
    public void windowAndFrameOverloads() {
        final PWindow relative = Element.newPWindow("n", "f");
        assertEquals("n", relative.getName());
        assertNull(relative.getUrl());

        final PWindow absolute = Element.newPWindow("http://u", "n2", "f");
        assertEquals("http://u", absolute.getUrl());

        final PWindow child = Element.newPWindow(absolute, "child", null);
        assertSame(absolute, child.getParent());
        final PWindow childWithUrl = Element.newPWindow(absolute, "http://c", "child2", null);
        assertSame(absolute, childWithUrl.getParent());
        assertEquals("http://c", childWithUrl.getUrl());
        assertEquals(2, absolute.getSubWindows().size());

        assertEquals(WidgetType.FRAME, Element.newPFrame().getWidgetType());
    }

    @Test
    public void htmlElementFactoriesUseTheirTag() {
        final Map<String, Supplier<PElement>> factories = new LinkedHashMap<>();
        factories.put(Element.E_A, Element::newA);
        factories.put(Element.E_B, Element::newB);
        factories.put(Element.E_H1, Element::newH1);
        factories.put(Element.E_H2, Element::newH2);
        factories.put(Element.E_H3, Element::newH3);
        factories.put(Element.E_H4, Element::newH4);
        factories.put(Element.E_H5, Element::newH5);
        factories.put(Element.E_HR, Element::newHr);
        factories.put(Element.E_UL, Element::newUl);
        factories.put(Element.E_LI, Element::newLi);
        factories.put(Element.E_DIV, Element::newDiv);
        factories.put(Element.E_SPAN, Element::newSpan);
        factories.put(Element.E_P, Element::newP);
        factories.put(Element.E_SMALL, Element::newSmall);
        factories.put(Element.E_BLOCKQUOTE, Element::newBlockquote);
        factories.put(Element.E_TABLE, Element::newTable);
        factories.put(Element.E_TFOOT, Element::newTfoot);
        factories.put(Element.E_THEAD, Element::newThead);
        factories.put(Element.E_TH, Element::newTh);
        factories.put(Element.E_TBODY, Element::newTbody);
        factories.put(Element.E_TR, Element::newTr);
        factories.put(Element.E_TD, Element::newTd);
        factories.put(Element.E_FORM, Element::newForm);
        factories.put(Element.E_INPUT, Element::newInput);
        factories.put(Element.E_SELECT, Element::newSelect);
        factories.put(Element.E_LABEL, Element::newLabel);
        factories.put(Element.E_BUTTON, Element::newButton);
        factories.put(Element.E_IFRAME, Element::newIframe);
        factories.put(Element.E_SVG, Element::newSvg);
        factories.put(Element.E_LINE, Element::newLine);
        factories.put(Element.E_CIRCLE, Element::newCircle);
        factories.put(Element.E_ELLIPSE, Element::newEllipse);
        factories.put(Element.E_TEXT, Element::newText);
        factories.put(Element.E_CANVAS, Element::newCanvas);
        factories.put(Element.E_SCRIPT, Element::newScript);
        factories.put(Element.E_STYLE, Element::newStyle);

        factories.forEach((tag, factory) -> assertEquals(tag, factory.get().getTagName()));
    }

}
