
package com.ponysdk.core.servlet;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

public class HttpResponse implements Response {

    private final HttpServletResponse response;

    public HttpResponse(final HttpServletResponse response) {
        this.response = response;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return this.response.getOutputStream();
    }

}
