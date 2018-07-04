/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.ui.list.paging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;

public class DefaultPagerView extends PFlowPanel implements PagerView, PClickHandler {

    private final Map<Integer, PAnchor> items = new HashMap<>();
    private final List<HandlerRegistration> registrations = new ArrayList<>();

    private final PAnchor startMenuItem = Element.newPAnchor("<<");
    private final PAnchor previousMenuItem = Element.newPAnchor("<");
    private final PAnchor nextMenuItem = Element.newPAnchor(">");
    private final PAnchor endMenuItem = Element.newPAnchor(">>");

    private final List<PagerListener> pagerListeners = new ArrayList<>();

    public DefaultPagerView() {
        setVisible(false);
        initUI();
    }

    private void initUI() {
        addStyleName("pony-Page-Navigation");

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
        final PAnchor item = Element.newPAnchor(String.valueOf(pageIndex + 1));
        item.setData(pageIndex);
        // TODO Rework
        insert(item, 2 + items.values().size());
        registrations.add(item.addClickHandler(this));
        items.put(pageIndex, item);
    }

    @Override
    public void addPagerListener(final PagerListener pagerListener) {
        pagerListeners.add(pagerListener);
    }

    @Override
    public void clear() {
        registrations.forEach(HandlerRegistration::removeHandler);
        items.values().forEach(PWidget::removeFromParent);
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
        item.addStyleName("pony-Page-Navigation-selectedItem");
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
