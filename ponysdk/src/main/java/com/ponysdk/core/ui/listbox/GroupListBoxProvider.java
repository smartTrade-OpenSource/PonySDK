/*
 * Copyright (c) 2021 PonySDK
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

package com.ponysdk.core.ui.listbox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.listbox.GroupListBoxProvider.ListBoxItemImpl;
import com.ponysdk.core.util.Pair;

/**
 *
 */
public class GroupListBoxProvider<D> implements ListBoxProvider<Pair<D, String>, ListBoxItemImpl<D>> {

    public static class ListBoxItemImpl<D> implements ListBoxItem<Pair<D, String>> {

        private final PButton button = Element.newPButton();
        private Pair<D, String> d;
        private final Function<D, String> displayer;

        public ListBoxItemImpl(final Function<D, String> displayer) {
            this.displayer = displayer;
            button.setStyleProperty("width", "100%");
        }

        @Override
        public PWidget asWidget() {
            return button;
        }

        @Override
        public Pair<D, String> getValue() {
            return d;
        }

        public void setValue(final Pair<D, String> d) {
            this.d = d;
            if (d.getFirst() == null) {
                button.setText(d.getSecond());
                button.addStyleName("group");
                button.setEnabled(false);
            } else {
                button.setText(displayer.apply(d.getFirst()));
                button.removeStyleName("group");
                button.setEnabled(true);
            }

        }

        @Override
        public String getString() {
            return d.getFirst().toString();
        }

    }

    private final Map<String, Map<String, D>> map = new LinkedHashMap<>();
    private final Map<String, Map<String, D>> copyMap = new LinkedHashMap<>();
    private final List<Pair<D, String>> selectedValue = new ArrayList<>();
    private final Function<D, String> displayer;
    private String filter;

    public GroupListBoxProvider(final Function<D, String> displayer) {
        this.displayer = displayer;
    }

    @Override
    public void getData(final int beginIndex, final int maxSize, final Consumer<List<Pair<D, String>>> callback) {
        final List<Pair<D, String>> data = new ArrayList<>();
        int globalPosition = 0;
        for (final Entry<String, Map<String, D>> entry : copyMap.entrySet()) {
            if (maxSize == data.size()) break;

            if (globalPosition + entry.getValue().size() + 1 <= beginIndex) {
                globalPosition += entry.getValue().size() + 1;
            } else {
                if (globalPosition >= beginIndex) {
                    data.add(new Pair<>(null, entry.getKey()));
                }
                globalPosition++;
                int i = Math.max(0, beginIndex - globalPosition);
                for (final D element : entry.getValue().values()) {
                    if (i >= entry.getValue().size() || data.size() >= maxSize) {
                        break;
                    }
                    data.add(new Pair<>(element, entry.getKey()));
                    i++;
                }
                globalPosition += entry.getValue().size() + 1;

            }
        }

        callback.accept(data);
    }

    @Override
    public void getFullSize(final Consumer<Integer> callback) {
        int fullSize = 0;
        for (final Entry<String, Map<String, D>> entry : copyMap.entrySet()) {
            fullSize += 1 + entry.getValue().size();

        }
        callback.accept(fullSize);
    }

    @Override
    public ListBoxItemImpl<D> handleUI(final int index, final Pair<D, String> data, ListBoxItemImpl<D> widget) {
        if (widget == null) widget = new ListBoxItemImpl<>(displayer);
        widget.setValue(data);
        return widget;
    }

    @Override
    public void addHandler(final Consumer<Pair<D, String>> handler) {
    }

    @Override
    public void setFilter(final String filter) {
        this.filter = filter;
        copyMap.clear();

        for (final Entry<String, Map<String, D>> entry : map.entrySet()) {
            final Map<String, D> groupitem = new LinkedHashMap<>();
            for (final Entry<String, D> entryValue : entry.getValue().entrySet()) {
                final Pair<D, String> element = new Pair<>(entryValue.getValue(), entry.getKey());
                if (!selectedValue.contains(element) && entryValue.getValue().toString().toLowerCase().contains(filter)) {
                    groupitem.put(entryValue.getKey(), entryValue.getValue());
                }
            }
            if (!groupitem.isEmpty()) {
                copyMap.put(entry.getKey(), groupitem);
            }
        }
    }

    public void addGroup(final String groupName, final List<D> data) {
        for (final D element : data) {
            addItemInGroup(groupName, element);
        }

    }

    public void addItemInGroup(final String groupName, final D data) {
        Map<String, D> groupItem = map.get(groupName);
        if (groupItem == null) {
            groupItem = new LinkedHashMap<>();
            map.put(groupName, groupItem);
        }
        groupItem.put(displayer.apply(data), data);

    }

    public void init() {
        setFilter("");
    }

    @Override
    public void selectedItem(final Pair<D, String> data) {
        selectedValue.add(data);
        this.filterOnSelectedItem();
    }

    public void filterOnSelectedItem() {
        copyMap.clear();
        for (final Entry<String, Map<String, D>> entry : map.entrySet()) {
            final Map<String, D> groupitem = new LinkedHashMap<>();
            for (final Entry<String, D> entryValue : entry.getValue().entrySet()) {
                final Pair<D, String> element = new Pair<>(entryValue.getValue(), entry.getKey());
                if (!selectedValue.contains(element)) {
                    groupitem.put(entryValue.getKey(), entryValue.getValue());
                }
            }
            if (!groupitem.isEmpty()) {
                copyMap.put(entry.getKey(), groupitem);
            }
        }
    }

    @Override
    public void removeSelectedItem(final Pair<D, String> data) {
        selectedValue.remove(data);
        this.setFilter(this.filter);
    }

}
