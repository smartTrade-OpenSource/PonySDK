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
import com.ponysdk.core.ui.component.PTextComponent;
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
    private PTextComponent currentTextComponent;

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
        System.out.println("[SlotControl] Binding slot '" + slotName + "' to component");
        
        // Update on value change (blur or Enter key)
        textBox.addValueChangeHandler(event -> {
            System.out.println("[SlotControl] ValueChangeHandler triggered for slot '" + slotName + "'");
            final String text = textBox.getValue();
            System.out.println("[SlotControl] Text value: '" + text + "'");
            final boolean success = updateSlotContent(component, text);
            System.out.println("[SlotControl] updateSlotContent result: " + success);
            if (success) {
                clearError();
            }
        });
        
        // Also update on key up for real-time feedback
        textBox.addKeyUpHandler(event -> {
            System.out.println("[SlotControl] KeyUpHandler triggered for slot '" + slotName + "'");
            final String text = textBox.getValue();
            System.out.println("[SlotControl] Text value: '" + text + "'");
            final boolean success = updateSlotContent(component, text);
            System.out.println("[SlotControl] updateSlotContent result: " + success);
            if (success) {
                clearError();
            }
        });
        
        System.out.println("[SlotControl] Handlers registered for slot '" + slotName + "'");
    }

    /**
     * Updates the slot content with the given text.
     * <p>
     * This method manages the lifecycle of PTextComponent instances in slots:
     * - If text is empty, removes any existing content from the slot
     * - If text is non-empty, creates a new PTextComponent and adds it to the slot
     * - Replaces existing content if the slot already contains a component
     * </p>
     * 
     * @return true if the operation succeeded, false if validation failed
     */
    boolean updateSlotContent(final PWebComponent<?> component, final String text) {
        System.out.println("[SlotControl] updateSlotContent called for slot '" + slotName + "' with text: '" + text + "'");
        
        // Validate text length
        if (text != null && text.length() > 10000) {
            System.out.println("[SlotControl] Text too long: " + text.length() + " characters");
            showError("Text exceeds maximum length of 10000 characters");
            return false;
        }

        // Validate slot exists (for named slots)
        if (!slotName.isEmpty() && !component.getDeclaredSlots().contains(slotName)) {
            System.out.println("[SlotControl] Slot '" + slotName + "' does not exist on component");
            showError("Slot '" + slotName + "' does not exist on this component");
            return false;
        }

        // Remove existing content if present
        if (currentTextComponent != null) {
            System.out.println("[SlotControl] Removing existing text component");
            try {
                if (slotName.isEmpty()) {
                    component.removeFromSlot(null, currentTextComponent);
                } else {
                    component.removeFromSlot(slotName, currentTextComponent);
                }
            } catch (final Exception e) {
                System.out.println("[SlotControl] Failed to remove old content: " + e.getMessage());
                showError("Failed to remove old content: " + e.getMessage());
                return false;
            }
            currentTextComponent = null;
        }

        // Add new content if text is non-empty
        if (text != null && !text.trim().isEmpty()) {
            System.out.println("[SlotControl] Creating new PTextComponent with text: '" + text + "'");
            try {
                currentTextComponent = new PTextComponent(text);
                if (slotName.isEmpty()) {
                    System.out.println("[SlotControl] Adding to default slot");
                    component.addToDefaultSlot(currentTextComponent);
                } else {
                    System.out.println("[SlotControl] Adding to named slot: " + slotName);
                    component.addToSlot(slotName, currentTextComponent);
                }
                System.out.println("[SlotControl] Successfully added text component");
            } catch (final Exception e) {
                System.out.println("[SlotControl] Failed to add content: " + e.getMessage());
                e.printStackTrace();
                showError("Failed to add content: " + e.getMessage());
                currentTextComponent = null;
                return false;
            }
        } else {
            System.out.println("[SlotControl] Text is empty, not adding content");
        }
        
        System.out.println("[SlotControl] updateSlotContent completed successfully");
        return true;
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
