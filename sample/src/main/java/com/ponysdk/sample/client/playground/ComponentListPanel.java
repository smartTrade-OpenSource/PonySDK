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
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.event.PChangeEvent;
import com.ponysdk.core.ui.basic.event.PChangeHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Left panel containing the component selection list with search functionality.
 * <p>
 * Displays all discovered Web Awesome components in a list box with:
 * <ul>
 *   <li>Search textbox for filtering components</li>
 *   <li>Real-time filtering as user types</li>
 *   <li>Result count display</li>
 *   <li>Empty state when no matches found</li>
 *   <li>Selection preservation during filtering</li>
 *   <li>Alphabetically sorted component names</li>
 * </ul>
 * </p>
 */
public class ComponentListPanel extends PSimplePanel {

    private final PFlowPanel containerPanel;
    private final PListBox listBox;
    private SearchPanel searchPanel;
    private PLabel emptyStateLabel;
    private Consumer<String> selectionHandler;

    // Search-related state
    private List<String> allComponents;
    private List<String> filteredComponents;
    private String currentQuery;
    private Debouncer searchDebouncer;
    private String selectedBeforeFilter;

    /**
     * Creates a new ComponentListPanel.
     */
    public ComponentListPanel() {
        this.containerPanel = Element.newPFlowPanel();
        this.listBox = Element.newPListBox();
        this.allComponents = new ArrayList<>();
        this.filteredComponents = new ArrayList<>();
        this.currentQuery = "";

        initializePanel();
    }

    /**
     * Initializes the panel structure and styling.
     */
    private void initializePanel() {
        listBox.setWidth("100%");
        listBox.setVisibleItemCount(30);
        listBox.addStyleName("component-list");

        listBox.addChangeHandler(new PChangeHandler() {
            @Override
            public void onChange(final PChangeEvent event) {
                handleSelectionChange();
            }
        });

        emptyStateLabel = Element.newPLabel("");
        emptyStateLabel.addStyleName("empty-state-message");
        emptyStateLabel.setAttribute("aria-live", "polite");
        emptyStateLabel.setVisible(false);

        containerPanel.add(listBox);
        containerPanel.add(emptyStateLabel);

        setWidget(containerPanel);

        addStyleName("component-list-panel");
        setWidth("100%");
        setHeight("100%");
    }

    /**
     * Initializes the search panel. Called when component names are first set.
     */
    private void initializeSearchPanel() {
        if (searchPanel != null) {
            return;
        }

        searchPanel = new SearchPanel();
        searchPanel.setSearchHandler(this::handleSearchChange);
        searchDebouncer = new Debouncer(50);

        containerPanel.insert(searchPanel, 0);
    }

    /**
     * Handles search text changes with debouncing.
     *
     * @param query the search query
     */
    private void handleSearchChange(final String query) {
        searchDebouncer.debounce(() -> applyFilter(query));
    }

    /**
     * Applies the filter to the component list.
     *
     * @param query the search query
     */
    private void applyFilter(final String query) {
        currentQuery = query;
        filteredComponents = ComponentFilter.filter(allComponents, query);

        preserveSelection();
        updateListDisplay();
        updateResultCount();
        updateEmptyState();
    }

    /**
     * Updates the list box to display filtered components.
     */
    private void updateListDisplay() {
        listBox.clear();

        for (final String componentName : filteredComponents) {
            listBox.addItem(componentName);
        }

        // Restore selection if component is still visible
        if (selectedBeforeFilter != null && filteredComponents.contains(selectedBeforeFilter)) {
            final int index = filteredComponents.indexOf(selectedBeforeFilter);
            if (index >= 0) {
                listBox.setSelectedIndex(index);
            }
        }
    }

    /**
     * Updates the result count display.
     */
    private void updateResultCount() {
        if (searchPanel != null) {
            searchPanel.setResultCount(filteredComponents.size(), allComponents.size());
        }
    }

    /**
     * Updates the empty state message visibility.
     */
    private void updateEmptyState() {
        final boolean isEmpty = filteredComponents.isEmpty();

        if (isEmpty) {
            final String message = "No components found"
                + (currentQuery != null && !currentQuery.trim().isEmpty()
                    ? " for '" + currentQuery + "'"
                    : "");
            emptyStateLabel.setText(message);
            emptyStateLabel.setVisible(true);
            listBox.setVisible(false);
        } else {
            emptyStateLabel.setVisible(false);
            listBox.setVisible(true);
        }
    }

    /**
     * Preserves the current selection if it matches the filter.
     * Clears the selection if the current selection doesn't match.
     */
    private void preserveSelection() {
        final String currentSelection = getSelectedComponent();

        if (currentSelection != null) {
            if (!ComponentFilter.matches(currentSelection, currentQuery)) {
                listBox.setSelectedIndex(-1);
                if (selectionHandler != null) {
                    selectionHandler.accept(null);
                }
            }
        }
    }

    /**
     * Populates the list box with component names.
     *
     * @param componentNames the list of component names to display, must not be null
     * @throws IllegalArgumentException if componentNames is null
     */
    public void setComponentNames(final List<String> componentNames) {
        if (componentNames == null) {
            throw new IllegalArgumentException("componentNames must not be null");
        }

        allComponents = new ArrayList<>(componentNames);
        filteredComponents = new ArrayList<>(componentNames);
        currentQuery = "";

        initializeSearchPanel();

        updateListDisplay();
        updateResultCount();
        updateEmptyState();
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

        if (selectedIndex >= 0) {
            final String selectedComponent = listBox.getSelectedItem();
            selectedBeforeFilter = selectedComponent;

            if (selectionHandler != null) {
                selectionHandler.accept(selectedComponent);
            }
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
        allComponents.clear();
        filteredComponents.clear();
    }

    /**
     * Displays an error message in the component list.
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

        if (searchPanel != null) {
            searchPanel.setVisible(false);
        }
    }

    /**
     * Gets the search panel for testing purposes.
     *
     * @return the search panel, or null if not initialized
     */
    SearchPanel getSearchPanel() {
        return searchPanel;
    }

    /**
     * Gets the empty state label for testing purposes.
     *
     * @return the empty state label
     */
    PLabel getEmptyStateLabel() {
        return emptyStateLabel;
    }
}
