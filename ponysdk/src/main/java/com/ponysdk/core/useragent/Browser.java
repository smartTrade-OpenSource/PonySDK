/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.useragent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Browser {

    OPERA(Manufacturer.OPERA, null, 1, "Opera", new String[]{"Opera"}, BrowserType.WEB_BROWSER, RenderingEngine.PRESTO,
            "Opera\\/(([\\d]+)\\.([\\w]+))"), // before MSIE
    OPERA_MINI(Manufacturer.OPERA, Browser.OPERA, 20, "Opera Mini", new String[]{"Opera Mini"}, BrowserType.MOBILE_BROWSER,
            RenderingEngine.PRESTO, null), // Opera for mobile devices

    KONQUEROR(Manufacturer.OTHER, null, 1, "Konqueror", new String[]{"Konqueror"}, BrowserType.WEB_BROWSER, RenderingEngine.KHTML,
            "Konqueror\\/(([0-9]+)\\.?([\\w]+)?(-[\\w]+)?)"),

    /**
     * Outlook email client
     */
    OUTLOOK(Manufacturer.MICROSOFT, null, 100, "Outlook", new String[]{"MSOffice"}, BrowserType.EMAIL_CLIENT, RenderingEngine.WORD,
            "MSOffice (([0-9]+))"), // before IE7

    /**
     * Family of Internet Explorer browsers
     */
    IE(Manufacturer.MICROSOFT, null, 1, "Internet Explorer", new String[]{"MSIE"}, BrowserType.WEB_BROWSER, RenderingEngine.TRIDENT,
            "MSIE (([\\d]+)\\.([\\w]+))"), // before Mozilla
    /**
     * Since version 7 Outlook Express is identifying itself. By detecting Outlook Express we can not identify the
     * Internet Explorer version which is probably used for the rendering. Obviously this product is now called Windows
     * Live Mail Desktop or just Windows Live Mail.
     */
    OUTLOOK_EXPRESS7(Manufacturer.MICROSOFT, Browser.IE, 110, "Windows Live Mail", new String[]{"Outlook-Express/7.0"},
            BrowserType.EMAIL_CLIENT, RenderingEngine.TRIDENT, null), // before IE7, previously known as Outlook Express. First released in 2006, offered with different name later
    /**
     * Since 2007 the mobile edition of Internet Explorer identifies itself as IEMobile in the user-agent. If previous
     * versions have to be detected, use the operating system information as well.
     */
    IEMOBILE9(Manufacturer.MICROSOFT, Browser.IE, 123, "IE Mobile 9", new String[]{"IEMobile/9"}, BrowserType.MOBILE_BROWSER,
            RenderingEngine.TRIDENT, null), // before MSIE strings
    IEMOBILE7(Manufacturer.MICROSOFT, Browser.IE, 121, "IE Mobile 7", new String[]{"IEMobile 7"}, BrowserType.MOBILE_BROWSER,
            RenderingEngine.TRIDENT, null), // before MSIE strings
    IEMOBILE6(Manufacturer.MICROSOFT, Browser.IE, 120, "IE Mobile 6", new String[]{"IEMobile 6"}, BrowserType.MOBILE_BROWSER,
            RenderingEngine.TRIDENT, null), // before MSIE
    IE10(Manufacturer.MICROSOFT, Browser.IE, 92, "Internet Explorer 10", new String[]{"MSIE 10"}, BrowserType.WEB_BROWSER,
            RenderingEngine.TRIDENT, null), // before MSIE
    IE9(Manufacturer.MICROSOFT, Browser.IE, 90, "Internet Explorer 9", new String[]{"MSIE 9"}, BrowserType.WEB_BROWSER,
            RenderingEngine.TRIDENT, null), // before MSIE
    IE8(Manufacturer.MICROSOFT, Browser.IE, 80, "Internet Explorer 8", new String[]{"MSIE 8"}, BrowserType.WEB_BROWSER,
            RenderingEngine.TRIDENT, null), // before MSIE
    IE7(Manufacturer.MICROSOFT, Browser.IE, 70, "Internet Explorer 7", new String[]{"MSIE 7"}, BrowserType.WEB_BROWSER,
            RenderingEngine.TRIDENT, null), // before MSIE
    IE6(Manufacturer.MICROSOFT, Browser.IE, 60, "Internet Explorer 6", new String[]{"MSIE 6"}, BrowserType.WEB_BROWSER,
            RenderingEngine.TRIDENT, null), // before MSIE
    IE5_5(Manufacturer.MICROSOFT, Browser.IE, 55, "Internet Explorer 5.5", new String[]{"MSIE 5.5"}, BrowserType.WEB_BROWSER,
            RenderingEngine.TRIDENT, null), // before MSIE
    IE5(Manufacturer.MICROSOFT, Browser.IE, 50, "Internet Explorer 5", new String[]{"MSIE 5"}, BrowserType.WEB_BROWSER,
            RenderingEngine.TRIDENT, null), // before MSIE

    EDGE(Manufacturer.MICROSOFT, null, 1, "Edge", new String[]{"Edge"}, BrowserType.WEB_BROWSER, RenderingEngine.EDGE_HTML, null),

    /**
     * Google Chrome browser
     */
    CHROME(Manufacturer.GOOGLE, null, 1, "Chrome", new String[]{"Chrome"}, BrowserType.WEB_BROWSER, RenderingEngine.WEBKIT,
            "Chrome\\/(([0-9]+)\\.?([\\w]+)?(\\.[\\w]+)?(\\.[\\w]+)?)"), // before Mozilla

    OMNIWEB(Manufacturer.OTHER, null, 2, "Omniweb", new String[]{"OmniWeb"}, BrowserType.WEB_BROWSER, RenderingEngine.WEBKIT, null), //

    SAFARI(Manufacturer.APPLE, null, 1, "Safari", new String[]{"Safari"}, BrowserType.WEB_BROWSER, RenderingEngine.WEBKIT,
            "Version\\/(([0-9]+)\\.?([\\w]+)?(\\.[\\w]+)?)"), // before AppleWebKit
    CHROME_MOBILE(Manufacturer.GOOGLE, Browser.SAFARI, 100, "Chrome Mobile", new String[]{"CrMo"}, BrowserType.MOBILE_BROWSER,
            RenderingEngine.WEBKIT, "CrMo\\/(([0-9]+)\\.?([\\w]+)?(\\.[\\w]+)?(\\.[\\w]+)?)"),
    MOBILE_SAFARI(Manufacturer.APPLE, Browser.SAFARI, 2, "Mobile Safari", new String[]{"Mobile Safari", "Mobile/"},
            BrowserType.MOBILE_BROWSER, RenderingEngine.WEBKIT, null), // before Safari
    SILK(Manufacturer.AMAZON, Browser.SAFARI, 15, "Silk", new String[]{"Silk/"}, BrowserType.WEB_BROWSER, RenderingEngine.WEBKIT,
            "Silk\\/(([0-9]+)\\.?([\\w]+)?(\\.[\\w]+)?(\\-[\\w]+)?)"), // http://en.wikipedia.org/wiki/Amazon_Silk

    DOLFIN2(Manufacturer.SAMSUNG, null, 1, "Samsung Dolphin 2", new String[]{"Dolfin/2"}, BrowserType.MOBILE_BROWSER,
            RenderingEngine.WEBKIT, null), // webkit based browser for the bada os

    APPLE_MAIL(Manufacturer.APPLE, null, 50, "Apple Mail", new String[]{"AppleWebKit"}, BrowserType.EMAIL_CLIENT,
            RenderingEngine.WEBKIT, null), // Microsoft Entrourage/Outlook 2010 also only identifies itself as AppleWebKit
    LOTUS_NOTES(Manufacturer.OTHER, null, 3, "Lotus Notes", new String[]{"Lotus-Notes"}, BrowserType.EMAIL_CLIENT,
            RenderingEngine.OTHER, "Lotus-Notes\\/(([\\d]+)\\.([\\w]+))"), // before Mozilla

    /**
     * Thunderbird email client, based on the same Gecko engine Firefox is using.
     */
    THUNDERBIRD(Manufacturer.MOZILLA, null, 110, "Thunderbird", new String[]{"Thunderbird"}, BrowserType.EMAIL_CLIENT,
            RenderingEngine.GECKO, "Thunderbird\\/(([0-9]+)\\.?([\\w]+)?(\\.[\\w]+)?(\\.[\\w]+)?)"), // using Gecko Engine

    CAMINO(Manufacturer.OTHER, null, 5, "Camino", new String[]{"Camino"}, BrowserType.WEB_BROWSER, RenderingEngine.GECKO,
            "Camino\\/(([0-9]+)\\.?([\\w]+)?(\\.[\\w]+)?)"), // using Gecko Engine
    FLOCK(Manufacturer.OTHER, null, 4, "Flock", new String[]{"Flock"}, BrowserType.WEB_BROWSER, RenderingEngine.GECKO,
            "Flock\\/(([0-9]+)\\.?([\\w]+)?(\\.[\\w]+)?)"),

    FIREFOX(Manufacturer.MOZILLA, null, 10, "Firefox", new String[]{"Firefox"}, BrowserType.WEB_BROWSER, RenderingEngine.GECKO,
            "Firefox\\/(([0-9]+)\\.?([\\w]+)?(\\.[\\w]+)?(\\.[\\w]+)?)"), // using Gecko Engine
    FIREFOX3MOBILE(Manufacturer.MOZILLA, Browser.FIREFOX, 31, "Firefox 3 Mobile", new String[]{"Firefox/3.5 Maemo"},
            BrowserType.MOBILE_BROWSER, RenderingEngine.GECKO, null), // using Gecko Engine

    SEAMONKEY(Manufacturer.OTHER, null, 15, "SeaMonkey", new String[]{"SeaMonkey"}, BrowserType.WEB_BROWSER, RenderingEngine.GECKO,
            "SeaMonkey\\/(([0-9]+)\\.?([\\w]+)?(\\.[\\w]+)?)"), // using Gecko Engine

    BOT(Manufacturer.OTHER, null, 12, "Robot/Spider",
            new String[]{"Googlebot", "bot", "spider", "crawler", "Feedfetcher", "Slurp", "Twiceler", "Nutch", "BecomeBot"},
            BrowserType.ROBOT, RenderingEngine.OTHER, null),

    MOZILLA(Manufacturer.MOZILLA, null, 1, "Mozilla", new String[]{"Mozilla", "Moozilla"}, BrowserType.WEB_BROWSER,
            RenderingEngine.OTHER, null), // rest of the mozilla browsers

    CFNETWORK(Manufacturer.OTHER, null, 6, "CFNetwork", new String[]{"CFNetwork"}, BrowserType.UNKNOWN, RenderingEngine.OTHER,
            null), // Mac OS X cocoa library

    EUDORA(Manufacturer.OTHER, null, 7, "Eudora", new String[]{"Eudora", "EUDORA"}, BrowserType.EMAIL_CLIENT, RenderingEngine.OTHER,
            null), // email client by Qualcomm

    POCOMAIL(Manufacturer.OTHER, null, 8, "PocoMail", new String[]{"PocoMail"}, BrowserType.EMAIL_CLIENT, RenderingEngine.OTHER,
            null),

    THEBAT(Manufacturer.OTHER, null, 9, "The Bat!", new String[]{"The Bat"}, BrowserType.EMAIL_CLIENT, RenderingEngine.OTHER, null), // Email Client

    NETFRONT(Manufacturer.OTHER, null, 10, "NetFront", new String[]{"NetFront"}, BrowserType.MOBILE_BROWSER, RenderingEngine.OTHER,
            null), // mobile device browser

    EVOLUTION(Manufacturer.OTHER, null, 11, "Evolution", new String[]{"CamelHttpStream"}, BrowserType.EMAIL_CLIENT,
            RenderingEngine.OTHER, null), // http://www.go-evolution.org/Camel.Stream

    LYNX(Manufacturer.OTHER, null, 13, "Lynx", new String[]{"Lynx"}, BrowserType.TEXT_BROWSER, RenderingEngine.OTHER,
            "Lynx\\/(([0-9]+)\\.([\\d]+)\\.?([\\w-+]+)?\\.?([\\w-+]+)?)"),

    DOWNLOAD(Manufacturer.OTHER, null, 16, "Downloading Tool", new String[]{"cURL", "wget"}, BrowserType.TEXT_BROWSER,
            RenderingEngine.OTHER, null),

    UNKNOWN(Manufacturer.OTHER, null, 14, "Unknown", new String[0], BrowserType.UNKNOWN, RenderingEngine.OTHER, null);

    private final short id;
    private final String name;
    private final String[] aliases;
    private final BrowserType browserType;
    private final Manufacturer manufacturer;
    private final RenderingEngine renderingEngine;
    private final Browser parent;
    private List<Browser> children;
    private Pattern versionRegEx;

    Browser(final Manufacturer manufacturer, final Browser parent, final int versionId, final String name, final String[] aliases,
            final BrowserType browserType, final RenderingEngine renderingEngine, final String versionRegexString) {
        this.id = (short) ((manufacturer.getId() << 8) + (byte) versionId);
        this.name = name;
        this.parent = parent;
        this.children = new ArrayList<>();
        if (this.parent != null) this.parent.children.add(this);
        this.aliases = aliases;
        this.browserType = browserType;
        this.manufacturer = manufacturer;
        this.renderingEngine = renderingEngine;
        if (versionRegexString != null) this.versionRegEx = Pattern.compile(versionRegexString);
    }

    /**
     * Iterates over all Browsers to compare the browser signature with the user agent string.
     * If no match can be found Browser.UNKNOWN will be returned.
     */
    public static Browser parseUserAgentString(final String agentString) {
        for (final Browser browser : Browser.values()) {
            // only check top level objects
            if (browser.parent == null) {
                final Browser match = browser.checkUserAgent(agentString);
                if (match != null && !Browser.UNKNOWN.equals(match))
                    return match; // either current operatingSystem or a child object
            }
        }
        return Browser.UNKNOWN;
    }

    /**
     * Returns the enum constant of this type with the specified id.
     * Throws IllegalArgumentException if the value does not exist.
     */
    public static Browser valueOf(final short id) {
        for (final Browser browser : Browser.values()) {
            if (browser.getId() == id) return browser;
        }

        // same behavior as standard valueOf(string) method
        throw new IllegalArgumentException("No enum const for id " + id);
    }

    public short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    private Pattern getVersionRegEx() {
        if (this.versionRegEx == null) {
            final Browser group = this.getGroup();
            return group != this ? group.getVersionRegEx() : null;
        }
        return this.versionRegEx;
    }

    /**
     * Detects the detailed version information of the browser.
     * Depends on the userAgent to be available.
     * Returns null if it can not detect the version information.
     */
    public Version getVersion(final String userAgentString) {
        final Pattern pattern = this.getVersionRegEx();
        if (userAgentString != null && pattern != null) {
            final Matcher matcher = pattern.matcher(userAgentString);
            if (matcher.find()) {
                final String fullVersionString = matcher.group(1);
                final String majorVersion = matcher.group(2);
                String minorVersion = "0";
                if (matcher.groupCount() > 2)
                    minorVersion = matcher.group(3); // usually but not always there is a minor version
                return new Version(fullVersionString, majorVersion, minorVersion);
            }
        }
        return null;
    }

    public BrowserType getBrowserType() {
        return browserType;
    }

    public Manufacturer getManufacturer() {
        return manufacturer;
    }

    public RenderingEngine getRenderingEngine() {
        return renderingEngine;
    }

    /**
     * @return top level browser family
     */
    public Browser getGroup() {
        if (this.parent != null) return parent.getGroup();
        else return this;
    }

    /**
     * Checks if the given user-agent string matches to the browser. Only checks for one specific browser.
     */
    private boolean isInUserAgentString(final String agentString) {
        for (final String alias : aliases) {
            if (agentString.toLowerCase().contains(alias.toLowerCase())) return true;
        }
        return false;
    }

    private Browser checkUserAgent(final String agentString) {
        if (agentString != null && isInUserAgentString(agentString)) {
            if (!children.isEmpty()) {
                for (final Browser childBrowser : children) {
                    final Browser match = childBrowser.checkUserAgent(agentString);
                    if (match != null && !Browser.UNKNOWN.equals(match)) return match;
                }
            }

            return this;
        }

        return Browser.UNKNOWN;
    }

}
