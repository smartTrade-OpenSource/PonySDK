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

package com.ponysdk.core.ui.component;

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ServerToClientModel;

/**
 * Abstract base class for Web Component-based PComponents.
 * <p>
 * PWebComponent extends {@link PComponent} and specifies Web Components as the target
 * UI framework. Components extending this class will be rendered using the
 * Web Components client-side adapter, which uses custom elements and shadow DOM
 * for encapsulated rendering.
 * </p>
 * <p>
 * This class implements {@link SlotSupport} to allow child component composition
 * via named slots, following the Web Components slot specification.
 * </p>
 *
 * @param <TProps> the props type, must be a Java Record
 * @see PComponent
 * @see SlotSupport
 * @see FrameworkType#WEB_COMPONENT
 */
public abstract class PWebComponent<TProps extends Record> extends PComponent<TProps> implements SlotSupport {

    private static final Logger log = LoggerFactory.getLogger(PWebComponent.class);

    private final Set<String> declaredSlots;

    /**
     * Creates a new PWebComponent with the specified initial props and no declared slots.
     *
     * @param initialProps the initial props state, must not be null
     */
    protected PWebComponent(final TProps initialProps) {
        this(initialProps, Collections.emptySet());
    }

    /**
     * Creates a new PWebComponent with the specified initial props and declared slots.
     *
     * @param initialProps  the initial props state, must not be null
     * @param declaredSlots the set of slot names declared by this component
     */
    protected PWebComponent(final TProps initialProps, final Set<String> declaredSlots) {
        super(initialProps, FrameworkType.WEB_COMPONENT);
        this.declaredSlots = declaredSlots != null ? Collections.unmodifiableSet(declaredSlots) : Collections.emptySet();
    }

    @Override
    public void addToSlot(final String slotName, final PComponent<?> child) {
        if (slotName == null) {
            addToDefaultSlot(child);
            return;
        }
        if (!declaredSlots.contains(slotName)) {
            log.warn("PWebComponent {} - slot '{}' is not declared. Declared slots: {}. Operation ignored.",
                getComponentSignature(), slotName, declaredSlots);
            return;
        }
        sendSlotOperation(slotName, child, "add");
    }

    @Override
    public void addToDefaultSlot(final PComponent<?> child) {
        sendSlotOperation(null, child, "add");
    }

    @Override
    public void removeFromSlot(final String slotName, final PComponent<?> child) {
        if (slotName == null) {
            sendSlotOperation(null, child, "remove");
            return;
        }
        if (!declaredSlots.contains(slotName)) {
            log.warn("PWebComponent {} - slot '{}' is not declared. Declared slots: {}. Operation ignored.",
                getComponentSignature(), slotName, declaredSlots);
            return;
        }
        sendSlotOperation(slotName, child, "remove");
    }

    @Override
    public Set<String> getDeclaredSlots() {
        return declaredSlots;
    }

    /**
     * Sends a slot operation message to the client.
     * <p>
     * For PTextComponent children, sends the text content directly.
     * For other components, sends the child object ID.
     * </p>
     * <p>
     * Message format for text: {@code {"type":"text","slotName":"prefix","content":"Hello"}}
     * Message format for component: {@code {"type":"slot","slotName":"prefix","childObjectId":43,"operation":"add"}}
     * </p>
     */
    private void sendSlotOperation(final String slotName, final PComponent<?> child, final String operation) {
        // Special handling for PTextComponent - send text content directly
        if (child instanceof PTextComponent) {
            final PTextComponent textComponent = (PTextComponent) child;
            final String text = textComponent.getText();
            
            final StringBuilder json = new StringBuilder(64);
            json.append("{\"type\":\"text\",\"slotName\":");
            if (slotName != null) {
                json.append('"').append(slotName).append('"');
            } else {
                json.append("\"default\"");
            }
            json.append(",\"content\":\"");
            // Escape quotes in text
            if (text != null) {
                json.append(text.replace("\"", "\\\""));
            }
            json.append("\"}");

            saveUpdate(writer -> {
                writer.write(ServerToClientModel.PCOMPONENT_UPDATE);
                writer.write(ServerToClientModel.PCOMPONENT_SLOT_OPERATION, json.toString());
            });
            return;
        }
        
        // Standard component slot operation
        final StringBuilder json = new StringBuilder(64);
        json.append("{\"type\":\"slot\",\"slotName\":");
        if (slotName != null) {
            json.append('"').append(slotName).append('"');
        } else {
            json.append("null");
        }
        json.append(",\"childObjectId\":").append(child.getID());
        json.append(",\"operation\":\"").append(operation).append("\"}");

        saveUpdate(writer -> {
            writer.write(ServerToClientModel.PCOMPONENT_UPDATE);
            writer.write(ServerToClientModel.PCOMPONENT_SLOT_OPERATION, json.toString());
        });
    }
}
