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
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.event.PKeyUpEvent;

import java.util.function.Consumer;

/**
 * Search panel component for filtering components.
 * <p>
 * Provides a search textbox, clear button, and result count display.
 * </p>
 * <p>
 * Requirements: 1.1, 1.2, 1.3, 1.4, 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 7.1, 7.2, 7.3, 7.4
 * </p>
 */
public class SearchPanel extends PFlowPanel {

    private final PTextBox searchBox;
    private final PButton clearButton;
    private final PLabel resultCount;
    private Consumer<String> searchHandler;

    /**
     * Creates a new SearchPanel.
     */
    public SearchPanel() {
        // Initialize UI components
        this.searchBox = Element.newPTextBox();
        this.clearButton = Element.newPButton("\u00d7");
        this.resultCount = Element.newPLabel("");

        // Initialize the panel
        initializePanel();
    }

    /**
     * Initializes the panel structure and styling.
     */
    private void initializePanel() {
        // Configure search textbox
        searchBox.setPlaceholder("Search components...");
        searchBox.setWidth("100%");
        searchBox.setAttribute("aria-label", "Search components");

        // Add keyup event handler
        searchBox.addKeyUpHandler(this::handleKeyUp);

        // Configure clear button
        clearButton.addStyleName("search-clear-button");
        clearButton.setAttribute("aria-label", "Clear search");
        clearButton.setVisible(false);
        clearButton.addClickHandler(event -> handleClearClick());

        // Configure result count
        resultCount.addStyleName("search-result-count");
        resultCount.setAttribute("aria-live", "polite");

        // Add components to panel
        final PFlowPanel searchBoxContainer = Element.newPFlowPanel();
        searchBoxContainer.addStyleName("search-box-container");
        searchBoxContainer.add(searchBox);
        searchBoxContainer.add(clearButton);

        add(searchBoxContainer);
        add(resultCount);

        // Apply panel styling
        addStyleName("search-panel");
    }

    /**
     * Handles keyup events from the search textbox.
     */
    private void handleKeyUp(final PKeyUpEvent event) {
        // Update clear button visibility
        updateClearButtonVisibility();

        // Handle Escape key
        if (event.getKeyCode() == 27) { // Escape key
            clearSearch();
            searchBox.blur();
            if (searchHandler != null) {
                searchHandler.accept("");
            }
            return;
        }

        // Notify search handler
        if (searchHandler != null) {
            searchHandler.accept(searchBox.getText());
        }
    }

    /**
     * Handles clear button click events.
     */
    private void handleClearClick() {
        clearSearch();
        if (searchHandler != null) {
            searchHandler.accept("");
        }
    }

    /**
     * Updates clear button visibility based on text presence.
     */
    private void updateClearButtonVisibility() {
        final String text = searchBox.getText();
        final boolean hasText = text != null && !text.isEmpty();
        clearButton.setVisible(hasText);
    }

    /**
     * Sets the search handler to be called when search text changes.
     *
     * @param handler the search handler, receives the search text
     */
    public void setSearchHandler(final Consumer<String> handler) {
        this.searchHandler = handler;
    }

    /**
     * Gets the current search text.
     *
     * @return the search text, never null
     */
    public String getSearchText() {
        final String text = searchBox.getText();
        return text != null ? text : "";
    }

    /**
     * Clears the search textbox.
     */
    public void clearSearch() {
        searchBox.setText("");
        updateClearButtonVisibility();
    }

    /**
     * Sets focus to the search textbox.
     */
    public void focus() {
        searchBox.focus();
    }

    /**
     * Sets the result count display.
     *
     * @param filtered the number of filtered components
     * @param total    the total number of components
     */
    public void setResultCount(final int filtered, final int total) {
        final String text = filtered + " of " + total + " components";
        resultCount.setText(text);
    }
}
