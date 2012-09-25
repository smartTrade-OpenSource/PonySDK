
package com.ponysdk.core.servlet;

import com.ponysdk.core.useragent.UserAgent;

public class HttpSession implements Session {

    private final javax.servlet.http.HttpSession httpSession;

    private UserAgent userAgent;

    public HttpSession(final javax.servlet.http.HttpSession httpSession) {
        this.httpSession = httpSession;
    }

    @Override
    public String getId() {
        return httpSession.getId();
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        httpSession.setAttribute(name, value);
    }

    @Override
    public Object getAttribute(final String name) {
        return httpSession.getAttribute(name);
    }

    @Override
    public void invalidate() {
        httpSession.invalidate();
    }

    @Override
    public void setUserAgent(final String attribute) {
        if (attribute != null) this.userAgent = UserAgent.parseUserAgentString(attribute);
        else this.userAgent = null;
    }

    @Override
    public UserAgent getUserAgent() {
        return userAgent;
    }

    @Override
    public SessionType getSessionType() {
        return SessionType.HTTP;
    }

    @Override
    public boolean isValid() {
        return true;
    }

}
