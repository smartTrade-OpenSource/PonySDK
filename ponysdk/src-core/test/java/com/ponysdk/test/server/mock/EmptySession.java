
package com.ponysdk.test.server.mock;

import com.ponysdk.core.servlet.Session;
import com.ponysdk.core.servlet.SessionType;
import com.ponysdk.core.useragent.Browser;
import com.ponysdk.core.useragent.OperatingSystem;
import com.ponysdk.core.useragent.UserAgent;

public class EmptySession implements Session {

    UserAgent userAgent = new UserAgent(OperatingSystem.WINDOWS_7, Browser.CHROME);

    @Override
    public void setUserAgent(final String attribute) {}

    @Override
    public void setAttribute(final String name, final Object value) {}

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void invalidate() {}

    @Override
    public UserAgent getUserAgent() {
        return userAgent;
    }

    @Override
    public SessionType getSessionType() {
        return SessionType.WEBSOCKET;
    }

    @Override
    public String getId() {
        return "SESSION01";
    }

    @Override
    public Object getAttribute(final String name) {
        return null;
    }

}
