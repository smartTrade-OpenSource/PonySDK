
package com.ponysdk.core.servlet;

import javax.servlet.http.HttpServletRequest;

import com.ponysdk.core.useragent.UserAgent;

public class HttpSession implements Session {

    private final javax.servlet.http.HttpSession httpSession;

    private final UserAgent userAgent;

    public HttpSession(final HttpServletRequest httpRequest) {
        this.httpSession = httpRequest.getSession();
        this.userAgent = UserAgent.parseUserAgentString((String) httpSession.getAttribute("User-Agent"));
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
