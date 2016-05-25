
package com.ponysdk.core.servlet;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;

public class SocketRequest implements Request {

    private final ServletUpgradeRequest request;

    private StringReader reader;

    public SocketRequest(final ServletUpgradeRequest request) {
        this.request = request;
    }

    @Override
    public Reader getReader() throws IOException {
        return reader;
    }

    @Override
    public String getHeader(final String header) {
        return request.getHeader(header);
    }

    @Override
    public String getRemoteAddr() {
        return request.getRemoteAddress();
    }

    public void setText(final String text) {
        reader = new StringReader(text);
    }

    public String getSessionId() {
        return request.getSession().getId();
    }

}
