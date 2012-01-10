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
 */package com.ponysdk.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PonyBootstrapServlet extends HttpServlet {

	private static final long serialVersionUID = 6993633431616272739L;

	private static final Logger log = LoggerFactory.getLogger(PonyBootstrapServlet.class);
    private static final int BUFFER_SIZE = 4096;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handlePonyResource(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handlePonyResource(request, response);
    }

    private void handlePonyResource(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
    }

    private void handleRequest(HttpServletRequest request, HttpServletResponse response, String path) throws ServletException, IOException {
        // Try to load from webapp context
        InputStream inputStream = getServletContext().getResourceAsStream(path);
        String type;

        if (inputStream == null) {
            // Try to load from jar
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            final String jarPath = path.substring(1, path.length());
            inputStream = classLoader.getResourceAsStream(jarPath);
            if (inputStream == null) {
                log.error("Failed to load resource: " + request.getPathInfo());
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            type = new MimetypesFileTypeMap().getContentType(new File(jarPath));
        } else {
        	type = new MimetypesFileTypeMap().getContentType(new File(path));
        }
        
        response.setContentType(type);
        copy(inputStream, response.getOutputStream());
    }
    
    public static int copy(InputStream in, OutputStream out) throws IOException {
        try {
            int byteCount = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = -1;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                byteCount += bytesRead;
            }
            out.flush();
            return byteCount;
        }
        finally {
            try {
                in.close();
            }
            catch (IOException ex) {
            }
            try {
                out.close();
            }
            catch (IOException ex) {
            }
        }
    }

}
