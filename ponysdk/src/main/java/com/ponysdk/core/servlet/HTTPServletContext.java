
package com.ponysdk.core.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HTTPServletContext {

    private static final HTTPServletContext instance = new HTTPServletContext();

    private final ThreadLocal<HttpServletRequest> servletRequests = new ThreadLocal<>();
    private final ThreadLocal<HttpServletResponse> servletResponses = new ThreadLocal<>();

    private HTTPServletContext() {
    }

    public static HTTPServletContext get() {
        return instance;
    }

    void setContext(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse) {
        servletRequests.set(servletRequest);
        servletResponses.set(servletResponse);
    }

    void remove() {
        servletRequests.remove();
        servletResponses.remove();
    }

    public HttpServletRequest getRequest() {
        return servletRequests.get();
    }

    public HttpServletResponse getResponse() {
        return servletResponses.get();
    }

}
