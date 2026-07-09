package com.ponysdk.core.ui.flexlayout;

/**
 * Enum of all user-triggered actions in FlexLayout that can be bound to keyboard shortcuts.
 *
 * <pre>{@code
 * String key = FlexAction.TOGGLE_LEFT.getKey(); // "toggleLeft"
 * }</pre>
 */
public enum FlexAction {
    TOGGLE_LEFT("toggleLeft"),
    TOGGLE_RIGHT("toggleRight"),
    TOGGLE_BOTTOM("toggleBottom"),
    CLOSE_ALL("closeAll"),
    UNDO("undo"),
    REDO("redo"),
    CLOSE_ACTIVE_TAB("closeActiveTab"),
    RENAME_TAB("renameTab"),
    COMMAND_PALETTE("commandPalette");

    private final String key;

    FlexAction(final String key) { this.key = key; }

    /** Returns the JSON key identifier for this action. */
    public String getKey() { return key; }
}
