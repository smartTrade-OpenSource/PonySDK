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
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import javax.json.JsonObject;

import com.ponysdk.core.Parser;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.server.basic.event.HasPChangeHandlers;
import com.ponysdk.ui.server.basic.event.PChangeEvent;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.Model;

/**
 * A widget that presents a list of choices to the user, either as a list box or as a drop-down list.
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-ListBox { }</li>
 * </ul>
 */
public class PListBox extends PFocusWidget implements HasPChangeHandlers, PChangeHandler {

    private static final String EMPTY = "";

    private static final String COMMA = ",";

    private List<PChangeHandler> handlers;

    private final List<ListItem> items = new ArrayList<>();

    private final Set<Integer> selectedIndexes = new TreeSet<>();

    protected int selectedIndex = -1;

    private boolean isMultipleSelect;

    private final boolean containsEmptyItem;

    private int visibleItemCount;

    public PListBox() {
        this(false);
    }

    public PListBox(final boolean containsEmptyItem) {
        init();

        this.containsEmptyItem = containsEmptyItem;

        if (containsEmptyItem) {
            addItem(EMPTY, null);
        }

        saveAddHandler(Model.HANDLER_CHANGE_HANDLER);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.LISTBOX;
    }

    @Override
    public void onClientData(final JsonObject jsonObject) {
        if (jsonObject.containsKey(Model.HANDLER_CHANGE_HANDLER.getKey())) {
            final String data = jsonObject.getString(Model.VALUE.getKey());
            final String[] tokens = data.split(COMMA);
            final List<Integer> selectedItems = new ArrayList<>();

            this.selectedIndex = Integer.parseInt(tokens[0]);

            for (final String index : tokens) {
                selectedItems.add(Integer.valueOf(index));
            }

            this.selectedIndexes.clear();
            this.selectedIndexes.addAll(selectedItems);

            onChange(new PChangeEvent(PListBox.this));
        } else {
            super.onClientData(jsonObject);
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

        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_ADD);
        parser.comma();
        parser.parse(Model.ITEM_ADD);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        final String s = items.toString();
        parser.parse(Model.ITEM_TEXT, s.substring(1, s.length() - 1).replaceAll(",", ";").replaceAll(" ", EMPTY));
        parser.comma();
        parser.parse(Model.ITEM_GROUP, group);
        parser.endObject();

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

        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(Model.ITEM_INSERTED);
        parser.comma();
        parser.parse(Model.INDEX, index);
        parser.comma();
        parser.parse(Model.ITEM_TEXT, label);
        parser.endObject();
    }

    public void setItemText(final int index, final String text) {
        checkIndex(index);

        items.get(index).label = text;

        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(Model.ITEM_UPDATED);
        parser.comma();
        parser.parse(Model.INDEX, index);
        parser.comma();
        parser.parse(Model.ITEM_TEXT, text);
        parser.endObject();
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
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(Model.ITEM_REMOVED);
        parser.comma();
        parser.parse(Model.INDEX, index);
        parser.endObject();

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

        saveUpdate(Model.CLEAR, true);

        if (containsEmptyItem) {
            addItem(EMPTY, null);
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

        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(Model.SELECTED, selected);
        parser.comma();
        parser.parse(Model.SELECTED_INDEX, index);
        parser.endObject();
    }

    public void setSelectedIndex(final int index) {
        setSelectedIndex(index, true);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public String getSelectedItem() {
        return selectedIndex >= 0 ? items.get(selectedIndex).label : null;
    }

    public Object getSelectedValue() {
        return selectedIndex >= 0 ? items.get(selectedIndex).value : null;
    }

    @Override
    public void addChangeHandler(final PChangeHandler handler) {
        if (handlers == null) handlers = new ArrayList<>();
        handlers.add(handler);
    }

    public boolean removeChangeHandler(final PChangeHandler handler) {
        return handlers != null ? handlers.remove(handler) : false;
    }

    @Override
    public Collection<PChangeHandler> getChangeHandlers() {
        return handlers != null ? Collections.unmodifiableCollection(handlers) : Collections.emptyList();
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
        if (handlers != null) {
            for (final PChangeHandler handler : handlers) {
                handler.onChange(event);
            }
        }
    }

    public boolean isEmptySelection() {
        return containsEmptyItem;
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
        return new ArrayList<>(selectedIndexes);
    }

    public List<String> getSelectedItems() {
        final List<String> items = new ArrayList<>();
        for (final Integer index : selectedIndexes) {
            items.add(this.items.get(index).label);
        }
        return items;
    }

    public List<Object> getSelectedValues() {
        final List<Object> values = new ArrayList<>();
        for (final Integer index : selectedIndexes) {
            values.add(this.items.get(index).value);
        }
        return values;
    }

    public void setVisibleItemCount(final int visibleItemCount) {
        this.visibleItemCount = visibleItemCount;
        saveUpdate(Model.VISIBLE_ITEM_COUNT, visibleItemCount);
    }

    public int getVisibleItemCount() {
        return visibleItemCount;
    }

    public boolean isMultipleSelect() {
        return isMultipleSelect;
    }

    public void setMultipleSelect(final boolean isMultipleSelect) {
        this.isMultipleSelect = isMultipleSelect;
        saveUpdate(Model.MULTISELECT, isMultipleSelect);
    }

    public class ListItem {

        protected String label;

        protected Object value;

        public ListItem(final String label, final Object value) {
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
