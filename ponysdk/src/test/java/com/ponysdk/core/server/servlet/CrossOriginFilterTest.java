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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;

/** {@link CrossOriginFilter}: CORS for the hosts allowed to embed the application (ADR-0008). */
public class CrossOriginFilterTest {

    private static final String HOST = "https://portal.example.com";

    private final CrossOriginFilter filter = new CrossOriginFilter(() -> Set.of(HOST));
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain chain = mock(FilterChain.class);

    private static HttpServletRequest request(final String method, final String origin) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getHeader("Origin")).thenReturn(origin);
        return request;
    }

    @Test
    public void anAllowedHostReadsTheResponseWithItsCredentials() throws Exception {
        final HttpServletRequest request = request("GET", HOST);
        filter.doFilter(request, response, chain);
        verify(response).setHeader("Access-Control-Allow-Origin", HOST);
        verify(response).setHeader("Access-Control-Allow-Credentials", "true");
        verify(response).addHeader("Vary", "Origin");
        verify(chain).doFilter(request, response);
    }

    @Test
    public void aPreflightIsAnsweredWithTheHeadersAsked() throws Exception {
        final HttpServletRequest request = request("OPTIONS", HOST);
        when(request.getHeader("Access-Control-Request-Method")).thenReturn("GET");
        when(request.getHeader("Access-Control-Request-Headers")).thenReturn("ui_context_id,object_id,price");
        filter.doFilter(request, response, chain);
        verify(response).setHeader("Access-Control-Allow-Origin", HOST);
        verify(response).setHeader("Access-Control-Allow-Methods", "GET, POST");
        verify(response).setHeader("Access-Control-Allow-Headers", "ui_context_id,object_id,price");
        verify(response).setStatus(HttpServletResponse.SC_NO_CONTENT);
        verifyNoInteractions(chain);
    }

    @Test
    public void anotherOriginGetsNoCorsHeaders() throws Exception {
        final HttpServletRequest request = request("GET", "https://evil.example.org");
        filter.doFilter(request, response, chain);
        verify(response, never()).setHeader(anyString(), anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    public void aRequestWithoutOriginPassesUntouched() throws Exception {
        final HttpServletRequest request = request("GET", null);
        filter.doFilter(request, response, chain);
        verify(response, never()).setHeader(anyString(), anyString());
        verify(chain).doFilter(request, response);
    }
}
