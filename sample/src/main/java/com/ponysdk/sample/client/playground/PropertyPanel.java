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

import java.util.List;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.component.PComponent;

/**
 * Right panel containing component preview and property controls.
 * <p>
 * The panel is organized vertically with:
 * <ul>
 *   <li>Top: Component preview container (200px min height, light background)</li>
 *   <li>Bottom: Scrollable form controls for property manipulation</li>
 * </ul>
 * </p>
 * <p>
 * Requirements: 8.3, 8.4, 8.5, 10.1, 10.3, 10.4
 * </p>
 */
public class PropertyPanel extends PVerticalPanel {

    private static final String MIN_HEIGHT_PREVIEW = "200px";
    private static final String VERTICAL_SPACING = "10px";

    private final PSimplePanel previewContainer;
    private final PScrollPanel formScrollPanel;
    private final PVerticalPanel formContainer;
    private final PVerticalPanel slotsContainer;

    /**
     * Creates a new PropertyPanel.
     */
    public PropertyPanel() {
        this.previewContainer = Element.newPSimplePanel();
        this.formScrollPanel = Element.newPScrollPanel();
        this.formContainer = Element.newPVerticalPanel();
        this.slotsContainer = Element.newPVerticalPanel();
        
        initializePanel();
    }

    /**
     * Initializes the panel structure and styling.
     */
    private void initializePanel() {
        // Set up main panel
        setWidth("100%");
        setHeight("100%");
        addStyleName("property-panel");

        // Configure preview container
        previewContainer.setWidth("100%");
        previewContainer.addStyleName("component-preview");
        
        // Show placeholder message initially
        showPlaceholder();
        
        // Configure form scroll panel
        formScrollPanel.setWidth("100%");
        formScrollPanel.setHeight("100%");
        formScrollPanel.addStyleName("form-scroll-panel");

        // Configure form container
        formContainer.setWidth("100%");
        formContainer.addStyleName("form-container");

        // Configure slots container
        slotsContainer.setWidth("100%");
        slotsContainer.addStyleName("slots-container");
        slotsContainer.setVisible(false); // Hidden by default

        // Create main container for form and slots
        final PVerticalPanel mainContainer = Element.newPVerticalPanel();
        mainContainer.setWidth("100%");
        mainContainer.add(slotsContainer);
        mainContainer.add(formContainer);

        // Add main container to scroll panel
        formScrollPanel.setWidget(mainContainer);

        // Add both sections to main panel
        add(previewContainer);
        add(formScrollPanel);
    }

    /**
     * Shows a placeholder message when no component is selected.
     */
    private void showPlaceholder() {
        final PVerticalPanel placeholder = Element.newPVerticalPanel();
        placeholder.setWidth("100%");
        placeholder.addStyleName("placeholder-message");
        
        final PLabel icon = Element.newPLabel("🎨");
        icon.addStyleName("placeholder-icon");
        
        final PLabel message = Element.newPLabel("Select a component from the list");
        message.addStyleName("placeholder-text");
        
        final PLabel hint = Element.newPLabel("Choose a Web Awesome component to start testing");
        hint.addStyleName("placeholder-hint");
        
        placeholder.add(icon);
        placeholder.add(message);
        placeholder.add(hint);
        
        previewContainer.setWidget(placeholder);
    }

    /**
     * Sets the component instance to display in the preview container.
     *
     * @param component the component to preview, must not be null
     * @throws IllegalArgumentException if component is null
     */
    public void setPreviewComponent(final PComponent<?> component) {
        if (component == null) {
            throw new IllegalArgumentException("component must not be null");
        }

        // Clear previous content
        previewContainer.clear();

        // For PWebComponent, we need to create a div container and let the component render itself
        final com.ponysdk.core.ui.basic.PElement container = com.ponysdk.core.ui.basic.Element.newDiv();
        container.setAttribute("id", "preview-container-" + component.getID());
        container.addStyleName("web-component-preview");
        
        // Add the container to the preview
        previewContainer.setWidget(container);
        
        // Attach the component - it will render itself via JavaScript
        component.attach(com.ponysdk.core.ui.basic.PWindow.getMain());
        
        // Use JavaScript to move the component into our container
        final String script = String.format(
            "(function() {" +
            "  setTimeout(function() {" +
            "    var component = document.getElementById('pcomponent-%d');" +
            "    var container = document.getElementById('preview-container-%d');" +
            "    if (component && container) {" +
            "      container.appendChild(component);" +
            "      console.log('Component moved to preview container');" +
            "    } else {" +
            "      console.error('Component or container not found', component, container);" +
            "    }" +
            "  }, 200);" +
            "})()",
            component.getID(),
            component.getID()
        );
        
        com.ponysdk.core.ui.basic.PScript.execute(com.ponysdk.core.ui.basic.PWindow.getMain(), script);
    }

    /**
     * Adds slot controls to the slots area.
     *
     * @param slotControls the list of slot controls to display, must not be null
     * @throws IllegalArgumentException if slotControls is null
     */
    public void setSlotControls(final List<SlotControl> slotControls) {
        if (slotControls == null) {
            throw new IllegalArgumentException("slotControls must not be null");
        }

        // Clear existing slots
        slotsContainer.clear();

        if (slotControls.isEmpty()) {
            slotsContainer.setVisible(false);
            return;
        }

        // Add section header
        final PLabel slotsHeader = Element.newPLabel("Slots");
        slotsHeader.addStyleName("section-header");
        slotsContainer.add(slotsHeader);

        // Add each slot control as a row
        for (final SlotControl slotControl : slotControls) {
            final PVerticalPanel slotRow = createSlotRow(slotControl);
            slotsContainer.add(slotRow);
        }

        slotsContainer.setVisible(true);
    }

    /**
     * Creates a vertical row for a slot control.
     */
    private PVerticalPanel createSlotRow(final SlotControl slotControl) {
        final PVerticalPanel row = Element.newPVerticalPanel();
        row.setWidth("100%");
        row.addStyleName("slot-row");

        // Create horizontal panel for label and textbox
        final PHorizontalPanel inputRow = Element.newPHorizontalPanel();
        inputRow.setWidth("100%");
        inputRow.addStyleName("input-row");

        // Add label (fixed width for alignment)
        slotControl.getLabel().setWidth("150px");
        inputRow.add(slotControl.getLabel());

        // Add textbox (flexible width)
        slotControl.getTextBox().setWidth("100%");
        inputRow.add(slotControl.getTextBox());
        inputRow.setCellWidth(slotControl.getTextBox(), "100%");

        // Add input row to main row
        row.add(inputRow);

        // Add error label below
        slotControl.getErrorLabel().setWidth("100%");
        row.add(slotControl.getErrorLabel());

        return row;
    }

    /**
     * Adds property controls to the form area.
     * <p>
     * Each control is displayed in a horizontal row with consistent 10px vertical spacing.
     * </p>
     *
     * @param controls the list of property controls to display, must not be null
     * @throws IllegalArgumentException if controls is null
     */
    public void setPropertyControls(final List<PropertyControl> controls) {
        if (controls == null) {
            throw new IllegalArgumentException("controls must not be null");
        }

        // Clear existing controls
        formContainer.clear();

        // Add section header if there are controls
        if (!controls.isEmpty()) {
            final PLabel propsHeader = Element.newPLabel("Properties");
            propsHeader.addStyleName("section-header");
            formContainer.add(propsHeader);
        }

        // Add each property control as a row
        for (final PropertyControl control : controls) {
            final PVerticalPanel controlRow = createControlRow(control);
            formContainer.add(controlRow);
        }
    }

    /**
     * Creates a vertical row for a property control.
     * <p>
     * The row contains:
     * <ul>
     *   <li>A horizontal panel with label and input control</li>
     *   <li>An error label below (initially hidden)</li>
     * </ul>
     * </p>
     *
     * @param control the property control to create a row for
     * @return a PVerticalPanel containing the control row
     */
    private PVerticalPanel createControlRow(final PropertyControl control) {
        final PVerticalPanel row = Element.newPVerticalPanel();
        row.setWidth("100%");
        row.addStyleName("control-row");

        // Create horizontal panel for label and control
        final PHorizontalPanel inputRow = Element.newPHorizontalPanel();
        inputRow.setWidth("100%");
        inputRow.addStyleName("input-row");

        // Add label (fixed width for alignment)
        control.label().setWidth("150px");
        inputRow.add(control.label());

        // Add control (flexible width)
        control.control().setWidth("100%");
        inputRow.add(control.control());
        inputRow.setCellWidth(control.control(), "100%");

        // Add input row to main row
        row.add(inputRow);

        // Add error label below
        control.errorLabel().setWidth("100%");
        row.add(control.errorLabel());

        return row;
    }

    /**
     * Clears all content from the property panel.
     * <p>
     * Removes both the preview component and all form controls.
     * </p>
     */
    public void clear() {
        previewContainer.clear();
        formContainer.clear();
        slotsContainer.clear();
        slotsContainer.setVisible(false);
    }

    /**
     * Displays an error message in the property panel.
     * <p>
     * Used when component instantiation fails.
     * </p>
     *
     * @param errorMessage the error message to display, must not be null
     * @throws IllegalArgumentException if errorMessage is null
     */
    public void showError(final String errorMessage) {
        if (errorMessage == null) {
            throw new IllegalArgumentException("errorMessage must not be null");
        }

        clear();

        final PLabel errorLabel = Element.newPLabel("⚠ " + errorMessage);
        errorLabel.addStyleName("error-message");
        previewContainer.setWidget(errorLabel);
    }

    /**
     * Gets the preview container.
     *
     * @return the preview container panel
     */
    public PSimplePanel getPreviewContainer() {
        return previewContainer;
    }

    /**
     * Gets the form container.
     *
     * @return the form container panel
     */
    public PVerticalPanel getFormContainer() {
        return formContainer;
    }
}
