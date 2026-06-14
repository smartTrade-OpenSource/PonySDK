package com.ponysdk.sample.client.page.addon.flexlayout;

/**
 * Enum of available FlexLayout visual themes, each mapping to a CSS class.
 *
 * <pre>{@code
 * String cls = FlexTheme.NORD.getCssClass(); // "fl-theme-nord"
 * }</pre>
 */
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

    /** Returns the CSS class name to apply to the layout container. */
    public String getCssClass() { return cssClass; }
}
