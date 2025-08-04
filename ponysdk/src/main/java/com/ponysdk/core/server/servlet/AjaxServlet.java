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
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.PObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AjaxServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AjaxServlet.class);

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            process(req, resp);
        } catch (final IOException e) {
            log.error("Cannot stream request", e);
        }
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            process(req, resp);
        } catch (final IOException e) {
            log.error("Cannot stream request", e);
        }
    }

    private void process(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            final int uiContextID = Integer.parseInt(req.getHeader(ClientToServerModel.UI_CONTEXT_ID.name()));
            final UIContext uiContext = SessionManager.get().getUIContext(uiContextID);
            if (uiContext != null) {
                final int objectID = Integer.parseInt(req.getHeader(ClientToServerModel.OBJECT_ID.name()));
                uiContext.execute(() -> {
                    try {
                        final PObject pObject = uiContext.getObject(objectID);
                        pObject.handleAjaxRequest(req, resp);
                    } catch (ServletException | IOException e) {
                        log.error("Cannot stream request", e);
                        try {
                            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        } catch (final IOException e1) {
                            log.error("Cannot send error", e);
                        }
                    }
                });
            } else {
                log.warn("Can't found UI Context #{}, already destroyed ?", uiContextID);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "UI Context #" + uiContextID + " not found");
            }
        } catch (final Exception e) {
            log.error("Cannot stream request", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
