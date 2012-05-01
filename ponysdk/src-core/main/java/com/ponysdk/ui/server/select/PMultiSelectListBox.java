
package com.ponysdk.ui.server.select;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PKeyCodes;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.HasPChangeHandlers;
import com.ponysdk.ui.server.basic.event.PBlurEvent;
import com.ponysdk.ui.server.basic.event.PBlurHandler;
import com.ponysdk.ui.server.basic.event.PChangeEvent;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PFocusEvent;
import com.ponysdk.ui.server.basic.event.PFocusHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpFilterHandler;

public class PMultiSelectListBox implements IsPWidget, HasPChangeHandlers {

    private static final Logger log = LoggerFactory.getLogger(PMultiSelectListBox.class);

    private final Set<PChangeHandler> handlers = new HashSet<PChangeHandler>();

    private final PMultiSelectListBoxView multiSelectListBoxView;

    private final List<String> items = new ArrayList<String>();

    private final List<String> selectedItems = new ArrayList<String>();

    private final List<String> unSelectedItems = new ArrayList<String>();

    private final Map<String, PClickHandler> selectedItemClickHandler = new HashMap<String, PClickHandler>();

    private final Map<String, PClickHandler> shownItemClickHandler = new HashMap<String, PClickHandler>();

    private Integer selectedItem;

    public PMultiSelectListBox() {
        multiSelectListBoxView = new DefaultMultiSelectListBoxView();
        multiSelectListBoxView.addKeyUpHandler(new PKeyUpFilterHandler(PKeyCodes.BACKSPACE, PKeyCodes.LEFT, PKeyCodes.RIGHT) {

            @Override
            public void onKeyUp(final int keyCode) {
                if (selectedItems.isEmpty()) return;

                if (PKeyCodes.BACKSPACE.equals(keyCode)) {
                    if (selectedItem == null) return;
                    multiSelectListBoxView.blurSelectedItem(selectedItems.get(selectedItem));
                    if (selectedItem > 0) {
                        selectedItem--;
                        multiSelectListBoxView.focusSelectedItem(selectedItems.get(selectedItem));
                    }
                    unSelectItem(selectedItems.get(selectedItem));
                } else if (PKeyCodes.LEFT.equals(keyCode)) {
                    if (selectedItem == null) {
                        selectedItem = selectedItems.size() - 1;
                        multiSelectListBoxView.focusSelectedItem(selectedItems.get(selectedItem));
                    } else if (selectedItem > 0) {
                        multiSelectListBoxView.blurSelectedItem(selectedItems.get(selectedItem));
                        selectedItem--;
                        multiSelectListBoxView.focusSelectedItem(selectedItems.get(selectedItem));
                    }
                } else if (PKeyCodes.RIGHT.equals(keyCode)) {
                    if (selectedItem == null) return;
                    if (selectedItem < selectedItems.size() - 1) {
                        multiSelectListBoxView.blurSelectedItem(selectedItems.get(selectedItem));
                        selectedItem++;
                        multiSelectListBoxView.focusSelectedItem(selectedItems.get(selectedItem));
                    }
                }
            }
        });

        multiSelectListBoxView.addFocusHandler(new PFocusHandler() {

            @Override
            public void onFocus(final PFocusEvent event) {

            }
        });
        multiSelectListBoxView.addBlurHandler(new PBlurHandler() {

            @Override
            public void onBlur(final PBlurEvent event) {
                if (selectedItem != null) multiSelectListBoxView.blurSelectedItem(selectedItems.get(selectedItem));
                selectedItem = null;
            }
        });
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
