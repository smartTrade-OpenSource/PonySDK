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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UserAgentTest {

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
        final UserAgent userAgent = UserAgent.valueOf("WINDOWS_XP-IE10");
        assertEquals(Browser.IE10, userAgent.getBrowser());
        assertEquals(BrowserType.WEB_BROWSER, userAgent.getBrowser().getBrowserType());
        assertEquals(Manufacturer.MICROSOFT, userAgent.getBrowser().getManufacturer());
        assertEquals(OperatingSystem.WINDOWS_XP, userAgent.getOperatingSystem());
        assertEquals(DeviceType.COMPUTER, userAgent.getOperatingSystem().getDeviceType());
    }

    @Test
    public void testMacFirefoxUserAgent() {
        final UserAgent userAgent = UserAgent.valueOf("MAC_OS-FIREFOX");
        assertEquals(Browser.FIREFOX, userAgent.getBrowser());
        assertEquals(BrowserType.WEB_BROWSER, userAgent.getBrowser().getBrowserType());
        assertEquals(Manufacturer.MOZILLA, userAgent.getBrowser().getManufacturer());
        assertEquals(OperatingSystem.MAC_OS, userAgent.getOperatingSystem());
        assertEquals(DeviceType.COMPUTER, userAgent.getOperatingSystem().getDeviceType());
    }

}
