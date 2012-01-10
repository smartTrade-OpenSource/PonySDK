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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.event.StreamHandler;

/**
 * The server side implementation of the RPC service.
 */
public class StreamServiceServlet extends HttpServlet {

	private static final long serialVersionUID = 5368766616550622126L;

	private static final Logger log = LoggerFactory.getLogger(StreamServiceServlet.class);

    private static final String STREAM_REQUEST_ID = "STREAM_REQUEST_ID";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        streamRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        streamRequest(req, resp);
    }

    private void streamRequest(HttpServletRequest req, HttpServletResponse resp) {
        try {
            final PonyApplicationSession ponyApplicationSession = (PonyApplicationSession) req.getSession().getAttribute(PonyApplicationSession.class.getCanonicalName());
            final Long ponySessionID = Long.parseLong(req.getParameter("ponySessionID"));
            final PonySession ponySession = ponyApplicationSession.getPonySession(ponySessionID);

            final StreamHandler streamHandler = ponySession.removeStreamListener(Long.parseLong(req.getParameter(STREAM_REQUEST_ID)));
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
