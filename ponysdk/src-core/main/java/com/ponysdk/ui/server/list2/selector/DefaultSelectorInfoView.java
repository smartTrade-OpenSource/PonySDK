
package com.ponysdk.ui.server.list2.selector;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;

public class DefaultSelectorInfoView extends PHorizontalPanel implements SelectorInfoView {

    private final List<SelectorInfoViewListener> selectorInfoViewListeners = new ArrayList<SelectorInfoViewListener>();

    final PLabel numberOfSelectedMessage = new PLabel();
    final PAnchor selectAllAnchor = new PAnchor();
    final PAnchor selectNoneAnchor = new PAnchor();

    public DefaultSelectorInfoView() {
        setHorizontalAlignment(PHorizontalAlignment.ALIGN_CENTER);
        setStyleName("pony-ComplexList-OptionSelectionPanel");
        setVisible(false);
        selectAllAnchor.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                for (final SelectorInfoViewListener listener : selectorInfoViewListeners) {
                    listener.onFullSelection();
                }
            }
        });
        selectNoneAnchor.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                for (final SelectorInfoViewListener listener : selectorInfoViewListeners) {
                    listener.onClearFullSelection();
                }
            }
        });
    }

    @Override
    public void showAllSelected(final int numberOfSelectedItems) {
        setVisible(true);
        clear();
        add(numberOfSelectedMessage);
        numberOfSelectedMessage.setText("All " + numberOfSelectedItems + " items are selected.");
    }

    @Override
    public void showSelectAllOption(final int numberOfSelectedItems, final int fullSize) {
        setVisible(true);
        clear();
        numberOfSelectedMessage.setText("All " + numberOfSelectedItems + " items on this page are selected.");
        add(numberOfSelectedMessage);
        final int itemsLeft = fullSize - numberOfSelectedItems;
        selectAllAnchor.setText("Select all " + itemsLeft + " final items");
        add(selectAllAnchor);
    }

    @Override
    public void showClearSelectionOption(final int numberOfSelectedIntems) {
        setVisible(true);
        clear();
        numberOfSelectedMessage.setText("All " + numberOfSelectedIntems + " items are selected.");
        add(numberOfSelectedMessage);
        selectNoneAnchor.setText("Clear Selection");
        add(selectNoneAnchor);
    }

    @Override
    public void hide() {
        setVisible(false);
    }

    @Override
    public PWidget asWidget() {
        return this;
    }

    @Override
    public void addSelectorInfoViewListener(final SelectorInfoViewListener selectorInfoViewListener) {
        selectorInfoViewListeners.add(selectorInfoViewListener);
    }

}
