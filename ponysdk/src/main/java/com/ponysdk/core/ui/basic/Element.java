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
import com.ponysdk.core.ui.formatter.TextFunction;

public class Element {

    public static final String E_A = "a";
    public static final String E_B = "b";
    public static final String E_H1 = "h1";
    public static final String E_H2 = "h2";
    public static final String E_H3 = "h3";
    public static final String E_H4 = "h4";
    public static final String E_H5 = "h5";
    public static final String E_HR = "hr";
    public static final String E_UL = "ul";
    public static final String E_LI = "li";
    public static final String E_DIV = "div";
    public static final String E_SPAN = "span";
    public static final String E_P = "p";
    public static final String E_SMALL = "small";
    public static final String E_BLOCKQUOTE = "blockquote";
    public static final String E_TABLE = "table";
    public static final String E_TBODY = "tbody";
    public static final String E_THEAD = "thead";
    public static final String E_TH = "th";
    public static final String E_TR = "tr";
    public static final String E_TD = "td";
    public static final String E_FORM = "form";
    public static final String E_INPUT = "input";
    public static final String E_LABEL = "label";
    public static final String E_BUTTON = "button";
    public static final String E_SELECT = "select";
    public static final String E_IFRAME = "iframe";
    public static final String E_SVG = "svg";
    public static final String E_LINE = "line";
    public static final String E_CIRCLE = "circle";
    public static final String E_ELLIPSE = "ellipse";
    public static final String E_TEXT = "text";
    public static final String E_CANVAS = "canvas";
    public static final String E_SCRIPT = "script";
    public static final String E_STYLE = "style";

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

    public static final PFunctionalLabel newPFunctionalLabel(final TextFunction textFunction) {
        return f.newPFunctionalLabel(textFunction);
    }

    public static final PFunctionalLabel newPFunctionalLabel(final TextFunction textFunction, final Object... args) {
        return f.newPFunctionalLabel(textFunction, args);
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
        return newPElement(E_A);
    }

    public static final PElement newB() {
        return newPElement(E_B);
    }

    public static final PElement newH1() {
        return newPElement(E_H1);
    }

    public static final PElement newH2() {
        return newPElement(E_H2);
    }

    public static final PElement newH3() {
        return newPElement(E_H3);
    }

    public static final PElement newH4() {
        return newPElement(E_H4);
    }

    public static final PElement newH5() {
        return newPElement(E_H5);
    }

    public static final PElement newHr() {
        return newPElement(E_HR);
    }

    public static final PElement newUl() {
        return newPElement(E_UL);
    }

    public static final PElement newLi() {
        return newPElement(E_LI);
    }

    public static final PElement newDiv() {
        return newPElement(E_DIV);
    }

    public static final PElement newSpan() {
        return newPElement(E_SPAN);
    }

    public static final PElement newP() {
        return newPElement(E_P);
    }

    public static final PElement newSmall() {
        return newPElement(E_SMALL);
    }

    public static final PElement newBlockquote() {
        return newPElement(E_BLOCKQUOTE);
    }

    public static final PElement newTable() {
        return newPElement(E_TABLE);
    }

    public static final PElement newThead() {
        return newPElement(E_THEAD);
    }

    public static final PElement newTh() {
        return newPElement(E_TH);
    }

    public static final PElement newTbody() {
        return newPElement(E_TBODY);
    }

    public static final PElement newTr() {
        return newPElement(E_TR);
    }

    public static final PElement newTd() {
        return newPElement(E_TD);
    }

    public static final PElement newForm() {
        return newPElement(E_FORM);
    }

    public static final PElement newInput() {
        return newPElement(E_INPUT);
    }

    public static final PElement newSelect() {
        return newPElement(E_SELECT);
    }

    public static final PElement newLabel() {
        return newPElement(E_LABEL);
    }

    public static final PElement newButton() {
        return newPElement(E_BUTTON);
    }

    public static final PElement newIframe() {
        return newPElement(E_IFRAME);
    }

    public static final PElement newSvg() {
        return newPElement(E_SVG);
    }

    public static final PElement newLine() {
        return newPElement(E_LINE);
    }

    public static final PElement newCircle() {
        return newPElement(E_CIRCLE);
    }

    public static final PElement newEllipse() {
        return newPElement(E_ELLIPSE);
    }

    public static final PElement newText() {
        return newPElement(E_TEXT);
    }

    public static final PElement newCanvas() {
        return newPElement(E_CANVAS);
    }

    public static final PElement newScript() {
        return newPElement(E_SCRIPT);
    }

    public static final PElement newStyle() {
        return newPElement(E_STYLE);
    }

}
