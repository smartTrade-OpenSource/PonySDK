
package com.ponysdk.sample.trading.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.ApplicationManagerOption;
import com.ponysdk.core.servlet.JavaApplicationLoader;
import com.ponysdk.core.servlet.BootstrapServlet;
import com.ponysdk.impl.main.PonySDKServer;

public class TradingSample {

    private static Logger log = LoggerFactory.getLogger(TradingSample.class);

    public static void main(final String[] args) {
        try {
            // Register service
            // PonyServiceRegistry.registerPonyService(new TradingServiceImpl());

            // Start webserver
            final ApplicationManagerOption applicationManagerOption = new ApplicationManagerOption();
            applicationManagerOption.setApplicationContextName("trading");
            applicationManagerOption.setEntryPointClass(com.ponysdk.sample.trading.client.TradingSampleEntryPoint.class);

            final JavaApplicationLoader applicationLoader = new JavaApplicationLoader();
            applicationLoader.setApplicationManagerOption(applicationManagerOption);

            final PonySDKServer server = new PonySDKServer() {

                @Override
                protected BootstrapServlet createBootstrapServlet() {
                    final BootstrapServlet bootstrapServlet = new BootstrapServlet();
                    bootstrapServlet.addStylesheet("css/sample.less");
                    bootstrapServlet.addStylesheet("css/ponysdk.less");
                    bootstrapServlet.addJavascript("script/less.js");
                    return bootstrapServlet;
                }
            };
            server.setApplicationLoader(applicationLoader);

            server.start();
        } catch (final Exception e) {
            log.error("", e);
        }
    }

}
