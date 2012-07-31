
package com.ponysdk.core.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

public class HttpResponse implements Response {

    private final HttpServletResponse response;

    public HttpResponse(final HttpServletResponse response) {
        this.response = response;
    }

    @Override
    public void write(final String s) throws IOException {
        this.response.getWriter().write(s);
    }

    @Override
    public void flush() throws IOException {
        this.response.getWriter().flush();
    }

}
