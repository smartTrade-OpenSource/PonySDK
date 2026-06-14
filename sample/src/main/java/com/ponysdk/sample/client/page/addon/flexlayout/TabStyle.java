package com.ponysdk.sample.client.page.addon.flexlayout;

public enum TabStyle {
    AUTO("auto"), ICON("icon"), LABEL("label"), ICON_LABEL("iconLabel");

    private final String value;

    TabStyle(final String value) { this.value = value; }

    public String getValue() { return value; }
}
