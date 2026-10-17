package com.ponysdk.core.ui.flexlayout;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

/**
 * Builder for a border (sidebar) panel in a FlexLayout.
 *
 * <pre>{@code
 * FlexBorder border = FlexBorder.left().size(250).tabStyle(TabStyle.ICON);
 * }</pre>
 */
public final class FlexBorder {
    private final String side;
    private int size = 220;
    private TabStyle tabStyle = TabStyle.AUTO;

    private FlexBorder(final String side) { this.side = side; }

    /** Creates a left-top border (alias for {@link #leftTop()}). */
    public static FlexBorder left() { return new FlexBorder("left-top"); }
    /** Creates a left-top border. */
    public static FlexBorder leftTop() { return new FlexBorder("left-top"); }
    /** Creates a left-bottom border. */
    public static FlexBorder leftBottom() { return new FlexBorder("left-bottom"); }
    /** Creates a right-top border (alias for {@link #rightTop()}). */
    public static FlexBorder right() { return new FlexBorder("right-top"); }
    /** Creates a right-top border. */
    public static FlexBorder rightTop() { return new FlexBorder("right-top"); }
    /** Creates a right-bottom border. */
    public static FlexBorder rightBottom() { return new FlexBorder("right-bottom"); }
    /** Creates a bottom border. */
    public static FlexBorder bottom() { return new FlexBorder("bottom"); }

    /** Sets the initial panel width/height in pixels. */
    public FlexBorder size(final int size) { this.size = size; return this; }
    /** Sets the tab display style for this border's strip. */
    public FlexBorder tabStyle(final TabStyle style) { this.tabStyle = style; return this; }

    /** Returns the side identifier string. */
    public String getSide() { return side; }
    /** Returns the tab display style. */
    public TabStyle getTabStyle() { return tabStyle; }

    /** Serializes this border to a JSON string for the FlexLayout model. */

    String toJson() {
        final JsonObjectBuilder b = Json.createObjectBuilder()
            .add("type", "border")
            .add("location", side)
            .add("size", size);
        if (tabStyle != TabStyle.AUTO) b.add("tabStyle", tabStyle.getValue());
        return b.build().toString();
    }
}
