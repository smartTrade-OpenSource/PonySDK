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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;

import com.ponysdk.core.AbstractApplicationManager;
import com.ponysdk.core.ApplicationManagerOption;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.event.RootEventBus;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.ui.server.basic.PHistory;

public class ApplicationLoader extends AbstractApplicationLoader {

    private Class<? extends EntryPoint> entryPointClass;

    public ApplicationLoader(final ApplicationManagerOption option) {
        super(option);
    }

    @Override
    protected AbstractApplicationManager createApplicationManager(final ApplicationManagerOption option) {
        return new AbstractApplicationManager(option) {

            @Override
            protected EntryPoint initializePonySession(final UIContext ponySession) throws ServletException {
                EntryPoint entryPoint = null;
                try {
                    entryPoint = entryPointClass.newInstance();
                } catch (final Exception e) {
                    throw new ServletException("Failed to instantiate the EntryPoint #" + entryPointClass, e);
                }

                final EventBus rootEventBus = new RootEventBus();

                final PHistory history = new PHistory();

                ponySession.setRootEventBus(rootEventBus);
                ponySession.setHistory(history);

                return entryPoint;
            }
        };
    }

    @SuppressWarnings("unchecked")
    public void setEntryPointClassName(final String entryPointClassName) throws ClassNotFoundException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        this.entryPointClass = (Class<? extends EntryPoint>) classLoader.loadClass(entryPointClassName);
    }

    public void setEntryPointClass(final Class<? extends EntryPoint> entryPointClass) {
        this.entryPointClass = entryPointClass;
    }

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        super.contextInitialized(event);

        if (entryPointClass == null) {
            try {
                setEntryPointClassName(event.getServletContext().getInitParameter("entryPoint"));
            } catch (final ClassNotFoundException e) {
                log.error("Cannot initialize EntryPoint", e);
            }
        }
        if (entryPointClass == null) { throw new IllegalArgumentException("The entry point must be defined in the ServletContext."); }
    }
}
