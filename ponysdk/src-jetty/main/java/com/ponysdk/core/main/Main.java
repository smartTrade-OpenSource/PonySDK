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

package com.ponysdk.core.main;

import java.io.IOException;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.service.ApplicationLoader;
import com.ponysdk.core.servlet.BootstrapServlet;
import com.ponysdk.core.servlet.StreamServiceServlet;
import com.ponysdk.core.servlet.WebSocketServlet;
import com.ponysdk.spring.service.SpringApplicationLoader;
import com.ponysdk.spring.servlet.SpringHttpServlet;

public class Main {

    protected static final Logger log = LoggerFactory.getLogger(Main.class);

    private Server webServer;

    private Integer port;

    private String war;

    private String applicationContextName;

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
            }
        }

        main.start();
    }

    public void start() throws Exception {

        Handler handler;
        if (war == null || war.isEmpty()) {
            handler = loadServletContext();
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

    private ServletContextHandler loadServletContext() {
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/" + applicationContextName);
        context.addServlet(new ServletHolder(new SpringHttpServlet()), "/ponyterminal/p");
        context.addServlet(new ServletHolder(new StreamServiceServlet()), "/ponyterminal/stream");
        context.addServlet(new ServletHolder(new WebSocketServlet()), "/ws/*");
        context.addServlet(new ServletHolder(new BootstrapServlet()), "/*");

        final ApplicationLoader applicationLoader = new SpringApplicationLoader();
        context.addEventListener(applicationLoader);
        context.getSessionHandler().addEventListener(applicationLoader);
        return context;
    }

    public Handler loadWar() throws IOException {

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

    public void setWar(final String war) {
        this.war = war;
    }
}
