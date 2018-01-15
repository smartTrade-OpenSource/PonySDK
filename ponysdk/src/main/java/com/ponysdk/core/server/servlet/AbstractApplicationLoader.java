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

import com.ponysdk.core.server.application.AbstractApplicationManager;
import com.ponysdk.core.server.application.ApplicationManagerOption;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

public abstract class AbstractApplicationLoader implements ApplicationLoader {

    protected ApplicationManagerOption applicationManagerOption;

    private AbstractApplicationManager applicationManager;

    @Override
    public void start() {
        applicationManager = createApplicationManager();
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {

    }

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        final ServletContext servletContext = event.getServletContext();
        servletContext.setAttribute(AbstractApplicationLoader.class.getCanonicalName(), this);
        servletContext.setAttribute(AbstractApplicationManager.class.getCanonicalName(), applicationManager);
    }

    @Override
    public ApplicationManagerOption getApplicationManagerOption() {
        return applicationManagerOption;
    }

    @Override
    public void setApplicationManagerOption(final ApplicationManagerOption applicationManagerOption) {
        this.applicationManagerOption = applicationManagerOption;
    }

}
