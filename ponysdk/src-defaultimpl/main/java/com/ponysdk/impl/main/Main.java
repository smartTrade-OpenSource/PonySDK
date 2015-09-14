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

package com.ponysdk.impl.main;

import java.io.IOException;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.ApplicationManagerOption;
import com.ponysdk.core.servlet.AbstractApplicationLoader;
import com.ponysdk.core.servlet.BootstrapServlet;
import com.ponysdk.core.servlet.PonySDKHttpServlet;
import com.ponysdk.core.servlet.ServletContextFilter;
import com.ponysdk.core.servlet.StreamServiceServlet;
import com.ponysdk.core.servlet.WebSocketServlet;
import com.ponysdk.spring.servlet.SpringApplicationLoader;

public class Main {

    public static final String MAPPING_TERMINAL = "/p";
    public static final String MAPPING_BOOTSTRAP = "/*";
    public static final String MAPPING_WS = "/ws/*";
    public static final String MAPPING_STREAM = "/stream";

    protected static final Logger log = LoggerFactory.getLogger(Main.class);

    private Server webServer;

    private Integer port;
    private int sessionTimeout = 60;
    private String war;
    private String applicationContextName;
    private String applicationID;
    private String applicationName;
    private String applicationDescription;

    private AbstractApplicationLoader applicationLoader;
    private Servlet bootstrapServlet;
    private Filter servletConextFilter;
    private Handler handler;

    public static void main(final String[] args) throws Exception {
        final Main main = new Main();

        for (final String arg : args) {
            final String[] parameter = arg.split("=");

            if (parameter[0].equals("contextName")) {
                main.setApplicationContextName(parameter[1]);
            } else if (parameter[0].equals("port")) {
                main.setPort(Integer.valueOf(parameter[1]));
            } else if (parameter[0].equals("war")) {
                main.setWar(parameter[1]);
            } else if (parameter[0].equals("applicationName")) {
                main.setApplicationName(parameter[1]);
            } else if (parameter[0].equals("applicationID")) {
                main.setApplicationID(parameter[1]);
            } else if (parameter[0].equals("applicationDescription")) {
                main.setApplicationDescription(parameter[1]);
            }
        }

        main.start();
    }

    public Main() throws IOException {}

    public void start() throws Exception {
        if (war == null || war.isEmpty()) {
            handler = newServletContext();
        } else {
            handler = loadWar();
        }

        if (port != null) {
            webServer = new Server(port);
        } else {
            webServer = new Server();
        }
        webServer.setHandler(handler);
        webServer.start();
    }

    protected ServletContextHandler newServletContext() {
        // set default value
        if (bootstrapServlet == null) {
            bootstrapServlet = new BootstrapServlet();
        }

        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

        if (applicationLoader == null) {
            final ApplicationManagerOption option = new ApplicationManagerOption();
            option.setApplicationID(applicationID);
            option.setApplicationName(applicationName);
            option.setApplicationDescription(applicationDescription);
            applicationLoader = new SpringApplicationLoader(option);
        }

        if (servletConextFilter == null) {
            servletConextFilter = new ServletContextFilter();
        }

        context.setContextPath("/" + applicationContextName);

        context.addServlet(new ServletHolder(new PonySDKHttpServlet()), MAPPING_TERMINAL);
        context.addServlet(new ServletHolder(bootstrapServlet), MAPPING_BOOTSTRAP);
        context.addServlet(new ServletHolder(new StreamServiceServlet()), MAPPING_STREAM);
        context.addServlet(new ServletHolder(new WebSocketServlet()), MAPPING_WS);

        context.addFilter(new FilterHolder(servletConextFilter), MAPPING_BOOTSTRAP, EnumSet.of(DispatcherType.REQUEST));

        final FilterHolder filterHolder = new FilterHolder(GzipFilter.class);
        context.addFilter(filterHolder, "/*", EnumSet.allOf(DispatcherType.class));

        context.addEventListener(applicationLoader);

        context.getSessionHandler().getSessionManager().setMaxInactiveInterval(60 * sessionTimeout);
        context.getSessionHandler().addEventListener(applicationLoader);

        return context;
    }

    protected Handler loadWar() throws IOException {

        final WebAppContext webapp = new WebAppContext();
        if (applicationContextName != null) {
            webapp.setContextPath("/" + applicationContextName);
            webapp.setDescriptor(applicationContextName);
        } else {
            webapp.setContextPath("/");
        }

        webapp.setWar(war);
        webapp.setExtractWAR(true);
        webapp.setParentLoaderPriority(true);
        webapp.setClassLoader(new WebAppClassLoader(Main.class.getClassLoader(), webapp));

        return webapp;
    }

    public void setPort(final Integer port) {
        this.port = port;
    }

    public void setApplicationContextName(final String applicationContextName) {
        this.applicationContextName = applicationContextName;
    }

    public void setApplicationID(final String applicationID) {
        this.applicationID = applicationID;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    public void setWar(final String war) {
        this.war = war;
    }

    public void setBootstrapServlet(final Servlet bootstrapServlet) {
        this.bootstrapServlet = bootstrapServlet;
    }

    public void setServletConextFilter(final Filter servletConextFilter) {
        this.servletConextFilter = servletConextFilter;
    }

    public Handler getHandler() {
        return handler;
    }

    public void setSessionTimeout(final int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    private void setApplicationDescription(final String applicationDescription) {
        this.applicationDescription = applicationDescription;
    }

    public void setApplicationLoader(final AbstractApplicationLoader applicationLoader) {
        this.applicationLoader = applicationLoader;
    }

}
