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

package com.ponysdk.core;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.ui.terminal.exception.PonySessionException;
import com.ponysdk.ui.terminal.instruction.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.instruction.Dictionnary.HISTORY;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public abstract class AbstractPonyEngineServiceImpl extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AbstractPonyEngineServiceImpl.class);

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        doProcess(req, resp);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        doProcess(req, resp);
    }

    protected void doProcess(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            final JSONObject data = new JSONObject(new JSONTokener(req.getReader()));
            if (data.has(APPLICATION.KEY)) {
                startApplication(data, req, resp);
            } else {
                fireInstructions(data, req, resp);
            }
        } catch (final Throwable e) {
            resp.sendError(501, e.getMessage());
            log.error("Failed to process request", e);
        }
    }

    public void startApplication(final JSONObject data, final HttpServletRequest req, final HttpServletResponse resp) throws Exception {
        final JSONObject response = new JSONObject();

        boolean isNewHttpSession = false;
        final HttpSession session = req.getSession();
        PonyApplicationSession applicationSession = (PonyApplicationSession) session.getAttribute(PonyApplicationSession.class.getCanonicalName());
        if (applicationSession == null) {
            log.info("Creating a new application ... Session ID #" + session.getId());
            applicationSession = new PonyApplicationSession(session);
            session.setAttribute(PonyApplicationSession.class.getCanonicalName(), applicationSession);
            isNewHttpSession = true;
        } else {
            log.info("Reloading application ... Session ID #" + session.getId());
        }

        synchronized (applicationSession) {
            final PonySession ponySession = new PonySession(applicationSession);

            response.put(APPLICATION.VIEW_ID, applicationSession.registerPonySession(ponySession));
            PonySession.setCurrent(ponySession);

            final EntryPoint entryPoint = initializePonySession(ponySession);
            if (isNewHttpSession) {
                entryPoint.start(ponySession);
            } else {
                entryPoint.restart(ponySession);
            }

            ponySession.getHistory().fireHistoryChanged(data.getString(HISTORY.TOKEN));

            try {
                ponySession.flushInstructions(response);
                final PrintWriter writer = resp.getWriter();
                writer.write(response.toString());
                writer.flush();
            } catch (final Throwable e) {
                log.error("Cannot send instructions to the browser, Session ID #" + req.getSession().getId(), e);
            }
        }
    }

    protected void fireInstructions(final JSONObject data, final HttpServletRequest req, final HttpServletResponse resp) throws Exception {
        final JSONObject response = new JSONObject();

        final long key = data.getLong(APPLICATION.VIEW_ID);

        final HttpSession session = req.getSession();
        final PonyApplicationSession applicationSession = (PonyApplicationSession) session.getAttribute(PonyApplicationSession.class.getCanonicalName());

        if (applicationSession == null) { throw new PonySessionException("Invalid session, please reload your application"); }

        final PonySession ponySession = applicationSession.getPonySession(key);

        if (ponySession == null) { throw new PonySessionException("Invalid session, please reload your application"); }

        synchronized (ponySession) {
            PonySession.setCurrent(ponySession);
            if (data.has(APPLICATION.INSTRUCTIONS)) {
                final JSONArray instructions = data.getJSONArray(APPLICATION.INSTRUCTIONS);
                for (int i = 0; i < instructions.length(); i++) {
                    ponySession.fireInstruction(instructions.getJSONObject(i));
                }
            }

            try {
                if (ponySession.flushInstructions(response)) {
                    final PrintWriter writer = resp.getWriter();
                    writer.write(response.toString());
                    writer.flush();
                }
            } catch (final Throwable e) {
                log.error("Cannot send instructions to the browser, Session ID #" + req.getSession().getId(), e);
            }
        }
    }

    /**
     * Initialize the session with:
     * <ul>
     * <li>event bus</li>
     * <li>place controller</li>
     * <li>history manager</li>
     * <li>cookies manager</li>
     * </ul>
     * 
     * @param ponySession
     * @return
     */
    protected abstract EntryPoint initializePonySession(final PonySession ponySession) throws Exception;
}
