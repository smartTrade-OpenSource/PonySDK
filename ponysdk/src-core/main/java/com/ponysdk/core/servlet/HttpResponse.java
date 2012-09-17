
package com.ponysdk.core.servlet;

import java.io.IOException;

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
        this.response.getWriter().write(s);
    }

    @Override
    public void flush() throws IOException {
        this.response.getWriter().flush();
    }

}
