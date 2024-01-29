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

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.server.context.UIContextImpl;
import com.ponysdk.core.ui.basic.PObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AjaxServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AjaxServlet.class);

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) {
        process(req, resp);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) {
        process(req, resp);
    }

    private void process(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            final int uiContextID = parseInt(req.getHeader(ClientToServerModel.UI_CONTEXT_ID.name()));
            final UIContextImpl uiContext = UIContextImpl.get(uiContextID);

            if (uiContext == null) {
                throw new IllegalStateException("UI Context #" + uiContextID + " not found");
            }

            final int objectID = parseInt(req.getHeader(ClientToServerModel.OBJECT_ID.name()));
            uiContext.execute(() -> handleAjaxRequest(uiContext, objectID, req, resp));
        } catch (final Exception e) {
            handleError(resp, e.getMessage());
            log.error("Cannot stream request", e);
        }
    }

    private void handleAjaxRequest(UIContextImpl uiContext, int objectID, HttpServletRequest req, HttpServletResponse resp) {
        try {
            final PObject pObject = uiContext.getObject(objectID);
            if (pObject == null) {
                throw new IllegalStateException("PObject #" + objectID + " not found in UIContext");
            }
            pObject.handleAjaxRequest(req, resp);
        } catch (ServletException | IOException e) {
            handleError(resp, e.getMessage());
            log.error("Cannot handle Ajax request", e);
        }
    }

    private int parseInt(String value) throws NumberFormatException {
        if (value == null) {
            throw new IllegalArgumentException("Value is null");
        }
        return Integer.parseInt(value);
    }

    private void handleError(HttpServletResponse resp, String message) {
        try {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
        } catch (IOException e) {
            log.error("Cannot send error", e);
        }
    }
}