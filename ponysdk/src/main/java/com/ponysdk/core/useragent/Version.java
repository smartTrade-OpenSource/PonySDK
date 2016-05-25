
package com.ponysdk.core.useragent;

public class Version {

    String version;
    String majorVersion;
    String minorVersion;

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
    public String toString() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((majorVersion == null) ? 0 : majorVersion.hashCode());
        result = prime * result + ((minorVersion == null) ? 0 : minorVersion.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Version other = (Version) obj;
        if (majorVersion == null) {
            if (other.majorVersion != null) return false;
        } else if (!majorVersion.equals(other.majorVersion)) return false;
        if (minorVersion == null) {
            if (other.minorVersion != null) return false;
        } else if (!minorVersion.equals(other.minorVersion)) return false;
        if (version == null) {
            if (other.version != null) return false;
        } else if (!version.equals(other.version)) return false;
        return true;
    }

}
