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

import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.application.ApplicationConfiguration;

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

    // ---- buildIndexHTML / HTML generation ----

    /** Bootstrap servlet whose servlet context is a mock (so addStyle can resolve mime types). */
    private static final class TestableBootstrapServlet extends BootstrapServlet {

        private final ServletContext ctx;

        TestableBootstrapServlet(final ServletContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public ServletContext getServletContext() {
            return ctx;
        }
    }

    private static TestableBootstrapServlet servletWith(final ApplicationConfiguration config) {
        final ServletContext ctx = Mockito.mock(ServletContext.class);
        Mockito.when(ctx.getMimeType(Mockito.anyString())).thenReturn("text/css");
        final TestableBootstrapServlet servlet = new TestableBootstrapServlet(ctx);
        servlet.setConfiguration(config);
        return servlet;
    }

    @Test
    public void buildIndexHTML_containsAllSectionsForAFullConfiguration() {
        final ApplicationConfiguration config = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(config.getApplicationName()).thenReturn("MyApp");
        Mockito.when(config.getMeta()).thenReturn(Set.of("name=\"robots\" content=\"noindex\""));
        Mockito.when(config.getStyle()).thenReturn(Map.of("main", "css/app.css"));
        Mockito.when(config.getJavascript()).thenReturn(Set.of("script/app.js"));
        Mockito.when(config.getWcSharedSheets()).thenReturn(Set.of("shared.css"));
        Mockito.when(config.getReconnectionTimeoutMs()).thenReturn(5000L);

        final String html = servletWith(config).buildIndexHTML(request("", "/index.html"));

        assertTrue(html.startsWith("<!doctype html>"));
        assertTrue("title", html.contains("<title>MyApp</title>"));
        assertTrue("content-type meta", html.contains("http-equiv=\"content-type\""));
        assertTrue("custom meta", html.contains("name=\"robots\" content=\"noindex\""));
        assertTrue("style link", html.contains("<link id=\"main\" rel=\"stylesheet\" type=\"text/css\" href=\"css/app.css\"/>"));
        assertTrue("terminal script", html.contains("ponyterminal/ponyterminal.nocache.js"));
        assertTrue("reconnect script", html.contains("window.ponyReconnectMode = true"));
        assertTrue("wc shared sheet", html.contains("pony.wc.registerSharedSheet('shared.css')"));
        assertTrue("app script", html.contains("src=\"script/app.js\""));
        assertTrue("loading", html.contains("<div id=\"loading\">Loading MyApp"));
        assertTrue("history iframe", html.contains("__gwt_historyFrame"));
        assertTrue("noscript", html.contains("<noscript>"));
        assertTrue(html.trim().endsWith("</html>"));
    }

    @Test
    public void buildIndexHTML_handlesMinimalConfigurationWithoutOptionalSections() {
        final ApplicationConfiguration config = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(config.getApplicationName()).thenReturn("Bare");
        // meta/style/javascript/wcSharedSheets all null, no reconnection
        Mockito.when(config.getReconnectionTimeoutMs()).thenReturn(0L);

        final String html = servletWith(config).buildIndexHTML(request("", "/index.html"));

        assertTrue(html.contains("<title>Bare</title>"));
        assertTrue("terminal script always present", html.contains("ponyterminal/ponyterminal.nocache.js"));
        assertFalse("no reconnect script when disabled", html.contains("window.ponyReconnectMode"));
        assertFalse("no shared-sheet registration", html.contains("registerSharedSheet"));
        assertTrue(html.contains("<div id=\"loading\">Loading Bare"));
    }

    @Test
    public void buildIndexHTML_prependsRootPathToTerminalAndCoreScripts() {
        final ApplicationConfiguration config = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(config.getApplicationName()).thenReturn("Rooted");
        Mockito.when(config.getReconnectionTimeoutMs()).thenReturn(0L);
        final TestableBootstrapServlet servlet = servletWith(config);
        servlet.setRootPath("ctx/");

        final String html = servlet.buildIndexHTML(request("", "/index.html"));

        assertTrue(html.contains("src=\"ctx/ponyterminal/ponyterminal.nocache.js\""));
        assertTrue(html.contains("src=\"ctx/script/ponysdk.js\""));
    }
}
