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

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
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
        assertTrue(BootstrapServlet.isSafeResourcePath("/ponyterminal/terminal.js"));
        assertTrue(BootstrapServlet.isSafeResourcePath("/ponyterminal/chunks/date-picker.js"));
    }

    @Test
    public void traversalAndMalformedPathsAreRejected() {
        assertFalse(BootstrapServlet.isSafeResourcePath(null), "null");
        assertFalse(BootstrapServlet.isSafeResourcePath(""), "empty");
        assertFalse(BootstrapServlet.isSafeResourcePath("/../etc/passwd"), "parent traversal");
        assertFalse(BootstrapServlet.isSafeResourcePath("/a/../../b"), "nested traversal");
        assertFalse(BootstrapServlet.isSafeResourcePath(".."), "bare ..");
        assertFalse(BootstrapServlet.isSafeResourcePath("/assets/.."), "trailing traversal");
        assertFalse(BootstrapServlet.isSafeResourcePath("/a\\..\\b"), "backslash");
        assertFalse(BootstrapServlet.isSafeResourcePath("/a\u0000.js"), "nul byte");
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
        assertTrue(html.contains("<title>MyApp</title>"), "title");
        assertTrue(html.contains("http-equiv=\"content-type\""), "content-type meta");
        assertTrue(html.contains("name=\"robots\" content=\"noindex\""), "custom meta");
        assertTrue(html.contains("<link id=\"main\" rel=\"stylesheet\" type=\"text/css\" href=\"css/app.css\"/>"), "style link");
        assertTrue(html.contains("ponyterminal/terminal.js"), "terminal script");
        assertTrue(html.contains("window.ponyReconnectMode = true"), "reconnect script");
        assertTrue(html.contains("pony.wc.registerSharedSheet('shared.css')"), "wc shared sheet");
        assertTrue(html.contains("src=\"script/app.js\""), "app script");
        assertTrue(html.contains("<div id=\"loading\">Loading MyApp"), "loading");
        assertTrue(html.contains("<noscript>"), "noscript");
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
        assertTrue(html.contains("ponyterminal/terminal.js"), "terminal script always present");
        assertFalse(html.contains("window.ponyReconnectMode"), "no reconnect script when disabled");
        assertFalse(html.contains("registerSharedSheet"), "no shared-sheet registration");
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

        assertTrue(html.contains("type=\"module\" src=\"ctx/ponyterminal/terminal.js\""));
        assertTrue(html.contains("src=\"ctx/script/ponysdk.js\""));
    }

    @Test
    public void theBootstrapPageOpensTheWebSocketBeforeLoadingTheTerminal() {
        final ApplicationConfiguration config = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(config.getApplicationName()).thenReturn("App");
        final String html = servletWith(config).buildIndexHTML(request("", "/index.html"));

        final int socket = html.indexOf("window.ponyEarlySocket=");
        final int terminal = html.indexOf("ponyterminal/terminal.js");
        assertTrue(socket > 0, "early socket script present");
        assertTrue(socket < terminal, "before the terminal script");
        assertTrue(html.contains("&w=" + com.ponysdk.core.model.ProtocolVersion.CURRENT + "'"),
            "announces the protocol version the terminal speaks");
        assertTrue(html.contains("has('1')"), "not in a child window");
    }

    @Test
    public void theTerminalIsAModuleLoadedAfterPonysdkJs() {
        final ApplicationConfiguration config = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(config.getApplicationName()).thenReturn("App");
        Mockito.when(config.getJavascript()).thenReturn(Set.of("script/app.js"));
        final String html = servletWith(config).buildIndexHTML(request("", "/index.html"));

        final int ponysdk = html.indexOf("src=\"script/ponysdk.js\"");
        final int terminal = html.indexOf("type=\"module\" src=\"ponyterminal/terminal.js\"");
        assertTrue(ponysdk > 0, "ponysdk.js present");
        assertTrue(terminal > ponysdk, "deferred terminal after ponysdk.js");
        assertTrue(html.indexOf("window.ponyEarlySocket=") < terminal, "early socket before the terminal");
    }

    @Test
    public void theLegacyLayerRunsBeforeTheTerminalWhenAskedFor() {
        final ApplicationConfiguration config = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(config.getApplicationName()).thenReturn("App");
        assertFalse(servletWith(config).buildIndexHTML(request("", "/index.html")).contains("legacy.js"), "off by default");

        Mockito.when(config.isLegacyClassNames()).thenReturn(true);
        final String html = servletWith(config).buildIndexHTML(request("", "/index.html"));
        final int legacy = html.indexOf("src=\"ponyterminal/legacy.js\" defer");
        assertTrue(legacy > 0, "legacy layer present");
        assertTrue(legacy < html.indexOf("type=\"module\" src=\"ponyterminal/terminal.js\""), "before the terminal");
    }

    // ---- HTTP caching of the served files ----

    /** A servlet serving {@code file} at {@code path} from its servlet context. */
    private static TestableBootstrapServlet servletServing(final String path, final File file) throws IOException {
        final ServletContext ctx = Mockito.mock(ServletContext.class);
        Mockito.when(ctx.getMimeType(Mockito.anyString())).thenReturn("text/javascript");
        Mockito.when(ctx.getResource(path)).thenReturn(file.toURI().toURL());
        return new TestableBootstrapServlet(ctx);
    }

    private static HttpServletResponse response(final ByteArrayOutputStream body) throws IOException {
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(final WriteListener listener) {}
            @Override public void write(final int b) { body.write(b); }
        });
        return response;
    }

    private static File tempFile(final String content) throws IOException {
        final File f = File.createTempFile("pony", ".js");
        f.deleteOnExit();
        Files.writeString(f.toPath(), content);
        return f;
    }

    @Test
    public void contentHashedFilesAreCachedForAYear() throws IOException {
        final String path = "/script/app.18E9900BEF20543F36A9691E2645F505.cache.js";
        final TestableBootstrapServlet servlet = servletServing(path, tempFile("terminal"));
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        final HttpServletResponse response = response(body);

        servlet.handleRequest(request("", path), response, path);

        Mockito.verify(response).setHeader("Cache-Control", "public, max-age=31536000, immutable");
        assertEquals("terminal", body.toString());
    }

    @Test
    public void otherFilesAreRevalidatedAgainstTheirModificationTime() throws IOException {
        final String path = "/script/ponysdk.js";
        final File file = tempFile("core");
        assertTrue(file.setLastModified(1_760_000_000_789L)); // ms precision, as on most file systems
        final TestableBootstrapServlet servlet = servletServing(path, file);
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        final HttpServletResponse response = response(body);
        final HttpServletRequest request = request("", path);

        servlet.handleRequest(request, response, path);

        Mockito.verify(response).setHeader("Cache-Control", "no-cache");
        assertEquals("core", body.toString());
        // HttpServlet.service answers 304 when If-Modified-Since (one-second precision) is not older
        assertEquals(1_760_000_000_000L, servlet.getLastModified(request));
    }

    @Test
    public void theBootstrapPageIsNeverServedFromCache() throws IOException {
        final ApplicationConfiguration config = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(config.getApplicationName()).thenReturn("App");
        final TestableBootstrapServlet servlet = servletWith(config);
        final HttpServletResponse response = response(new ByteArrayOutputStream());
        final HttpServletRequest request = request("", "/");

        servlet.handleRequest(request, response, "/index.html");

        Mockito.verify(response).setHeader("Cache-Control", "no-cache");
        assertEquals(-1L, servlet.getLastModified(request), "generated per request: no modification time");
    }

    /**
     * A client that never sends the cookie back (a page of another site, a health check) would leave a session per
     * request: Jetty never expires them by default. The WebSocket upgrade creates the session (ADR-0009, F3).
     */
    @Test
    public void noRequestCreatesASession() throws IOException {
        final ApplicationConfiguration config = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(config.getApplicationName()).thenReturn("App");
        final TestableBootstrapServlet servlet = servletWith(config);
        for (final String path : new String[] { "/index.html", "/ponyapp.json", "/script/ponysdk.js" }) {
            final HttpServletRequest request = request("", path);
            servlet.handleRequest(request, response(new ByteArrayOutputStream()), path);
            Mockito.verify(request, Mockito.never()).getSession();
            Mockito.verify(request, Mockito.never()).getSession(true);
        }
    }

    // ---- the manifest of an embedded application (ADR-0008) ----

    @Test
    public void theManifestListsWhatTheBootstrapPageLoads() {
        final ApplicationConfiguration config = Mockito.mock(ApplicationConfiguration.class);
        final Map<String, String> styles = new java.util.LinkedHashMap<>();
        styles.put("base", "style/base.css");
        styles.put("theme", "css/theme.css");
        Mockito.when(config.getStyle()).thenReturn(styles);
        Mockito.when(config.getJavascript()).thenReturn(new java.util.LinkedHashSet<>(java.util.List.of("script/a.js", "script/b.js")));
        Mockito.when(config.getWcSharedSheets()).thenReturn(Set.of("css/shared.css"));
        Mockito.when(config.getReconnectionTimeoutMs()).thenReturn(10_000L);
        final TestableBootstrapServlet servlet = servletWith(config);
        servlet.setRootPath("ctx/");

        final jakarta.json.JsonObject manifest = jakarta.json.Json.createReader(
            new java.io.StringReader(servlet.buildManifest(request("", "/ponyapp.json")))).readObject();

        assertEquals("[{\"id\":\"base\",\"href\":\"style/base.css\"},{\"id\":\"theme\",\"href\":\"css/theme.css\"}]",
            manifest.getJsonArray("styles").toString(), "in the configured order");
        assertEquals("[\"ctx/script/ponysdk.js\",\"script/a.js\",\"script/b.js\"]", manifest.getJsonArray("scripts").toString(),
            "ponysdk.js first, as in the bootstrap page");
        assertEquals("[\"css/shared.css\"]", manifest.getJsonArray("sharedSheets").toString());
        assertTrue(manifest.getBoolean("reconnection"));
    }

    @Test
    public void theManifestIsServedAsJsonAndRevalidated() throws IOException {
        final ApplicationConfiguration config = Mockito.mock(ApplicationConfiguration.class);
        final TestableBootstrapServlet servlet = servletWith(config);
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        final HttpServletResponse response = response(body);

        servlet.handleRequest(request("", "/ponyapp.json"), response, "/ponyapp.json");

        Mockito.verify(response).setContentType("application/json;charset=UTF-8");
        Mockito.verify(response).setHeader("Cache-Control", "no-cache");
        assertEquals("{\"styles\":[],\"scripts\":[\"script/ponysdk.js\"],\"sharedSheets\":[],\"reconnection\":false}",
            body.toString());
    }
}
