
package com.ponysdk.core.servlet;

public interface SessionListener {

    void sessionCreated(Session session);

    void sessionDestroyed(Session session);

}
