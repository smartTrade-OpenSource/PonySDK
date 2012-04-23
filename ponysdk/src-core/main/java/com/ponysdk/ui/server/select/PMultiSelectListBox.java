
package com.ponysdk.ui.server.select;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.HasPChangeHandlers;
import com.ponysdk.ui.server.basic.event.PChangeEvent;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class PMultiSelectListBox implements IsPWidget, HasPChangeHandlers {

    private static final Logger log = LoggerFactory.getLogger(PMultiSelectListBox.class);

    private final Set<PChangeHandler> handlers = new HashSet<PChangeHandler>();

    private final PMultiSelectListBoxView multiSelectListBoxView;

    private final Set<String> items = new HashSet<String>();

    private final Set<String> selectedItems = new HashSet<String>();

    private final Set<String> unSelectedItems = new HashSet<String>();

    private final Map<String, PClickHandler> selectedItemClickHandler = new HashMap<String, PClickHandler>();

    private final Map<String, PClickHandler> shownItemClickHandler = new HashMap<String, PClickHandler>();

    public PMultiSelectListBox() {
        this.multiSelectListBoxView = new DefaultMultiSelectListBoxView();
    }

    public void addItem(final String item) {
        if (!items.add(item)) return;

        multiSelectListBoxView.addItem(item);

        PClickHandler handler = shownItemClickHandler.get(item);

        if (handler == null) {
            handler = new PClickHandler() {

                @Override
                public void onClick(final PClickEvent event) {
                    selectItem(item);
                    fireChangeHandler(item);
                }
            };
            shownItemClickHandler.put(item, handler);
            multiSelectListBoxView.addShownItemClickHandler(item, handler);
        }
    }

    protected void fireChangeHandler(final String item) {
        for (final PChangeHandler handler : handlers) {
            handler.onChange(new PChangeEvent(this));
        }

    }

    public void selectItem(final String item) {
        if (!items.contains(item)) {
            log.warn("Unknown item #" + item);
            return;
        }

        multiSelectListBoxView.selectItem(item);

        PClickHandler handler = selectedItemClickHandler.get(item);

        if (handler == null) {
            handler = new PClickHandler() {

                @Override
                public void onClick(final PClickEvent event) {
                    unSelectItem(item);
                }
            };
            selectedItemClickHandler.put(item, handler);
            multiSelectListBoxView.addSelectedClickHandler(item, handler);
        }

        unSelectedItems.remove(item);
        selectedItems.add(item);

    }

    public void unSelectItem(final String item) {
        if (!items.contains(item)) {
            log.warn("Unknown item #" + item);
            return;
        }
        multiSelectListBoxView.unSelectItem(item);
        selectedItems.remove(item);
        unSelectedItems.add(item);
    }

    @Override
    public PWidget asWidget() {
        return multiSelectListBoxView.asWidget();
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

    public Collection<String> getSelectedItems() {
        return Collections.unmodifiableCollection(selectedItems);
    }

    public Collection<String> getUnSelectedItems() {
        return Collections.unmodifiableCollection(unSelectedItems);
    }

}
