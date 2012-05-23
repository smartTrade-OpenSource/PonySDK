/*
 * Copyright (c) 2011 PonySDK Owners: Luciano Broussal <luciano.broussal AT gmail.com> Mathieu Barbier <mathieu.barbier AT gmail.com> Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 * 
 * WebSite: http://code.google.com/p/pony-sdk/
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.ponysdk.impl.webapplication.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.ui.server.basic.PSimpleLayoutPanel;
import com.ponysdk.ui.server.basic.PTree;
import com.ponysdk.ui.server.basic.PTreeItem;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;

public class DefaultMenuView extends PSimpleLayoutPanel implements MenuView {

    private final Map<String, PTreeItem> categoryByName = new LinkedHashMap<String, PTreeItem>();

    private final Map<PTreeItem, MenuItem> menuItemByUIObject = new LinkedHashMap<PTreeItem, MenuItem>();
    private final Map<MenuItem, PTreeItem> uiObjectByMenuItem = new LinkedHashMap<MenuItem, PTreeItem>();

    private final PTree tree;

    private final List<PSelectionHandler<MenuItem>> selectionHandlers = new ArrayList<PSelectionHandler<MenuItem>>();

    public DefaultMenuView() {
        tree = new PTree();
        setWidget(tree);
        setSizeFull();
        tree.setSizeFull();
        tree.addSelectionHandler(new PSelectionHandler<PTreeItem>() {

            @Override
            public void onSelection(final PSelectionEvent<PTreeItem> event) {
                final MenuItem menuItem = menuItemByUIObject.get(event.getSelectedItem());
                final PSelectionEvent<MenuItem> e = new PSelectionEvent<MenuItem>(this, menuItem);
                for (final PSelectionHandler<MenuItem> handler : selectionHandlers) {
                    handler.onSelection(e);
                }
            }
        });
    }

    private PTreeItem createCategoryItemIfNeeded(final String category) {
        PTreeItem categoryItem = categoryByName.get(category);
        if (categoryItem == null) {
            categoryItem = tree.addItem(category);
            categoryByName.put(category, categoryItem);
        }
        return categoryItem;
    }

    @Override
    public void addItem(final MenuItem menuItem) {
        final PTreeItem categoryItem = createCategoryItemIfNeeded(menuItem.getCategories().iterator().next());
        if (menuItem.getName() != null) {
            final PTreeItem captionItem = new PTreeItem(menuItem.getName());
            categoryItem.addItem(captionItem);
            menuItemByUIObject.put(captionItem, menuItem);
            uiObjectByMenuItem.put(menuItem, captionItem);
        }
    }

    @Override
    public void selectItem(final MenuItem menuItem) {
        final PTreeItem treeItem = uiObjectByMenuItem.get(menuItem);
        tree.setSelectedItem(treeItem);
    }

    @Override
    public void addSelectionHandler(final PSelectionHandler<MenuItem> handler) {
        selectionHandlers.add(handler);
    }

    @Override
    public void removeSelectionHandler(final PSelectionHandler<MenuItem> handler) {
        selectionHandlers.remove(handler);
    }

    @Override
    public Collection<PSelectionHandler<MenuItem>> getSelectionHandlers() {
        return selectionHandlers;
    }

}
