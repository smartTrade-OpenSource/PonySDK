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

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PDialogBox;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.component.PWebComponent;

/**
 * A modal dialog for selecting and configuring a WA widget before insertion into a slot.
 * <p>
 * The dialog contains a widget type selector (PListBox), a dynamically generated
 * properties form area, and Insert/Cancel buttons. It uses {@link FormGenerator}
 * to create property controls and {@link PropertyBinder} to apply configured values
 * to the instantiated widget.
 * </p>
 */
public class WidgetEditorDialog extends PDialogBox {

    /**
     * Functional interface for the insertion callback.
     */
    @FunctionalInterface
    public interface InsertCallback {
        void onInsert(PWebComponent<?> widgetInstance);
    }

    private final String slotDisplayName;
    private final List<String> availableWidgetNames;
    private final ComponentRegistry registry;
    private final MethodIntrospector introspector;
    private final InsertCallback callback;

    private final PListBox widgetSelector;
    private final PVerticalPanel propsFormContainer;
    private final PButton insertButton;
    private final PButton cancelButton;
    private final PLabel errorLabel;

    private final FormGenerator formGenerator;
    private final PropertyBinder propertyBinder;

    private Class<? extends PWebComponent<?>> selectedWidgetClass;
    private List<PropertyControl> currentControls = new ArrayList<>();

    /**
     * Creates a new WidgetEditorDialog.
     *
     * @param slotDisplayName      the display name of the target slot (shown in the caption)
     * @param availableWidgetNames the list of WA widget names available for selection
     * @param registry             the component registry for resolving widget classes
     * @param introspector         the method introspector for discovering setter methods
     * @param callback             the callback invoked on successful widget insertion
     */
    public WidgetEditorDialog(
            final String slotDisplayName,
            final List<String> availableWidgetNames,
            final ComponentRegistry registry,
            final MethodIntrospector introspector,
            final InsertCallback callback
    ) {
        super(true); // auto-hide enabled (Req 3.5)
        setGlassEnabled(true); // semi-transparent backdrop

        this.slotDisplayName = slotDisplayName;
        this.availableWidgetNames = availableWidgetNames;
        this.registry = registry;
        this.introspector = introspector;
        this.callback = callback;

        this.formGenerator = new FormGenerator();
        this.propertyBinder = new PropertyBinder();

        // Set caption (Req 3.2)
        setCaption("Insert Widget - " + slotDisplayName);
        addStyleName("widget-editor-dialog");

        // Build buttons first so they can be referenced in handlers (Req 3.3, 3.4)
        this.insertButton = Element.newPButton("Insert");
        this.insertButton.addStyleName("insert-button");
        this.insertButton.setEnabled(false); // disabled until widget selected (Req 1.5)
        this.insertButton.addClickHandler(event -> onInsertClicked());

        this.cancelButton = Element.newPButton("Cancel");
        this.cancelButton.addStyleName("cancel-button");
        this.cancelButton.addClickHandler(event -> hide());

        // Build widget selector (Req 1.1, 1.2)
        this.widgetSelector = Element.newPListBox(true); // contains empty item for no-selection
        this.widgetSelector.addStyleName("widget-selector");
        for (final String widgetName : availableWidgetNames) {
            final String tagName = extractTagName(widgetName);
            widgetSelector.addItem(tagName, widgetName);
        }
        widgetSelector.addChangeHandler(event -> {
            final int selectedIndex = widgetSelector.getSelectedIndex();
            if (selectedIndex <= 0) {
                // Empty item selected — clear form and disable Insert
                clearForm();
                insertButton.setEnabled(false);
                selectedWidgetClass = null;
            } else {
                final Object value = widgetSelector.getSelectedValue();
                onWidgetTypeSelected((String) value);
            }
        });

        // Build props form container
        this.propsFormContainer = Element.newPVerticalPanel();
        this.propsFormContainer.addStyleName("props-form-container");

        // Build error label
        this.errorLabel = Element.newPLabel();
        this.errorLabel.addStyleName("error-label");
        this.errorLabel.setVisible(false);

        // Layout
        final PVerticalPanel mainPanel = Element.newPVerticalPanel();
        mainPanel.add(widgetSelector);
        mainPanel.add(propsFormContainer);
        mainPanel.add(errorLabel);

        final PHorizontalPanel buttonPanel = Element.newPHorizontalPanel();
        buttonPanel.add(insertButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel);

        setWidget(mainPanel);
    }

    /**
     * Shows the dialog with no selection and Insert button disabled.
     */
    @Override
    public void show() {
        // Reset state (Req 1.4)
        widgetSelector.setSelectedIndex(0);
        clearForm();
        insertButton.setEnabled(false);
        selectedWidgetClass = null;
        clearError();
        super.show();
    }

    /**
     * Resets state and shows the dialog centered on screen.
     * Uses {@link #center()} which is required by PonySDK for proper popup positioning.
     * Call this instead of {@link #show()} when displaying from a click handler.
     */
    public void showCentered() {
        // Reset state (Req 1.4)
        widgetSelector.setSelectedIndex(0);
        clearForm();
        insertButton.setEnabled(false);
        selectedWidgetClass = null;
        clearError();
        center();
    }

    /**
     * Called when the user selects a widget type from the dropdown.
     * Resolves the class via the registry, introspects setter methods,
     * generates property controls via FormGenerator, and displays them.
     *
     * @param widgetName the WA class name (e.g. "WAIcon")
     */
    void onWidgetTypeSelected(final String widgetName) {
        clearError();
        clearForm();

        // Resolve class from registry (Req 1.3)
        selectedWidgetClass = registry.get(widgetName);
        if (selectedWidgetClass == null) {
            showError("Widget type not found: " + widgetName);
            insertButton.setEnabled(false);
            return;
        }

        // Discover setter methods and generate controls (Req 2.1, 2.2, 2.3)
        final List<MethodSignature> methods = introspector.discoverSetters(selectedWidgetClass);
        currentControls = formGenerator.generateControls(methods);

        // Display controls in the form container
        for (final PropertyControl pc : currentControls) {
            final PHorizontalPanel row = Element.newPHorizontalPanel();
            row.add(pc.label());
            row.add(pc.control());
            row.add(pc.errorLabel());
            propsFormContainer.add(row);
        }

        // Enable Insert button (Req 1.5)
        insertButton.setEnabled(true);
    }

    /**
     * Called when the user clicks the Insert button.
     * Instantiates the widget via reflection, applies property values
     * via PropertyBinder, invokes the callback, and closes the dialog.
     */
    void onInsertClicked() {
        if (selectedWidgetClass == null) {
            return;
        }

        clearError();

        // Instantiate widget via reflection (Req 4.1)
        final PWebComponent<?> widget;
        try {
            widget = selectedWidgetClass.getDeclaredConstructor().newInstance();
        } catch (final Exception e) {
            final String cause = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            showError("Failed to create " + extractTagName(getSelectedWidgetName()) + ": " + cause);
            return; // Stay open on failure (Req 4.6)
        }

        // Apply property values (Req 4.2)
        try {
            propertyBinder.bindControls(currentControls, widget);
        } catch (final Exception e) {
            showError("Failed to apply properties: " + e.getMessage());
            return; // Stay open on failure (Req 4.6)
        }

        // Invoke callback and close (Req 4.5)
        try {
            callback.onInsert(widget);
            hide();
        } catch (final Exception e) {
            showError("Failed to insert widget: " + e.getMessage());
        }
    }

    /**
     * Clears the properties form container and current controls list.
     */
    private void clearForm() {
        propsFormContainer.clear();
        currentControls = new ArrayList<>();
    }

    /**
     * Shows an error message in the dialog.
     */
    private void showError(final String message) {
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
    }

    /**
     * Clears the error message.
     */
    private void clearError() {
        errorLabel.setVisible(false);
    }

    /**
     * Returns the currently selected widget name (WA class name), or null if none selected.
     */
    private String getSelectedWidgetName() {
        final int index = widgetSelector.getSelectedIndex();
        if (index <= 0) return null;
        return (String) widgetSelector.getSelectedValue();
    }

    /**
     * Converts a WA class name to its kebab-case tag name.
     * Same logic as ComponentPlayground.extractTagName().
     */
    static String extractTagName(final String componentName) {
        if (componentName.startsWith("WA")) {
            final String withoutPrefix = componentName.substring(2);
            return "wa-" + camelToKebab(withoutPrefix);
        }
        return "wa-" + camelToKebab(componentName);
    }

    /**
     * Converts a camelCase string to kebab-case.
     */
    static String camelToKebab(final String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    // ========== Accessors for testing ==========

    PListBox getWidgetSelector() {
        return widgetSelector;
    }

    PVerticalPanel getPropsFormContainer() {
        return propsFormContainer;
    }

    PButton getInsertButton() {
        return insertButton;
    }

    PButton getCancelButton() {
        return cancelButton;
    }

    PLabel getErrorLabel() {
        return errorLabel;
    }

    Class<? extends PWebComponent<?>> getSelectedWidgetClass() {
        return selectedWidgetClass;
    }

    List<PropertyControl> getCurrentControls() {
        return currentControls;
    }

    String getSlotDisplayName() {
        return slotDisplayName;
    }
}
