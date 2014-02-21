
package com.ponysdk.core.servlet;

import java.io.IOException;
import java.io.Reader;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequest implements Request {

    private static Logger log = LoggerFactory.getLogger(HttpRequest.class);

    private final HttpServletRequest request;
    private final Session session;

    public HttpRequest(final Session session, final HttpServletRequest request) {
        this.request = request;
        this.session = session;
        if (log.isDebugEnabled()) outputIncomingRequest();
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public Reader getReader() throws IOException {
        return request.getReader();
    }

    private void outputIncomingRequest() {
        try {
            getReader().mark(1024 * 10000);
            final String input = readFully(getReader());
            getReader().reset();
            if (log.isDebugEnabled()) log.debug("Received: " + input);
        } catch (final IOException e) {
            log.error("Cannot read received data", e);
        }
    }

    private static String readFully(final Reader reader) throws IOException {
        final StringBuffer buf = new StringBuffer();
        final char[] arr = new char[8 * 1024];
        int numChars;

        while ((numChars = reader.read(arr, 0, arr.length)) > 0) {
            buf.append(arr, 0, numChars);
        }
        return buf.toString();
    }

    @Override
    public String getHeader(final String header) {
        return request.getHeader(header);
    }

    @Override
    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }

}
