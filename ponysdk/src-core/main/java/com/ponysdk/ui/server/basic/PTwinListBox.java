/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.ui.server.basic;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.ui.server.basic.PListBox.ListItem;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class PTwinListBox<T> extends PFlowPanel {

    private static final Logger log = LoggerFactory.getLogger(PTwinListBox.class);

    private boolean enabled = true;

    private final String leftCaption;

    private final String rightCaption;

    private PListBox leftListBox;

    private PListBox rightListBox;

    private PButton switchButton;

    private PButton leftToRightButton;

    private PButton rightToLeftButton;

    private boolean multiButton;

    public PTwinListBox() {
        this(null, null, false, false);
    }

    public PTwinListBox(final String leftCaption, final String rightCaption) {
        this(leftCaption, rightCaption, true, false);
    }

    public PTwinListBox(final String leftCaption, final String rightCaption, final boolean containsEmptyItem) {
        this(rightCaption, rightCaption, containsEmptyItem, false);
    }

    public PTwinListBox(final String leftCaption, final String rightCaption, final boolean containsEmptyItem, final boolean multiButton) {
        this.leftCaption = leftCaption;
        this.rightCaption = rightCaption;
        this.multiButton = multiButton;
        init(containsEmptyItem);
    }

    private void init(final boolean containsEmptyItem) {
        leftListBox = new PListBox(containsEmptyItem, true);
        rightListBox = new PListBox(containsEmptyItem, true);

        add(leftListBox);

        if (!multiButton) {
            switchButton = new PButton("<>");
            switchButton.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent clickEvent) {
                    final List<ListItem> leftRemovedItems = new ArrayList<ListItem>();
                    for (int i = leftListBox.getItemCount(); i > 0; i--) {
                        if (leftListBox.isItemSelected(i - 1)) {
                            leftRemovedItems.add(leftListBox.removeItem(i - 1));
                        }
                    }

                    final List<ListItem> rightRemovedItems = new ArrayList<ListItem>();
                    for (int i = rightListBox.getItemCount(); i > 0; i--) {
                        if (rightListBox.isItemSelected(i - 1)) {
                            rightRemovedItems.add(rightListBox.removeItem(i - 1));
                        }
                    }

                    for (int i = leftRemovedItems.size() - 1; i >= 0; i--) {
                        final ListItem listItem = leftRemovedItems.get(i);
                        rightListBox.addItem(listItem.label, listItem.value);
                    }
                    for (int i = rightRemovedItems.size() - 1; i >= 0; i--) {
                        final ListItem listItem = rightRemovedItems.get(i);
                        leftListBox.addItem(listItem.label, listItem.value);
                    }
                }
            });
            add(switchButton);
        } else {
            final PFlowPanel buttonsPanel = new PFlowPanel();
            leftToRightButton = new PButton(">");
            leftToRightButton.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent clickEvent) {
                    int start = 0;
                    if (containsEmptyItem) {
                        start++;
                    }
                    for (int i = start; i < rightListBox.getItemCount(); i++) {
                        if (rightListBox.isItemSelected(i)) {
                            // selectValues.add(rightListBox.getItem(i));
                        }
                    }
                    // refresh();
                }
            });
            buttonsPanel.add(leftToRightButton);
            rightToLeftButton = new PButton("<");
            rightToLeftButton.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent clickEvent) {
                    int start = 0;
                    if (containsEmptyItem) {
                        start++;
                    }
                    for (int i = start; i < leftListBox.getItemCount(); i++) {
                        if (leftListBox.isItemSelected(i)) {
                            // selectValues.remove(leftListBox.getItem(i));
                        }
                    }
                    // refresh();
                }
            });
            buttonsPanel.add(rightToLeftButton);
            add(buttonsPanel);
        }
        add(rightListBox);
    }

    @Override
    public void clear() {
        rightListBox.clear();
        leftListBox.clear();
    }

    public PListBox getLeftListBox() {
        return leftListBox;
    }

    public PListBox getRightListBox() {
        return rightListBox;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        rightListBox.setEnabled(enabled);
        leftListBox.setEnabled(enabled);
        if (multiButton) {
            leftToRightButton.setEnabled(false);
            rightToLeftButton.setEnabled(false);
        } else {
            switchButton.setEnabled(enabled);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    // void refresh() {
    // rightListBox.clear();
    // leftListBox.clear();
    // rightListBox.setSelectedIndex(-1);
    // leftListBox.setSelectedIndex(-1);
    // for (final String item : hiddenValueByItems.keySet()) {
    // if (selectValues.contains(item)) {
    // leftListBox.addItem(item);
    // } else {
    // rightListBox.addItem(item);
    // }
    // }
    // }
    //
    // public void setSelectedItem(final String text, final boolean selected) {
    //
    // final Object selectedValue = hiddenValueByItems.get(text);
    // if (selectedValue != null) {
    // if (selected) {
    // if (selectValues.contains(text)) {
    // // log.warn(arg0);
    // // throw new IllegalArgumentException("Item '" + text + "' already selected for listbox '"
    // // + caption + "'");
    // } else {
    // selectValues.add(text);
    // refresh();
    // }
    // } else {
    // if (selectValues.contains(text)) {
    // selectValues.remove(text);
    // refresh();
    // } else {
    // // throw new IllegalArgumentException("Item '" + text +
    // // "' already unselected for listbox '" + caption + "'");
    // }
    // }
    // } else {
    // // throw new IllegalArgumentException("unknow Item '" + text + "' for listbox '" + caption + "'");
    // }
    // }
    //
    // public void setSelectedItem(final String text) {
    // setSelectedItem(text, true);
    // }
    //
    // public void setSelectedValue(final Object value, final boolean selected) {
    // final String item = itemsByHiddenValue.get(value);
    // setSelectedItem(item, selected);
    // }
    //
    // public void setSelectedValue(final Object value) {
    // setSelectedValue(value, true);
    // }
    //
    // public List<T> getValue() {
    // final List<T> values = new ArrayList<T>();
    // for (final String selectedItem : selectValues) {
    // @SuppressWarnings("unchecked")
    // final T t = (T) hiddenValueByItems.get(selectedItem);
    // values.add(t);
    // }
    // return values;
    // }
    //
    // public void setValue(final Object value) {
    // setSelectedValue(value);
    // }

}
