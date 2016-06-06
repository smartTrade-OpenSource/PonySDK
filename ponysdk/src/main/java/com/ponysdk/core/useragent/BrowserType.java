
package com.ponysdk.core.useragent;

public enum BrowserType {

    /**
     * Standard web-browser
     */
    WEB_BROWSER("Browser"),
    /**
     * Special web-browser for mobile devices
     */
    MOBILE_BROWSER("Browser (mobile)"),
    /**
     * Text only browser like the good old Lynx
     */
    TEXT_BROWSER("Browser (text only)"),
    /**
     * Email client like Thunderbird
     */
    EMAIL_CLIENT("Email Client"),
    /**
     * Search robot, spider, crawler,...
     */
    ROBOT("Robot"),
    /**
     * Downloading tools
     */
    TOOL("Downloading tool"), UNKNOWN("unknown");

    private String name;

    BrowserType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
