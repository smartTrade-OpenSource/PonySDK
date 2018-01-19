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

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.ApplicationManagerOption;

public class BootstrapServlet extends HttpServlet {

    private static final String INDEX_URL = "/index.html";

    private static final Logger log = LoggerFactory.getLogger(BootstrapServlet.class);

    private final MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();

    protected ApplicationManagerOption application;

    private ClassLoader childClassLoader;

    public BootstrapServlet() {
    }

    public BootstrapServlet(final ClassLoader classLoader) {
        this.childClassLoader = classLoader;
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        handlePonyResource(request, response);
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        handlePonyResource(request, response);
    }

    protected void handlePonyResource(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        try {
            final String extraPathInfo = getPath(request);

            if (extraPathInfo == null || extraPathInfo.isEmpty() || extraPathInfo.equals("/")) {
                handleRequest(request, response, INDEX_URL);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Loading resource: " + extraPathInfo);
                }
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
        request.getSession();

        final InputStream inputStream = getInputStreamFromPath(path);

        final String mimeType = fileTypeMap.getContentType(path);
        response.setContentType(mimeType);

        if (inputStream != null) {
            ReadableByteChannel inputChannel = null;
            WritableByteChannel outputChannel = null;
            try {
                inputChannel = Channels.newChannel(inputStream);
                outputChannel = Channels.newChannel(response.getOutputStream());

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
            } finally {
                if (inputChannel != null) inputChannel.close();
                if (outputChannel != null) outputChannel.close();
            }
        } else {
            if (path.equals(INDEX_URL)) {
                final WritableByteChannel outputChannel = Channels.newChannel(response.getOutputStream());
                final ByteBuffer buffer = ByteBuffer.wrap(buildIndexHTML(request).getBytes(Charset.forName("UTF8")));
                outputChannel.write(buffer);
            } else {
                log.error("Failed to load resource: " + request.getPathInfo());
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
            final String jarPath = path.substring(1, path.length());
            inputStream = classLoader.getResourceAsStream(jarPath);
            if (inputStream == null && childClassLoader != null) inputStream = childClassLoader.getResourceAsStream(jarPath);
        }
        return inputStream;
    }

    protected String buildIndexHTML(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();

        sb.append("<!doctype html>\n");
        sb.append("<html>\n");
        sb.append("<head>\n");
        sb.append("<title>").append(application.getApplicationName()).append("</title>\n");
        sb.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">\n");

        final Set<String> metas = application.getMeta();
        if (metas != null) {
            for (final String m : metas) {
                sb.append("<meta ").append(m).append(">\n");
            }
        }
        sb.append(addToMeta(request));

        final Map<String, String> styles = application.getStyle();
        if (styles != null && !styles.isEmpty()) {
            for (final Entry<String, String> style : styles.entrySet()) {
                final String id = style.getKey();
                final String url = style.getValue();
                final String contentType = fileTypeMap.getContentType(url);
                sb.append("<link id=\"").append(id).append("\"  rel=\"stylesheet");
                if (!contentType.equals("text/css")) sb.append("/less");
                sb.append("\" type=\"").append(contentType).append("\" href=\"").append(url).append("\">\n");
            }
        }
        sb.append(addToStyle(request));

        String ponyTerminalJsFileName;
        if (application.isDebugMode()) ponyTerminalJsFileName = "ponyterminaldebug/ponyterminaldebug.nocache.js";
        else ponyTerminalJsFileName = "ponyterminal/ponyterminal.nocache.js";
        sb.append("<script type=\"text/javascript\" src=\"").append(ponyTerminalJsFileName).append("\"></script>\n");
        sb.append("<script type=\"text/javascript\" src=\"script/ponysdk.js\"></script>\n");

        final Set<String> scripts = application.getJavascript();
        if (scripts != null && !scripts.isEmpty()) {
            for (final String script : scripts) {
                sb.append("<script type=\"text/javascript\" src=\"").append(script).append("\"></script>\n");
            }
        }
        sb.append(addToScript(request));

        sb.append(addToHeader(request));
        sb.append("</head>\n");
        sb.append("<body>\n");
        sb.append(addHistoryIFrame(request));
        sb.append(addLoading(request));
        sb.append(addNoScript(request));
        sb.append(addToBody(request));
        sb.append("</body>\n");
        sb.append("</html>\n");

        return sb.toString();
    }

    protected String addHistoryIFrame(final HttpServletRequest request) {
        return "<iframe src=\"javascript:''\" id=\"__gwt_historyFrame\" tabIndex='-1' style=\"position:absolute;width:0;height:0;border:0\"></iframe>\n";
    }

    protected String addLoading(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<div id=\"loading\">Loading ");
        sb.append(application.getApplicationName());
        sb.append("...</div>\n");
        return sb.toString();
    }

    protected String addNoScript(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<noscript>\n");
        sb.append(
            "<div style=\"width: 22em; position: absolute; left: 50%; margin-left: -11em; color: red; background-color: white; border: 1px solid red; padding: 4px;\">\n");
        sb.append("Your web browser must have JavaScript enabled\n");
        sb.append("in order for this application to display correctly.\n");
        sb.append("</div>\n");
        sb.append("</noscript>\n");
        return sb.toString();
    }

    protected String addToMeta(final HttpServletRequest request) {
        return "\n";
    }

    protected String addToStyle(final HttpServletRequest request) {
        return "\n";
    }

    protected String addToScript(final HttpServletRequest request) {
        return "\n";
    }

    protected String addToHeader(final HttpServletRequest request) {
        return "\n";
    }

    protected String addToBody(final HttpServletRequest request) {
        return "\n";
    }

    public void setApplication(final ApplicationManagerOption application) {
        this.application = application;
    }
}
