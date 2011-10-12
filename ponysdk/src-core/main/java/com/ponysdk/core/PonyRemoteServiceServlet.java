/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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
package com.ponysdk.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;

/**
 * Same as RemoteServiceServlet, but get the policy file from current classloader
 */
public class PonyRemoteServiceServlet extends RemoteServiceServlet {

    static SerializationPolicy loadSerializationPolicy(HttpServlet servlet, HttpServletRequest request, String moduleBaseURL, String strongName) {
        // The request can tell you the path of the web app relative to the
        // container root.
        final String contextPath = request.getContextPath();

        String modulePath = null;
        if (moduleBaseURL != null) {
            try {
                modulePath = new URL(moduleBaseURL).getPath();
            } catch (final MalformedURLException ex) {
                // log the information, we will default
                servlet.log("Malformed moduleBaseURL: " + moduleBaseURL, ex);
            }
        }

        SerializationPolicy serializationPolicy = null;

        /*
         * Check that the module path must be in the same web app as the servlet itself. If you need to implement a scheme different than this, override this method.
         */
        if (modulePath == null || !modulePath.startsWith(contextPath)) {
            final String message = "ERROR: The module path requested, " + modulePath + ", is not in the same web application as this servlet, " + contextPath
                    + ".  Your module may not be properly configured or your client and server code maybe out of date.";
            servlet.log(message);
        } else {
            // Strip off the context path from the module base URL. It should be a
            // strict prefix.
            final String contextRelativePath = modulePath.substring(contextPath.length());

            final String serializationPolicyFilePath = SerializationPolicyLoader.getSerializationPolicyFileName(contextRelativePath + strongName);

            // Open the RPC resource file and read its contents.

            // Try to load from webapp context
            InputStream is = servlet.getServletContext().getResourceAsStream(serializationPolicyFilePath);
            if (is == null) {
                // Try to load from jar
                final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                final String jarPath = serializationPolicyFilePath.substring(1, serializationPolicyFilePath.length());
                is = classLoader.getResourceAsStream(jarPath);
            }

            try {
                if (is != null) {
                    try {
                        serializationPolicy = SerializationPolicyLoader.loadFromStream(is, null);
                    } catch (final ParseException e) {
                        servlet.log("ERROR: Failed to parse the policy file '" + serializationPolicyFilePath + "'", e);
                    } catch (final IOException e) {
                        servlet.log("ERROR: Could not read the policy file '" + serializationPolicyFilePath + "'", e);
                    }
                } else {
                    final String message = "ERROR: The serialization policy file '" + serializationPolicyFilePath + "' was not found; did you forget to include it in this deployment?";
                    servlet.log(message);
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (final IOException e) {
                        // Ignore this error
                    }
                }
            }
        }

        return serializationPolicy;
    }

    @Override
    protected SerializationPolicy doGetSerializationPolicy(HttpServletRequest request, String moduleBaseURL, String strongName) {
        return loadSerializationPolicy(this, request, moduleBaseURL, strongName);
    }

}
