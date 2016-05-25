
package com.ponysdk.ui.server.list.paging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PCommand;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PMenuBar;
import com.ponysdk.ui.server.basic.PMenuItem;

public class DefaultPagerView extends PHorizontalPanel implements PagerView {

    private final Map<Integer, IsPWidget> items = new HashMap<>();

    private PMenuBar menuBar;

    private PMenuItem startMenuItem;
    private PMenuItem previousMenuItem;
    private PMenuItem nextMenuItem;
    private PMenuItem endMenuItem;

    List<PagerListener> pagerListeners = new ArrayList<>();

    public DefaultPagerView() {
        setVisible(false);
        initUI();
    }

    private void initUI() {
        menuBar = new PMenuBar();
        addStyleName(PonySDKTheme.PAGE_NAVIGATION);
        add(menuBar);

        startMenuItem = new PMenuItem("<<");
        menuBar.addItem(startMenuItem);
        menuBar.addSeparator();

        previousMenuItem = new PMenuItem("<");
        menuBar.addItem(previousMenuItem);
        menuBar.addSeparator();

        menuBar.addSeparator();
        nextMenuItem = new PMenuItem(">");
        menuBar.addItem(nextMenuItem);
        menuBar.addSeparator();

        endMenuItem = new PMenuItem(">>");
        menuBar.addItem(endMenuItem);
    }

    @Override
    public void addPageIndex(final int pageIndex) {
        final PMenuItem item = new PMenuItem(String.valueOf(pageIndex + 1));
        item.setCommand(getClickCommand(pageIndex));
        menuBar.insertItem(item, 4 + items.values().size());
        items.put(pageIndex, item);
    }

    private PCommand getClickCommand(final int pageIndex) {
        return new PCommand() {

            @Override
            public void execute() {
                for (final PagerListener pagerListener : pagerListeners) {
                    pagerListener.onPageChange(pageIndex);
                }
            }
        };
    }

    @Override
    public void addPagerListener(final PagerListener pagerListener) {
        pagerListeners.add(pagerListener);
    }

    @Override
    public void clear() {
        super.clear();
        items.clear();
        initUI();
    }

    @Override
    public void setStart(final boolean enabled, final int pageIndex) {
        startMenuItem.setEnabled(enabled);
        startMenuItem.setCommand(getClickCommand(pageIndex));
    }

    @Override
    public void setEnd(final boolean enabled, final int pageIndex) {
        endMenuItem.setEnabled(enabled);
        endMenuItem.setCommand(getClickCommand(pageIndex));

    }

    @Override
    public void setPrevious(final boolean enabled, final int pageIndex) {
        previousMenuItem.setEnabled(enabled);
        previousMenuItem.setCommand(getClickCommand(pageIndex));
    }

    @Override
    public void setNext(final boolean enabled, final int pageIndex) {
        nextMenuItem.setEnabled(enabled);
        nextMenuItem.setCommand(getClickCommand(pageIndex));
    }

    @Override
    public void setSelectedPage(final int pageIndex) {
        final PMenuItem item = (PMenuItem) items.get(pageIndex).asWidget();
        item.setEnabled(false);
        item.setStyleName(PonySDKTheme.PAGE_NAVIGATION_ITEM_SELECTED);
    }

}
