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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.Style.Unit;
import com.ponysdk.ui.server.addon.PDisclosurePanel;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PImage;
import com.ponysdk.ui.server.basic.PStackLayoutPanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;

public class StackLayoutMenuView extends PStackLayoutPanel implements MenuView {

    private final Map<String, PAnchor> anchorByName = new LinkedHashMap<String, PAnchor>();
    private final Map<String, PVerticalPanel> categoriesByName = new LinkedHashMap<String, PVerticalPanel>();

    private final double headerWidth = 2;// em
    private PSelectionHandler<String> handler;

    private PAnchor selectedItem;

    public StackLayoutMenuView() {
        super(Unit.EM);
        setSizeFull();
    }

    @Override
    public void addCategory(String category) {
        createCategoryItemIfNeeded(category);
    }

    private String getCategory(String[] split, int index) {
        String category = split[0];
        for (int i = 0; i <= index; i++) {
            category = category + "," + split[i];
        }
        return category;
    }

    private PVerticalPanel createCategoryItemIfNeeded(String category) {
        final String[] split = category.split(",");
        final List<String> list = Arrays.asList(split);
        int i = 0;
        PVerticalPanel categoryPanel = null;
        while (i != list.size()) {
            final String currentCategory = getCategory(split, i);
            categoryPanel = categoriesByName.get(currentCategory);
            if (categoryPanel == null) {
                final String currentCategoryHeader = split[i];
                categoryPanel = new PVerticalPanel();
                categoryPanel.setHeight("1px");
                categoryPanel.setWidth("100%");
                categoriesByName.put(currentCategory, categoryPanel);
                if (i == 0) {
                    add(categoryPanel, currentCategoryHeader, true, headerWidth);
                } else {
                    final String containingCategory = getCategory(split, i - 1);
                    final PVerticalPanel containingPanel = categoriesByName.get(containingCategory);
                    final PImage openImage = new PImage("images/treeDownTriangleBlack.png");
                    final PImage closeImage = new PImage("images/treeRightTriangleBlack.png");
                    openImage.addStyleName("disclosure-open");
                    final PDisclosurePanel newCategoryStackPanel = new PDisclosurePanel(currentCategoryHeader, openImage, closeImage);
                    containingPanel.add(newCategoryStackPanel);
                    newCategoryStackPanel.add(categoryPanel);
                    newCategoryStackPanel.setSizeFull();
                }
            }
            i++;
        }
        categoryPanel.ensureDebugId("category_" + category);
        return categoryPanel;
    }

    @Override
    public void addItem(String category, final String caption) {
        final PVerticalPanel categoryPanel = createCategoryItemIfNeeded(category);
        if (caption != null) {
            final PAnchor item = new PAnchor(caption);
            item.ensureDebugId("page_" + caption);
            item.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(PClickEvent clickEvent) {
                    final PSelectionEvent<String> event = new PSelectionEvent<String>();
                    event.setSelectedItem(caption);
                    handler.onSelection(event);
                    // item.addStyleName("selectedItem");
                    // if (selectedItem != null)
                    // selectedItem.removeStyleName("selectedItem");
                    // selectedItem = item;
                }
            });
            categoryPanel.add(item);
            anchorByName.put(caption, item);
        }
    }

    @Override
    public void addSelectionHandler(PSelectionHandler<String> handler) {
        this.handler = handler;
    }

    @Override
    public void selectItem(String category, String caption) {
        if (caption != null) {
            if (selectedItem != null)
                selectedItem.removeStyleName("selectedItem");
            final PAnchor item = anchorByName.get(caption);
            item.addStyleName("selectedItem");
            selectedItem = item;
        } else {
            // tree.setSelectedItem(anchorByName.get(category));
        }
    }
}
