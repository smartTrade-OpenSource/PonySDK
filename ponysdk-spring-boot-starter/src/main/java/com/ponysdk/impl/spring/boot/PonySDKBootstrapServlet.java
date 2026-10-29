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

package com.ponysdk.impl.spring.boot;

import java.io.IOException;

import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.servlet.BootstrapServlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@link BootstrapServlet} mapped under {@code /<application context name>/*} of Boot's servlet context: the resources are
 * those after that path, as when PonySDKServer gives the application a context of its own. {@code /<name>} without its
 * slash goes to {@code /<name>/}, from where the page's relative URLs resolve.
 */
final class PonySDKBootstrapServlet extends BootstrapServlet {

    PonySDKBootstrapServlet(final ApplicationConfiguration configuration) {
        setConfiguration(configuration);
    }

    @Override
    protected String getPath(final HttpServletRequest request) {
        final String path = request.getPathInfo(); // what the mapping /<name>/* leaves, decoded; null for /<name>
        return path == null ? "" : path;
    }

    @Override
    protected void handlePonyResource(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        if (getPath(request).isEmpty()) {
            response.sendRedirect(request.getContextPath() + request.getServletPath() + "/");
            return;
        }
        super.handlePonyResource(request, response);
    }
}
