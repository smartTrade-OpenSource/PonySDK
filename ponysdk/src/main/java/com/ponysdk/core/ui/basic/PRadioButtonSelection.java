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
 * <p>
 * This class provides an improved alternative to {@link PRadioButtonGroup} with proper
 * value change event behavior: {@link #setValue(PRadioButton)} does NOT fire value change
 * events (programmatic changes), while user interactions DO fire events. This follows
 * standard UI patterns and prevents unwanted side effects during initialization or
 * programmatic updates.
 * </p>
 *
 * <h3>Initialization</h3>
 * <p>
 * The group waits for all radio buttons to be initialized on the terminal (browser) before
 * applying the initial selection. This prevents race conditions where the server-side state
 * would be set before the widgets exist on the client side.
 * </p>
 * <p>
 * During initialization:
 * <ul>
 * <li>If {@link #setValue(PRadioButton)} is called before initialization completes, the selection
 * is stored but not applied until all buttons are ready</li>
 * <li>Once all buttons are initialized, the stored selection is applied, or the first button
 * is selected by default if no selection was made</li>
 * <li>Value change events from user interactions are ignored until initialization completes</li>
 * </ul>
 * </p>
 */
public class PRadioButtonSelection implements HasPValue<PRadioButton> {

    private final List<PRadioButton> radios;
    private List<PValueChangeHandler<PRadioButton>> handlers;

    private PRadioButton selected;

    private boolean initialized;
    private boolean initializedSubscribed;

    /**
     * Creates a new radio button selection group.
     * <p>
     * All radio buttons are assigned a unique group name and configured to work together.
     * The initialization process begins immediately but may complete asynchronously if the
     * buttons are not yet initialized on the terminal.
     * </p>
     *
     * @param radioButtons the list of radio buttons to group together. The list cannot be empty.
     * @throws IllegalArgumentException if the radioButtons list is empty.
     */
    protected PRadioButtonSelection(Collection<PRadioButton> radioButtons) {
        if (radioButtons.isEmpty()) throw new IllegalArgumentException("radioButtons list cannot be empty");

        String name = "radio-group-" + UIContext.get().nextID();

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
     * Selects a radio button in the group programmatically.
     * <p>
     * This will deselect the previously selected button and update the new selection.
     * If the group is not yet fully initialized, the selection is stored and will be
     * applied once all buttons are ready on the terminal.
     * </p>
     * <p>
     * <b>Note:</b> This method does NOT fire value change events. Value change events are only
     * fired when the user interacts with the radio buttons in the browser. This is consistent
     * with standard UI behavior where programmatic changes don't trigger change handlers.
     * </p>
     *
     * @param value the radio button to select.
     */
    @Override
    public void setValue(PRadioButton value) {
        setValue(value, true);
    }

    /**
     * Initializes the group once all radio buttons are ready on the terminal.
     * <p>
     * This method checks if all buttons have been initialized (sent to and acknowledged by the browser).
     * If not all buttons are ready, it subscribes to their initialization events and will retry
     * when they become available.
     * </p>
     * <p>
     * Once initialization completes:
     * <ul>
     * <li>The stored selection (if any) is applied to the buttons</li>
     * <li>If no selection was made, the first button is selected by default</li>
     * <li>The group starts handling user interaction events</li>
     * </ul>
     * </p>
     *
     * @return {@code true} if initialization is complete, {@code false} if still waiting for buttons
     */
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