/*
 * Copyright (c) 2026 PonySDK
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.server.websocket;

import java.net.HttpCookie;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.servlet.http.HttpSession;

/**
 * Immutable snapshot of the data read from the WebSocket upgrade request, captured DURING the
 * handshake (in {@link WebSocketServlet#createWebsocket}). Jetty 12 recycles the underlying servlet
 * request once the upgrade completes, so any lazy read of
 * {@link org.eclipse.jetty.ee11.websocket.server.JettyServerUpgradeRequest} afterwards (parameter
 * map, headers, cookies, host, secure flag, remote address, session) throws a NullPointerException
 * or returns data from an unrelated request. UIContext serves request-derived data from this
 * snapshot instead of touching the live request.
 */
public final class UpgradeRequestData {

    private final Map<String, List<String>> parameterMap;
    private final Map<String, List<String>> headers;
    private final List<HttpCookie> cookies;
    private final String host;
    private final boolean secure;
    private final SocketAddress remoteSocketAddress;
    private final String userAgent;
    private final HttpSession session;

    public UpgradeRequestData(final Map<String, List<String>> parameterMap, final String userAgent,
                              final HttpSession session) {
        this(parameterMap, null, null, null, false, null, userAgent, session);
    }

    public UpgradeRequestData(final Map<String, List<String>> parameterMap,
                              final Map<String, List<String>> headers, final List<HttpCookie> cookies,
                              final String host, final boolean secure, final SocketAddress remoteSocketAddress,
                              final String userAgent, final HttpSession session) {
        // Deep, null-tolerant copies: the source collections may be backed by the live request (which
        // Jetty recycles after the upgrade), and Map.copyOf/List.copyOf reject null keys/values. Copy
        // each entry into independent unmodifiable structures so this snapshot is fully detached.
        this.parameterMap = copyMultiMap(parameterMap, false);
        this.headers = copyMultiMap(headers, true);
        this.cookies = cookies == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(cookies));
        this.host = host;
        this.secure = secure;
        this.remoteSocketAddress = remoteSocketAddress;
        this.userAgent = userAgent;
        this.session = session;
    }

    /**
     * @param caseInsensitiveKeys when true the returned map ignores key case (for HTTP headers, which
     *            are case-insensitive per RFC 7230); otherwise keys are compared exactly (query params).
     */
    private static Map<String, List<String>> copyMultiMap(final Map<String, List<String>> source,
                                                           final boolean caseInsensitiveKeys) {
        final Map<String, List<String>> copy = caseInsensitiveKeys
                ? new TreeMap<>(String.CASE_INSENSITIVE_ORDER) : new HashMap<>();
        if (source != null) {
            for (final Map.Entry<String, List<String>> e : source.entrySet()) {
                // A null key would NPE the case-insensitive TreeMap comparator; HTTP has no null header
                // name, so skip defensively rather than fail the whole handshake snapshot.
                if (e.getKey() == null) continue;
                final List<String> values = e.getValue();
                copy.put(e.getKey(), values == null ? Collections.emptyList()
                        : Collections.unmodifiableList(new ArrayList<>(values)));
            }
        }
        return Collections.unmodifiableMap(copy);
    }

    /** The upgrade request query parameters (never null). */
    public Map<String, List<String>> getParameterMap() {
        return parameterMap;
    }

    /** The upgrade request headers, keyed case-insensitively (never null). */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /** The first value of the given header, or null if absent (case-insensitive lookup). */
    public String getHeader(final String name) {
        final List<String> values = headers.get(name);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    /** The upgrade request cookies (never null). */
    public List<HttpCookie> getCookies() {
        return cookies;
    }

    /** The Host of the upgrade request, or null. */
    public String getHost() {
        return host;
    }

    /** Whether the upgrade request was received over a secure (TLS) transport. */
    public boolean isSecure() {
        return secure;
    }

    /** The remote socket address of the connection, or null. */
    public SocketAddress getRemoteSocketAddress() {
        return remoteSocketAddress;
    }

    /** The client User-Agent header, or null if absent. */
    public String getUserAgent() {
        return userAgent;
    }

    /** The HTTP session associated with the upgrade, or null. */
    public HttpSession getSession() {
        return session;
    }
}
