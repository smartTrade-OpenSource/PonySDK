
package com.ponysdk.ui.server.list2.selector;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PCommand;
import com.ponysdk.ui.server.basic.PMenuBar;
import com.ponysdk.ui.server.basic.PMenuItem;
import com.ponysdk.ui.server.basic.PWidget;

public class DefaultActionSelectorView extends PMenuBar implements SelectorActionView {

    private final List<SelectorActionViewListener> selectorViewListeners = new ArrayList<SelectorActionViewListener>();
    private final PMenuItem selectAllMenuItem;
    private final PMenuItem selectNoneMenuItem;

    public DefaultActionSelectorView() {
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
    public void addSelectorActionViewListener(final SelectorActionViewListener selectorListener) {
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
                for (final SelectorActionViewListener selectorListener : selectorViewListeners) {
                    selectorListener.onSelectAllVisible();
                }
            }
        };
    }

    private PCommand getSelectNoneCommand() {
        return new PCommand() {

            @Override
            public void execute() {
                for (final SelectorActionViewListener selectorListener : selectorViewListeners) {
                    selectorListener.onUnselectAllVisible();
                }
            }
        };
    }

}
