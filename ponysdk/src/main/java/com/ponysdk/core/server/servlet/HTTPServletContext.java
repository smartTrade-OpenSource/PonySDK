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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HTTPServletContext {

    private static final HTTPServletContext instance = new HTTPServletContext();

    private final ThreadLocal<HttpServletRequest> servletRequests = new ThreadLocal<>();
    private final ThreadLocal<HttpServletResponse> servletResponses = new ThreadLocal<>();

    private HTTPServletContext() {
    }

    public static HTTPServletContext get() {
        return instance;
    }

    void setContext(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse) {
        servletRequests.set(servletRequest);
        servletResponses.set(servletResponse);
    }

    void remove() {
        servletRequests.remove();
        servletResponses.remove();
    }

    public HttpServletRequest getRequest() {
        return servletRequests.get();
    }

    public HttpServletResponse getResponse() {
        return servletResponses.get();
    }

}
