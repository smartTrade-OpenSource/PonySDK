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

import java.util.Calendar;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.SystemProperty;
import com.ponysdk.core.tools.BannerPrinter;

public class ApplicationLoader implements ServletContextListener, HttpSessionListener {

    private static final Logger log = LoggerFactory.getLogger(ApplicationLoader.class);

    private String applicationName;
    private String applicationDescription;

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        applicationName = System.getProperty(SystemProperty.APPLICATION_NAME);
        applicationDescription = System.getProperty(SystemProperty.APPLICATION_DESCRIPTION);

        printLicence();
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        printDestroyedBanner();
    }

    @Override
    public void sessionCreated(final HttpSessionEvent httpSessionEvent) {
        if (log.isDebugEnabled()) {
            log.debug("Session created #" + httpSessionEvent.getSession().getId());
        }

        SessionManager.get().registerSession(new HttpSession(httpSessionEvent.getSession()));
    }

    @Override
    public void sessionDestroyed(final HttpSessionEvent httpSessionEvent) {
        if (log.isDebugEnabled()) {
            log.debug("Session Destroyed #" + httpSessionEvent.getSession().getId());
        }

        SessionManager.get().unregisterSession(httpSessionEvent.getSession().getId());
    }

    private void printDestroyedBanner() {
        String title;

        if (applicationName == null) {
            title = "Pony Application - Context Destroyed";
        } else {
            title = applicationName + " - Context Destroyed";
        }

        final int columnCount = title.length() + 5;

        final BannerPrinter bannerPrinter = new BannerPrinter(columnCount);
        bannerPrinter.appendNewEmptyLine(2);
        bannerPrinter.appendLineSeparator();
        bannerPrinter.appendNewLine(2);
        bannerPrinter.appendCenteredLine(title);
        bannerPrinter.appendNewLine(2);
        bannerPrinter.appendLineSeparator();

        log.info(bannerPrinter.toString());
    }

    private void printLicence() {
        String title = "";
        if (applicationName == null && applicationDescription == null) {
            title = "Powered by PonySDK http://www.ponysdk.com";
        } else if (applicationName == null && applicationDescription != null) {
            title = applicationDescription;
        } else if (applicationName == null && applicationDescription != null) {
            title = applicationName;
        } else {
            title = applicationName + " - " + applicationDescription;
        }

        final int columnCount = title.length() + 30;

        final BannerPrinter bannerPrinter = new BannerPrinter(columnCount);
        bannerPrinter.appendNewEmptyLine();
        bannerPrinter.appendLineSeparator();
        bannerPrinter.appendNewLine();
        bannerPrinter.appendCenteredLine(title);
        bannerPrinter.appendNewLine();
        bannerPrinter.appendCenteredLine("WEB  APPLICATION");
        bannerPrinter.appendNewLine();
        bannerPrinter.appendCenteredLine("(c) " + Calendar.getInstance().get(Calendar.YEAR) + " PonySDK");
        bannerPrinter.appendNewLine();
        bannerPrinter.appendLineSeparator();

        log.info(bannerPrinter.toString());
    }
}
