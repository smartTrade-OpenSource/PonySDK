package com.ponysdk.core.server.context;

import java.net.HttpCookie;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;

import com.ponysdk.core.useragent.UserAgent;

public class RequestContext {
    private final Map<String, List<String>> parameters;
    private final Map<String, List<String>> headers;
    private final Object session;
    private final UserAgent userAgent;
    private final List<HttpCookie> cookies;
    private final boolean secure;
    private final String remoteAddr;
    private final String host;

    public RequestContext(JettyServerUpgradeRequest request) {
        parameters = Collections.unmodifiableMap(request.getParameterMap());
        headers = Collections.unmodifiableMap(request.getHeaders());
        session = request.getSession();
        userAgent = request.getHeaders().get("User-Agent").stream().map(UserAgent::parseUserAgentString).findFirst().orElse(null);
        cookies = Collections.unmodifiableList(request.getCookies());
        secure = request.isSecure();
        remoteAddr = request.getHttpServletRequest().getRemoteAddr();
        host = request.getHost();
    }

    public Map<String, List<String>> getParameters() {
        return parameters;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Object getSession() {
        return session;
    }

    public UserAgent getUserAgent() {
        return userAgent;
    }

    public List<HttpCookie> getCookies() {
        return cookies;
    }

    public boolean isSecure() {
        return secure;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public String getHost() {
        return host;
    }
}