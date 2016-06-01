
package com.ponysdk.ui.server.rich;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PElement;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;

public class PBreadCrumbs extends PElement {

    private final Map<String, ItemLevel> elementsByItems = new HashMap<>();

    private final Set<PSelectionHandler<ItemLevel>> handlers = new HashSet<>();

    private ItemLevel current;

    public PBreadCrumbs() {
        super("ul");
        addStyleName("breadcrumbs");
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
