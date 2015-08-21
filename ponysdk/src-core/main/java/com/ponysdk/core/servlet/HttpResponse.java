
package com.ponysdk.core.servlet;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponse implements Response {

    private static Logger log = LoggerFactory.getLogger(HttpResponse.class);

    private final HttpServletResponse response;

    public HttpResponse(final HttpServletResponse response) {
        this.response = response;
    }

    @Override
    public void write(final String s) throws IOException {
        if (log.isDebugEnabled()) log.debug("Sending: " + s);
        response.getWriter().write(s);
    }

    @Override
    public void flush() {
        try {
            response.getWriter().flush();
        } catch (final Throwable t) {
            log.error("Cannot flush to HTTP response", t);
        }
    }

    @Override
    public Writer getWriter() throws IOException {
        return response.getWriter();
    }

}
