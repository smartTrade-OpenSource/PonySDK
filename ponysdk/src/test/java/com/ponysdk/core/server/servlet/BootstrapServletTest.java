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
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.LogLevel;
import com.ponysdk.core.server.LogCapture;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.ui.i18n.Locales;

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

    /** getResourceURL is protected: a subclass may call it without the request's check, so it checks itself. */
    @Test
    public void theResourceURLOfATraversalIsNull() throws IOException {
        final File f = File.createTempFile("pony", ".js");
        f.deleteOnExit();
        final String path = "/script/../secret.js";

        assertNull(servletServing(path, f).getResourceURL(path));
        assertNull(servletServing("/a\\b.js", f).getResourceURL("/a\\b.js"));
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
        assertTrue(html.contains("<meta name=\"pony-reconnection\" content=\"true\">"), "reconnection flag");
        assertTrue(html.contains("<meta name=\"pony-wc-shared-sheets\" content=\"shared.css\">"), "wc shared sheet");
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
        assertFalse(html.contains("pony-reconnection"), "no reconnection flag when disabled");
        assertFalse(html.contains("pony-wc-shared-sheets"), "no shared sheets");
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

    /** A window or a frame of an embedded terminal accepts these pages only as its parent (ADR-0009). */
    @Test
    public void theBootstrapPageListsThePagesAllowedToEmbedIt() {
        final ApplicationConfiguration config = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(config.getApplicationName()).thenReturn("App");
        Mockito.when(config.getWsAllowedOrigins()).thenReturn(Set.of("https://b.example", "https://a.example"));
        final String html = servletWith(config).buildIndexHTML(request("", "/index.html"));
        assertTrue(html.contains("<meta name=\"pony-parent-origins\" content=\"https://a.example https://b.example\">"), html);

        Mockito.when(config.getWsAllowedOrigins()).thenReturn(Set.of());
        assertFalse(servletWith(config).buildIndexHTML(request("", "/index.html")).contains("pony-parent-origins"));
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
        assertTrue(manifest.isNull("legacy"));
        assertEquals("ctx/ponyterminal/terminal.js", manifest.getString("terminal"), "under the root path, as ponysdk.js");
        assertEquals(1, manifest.getInt("mount"));
    }

    // ---- the default theme (ADR-0024) ----

    @Test
    public void theThemeIsLoadedBeforeTheApplicationsStylesheets() {
        final ApplicationConfiguration config = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(config.getApplicationName()).thenReturn("App");
        Mockito.when(config.getTheme()).thenReturn("pony");
        Mockito.when(config.getStyle()).thenReturn(Map.of("main", "css/app.css"));
        final TestableBootstrapServlet servlet = servletWith(config);
        servlet.setRootPath("ctx/");

        final String html = servlet.buildIndexHTML(request("", "/index.html"));

        final String theme = "<link id=\"pony-theme\" rel=\"stylesheet\" type=\"text/css\" href=\"ctx/style/pony-theme.css\"/>";
        final String main = "<link id=\"main\" rel=\"stylesheet\" type=\"text/css\" href=\"css/app.css\"/>";
        assertTrue(html.contains(theme), html);
        assertTrue(html.indexOf(theme) < html.indexOf(main), "the application's stylesheet comes after, so it wins");

        final jakarta.json.JsonObject manifest = jakarta.json.Json.createReader(
            new java.io.StringReader(servlet.buildManifest(request("", "/ponyapp.json")))).readObject();
        assertEquals("[{\"id\":\"pony-theme\",\"href\":\"ctx/style/pony-theme.css\"},{\"id\":\"main\",\"href\":\"css/app.css\"}]",
            manifest.getJsonArray("styles").toString(), "an embedded application loads it as the page does");
    }

    @Test
    public void theThemeAloneIsLoadedWithoutOtherStylesheets() {
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setTheme("pony");
        final String html = servletWith(config).buildIndexHTML(request("", "/index.html"));
        assertTrue(html.contains("href=\"style/pony-theme.css\""), html);
        assertTrue(html.contains("<html lang=\"en\" class=\"pony-theme\">"), "the theme applies to the whole page: the class is on its root");
    }

    // ---- the language of the page (WCAG 3.1.1, docs/guide/accessibility.md) ----

    @Test
    public void thePageHasALanguageEnglishUnlessTheApplicationSaysOtherwise() {
        String html = servletWith(new ApplicationConfiguration()).buildIndexHTML(request("", "/index.html"));
        assertTrue(html.contains("<html lang=\"en\">"), html);

        final ApplicationConfiguration french = new ApplicationConfiguration();
        french.setLanguage("fr-CA");
        french.setTheme("pony");
        html = servletWith(french).buildIndexHTML(request("", "/index.html"));
        assertTrue(html.contains("<html lang=\"fr-CA\" class=\"pony-theme\">"), html);
    }

    @Test
    public void withoutAThemeNoStylesheetIsAdded() {
        final String html = servletWith(new ApplicationConfiguration()).buildIndexHTML(request("", "/index.html"));
        assertFalse(html.contains("pony-theme"), html);
        assertTrue(html.contains("<html lang=\"en\">"), html);
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
        assertEquals("{\"styles\":[],\"scripts\":[\"script/ponysdk.js\"],\"sharedSheets\":[],\"reconnection\":false,\"terminal\":\"ponyterminal/terminal.js\",\"mount\":1,\"legacy\":null}",
            body.toString());
    }

    /** A servlet whose servlet context has no file: everything comes from the classpath. */
    private static TestableBootstrapServlet servletOnTheClasspath(final ApplicationConfiguration config) {
        final ServletContext ctx = Mockito.mock(ServletContext.class);
        final TestableBootstrapServlet servlet = new TestableBootstrapServlet(ctx);
        servlet.setConfiguration(config);
        return servlet;
    }

    private static HttpServletResponse served(final TestableBootstrapServlet servlet, final String path, final ByteArrayOutputStream body)
            throws IOException {
        final HttpServletResponse response = response(body);
        servlet.handleRequest(request("", path), response, path);
        return response;
    }

    @Test
    public void theClasspathServesWebFilesOnly() throws IOException {
        // Anything on the classpath used to be served: the classes, and an application's configuration
        final TestableBootstrapServlet servlet = servletOnTheClasspath(new ApplicationConfiguration());
        for (final String path : List.of("/com/ponysdk/core/server/servlet/BootstrapServlet.class",
            "/conf/MessagesCore.properties", "/conf/client_application.inc.xml", "/META-INF/mime.types")) {
            final ByteArrayOutputStream body = new ByteArrayOutputStream();
            Mockito.verify(served(servlet, path, body)).sendError(HttpServletResponse.SC_NOT_FOUND);
            assertEquals(0, body.size(), path + " is not served");
        }
        final ByteArrayOutputStream script = new ByteArrayOutputStream();
        served(servlet, "/script/ponysdk.js", script);
        assertTrue(script.size() > 0, "a script is served");
    }

    @Test
    public void anApplicationServesMoreTypesFromTheClasspath() throws IOException {
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.addServedResourceExtensions("properties");
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        served(servletOnTheClasspath(config), "/conf/MessagesCore.properties", body);
        assertTrue(body.size() > 0, "a type the application adds is served");
    }

    @Test
    public void getLastModifiedSaysNothingOfAFileThatIsNotServed() {
        final TestableBootstrapServlet servlet = servletOnTheClasspath(new ApplicationConfiguration());
        final String path = "/com/ponysdk/core/server/servlet/BootstrapServlet.class";
        assertEquals(-1, servlet.getLastModified(request("", path)));
    }

    // ---- Content Security Policy (ADR-0015) ----

    /** The headers the bootstrap page is served with, by name. */
    private static Map<String, String> pageHeaders(final ApplicationConfiguration config) throws IOException {
        final HttpServletResponse response = response(new ByteArrayOutputStream());
        final Map<String, String> headers = new java.util.HashMap<>();
        Mockito.doAnswer(call -> headers.put(call.getArgument(0), call.getArgument(1))).when(response)
            .setHeader(Mockito.anyString(), Mockito.anyString());
        servletWith(config).handleRequest(request("", "/"), response, "/index.html");
        return headers;
    }

    private static String sha256(final String script) throws Exception {
        final byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(script.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "'sha256-" + java.util.Base64.getEncoder().encodeToString(digest) + "'";
    }

    private static ApplicationConfiguration configuration() {
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setApplicationName("App");
        return config;
    }

    @Test
    public void theEarlySocketHashIsTheHashOfItsSource() throws Exception {
        assertEquals(sha256(BootstrapServlet.EARLY_SOCKET_SOURCE), BootstrapServlet.EARLY_SOCKET_HASH,
            "the early socket changed: update EARLY_SOCKET_HASH to this value");
    }

    @Test
    public void theBootstrapPageRunsOneInlineScriptWhoseHashThePolicyAllows() throws Exception {
        final ApplicationConfiguration config = configuration();
        config.setReconnectionTimeoutMs(5000);
        config.setWcSharedSheets(Set.of("css/shared.css"));
        final String html = servletWith(config).buildIndexHTML(request("", "/index.html"));

        final java.util.regex.Matcher inline = java.util.regex.Pattern.compile("<script[^>]*>([^<]+)</script>").matcher(html);
        assertTrue(inline.find(), "the early socket");
        final String script = inline.group(1);
        assertFalse(inline.find(), "no other inline script: " + html);
        assertTrue(html.contains("<meta name=\"pony-reconnection\" content=\"true\">"), "the reconnection flag");
        assertTrue(html.contains("<meta name=\"pony-wc-shared-sheets\" content=\"css/shared.css\">"), "the shared sheets");

        final String policy = pageHeaders(config).get("Content-Security-Policy");
        assertNotNull(policy, "the page carries a policy");
        assertTrue(policy.startsWith("script-src 'self' " + sha256(script)), policy);
        assertTrue(policy.contains("; object-src 'none'; base-uri 'self'; frame-ancestors 'self'"), policy);
        assertFalse(policy.contains("unsafe-inline"), policy);
    }

    @Test
    public void thePolicyAllowsWhatTheConfigurationLoadsAndTheSitesThatEmbedTheApplication() throws Exception {
        final ApplicationConfiguration config = configuration();
        config.setJavascript(new java.util.LinkedHashSet<>(List.of("script/app.js", "https://cdn.example/lib/x.js",
            "https://static.example:8443/y.js")));
        config.addScriptSources("https://api.example");
        config.setWsAllowedOrigins(Set.of("https://portal.example"));

        final String policy = pageHeaders(config).get("Content-Security-Policy");

        assertTrue(policy.contains(" https://cdn.example "), policy);
        assertTrue(policy.contains(" https://static.example:8443 "), policy);
        assertTrue(policy.contains(" https://api.example;"), policy);
        assertFalse(policy.contains("script/app.js"), "a path of the application is 'self': " + policy);
        assertTrue(policy.endsWith("frame-ancestors 'self' https://portal.example"), policy);
    }

    /** CSP has no syntax for an IPv6 address: Chrome rejects the whole expression and logs an error on every page. */
    @Test
    public void anOriginAtAnIpv6AddressIsLeftOut() throws Exception {
        final ApplicationConfiguration config = configuration();
        config.setWsAllowedOrigins(Set.of("http://[::1]:8082", "http://localhost:8082"));
        config.setJavascript(Set.of("http://[::1]:9000/x.js"));

        final String policy = pageHeaders(config).get("Content-Security-Policy");

        assertFalse(policy.contains("[::1]"), policy);
        assertTrue(policy.endsWith("frame-ancestors 'self' http://localhost:8082"), policy);
    }

    /** No API sends JavaScript source (ADR-0015): the page never needs eval. */
    @Test
    public void thePolicyNeverAllowsEval() throws Exception {
        final String policy = pageHeaders(configuration()).get("Content-Security-Policy");
        assertFalse(policy.contains("unsafe-eval"), policy);
    }

    /** ADR-0029: a WebAssembly library compiles its module, which `'wasm-unsafe-eval'` allows and eval is not. */
    @Test
    public void thePolicyAllowsWebAssemblyOnlyWhenAskedAndStillNeverEval() throws Exception {
        final String off = pageHeaders(configuration()).get("Content-Security-Policy");
        assertFalse(off.contains("wasm-unsafe-eval"), off);

        final ApplicationConfiguration config = configuration();
        config.setWebAssemblyEnabled(true);
        config.addScriptSources("blob:");
        final String on = pageHeaders(config).get("Content-Security-Policy");
        assertTrue(on.matches("script-src 'self' 'sha256-[A-Za-z0-9+/=]+' 'wasm-unsafe-eval' blob:; object-src.*"), on);
        assertFalse(on.contains("'unsafe-eval'"), on);
    }

    @Test
    public void anApplicationThatSendsItsOwnPolicyTurnsPonySdksOff() throws Exception {
        final ApplicationConfiguration config = configuration();
        config.setContentSecurityPolicyEnabled(false);
        assertNull(pageHeaders(config).get("Content-Security-Policy"));
    }

    @Test
    public void noResponseIsSniffedIntoAnotherType() throws Exception {
        assertEquals("nosniff", pageHeaders(configuration()).get("X-Content-Type-Options"), "the page");
        final HttpServletResponse file = served(servletOnTheClasspath(new ApplicationConfiguration()), "/script/ponysdk.js",
            new ByteArrayOutputStream());
        Mockito.verify(file).setHeader("X-Content-Type-Options", "nosniff");
    }

    @Test
    public void aSharedSheetCannotBreakOutOfItsAttribute() {
        final ApplicationConfiguration config = configuration();
        config.setWcSharedSheets(Set.of("a\"><script>x</script>.css"));
        final String html = servletWith(config).buildIndexHTML(request("", "/index.html"));
        assertTrue(html.contains("content=\"a&quot;>&lt;script>x&lt;/script>.css\""), html);
    }

    // ---- request dispatch and failures ----

    @Test
    public void theRootAndAnEmptyPathServeTheBootstrapPage() {
        final TestableBootstrapServlet servlet = servletWith(configuration());
        for (final String uri : new String[] { "/app", "/app/" }) {
            final ByteArrayOutputStream body = new ByteArrayOutputStream();
            final HttpServletResponse response;
            try {
                response = response(body);
            } catch (final IOException e) {
                throw new AssertionError(e);
            }
            servlet.doGet(request("/app", uri), response);
            assertTrue(body.toString().startsWith("<!doctype html>"), uri + ": " + body);
        }
    }

    @Test
    public void aPostServesTheResourceOfItsPath() throws IOException {
        final String path = "/script/app.js";
        final TestableBootstrapServlet servlet = servletServing(path, tempFile("posted"));
        final ByteArrayOutputStream body = new ByteArrayOutputStream();

        servlet.doPost(request("", path), response(body));

        assertEquals("posted", body.toString());
    }

    @Test
    public void theDebugLogChangesNothingToTheResourceServed() throws IOException {
        final String path = "/script/app.js";
        final TestableBootstrapServlet servlet = servletServing(path, tempFile("logged"));
        final ByteArrayOutputStream body = new ByteArrayOutputStream();

        try (LogLevel debug = LogLevel.set(BootstrapServlet.class, "DEBUG")) {
            servlet.doGet(request("", path), response(body));
        }

        assertEquals("logged", body.toString());
    }

    @Test
    public void anUnsafePathIsNotFound() throws IOException {
        final TestableBootstrapServlet servlet = servletOnTheClasspath(new ApplicationConfiguration());
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        final HttpServletResponse response = response(body);

        servlet.doGet(request("", "/script/../conf/x.js"), response);

        Mockito.verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        assertEquals(0, body.size());
    }

    @Test
    public void aFailureBeforeTheResponseIsCommittedAnswers500() throws IOException {
        final ServletContext ctx = Mockito.mock(ServletContext.class);
        Mockito.when(ctx.getResource(Mockito.anyString())).thenThrow(new IllegalStateException("broken context"));
        final HttpServletResponse response = response(new ByteArrayOutputStream());

        new TestableBootstrapServlet(ctx).doGet(request("", "/script/app.js"), response);

        Mockito.verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void aBrowserLeavingBeforeTheResponseIsCommittedIsAnswered500() throws IOException {
        final String path = "/script/app.js";
        final TestableBootstrapServlet servlet = servletServing(path, tempFile("content"));
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(response.getOutputStream()).thenThrow(new IOException("browser left"));

        servlet.doGet(request("", path), response);

        Mockito.verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void aRequestWithoutPathServesTheBootstrapPage() throws IOException {
        final TestableBootstrapServlet servlet = servletWith(configuration());
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        final HttpServletRequest request = request(null, null);

        servlet.doGet(request, response(body));

        assertTrue(body.toString().startsWith("<!doctype html>"), body.toString());
        assertEquals(-1L, servlet.getLastModified(request));
    }

    @Test
    public void getPathOfARequestWithoutContextPathIsItsUri() {
        assertEquals("/x.js", new BootstrapServlet().getPath(request(null, "/x.js")));
    }

    @Test
    public void theBootstrapPageAndUnsafePathsHaveNoModificationTime() {
        final TestableBootstrapServlet servlet = servletOnTheClasspath(new ApplicationConfiguration());
        for (final String uri : new String[] { "/app", "/app/index.html", "/app/script/../conf/x.js" }) {
            assertEquals(-1L, servlet.getLastModified(request("/app", uri)), uri);
        }
    }

    @Test
    public void aFailureAfterTheResponseIsCommittedSendsNothingMore() throws IOException {
        final String path = "/script/app.js";
        final TestableBootstrapServlet servlet = servletServing(path, tempFile("content"));
        for (final Exception failure : new Exception[] { new IOException("browser left"), new IllegalStateException("bug") }) {
            final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
            Mockito.when(response.isCommitted()).thenReturn(true);
            Mockito.when(response.getOutputStream()).thenThrow(failure);

            servlet.doGet(request("", path), response);

            Mockito.verify(response, Mockito.never()).sendError(Mockito.anyInt());
        }
    }

    @Test
    public void aFailingErrorResponseIsLogged() throws IOException {
        final ServletContext ctx = Mockito.mock(ServletContext.class);
        Mockito.when(ctx.getResource(Mockito.anyString())).thenThrow(new IllegalStateException("broken context"));
        final HttpServletResponse response = response(new ByteArrayOutputStream());
        Mockito.doThrow(new IOException("connection reset")).when(response).sendError(Mockito.anyInt());
        final TestableBootstrapServlet servlet = new TestableBootstrapServlet(ctx);

        assertDoesNotThrow(() -> servlet.doGet(request("", "/script/app.js"), response));
        assertDoesNotThrow(() -> servlet.doPost(request("", "/script/app.js"), response));
        Mockito.verify(response, Mockito.times(2)).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void aResourceThatCannotBeOpenedIsNotFound() throws IOException {
        final String path = "/script/gone.js";
        final File missing = new File(Files.createTempDirectory("pony").toFile(), "gone.js");
        final TestableBootstrapServlet servlet = servletServing(path, missing);
        final HttpServletResponse response = response(new ByteArrayOutputStream());

        servlet.handleRequest(request("", path), response, path);

        Mockito.verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        assertEquals(-1, servlet.getLastModified(request("", path)), "no modification time for a missing file");
    }

    @Test
    public void aResourceWhoseConnectionFailsHasNoModificationTime() throws IOException {
        final String path = "/script/ponysdk.js";
        final URL unreachable = URL.of(URI.create("pony://host" + path), new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(final URL url) throws IOException {
                throw new IOException("unreachable");
            }
        });
        final ServletContext ctx = Mockito.mock(ServletContext.class);
        Mockito.when(ctx.getResource(path)).thenReturn(unreachable);

        assertEquals(-1, new TestableBootstrapServlet(ctx).getLastModified(request("", path)));
    }

    @Test
    public void aMalformedResourceUrlIsNotFound() throws IOException {
        final ServletContext ctx = Mockito.mock(ServletContext.class);
        Mockito.when(ctx.getResource(Mockito.anyString())).thenThrow(new java.net.MalformedURLException("bad"));
        final TestableBootstrapServlet servlet = new TestableBootstrapServlet(ctx);
        final HttpServletResponse response = response(new ByteArrayOutputStream());

        servlet.handleRequest(request("", "/script/ponysdk.js"), response, "/script/ponysdk.js");

        Mockito.verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        assertEquals(-1, servlet.getLastModified(request("", "/script/ponysdk.js")));
    }

    @Test
    public void aMissingResourceIsNotAServerError() throws IOException {
        final TestableBootstrapServlet servlet = servletOnTheClasspath(null);
        try (LogCapture log = LogCapture.of(BootstrapServlet.class)) {
            servlet.handleRequest(request("", "/script/absent.js"), response(new ByteArrayOutputStream()), "/script/absent.js");
            assertEquals(List.of(), log.events().stream().filter(e -> !e.startsWith("DEBUG")).toList());
        }
    }

    @Test
    public void aPathWithoutExtensionIsNotServedFromTheClasspath() throws IOException {
        final TestableBootstrapServlet servlet = servletOnTheClasspath(null);
        final HttpServletResponse response = response(new ByteArrayOutputStream());

        servlet.handleRequest(request("", "/script.d/ponysdk"), response, "/script.d/ponysdk");

        Mockito.verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void withoutConfigurationTheClasspathServesTheDefaultTypes() throws IOException {
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        served(servletOnTheClasspath(null), "/script/ponysdk.js", body);
        assertTrue(body.size() > 0);
    }

    @Test
    public void aNullServedTypeSetFallsBackToTheDefaultTypes() throws IOException {
        final ApplicationConfiguration config = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(config.getServedResourceExtensions()).thenReturn(null);
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        served(servletOnTheClasspath(config), "/script/ponysdk.js", body);
        assertTrue(body.size() > 0);
    }

    @Test
    public void theChildClassLoaderServesWhatTheContextClassLoaderLacks() throws IOException {
        final File dir = Files.createTempDirectory("pony").toFile();
        final File script = new File(dir, "plugin-only.js");
        Files.writeString(script.toPath(), "plugin");
        try (java.net.URLClassLoader child = new java.net.URLClassLoader(new java.net.URL[] { dir.toURI().toURL() }, null)) {
            final ServletContext ctx = Mockito.mock(ServletContext.class);
            final BootstrapServlet servlet = new BootstrapServlet(child) {

                @Override
                public ServletContext getServletContext() {
                    return ctx;
                }
            };
            final ByteArrayOutputStream body = new ByteArrayOutputStream();

            servlet.handleRequest(request("", "/plugin-only.js"), response(body), "/plugin-only.js");

            assertEquals("plugin", body.toString());
        } finally {
            script.delete();
            dir.delete();
        }
    }

    @Test
    public void aMalformedScriptUrlIsLeftOutOfThePolicy() throws Exception {
        final ApplicationConfiguration config = configuration();
        config.setJavascript(Set.of("http://exa mple/x.js", "//cdn.example/y.js"));

        final String policy = pageHeaders(config).get("Content-Security-Policy");

        assertFalse(policy.contains("exa mple"), policy);
        assertTrue(policy.contains(" cdn.example"), "a scheme-relative URL keeps its host: " + policy);
    }

    @Test
    public void theManifestNamesTheLegacyLayerWhenAskedFor() {
        final ApplicationConfiguration config = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(config.isLegacyClassNames()).thenReturn(true);

        final jakarta.json.JsonObject manifest = jakarta.json.Json.createReader(
            new java.io.StringReader(servletWith(config).buildManifest(request("", "/ponyapp.json")))).readObject();

        assertEquals("ponyterminal/legacy.js", manifest.getString("legacy"));
    }

    @Test
    public void theManifestOfAConfigurationWithoutStylesScriptsOrSheetsLoadsPonysdkJsAlone() {
        final jakarta.json.JsonObject manifest = jakarta.json.Json.createReader(new java.io.StringReader(
            servletWith(new ApplicationConfiguration()).buildManifest(request("", "/ponyapp.json")))).readObject();

        assertEquals("[]", manifest.getJsonArray("styles").toString());
        assertEquals("[\"script/ponysdk.js\"]", manifest.getJsonArray("scripts").toString());
        assertEquals("[]", manifest.getJsonArray("sharedSheets").toString());
    }

    // ---- the visitor's locale on the page's root (docs/decisions/0022-locale.md) ----

    private static HttpServletRequest requestFrom(final String acceptLanguage, final String localeCookie) {
        final HttpServletRequest request = request("", "/");
        Mockito.when(request.getHeader("Accept-Language")).thenReturn(acceptLanguage);
        if (localeCookie != null) {
            Mockito.when(request.getCookies()).thenReturn(new jakarta.servlet.http.Cookie[] {
                new jakarta.servlet.http.Cookie("theme", "dark"), new jakarta.servlet.http.Cookie(Locales.COOKIE, localeCookie)});
        }
        return request;
    }

    private static ApplicationConfiguration translated() {
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setApplicationName("App");
        config.setSupportedLocales(List.of(java.util.Locale.ENGLISH, java.util.Locale.FRENCH, java.util.Locale.forLanguageTag("ar")));
        return config;
    }

    @Test
    public void theRootOfAPageWithoutSupportedLocalesHasTheConfiguredLanguageNotTheVisitors() {
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setApplicationName("App");

        assertTrue(servletWith(config).buildIndexHTML(requestFrom("fr", null)).contains("\n<html lang=\"en\">\n"));
    }

    @Test
    public void theRootCarriesTheLanguageAndTheDirectionOfTheVisitor() {
        final BootstrapServlet servlet = servletWith(translated());

        assertTrue(servlet.buildIndexHTML(requestFrom("fr-CH,fr;q=0.9", null)).contains("<html lang=\"fr\" dir=\"ltr\">"));
        assertTrue(servlet.buildIndexHTML(requestFrom("ar", null)).contains("<html lang=\"ar\" dir=\"rtl\">"));
        assertTrue(servlet.buildIndexHTML(requestFrom(null, null)).contains("<html lang=\"en\" dir=\"ltr\">"), "the first supported");
    }

    @Test
    public void theVisitorsChoiceIsTheCookiesAndTheCookiesOfOtherNamesAreIgnored() {
        final BootstrapServlet servlet = servletWith(translated());

        assertTrue(servlet.buildIndexHTML(requestFrom("fr", "ar")).contains("<html lang=\"ar\" dir=\"rtl\">"));
        final HttpServletRequest others = request("", "/");
        Mockito.when(others.getCookies()).thenReturn(new jakarta.servlet.http.Cookie[] {new jakarta.servlet.http.Cookie("a", "b")});
        assertTrue(servlet.buildIndexHTML(others).contains("<html lang=\"en\" dir=\"ltr\">"));
    }

    @Test
    public void thePageVariesWithTheHeaderAndTheCookieItIsNegotiatedFrom() throws IOException {
        final HttpServletResponse response = response(new ByteArrayOutputStream());
        servletWith(translated()).handleRequest(requestFrom("fr", null), response, "/index.html");
        Mockito.verify(response).setHeader("Vary", "Accept-Language, Cookie");

        final HttpServletResponse plain = response(new ByteArrayOutputStream());
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setApplicationName("App");
        servletWith(config).handleRequest(requestFrom("fr", null), plain, "/index.html");
        Mockito.verify(plain, Mockito.never()).setHeader(Mockito.eq("Vary"), Mockito.anyString());
    }
}
