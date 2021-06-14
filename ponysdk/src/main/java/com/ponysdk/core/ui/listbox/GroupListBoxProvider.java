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
import java.util.List;
import java.util.Random;
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
public class GroupListBoxProvider<D> implements ListBoxProvider<Pair<D, String>, ListBoxItemImpl<Pair<D, String>>> {

    public static class ListBoxItemImpl<D> implements ListBoxItem<D> {

        final PButton button = Element.newPButton();
        private D d;

        public ListBoxItemImpl() {
            button.setStyleProperty("width", "100%");
        }

        @Override
        public PWidget asWidget() {
            return button;
        }

        @Override
        public D getValue() {
            return d;
        }

        public void setValue(final D d, final String caption) {
            this.d = d;
            button.setText(caption);
            button.removeStyleName("group");
            button.setEnabled(true);

        }

        public void setGroup(final String s) {
            this.d = null;
            button.setText(s);
            //            this.setStyleProperty("font-weight", "bold");
            //            this.setStyleProperty("pointer-events", "none");
            button.addStyleName("group");
            button.setEnabled(false);

        }

        public void setVisible(final boolean b) {
            button.setVisible(b);
        }

        public void setStyleProperty(final String attribut, final String name) {
            button.setStyleProperty(attribut, name);

        }

    }

    private final List<GroupItem<D>> groupList = new ArrayList<>();
    private final List<GroupItem<D>> filterGroupList = new ArrayList<>();
    private final Function<D, String> displayer;

    public GroupListBoxProvider(final Function<D, String> displayer) {
        this.displayer = displayer;
    }

    @Override
    public void getData(final int beginIndex, final int maxSize, final Consumer<List<Pair<D, String>>> callback) {
        final List<Pair<D, String>> arrayL = new ArrayList<>();
        int globalPosition = 0;
        for (final GroupItem<D> g : filterGroupList) {
            if (maxSize == arrayL.size()) break;

            if (globalPosition + g.getData().size() + 1 <= beginIndex) {
                globalPosition += g.getData().size() + 1;
            } else {
                if (globalPosition >= beginIndex) {
                    arrayL.add(new Pair<>(null, g.getName()));
                }
                globalPosition++;
                for (int i = Math.max(0, beginIndex - globalPosition); i < g.getData().size() && arrayL.size() < maxSize; i++) {

                    final D element = g.getData().get(i);
                    arrayL.add(new Pair<>(element, displayer.apply(element)));

                }
                globalPosition += g.getData().size();

            }
        }
        callback.accept(arrayL);
    }

    @Override
    public void getFullSize(final Consumer<Integer> callback) {
        int fullSize = 0;
        for (final GroupItem<D> groupItem : filterGroupList) {
            fullSize += 1 + groupItem.getData().size();

        }
        callback.accept(fullSize);
    }

    @Override
    public ListBoxItemImpl<Pair<D, String>> handleUI(final int index, final Pair<D, String> data,
                                                     ListBoxItemImpl<Pair<D, String>> widget) {
        if (widget == null) widget = new ListBoxItemImpl<>();
        if (data.getFirst() == null) {
            widget.setGroup(data.getSecond());

        } else {
            widget.setValue(data, data.getSecond());
        }

        final Random rand = new Random();
        final int randomNum = rand.nextInt(50 - 20 + 1) + 20;
        widget.setStyleProperty("height", randomNum + "px");
        return widget;
    }

    @Override
    public void addHandler(final Consumer<Pair<D, String>> handler) {
    }

    @Override
    public void setFilter(final String filter) {
        filterGroupList.clear();
        if (filter == null) {
            filterGroupList.addAll(groupList);
        } else {
            for (final GroupItem<D> groupItem : groupList) {
                final List<D> filterList = new ArrayList<>();
                for (final D d : groupItem.getData()) {

                    if (d.toString().toLowerCase().contains(filter) || filter.isEmpty()) {
                        filterList.add(d);
                    }
                }
                if (!filterList.isEmpty()) {
                    filterGroupList.add(new GroupItem<>(groupItem.getName(), filterList));
                }
            }
        }
    }

    public void addGroup(final String name, final List<D> data) {
        final GroupItem<D> group = new GroupItem<>(name, data);
        groupList.add(group);

    }

    public void init() {
        setFilter("");
    }

    public void addElement(final String name, final D d) {
        for (final GroupItem<D> g : groupList) {
            if (g.getName().equals(name)) {
                g.addElement(d);
            }
        }
    }

    /**
     * @return the groupList
     */
    public List<GroupItem<D>> getGroupList() {
        return groupList;
    }

}
