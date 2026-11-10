/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.impl.main;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.Callback;

/** Redirects a context path without its trailing slash with a 302 and an absolute Location, as Jetty 9 did. */
final class ContextPathRedirectHandler extends Handler.Wrapper {

    ContextPathRedirectHandler(final Handler handler) {
        super(handler);
    }

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {
        final HttpURI uri = request.getHttpURI();
        if (isContextPath(uri.getDecodedPath())) {
            final String query = uri.getQuery();
            Response.sendRedirect(request, response, callback, HttpStatus.FOUND_302,
                uri.getPath() + '/' + (query == null ? "" : "?" + query), true);
            return true;
        }
        return super.handle(request, response, callback);
    }

    private boolean isContextPath(final String path) {
        return path.length() > 1 && !path.endsWith("/")
                && getServer().getDescendants(ContextHandler.class).stream().anyMatch(context -> path.equals(context.getContextPath()));
    }
}
