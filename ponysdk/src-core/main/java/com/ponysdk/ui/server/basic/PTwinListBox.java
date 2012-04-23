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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ponysdk.ui.server.basic.event.HasPChangeHandlers;
import com.ponysdk.ui.server.basic.event.PChangeEvent;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

public class PTwinListBox<T> extends PHorizontalPanel implements HasPChangeHandlers, PChangeHandler {

    private final List<PChangeHandler> changeHandlers = new ArrayList<PChangeHandler>();

    private final List<String> items = new ArrayList<String>();

    private final Map<String, Object> hiddenValueByItems = new HashMap<String, Object>();

    private final Map<Object, String> itemsByHiddenValue = new HashMap<Object, String>();

    private boolean enabled = true;

    private final String caption;

    private final Set<String> selectValues = new HashSet<String>();

    private PListBox selectedListBox;

    private PListBox unselectedListBox;

    private PButton switchButton;

    private PButton selectButton;

    private PButton unselectButton;

    private boolean multiButton;

    public PTwinListBox() {
        this(null, true, false);
    }

    public PTwinListBox(final String caption) {
        this(caption, true, false);
    }

    public PTwinListBox(final String caption, final boolean containsEmptyItem) {
        this(caption, containsEmptyItem, false);
    }

    public PTwinListBox(final String caption, final boolean containsEmptyItem, final boolean multiButton) {
        this.caption = caption;
        this.multiButton = multiButton;
        init(containsEmptyItem);
    }

    private void init(final boolean containsEmptyItem) {
        setVerticalAlignment(PVerticalAlignment.ALIGN_MIDDLE);
        selectedListBox = new PListBox(containsEmptyItem, true);
        unselectedListBox = new PListBox(containsEmptyItem, true);
        selectedListBox.addChangeHandler(this);
        unselectedListBox.addChangeHandler(this);
        setSpacing(5);

        for (final String item : items) {
            unselectedListBox.addItem(item);
        }
        setTitle("caption");
        add(unselectedListBox.asWidget());
        if (!multiButton) {
            switchButton = new PButton("<>");
            switchButton.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent clickEvent) {
                    selectValues.clear();
                    int start = 0;
                    if (containsEmptyItem) {
                        start++;
                    }
                    for (int i = start; i < unselectedListBox.getItemCount(); i++) {
                        if (unselectedListBox.isItemSelected(i)) {
                            selectValues.add(unselectedListBox.getItem(i));
                        }
                    }
                    for (int i = start; i < selectedListBox.getItemCount(); i++) {
                        if (!selectedListBox.isItemSelected(i)) {
                            selectValues.add(selectedListBox.getItem(i));
                        }
                    }
                    refresh();
                }
            });
            add(switchButton);
        } else {
            final PVerticalPanel buttonsPanel = new PVerticalPanel();
            buttonsPanel.setSpacing(5);
            selectButton = new PButton(">");
            selectButton.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent clickEvent) {
                    int start = 0;
                    if (containsEmptyItem) {
                        start++;
                    }
                    for (int i = start; i < unselectedListBox.getItemCount(); i++) {
                        if (unselectedListBox.isItemSelected(i)) {
                            selectValues.add(unselectedListBox.getItem(i));
                        }
                    }
                    refresh();
                }
            });
            buttonsPanel.add(selectButton);
            unselectButton = new PButton("<");
            unselectButton.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent clickEvent) {
                    int start = 0;
                    if (containsEmptyItem) {
                        start++;
                    }
                    for (int i = start; i < selectedListBox.getItemCount(); i++) {
                        if (selectedListBox.isItemSelected(i)) {
                            selectValues.remove(selectedListBox.getItem(i));
                        }
                    }
                    refresh();
                }
            });
            buttonsPanel.add(unselectButton);
            add(buttonsPanel);
        }
        final PWidget selectedWidget = selectedListBox.asWidget();
        add(selectedWidget);
        selectedListBox.setWidth("150px");
        unselectedListBox.setWidth("150px");
        setCellVerticalAlignment(selectedWidget, PVerticalAlignment.ALIGN_BOTTOM);
    }

    @Override
    public void clear() {
        unselectedListBox.clear();
        selectedListBox.clear();
        items.clear();
        hiddenValueByItems.clear();
        itemsByHiddenValue.clear();
    }

    public void addItem(final String item) {
        unselectedListBox.addItem(item);
        items.add(item);
    }

    public void addItem(final String item, final Object hiddenValue) {
        unselectedListBox.addItem(item);
        items.add(item);
        hiddenValueByItems.put(item, hiddenValue);
        itemsByHiddenValue.put(hiddenValue, item);
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        unselectedListBox.setEnabled(enabled);
        selectedListBox.setEnabled(enabled);
        if (multiButton) {
            selectButton.setEnabled(false);
            unselectButton.setEnabled(false);
        } else {
            switchButton.setEnabled(enabled);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    void refresh() {
        unselectedListBox.clear();
        selectedListBox.clear();
        unselectedListBox.setSelectedIndex(-1);
        selectedListBox.setSelectedIndex(-1);
        for (final String item : hiddenValueByItems.keySet()) {
            if (selectValues.contains(item)) {
                selectedListBox.addItem(item);
            } else {
                unselectedListBox.addItem(item);
            }
        }
    }

    public void setSelectedItem(final String text, final boolean selected) {

        final Object selectedValue = hiddenValueByItems.get(text);
        if (selectedValue != null) {
            if (selected) {
                if (selectValues.contains(text)) {
                    throw new IllegalArgumentException("Item '" + text + "' already selected for listbox '" + caption + "'");
                } else {
                    selectValues.add(text);
                    refresh();
                }
            } else {
                if (selectValues.contains(text)) {
                    selectValues.remove(text);
                    refresh();
                } else {
                    throw new IllegalArgumentException("Item '" + text + "' already unselected for listbox '" + caption + "'");
                }
            }
        } else throw new IllegalArgumentException("unknow Item '" + text + "' for listbox '" + caption + "'");
    }

    public void setSelectedItem(final String text) {
        setSelectedItem(text, true);
    }

    public void setSelectedValue(final Object value, final boolean selected) {
        final String item = itemsByHiddenValue.get(value);
        setSelectedItem(item, selected);
    }

    public void setSelectedValue(final Object value) {
        setSelectedValue(value, true);
    }

    @Override
    public void addChangeHandler(final PChangeHandler handler) {
        changeHandlers.add(handler);
    }

    @Override
    public Collection<PChangeHandler> getChangeHandlers() {
        return changeHandlers;
    }

    public List<T> getValue() {
        final ArrayList<T> values = new ArrayList<T>();
        for (final String selectedItem : selectValues) {
            @SuppressWarnings("unchecked")
            final T t = (T) hiddenValueByItems.get(selectedItem);
            values.add(t);
        }
        return values;
    }

    @Override
    public void onChange(final PChangeEvent event) {
        for (final PChangeHandler changeHandler : changeHandlers) {
            changeHandler.onChange(event);
        }
    }

    public void setValue(final Object value) {
        setSelectedValue(value);
    }

}
