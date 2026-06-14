package com.ponysdk.sample.client.page.addon.flexlayout;

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

    public String getKey() { return key; }
}
