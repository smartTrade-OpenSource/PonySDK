
package com.ponysdk.core.servlet;

import java.io.IOException;
import java.io.Reader;

import javax.servlet.http.HttpServletRequest;

public class SocketRequest implements Request {

    private final HttpServletRequest request;
    private final Session session;

    public SocketRequest(final Session session, final HttpServletRequest request) {
        this.request = request;
        this.session = session;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public Reader getReader() throws IOException {
        return request.getReader();
    }

    @Override
    public String getHeader(final String header) {
        return request.getHeader(header);
    }

}
