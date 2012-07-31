
package com.ponysdk.core.servlet;

import com.ponysdk.core.useragent.UserAgent;

public interface Session {

    public String getId();

    public void setAttribute(String name, Object value);

    public Object getAttribute(String name);

    public void invalidate();

    public UserAgent getUserAgent();

    public SessionType getSessionType();

    public boolean isValid();
}
