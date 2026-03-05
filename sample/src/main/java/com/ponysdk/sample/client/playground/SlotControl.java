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
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.component.PWebComponent;

/**
 * Represents a slot control in the playground UI.
 * <p>
 * A slot control allows users to set text content for a component slot.
 * When text is entered, it creates a PLabel and adds it to the appropriate slot.
 * </p>
 */
public class SlotControl {

    private final String slotName;
    private final String description;
    private final PLabel label;
    private final PTextBox textBox;
    private final PLabel errorLabel;

    /**
     * Creates a new SlotControl.
     *
     * @param slotMetadata the slot metadata
     */
    public SlotControl(final SlotMetadata slotMetadata) {
        this.slotName = slotMetadata.name();
        this.description = slotMetadata.description();
        
        // Create label
        final String displayName = slotName.isEmpty() ? "(default)" : slotName;
        this.label = Element.newPLabel(displayName);
        this.label.addStyleName("slot-label");
        
        // Create text box
        this.textBox = Element.newPTextBox();
        this.textBox.setPlaceholder(description.isEmpty() ? "Enter text..." : description);
        this.textBox.addStyleName("slot-textbox");
        
        // Create error label
        this.errorLabel = Element.newPLabel();
        this.errorLabel.addStyleName("error-label");
        this.errorLabel.setVisible(false);
    }

    /**
     * Binds this slot control to a component instance.
     *
     * @param component the component to bind to
     */
    public void bindTo(final PWebComponent<?> component) {
        textBox.addValueChangeHandler(event -> {
            try {
                final String text = textBox.getValue();
                updateSlotContent(component, text);
                clearError();
            } catch (final Exception e) {
                showError("Failed to update slot: " + e.getMessage());
            }
        });
    }

    /**
     * Updates the slot content with the given text.
     * <p>
     * Note: PLabel is not a PComponent, so we cannot add it to slots.
     * This is a limitation - slots can only contain PComponent instances.
     * For now, we'll log a warning. A future enhancement could create
     * a simple PTextComponent wrapper.
     * </p>
     */
    private void updateSlotContent(final PWebComponent<?> component, final String text) {
        // TODO: Create a PTextComponent or similar to wrap text content
        // For now, just show a message that text slots are not yet supported
        if (text != null && !text.trim().isEmpty()) {
            showError("Text slots not yet implemented - need PComponent wrapper");
        }
    }

    /**
     * Shows an error message.
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

    public PLabel getLabel() {
        return label;
    }

    public PTextBox getTextBox() {
        return textBox;
    }

    public PLabel getErrorLabel() {
        return errorLabel;
    }

    public String getSlotName() {
        return slotName;
    }
}
