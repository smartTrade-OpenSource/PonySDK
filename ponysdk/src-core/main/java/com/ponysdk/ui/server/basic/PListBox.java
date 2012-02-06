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

package com.ponysdk.ui.server.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.ponysdk.ui.server.basic.event.HasPChangeHandlers;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PListBox extends PFocusWidget implements HasPChangeHandlers, PChangeHandler {

    private final List<PChangeHandler> handlers = new ArrayList<PChangeHandler>();

    private final List<ListItem> items = new ArrayList<ListItem>();

    private List<Integer> selectedItems = new ArrayList<Integer>();

    private int selectedIndex = -1;

    private boolean isMultipleSelect;

    private final boolean containsEmptyItem;

    private int visibleItemCount;

    public PListBox() {
        this(false, false);
    }

    public PListBox(final boolean containsEmptyItem) {
        this(containsEmptyItem, false);
    }

    public PListBox(final boolean containsEmptyItem, final boolean isMultipleSelect) {
        this.containsEmptyItem = containsEmptyItem;
        this.isMultipleSelect = isMultipleSelect;

        if (containsEmptyItem) {
            addItem("", null);
        }

        final AddHandler addHandler = new AddHandler(getID(), HandlerType.CHANGE_HANDLER);

        getPonySession().stackInstruction(addHandler);

        setMainProperty(new Property(PropertyKey.MULTISELECT, isMultipleSelect));
    }

    @Override
    public void onEventInstruction(final EventInstruction instruction) {
        if (HandlerType.CHANGE_HANDLER.equals(instruction.getHandlerType())) {
            final String data = instruction.getMainProperty().getValue();
            final String[] tokens = data.split(",");
            Integer selectedItemIndex = null;
            final List<Integer> selectedItems = new ArrayList<Integer>();
            for (final String index : tokens) {
                if (selectedItemIndex == null) {
                    selectedItemIndex = Integer.valueOf(index);
                    selectedItems.add(selectedItemIndex);
                } else selectedItems.add(Integer.valueOf(index));
            }
            syncSelectedItems(selectedItems);
            onChange(this, selectedItemIndex);
        } else {
            super.onEventInstruction(instruction);
        }
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.LISTBOX;
    }

    public void addItem(final String item) {
        insertItem(item, item, items.size());
    }

    public void addItem(final String item, final Object value) {
        insertItem(item, value, items.size());
    }

    public void insertItem(final String item, final int index) {
        insertItem(item, item, index);
    }

    public void insertItem(final String label, final Object value, int index) {

        final int itemCount = getItemCount();
        if (index < 0 || index > itemCount) {
            index = itemCount;
        }

        ListItem item = new ListItem(label, value);

        items.add(index, item);

        final Update update = new Update(getID());

        final Property property = new Property(PropertyKey.ITEM_INSERTED);
        property.setProperty(PropertyKey.INDEX, index);
        property.setProperty(PropertyKey.ITEM_TEXT, label);
        property.setProperty(PropertyKey.VALUE, value == null ? null : value.toString());

        update.setMainProperty(property);

        getPonySession().stackInstruction(update);
    }

    public void setItemText(final int index, final String text) {
        checkIndex(index);

        items.get(index).label = text;

        final Update update = new Update(getID());
        final Property property = new Property(PropertyKey.ITEM_TEXT);
        property.setProperty(PropertyKey.INDEX, index);
        property.setProperty(PropertyKey.ITEM_TEXT, text);
        update.setMainProperty(property);
        getPonySession().stackInstruction(update);
    }

    public void setValue(final int index, final String value) {
        checkIndex(index);

        items.get(index).value = value;

        final Update update = new Update(getID());
        final Property property = new Property(PropertyKey.VALUE);
        property.setProperty(PropertyKey.INDEX, index);
        property.setProperty(PropertyKey.VALUE, value);
        update.setMainProperty(property);
        getPonySession().stackInstruction(update);
    }

    public void removeItem(final int index) {
        checkIndex(index);

        items.remove(index);
        sendRemoveItemInstruction(index);
    }

    public void removeItem(final String label) {
        int currentIndex = 0;
        for (Iterator<ListItem> iterator = items.iterator(); iterator.hasNext();) {
            ListItem item = iterator.next();
            if (item.label.equals(label)) {
                iterator.remove();
                sendRemoveItemInstruction(currentIndex);
            } else {
                currentIndex++;
            }
        }
    }

    public void removeItem(final Object value) {
        int currentIndex = 0;
        for (Iterator<ListItem> iterator = items.iterator(); iterator.hasNext();) {
            ListItem item = iterator.next();
            if (item.value.equals(value)) {
                iterator.remove();
                sendRemoveItemInstruction(currentIndex);
            } else {
                currentIndex++;
            }
        }
    }

    private void sendRemoveItemInstruction(final int index) {
        final Update update = new Update(getID());

        final Property property = new Property(PropertyKey.ITEM_REMOVED);
        property.setProperty(PropertyKey.INDEX, index);
        update.setMainProperty(property);
        getPonySession().stackInstruction(update);
    }

    public Object getValue(final int index) {
        checkIndex(index);
        return items.get(index).value;
    }

    public void clear() {
        selectedIndex = -1;
        items.clear();
        selectedItems.clear();
        final Update update = new Update(getID());
        final Property property = new Property(PropertyKey.CLEAR, true);
        update.setMainProperty(property);
        getPonySession().stackInstruction(update);

        if (containsEmptyItem) {
            addItem("", null);
        }
    }

    public int getItemCount() {
        return items.size();
    }

    public void setSelectedIndex(final int index, final boolean selected) {
        checkIndex(index);
        this.selectedIndex = index;
        final Update update = new Update(getID());
        final Property property = new Property(PropertyKey.SELECTED, selected);
        property.setProperty(PropertyKey.SELECTED_INDEX, index);
        update.setMainProperty(property);
        getPonySession().stackInstruction(update);
    }

    public void setSelectedIndex(final int index) {
        setSelectedIndex(index, true);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public String getSelectedItem() {
        if (selectedIndex < 0) return null;
        return items.get(selectedIndex).label;
    }

    public Object getSelectedValue() {
        if (selectedIndex < 0) return null;
        return items.get(selectedIndex).value;
    }

    @Override
    public void addChangeHandler(final PChangeHandler handler) {
        handlers.add(handler);
    }

    public boolean removeChangeHandler(final PChangeHandler handler) {
        return handlers.remove(handler);
    }

    @Override
    public Collection<PChangeHandler> getChangeHandlers() {
        return Collections.unmodifiableCollection(handlers);
    }

    public void setSelectedItem(final String item, final boolean selected) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).equals(item)) {
                setSelectedIndex(i, selected);
                break;
            }
        }
    }

    public void setSelectedItem(final String item) {
        setSelectedItem(item, true);
    }

    @Override
    public void onChange(final Object source, final int selectedIndex) {
        this.selectedIndex = selectedIndex;
        for (final PChangeHandler handler : handlers) {
            handler.onChange(source, selectedIndex);
        }
    }

    public boolean isEmptySelection() {
        return containsEmptyItem;
    }

    public boolean isMultipleSelect() {
        return isMultipleSelect;
    }

    public boolean isItemSelected(final int index) {
        return selectedItems.contains(index);
    }

    public String getItem(final int index) {
        return items.get(index).label;
    }

    // TODO nciaravola must be package
    public void syncSelectedItems(final List<Integer> selectedItems) {
        this.selectedItems = selectedItems;
    }

    private void checkIndex(final int index) {
        if (index >= getItemCount()) throw new IndexOutOfBoundsException();
    }

    public List<Integer> getSelectedItems() {
        return selectedItems;
    }

    public void setVisibleItemCount(final int visibleItemCount) {
        this.visibleItemCount = visibleItemCount;
        final Update update = new Update(getID());
        update.setMainPropertyValue(PropertyKey.VISIBLE_ITEM_COUNT, visibleItemCount);
        getPonySession().stackInstruction(update);
    }

    public int getVisibleItemCount() {
        return visibleItemCount;
    }

    public void setMultiSelect(final Boolean isMultipleSelect) {
        this.isMultipleSelect = isMultipleSelect;
        final Update update = new Update(getID());
        update.setMainPropertyValue(PropertyKey.MULTISELECT, isMultipleSelect);
        getPonySession().stackInstruction(update);
    }

    private class ListItem {

        protected String label;

        protected Object value;

        public ListItem(final String label, final Object value) {
            super();
            this.label = label;
            this.value = value;
        }

    }

}
