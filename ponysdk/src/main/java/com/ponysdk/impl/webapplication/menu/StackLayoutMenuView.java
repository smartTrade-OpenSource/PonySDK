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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.core.terminal.PUnit;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PSimpleLayoutPanel;
import com.ponysdk.core.ui.basic.PStackLayoutPanel;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PSelectionEvent;
import com.ponysdk.core.ui.basic.event.PSelectionHandler;

public class StackLayoutMenuView extends PSimpleLayoutPanel implements MenuView {

    private final Node root = new Node(null, "ROOT");
    private final PStackLayoutPanel layoutPanel;
    private final double headerWidth = 2;// em
    private final double paddingLeft = 16;// px
    private final Map<Node, PComplexPanel> categoriesByNode = new LinkedHashMap<>();
    private final List<PSelectionHandler<MenuItem>> selectionHandlers = new ArrayList<>();
    private final Map<MenuItem, PAnchor> anchorByName = new LinkedHashMap<>();
    private PAnchor selectedItem;

    public StackLayoutMenuView() {
        layoutPanel = new PStackLayoutPanel(PUnit.EM);
        setWidget(layoutPanel);
        setSizeFull();
        layoutPanel.setSizeFull();
        layoutPanel.setAnimationDuration(Duration.ofMillis(100));
    }

    @Override
    public void addItem(final MenuItem menuItem) {
        final Node categoryNode = createCategoriesItemIfNeeded(menuItem.getCategories());
        if (menuItem.getName() != null) addItem(categoryNode, menuItem);
    }

    private Node createCategoriesItemIfNeeded(final Collection<String> categories) {
        Node current = root;
        for (final String category : categories) {
            Node categoryNode = current.getChild(category);
            if (categoryNode == null) {
                categoryNode = new Node(current, category);
                addCategory(categoryNode);
            }
            current = categoryNode;
        }
        return current;
    }

    private void addCategory(final Node categoryNode) {

        if (categoryNode.level == 1) {
            // Main category
            final PVerticalPanel categoryPanel = new PVerticalPanel();
            categoryPanel.setHeight("1px");
            categoryPanel.setWidth("100%");
            categoryPanel.ensureDebugId("category_" + categoryNode.name);

            categoryNode.ui = categoryPanel;
            layoutPanel.add(categoryPanel, categoryNode.name, true, headerWidth);
            categoriesByNode.put(categoryNode, categoryPanel);

        } else {

            // Sub category
            final Node parentCategory = categoryNode.parent;
            final PComplexPanel categoryPanel = categoriesByNode.get(parentCategory);
            if (categoryPanel == null)
                throw new IllegalArgumentException("Category '" + categoryNode.name + "' not assigned to a parent category");

            final PAnchor category = new PAnchor(categoryNode.name);
            applyPadding(categoryNode, category);
            applyExpandableStyle(categoryNode, category);
            category.addClickHandler(clickEvent -> {
                if (categoryNode.open) {
                    collapseNode(categoryNode);
                    applyExpandableStyle(categoryNode, category);
                } else {
                    expandNode(categoryNode);
                    applyExpandableStyle(categoryNode, category);
                }
            });

            if (!parentCategory.open) category.setVisible(false);

            categoryPanel.add(category);

            categoryNode.ui = category;
            categoriesByNode.put(categoryNode, categoryPanel);
        }
    }

    private void applyExpandableStyle(final Node categoryNode, final PAnchor category) {

        final String left = (categoryNode.level - 1) * paddingLeft - 10 + "px";
        category.setStyleProperty("backgroundPosition", left + " center");
        category.setStyleProperty("backgroundRepeat", "no-repeat");

        if (categoryNode.open) category.setStyleProperty("backgroundImage", "url('images/treeDownTriangleBlack.png')");
        else category.setStyleProperty("backgroundImage", "url('images/treeRightTriangleBlack.png')");
    }

    private void applyPadding(final Node categoryNode, final PAnchor category) {
        category.setStyleProperty("paddingLeft", (categoryNode.level - 1) * paddingLeft + "px");
    }

    private void addItem(final Node categoryNode, final MenuItem menuItem) {
        final Node itemNode = new Node(categoryNode, menuItem.getName());

        final PComplexPanel categoryPanel = categoriesByNode.get(categoryNode);
        final PAnchor item = new PAnchor(menuItem.getName());
        item.ensureDebugId("page_" + menuItem.getName());
        applyPadding(itemNode, item);
        item.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                final PSelectionEvent<MenuItem> event = new PSelectionEvent<>(this, menuItem);
                for (final PSelectionHandler<MenuItem> handler : selectionHandlers) {
                    handler.onSelection(event);
                }
            }
        });
        if (!categoryNode.open) item.setVisible(false);

        itemNode.ui = item;

        categoryPanel.add(item);
        anchorByName.put(menuItem, item);
    }

    protected void expandNode(final Node categoryNode) {
        categoryNode.open = true;
        for (final Node child : categoryNode.children) {
            child.ui.setVisible(true);
        }
    }

    protected void collapseNode(final Node categoryNode) {
        categoryNode.open = false;
        for (final Node child : categoryNode.children) {
            child.ui.setVisible(false);
        }
    }

    @Override
    public void selectItem(final MenuItem menuItem) {

        if (selectedItem != null) selectedItem.removeStyleName("selectedItem");

        final PAnchor item = anchorByName.get(menuItem);
        item.addStyleName("selectedItem");
        selectedItem = item;

        int i = 1;
        Node current = null;
        for (final String category : menuItem.getCategories()) {
            if (i == 1) {
                current = root.getChild(category);
                final PComplexPanel categoryPanel = categoriesByNode.get(current);
                if (categoryPanel != null) layoutPanel.showWidget(categoryPanel);
            } else {
                if (current != null) {
                    current = current.getChild(category);
                    if (current != null) {
                        if (!current.open) expandNode(current);
                    }
                }
            }
            i++;
        }
    }

    @Override
    public void addSelectionHandler(final PSelectionHandler<MenuItem> handler) {
        selectionHandlers.add(handler);
    }

    @Override
    public void removeSelectionHandler(final PSelectionHandler<MenuItem> handler) {
        selectionHandlers.remove(handler);
    }

    private class Node {

        private final String name;
        private final Node parent;
        private final List<Node> children;
        private final int level;
        private PWidget ui;
        private boolean open;

        public Node(final Node parent, final String name) {
            this.parent = parent;
            this.name = name;
            this.children = new ArrayList<>();

            if (parent != null) {
                this.parent.children.add(this);
                this.level = parent.level + 1;
            } else {
                this.level = 0;
            }

            this.open = level < 2;
        }

        public Node getChild(final String name) {
            for (final Node child : children) {
                if (child.name.equals(name)) return child;
            }
            return null;
        }

    }

}
