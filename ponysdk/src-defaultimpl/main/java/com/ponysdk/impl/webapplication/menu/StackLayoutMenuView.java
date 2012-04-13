/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.impl.webapplication.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.Style.Unit;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PDisclosurePanel;
import com.ponysdk.ui.server.basic.PImage;
import com.ponysdk.ui.server.basic.PSimpleLayoutPanel;
import com.ponysdk.ui.server.basic.PStackLayoutPanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;

public class StackLayoutMenuView extends PSimpleLayoutPanel implements MenuView {

    private final Map<String, PAnchor> anchorByName = new LinkedHashMap<String, PAnchor>();

    private final Map<String, PVerticalPanel> categoriesByName = new LinkedHashMap<String, PVerticalPanel>();

    private final Map<String, PWidget> categoryDisclosureByName = new LinkedHashMap<String, PWidget>();

    private final double headerWidth = 2;// em

    private PAnchor selectedItem;

    private final PStackLayoutPanel layoutPanel;

    private final List<PSelectionHandler<String>> selectionHandlers = new ArrayList<PSelectionHandler<String>>();

    public StackLayoutMenuView() {
        layoutPanel = new PStackLayoutPanel(Unit.EM);
        setWidget(layoutPanel);
        setSizeFull();
        layoutPanel.setSizeFull();

        layoutPanel.addSelectionHandler(new PSelectionHandler<Integer>() {

            @Override
            public void onSelection(final PSelectionEvent<Integer> event) {
                // PSelectionEvent<String> e = new PSelectionEvent<String>(StackLayoutMenuView.this, );
                // for (PSelectionHandler<String> handler : selectionHandlers) {
                // handler.onSelection(e);
                // }
            }
        });

    }

    @Override
    public void addCategory(final String category) {
        createCategoryItemIfNeeded(category);
    }

    private String getCategory(final List<String> split, final int index) {
        final Iterator<String> iterator = split.iterator();
        int currentIndex = 0;
        String category = "";
        while (iterator.hasNext() && currentIndex <= index) {
            category += iterator.next();
            currentIndex++;
            if (iterator.hasNext() && currentIndex <= index) category += ",";
        }
        return category;
    }

    private PVerticalPanel createCategoryItemIfNeeded(final String category) {
        final String[] split = category.split(",");
        final List<String> list = Arrays.asList(split);
        int i = 0;
        PVerticalPanel categoryPanel = null;
        while (i != list.size()) {
            final String currentCategory = getCategory(list, i);
            categoryPanel = categoriesByName.get(currentCategory);
            if (categoryPanel == null) {
                final String currentCategoryHeader = split[i];
                categoryPanel = new PVerticalPanel();
                categoryPanel.setHeight("1px");
                categoryPanel.setWidth("100%");
                categoriesByName.put(currentCategory, categoryPanel);
                if (i == 0) {
                    layoutPanel.add(categoryPanel, currentCategoryHeader, true, headerWidth);
                    categoryDisclosureByName.put(currentCategory, this);
                } else {
                    final String containingCategory = getCategory(list, i - 1);
                    final PVerticalPanel containingPanel = categoriesByName.get(containingCategory);
                    final PImage openImage = new PImage("images/treeDownTriangleBlack.png");
                    final PImage closeImage = new PImage("images/treeRightTriangleBlack.png");
                    openImage.addStyleName("disclosure-open");
                    final PDisclosurePanel newCategoryStackPanel = new PDisclosurePanel(currentCategoryHeader, openImage, closeImage);
                    containingPanel.add(newCategoryStackPanel);
                    newCategoryStackPanel.add(categoryPanel);
                    newCategoryStackPanel.setSizeFull();
                    categoryDisclosureByName.put(currentCategory, newCategoryStackPanel);
                }
            }
            i++;
        }
        categoryPanel.ensureDebugId("category_" + category);
        return categoryPanel;
    }

    @Override
    public void addItem(final String category, final String caption) {
        final PVerticalPanel categoryPanel = createCategoryItemIfNeeded(category);
        if (caption != null) {
            final PAnchor item = new PAnchor(caption);
            item.ensureDebugId("page_" + caption);
            item.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent clickEvent) {
                    final PSelectionEvent<String> event = new PSelectionEvent<String>(this, caption);
                    for (final PSelectionHandler<String> handler : selectionHandlers) {
                        handler.onSelection(event);
                    }
                }
            });
            categoryPanel.add(item);
            anchorByName.put(caption, item);
        }
    }

    @Override
    public void selectItem(final String category, final String caption) {
        if (caption != null) {
            if (selectedItem != null) selectedItem.removeStyleName("selectedItem");
            final PAnchor item = anchorByName.get(caption);
            item.addStyleName("selectedItem");
            selectedItem = item;
            final String[] split = category.split(",");
            final List<String> list = Arrays.asList(split);
            int i = 0;
            while (i != list.size()) {
                final String currentCategory = getCategory(list, i);
                if (i == 0) {
                    final PVerticalPanel verticalPanel = categoriesByName.get(currentCategory);
                    layoutPanel.showWidget(verticalPanel);
                } else {
                    final PDisclosurePanel containingPanel = (PDisclosurePanel) categoryDisclosureByName.get(currentCategory);
                    containingPanel.setOpen(true);
                }
                i++;
            }
        } else {
            // tree.setSelectedItem(anchorByName.get(category));
        }
    }

    @Override
    public void addSelectionHandler(final PSelectionHandler<String> handler) {
        selectionHandlers.add(handler);
    }

    @Override
    public void removeSelectionHandler(final PSelectionHandler<String> handler) {
        selectionHandlers.remove(handler);
    }

    @Override
    public Collection<PSelectionHandler<String>> getSelectionHandlers() {
        return selectionHandlers;
    }

}
