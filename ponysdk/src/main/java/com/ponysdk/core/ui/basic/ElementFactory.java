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

public interface ElementFactory {

    public PAbsolutePanel newPAbsolutePanel();

    public PAnchor newPAnchor(final String text, final String href);

    public PAnchor newPAnchor(final String text);

    public PAnchor newPAnchor();

    public PButton newPButton(final String text, final String html);

    public PButton newPButton(final String text);

    public PButton newPButton();

    public PCheckBox newPCheckBox();

    public PCheckBox newPCheckBox(final String label);

    public PDateBox newPDateBox(final PDatePicker picker, final SimpleDateFormat dateFormat);

    public PDateBox newPDateBox(final SimpleDateFormat dateFormat);

    public PDateBox newPDateBox();

    public PDatePicker newPDatePicker();

    public PDecoratedPopupPanel newPDecoratedPopupPanel(int id, boolean b);

    public PDecoratorPanel newPDecoratorPanel();

    public PDialogBox newPDialogBox(final int windowID);

    public PDialogBox newPDialogBox(final int windowID, final boolean autoHide);

    public PDisclosurePanel newPDisclosurePanel(final String headerText);

    public PDockLayoutPanel newPDockLayoutPanel(final PUnit unit);

    public PElement newPElement(final String tagName);

    public PFileUpload newPFileUpload();

    public PFlexTable newPFlexTable();

    public PFlowPanel newPFlowPanel();

    public PFocusPanel newPFocusPanel();

    public PGrid newPGrid();

    public PGrid newPGrid(final int rows, final int columns);

    public PHeaderPanel newPHeaderPanel();

    public PHorizontalPanel newPHorizontalPanel();

    public PHTML newPHTML(final String html, final boolean wordWrap);

    public PHTML newPHTML(final String html);

    public PHTML newPHTML();

    public PImage newPImage(final String url, final int left, final int top, final int width, final int height);

    public PImage newPImage(final String url);

    public PImage newPImage(final PImage.ClassPathURL classpathURL);

    public PImage newPImage();

    public PLabel newPLabel();

    public PLabel newPLabel(final String text);

    public PLayoutPanel newPLayoutPanel();

    public PListBox newPListBox();

    public PListBox newPListBox(final boolean containsEmptyItem);

    public PMenuBar newPMenuBar();

    public PMenuBar newPMenuBar(final boolean vertical);

    public PMenuItem newPMenuItem(final String text, final boolean asHTML);

    public PMenuItem newPMenuItem(final String text, final PMenuBar subMenu);

    public PMenuItem newPMenuItem(final String text);

    public PMenuItem newPMenuItem(final String text, final boolean asHTML, final Runnable cmd);

    public PMenuItem newPMenuItem(final String text, final boolean asHTML, final PMenuBar subMenu);

    public PMenuItem newPMenuItem(final String text, final Runnable cmd);

    public PMenuItemSeparator newPMenuItemSeparator();

    public PPasswordTextBox newPPasswordTextBox();

    public PPasswordTextBox newPPasswordTextBox(final String text);

    public PPopupPanel newPPopupPanel(final int windowID, final boolean autoHide);

    public PPopupPanel newPPopupPanel(final int windowID);

    public PPushButton newPPushButton(final PImage image);

    public PRadioButton newPRadioButton();

    public PRadioButton newPRadioButton(final String label);

    public PRadioButtonGroup newPRadioButtonGroup(final String name);

    public PRichTextArea newPRichTextArea();

    public PRichTextToolbar newPRichTextToolbar(final PRichTextArea richTextArea);

    public PScrollPanel newPScrollPanel();

    public PSimpleLayoutPanel newPSimpleLayoutPanel();

    public PSimplePanel newPSimplePanel();

    public PSplitLayoutPanel newPSplitLayoutPanel();

    public PStackLayoutPanel newPStackLayoutPanel(final PUnit unit);

    public PSuggestBox newPSuggestBox();

    public PSuggestBox newPSuggestBox(final PSuggestOracle suggestOracle);

    public PTabLayoutPanel newPTabLayoutPanel();

    public PTabPanel newPTabPanel();

    public PTextArea newPTextArea();

    public PTextArea newPTextArea(final String text);

    public PTextBox newPTextBox();

    public PTextBox newPTextBox(final String text);

    public PTree newPTree();

    public PTreeItem newPTreeItem(final String html);

    public PTreeItem newPTreeItem(final PWidget widget);

    public PTreeItem newPTreeItem();

    public PVerticalPanel newPVerticalPanel();

    public PWindow newPWindow(final String url, final String name, final String features);

}
