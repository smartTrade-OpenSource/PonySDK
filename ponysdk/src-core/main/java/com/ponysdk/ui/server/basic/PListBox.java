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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import com.ponysdk.core.instruction.AddHandler;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.core.tools.Objects;
import com.ponysdk.ui.server.basic.event.HasPChangeHandlers;
import com.ponysdk.ui.server.basic.event.PChangeEvent;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * A widget that presents a list of choices to the user, either as a list box or as a drop-down list. <h3>CSS
 * Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-ListBox { }</li>
 * </ul>
 */
public class PListBox extends PFocusWidget implements HasPChangeHandlers, PChangeHandler {

    private static final String COMMA = ",";

    private final List<PChangeHandler> handlers = new ArrayList<PChangeHandler>();

    private final List<ListItem> items = new ArrayList<ListItem>();

    private final Set<Integer> selectedIndexes = new TreeSet<Integer>();

    protected int selectedIndex = -1;

    private final boolean isMultipleSelect;

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

        final AddHandler addHandler = new AddHandler(getID(), HANDLER.KEY_.CHANGE_HANDLER);
        Txn.get().getTxnContext().save(addHandler);

        create.put(PROPERTY.MULTISELECT, isMultipleSelect);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.LISTBOX;
    }

    @Override
    public void onClientData(final JSONObject instruction) throws JSONException {
        if (instruction.getString(HANDLER.KEY).contains(HANDLER.KEY_.CHANGE_HANDLER)) {
            final String data = instruction.getString(PROPERTY.VALUE);
            final String[] tokens = data.split(COMMA);
            final List<Integer> selectedItems = new ArrayList<Integer>();

            this.selectedIndex = Integer.parseInt(tokens[0]);

            for (final String index : tokens) {
                selectedItems.add(Integer.valueOf(index));
            }

            this.selectedIndexes.clear();
            this.selectedIndexes.addAll(selectedItems);

            onChange(new PChangeEvent(PListBox.this));
        } else {
            super.onClientData(instruction);
        }
    }

    public void addItemsInGroup(final String group, final String... items) {
        addItemsInGroup(group, Arrays.asList(items));
    }

    public void addItemsInGroup(final String group, final List<String> items) {
        final ListItem groupItem = new ListGroupItem(group);
        this.items.add(groupItem);
        for (final String i : items) {
            this.items.add(new ListItem(i, i));
        }
        final Update update = new Update(getID());
        update.put(PROPERTY.ITEM_ADD);
        final String s = items.toString();
        update.put(PROPERTY.ITEM_TEXT, s.substring(1, s.length() - 1).replaceAll(",", ";").replaceAll(" ", ""));
        update.put(PROPERTY.ITEM_GROUP, group);
        Txn.get().getTxnContext().save(update);
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
        checkItem(label);

        final int itemCount = getItemCount();
        if (index < 0 || index > itemCount) {
            index = itemCount;
        }

        final ListItem item = new ListItem(label, value);

        items.add(index, item);

        final Update update = new Update(getID());
        update.put(PROPERTY.ITEM_INSERTED);
        update.put(PROPERTY.INDEX, index);
        update.put(PROPERTY.ITEM_TEXT, label);
        Txn.get().getTxnContext().save(update);
    }

    public void setItemText(final int index, final String text) {
        checkIndex(index);

        items.get(index).label = text;

        final Update update = new Update(getID());
        update.put(PROPERTY.ITEM_UPDATED);
        update.put(PROPERTY.INDEX, index);
        update.put(PROPERTY.ITEM_TEXT, text);

        Txn.get().getTxnContext().save(update);
    }

    public void setValue(final int index, final Object value) {
        checkIndex(index);
        items.get(index).value = value;
    }

    public ListItem removeItem(final int index) {
        checkIndex(index);
        selectedIndexes.remove(index);
        final ListItem removedItem = items.remove(index);
        sendRemoveItemInstruction(index);
        return removedItem;
    }

    public void removeItem(final String label) {
        checkItem(label);
        int currentIndex = 0;
        for (final Iterator<ListItem> iterator = items.iterator(); iterator.hasNext();) {
            final ListItem item = iterator.next();
            if (Objects.equals(item.label, label)) {
                selectedIndexes.remove(currentIndex);
                iterator.remove();
                sendRemoveItemInstruction(currentIndex);
            } else {
                currentIndex++;
            }
        }
    }

    public void removeValue(final Object value) {
        int currentIndex = 0;
        for (final Iterator<ListItem> iterator = items.iterator(); iterator.hasNext();) {
            final ListItem item = iterator.next();
            if (Objects.equals(item.value, value)) {
                selectedIndexes.remove(currentIndex);
                iterator.remove();
                sendRemoveItemInstruction(currentIndex);
            } else {
                currentIndex++;
            }
        }
    }

    private void sendRemoveItemInstruction(final int index) {
        final Update update = new Update(getID());

        update.put(PROPERTY.ITEM_REMOVED);
        update.put(PROPERTY.INDEX, index);
        Txn.get().getTxnContext().save(update);

        if (selectedIndex >= index) setSelectedIndex((selectedIndex - 1));
    }

    public Object getValue(final int index) {
        checkIndex(index);
        return items.get(index).value;
    }

    public void clear() {
        selectedIndex = -1;
        items.clear();
        selectedIndexes.clear();
        final Update update = new Update(getID());
        update.put(PROPERTY.CLEAR, true);
        Txn.get().getTxnContext().save(update);

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

        if (isMultipleSelect && selected) {
            selectedIndexes.add(index);
        } else {
            selectedIndexes.remove(index);
        }

        final Update update = new Update(getID());
        update.put(PROPERTY.SELECTED, selected);
        update.put(PROPERTY.SELECTED_INDEX, index);
        Txn.get().getTxnContext().save(update);
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
            if (items.get(i).label.equals(item)) {
                setSelectedIndex(i, selected);
                break;
            }
        }
    }

    public void setSelectedValue(final Object value, final boolean selected) {
        for (int i = 0; i < items.size(); i++) {
            if (value.equals(items.get(i).value)) {
                setSelectedIndex(i, selected);
                break;
            }
        }
    }

    public void setSelectedItem(final String item) {
        setSelectedItem(item, true);
    }

    public void setSelectedValue(final Object value) {
        setSelectedValue(value, true);
    }

    @Override
    public void onChange(final PChangeEvent event) {
        for (final PChangeHandler handler : handlers) {
            handler.onChange(event);
        }
    }

    public boolean isEmptySelection() {
        return containsEmptyItem;
    }

    public boolean isMultipleSelect() {
        return isMultipleSelect;
    }

    public boolean isItemSelected(final int index) {
        return selectedIndexes.contains(index);
    }

    public String getItem(final int index) {
        return items.get(index).label;
    }

    private void checkItem(final String label) {
        if (label == null) throw new NullPointerException("Null item is unsupported");
    }

    private void checkIndex(final int index) {
        if (index >= getItemCount()) throw new IndexOutOfBoundsException();
    }

    public List<Integer> getSelectedIndexes() {
        return new ArrayList<Integer>(selectedIndexes);
    }

    public List<String> getSelectedItems() {
        final List<String> items = new ArrayList<String>();
        for (final Integer index : selectedIndexes) {
            items.add(this.items.get(index).label);
        }
        return items;
    }

    public List<Object> getSelectedValues() {
        final List<Object> values = new ArrayList<Object>();
        for (final Integer index : selectedIndexes) {
            values.add(this.items.get(index).value);
        }
        return values;
    }

    public void setVisibleItemCount(final int visibleItemCount) {
        this.visibleItemCount = visibleItemCount;
        final Update update = new Update(getID());
        update.put(PROPERTY.VISIBLE_ITEM_COUNT, visibleItemCount);
        Txn.get().getTxnContext().save(update);
    }

    public int getVisibleItemCount() {
        return visibleItemCount;
    }

    public class ListItem {

        protected String label;

        protected Object value;

        public ListItem(final String label, final Object value) {
            super();
            this.label = label;
            this.value = value;
        }

    }

    public class ListGroupItem extends ListItem {

        public ListGroupItem(final String group) {
            super(group, null);
        }
    }

}
