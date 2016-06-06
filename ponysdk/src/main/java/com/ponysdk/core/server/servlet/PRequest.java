
package com.ponysdk.core.server.servlet;

import java.io.Reader;
import java.io.StringReader;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;

public class PRequest {

    private final ServletUpgradeRequest request;

    private StringReader reader;

    public PRequest(final ServletUpgradeRequest request) {
        this.request = request;
    }

    public Reader getReader() {
        return reader;
    }

    public String getHeader(final String header) {
        return request.getHeader(header);
    }

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
