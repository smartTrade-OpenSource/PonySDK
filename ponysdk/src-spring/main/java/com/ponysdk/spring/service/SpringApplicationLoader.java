
package com.ponysdk.spring.service;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.ponysdk.core.SystemProperty;
import com.ponysdk.core.servlet.ApplicationLoader;

public class SpringApplicationLoader extends ApplicationLoader {

    private static final Logger log = LoggerFactory.getLogger(SpringApplicationLoader.class);

    private XmlWebApplicationContext context;

    private String serverConfiguration;

    public SpringApplicationLoader() {
        serverConfiguration = System.getProperty(SystemProperty.CONTEXT_CONFIG_LOCATION, serverConfiguration);
    }

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        super.contextInitialized(event);

        if (serverConfiguration != null) {
            serverConfiguration = "classpath:" + serverConfiguration;
        } else {
            serverConfiguration = "classpath:server_application.xml";
        }
        try {
            final ServletContext servletContext = event.getServletContext();

            context = new XmlWebApplicationContext();
            context.setServletContext(servletContext);
            context.setConfigLocations(StringUtils.tokenizeToStringArray(serverConfiguration, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
            context.refresh();

            servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
        } catch (final Exception e) {
            log.error("Failure during Spring context initialisation", e);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        log.info("Closing Spring context");
        try {
            if (this.context instanceof ConfigurableWebApplicationContext) {
                ((ConfigurableWebApplicationContext) this.context).close();
            }
        } catch (final Exception e) {
            log.error("Failure during Spring context closure", e);
        }
        super.contextDestroyed(event);
    }

    public void setServerConfiguration(final String serverConfiguration) {
        this.serverConfiguration = serverConfiguration;
    }

}
