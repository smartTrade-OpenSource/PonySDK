
package com.ponysdk.core.servlet;

import javax.servlet.http.HttpSession;

import com.ponysdk.core.stm.TxnContextHttp;
import com.ponysdk.core.stm.TxnSocketContext;

public class PonySDKSession implements Session {

    private final HttpSession session;

    private TxnContextHttp httpContext;
    private TxnSocketContext socketContext;

    public PonySDKSession(final HttpSession session) {
        this.session = session;
    }

    @Override
    public String getId() {
        return session.getId();
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        session.setAttribute(name, value);
    }

    @Override
    public Object getAttribute(final String name) {
        return session.getAttribute(name);
    }

    @Override
    public void invalidate() {
        session.invalidate();
    }

    @Override
    public void setHttpContext(final TxnContextHttp context) {
        this.httpContext = context;
    }

    @Override
    public TxnContextHttp getHttpContext() {
        return httpContext;
    }

    @Override
    public void setSocketContext(final TxnSocketContext context) {
        this.socketContext = context;
    }

    @Override
    public TxnSocketContext getSocketContext() {
        return socketContext;
    }

}
