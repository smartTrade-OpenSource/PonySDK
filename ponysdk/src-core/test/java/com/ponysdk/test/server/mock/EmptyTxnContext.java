
package com.ponysdk.test.server.mock;

import javax.json.JsonReader;

import com.ponysdk.core.Application;
import com.ponysdk.core.ParserImpl;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.servlet.Request;
import com.ponysdk.core.servlet.Response;
import com.ponysdk.core.stm.TxnContext;
import com.ponysdk.core.useragent.UserAgent;

public class EmptyTxnContext implements TxnContext {

    @Override
    public void flush() {}

    @Override
    public ParserImpl getParser() {
        return null;
    }

    @Override
    public JsonReader getReader() {
        return null;
    }

    @Override
    public UserAgent getUserAgent() {
        return null;
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public Application getApplication() {
        return null;
    }

    @Override
    public void setApplication(final Application application) {}

    @Override
    public void setAttribute(final String name, final Object value) {

    }

    @Override
    public Object getAttribute(final String name) {
        return null;
    }

    @Override
    public void setRequest(final Request request) {}

    @Override
    public void setResponse(final Response response) {}

    @Override
    public long getSeqNum() {
        return 0;
    }

    @Override
    public String getHistoryToken() {
        return null;
    }

    @Override
    public Request getRequest() {
        return null;
    }

    @Override
    public UIContext getUIContext() {
        return null;
    }

    @Override
    public void setUIContext(final UIContext uiContext) {}

    @Override
    public void sendHeartBeat() {
        // TODO Auto-generated method stub

    }

}
