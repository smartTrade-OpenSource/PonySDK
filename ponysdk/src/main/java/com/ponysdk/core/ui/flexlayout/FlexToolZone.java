package com.ponysdk.core.ui.flexlayout;

/**
 * A tool zone around a FlexLayout: the left, center or right part of a bar above the layout (top) or below it
 * (bottom), where the application puts its own widget, independent of the tabs.
 *
 * <pre>{@code
 * layout.setToolZone(FlexToolZone.TOP_RIGHT, userMenu);
 * }</pre>
 *
 * Left and right zones take their content's width; the center zone takes the rest and centers its content.
 */
public enum FlexToolZone {
    TOP_LEFT("top-left"), TOP_CENTER("top-center"), TOP_RIGHT("top-right"),
    BOTTOM_LEFT("bottom-left"), BOTTOM_CENTER("bottom-center"), BOTTOM_RIGHT("bottom-right");

    private final String value;

    FlexToolZone(final String value) { this.value = value; }

    /** Returns the zone's name on the wire: its bar, then its place, for instance {@code "top-left"}. */
    public String getValue() { return value; }
}
