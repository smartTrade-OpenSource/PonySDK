package com.ponysdk.core.ui.flexlayout;

/**
 * Enum controlling how tabs are displayed in a border strip (icon, label, or both).
 *
 * <pre>{@code
 * FlexBorder.left().tabStyle(TabStyle.ICON_LABEL);
 * }</pre>
 */
public enum TabStyle {
    AUTO("auto"), ICON("icon"), LABEL("label"), ICON_LABEL("iconLabel");

    private final String value;

    TabStyle(final String value) { this.value = value; }

    /** Returns the string value used in JSON serialization. */
    public String getValue() { return value; }
}
