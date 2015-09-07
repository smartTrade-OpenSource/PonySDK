
package com.ponysdk.test.server.mock;

import com.ponysdk.core.servlet.Session;
import com.ponysdk.core.stm.TxnContextHttp;
import com.ponysdk.core.stm.TxnSocketContext;

public class EmptySession implements Session {

    @Override
    public void setAttribute(final String name, final Object value) {}

    @Override
    public void invalidate() {}

    @Override
    public String getId() {
        return "SESSION01";
    }

    @Override
    public Object getAttribute(final String name) {
        return null;
    }

    @Override
    public void setHttpContext(final TxnContextHttp context) {

    }

    @Override
    public TxnContextHttp getHttpContext() {
        return null;
    }

    @Override
    public void setSocketContext(final TxnSocketContext context) {}

    @Override
    public TxnSocketContext getSocketContext() {
        return null;
    }

}
