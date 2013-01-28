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

package com.ponysdk.core.servlet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.SystemProperty;

public class BootstrapServlet extends HttpServlet {

    private static final long serialVersionUID = 6993633431616272739L;

    private static final Logger log = LoggerFactory.getLogger(BootstrapServlet.class);

    private static final int BUFFER_SIZE = 4096;

    private static byte[] indexPage;

    private String applicationName = "";

    private final List<String> stylesheets = new ArrayList<String>();
    private final List<String> javascripts = new ArrayList<String>();

    public BootstrapServlet() {}

    @Override
    public void init() throws ServletException {
        super.init();
        applicationName = System.getProperty(SystemProperty.APPLICATION_NAME, applicationName);

        final String styles = System.getProperty(SystemProperty.STYLESHEETS);
        if (styles != null && !styles.isEmpty()) {
            stylesheets.addAll(Arrays.asList(styles.trim().split(";")));
        }
        final String scripts = System.getProperty(SystemProperty.JAVASCRIPTS);
        if (scripts != null && !scripts.isEmpty()) {
            javascripts.addAll(Arrays.asList(scripts.trim().split(";")));
        }
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        handlePonyResource(request, response);
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        handlePonyResource(request, response);
    }

    private void handlePonyResource(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        try {
            final String contextPath = request.getContextPath();
            final String requestURI = request.getRequestURI();
            final String extraPathInfo = requestURI.replaceFirst(contextPath, "");

            if (extraPathInfo == null || extraPathInfo.isEmpty() || extraPathInfo.equals("/")) {
                log.info("Loading initial webpage ...");
                response.setContentType("text/html");
                handleRequest(request, response, "/index.html");
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

    private void handleRequest(final HttpServletRequest request, final HttpServletResponse response, final String path) throws ServletException, IOException {
        // Try to load from webapp context
        InputStream inputStream = getServletContext().getResourceAsStream(path);
        String type;

        if (inputStream == null) {
            // Try to load from jar
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            final String jarPath = path.substring(1, path.length());
            inputStream = classLoader.getResourceAsStream(jarPath);
            if (inputStream == null) {
                if (path.equals("/index.html")) {
                    inputStream = new ByteArrayInputStream(generateIndexPage());
                } else {
                    log.error("Failed to load resource: " + request.getPathInfo());
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }

            type = new MimetypesFileTypeMap().getContentType(new File(jarPath));
        } else {
            type = new MimetypesFileTypeMap().getContentType(new File(path));
        }

        response.setContentType(type);
        copy(inputStream, response.getOutputStream());
    }

    public static int copy(final InputStream in, final OutputStream out) throws IOException {
        try {
            int byteCount = 0;
            final byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = -1;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                byteCount += bytesRead;
            }
            out.flush();
            return byteCount;
        } finally {
            try {
                in.close();
            } catch (final IOException ex) {}
            try {
                out.close();
            } catch (final IOException ex) {}
        }
    }

    private byte[] generateIndexPage() {
        final StringBuilder builder = new StringBuilder();

        builder.append("<!doctype html>");
        builder.append("<html>");
        builder.append("<head>");
        builder.append("    <!-- Powered by PonySDK http://www.ponysdk.com -->");
        builder.append("    <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">");
        builder.append("    <meta name=\"apple-mobile-web-app-capable\" content=\"yes\">");
        builder.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\">");
        builder.append("    <title>" + applicationName + "</title>");
        builder.append("    <script type=\"text/javascript\" src=\"ponyterminal/ponyterminal.nocache.js\"></script>");

        for (final String style : stylesheets) {
            final String contentType = new MimetypesFileTypeMap().getContentType(style);
            if (!contentType.equals("text/css")) builder.append("    <link rel=\"stylesheet/less\" type=\"" + contentType + "\" href=\"" + style + "\">");
            else builder.append("    <link rel=\"stylesheet\" type=\"" + contentType + "\" href=\"" + style + "\">");
        }

        for (final String script : javascripts) {
            builder.append("    <script type=\"text/javascript\" src=\"" + script + "\"></script>");
        }

        // builder.append("    <script src=\"script/less.js\" type=\"text/javascript\"></script>");

        builder.append("</head>");
        builder.append("<body>");
        builder.append("    <iframe src=\"javascript:''\" id=\"__gwt_historyFrame\" tabIndex='-1' style=\"position:absolute;width:0;height:0;border:0\"></iframe>");
        builder.append("    <div id=\"loading\">Loading " + applicationName + "...</div>");
        builder.append("    <noscript>");
        builder.append("        <div style=\"width: 22em; position: absolute; left: 50%; margin-left: -11em; color: red; background-color: white; border: 1px solid red; padding: 4px; font-family: sans-serif\">");
        builder.append("            Your web browser must have JavaScript enabled");
        builder.append("            in order for this application to display correctly.");
        builder.append("        </div>");
        builder.append("    </noscript>");
        builder.append("</body>");
        builder.append("</html>");

        indexPage = builder.toString().getBytes();
        return indexPage;
    }

    public void addStylesheet(final String stylesheetPath) {
        stylesheets.add(stylesheetPath);
    }

    public void addJavascript(final String javascriptPath) {
        javascripts.add(javascriptPath);
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }
}