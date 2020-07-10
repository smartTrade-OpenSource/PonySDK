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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.PListBox.ListItem;
import com.ponysdk.core.ui.basic.event.HasPChangeHandlers;
import com.ponysdk.core.ui.basic.event.PChangeEvent;
import com.ponysdk.core.ui.basic.event.PChangeHandler;

/**
 * A widget that presents a list of choices to the user, either as a list box or
 * as a drop-down list.
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-ListBox { }</li>
 * </ul>
 */
public class PListBox extends PFocusWidget implements HasPChangeHandlers, PChangeHandler, Iterable<ListItem> {

    private static final String EMPTY = "";

    private static final String COMMA = ",";

    private final List<PChangeHandler> handlers = new ArrayList<>();

    private final List<ListItem> items = new ArrayList<>();

    private final Set<Integer> selectedIndexes = new TreeSet<>();
    private final boolean containsEmptyItem;
    protected int selectedIndex = -1;
    private boolean isMultipleSelect;
    private int visibleItemCount;

    protected PListBox() {
        this(false);
    }

    protected PListBox(final boolean containsEmptyItem) {
        this.containsEmptyItem = containsEmptyItem;
        if (this.containsEmptyItem) addItem(EMPTY, null);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.LISTBOX;
    }

    @Override
    public void onClientData(final JsonObject jsonObject) {
        if (!isVisible() || !isEnabled()) return;
        if (jsonObject.containsKey(ClientToServerModel.HANDLER_CHANGE.toStringValue())) {
            final String data = jsonObject.getString(ClientToServerModel.HANDLER_CHANGE.toStringValue());
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

        items.forEach(item -> this.items.add(new ListItem(item, item)));

        saveUpdate(writer -> {
            writer.write(ServerToClientModel.ITEM_ADD, items.toArray(new String[items.size()]));
            writer.write(ServerToClientModel.ITEM_GROUP, group);
        });
    }

    public void addItem(final String item) {
        addItem(item, item);
    }

    public void addItem(final String label, final Object value) {
        checkItem(label);

        final ListItem item = new ListItem(label, value);
        items.add(item);

        saveUpdate(writer -> writer.write(ServerToClientModel.ITEM_INSERTED, label));
    }

    public void insertItem(final String item, final int index) {
        insertItem(item, item, index);
    }

    public void insertItem(final String label, final Object value, int index) {
        checkItem(label);

        final int itemCount = getItemCount();
        if (index < 0 || index > itemCount) index = itemCount;

        final ListItem item = new ListItem(label, value);

        items.add(index, item);

        final int indexFinal = index;
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.ITEM_INSERTED, label);
            writer.write(ServerToClientModel.INDEX, indexFinal);
        });
    }

    public void setItemText(final int index, final String text) {
        checkIndex(index);

        items.get(index).label = text;

        saveUpdate(writer -> {
            writer.write(ServerToClientModel.ITEM_UPDATED, text);
            writer.write(ServerToClientModel.INDEX, index);
        });
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
        saveUpdate(writer -> writer.write(ServerToClientModel.ITEM_REMOVED, index));
        if (selectedIndex >= index) setSelectedIndex(selectedIndex - 1);
    }

    public Object getValue(final int index) {
        checkIndex(index);
        return items.get(index).value;
    }

    public void clear() {
        selectedIndex = -1;
        items.clear();
        selectedIndexes.clear();

        saveUpdate(writer -> writer.write(ServerToClientModel.CLEAR));

        if (containsEmptyItem) addItem(EMPTY, null);
    }

    public int getItemCount() {
        return items.size();
    }

    public void setSelectedIndex(final int index, final boolean selected) {
        checkIndex(index);
        this.selectedIndex = index;

        if (isMultipleSelect && selected) selectedIndexes.add(index);
        else selectedIndexes.remove(index);

        saveUpdate(writer -> {
            writer.write(ServerToClientModel.SELECTED, selected);
            writer.write(ServerToClientModel.INDEX, index);
        });
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(final int index) {
        setSelectedIndex(index, true);
    }

    public String getSelectedItem() {
        return selectedIndex >= 0 ? items.get(selectedIndex).label : null;
    }

    public void setSelectedItem(final String item) {
        setSelectedItem(item, true);
    }

    public Object getSelectedValue() {
        return selectedIndex >= 0 ? items.get(selectedIndex).value : null;
    }

    public void setSelectedValue(final Object value) {
        setSelectedValue(value, true);
    }

    @Override
    public void addChangeHandler(final PChangeHandler handler) {
        handlers.add(handler);
    }

    public boolean removeChangeHandler(final PChangeHandler handler) {
        return handlers.remove(handler);
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
            if (Objects.equals(value, items.get(i).value)) {
                setSelectedIndex(i, selected);
                break;
            }
        }
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

    public boolean isItemSelected(final int index) {
        return selectedIndexes.contains(index);
    }

    public String getItem(final int index) {
        return items.get(index).label;
    }

    @Override
    public Iterator<ListItem> iterator() {
        return items.iterator();
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
        return selectedIndexes.stream().map(index -> this.items.get(index).label).collect(Collectors.toList());
    }

    public List<Object> getSelectedValues() {
        return selectedIndexes.stream().map(index -> this.items.get(index).value).collect(Collectors.toList());
    }

    public int getVisibleItemCount() {
        return visibleItemCount;
    }

    public void setVisibleItemCount(final int visibleItemCount) {
        if (Objects.equals(this.visibleItemCount, visibleItemCount)) return;
        this.visibleItemCount = visibleItemCount;
        saveUpdate(ServerToClientModel.VISIBLE_ITEM_COUNT, visibleItemCount);
    }

    public boolean isMultipleSelect() {
        return isMultipleSelect;
    }

    public void setMultipleSelect(final boolean isMultipleSelect) {
        if (Objects.equals(this.isMultipleSelect, isMultipleSelect)) return;
        this.isMultipleSelect = isMultipleSelect;
        saveUpdate(ServerToClientModel.MULTISELECT, isMultipleSelect);
    }

    public static class ListItem {

        protected String label;

        protected Object value;

        ListItem(final String label, final Object value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public Object getValue() {
            return value;
        }
    }

    private static class ListGroupItem extends ListItem {

        ListGroupItem(final String group) {
            super(group, null);
        }
    }

    @Override
    protected String dumpDOM() {
        String DOM = "<select>";

        for (int i = 0; i < items.size(); i++) {
            ListItem item = items.get(0);
            DOM += "<option  value=\"" + i + "\" " + (selectedIndex == i ? "selected" : "") + ">" + item.label + "</option>";
        }

        DOM += "</select>";

        return DOM;
    }
}
