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

public interface ElementFactory {

    PAbsolutePanel newPAbsolutePanel();

    PAnchor newPAnchor(final String text, final String href);

    PAnchor newPAnchor(final String text);

    PAnchor newPAnchor();

    PButton newPButton(final String text, final String html);

    PButton newPButton(final String text);

    PButton newPButton();

    PCheckBox newPCheckBox();

    PCheckBox newPCheckBox(final String label);

    PDateBox newPDateBox(final PDatePicker picker, final SimpleDateFormat dateFormat, final boolean keepDayTimeNeeded);

    PDateBox newPDateBox(final PDatePicker picker, final SimpleDateFormat dateFormat);

    PDateBox newPDateBox(final SimpleDateFormat dateFormat);

    PDateBox newPDateBox(final SimpleDateFormat dateFormat, final boolean keepDayTimeNeeded);

    PDateBox newPDateBox();

    PDatePicker newPDatePicker();

    PDecoratorPanel newPDecoratorPanel();

    PDecoratedPopupPanel newPDecoratedPopupPanel(final boolean autoHide);

    PDialogBox newPDialogBox();

    PDialogBox newPDialogBox(final boolean autoHide);

    PDisclosurePanel newPDisclosurePanel(final String headerText);

    PDockLayoutPanel newPDockLayoutPanel(final PUnit unit);

    PElement newPElement(final String tagName);

    PFileUpload newPFileUpload();

    PFlexTable newPFlexTable();

    PFlowPanel newPFlowPanel();

    PFocusPanel newPFocusPanel();

    PGrid newPGrid();

    PGrid newPGrid(final int rows, final int columns);

    PHeaderPanel newPHeaderPanel();

    PHorizontalPanel newPHorizontalPanel();

    PHTML newPHTML(final String html, final boolean wordWrap);

    PHTML newPHTML(final String html);

    PHTML newPHTML();

    PImage newPImage(final String url, final int left, final int top, final int width, final int height);

    PImage newPImage(final String url);

    PImage newPImage(final PImage.ClassPathURL classpathURL);

    PImage newPImage();

    PLabel newPLabel();

    PLabel newPLabel(final String text);

    PLayoutPanel newPLayoutPanel();

    PListBox newPListBox();

    PListBox newPListBox(final boolean containsEmptyItem);

    PMenuBar newPMenuBar();

    PMenuBar newPMenuBar(final boolean vertical);

    PMenuItem newPMenuItem(final String text, final boolean asHTML);

    PMenuItem newPMenuItem(final String text, final PMenuBar subMenu);

    PMenuItem newPMenuItem(final String text);

    PMenuItem newPMenuItem(final String text, final boolean asHTML, final Runnable cmd);

    PMenuItem newPMenuItem(final String text, final boolean asHTML, final PMenuBar subMenu);

    PMenuItem newPMenuItem(final String text, final Runnable cmd);

    PMenuItemSeparator newPMenuItemSeparator();

    PPasswordTextBox newPPasswordTextBox();

    PPasswordTextBox newPPasswordTextBox(final String text);

    PPopupPanel newPPopupPanel(final boolean autoHide);

    PPopupPanel newPPopupPanel();

    PPushButton newPPushButton(final PImage image);

    PRadioButton newPRadioButton();

    PRadioButton newPRadioButton(final String label);

    PRadioButtonGroup newPRadioButtonGroup(final String name);

    PRichTextArea newPRichTextArea();

    PRichTextToolbar newPRichTextToolbar(final PRichTextArea richTextArea);

    PScrollPanel newPScrollPanel();

    PSimpleLayoutPanel newPSimpleLayoutPanel();

    PSimplePanel newPSimplePanel();

    PSplitLayoutPanel newPSplitLayoutPanel();

    PStackLayoutPanel newPStackLayoutPanel(final PUnit unit);

    PSuggestBox newPSuggestBox();

    PSuggestBox newPSuggestBox(final PSuggestOracle suggestOracle);

    PTabLayoutPanel newPTabLayoutPanel();

    PTabPanel newPTabPanel();

    PTextArea newPTextArea();

    PTextArea newPTextArea(final String text);

    PTextBox newPTextBox();

    PTextBox newPTextBox(final String text);

    PTree newPTree();

    PTreeItem newPTreeItem(final String text);

    PTreeItem newPTreeItem(final PWidget widget);

    PTreeItem newPTreeItem();

    PVerticalPanel newPVerticalPanel();

    PWindow newPWindow(final boolean relative, final String url, final String name, final String features);

    PWindow newPWindow(final PWindow parentWindow, final boolean relative, final String url, final String name, final String features);

    PFrame newPFrame(final String url);

    PFunctionalLabel newPFunctionalLabel(final TextFunction textFunction);

    PFunctionalLabel newPFunctionalLabel(final TextFunction textFunction, final Object... args);

}
