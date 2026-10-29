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

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.MappingPath;
import com.ponysdk.core.model.ProtocolVersion;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.ui.i18n.Locales;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public class BootstrapServlet extends HttpServlet {

    private static final String INDEX_URL = "/index.html";
    /** What a {@code <pony-app>} element of another page loads (docs/decisions/0008-embedded-application.md). */
    private static final String MANIFEST_URL = "/ponyapp.json";
    /** Files named after their content (*.cache.*) never change under the same name. */
    private static final String IMMUTABLE = "public, max-age=31536000, immutable";
    /** Everything else may change under the same name: the browser revalidates (304 when unmodified). */
    private static final String REVALIDATE = "no-cache";
    protected static final String NEW_LINE = "\n";
    /** The id of the theme's link (ApplicationConfiguration.setTheme). */
    private static final String THEME_ID = "pony-theme";

    protected static final String TITLE_PATTERN = "<title>%s</title>";
    protected static final String META_PATTERN = "<meta %s>";
    protected static final String STYLE_PATTERN = "<link id=\"%s\" rel=\"stylesheet\" type=\"%s\" href=\"%s\"/>";
    protected static final String SCRIPT_PATTERN = "<script type=\"text/javascript\" src=\"%s\"></script>";
    protected static final String DEFERRED_SCRIPT_PATTERN = "<script type=\"text/javascript\" src=\"%s\" defer></script>";
    /** An ES module: deferred too, run in order with the deferred scripts. */
    protected static final String MODULE_SCRIPT_PATTERN = "<script type=\"module\" src=\"%s\"></script>";

    private static final Logger log = LoggerFactory.getLogger(BootstrapServlet.class);

    protected ApplicationConfiguration configuration;

    protected String rootPath = "";

    private ClassLoader childClassLoader;

    public BootstrapServlet() {
    }

    public BootstrapServlet(final ClassLoader classLoader) {
        this.childClassLoader = classLoader;
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            handlePonyResource(request, response);
        } catch (final IOException e) {
            log.error("Cannot stream request", e);
        }
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            handlePonyResource(request, response);
        } catch (final IOException e) {
            log.error("Cannot stream request", e);
        }
    }

    protected void handlePonyResource(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        try {
            final String extraPathInfo = getPath(request);

            if (extraPathInfo == null || extraPathInfo.isEmpty() || extraPathInfo.equals("/")) {
                handleRequest(request, response, INDEX_URL);
            } else {
                if (log.isDebugEnabled()) log.debug("Loading resource: {}", extraPathInfo);
                handleRequest(request, response, extraPathInfo);
            }

        } catch (final Throwable e) {
            if (e instanceof IOException && response.isCommitted()) {
                // The browser left during the response (a page closed while a terminal chunk downloads)
                log.debug("Response to {} not completed: {}", request.getRequestURI(), e.toString());
                return;
            }
            log.error("Cannot process the request", e);
            if (!response.isCommitted()) response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected String getPath(final HttpServletRequest request) {
        final String contextPath = request.getContextPath();
        final String uri = request.getRequestURI();
        // Strip the context path as a literal prefix — NOT as a regex (the previous
        // replaceFirst treated contextPath as a regex and could strip the wrong span).
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    /**
     * Validates that a requested resource path cannot escape the web root / classpath.
     * Rejects absolute traversal ({@code ..} segments), backslashes and NUL bytes.
     * Returns {@code false} for unsafe paths, which callers must turn into a 404.
     */
    protected static boolean isSafeResourcePath(final String path) {
        if (path == null || path.isEmpty()) return false;
        // No ".." anywhere, not only as a segment: no web file needs one, and the check stays one test
        return !path.contains("..") && path.indexOf('\\') < 0 && path.indexOf('\0') < 0;
    }

    protected void handleRequest(final HttpServletRequest request, final HttpServletResponse response, final String path)
            throws IOException {
        // A script, a stylesheet or the page is taken for what its type says, never sniffed into another (ADR-0015)
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Reject path traversal before touching the servlet context / classpath.
        if (!isSafeResourcePath(path)) {
            log.warn("Rejected unsafe resource path");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // No session here: the WebSocket upgrade creates it. A client that never sends the cookie back (a page of
        // another site, a health check) would leave one per request, which Jetty never expires (ADR-0009, F3)
        if (path.equals(MANIFEST_URL)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Cache-Control", REVALIDATE);
            try (OutputStream out = response.getOutputStream()) {
                out.write(buildManifest(request).getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        final InputStream inputStream = getInputStreamFromPath(path);

        final String mimeType = getServletContext().getMimeType(path);
        response.setContentType(mimeType);

        if (inputStream != null) {
            response.setHeader("Cache-Control", path.contains(".cache.") ? IMMUTABLE : REVALIDATE);
            try (inputStream; OutputStream out = response.getOutputStream()) {
                inputStream.transferTo(out);
            }
        } else {
            if (path.equals(INDEX_URL)) {
                response.setHeader("Cache-Control", REVALIDATE);
                if (!configuration.getSupportedLocales().isEmpty()) response.setHeader("Vary", "Accept-Language, Cookie");
                if (configuration.isContentSecurityPolicyEnabled())
                    response.setHeader("Content-Security-Policy", buildContentSecurityPolicy());
                try (OutputStream out = response.getOutputStream()) {
                    out.write(buildIndexHTML(request).getBytes(StandardCharsets.UTF_8));
                }
            } else {
                // a client asking for a path the application does not have: not a server fault
                log.debug("Resource not found: {}", path);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    public InputStream getInputStreamFromPath(final String path) {
        final URL url = getResourceURL(path);
        if (url == null) return null;
        try {
            return url.openStream();
        } catch (final IOException e) {
            log.error("Cannot open resource {}", path, e);
            return null;
        }
    }

    /**
     * The served file: from the servlet context first, then from the jars, of a type the configuration serves (web
     * files by default: never a class, a configuration or a key store). Null when there is none.
     */
    protected URL getResourceURL(final String path) {
        if (!isSafeResourcePath(path)) return null;
        try {
            final URL url = getServletContext().getResource(path);
            if (url != null) return url;
        } catch (final java.net.MalformedURLException e) {
            return null;
        }
        if (!isServedFromClasspath(path)) return null;
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final String jarPath = path.substring(1);
        final URL url = classLoader.getResource(jarPath);
        return url != null || childClassLoader == null ? url : childClassLoader.getResource(jarPath);
    }

    private boolean isServedFromClasspath(final String path) {
        final int slash = path.lastIndexOf('/');
        final int dot = path.lastIndexOf('.');
        if (dot <= slash) return false;
        final String extension = path.substring(dot + 1).toLowerCase(Locale.ROOT);
        final ApplicationConfiguration config = configuration != null ? configuration : DEFAULT_CONFIGURATION;
        final Set<String> served = config.getServedResourceExtensions();
        return (served != null ? served : DEFAULT_CONFIGURATION.getServedResourceExtensions()).contains(extension);
    }

    /** The served types when the servlet has no configuration of its own. */
    private static final ApplicationConfiguration DEFAULT_CONFIGURATION = new ApplicationConfiguration();

    /**
     * Modification time of the requested file, for conditional GETs ({@link HttpServlet#service} answers 304
     * to an up-to-date {@code If-Modified-Since}). -1 for the bootstrap page, generated per request.
     */
    @Override
    protected long getLastModified(final HttpServletRequest request) {
        final String path = getPath(request);
        if (path == null || path.isEmpty() || path.equals("/") || path.equals(INDEX_URL) || !isSafeResourcePath(path)) {
            return -1;
        }
        final URL url = getResourceURL(path);
        if (url == null) return -1;
        try {
            final long time = url.openConnection().getLastModified();
            // HTTP dates have a one-second precision: a file time in ms would never match If-Modified-Since
            return time > 0 ? time / 1000 * 1000 : -1;
        } catch (final IOException e) {
            return -1;
        }
    }

    protected String buildIndexHTML(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();

        sb.append("<!doctype html>").append(NEW_LINE);
        sb.append(htmlTag(request)).append(NEW_LINE);
        sb.append("<head>").append(NEW_LINE);
        sb.append(addHeader(request));
        sb.append("</head>").append(NEW_LINE);
        sb.append("<body>").append(NEW_LINE);
        sb.append(addToBody(request));
        sb.append("</body>").append(NEW_LINE);
        sb.append("</html>").append(NEW_LINE);

        return sb.toString();
    }

    /**
     * {@code <html>}, with the {@code lang} and {@code dir} of the visitor's locale when the application has supported
     * locales (docs/decisions/0022-locale.md): the first paint is already in the right direction. The negotiation is
     * the context's, from the same cookie and header.
     */
    protected String htmlTag(final HttpServletRequest request) {
        // The theme's stylesheet applies where the class pony-theme is: on the page's root, to the whole page
        final String theme = configuration.getTheme() == null ? "" : " class=\"pony-theme\"";
        final List<Locale> supported = configuration.getSupportedLocales();
        if (supported.isEmpty()) return "<html" + theme + ">";
        String cookie = null;
        if (request.getCookies() != null) {
            for (final jakarta.servlet.http.Cookie c : request.getCookies()) {
                if (c.getName().equals(Locales.COOKIE)) cookie = c.getValue();
            }
        }
        final Locale locale = Locales.negotiate(supported, cookie, request.getHeader("Accept-Language"));
        return "<html" + theme + " lang=\"" + locale.toLanguageTag() + "\" dir=\"" + (Locales.isRightToLeft(locale) ? "rtl" : "ltr") + "\">";
    }

    /**
     * What a {@code <pony-app>} element of another page loads before it connects, as the bootstrap page loads it
     * (docs/decisions/0008-embedded-application.md): the stylesheets, the scripts with ponysdk.js first, the shared
     * web-component sheets, paths relative to the application; and whether transparent reconnection is on.
     */
    protected String buildManifest(final HttpServletRequest request) {
        final JsonArrayBuilder styles = Json.createArrayBuilder();
        stylesheets().forEach((id, href) -> styles.add(Json.createObjectBuilder().add("id", id).add("href", href)));
        final JsonArrayBuilder scripts = Json.createArrayBuilder().add(rootPath + "script/ponysdk.js");
        final Set<String> configuredScripts = configuration.getJavascript();
        if (configuredScripts != null) configuredScripts.forEach(scripts::add);
        final JsonArrayBuilder sheets = Json.createArrayBuilder();
        final Set<String> configuredSheets = configuration.getWcSharedSheets();
        if (configuredSheets != null) configuredSheets.forEach(sheets::add);
        final JsonObjectBuilder manifest = Json.createObjectBuilder()
            .add("styles", styles)
            .add("scripts", scripts)
            .add("sharedSheets", sheets)
            .add("reconnection", configuration.getReconnectionTimeoutMs() > 0)
            // The terminal that runs it, and the contract of its mountPonyApp: an element another PonySDK version
            // defined hands itself to it (ADR-0009, decision 10)
            .add("terminal", rootPath + "ponyterminal/terminal.js")
            .add("mount", 1);
        // The legacy layer, which the element runs on its container (ADR-0009); null without
        if (configuration.isLegacyClassNames()) manifest.add("legacy", rootPath + "ponyterminal/legacy.js");
        else manifest.addNull("legacy");
        return manifest.build().toString();
    }

    protected String addHeader(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();

        sb.append(addTitle(request));
        sb.append(addMeta(request));
        sb.append(addStyle(request));
        sb.append(addScript(request));

        return sb.toString();
    }

    protected String addTitle(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(TITLE_PATTERN, configuration.getApplicationName())).append(NEW_LINE);

        return sb.toString();
    }

    protected String addMeta(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(META_PATTERN, "http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"")).append(NEW_LINE);

        final Set<String> metas = configuration.getMeta();
        if (metas != null) {
            for (final String m : metas) {
                sb.append(String.format(META_PATTERN, m)).append(NEW_LINE);
            }
        }

        // The pages allowed to embed the application: a window or a frame of an embedded terminal accepts them as its
        // parent, and no other page (ADR-0009, decision 6)
        final Set<String> parents = configuration.getWsAllowedOrigins();
        if (parents != null && !parents.isEmpty()) {
            sb.append(String.format(META_PATTERN, "name=\"pony-parent-origins\" content=\""
                    + escapeAttribute(String.join(" ", new java.util.TreeSet<>(parents))) + "\"")).append(NEW_LINE);
        }

        // Read by the terminal and ponysdk.js: data, not inline scripts, which the Content-Security-Policy would have
        // to allow (ADR-0015)
        if (configuration.getReconnectionTimeoutMs() > 0) {
            sb.append(String.format(META_PATTERN, "name=\"pony-reconnection\" content=\"true\"")).append(NEW_LINE);
        }
        final Set<String> wcSheets = configuration.getWcSharedSheets();
        if (wcSheets != null && !wcSheets.isEmpty()) {
            sb.append(String.format(META_PATTERN, "name=\"pony-wc-shared-sheets\" content=\""
                    + escapeAttribute(String.join(" ", wcSheets)) + "\"")).append(NEW_LINE);
        }

        return sb.toString();
    }

    /**
     * The policy of the page PonySDK builds (ADR-0015): its one inline script by hash, the script origins the
     * configuration names, no eval, frames by the pages that embed it. An
     * application that adds inline scripts to the page overrides this to allow them.
     */
    protected String buildContentSecurityPolicy() {
        final Set<String> scripts = new LinkedHashSet<>();
        scripts.add("'self'");
        scripts.add(EARLY_SOCKET_HASH);
        final Set<String> configured = configuration.getJavascript();
        if (configured != null) configured.forEach(url -> {
            final String origin = originOf(url);
            if (origin != null) scripts.add(origin);
        });
        scripts.addAll(configuration.getScriptSources());

        final Set<String> ancestors = new LinkedHashSet<>();
        ancestors.add("'self'");
        final Set<String> parents = configuration.getWsAllowedOrigins();
        if (parents != null) new TreeSet<>(parents).forEach(parent -> {
            final String origin = originOf(parent);
            if (origin != null) ancestors.add(origin);
        });

        return "script-src " + String.join(" ", scripts) + "; object-src 'none'; base-uri 'self'; frame-ancestors "
                + String.join(" ", ancestors);
    }

    /**
     * The origin of an absolute or scheme-relative URL; null for a path of the application, and for an IPv6 address,
     * which a CSP source expression cannot name (the browser would reject it and log an error on each page).
     */
    private static String originOf(final String url) {
        try {
            final URI uri = new URI(url);
            if (uri.getHost() == null) return null;
            if (uri.getHost().startsWith("[")) {
                log.warn("An IPv6 address cannot appear in a Content-Security-Policy, left out: {}", url);
                return null;
            }
            final String authority = uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
            return uri.getScheme() == null ? authority : uri.getScheme() + "://" + authority;
        } catch (final URISyntaxException e) {
            log.warn("Not a URL, left out of the Content-Security-Policy: {}", url);
            return null;
        }
    }

    private static String escapeAttribute(final String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;");
    }

    protected String addStyle(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();

        for (final Entry<String, String> style : stylesheets().entrySet()) {
            final String id = style.getKey();
            final String url = style.getValue();
            final String contentType = getServletContext().getMimeType(url);
            sb.append(String.format(STYLE_PATTERN, id, contentType, url)).append(NEW_LINE);
        }

        return sb.toString();
    }

    /** The stylesheets of the page: the theme when asked for, then the application's, which win over it. */
    private Map<String, String> stylesheets() {
        final Map<String, String> configured = configuration.getStyle();
        if (configuration.getTheme() == null) return configured == null ? Map.of() : configured;
        final Map<String, String> all = new LinkedHashMap<>();
        all.put(THEME_ID, rootPath + "style/pony-theme.css");
        if (configured != null) all.putAll(configured);
        return all;
    }

    /**
     * Opens the WebSocket while the terminal script downloads, so that the server builds the page meanwhile; the
     * terminal adopts the socket and replays what it received (Socket.connect). Same URL as the terminal builds (its
     * history token is the decoded hash); main window only: a child window ("1" parameter) attaches to
     * its parent's context instead.
     */
    static final String EARLY_SOCKET_SOURCE = "(function(){"
            + "if(!window.WebSocket||new URLSearchParams(location.search).has('" + ClientToServerModel.UI_CONTEXT_ID.toStringValue() + "'))return;"
            + "var l=location,t=decodeURI(l.hash.substring(1).replace(/%23/g,'#')),d=l.pathname.replace(/[^/]*$/,''),"
            + "k='ponyMovedState:'+l.host+l.pathname+l.search+l.hash,m='',"
            + "e={opened:false,messages:[],times:[],closeEvent:null};"
            // the state a move left for this page (ADR-0018), taken once; the terminal reads the same key
            + "try{var s=sessionStorage.getItem(k);sessionStorage.removeItem(k);if(s!==null)m='&"
            + ClientToServerModel.MOVED_STATE.toStringValue() + "='+s;}catch(x){}"
            + "e.socket=new WebSocket(l.protocol.replace('http','ws')+'//'+l.host+d+'" + MappingPath.WEBSOCKET + "?"
            + ClientToServerModel.TYPE_HISTORY.toStringValue() + "='+t+m+'&" + ClientToServerModel.PROTOCOL_VERSION.toStringValue()
            + "=" + ProtocolVersion.CURRENT + "'+l.search.replace('?','&'));"
            + "e.socket.binaryType='arraybuffer';"
            + "e.socket.onopen=function(){e.opened=true;};"
            + "e.socket.onmessage=function(m){e.messages.push(m.data);e.times.push(m.timeStamp);};"
            + "e.socket.onclose=function(c){e.closeEvent=c;};"
            + "window.ponyEarlySocket=e;})();";
    static final String EARLY_SOCKET_SCRIPT = "<script type=\"text/javascript\">" + EARLY_SOCKET_SOURCE + "</script>";
    /**
     * The source expression that allows it in the Content-Security-Policy: the SHA-256 of EARLY_SOCKET_SOURCE, which is
     * a constant. BootstrapServletTest recomputes it: a change of the source fails the build until this is updated.
     */
    static final String EARLY_SOCKET_HASH = "'sha256-OhNJKbDiayJAmhrxR51A/GfuQ8nw82ySywAFDh3OmKI='";

    protected String addScript(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();
        sb.append(EARLY_SOCKET_SCRIPT).append(NEW_LINE);

        // Deferred: the terminal starts once the page is parsed, after ponysdk.js and the application scripts; the
        // legacy layer, when asked for, runs just before it
        sb.append(String.format(SCRIPT_PATTERN, rootPath + "script/ponysdk.js")).append(NEW_LINE);
        if (configuration.isLegacyClassNames()) {
            sb.append(String.format(DEFERRED_SCRIPT_PATTERN, rootPath + "ponyterminal/legacy.js")).append(NEW_LINE);
        }
        // A module: it loads its rarely shown widgets on demand (docs/decisions/0004-terminal-chunks.md)
        sb.append(String.format(MODULE_SCRIPT_PATTERN, rootPath + "ponyterminal/terminal.js")).append(NEW_LINE);

        final Set<String> scripts = configuration.getJavascript();
        if (scripts != null && !scripts.isEmpty()) {
            for (final String script : scripts) {
                sb.append(String.format(SCRIPT_PATTERN, script)).append(NEW_LINE);
            }
        }

        return sb.toString();
    }

    protected String addToBody(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();

        sb.append(addLoading(request));
        sb.append(addNoScript(request));

        return sb.toString();
    }

    protected String addLoading(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<div id=\"loading\">Loading ");
        sb.append(configuration.getApplicationName());
        sb.append("...</div>").append(NEW_LINE);
        return sb.toString();
    }

    protected String addNoScript(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();

        sb.append("<noscript>").append(NEW_LINE);
        sb.append(
                        "<div style=\"width: 22em; position: absolute; left: 50%; margin-left: -11em; color: red; background-color: white; border: 1px solid red; padding: 4px;\">")
                .append(NEW_LINE);
        sb.append("Your web browser must have JavaScript enabled").append(NEW_LINE);
        sb.append("in order for this application to display correctly.").append(NEW_LINE);
        sb.append("</div>").append(NEW_LINE);
        sb.append("</noscript>").append(NEW_LINE);

        return sb.toString();
    }

    public void setConfiguration(final ApplicationConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
    }
}
