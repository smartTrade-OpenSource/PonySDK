/*
 * Copyright (c) 2011 PonySDK
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for {@link BootstrapServlet} path handling — context-path stripping and the
 * path-traversal guard ({@link BootstrapServlet#isSafeResourcePath(String)}).
 */
public class BootstrapServletTest {

    @Test
    public void legitimateResourcePathsAreAccepted() {
        assertTrue(BootstrapServlet.isSafeResourcePath("/index.html"));
        assertTrue(BootstrapServlet.isSafeResourcePath("/script/ponysdk.js"));
        assertTrue(BootstrapServlet.isSafeResourcePath("/css/sample.css"));
        assertTrue(BootstrapServlet.isSafeResourcePath("/ponyterminal/ponyterminal.nocache.js"));
    }

    @Test
    public void traversalAndMalformedPathsAreRejected() {
        assertFalse("null", BootstrapServlet.isSafeResourcePath(null));
        assertFalse("empty", BootstrapServlet.isSafeResourcePath(""));
        assertFalse("parent traversal", BootstrapServlet.isSafeResourcePath("/../etc/passwd"));
        assertFalse("nested traversal", BootstrapServlet.isSafeResourcePath("/a/../../b"));
        assertFalse("bare ..", BootstrapServlet.isSafeResourcePath(".."));
        assertFalse("trailing traversal", BootstrapServlet.isSafeResourcePath("/assets/.."));
        assertFalse("backslash", BootstrapServlet.isSafeResourcePath("/a\\..\\b"));
        assertFalse("nul byte", BootstrapServlet.isSafeResourcePath("/a\u0000.js"));
    }

    @Test
    public void getPathStripsContextPathAsLiteralPrefix() {
        final BootstrapServlet servlet = new BootstrapServlet();

        assertEquals("/index.html", servlet.getPath(request("/app", "/app/index.html")));
        assertEquals("/script/x.js", servlet.getPath(request("/app", "/app/script/x.js")));
        // No context path → URI returned as-is
        assertEquals("/index.html", servlet.getPath(request("", "/index.html")));
        // Context path not a prefix → URI returned unchanged (no accidental mid-string strip)
        assertEquals("/other/app/x", servlet.getPath(request("/app", "/other/app/x")));
    }

    private static HttpServletRequest request(final String contextPath, final String requestURI) {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getContextPath()).thenReturn(contextPath);
        Mockito.when(request.getRequestURI()).thenReturn(requestURI);
        return request;
    }
}
