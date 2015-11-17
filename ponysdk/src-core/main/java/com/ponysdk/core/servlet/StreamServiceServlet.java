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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.Application;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.event.StreamHandler;
import com.ponysdk.ui.terminal.model.Model;

/**
 * The server side implementation of the RPC service.
 */
public class StreamServiceServlet extends HttpServlet {

    private static final long serialVersionUID = 5368766616550622126L;

    private static final Logger log = LoggerFactory.getLogger(StreamServiceServlet.class);

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        streamRequest(req, resp);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        streamRequest(req, resp);
    }

    private void streamRequest(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            final Application ponyApplicationSession = (Application) req.getSession().getAttribute(Application.class.getCanonicalName());
            final Integer ponySessionID = Integer.parseInt(req.getParameter("ponySessionID"));
            final UIContext ponySession = ponyApplicationSession.getUIContext(ponySessionID);
            final StreamHandler streamHandler = ponySession.removeStreamListener(Integer.parseInt(req.getParameter(Model.STREAM_REQUEST_ID.asString())));
            streamHandler.onStream(req, resp);
        } catch (final Exception e) {
            log.error("Cannot stream request", e);
            try {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (final IOException e1) {
                log.error("Request failure", e1);
            }
        }
    }
}