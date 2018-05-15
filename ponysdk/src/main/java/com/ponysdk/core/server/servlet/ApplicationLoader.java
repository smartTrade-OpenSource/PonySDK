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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.application.ApplicationManagerOption;

/**
 * @deprecated Use {@link com.ponysdk.core.server.application.ApplicationManager} directly
 */
@Deprecated(forRemoval = true, since = "v2.8.1")
public abstract class ApplicationLoader implements ServletContextListener {

    private ApplicationManager applicationManager;
    protected ApplicationManagerOption configuration;

    public ApplicationLoader() {
        applicationManager = createApplicationManager();
    }

    public void start() {
        applicationManager.start();
    }

    protected abstract ApplicationManager createApplicationManager();

    public ApplicationManagerOption getApplicationManagerOption() {
        return applicationManager.getConfiguration();
    }

    public void setApplicationManagerOption(final ApplicationManagerOption configuration) {
        this.configuration = configuration;
        applicationManager.setConfiguration(configuration);
    }

    public ApplicationManager getApplicationManager() {
        return applicationManager;
    }

    public void setApplicationManager(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        // Nothing to do
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        // Nothing to do
    }

}
