
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

import java.util.Map;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.ponysdk.core.AbstractPonyEngineServiceImpl;
import com.ponysdk.core.PonySession;
import com.ponysdk.core.event.PEventBus;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.place.PlaceController;
import com.ponysdk.impl.webapplication.page.InitializingActivity;
import com.ponysdk.ui.server.basic.PHistory;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class SpringPonyEngineServiceImpl extends AbstractPonyEngineServiceImpl {

    @Override
    protected EntryPoint initializePonySession(final PonySession ponySession) throws Exception {
        final ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(new String[] { "conf/client_application.inc.xml", "client_application.xml" });

        final PEventBus rootEventBus = applicationContext.getBean(PEventBus.class);
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

        return entryPoint;
    }

}
