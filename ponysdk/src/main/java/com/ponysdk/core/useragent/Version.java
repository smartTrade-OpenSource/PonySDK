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

import java.util.Objects;

public class Version {

    final String version;
    final String majorVersion;
    final String minorVersion;

    public Version(final String version, final String majorVersion, final String minorVersion) {
        super();
        this.version = version;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public String getVersion() {
        return version;
    }

    public String getMajorVersion() {
        return majorVersion;
    }

    public String getMinorVersion() {
        return minorVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version1 = (Version) o;
        return Objects.equals(version, version1.version) && Objects.equals(majorVersion, version1.majorVersion)
                && Objects.equals(minorVersion, version1.minorVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, majorVersion, minorVersion);
    }

    @Override
    public String toString() {
        return "Version{" + "version='" + version + '\'' + ", majorVersion='" + majorVersion + '\'' + ", minorVersion='" + minorVersion
                + '\'' + '}';
    }
}
