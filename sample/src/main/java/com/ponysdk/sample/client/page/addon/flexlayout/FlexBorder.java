package com.ponysdk.sample.client.page.addon.flexlayout;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

public class FlexBorder {
    private final String side;
    private int size = 220;
    private TabStyle tabStyle = TabStyle.AUTO;

    private FlexBorder(final String side) { this.side = side; }

    public static FlexBorder left() { return new FlexBorder("left-top"); }
    public static FlexBorder leftTop() { return new FlexBorder("left-top"); }
    public static FlexBorder leftBottom() { return new FlexBorder("left-bottom"); }
    public static FlexBorder right() { return new FlexBorder("right-top"); }
    public static FlexBorder rightTop() { return new FlexBorder("right-top"); }
    public static FlexBorder rightBottom() { return new FlexBorder("right-bottom"); }
    public static FlexBorder bottom() { return new FlexBorder("bottom"); }

    public FlexBorder size(final int size) { this.size = size; return this; }
    public FlexBorder tabStyle(final TabStyle style) { this.tabStyle = style; return this; }

    public String getSide() { return side; }
    public TabStyle getTabStyle() { return tabStyle; }

    public String toJson() {
        final JsonObjectBuilder b = Json.createObjectBuilder()
            .add("type", "border")
            .add("location", side)
            .add("size", size);
        if (tabStyle != TabStyle.AUTO) b.add("tabStyle", tabStyle.getValue());
        return b.build().toString();
    }
}
