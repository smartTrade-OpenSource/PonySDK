
package com.ponysdk.ui.server.list.selector;

import com.ponysdk.core.internalization.PString;
import com.ponysdk.core.tools.ListenerCollection;
import com.ponysdk.ui.server.basic.PMenuBar;
import com.ponysdk.ui.server.basic.PMenuItem;
import com.ponysdk.ui.server.basic.PWidget;

public class DefaultActionSelectorView extends PMenuBar implements SelectorView {

    private final ListenerCollection<SelectorViewListener> selectorViewListeners = new ListenerCollection<>();
    private final PMenuItem selectAllMenuItem;
    private final PMenuItem selectNoneMenuItem;

    public DefaultActionSelectorView() {
        final PMenuBar menuBarAction = new PMenuBar(true);
        addItem("", menuBarAction);
        selectAllMenuItem = new PMenuItem(PString.get("list.selector.all"));
        selectAllMenuItem.setCommand(getSelectAllCommand());
        addItem(selectAllMenuItem);
        selectNoneMenuItem = new PMenuItem(PString.get("list.selector.none"));
        selectNoneMenuItem.setCommand(getSelectNoneCommand());

        menuBarAction.addItem(selectAllMenuItem);
        menuBarAction.addItem(selectNoneMenuItem);
    }

    @Override
    public PWidget asWidget() {
        return this;
    }

    private Runnable getSelectAllCommand() {
        return () -> {
            for (final SelectorViewListener selectorListener : selectorViewListeners) {
                selectorListener.onSelectionChange(SelectionMode.PAGE);
            }
        };
    }

    private Runnable getSelectNoneCommand() {
        return () -> {
            for (final SelectorViewListener selectorListener : selectorViewListeners) {
                selectorListener.onSelectionChange(SelectionMode.NONE);
            }
        };
    }

    @Override
    public void addSelectorViewListener(final SelectorViewListener selectorViewListener) {
        selectorViewListeners.register(selectorViewListener);
    }

    @Override
    public void update(final SelectionMode selectionMode, final int numberOfSelectedItems, final int fullSize, final int pageSize) {
        // Nothing
    }

}
