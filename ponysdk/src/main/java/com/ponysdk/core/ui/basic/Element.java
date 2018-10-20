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

package com.ponysdk.core.ui.basic;

import java.text.SimpleDateFormat;

import com.ponysdk.core.model.PUnit;

public class Element {

    public static final String A = "a";
    public static final String B = "b";
    public static final String I = "i";
    public static final String H1 = "h1";
    public static final String H2 = "h2";
    public static final String H3 = "h3";
    public static final String H4 = "h4";
    public static final String H5 = "h5";
    public static final String H6 = "h6";
    public static final String HR = "hr";
    public static final String UL = "ul";
    public static final String LI = "li";
    public static final String DIV = "div";
    public static final String SPAN = "span";
    public static final String P = "p";
    public static final String SMALL = "small";
    public static final String BLOCKQUOTE = "blockquote";
    public static final String TABLE = "table";
    public static final String TBODY = "tbody";
    public static final String THEAD = "thead";
    public static final String TFOOT = "tfoot";
    public static final String TH = "th";
    public static final String TR = "tr";
    public static final String TD = "td";
    public static final String FORM = "form";
    public static final String INPUT = "input";
    public static final String LABEL = "label";
    public static final String BUTTON = "button";
    public static final String SELECT = "select";
    public static final String IFRAME = "iframe";
    public static final String SVG = "svg";
    public static final String LINE = "line";
    public static final String CIRCLE = "circle";
    public static final String ELLIPSE = "ellipse";
    public static final String TEXT = "text";
    public static final String CANVAS = "canvas";
    public static final String SCRIPT = "script";
    public static final String STYLE = "style";

    public static ElementFactory f = new DefaultElementFactory();

    public static final void setElementFactory(final ElementFactory ef) {
        f = ef;
    }

    public static final PAbsolutePanel newPAbsolutePanel() {
        return f.newPAbsolutePanel();
    }

    public static final PAnchor newPAnchor(final String text, final String href) {
        return f.newPAnchor(text, href);
    }

    public static final PAnchor newPAnchor(final String text) {
        return f.newPAnchor(text);
    }

    public static final PAnchor newPAnchor() {
        return f.newPAnchor();
    }

    public static final PButton newPButton(final String text, final String html) {
        return f.newPButton(text, html);
    }

    public static final PButton newPButton(final String text) {
        return f.newPButton(text);
    }

    public static final PButton newPButton() {
        return f.newPButton();
    }

    public static final PCheckBox newPCheckBox() {
        return f.newPCheckBox();
    }

    public static final PCheckBox newPCheckBox(final String label) {
        return f.newPCheckBox(label);
    }

    public static final PDateBox newPDateBox(final PDatePicker picker, final SimpleDateFormat dateFormat,
                                             final boolean keepDayTimeNeeded) {
        return f.newPDateBox(picker, dateFormat, keepDayTimeNeeded);
    }

    public static final PDateBox newPDateBox(final PDatePicker picker, final SimpleDateFormat dateFormat) {
        return f.newPDateBox(picker, dateFormat);
    }

    public static final PDateBox newPDateBox(final SimpleDateFormat dateFormat, final boolean keepDayTimeNeeded) {
        return f.newPDateBox(dateFormat, keepDayTimeNeeded);
    }

    public static final PDateBox newPDateBox(final SimpleDateFormat dateFormat) {
        return f.newPDateBox(dateFormat);
    }

    public static final PDateBox newPDateBox() {
        return f.newPDateBox();
    }

    public static final PDatePicker newPDatePicker() {
        return f.newPDatePicker();
    }

    public static final PDecoratedPopupPanel newPDecoratedPopupPanel(final boolean autoHide) {
        return f.newPDecoratedPopupPanel(autoHide);
    }

    public static final PDecoratorPanel newPDecoratorPanel() {
        return f.newPDecoratorPanel();
    }

    public static final PDialogBox newPDialogBox() {
        return f.newPDialogBox();
    }

    public static final PDialogBox newPDialogBox(final boolean autoHide) {
        return f.newPDialogBox(autoHide);
    }

    public static final PDisclosurePanel newPDisclosurePanel(final String headerText) {
        return f.newPDisclosurePanel(headerText);
    }

    public static final PDockLayoutPanel newPDockLayoutPanel(final PUnit unit) {
        return f.newPDockLayoutPanel(unit);
    }

    public static final PElement newPElement(final String tagName) {
        return f.newPElement(tagName);
    }

    public static final PFileUpload newPFileUpload() {
        return f.newPFileUpload();
    }

    public static final PFlexTable newPFlexTable() {
        return f.newPFlexTable();
    }

    public static final PFlowPanel newPFlowPanel() {
        return f.newPFlowPanel();
    }

    public static final PFocusPanel newPFocusPanel() {
        return f.newPFocusPanel();
    }

    public static final PGrid newPGrid() {
        return f.newPGrid();
    }

    public static final PGrid newPGrid(final int rows, final int columns) {
        return f.newPGrid(rows, columns);
    }

    public static final PHeaderPanel newPHeaderPanel() {
        return f.newPHeaderPanel();
    }

    public static final PHorizontalPanel newPHorizontalPanel() {
        return f.newPHorizontalPanel();
    }

    public static final PHTML newPHTML(final String html, final boolean wordWrap) {
        return f.newPHTML(html, wordWrap);
    }

    public static final PHTML newPHTML(final String html) {
        return f.newPHTML(html);
    }

    public static final PHTML newPHTML() {
        return f.newPHTML();
    }

    public static final PImage newPImage(final String url, final int left, final int top, final int width, final int height) {
        return f.newPImage(url, left, top, width, height);
    }

    public static final PImage newPImage(final String url) {
        return f.newPImage(url);
    }

    public static final PImage newPImage(final PImage.ClassPathURL classpathURL) {
        return f.newPImage(classpathURL);
    }

    public static final PImage newPImage() {
        return f.newPImage();
    }

    public static final PLabel newPLabel() {
        return f.newPLabel();
    }

    public static final PLabel newPLabel(final String text) {
        return f.newPLabel(text);
    }

    public static final PLayoutPanel newPLayoutPanel() {
        return f.newPLayoutPanel();
    }

    public static final PListBox newPListBox() {
        return f.newPListBox();
    }

    public static final PListBox newPListBox(final boolean containsEmptyItem) {
        return f.newPListBox(containsEmptyItem);
    }

    public static final PMenuBar newPMenuBar() {
        return f.newPMenuBar();
    }

    public static final PMenuBar newPMenuBar(final boolean vertical) {
        return f.newPMenuBar(vertical);
    }

    public static final PMenuItem newPMenuItem(final String text, final boolean asHTML) {
        return f.newPMenuItem(text, asHTML);
    }

    public static final PMenuItem newPMenuItem(final String text, final PMenuBar subMenu) {
        return f.newPMenuItem(text, subMenu);
    }

    public static final PMenuItem newPMenuItem(final String text) {
        return f.newPMenuItem(text);
    }

    public static final PMenuItem newPMenuItem(final String text, final boolean asHTML, final Runnable cmd) {
        return f.newPMenuItem(text, asHTML, cmd);
    }

    public static final PMenuItem newPMenuItem(final String text, final boolean asHTML, final PMenuBar subMenu) {
        return f.newPMenuItem(text, asHTML, subMenu);
    }

    public static final PMenuItem newPMenuItem(final String text, final Runnable cmd) {
        return f.newPMenuItem(text, cmd);
    }

    public static final PMenuItemSeparator newPMenuItemSeparator() {
        return f.newPMenuItemSeparator();
    }

    public static final PPasswordTextBox newPPasswordTextBox() {
        return f.newPPasswordTextBox();
    }

    public static final PPasswordTextBox newPPasswordTextBox(final String text) {
        return f.newPPasswordTextBox(text);
    }

    public static final PPopupPanel newPPopupPanel(final boolean autoHide) {
        return f.newPPopupPanel(autoHide);
    }

    public static final PPopupPanel newPPopupPanel() {
        return f.newPPopupPanel();
    }

    public static final PPushButton newPPushButton(final PImage image) {
        return f.newPPushButton(image);
    }

    public static final PRadioButton newPRadioButton() {
        return f.newPRadioButton();
    }

    public static final PRadioButton newPRadioButton(final String label) {
        return f.newPRadioButton(label);
    }

    public static final PRadioButtonGroup newPRadioButtonGroup(final String name) {
        return f.newPRadioButtonGroup(name);
    }

    public static final PRichTextArea newPRichTextArea() {
        return f.newPRichTextArea();
    }

    public static final PRichTextToolbar newPRichTextToolbar(final PRichTextArea richTextArea) {
        return f.newPRichTextToolbar(richTextArea);
    }

    public static final PScrollPanel newPScrollPanel() {
        return f.newPScrollPanel();
    }

    public static final PSimpleLayoutPanel newPSimpleLayoutPanel() {
        return f.newPSimpleLayoutPanel();
    }

    public static final PSimplePanel newPSimplePanel() {
        return f.newPSimplePanel();
    }

    public static final PSplitLayoutPanel newPSplitLayoutPanel() {
        return f.newPSplitLayoutPanel();
    }

    public static final PStackLayoutPanel newPStackLayoutPanel(final PUnit unit) {
        return f.newPStackLayoutPanel(unit);
    }

    public static final PSuggestBox newPSuggestBox() {
        return f.newPSuggestBox();
    }

    public static final PSuggestBox newPSuggestBox(final PSuggestOracle suggestOracle) {
        return f.newPSuggestBox(suggestOracle);
    }

    public static final PTabLayoutPanel newPTabLayoutPanel() {
        return f.newPTabLayoutPanel();
    }

    public static final PTabPanel newPTabPanel() {
        return f.newPTabPanel();
    }

    public static final PTextArea newPTextArea() {
        return f.newPTextArea();
    }

    public static final PTextArea newPTextArea(final String text) {
        return f.newPTextArea(text);
    }

    public static final PTextBox newPTextBox() {
        return f.newPTextBox();
    }

    public static final PTextBox newPTextBox(final String text) {
        return f.newPTextBox(text);
    }

    public static final PTree newPTree() {
        return f.newPTree();
    }

    public static final PTreeItem newPTreeItem(final String text) {
        return f.newPTreeItem(text);
    }

    public static final PTreeItem newPTreeItem(final PWidget widget) {
        return f.newPTreeItem(widget);
    }

    public static final PTreeItem newPTreeItem() {
        return f.newPTreeItem();
    }

    public static final PVerticalPanel newPVerticalPanel() {
        return f.newPVerticalPanel();
    }

    public static final PWindow newPWindow(final String name, final String features) {
        return newPWindow(true, null, name, features);
    }

    public static final PWindow newPWindow(final String url, final String name, final String features) {
        return newPWindow(false, url, name, features);
    }

    public static final PWindow newPWindow(final boolean relative, final String url, final String name, final String features) {
        return f.newPWindow(relative, url, name, features);
    }

    public static final PWindow newPWindow(final PWindow parentWindow, final String name, final String features) {
        return newPWindow(parentWindow, true, null, name, features);
    }

    public static final PWindow newPWindow(final PWindow parentWindow, final String url, final String name, final String features) {
        return newPWindow(parentWindow, false, url, name, features);
    }

    public static final PWindow newPWindow(final PWindow parentWindow, final boolean relative, final String url, final String name,
                                           final String features) {
        return f.newPWindow(parentWindow, relative, url, name, features);
    }

    public static final PFrame newPFrame() {
        return newPFrame(null);
    }

    public static final PFrame newPFrame(final String url) {
        return f.newPFrame(url);
    }

    public static final PElement newA() {
        return newPElement(A);
    }

    public static final PElement newB() {
        return newPElement(B);
    }

    public static final PElement newI() {
        return newPElement(I);
    }

    public static final PElement newH1() {
        return newPElement(H1);
    }

    public static final PElement newH2() {
        return newPElement(H2);
    }

    public static final PElement newH3() {
        return newPElement(H3);
    }

    public static final PElement newH4() {
        return newPElement(H4);
    }

    public static final PElement newH5() {
        return newPElement(H5);
    }

    public static final PElement newH6() {
        return newPElement(H6);
    }

    public static final PElement newHr() {
        return newPElement(HR);
    }

    public static final PElement newUl() {
        return newPElement(UL);
    }

    public static final PElement newLi() {
        return newPElement(LI);
    }

    public static final PElement newDiv() {
        return newPElement(DIV);
    }

    public static final PElement newSpan() {
        return newPElement(SPAN);
    }

    public static final PElement newP() {
        return newPElement(P);
    }

    public static final PElement newSmall() {
        return newPElement(SMALL);
    }

    public static final PElement newBlockquote() {
        return newPElement(BLOCKQUOTE);
    }

    public static final PElement newTable() {
        return newPElement(TABLE);
    }

    public static final PElement newThead() {
        return newPElement(THEAD);
    }

    public static final PElement newTfoot() {
        return newPElement(TFOOT);
    }

    public static final PElement newTh() {
        return newPElement(TH);
    }

    public static final PElement newTbody() {
        return newPElement(TBODY);
    }

    public static final PElement newTr() {
        return newPElement(TR);
    }

    public static final PElement newTd() {
        return newPElement(TD);
    }

    public static final PElement newForm() {
        return newPElement(FORM);
    }

    public static final PElement newInput() {
        return newPElement(INPUT);
    }

    public static final PElement newSelect() {
        return newPElement(SELECT);
    }

    public static final PElement newLabel() {
        return newPElement(LABEL);
    }

    public static final PElement newButton() {
        return newPElement(BUTTON);
    }

    public static final PElement newIframe() {
        return newPElement(IFRAME);
    }

    public static final PElement newSvg() {
        return newPElement(SVG);
    }

    public static final PElement newLine() {
        return newPElement(LINE);
    }

    public static final PElement newCircle() {
        return newPElement(CIRCLE);
    }

    public static final PElement newEllipse() {
        return newPElement(ELLIPSE);
    }

    public static final PElement newText() {
        return newPElement(TEXT);
    }

    public static final PElement newCanvas() {
        return newPElement(CANVAS);
    }

    public static final PElement newScript() {
        return newPElement(SCRIPT);
    }

    public static final PElement newStyle() {
        return newPElement(STYLE);
    }

}
