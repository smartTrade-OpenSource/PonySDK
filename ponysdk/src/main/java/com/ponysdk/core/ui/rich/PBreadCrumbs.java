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

package com.ponysdk.core.ui.rich;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PSelectionEvent;
import com.ponysdk.core.ui.basic.event.PSelectionHandler;

public class PBreadCrumbs extends PElement {

    private final Map<String, ItemLevel> elementsByItems = new HashMap<>();

    private final Set<PSelectionHandler<ItemLevel>> handlers = new HashSet<>();

    private ItemLevel current;

    public PBreadCrumbs() {
        super("ul");
        addStyleName("pony-BreadCrumbs");
    }

    public void addItem(final String item) {
        if (elementsByItems.containsKey(item)) return;

        final ItemLevel itemLevel = new ItemLevel(item, current);

        if (current != null) current.unselect();

        current = itemLevel;

        add(itemLevel);

        elementsByItems.put(item, itemLevel);
    }

    public void selectItem(final String item) {
        if (!elementsByItems.containsKey(item)) return;
        if (elementsByItems.size() == 1) return;

        ItemLevel itemLevel = elementsByItems.get(item);

        if (current == itemLevel) return;

        itemLevel.select();

        while (itemLevel.hasNext()) {
            itemLevel = itemLevel.next;
            elementsByItems.remove(itemLevel.item);
            itemLevel.removeFromParent();
        }
    }

    public void addSelectionHandler(final PSelectionHandler<ItemLevel> handler) {
        handlers.add(handler);
    }

    void fireOnClick(final ItemLevel itemLevel) {
        final PSelectionEvent<ItemLevel> event = new PSelectionEvent<>(this, itemLevel);

        for (final PSelectionHandler<ItemLevel> handler : handlers) {
            handler.onSelection(event);
        }
    }

    public class ItemLevel extends PElement implements PClickHandler {

        int level = 1;

        String item;
        PAnchor anchor;

        ItemLevel previous;
        ItemLevel next;

        public ItemLevel(final String item, final ItemLevel previous) {
            super("li");
            this.item = item;
            if (previous != null) {
                this.previous = previous;
                this.previous.next = this;
                this.level = previous.level + 1;
            }

            setInnerText(item);
        }

        void select() {
            current = this;
            setInnerText(item);
        }

        void unselect() {
            setInnerText(null);

            anchor = new PAnchor(item);
            anchor.addClickHandler(this);

            add(anchor);
        }

        @Override
        public void onClick(final PClickEvent event) {
            selectItem(item);
            fireOnClick(this);
        }

        public boolean hasNext() {
            return next != null;
        }

        public boolean hasPrevious() {
            return previous != null;
        }

        public int getLevel() {
            return level;
        }

        public String getItem() {
            return item;
        }

        public ItemLevel getPrevious() {
            return previous;
        }

        public ItemLevel getNext() {
            return next;
        }
    }
}
