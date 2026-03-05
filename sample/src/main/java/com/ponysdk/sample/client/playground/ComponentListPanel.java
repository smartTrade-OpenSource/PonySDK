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

package com.ponysdk.sample.client.playground;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.event.PChangeEvent;
import com.ponysdk.core.ui.basic.event.PChangeHandler;

import java.util.List;
import java.util.function.Consumer;

/**
 * Left panel containing the component selection list.
 * <p>
 * Displays all discovered Web Awesome components in a list box with:
 * <ul>
 *   <li>Alphabetically sorted component names</li>
 *   <li>Visible border separating from property panel</li>
 *   <li>Selection event handling</li>
 * </ul>
 * </p>
 * <p>
 * Requirements: 2.1, 2.2, 8.2
 * </p>
 */
public class ComponentListPanel extends PSimplePanel {

    private final PListBox listBox;
    private Consumer<String> selectionHandler;

    /**
     * Creates a new ComponentListPanel.
     */
    public ComponentListPanel() {
        this.listBox = Element.newPListBox();
        initializePanel();
    }

    /**
     * Initializes the panel structure and styling.
     */
    private void initializePanel() {
        // Set up the list box
        listBox.setWidth("100%");
        listBox.setHeight("100%");
        listBox.setVisibleItemCount(20); // Show multiple items at once
        listBox.addStyleName("component-list");

        // Add change handler for selection events
        listBox.addChangeHandler(new PChangeHandler() {
            @Override
            public void onChange(final PChangeEvent event) {
                handleSelectionChange();
            }
        });

        // Add list box to panel
        setWidget(listBox);

        // Apply panel styling with visible border
        addStyleName("component-list-panel");
        setWidth("100%");
        setHeight("100%");
    }

    /**
     * Populates the list box with component names.
     * <p>
     * The component names should already be sorted alphabetically by the caller.
     * </p>
     *
     * @param componentNames the list of component names to display, must not be null
     * @throws IllegalArgumentException if componentNames is null
     */
    public void setComponentNames(final List<String> componentNames) {
        if (componentNames == null) {
            throw new IllegalArgumentException("componentNames must not be null");
        }

        // Clear existing items
        listBox.clear();

        // Add all component names
        for (final String componentName : componentNames) {
            listBox.addItem(componentName);
        }
    }

    /**
     * Sets the selection handler to be called when a component is selected.
     *
     * @param handler the selection handler, receives the selected component name
     */
    public void setSelectionHandler(final Consumer<String> handler) {
        this.selectionHandler = handler;
    }

    /**
     * Handles selection change events from the list box.
     */
    private void handleSelectionChange() {
        final int selectedIndex = listBox.getSelectedIndex();
        
        if (selectedIndex >= 0 && selectionHandler != null) {
            final String selectedComponent = listBox.getSelectedItem();
            selectionHandler.accept(selectedComponent);
        }
    }

    /**
     * Gets the currently selected component name.
     *
     * @return the selected component name, or null if no selection
     */
    public String getSelectedComponent() {
        final int selectedIndex = listBox.getSelectedIndex();
        return selectedIndex >= 0 ? listBox.getSelectedItem() : null;
    }

    /**
     * Clears the component list.
     */
    public void clear() {
        listBox.clear();
    }

    /**
     * Displays an error message in the component list.
     * <p>
     * Used when component discovery fails.
     * </p>
     *
     * @param errorMessage the error message to display, must not be null
     * @throws IllegalArgumentException if errorMessage is null
     */
    public void showError(final String errorMessage) {
        if (errorMessage == null) {
            throw new IllegalArgumentException("errorMessage must not be null");
        }

        listBox.clear();
        listBox.addItem("Error: " + errorMessage);
    }
}
