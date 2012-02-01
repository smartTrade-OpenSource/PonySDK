
package com.ponysdk.ui.server.select;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.activity.AbstractActivity;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class PMultiSelectListBoxActivity extends AbstractActivity {

    private static final Logger log = LoggerFactory.getLogger(PMultiSelectListBoxActivity.class);

    private final PMultiSelectListBoxView multiSelectListBoxView;

    private final Set<String> items = new HashSet<String>();

    private final Set<String> selectedItems = new HashSet<String>();

    private final Set<String> unSelectedItems = new HashSet<String>();

    private final Map<String, PClickHandler> selectedItemClickHandler = new HashMap<String, PClickHandler>();

    private final Map<String, PClickHandler> shownItemClickHandler = new HashMap<String, PClickHandler>();

    public PMultiSelectListBoxActivity(PMultiSelectListBoxView multiSelectListBoxView) {
        this.multiSelectListBoxView = multiSelectListBoxView;
    }

    @Override
    public void start(PAcceptsOneWidget world) {
        world.setWidget(multiSelectListBoxView.asWidget());
    }

    public void addItem(final String item) {
        this.items.add(item);
        multiSelectListBoxView.addItem(item);

        PClickHandler handler = shownItemClickHandler.get(item);

        if (handler == null) {
            handler = new PClickHandler() {

                @Override
                public void onClick(PClickEvent event) {
                    selectItem(item);
                }
            };
            shownItemClickHandler.put(item, handler);
            multiSelectListBoxView.addShownItemClickHandler(item, handler);
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
                public void onClick(PClickEvent event) {
                    unSelectItem(item);
                }
            };
            selectedItemClickHandler.put(item, handler);
            multiSelectListBoxView.addSelectedClickHandler(item, handler);
        }

        unSelectedItems.remove(item);
        selectedItems.add(item);
    }

    public void unSelectItem(String item) {
        if (!items.contains(item)) {
            log.warn("Unknown item #" + item);
            return;
        }
        multiSelectListBoxView.unSelectItem(item);
        selectedItems.remove(item);
        unSelectedItems.add(item);
    }

}
