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

package com.ponysdk.core.ui.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;

/**
 * A group of {@link PRadioButton}s, where only one can be selected at a time.
 * <p>
 * This class manages the state of a collection of radio buttons, ensuring that
 * when one is selected, the previously selected one is deselected. It also
 * fires value change events when the selection changes.
 * </p>
 */
public class PRadioButtonGroupValue implements HasPValue<PRadioButton> {

    private final List<PRadioButton> radios;
    private List<PValueChangeHandler<PRadioButton>> handlers;

    private PRadioButton selected;

    private boolean initialized = false;
    private boolean initializedSubscribed;

    /**
     * Creates a new radio button group.
     *
     * @param radioButtons the list of radio buttons to group together. The list cannot be empty.
     * @throws IllegalArgumentException if the radioButtons list is empty.
     */
    protected PRadioButtonGroupValue(List<PRadioButton> radioButtons) {
        String name = "radio-group-" + UIContext.get().nextID();

        if (radioButtons.isEmpty()) throw new IllegalArgumentException("radioButtons list cannot be empty");

        this.radios = new ArrayList<>(radioButtons);
        for (PRadioButton radio : radioButtons) {
            radio.setName(name);

            radio.addValueChangeHandler(e -> {
                if (!initialized) {
                    // Do not handle event before the group is fully initialized.
                    return;
                }
                if (e.getData()) {
                    if (selected == radio) return;
                    setValue(radio, false);
                    if (handlers != null) {
                        PValueChangeEvent<PRadioButton> event = new PValueChangeEvent<>(this, selected);
                        for (final PValueChangeHandler<PRadioButton> handler : handlers) {
                            handler.onValueChange(event);
                        }
                    }
                }
            });
        }

        initialize();
    }

    /**
     * Adds a handler that will be notified when the selected radio button changes.
     * {@inheritDoc}
     */
    @Override
    public void addValueChangeHandler(final PValueChangeHandler<PRadioButton> handler) {
        if (handlers == null) handlers = new ArrayList<>();
        handlers.add(handler);
    }

    /**
     * Removes a previously added value change handler.
     * {@inheritDoc}
     */
    @Override
    public boolean removeValueChangeHandler(final PValueChangeHandler<PRadioButton> handler) {
        return handlers != null && handlers.remove(handler);
    }

    /**
     * Gets the collection of value change handlers.
     * {@inheritDoc}
     */
    @Override
    public Collection<PValueChangeHandler<PRadioButton>> getValueChangeHandlers() {
        return handlers != null ? Collections.unmodifiableCollection(handlers) : Collections.emptyList();
    }

    /**
     * Gets the currently selected radio button in the group.
     *
     * @return the selected {@link PRadioButton}. Can be {@code null} before the group is initialized.
     * After initialization, if no value was set, it defaults to the first radio button of the group.
     */
    @Override
    public PRadioButton getValue() {
        return selected;
    }

    private void setValue(PRadioButton value, boolean sendToTerminal) {
        selected = value;
        if (!initialize()) return;

        for (PRadioButton radio : radios) {
            if (radio == selected) {
                radio.setState(PCheckBoxState.CHECKED, sendToTerminal, false);
            } else {
                radio.setState(PCheckBoxState.UNCHECKED, false, false);
            }
        }
    }

    /**
     * Selects a radio button in the group.
     * <p>
     * This will deselect the previously selected button and update the new selection.
     * </p>
     *
     * @param value the radio button to select.
     */
    @Override
    public void setValue(PRadioButton value) {
        setValue(value, true);
    }

    private boolean initialize() {
        if (initialized) return true;

        boolean allInitialized = true;
        for (PRadioButton button : radios) {
            allInitialized &= button.isInitialized();
        }

        if (allInitialized) {
            initialized = true;
            if (selected != null) setValue(selected);
            else setValue(radios.get(0));
            return true;
        } else if (!initializedSubscribed) {
            initializedSubscribed = true;
            for (PRadioButton button : radios) {
                if (!button.isInitialized()) {
                    button.addInitializeListener(e -> initialize());
                }
            }
        }
        return false;
    }

}
