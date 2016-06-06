
package com.ponysdk.core.ui.list.paging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;

public class DefaultPagerView extends PFlowPanel implements PagerView, PClickHandler {

    private final Map<Integer, PAnchor> items = new HashMap<>();
    private final List<HandlerRegistration> registrations = new ArrayList<>();

    private final PAnchor startMenuItem = new PAnchor("<<");
    private final PAnchor previousMenuItem = new PAnchor("<");
    private final PAnchor nextMenuItem = new PAnchor(">");
    private final PAnchor endMenuItem = new PAnchor(">>");

    private final List<PagerListener> pagerListeners = new ArrayList<>();

    public DefaultPagerView() {
        setVisible(false);
        initUI();
    }

    private void initUI() {
        addStyleName("pager");

        startMenuItem.addClickHandler(this);
        previousMenuItem.addClickHandler(this);
        nextMenuItem.addClickHandler(this);
        endMenuItem.addClickHandler(this);

        add(startMenuItem);
        add(previousMenuItem);
        add(nextMenuItem);
        add(endMenuItem);
    }

    @Override
    public void addPageIndex(final int pageIndex) {
        final PAnchor item = new PAnchor(String.valueOf(pageIndex + 1));
        item.setData(pageIndex);
        insert(item, 4 + items.values().size());
        registrations.add(item.addClickHandler(this));
        items.put(pageIndex, item);
    }

    @Override
    public void addPagerListener(final PagerListener pagerListener) {
        pagerListeners.add(pagerListener);
    }

    @Override
    public void clear() {
        for (final HandlerRegistration registration : registrations) {
            registration.removeHandler();
        }

        for (final PAnchor anchor : items.values()) {
            anchor.removeFromParent();
        }

        registrations.clear();
        items.clear();
    }

    @Override
    public void setStart(final boolean enabled, final int pageIndex) {
        startMenuItem.setEnabled(enabled);
        startMenuItem.setData(pageIndex);
    }

    @Override
    public void setEnd(final boolean enabled, final int pageIndex) {
        endMenuItem.setEnabled(enabled);
        endMenuItem.setData(pageIndex);
    }

    @Override
    public void setPrevious(final boolean enabled, final int pageIndex) {
        previousMenuItem.setEnabled(enabled);
        previousMenuItem.setData(pageIndex);
    }

    @Override
    public void setNext(final boolean enabled, final int pageIndex) {
        nextMenuItem.setEnabled(enabled);
        nextMenuItem.setData(pageIndex);
    }

    @Override
    public void setSelectedPage(final int pageIndex) {
        final PAnchor item = items.get(pageIndex);
        item.setEnabled(false);
        item.addStyleName("selected");
    }

    @Override
    public void onClick(final PClickEvent event) {
        final PAnchor source = (PAnchor) event.getSource();
        final Integer pageIndex = (Integer) source.getData();

        if (pageIndex != null) {
            for (final PagerListener pagerListener : pagerListeners) {
                pagerListener.onPageChange(pageIndex);
            }
        }

    }

}
