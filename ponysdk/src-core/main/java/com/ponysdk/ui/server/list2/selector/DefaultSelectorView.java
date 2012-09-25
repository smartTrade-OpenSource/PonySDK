
package com.ponysdk.ui.server.list2.selector;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PCommand;
import com.ponysdk.ui.server.basic.PMenuBar;
import com.ponysdk.ui.server.basic.PMenuItem;
import com.ponysdk.ui.server.basic.PWidget;

public class DefaultSelectorView extends PMenuBar implements SelectorView {

    private final List<SelectorViewListener> selectorViewListeners = new ArrayList<SelectorViewListener>();
    private final PMenuItem selectAllMenuItem;
    private final PMenuItem selectNoneMenuItem;

    public DefaultSelectorView() {
        addStyleName(PonySDKTheme.MENUBAR_LIGHT);
        final PMenuBar menuBarAction = new PMenuBar(true);
        addItem("", menuBarAction);
        selectAllMenuItem = new PMenuItem("All");
        selectAllMenuItem.setCommand(getSelectAllCommand());
        addItem(selectAllMenuItem);
        selectNoneMenuItem = new PMenuItem("None");
        selectNoneMenuItem.setCommand(getSelectNoneCommand());

        menuBarAction.addItem(selectAllMenuItem);
        menuBarAction.addItem(selectNoneMenuItem);
    }

    @Override
    public void addSelectorViewListener(final SelectorViewListener selectorListener) {
        selectorViewListeners.add(selectorListener);
    }

    @Override
    public PWidget asWidget() {
        return this;
    }

    private PCommand getSelectAllCommand() {
        return new PCommand() {

            @Override
            public void execute() {
                for (final SelectorViewListener selectorListener : selectorViewListeners) {
                    selectorListener.onSelectAll();
                }
            }
        };
    }

    private PCommand getSelectNoneCommand() {
        return new PCommand() {

            @Override
            public void execute() {
                for (final SelectorViewListener selectorListener : selectorViewListeners) {
                    selectorListener.onUnselectAll();
                }
            }
        };
    }

}
