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
import com.ponysdk.core.server.application.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class BootstrapServlet extends HttpServlet {

    private static final String INDEX_URL = "/index.html";
    /** GWT names its compiled files after their content (*.cache.*): they never change under the same name. */
    private static final String IMMUTABLE = "public, max-age=31536000, immutable";
    /** Everything else may change under the same name: the browser revalidates (304 when unmodified). */
    private static final String REVALIDATE = "no-cache";
    protected static final String NEW_LINE = "\n";

    protected static final String TITLE_PATTERN = "<title>%s</title>";
    protected static final String META_PATTERN = "<meta %s>";
    protected static final String STYLE_PATTERN = "<link id=\"%s\" rel=\"stylesheet\" type=\"%s\" href=\"%s\"/>";
    protected static final String SCRIPT_PATTERN = "<script type=\"text/javascript\" src=\"%s\"></script>";
    protected static final String DEFERRED_SCRIPT_PATTERN = "<script type=\"text/javascript\" src=\"%s\" defer></script>";

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
            log.error("Cannot process the request", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
        if (path.indexOf('\\') >= 0 || path.indexOf('\0') >= 0) return false;
        for (final String segment : path.split("/")) {
            if ("..".equals(segment)) return false;
        }
        return true;
    }

    protected void handleRequest(final HttpServletRequest request, final HttpServletResponse response, final String path)
            throws IOException {
        // Reject path traversal before touching the servlet context / classpath.
        if (!isSafeResourcePath(path)) {
            log.warn("Rejected unsafe resource path");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Force session creation if there is no session
        // TODO Verify if needed
        request.getSession();

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
                try (OutputStream out = response.getOutputStream()) {
                    out.write(buildIndexHTML(request).getBytes(StandardCharsets.UTF_8));
                }
            } else {
                log.error("Failed to load resource: {}", request.getPathInfo());
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

    /** The served file: from the servlet context first, then from the jars. Null when there is none. */
    protected URL getResourceURL(final String path) {
        try {
            final URL url = getServletContext().getResource(path);
            if (url != null) return url;
        } catch (final java.net.MalformedURLException e) {
            return null;
        }
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final String jarPath = path.substring(1);
        final URL url = classLoader.getResource(jarPath);
        return url != null || childClassLoader == null ? url : childClassLoader.getResource(jarPath);
    }

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
        sb.append("<html>").append(NEW_LINE);
        sb.append("<head>").append(NEW_LINE);
        sb.append(addHeader(request));
        sb.append("</head>").append(NEW_LINE);
        sb.append("<body>").append(NEW_LINE);
        sb.append(addToBody(request));
        sb.append("</body>").append(NEW_LINE);
        sb.append("</html>").append(NEW_LINE);

        return sb.toString();
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

        return sb.toString();
    }

    protected String addStyle(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();

        final Map<String, String> styles = configuration.getStyle();
        if (styles != null && !styles.isEmpty()) {
            for (final Entry<String, String> style : styles.entrySet()) {
                final String id = style.getKey();
                final String url = style.getValue();
                final String contentType = getServletContext().getMimeType(url);
                sb.append(String.format(STYLE_PATTERN, id, contentType, url)).append(NEW_LINE);
            }
        }

        return sb.toString();
    }

    /**
     * Opens the WebSocket while the terminal script downloads, so that the server builds the page meanwhile; the
     * terminal adopts the socket and replays what it received (WebSocketClient.connect). Same URL as the terminal
     * builds (History.getToken() is the decoded hash); main window only: a child window ("1" parameter) attaches to
     * its parent's context instead.
     */
    static final String EARLY_SOCKET_SCRIPT = "<script type=\"text/javascript\">(function(){"
            + "if(!window.WebSocket||new URLSearchParams(location.search).has('" + ClientToServerModel.UI_CONTEXT_ID.toStringValue() + "'))return;"
            + "var l=location,t=decodeURI(l.hash.substring(1).replace(/%23/g,'#')),"
            + "e={opened:false,messages:[],closeEvent:null};"
            + "e.socket=new WebSocket(l.protocol.replace('http','ws')+'//'+l.host+l.pathname+'" + MappingPath.WEBSOCKET + "?"
            + ClientToServerModel.TYPE_HISTORY.toStringValue() + "='+t+'&" + ClientToServerModel.PROTOCOL_VERSION.toStringValue()
            + "=" + ProtocolVersion.CURRENT + "'+l.search.replace('?','&'));"
            + "e.socket.binaryType='arraybuffer';"
            + "e.socket.onopen=function(){e.opened=true;};"
            + "e.socket.onmessage=function(m){e.messages.push(m.data);};"
            + "e.socket.onclose=function(c){e.closeEvent=c;};"
            + "window.ponyEarlySocket=e;})();</script>";

    protected String addScript(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();
        sb.append(EARLY_SOCKET_SCRIPT).append(NEW_LINE);

        // Enable transparent WebSocket reconnection on the client side when the server supports it
        if (configuration != null && configuration.getReconnectionTimeoutMs() > 0) {
            sb.append("<script type=\"text/javascript\">window.ponyReconnectMode = true;</script>").append(NEW_LINE);
        }

        if (isTypeScriptTerminal()) {
            // Deferred: it starts once the page is parsed, after ponysdk.js and the application scripts, as the GWT
            // terminal does when its permutation arrives
            sb.append(String.format(SCRIPT_PATTERN, rootPath + "script/ponysdk.js")).append(NEW_LINE);
            sb.append(String.format(DEFERRED_SCRIPT_PATTERN, rootPath + "ponyterminal/terminal.js")).append(NEW_LINE);
        } else {
            sb.append(String.format(SCRIPT_PATTERN, rootPath + "ponyterminal/ponyterminal.nocache.js")).append(NEW_LINE);
            sb.append(String.format(SCRIPT_PATTERN, rootPath + "script/ponysdk.js")).append(NEW_LINE);
        }

        // Inject shared WC stylesheets as an inline script so they are registered
        // before application scripts define their web components.
        final Set<String> wcSheets = configuration.getWcSharedSheets();
        if (wcSheets != null && !wcSheets.isEmpty()) {
            sb.append("<script type=\"text/javascript\">").append(NEW_LINE);
            sb.append("document.onPonyLoaded(function() {").append(NEW_LINE);
            for (final String sheet : wcSheets) {
                sb.append("  pony.wc.registerSharedSheet('").append(sheet).append("');").append(NEW_LINE);
            }
            sb.append("});").append(NEW_LINE);
            sb.append("</script>").append(NEW_LINE);
        }

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

        sb.append(addHistoryIFrame(request));
        sb.append(addLoading(request));
        sb.append(addNoScript(request));

        return sb.toString();
    }

    /** GWT's history support needs this frame; the TypeScript terminal does not. */
    protected String addHistoryIFrame(final HttpServletRequest request) {
        if (isTypeScriptTerminal()) return "";
        return "<iframe src=\"javascript:''\" id=\"__gwt_historyFrame\" tabIndex='-1' style=\"position:absolute;width:0;height:0;border:0\"></iframe>"
                + NEW_LINE;
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

    private boolean isTypeScriptTerminal() {
        return configuration != null && configuration.getTerminal() == Terminal.TYPESCRIPT;
    }

    public void setConfiguration(final ApplicationConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * @deprecated Use {@link #setConfiguration(ApplicationConfiguration)} directly
     */
    @Deprecated(forRemoval = true, since = "v2.8.1")
    public void setApplication(final ApplicationConfiguration application) {
        setConfiguration(application);
    }

    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
    }
}
