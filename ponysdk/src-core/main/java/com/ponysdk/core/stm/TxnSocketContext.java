
package com.ponysdk.core.stm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.Application;
import com.ponysdk.core.ParserImpl;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.servlet.Request;
import com.ponysdk.core.servlet.Response;
import com.ponysdk.core.socket.WebSocket;
import com.ponysdk.core.useragent.UserAgent;

public class TxnSocketContext implements TxnContext, TxnListener {

    private static final Logger log = LoggerFactory.getLogger(TxnSocketContext.class);

    private WebSocket socket;

    private boolean polling = false;

    private boolean flushNow = false;

    private ParserImpl parser;

    private Application application;

    private final Map<String, Object> parameters = new HashMap<>();

    private Request request;

    private UIContext uiContext;

    public TxnSocketContext() {}

    public void setSocket(final WebSocket socket) {
        this.socket = socket;
        this.parser = new ParserImpl(socket);
    }

    @Override
    public void flush() {
        if (polling) return;
        parser.endOfParsing();
        socket.flush();
        parser.reset();
    }

    public void switchToPollingMode() {
        polling = true;
    }

    public void flushNow() {
        flushNow = true;
        Txn.get().addTnxListener(this);
    }

    @Override
    public void beforeFlush(final TxnContext txnContext) {
        if (!flushNow) return;

        flushNow = false;

        Txn.get().getTxnContext().flush();
    }

    @Override
    public void beforeRollback() {}

    @Override
    public void afterFlush(final TxnContext txnContext) {}

    @Override
    public ParserImpl getParser() {
        return parser;
    }

    @Override
    public void setRequest(final Request request) {
        this.request = request;
    }

    @Override
    public void setResponse(final Response response) {}

    @Override
    public JsonReader getReader() {
        try {
            return Json.createReader(request.getReader());
        } catch (final IOException e) {
            log.error("Cannot build reader from HTTP request", e);
        }

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
        return application;
    }

    @Override
    public void setApplication(final Application application) {
        this.application = application;
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        parameters.put(name, value);
    }

    @Override
    public Object getAttribute(final String name) {
        return parameters.get(name);
    }

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
        return request;
    }

    @Override
    public UIContext getUIContext() {
        return uiContext;
    }

    @Override
    public void setUIContext(final UIContext uiContext) {
        this.uiContext = uiContext;
    }

}
