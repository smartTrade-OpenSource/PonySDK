
package com.ponysdk.spring;

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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

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
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.ponysdk.core.PonyApplicationSession;
import com.ponysdk.core.PonyEngineServiceImpl;
import com.ponysdk.core.PonySession;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.place.PlaceController;
import com.ponysdk.impl.webapplication.page.InitializingActivity;
import com.ponysdk.ui.server.basic.PHistory;
import com.ponysdk.ui.terminal.exception.PonySessionException;
import com.ponysdk.ui.terminal.instruction.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.instruction.Dictionnary.HISTORY;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class SpringPonyEngineServiceImpl extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(PonyEngineServiceImpl.class);

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
        } catch (final Exception e) {
            resp.sendError(501, e.getMessage());
            e.printStackTrace();

        }
    }

    public void startApplication(final JSONObject data, final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            final JSONObject response = new JSONObject();

            boolean isNewHttpSession = false;
            final HttpSession session = req.getSession();
            PonyApplicationSession applicationSession = (PonyApplicationSession) session.getAttribute(PonyApplicationSession.class.getCanonicalName());
            if (applicationSession == null) {
                log.info("Creating a new application ... session[" + session.getId() + "]");
                applicationSession = new PonyApplicationSession(session);
                session.setAttribute(PonyApplicationSession.class.getCanonicalName(), applicationSession);
                isNewHttpSession = true;
            }

            synchronized (applicationSession) {

                final PonySession ponySession = new PonySession(applicationSession);

                response.put(APPLICATION.VIEW_ID, applicationSession.registerPonySession(ponySession));

                PonySession.setCurrent(ponySession);

                final ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(new String[] { "conf/client_application.inc.xml", "client_application.xml" });

                final EventBus rootEventBus = applicationContext.getBean(EventBus.class);
                final EntryPoint entryPoint = applicationContext.getBean(EntryPoint.class);
                final PlaceController placeController = applicationContext.getBean(PlaceController.class);
                final PHistory history = applicationContext.getBean(PHistory.class);

                ponySession.setRootEventBus(rootEventBus);
                ponySession.setHistory(history);
                ponySession.setPlaceController(placeController);
                ponySession.setEntryPoint(entryPoint);

                // final PCookies cookies = new PCookies(cookiesByName);
                // ponySession.setCookies(cookies);

                final Map<String, InitializingActivity> initializingPages = applicationContext.getBeansOfType(InitializingActivity.class);
                if (initializingPages != null && !initializingPages.isEmpty()) {
                    for (final InitializingActivity p : initializingPages.values()) {
                        p.afterContextInitialized();
                    }
                }

                if (isNewHttpSession) entryPoint.start(ponySession);
                else {
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

        } catch (final Throwable e) {
            log.error("[PonyEngineServiceImpl::startApplication]=>failed ", e);
            throw new RuntimeException(e);
        }
    }

    private void fireInstructions(final JSONObject data, final HttpServletRequest req, final HttpServletResponse resp) throws Exception {
        final JSONObject response = new JSONObject();

        final long key = data.getLong(APPLICATION.VIEW_ID);
        final long start = System.currentTimeMillis();

        try {

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
        } catch (final PonySessionException e) {
            // log.error("[PonyEngineServiceImpl::fireInstructions]=>failed : " + instructions.toString(), e);
            throw e;
        } catch (final Throwable e) {
            // log.error("[PonyEngineServiceImpl::fireInstructions]=>failed : " + instructions.toString(), e);
            throw new RuntimeException(e);
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("Server call took " + (System.currentTimeMillis() - start) + " millis");
            }
        }
    }

}
