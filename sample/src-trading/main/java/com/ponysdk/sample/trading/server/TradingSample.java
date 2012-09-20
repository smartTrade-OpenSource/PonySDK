
package com.ponysdk.sample.trading.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.main.Main;
import com.ponysdk.core.service.PonyServiceRegistry;
import com.ponysdk.core.servlet.ApplicationLoader;
import com.ponysdk.core.servlet.BootstrapServlet;
import com.ponysdk.core.servlet.HttpServlet;

public class TradingSample {

    private static Logger log = LoggerFactory.getLogger(TradingSample.class);

    public static void main(final String[] args) {
        try {
            // Register service
            PonyServiceRegistry.registerPonyService(new TradingServiceImpl());

            // Start webserver
            final ApplicationLoader applicationLoader = new ApplicationLoader();
            final HttpServlet httpServlet = new HttpServlet();
            httpServlet.setEntryPointClassName("com.ponysdk.sample.trading.client.TradingSampleEntryPoint");
            final BootstrapServlet bootstrapServlet = new BootstrapServlet();
            bootstrapServlet.addStylesheet("css/sample.less");
            bootstrapServlet.addStylesheet("css/ponysdk.less");
            bootstrapServlet.addJavascript("script/less.js");

            final Main main = new Main();
            main.setApplicationContextName("trading");
            main.setPort(8081);
            main.setHttpServlet(httpServlet);
            main.setHttpSessionListener(applicationLoader);
            main.setServletContextListener(applicationLoader);
            main.setBootstrapServlet(bootstrapServlet);
            main.start();
        } catch (final Exception e) {
            log.error("", e);
        }
    }

}
