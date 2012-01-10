
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

import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.ponysdk.core.PonyApplicationSession;
import com.ponysdk.core.PonyEngineServiceImpl;
import com.ponysdk.core.PonyRemoteServiceServlet;
import com.ponysdk.core.PonySession;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.place.PlaceController;
import com.ponysdk.impl.webapplication.page.InitializingActivity;
import com.ponysdk.ui.server.basic.PCookies;
import com.ponysdk.ui.server.basic.PHistory;
import com.ponysdk.ui.terminal.PonyEngineService;
import com.ponysdk.ui.terminal.PonySessionContext;
import com.ponysdk.ui.terminal.exception.PonySessionException;
import com.ponysdk.ui.terminal.instruction.Instruction;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class SpringPonyEngineServiceImpl extends PonyRemoteServiceServlet implements PonyEngineService {

    private static final Logger log = LoggerFactory.getLogger(PonyEngineServiceImpl.class);

    @Override
    public void init() throws ServletException {
        super.init();
    }

    @Override
    public PonySessionContext startApplication(String token, Map<String, String> cookiesByName) throws Exception {
        try {
            boolean isNewHttpSession = false;
            final HttpSession session = getThreadLocalRequest().getSession();
            PonyApplicationSession applicationSession = (PonyApplicationSession) session.getAttribute(PonyApplicationSession.class.getCanonicalName());
            if (applicationSession == null) {
                log.info("Creating a new application ... session[" + session.getId() + "]");
                applicationSession = new PonyApplicationSession(session);
                session.setAttribute(PonyApplicationSession.class.getCanonicalName(), applicationSession);
                isNewHttpSession = true;
            }

            synchronized (applicationSession) {

                final PonySession ponySession = new PonySession(applicationSession);

                final long ponySessionID = applicationSession.registerPonySession(ponySession);
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

                final PCookies cookies = new PCookies(cookiesByName);
                ponySession.setCookies(cookies);

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

                ponySession.getHistory().fireHistoryChanged(token); // update current token

                final PonySessionContext ponySessionContext = new PonySessionContext();
                ponySessionContext.setID(ponySessionID);
                ponySessionContext.setInstructions(ponySession.flushInstructions());

                return ponySessionContext;
            }

        } catch (final Throwable e) {
            log.error("[PonyEngineServiceImpl::startApplication]=>failed ", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Instruction> fireInstructions(final long key, final List<Instruction> instructions) throws Exception {
        final long start = System.currentTimeMillis();
        try {

            final HttpSession session = getThreadLocalRequest().getSession();
            final PonyApplicationSession applicationSession = (PonyApplicationSession) session.getAttribute(PonyApplicationSession.class.getCanonicalName());

            if (applicationSession == null) { throw new PonySessionException("Invalid session, please reload your application"); }

            final PonySession ponySession = applicationSession.getPonySession(key);

            if (ponySession == null) { throw new PonySessionException("Invalid session, please reload your application"); }

            synchronized (ponySession) {
                PonySession.setCurrent(ponySession);
                ponySession.fireInstructions(instructions);
                return ponySession.flushInstructions();
            }
        } catch (final PonySessionException e) {
            log.error("[PonyEngineServiceImpl::fireInstructions]=>failed : " + instructions.toString(), e);
            throw e;
        } catch (final Throwable e) {
            log.error("[PonyEngineServiceImpl::fireInstructions]=>failed : " + instructions.toString(), e);
            throw new RuntimeException(e);
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("Server call took " + (System.currentTimeMillis() - start) + " millis");
            }
        }
    }

}
