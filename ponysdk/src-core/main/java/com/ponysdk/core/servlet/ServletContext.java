
package com.ponysdk.core.servlet;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ServletContext {

    private static final ServletContext instance = new ServletContext();

    private final ThreadLocal<ServletRequest> servletRequests = new ThreadLocal<ServletRequest>();
    private final ThreadLocal<ServletResponse> servletResponses = new ThreadLocal<ServletResponse>();

    private ServletContext() {}

    public static ServletContext get() {
        return instance;
    }

    void setContext(final ServletRequest servletRequest, final ServletResponse servletResponse) {
        servletRequests.set(servletRequest);
        servletResponses.set(servletResponse);
    }

    void remove() {
        servletRequests.remove();
        servletResponses.remove();
    }

    public ServletRequest getServletRequest() {
        return servletRequests.get();
    }

    public ServletResponse getServletResponse() {
        return servletResponses.get();
    }

}
