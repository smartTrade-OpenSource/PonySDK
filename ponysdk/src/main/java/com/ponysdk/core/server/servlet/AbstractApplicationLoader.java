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

import java.util.Calendar;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpSessionEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.AbstractApplicationManager;
import com.ponysdk.core.server.application.ApplicationManagerOption;

public abstract class AbstractApplicationLoader implements ApplicationLoader {

    private static final Logger log = LoggerFactory.getLogger(AbstractApplicationLoader.class);

    protected ApplicationManagerOption applicationManagerOption;

    private AbstractApplicationManager applicationManager;

    @Override
    public void start() {
        applicationManager = createApplicationManager(applicationManagerOption);
        printWelcomBanner();
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        printGoodbyeBanner();
    }

    @Override
    public void sessionCreated(final HttpSessionEvent httpSessionEvent) {
        if (log.isInfoEnabled()) {
            log.info("HTTP Session created: {}", httpSessionEvent.getSession().getId());
        }

        SessionManager.get().registerSession(httpSessionEvent.getSession());
    }

    @Override
    public void sessionDestroyed(final HttpSessionEvent httpSessionEvent) {
        if (log.isInfoEnabled()) {
            log.info("HTTP Session Destroyed: {}", httpSessionEvent.getSession().getId());
        }

        SessionManager.get().unregisterSession(httpSessionEvent.getSession().getId());
    }

    private void printWelcomBanner() {
        final BannerPrinter bannerPrinter = new BannerPrinter(60);
        bannerPrinter.appendNewEmptyLine(2);
        bannerPrinter.appendLineSeparator();
        bannerPrinter.appendCenteredLine("PonySDK http://www.ponysdk.com");
        bannerPrinter.appendCenteredLine("WEB  APPLICATION");
        bannerPrinter.appendCenteredLine(applicationManagerOption.getApplicationID());
        bannerPrinter.appendCenteredLine(applicationManagerOption.getApplicationName());
        bannerPrinter.appendCenteredLine("(c) " + Calendar.getInstance().get(Calendar.YEAR) + " PonySDK");
        bannerPrinter.appendLineSeparator();

        bannerPrinter.print();
    }

    private void printGoodbyeBanner() {
        final BannerPrinter bannerPrinter = new BannerPrinter(60);
        bannerPrinter.appendNewEmptyLine(2);
        bannerPrinter.appendLineSeparator();
        bannerPrinter.appendCenteredLine("Context Destroyed");
        bannerPrinter.appendCenteredLine(applicationManagerOption.getApplicationID());
        bannerPrinter.appendCenteredLine(applicationManagerOption.getApplicationName());
        bannerPrinter.appendCenteredLine("(c) " + Calendar.getInstance().get(Calendar.YEAR) + " PonySDK");
        bannerPrinter.appendLineSeparator();

        bannerPrinter.print();
    }

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        final ServletContext servletContext = event.getServletContext();
        servletContext.setAttribute(AbstractApplicationLoader.class.getCanonicalName(), this);
        servletContext.setAttribute(AbstractApplicationManager.class.getCanonicalName(), applicationManager);
    }

    @Override
    public void setApplicationManagerOption(final ApplicationManagerOption applicationManagerOption) {
        this.applicationManagerOption = applicationManagerOption;
    }

    @Override
    public ApplicationManagerOption getApplicationManagerOption() {
        return applicationManagerOption;
    }

}
