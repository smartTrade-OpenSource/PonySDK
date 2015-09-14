
package com.ponysdk.impl.main;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.ApplicationManagerOption;
import com.ponysdk.core.servlet.ApplicationLoader;
import com.ponysdk.core.servlet.BootstrapServlet;
import com.ponysdk.core.servlet.PonySDKHttpServlet;
import com.ponysdk.core.servlet.ServletContextFilter;
import com.ponysdk.core.servlet.StreamServiceServlet;
import com.ponysdk.core.servlet.WebSocketServlet;

public class PonySDKServer {

    public static final String MAPPING_TERMINAL = "/p";
    public static final String MAPPING_BOOTSTRAP = "/*";
    public static final String MAPPING_WS = "/ws/*";
    public static final String MAPPING_STREAM = "/stream";

    protected static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) throws Exception {
        final Server server = new Server(8081);

        final ApplicationManagerOption option = new ApplicationManagerOption();
        option.setApplicationID("ID");
        option.setApplicationName("NAME");
        option.setApplicationDescription("DESCRIPTION");

        final ApplicationLoader applicationLoader = new ApplicationLoader(option);
        applicationLoader.setEntryPointClass(BasicEntryPoint.class);

        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/" + "sample");
        final ServletContextFilter servletContextFilter = new ServletContextFilter();

        context.addServlet(new ServletHolder(new PonySDKHttpServlet()), MAPPING_TERMINAL);
        context.addServlet(new ServletHolder(new BootstrapServlet()), MAPPING_BOOTSTRAP);
        context.addServlet(new ServletHolder(new StreamServiceServlet()), MAPPING_STREAM);
        context.addServlet(new ServletHolder(new WebSocketServlet()), MAPPING_WS);

        context.addFilter(new FilterHolder(servletContextFilter), MAPPING_BOOTSTRAP, EnumSet.of(DispatcherType.REQUEST));

        final FilterHolder filterHolder = new FilterHolder(GzipFilter.class);
        context.addFilter(filterHolder, "/*", EnumSet.allOf(DispatcherType.class));

        context.addEventListener(applicationLoader);

        context.getSessionHandler().getSessionManager().setMaxInactiveInterval(60 * 1000);
        context.getSessionHandler().addEventListener(applicationLoader);

        server.setHandler(context);

        server.start();

        server.join();
    }
}
