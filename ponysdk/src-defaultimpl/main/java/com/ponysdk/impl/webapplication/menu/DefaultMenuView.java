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

import java.util.LinkedHashMap;
import java.util.Map;

import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PTree;
import com.ponysdk.ui.server.basic.PTreeItem;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;

public class DefaultMenuView extends PSimplePanel implements MenuView {

    private final Map<String, PTreeItem> itemsByName = new LinkedHashMap<String, PTreeItem>();

    private final PTree tree;

    private PSelectionHandler<String> handler;

    public DefaultMenuView() {
        tree = new PTree();
        setWidget(tree);
        setSizeFull();
        tree.setSizeFull();
        tree.addSelectionHandler(new PSelectionHandler<PTreeItem>() {

            @Override
            public void onSelection(final PSelectionEvent<PTreeItem> event) {
                final PSelectionEvent<String> e = new PSelectionEvent<String>(this, event.getSelectedItem().getText());
                handler.onSelection(e);
            }
        });
    }

    @Override
    public void addCategory(final String category) {
        createCategoryItemIfNeeded(category);
    }

    private PTreeItem createCategoryItemIfNeeded(final String category) {
        PTreeItem categoryItem = itemsByName.get(category);
        if (categoryItem == null) {
            categoryItem = tree.addItem(category);
            itemsByName.put(category, categoryItem);
        }
        return categoryItem;
    }

    @Override
    public void addItem(final String category, final String caption) {
        final PTreeItem categoryItem = createCategoryItemIfNeeded(category);
        if (caption != null) {
            final PTreeItem captionItem = new PTreeItem(caption);
            categoryItem.addItem(captionItem);
            itemsByName.put(caption, captionItem);
        }
    }

    @Override
    public void selectItem(final String category, final String caption) {
        if (caption != null) {
            tree.setSelectedItem(itemsByName.get(caption));
        } else {
            tree.setSelectedItem(itemsByName.get(category));
        }
    }

    @Override
    public void addSelectionHandler(final PSelectionHandler<String> handler) {
        this.handler = handler;
    }

}
