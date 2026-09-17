package com.ponysdk.core.ui.flexlayout;

import com.ponysdk.core.ui.basic.IsPWidget;

/**
 * Interface that a PWidget must implement to be hosted as a tab in FlexLayout.
 * Provides metadata for the tab header and lifecycle hooks for pop-out/pop-in.
 */
public interface TabContent extends IsPWidget {

    /**
     * Display name shown in the tab header.
     */
    String getTabName();

    /**
     * Unique component identifier (used for factory recreation and persistence).
     */
    String getComponent();

    /**
     * Whether this tab can be closed by the user.
     */
    default boolean isClosable() { return true; }

    /**
     * Whether this tab can be popped out to a floating window.
     */
    default boolean isPopOutable() { return true; }

    /**
     * Called when the tab is popped out to a floating window.
     */
    default void onPopOut() {}

    /**
     * Called when the tab is popped back in from a floating window.
     */
    default void onPopIn() {}

    /**
     * Called when the tab is selected (becomes visible).
     */
    default void onActivated() {}

    /**
     * Called when the tab is deselected (hidden behind another tab).
     */
    default void onDeactivated() {}
}
