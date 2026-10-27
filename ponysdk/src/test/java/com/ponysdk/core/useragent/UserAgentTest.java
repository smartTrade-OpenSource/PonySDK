/*
 * Copyright (c) 2018 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class UserAgentTest {

    @Test
    public void everyOperatingSystemAndBrowserHasItsOwnId() {
        for (final OperatingSystem os : OperatingSystem.values()) assertSame(os, OperatingSystem.valueOf(os.getId()), os.name());
        for (final Browser browser : Browser.values()) assertSame(browser, Browser.valueOf(browser.getId()), browser.name());
    }

    @Test
    public void testAndroidChromeUserAgent() {
        final String userAgentString = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Mobile Safari/537.36";
        final UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
        assertEquals(Browser.CHROME, userAgent.getBrowser());
        assertEquals(BrowserType.WEB_BROWSER, userAgent.getBrowser().getBrowserType());
        assertEquals(Manufacturer.GOOGLE, userAgent.getBrowser().getManufacturer());
        assertEquals(OperatingSystem.ANDROID, userAgent.getOperatingSystem());
        assertEquals(DeviceType.MOBILE, userAgent.getOperatingSystem().getDeviceType());
    }

    @Test
    public void testWindows7ChromeUserAgent() {
        final String userAgentString = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36";
        final UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
        assertEquals(Browser.CHROME, userAgent.getBrowser());
        assertEquals(BrowserType.WEB_BROWSER, userAgent.getBrowser().getBrowserType());
        assertEquals(Manufacturer.GOOGLE, userAgent.getBrowser().getManufacturer());
        assertEquals(OperatingSystem.WINDOWS_7, userAgent.getOperatingSystem());
        assertEquals(DeviceType.COMPUTER, userAgent.getOperatingSystem().getDeviceType());
    }

    @Test
    public void testWindowsXPIEUserAgent() {
        final UserAgent userAgent = UserAgent.parseUserAgentString("Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 5.1; Trident/6.0)");
        assertEquals(Browser.IE10, userAgent.getBrowser());
        assertEquals(BrowserType.WEB_BROWSER, userAgent.getBrowser().getBrowserType());
        assertEquals(Manufacturer.MICROSOFT, userAgent.getBrowser().getManufacturer());
        assertEquals(OperatingSystem.WINDOWS_XP, userAgent.getOperatingSystem());
        assertEquals(DeviceType.COMPUTER, userAgent.getOperatingSystem().getDeviceType());
    }

    @Test
    public void testMacFirefoxUserAgent() {
        final UserAgent userAgent = UserAgent
            .parseUserAgentString("Mozilla/5.0 (Macintosh; Intel Mac OS X x.y; rv:10.0) Gecko/20100101 Firefox/10.0");
        assertEquals(Browser.FIREFOX, userAgent.getBrowser());
        assertEquals(BrowserType.WEB_BROWSER, userAgent.getBrowser().getBrowserType());
        assertEquals(Manufacturer.MOZILLA, userAgent.getBrowser().getManufacturer());
        assertEquals(OperatingSystem.MAC_OS_X, userAgent.getOperatingSystem());
        assertEquals(DeviceType.COMPUTER, userAgent.getOperatingSystem().getDeviceType());
    }

    @Test
    public void testWindows10EdgeUserAgent() {
        final String userAgentString = "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.10136";
        final UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
        assertEquals(Browser.EDGE, userAgent.getBrowser());
        assertEquals(BrowserType.WEB_BROWSER, userAgent.getBrowser().getBrowserType());
        assertEquals(Manufacturer.MICROSOFT, userAgent.getBrowser().getManufacturer());
        assertEquals(OperatingSystem.WINDOWS_10, userAgent.getOperatingSystem());
        assertEquals(DeviceType.COMPUTER, userAgent.getOperatingSystem().getDeviceType());
    }

    @Test
    public void browserFamiliesAndTheirVersions() {
        assertBrowser("Opera/9.80 (Windows NT 6.1) Presto/2.12.388 Version/12.16", Browser.OPERA, "9.80", "9", "80");
        assertBrowser("Opera/9.80 (J2ME/MIDP; Opera Mini/9.80) Presto/2.5.25", Browser.OPERA_MINI, "9.80", "9", "80");
        assertBrowser("Mozilla/5.0 (compatible; Konqueror/4.5; Linux) KHTML/4.5.4 (like Gecko)", Browser.KONQUEROR, "4.5", "4", "5");
        assertBrowser("Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; MSOffice 12)", Browser.OUTLOOK, "12", "12", null);
        assertBrowser("Mozilla/4.0 (compatible; MSIE 9.0; Windows Phone OS 7.5; IEMobile/9.0)", Browser.IEMOBILE9, "9.0", "9", "0");
        assertBrowser("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)", Browser.IE6, "6.0", "6", "0");
        assertBrowser("Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0)", Browser.IE8, "8.0", "8", "0");
        assertBrowser("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.75.14 (KHTML, like Gecko) Version/7.0.3 "
                + "Safari/7046A194A", Browser.SAFARI, "7.0.3", "7", "0");
        assertBrowser("Mozilla/5.0 (iPhone; CPU iPhone OS 5_0 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 "
                + "Mobile/9A334 Safari/7534.48.3", Browser.MOBILE_SAFARI, "5.1", "5", "1");
        assertBrowser("Mozilla/5.0 (Linux; U; Android 4.0.3) AppleWebKit/535.19 (KHTML, like Gecko) CrMo/18.0.1025.133 Mobile "
                + "Safari/535.19", Browser.CHROME_MOBILE, "18.0.1025.133", "18", "0");
        assertBrowser("Mozilla/5.0 (X11; Linux x86_64; rv:24.0) Gecko/20100101 Thunderbird/24.6.0", Browser.THUNDERBIRD, "24.6.0", "24",
            "6");
        assertBrowser("Mozilla/5.0 (Macintosh; U; PPC Mac OS X 10.4; en; rv:1.9.0.19) Gecko/2011091218 Camino/2.0.9 (like Firefox/3.0.19)",
            Browser.CAMINO, "2.0.9", "2", "0");
        assertBrowser("Mozilla/5.0 (X11; Linux x86_64; rv:31.0) Gecko/20100101 SeaMonkey/2.28", Browser.SEAMONKEY, "2.28", "2", "28");
        assertBrowser("Lynx/2.8.8dev.3 libwww-FM/2.14 SSL-MM/1.4.1", Browser.LYNX, "2.8.8dev.3", "2", "8");
        assertBrowser("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)", Browser.BOT, null, null, null);
        assertBrowser("curl/7.64.1", Browser.DOWNLOAD, null, null, null);
        assertBrowser("something else entirely", Browser.UNKNOWN, null, null, null);
    }

    @Test
    public void aMissingMinorVersionIsZero() {
        final Version version = Browser.CHROME.getVersion("Mozilla/5.0 Chrome/120 Safari/537.36");
        assertEquals(new Version("120", "120", "0"), version);
    }

    private static void assertBrowser(final String userAgentString, final Browser browser, final String version, final String major,
                                      final String minor) {
        final UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
        assertEquals(browser, userAgent.getBrowser(), userAgentString);
        final Version detected = userAgent.getBrowserVersion();
        if (version == null) {
            assertNull(detected, userAgentString);
        } else {
            assertEquals(version, detected.getVersion(), userAgentString);
            assertEquals(major, detected.getMajorVersion(), userAgentString);
            if (minor != null) assertEquals(minor, detected.getMinorVersion(), userAgentString);
        }
    }

    @Test
    public void operatingSystemsAndDevices() {
        assertOs("Mozilla/5.0 (iPad; CPU OS 5_1 like Mac OS X) AppleWebKit/534.46", OperatingSystem.MAC_OS_X_IPAD, DeviceType.TABLET);
        assertOs("Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_3 like Mac OS X)", OperatingSystem.iOS4_IPHONE, DeviceType.MOBILE);
        assertOs("Mozilla/5.0 (Linux; U; Android 2.3.4; GT-P1000)", OperatingSystem.ANDROID2_TABLET, DeviceType.TABLET);
        assertOs("Mozilla/5.0 (X11; Linux x86_64)", OperatingSystem.LINUX, DeviceType.COMPUTER);
        assertOs("Mozilla/5.0 (X11; CrOS x86_64 14541.0.0)", OperatingSystem.CHROME_OS, DeviceType.COMPUTER);
        assertOs("Mozilla/5.0 (PLAYSTATION 3; 3.55)", OperatingSystem.PLAYSTATION_3, DeviceType.GAME_CONSOLE);
        assertOs("Mozilla/5.0 (webOS/1.4.0; U; en-US)", OperatingSystem.WEBOS, DeviceType.MOBILE);
        assertOs("Mozilla/4.0 (compatible; MSIE 6.0; Windows 98; PalmSource/hspr-H102)", OperatingSystem.PALM, DeviceType.MOBILE);
        assertOs("BlackBerry9700/5.0.0.351 Profile/MIDP-2.1", OperatingSystem.BLACKBERRY, DeviceType.MOBILE);
        assertOs("Roku/DVP-4.1 (024.01E01250A)", OperatingSystem.ROKU, DeviceType.DMR);
        assertOs("nothing known", OperatingSystem.UNKNOWN, DeviceType.UNKNOWN);

        assertTrue(OperatingSystem.ANDROID4.isMobileDevice());
        assertFalse(OperatingSystem.WINDOWS_7.isMobileDevice());
        assertEquals(OperatingSystem.ANDROID, OperatingSystem.ANDROID4_TABLET.getGroup());
        assertEquals("Windows 7", OperatingSystem.WINDOWS_7.getName());
        assertEquals(Manufacturer.APPLE, OperatingSystem.IOS.getManufacturer());
        assertEquals("Apple Inc.", Manufacturer.APPLE.getName());
        assertEquals("Mobile", DeviceType.MOBILE.getName());
        assertEquals("Robot", BrowserType.ROBOT.getName());
        assertEquals(RenderingEngine.GECKO, Browser.FIREFOX.getRenderingEngine());
        assertEquals("Firefox", Browser.FIREFOX.getName());
        assertEquals(Browser.IE, Browser.IE9.getGroup());
    }

    private static void assertOs(final String userAgentString, final OperatingSystem os, final DeviceType deviceType) {
        final OperatingSystem parsed = UserAgent.parseUserAgentString(userAgentString).getOperatingSystem();
        assertEquals(os, parsed, userAgentString);
        assertEquals(deviceType, parsed.getDeviceType(), userAgentString);
    }

    @Test
    public void aNullUserAgentIsUnknown() {
        final UserAgent userAgent = new UserAgent((String) null);
        assertEquals(Browser.UNKNOWN, userAgent.getBrowser());
        assertEquals(OperatingSystem.UNKNOWN, userAgent.getOperatingSystem());
        assertNull(userAgent.getUserAgentString());
        assertNull(userAgent.getBrowserVersion());
    }

    @Test
    public void idAndNameRoundTrip() {
        final UserAgent userAgent = UserAgent.parseUserAgentString(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X x.y; rv:10.0) Gecko/20100101 Firefox/10.0");
        assertEquals("MAC_OS_X-FIREFOX", userAgent.toString());
        assertEquals(new UserAgent(OperatingSystem.MAC_OS_X, Browser.FIREFOX), UserAgent.valueOf(userAgent.getId()));
        assertEquals(new UserAgent(OperatingSystem.MAC_OS_X, Browser.FIREFOX), UserAgent.valueOf(userAgent.toString()));
        assertEquals((OperatingSystem.MAC_OS_X.getId() << 16) + Browser.FIREFOX.getId(), userAgent.getId());
        assertSame(Browser.FIREFOX, Browser.valueOf(Browser.FIREFOX.getId()));
        assertSame(OperatingSystem.MAC_OS_X, OperatingSystem.valueOf(OperatingSystem.MAC_OS_X.getId()));

        assertThrows(NullPointerException.class, () -> UserAgent.valueOf((String) null));
        assertThrows(IllegalArgumentException.class, () -> UserAgent.valueOf("MAC_OS_X"));
        assertThrows(IllegalArgumentException.class, () -> Browser.valueOf((short) -1));
        assertThrows(IllegalArgumentException.class, () -> OperatingSystem.valueOf((short) -1));
    }

    @Test
    public void equalityIsOnSystemAndBrowser() {
        final UserAgent a = new UserAgent(OperatingSystem.LINUX, Browser.FIREFOX);
        final UserAgent same = UserAgent.parseUserAgentString("Mozilla/5.0 (X11; Linux x86_64; rv:99.0) Gecko/20100101 Firefox/99.0");
        assertEquals(a, same);
        assertEquals(a.hashCode(), same.hashCode());
        assertEquals(a, a);
        assertNotEquals(a, null);
        assertNotEquals(a, "LINUX-FIREFOX");
        assertNotEquals(a, new UserAgent(OperatingSystem.LINUX, Browser.CHROME));
        assertNotEquals(a, new UserAgent(OperatingSystem.MAC_OS_X, Browser.FIREFOX));
    }

    @Test
    public void versionValueSemantics() {
        final Version version = new Version("1.2", "1", "2");
        assertEquals("1.2", version.getVersion());
        assertEquals(version, new Version("1.2", "1", "2"));
        assertEquals(version.hashCode(), new Version("1.2", "1", "2").hashCode());
        assertEquals(version, version);
        assertNotEquals(version, null);
        assertNotEquals(version, "1.2");
        assertNotEquals(version, new Version("1.3", "1", "2"));
        assertNotEquals(version, new Version("1.2", "2", "2"));
        assertNotEquals(version, new Version("1.2", "1", "3"));
        assertEquals("Version{version='1.2', majorVersion='1', minorVersion='2'}", version.toString());
    }

}
