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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final Map<String, Object> valueByItems = new HashMap<String, Object>();

    private final List<String> items = new ArrayList<String>();

    private List<Integer> selectedItems = new ArrayList<Integer>();

    private int selectedIndex = -1;

    private final boolean containsEmptyItem;

    private final boolean isMultipleSelect;

    public PListBox() {
        this(true, false);
    }

    public PListBox(boolean containsEmptyItem, boolean isMultipleSelect) {
        this.containsEmptyItem = containsEmptyItem;
        this.isMultipleSelect = isMultipleSelect;

        if (containsEmptyItem) {
            addItem("", null);
        }
        // comment....
        final AddHandler addHandler = new AddHandler(getID(), HandlerType.CHANGE_HANDLER);

        getPonySession().stackInstruction(addHandler);

        final Property mainProperty = new Property(PropertyKey.MULTISELECT, isMultipleSelect);
        setMainProperty(mainProperty);
    }

    @Override
    public void onEventInstruction(EventInstruction instruction) {
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

    public void addItem(String item) {
        insertItem(item, item, items.size());
    }

    public void addItem(String item, Object value) {
        insertItem(item, value, items.size());
    }

    public void insertItem(String item, int index) {
        insertItem(item, item, index);
    }

    public void insertItem(String item, Object value, int index) {

        final int itemCount = getItemCount();
        if (index < 0 || index > itemCount) {
            index = itemCount;
        }

        valueByItems.put(item, value);
        items.add(index, item);

        final Update update = new Update(getID());

        final Property property = new Property(PropertyKey.ITEM_INSERTED);
        property.setProperty(PropertyKey.INDEX, index);
        property.setProperty(PropertyKey.ITEM_TEXT, item);
        property.setProperty(PropertyKey.VALUE, value == null ? null : value.toString());

        update.setMainProperty(property);

        getPonySession().stackInstruction(update);
    }

    public void setItemText(int index, String text) {
        checkIndex(index);

        items.set(index, text);
        final Object value = valueByItems.get(text);
        valueByItems.put(text, value);

        final Update update = new Update(getID());

        final Property property = new Property(PropertyKey.ITEM_TEXT);
        property.setProperty(PropertyKey.INDEX, index);
        property.setProperty(PropertyKey.ITEM_TEXT, text);
        update.setMainProperty(property);
        getPonySession().stackInstruction(update);
    }

    public void setValue(int index, String value) {
        checkIndex(index);
        valueByItems.put(items.get(index), value);
        final Update update = new Update(getID());
        final Property property = new Property(PropertyKey.VALUE);
        property.setProperty(PropertyKey.INDEX, index);
        property.setProperty(PropertyKey.VALUE, value);
        update.setMainProperty(property);
        getPonySession().stackInstruction(update);
    }

    public void removeItem(int index) {
        checkIndex(index);
        valueByItems.remove(items.remove(index));
        sendRemoveItemInstruction(index);
    }

    // TODO nciaravola add a map
    public void removeItem(String item) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).equals(item)) {
                valueByItems.remove(items.remove(i));
                sendRemoveItemInstruction(i);
                return;
            }
        }
    }

    private void sendRemoveItemInstruction(int index) {
        final Update update = new Update(getID());

        final Property property = new Property(PropertyKey.ITEM_REMOVED);
        property.setProperty(PropertyKey.INDEX, index);

        update.setMainProperty(property);

        getPonySession().stackInstruction(update);
    }

    public Object getValue(int index) {
        return valueByItems.get(items.get(index));
    }

    public void clear() {
        selectedIndex = -1;
        valueByItems.clear();
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
        return valueByItems.size();
    }

    public void setSelectedIndex(int index, boolean selected) {
        checkIndex(index);
        this.selectedIndex = index;
        final Update update = new Update(getID());
        final Property property = new Property(PropertyKey.SELECTED, selected);
        property.setProperty(PropertyKey.SELECTED_INDEX, index);
        update.setMainProperty(property);
        getPonySession().stackInstruction(update);
    }

    public void setSelectedIndex(int index) {
        setSelectedIndex(index, true);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public String getSelectedItem() {
        if (selectedIndex < 0) return null;
        return items.get(selectedIndex);
    }

    public Object getSelectedValue() {
        if (selectedIndex < 0) return null;
        final String item = items.get(selectedIndex);
        if (item != null) { return valueByItems.get(item); }
        return null;
    }

    @Override
    public void addChangeHandler(PChangeHandler handler) {
        handlers.add(handler);
    }

    public boolean removeChangeHandler(PChangeHandler handler) {
        return handlers.remove(handler);
    }

    @Override
    public Collection<PChangeHandler> getChangeHandlers() {
        return Collections.unmodifiableCollection(handlers);
    }

    public void setSelectedItem(String item, boolean selected) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).equals(item)) {
                setSelectedIndex(i, selected);
                break;
            }
        }
    }

    public void setSelectedItem(String item) {
        setSelectedItem(item, true);
    }

    @Override
    public void onChange(Object source, final int selectedIndex) {
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

    public boolean isItemSelected(int index) {
        return selectedItems.contains(index);
    }

    public String getItem(int index) {
        return items.get(index);
    }

    // TODO nciaravola must be package
    public void syncSelectedItems(List<Integer> selectedItems) {
        this.selectedItems = selectedItems;
    }

    private void checkIndex(int index) {
        if (index >= getItemCount()) throw new IndexOutOfBoundsException();
    }

    public List<Integer> getSelectedItems() {
        return selectedItems;
    }

}
