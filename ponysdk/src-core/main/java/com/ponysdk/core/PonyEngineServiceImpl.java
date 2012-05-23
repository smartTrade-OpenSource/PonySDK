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

import javax.servlet.ServletException;

import com.ponysdk.core.event.PEventBus;
import com.ponysdk.core.event.PSimpleEventBus;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.ui.server.basic.PHistory;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class PonyEngineServiceImpl extends AbstractPonyEngineServiceImpl {

    @Override
    public void init() throws ServletException {
        super.init();

        final String entryPointClassName = getServletConfig().getInitParameter("entryPoint");

        if (entryPointClassName == null || entryPointClassName.isEmpty()) throw new ServletException("The entry point must be defined in your web.xml.");
    }

    @Override
    protected EntryPoint initializePonySession(final PonySession ponySession) throws Exception {

        EntryPoint entryPoint = null;
        final String entryPointClassName = getServletConfig().getInitParameter("entryPoint");
        try {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            final Class<?> clazz = classLoader.loadClass(entryPointClassName);
            entryPoint = (EntryPoint) clazz.newInstance();
        } catch (final ClassNotFoundException e) {
            throw new ServletException("Unable to load the entry point #" + entryPointClassName + " from the classpath.", e);
        } catch (final Exception e) {
            throw new ServletException("Failed to instantiate the entry point #" + entryPointClassName, e);
        }

        final PEventBus rootEventBus = new PSimpleEventBus();

        final PHistory history = new PHistory();

        ponySession.setRootEventBus(rootEventBus);
        ponySession.setHistory(history);
        ponySession.setEntryPoint(entryPoint);

        // final PCookies cookies = new PCookies(data.getString(PROPERTY.COOKIE));
        // ponySession.setCookies(cookies);

        return entryPoint;
    }

}
