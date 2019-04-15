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

import com.ponysdk.core.server.application.ApplicationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class BootstrapServlet extends HttpServlet {

    private static final String INDEX_URL = "/index.html";
    protected static final String NEW_LINE = "\n";

    protected static final String TITLE_PATTERN = "<title>%s</title>";
    protected static final String META_PATTERN = "<meta %s>";
    protected static final String STYLE_PATTERN = "<link id=\"%s\" rel=\"stylesheet\" type=\"%s\" href=\"%s\"/>";
    protected static final String SCRIPT_PATTERN = "<script type=\"text/javascript\" src=\"%s\"></script>";

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
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        try {
            handlePonyResource(request, response);
        } catch (final IOException e) {
            log.error("Cannot stream request", e);
        }
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
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
        return request.getRequestURI().replaceFirst(contextPath, "");
    }

    protected void handleRequest(final HttpServletRequest request, final HttpServletResponse response, final String path)
            throws IOException {
        // Force session creation if there is no session
        // TODO Verify if needed
        request.getSession();

        final InputStream inputStream = getInputStreamFromPath(path);

        final String mimeType = getServletContext().getMimeType(path);
        response.setContentType(mimeType);

        if (inputStream != null) {
            try (ReadableByteChannel inputChannel = Channels.newChannel(inputStream);
                 WritableByteChannel outputChannel = Channels.newChannel(response.getOutputStream())) {
                final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
                while (inputChannel.read(buffer) != -1) {
                    buffer.flip();
                    outputChannel.write(buffer);
                    response.getOutputStream().flush();
                    buffer.compact();
                }
                buffer.flip();
                while (buffer.hasRemaining()) {
                    outputChannel.write(buffer);
                }
            }
        } else {
            if (path.equals(INDEX_URL)) {
                try (final WritableByteChannel outputChannel = Channels.newChannel(response.getOutputStream())) {
                    final ByteBuffer buffer = ByteBuffer.wrap(buildIndexHTML(request).getBytes(Charset.forName("UTF8")));
                    outputChannel.write(buffer);
                }
            } else {
                log.error("Failed to load resource: {}", request.getPathInfo());
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    public InputStream getInputStreamFromPath(final String path) {
        // Try to load from context
        InputStream inputStream = getServletContext().getResourceAsStream(path);

        if (inputStream == null) {
            // Try to load from jar
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            final String jarPath = path.substring(1);
            inputStream = classLoader.getResourceAsStream(jarPath);
            if (inputStream == null && childClassLoader != null)
                inputStream = childClassLoader.getResourceAsStream(jarPath);
        }
        return inputStream;
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

    protected String addScript(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();

        String ponyTerminalJsFileName;
        if (configuration.isDebugMode()) ponyTerminalJsFileName = "ponyterminaldebug/ponyterminaldebug.nocache.js";
        else ponyTerminalJsFileName = "ponyterminal/ponyterminal.nocache.js";

        sb.append(String.format(SCRIPT_PATTERN, rootPath + ponyTerminalJsFileName)).append(NEW_LINE);
        sb.append(String.format(SCRIPT_PATTERN, rootPath + "script/ponysdk.js")).append(NEW_LINE);

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

    protected String addHistoryIFrame(final HttpServletRequest request) {
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

    public void setConfiguration(final ApplicationConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
    }
}
