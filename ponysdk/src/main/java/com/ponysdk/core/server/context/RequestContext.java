package com.ponysdk.core.server.context;

import java.util.List;
import java.util.Map;

public class RequestContext {
    private final Map<String, List<String>> parameterMap;
    private final Map<String, List<String>> headers;
    private final Object session;

    public RequestContext(Map<String, List<String>> parameterMap, Map<String, List<String>> headers, Object session) {
        this.parameterMap = parameterMap;
        this.headers = headers;
        this.session = session;
    }

    public Map<String, List<String>> getParameterMap() {
        return parameterMap;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Object getSession() {
        return session;
    }
}
