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

public class DefaultElementFactory implements ElementFactory {

    @Override
    public PAbsolutePanel newPAbsolutePanel() {
        return new PAbsolutePanel();
    }

    @Override
    public PAnchor newPAnchor(final String text, final String href) {
        return new PAnchor(text, href);
    }

    @Override
    public PAnchor newPAnchor(final String text) {
        return new PAnchor(text);
    }

    @Override
    public PAnchor newPAnchor() {
        return new PAnchor();
    }

    @Override
    public PButton newPButton(final String text, final String html) {
        return new PButton(text, html);
    }

    @Override
    public PButton newPButton(final String text) {
        return new PButton(text);
    }

    @Override
    public PButton newPButton() {
        return new PButton();
    }

    @Override
    public PCheckBox newPCheckBox() {
        return new PCheckBox();
    }

    @Override
    public PCheckBox newPCheckBox(final String label) {
        return new PCheckBox(label);
    }

    @Override
    public PDateBox newPDateBox(final PDatePicker picker, final SimpleDateFormat dateFormat, final boolean keepDayTimeNeeded) {
        return new PDateBox(picker, dateFormat, keepDayTimeNeeded);
    }

    @Override
    public PDateBox newPDateBox(final PDatePicker picker, final SimpleDateFormat dateFormat) {
        return new PDateBox(picker, dateFormat);
    }

    @Override
    public PDateBox newPDateBox(final SimpleDateFormat dateFormat, final boolean keepDayTimeNeeded) {
        return new PDateBox(dateFormat, keepDayTimeNeeded);
    }

    @Override
    public PDateBox newPDateBox(final SimpleDateFormat dateFormat) {
        return new PDateBox(dateFormat);
    }

    @Override
    public PDateBox newPDateBox() {
        return new PDateBox();
    }

    @Override
    public PDatePicker newPDatePicker() {
        return new PDatePicker();
    }

    @Override
    public PDecoratorPanel newPDecoratorPanel() {
        return new PDecoratorPanel();
    }

    @Override
    public PDecoratedPopupPanel newPDecoratedPopupPanel(final boolean autoHide) {
        return new PDecoratedPopupPanel(autoHide);
    }

    @Override
    public PDialogBox newPDialogBox() {
        return new PDialogBox();
    }

    @Override
    public PDialogBox newPDialogBox(final boolean autoHide) {
        return new PDialogBox(autoHide);
    }

    @Override
    public PDisclosurePanel newPDisclosurePanel(final String headerText) {
        return new PDisclosurePanel(headerText);
    }

    @Override
    public PDockLayoutPanel newPDockLayoutPanel(final PUnit unit) {
        return new PDockLayoutPanel(unit);
    }

    @Override
    public PElement newPElement(final String tagName) {
        return new PElement(tagName);
    }

    @Override
    public PFileUpload newPFileUpload() {
        return new PFileUpload();
    }

    @Override
    public PFlexTable newPFlexTable() {
        return new PFlexTable();
    }

    @Override
    public PFlowPanel newPFlowPanel() {
        return new PFlowPanel();
    }

    @Override
    public PFocusPanel newPFocusPanel() {
        return new PFocusPanel();
    }

    @Override
    public PGrid newPGrid() {
        return new PGrid();
    }

    @Override
    public PGrid newPGrid(final int rows, final int columns) {
        return new PGrid(rows, columns);
    }

    @Override
    public PHeaderPanel newPHeaderPanel() {
        return new PHeaderPanel();
    }

    @Override
    public PHorizontalPanel newPHorizontalPanel() {
        return new PHorizontalPanel();
    }

    @Override
    public PHTML newPHTML(final String html, final boolean wordWrap) {
        return new PHTML(html, wordWrap);
    }

    @Override
    public PHTML newPHTML(final String html) {
        return new PHTML(html);
    }

    @Override
    public PHTML newPHTML() {
        return new PHTML();
    }

    @Override
    public PImage newPImage(final String url, final int left, final int top, final int width, final int height) {
        return new PImage(url, left, top, width, height);
    }

    @Override
    public PImage newPImage(final String url) {
        return new PImage(url);
    }

    @Override
    public PImage newPImage(final PImage.ClassPathURL classpathURL) {
        return new PImage(classpathURL);
    }

    @Override
    public PImage newPImage() {
        return new PImage();
    }

    @Override
    public PLabel newPLabel() {
        return new PLabel();
    }

    @Override
    public PLabel newPLabel(final String text) {
        return new PLabel(text);
    }

    @Override
    public PLayoutPanel newPLayoutPanel() {
        return new PLayoutPanel();
    }

    @Override
    public PListBox newPListBox() {
        return new PListBox();
    }

    @Override
    public PListBox newPListBox(final boolean containsEmptyItem) {
        return new PListBox(containsEmptyItem);
    }

    @Override
    public PMenuBar newPMenuBar() {
        return new PMenuBar();
    }

    @Override
    public PMenuBar newPMenuBar(final boolean vertical) {
        return new PMenuBar(vertical);
    }

    @Override
    public PMenuItem newPMenuItem(final String text, final boolean asHTML) {
        return new PMenuItem(text, asHTML);
    }

    @Override
    public PMenuItem newPMenuItem(final String text, final PMenuBar subMenu) {
        return new PMenuItem(text, subMenu);
    }

    @Override
    public PMenuItem newPMenuItem(final String text) {
        return new PMenuItem(text);
    }

    @Override
    public PMenuItem newPMenuItem(final String text, final boolean asHTML, final Runnable cmd) {
        return new PMenuItem(text, asHTML, cmd);
    }

    @Override
    public PMenuItem newPMenuItem(final String text, final boolean asHTML, final PMenuBar subMenu) {
        return new PMenuItem(text, asHTML, subMenu);
    }

    @Override
    public PMenuItem newPMenuItem(final String text, final Runnable cmd) {
        return new PMenuItem(text, cmd);
    }

    @Override
    public PMenuItemSeparator newPMenuItemSeparator() {
        return new PMenuItemSeparator();
    }

    @Override
    public PPasswordTextBox newPPasswordTextBox() {
        return new PPasswordTextBox();
    }

    @Override
    public PPasswordTextBox newPPasswordTextBox(final String text) {
        return new PPasswordTextBox(text);
    }

    @Override
    public PPopupPanel newPPopupPanel(final boolean autoHide) {
        return new PPopupPanel(autoHide);
    }

    @Override
    public PPopupPanel newPPopupPanel() {
        return new PPopupPanel();
    }

    @Override
    public PPushButton newPPushButton(final PImage image) {
        return new PPushButton(image);
    }

    @Override
    public PRadioButton newPRadioButton() {
        return new PRadioButton();
    }

    @Override
    public PRadioButton newPRadioButton(final String label) {
        return new PRadioButton(label);
    }

    @Override
    public PRadioButtonGroup newPRadioButtonGroup(final String name) {
        return new PRadioButtonGroup(name);
    }

    @Override
    public PRichTextArea newPRichTextArea() {
        return new PRichTextArea();
    }

    @Override
    public PRichTextToolbar newPRichTextToolbar(final PRichTextArea richTextArea) {
        return new PRichTextToolbar(richTextArea);
    }

    @Override
    public PScrollPanel newPScrollPanel() {
        return new PScrollPanel();
    }

    @Override
    public PSimpleLayoutPanel newPSimpleLayoutPanel() {
        return new PSimpleLayoutPanel();
    }

    @Override
    public PSimplePanel newPSimplePanel() {
        return new PSimplePanel();
    }

    @Override
    public PSplitLayoutPanel newPSplitLayoutPanel() {
        return new PSplitLayoutPanel();
    }

    @Override
    public PStackLayoutPanel newPStackLayoutPanel(final PUnit unit) {
        return new PStackLayoutPanel(unit);
    }

    @Override
    public PSuggestBox newPSuggestBox() {
        return new PSuggestBox();
    }

    @Override
    public PSuggestBox newPSuggestBox(final PSuggestOracle suggestOracle) {
        return new PSuggestBox(suggestOracle);
    }

    @Override
    public PTabLayoutPanel newPTabLayoutPanel() {
        return new PTabLayoutPanel();
    }

    @Override
    public PTabPanel newPTabPanel() {
        return new PTabPanel();
    }

    @Override
    public PTextArea newPTextArea() {
        return new PTextArea();
    }

    @Override
    public PTextArea newPTextArea(final String text) {
        return new PTextArea(text);
    }

    @Override
    public PTextBox newPTextBox() {
        return new PTextBox();
    }

    @Override
    public PTextBox newPTextBox(final String text) {
        return new PTextBox(text);
    }

    @Override
    public PTree newPTree() {
        return new PTree();
    }

    @Override
    public PTreeItem newPTreeItem(final String text) {
        return new PTreeItem(text);
    }

    @Override
    public PTreeItem newPTreeItem(final PWidget widget) {
        return new PTreeItem(widget);
    }

    @Override
    public PTreeItem newPTreeItem() {
        return new PTreeItem();
    }

    @Override
    public PVerticalPanel newPVerticalPanel() {
        return new PVerticalPanel();
    }

    @Override
    public PWindow newPWindow(final boolean relative, final String url, final String name, final String features) {
        return new PWindow(relative, url, name, features);
    }

    @Override
    public PWindow newPWindow(final PWindow parentWindow, final boolean relative, final String url, final String name,
                              final String features) {
        return new PWindow(parentWindow, relative, url, name, features);
    }

    @Override
    public PFrame newPFrame(final String url) {
        return new PFrame(url);
    }

    @Override
    public PFunctionalLabel newPFunctionalLabel(final TextFunction textFunction) {
        return new PFunctionalLabel(textFunction);
    }

    @Override
    public PFunctionalLabel newPFunctionalLabel(final TextFunction textFunction, final Object... args) {
        return new PFunctionalLabel(textFunction, args);
    }

}
