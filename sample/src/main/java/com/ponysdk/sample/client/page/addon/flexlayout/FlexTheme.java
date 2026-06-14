package com.ponysdk.sample.client.page.addon.flexlayout;

public enum FlexTheme {
    DEFAULT(""),
    LIGHT("fl-theme-light"),
    GRAY("fl-theme-gray"),
    NORD("fl-theme-nord"),
    SOLARIZED("fl-theme-solarized"),
    GITHUB("fl-theme-github"),
    MONOKAI("fl-theme-monokai"),
    CORPORATE("fl-theme-corporate"),
    DEEP_ORANGE("fl-theme-deep-orange"),
    ROUNDED("fl-theme-rounded"),
    UNDERLINE("fl-theme-underline");

    private final String cssClass;

    FlexTheme(final String cssClass) { this.cssClass = cssClass; }

    public String getCssClass() { return cssClass; }
}
