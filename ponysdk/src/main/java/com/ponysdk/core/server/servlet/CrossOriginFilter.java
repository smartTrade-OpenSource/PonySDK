/*
 * Copyright (c) 2026 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
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

package com.ponysdk.core.server.servlet;

import java.io.IOException;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * CORS, with credentials, for the pages allowed to embed the application in a {@code <pony-app>} element
 * (docs/decisions/0008-embedded-application.md): the terminal's modules, the manifest, the reconnection ping and the
 * addons' AJAX requests come from the host's origin. A preflight is answered with the headers it asks for: the addons'
 * AJAX requests carry their data as headers whose names only the addon knows. A request from another origin gets no
 * CORS header, so its page cannot read the response; the WebSocket upgrade is left to {@code WebSocketServlet}'s own
 * Origin check.
 */
public class CrossOriginFilter implements Filter {

    private final Supplier<Set<String>> allowedOrigins;

    /** allowedOrigins: read on each request, as the WebSocket's Origin check reads them. */
    public CrossOriginFilter(final Supplier<Set<String>> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;
        final String origin = request.getHeader("Origin");
        final Set<String> allowed = origin == null ? null : allowedOrigins.get();
        if (allowed == null || !allowed.contains(origin)) {
            chain.doFilter(request, response);
            return;
        }
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.addHeader("Vary", "Origin");
        if ("OPTIONS".equals(request.getMethod()) && request.getHeader("Access-Control-Request-Method") != null) {
            response.setHeader("Access-Control-Allow-Methods", "GET, POST");
            final String headers = request.getHeader("Access-Control-Request-Headers");
            if (headers != null) response.setHeader("Access-Control-Allow-Headers", headers);
            response.setHeader("Access-Control-Max-Age", "600");
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        chain.doFilter(request, response);
    }
}
