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

package com.ponysdk.core.service;

import java.util.Calendar;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.PSystemProperty;
import com.ponysdk.core.PonyApplicationSession;
import com.ponysdk.core.tools.BannerPrinter;
import com.ponysdk.spring.SpringContextLoader;

public class ApplicationLoader implements ServletContextListener, HttpSessionListener {

    private static final Logger log = LoggerFactory.getLogger(ApplicationLoader.class);

    private final SpringContextLoader contextLoader;

    private String applicationName;

    private String applicationDescription;

    public ApplicationLoader() {
        contextLoader = new SpringContextLoader();
    }

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        applicationName = System.getProperty(PSystemProperty.APPLICATION_NAME);
        applicationDescription = System.getProperty(PSystemProperty.APPLICATION_DESCRIPTION);

        printLicence();

        contextLoader.initWebApplicationContext(event.getServletContext());
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        if (contextLoader != null) {
            contextLoader.closeWebApplicationContext(event.getServletContext());
        }
        printDestroyedBanner();
    }

    @Override
    public void sessionCreated(final HttpSessionEvent arg0) {
        if (log.isDebugEnabled()) {
            log.debug("Session created #" + arg0.getSession().getId());
        }
    }

    @Override
    public void sessionDestroyed(final HttpSessionEvent arg0) {
        final PonyApplicationSession applicationSession = (PonyApplicationSession) arg0.getSession().getAttribute(PonyApplicationSession.class.getCanonicalName());
        applicationSession.fireSessionDestroyed(arg0);
    }

    private void printDestroyedBanner() {
        final int columnCount = applicationName.length() + 30;

        final BannerPrinter bannerPrinter = new BannerPrinter(columnCount);
        bannerPrinter.appendNewEmptyLine(2);
        bannerPrinter.appendLineSeparator();
        bannerPrinter.appendNewLine(2);
        bannerPrinter.appendCenteredLine(applicationName + " - Context Destroyed");
        bannerPrinter.appendNewLine(2);
        bannerPrinter.appendLineSeparator();

        log.info(bannerPrinter.toString());
    }

    private void printLicence() {
        final int columnCount = applicationName.length() + applicationDescription.length() + 30;

        final BannerPrinter bannerPrinter = new BannerPrinter(columnCount);
        bannerPrinter.appendNewEmptyLine();
        bannerPrinter.appendLineSeparator();
        bannerPrinter.appendNewLine();
        bannerPrinter.appendCenteredLine(applicationName + " - " + applicationDescription);
        bannerPrinter.appendNewLine();
        bannerPrinter.appendCenteredLine("WEB  APPLICATION");
        bannerPrinter.appendNewLine();
        bannerPrinter.appendCenteredLine("(c) " + Calendar.getInstance().get(Calendar.YEAR) + " PonySDK");
        bannerPrinter.appendNewLine();
        bannerPrinter.appendLineSeparator();

        log.info(bannerPrinter.toString());
    }

}
