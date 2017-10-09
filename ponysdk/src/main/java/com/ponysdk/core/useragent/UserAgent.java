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

import java.io.Serializable;

public class UserAgent implements Serializable {

    private static final long serialVersionUID = 7025462762784240212L;
    private final int id;
    private OperatingSystem operatingSystem = OperatingSystem.UNKNOWN;
    private Browser browser = Browser.UNKNOWN;
    private String userAgentString;

    public UserAgent(final OperatingSystem operatingSystem, final Browser browser) {
        this.operatingSystem = operatingSystem;
        this.browser = browser;
        this.id = (operatingSystem.getId() << 16) + browser.getId();
    }

    public UserAgent(final String userAgentString) {
        final Browser browser = Browser.parseUserAgentString(userAgentString);

        OperatingSystem operatingSystem = OperatingSystem.UNKNOWN;

        // BOTs don't have an interesting OS for us
        if (browser != Browser.BOT) operatingSystem = OperatingSystem.parseUserAgentString(userAgentString);

        this.operatingSystem = operatingSystem;
        this.browser = browser;
        this.id = (operatingSystem.getId() << 16) + browser.getId();
        this.userAgentString = userAgentString;
    }

    public static UserAgent parseUserAgentString(final String userAgentString) {
        return new UserAgent(userAgentString);
    }

    /**
     * Returns UserAgent based on specified unique id
     */
    public static UserAgent valueOf(final int id) {
        final OperatingSystem operatingSystem = OperatingSystem.valueOf((short) (id >> 16));
        final Browser browser = Browser.valueOf((short) (id & 0x0FFFF));
        return new UserAgent(operatingSystem, browser);
    }

    /**
     * Returns UserAgent based on combined string representation
     */
    public static UserAgent valueOf(final String name) {
        if (name == null) throw new NullPointerException("Name is null");

        final String[] elements = name.split("-");

        if (elements.length == 2) {
            final OperatingSystem operatingSystem = OperatingSystem.valueOf(elements[0]);
            final Browser browser = Browser.valueOf(elements[1]);
            return new UserAgent(operatingSystem, browser);
        }

        throw new IllegalArgumentException("Invalid string for userAgent " + name);
    }

    /**
     * Detects the detailed version information of the browser. Depends on the
     * userAgent to be available. Use it only after using UserAgent(String) or
     * UserAgent.parseUserAgent(String). Returns null if it can not detect the
     * version information.
     *
     * @return Version
     */
    public Version getBrowserVersion() {
        return this.browser.getVersion(this.userAgentString);
    }

    /**
     * @return the system
     */
    public OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }

    /**
     * @return the browser
     */
    public Browser getBrowser() {
        return browser;
    }

    /**
     * Returns an unique integer value of the operating system & browser
     * combination
     *
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Combined string representation of both enums
     */
    @Override
    public String toString() {
        return this.operatingSystem.toString() + "-" + this.browser.toString();
    }

    public String getUserAgentString() {
        return userAgentString;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (browser == null ? 0 : browser.hashCode());
        result = prime * result + id;
        result = prime * result + (operatingSystem == null ? 0 : operatingSystem.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final UserAgent other = (UserAgent) obj;
        if (browser == null) {
            if (other.browser != null) return false;
        } else if (browser != other.browser) return false;
        if (id != other.id) return false;
        if (operatingSystem == null) {
            if (other.operatingSystem != null) return false;
        } else if (operatingSystem != other.operatingSystem) return false;
        return true;
    }

}
