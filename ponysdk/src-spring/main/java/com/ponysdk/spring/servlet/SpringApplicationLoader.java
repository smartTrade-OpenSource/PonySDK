
package com.ponysdk.spring.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.ponysdk.core.AbstractApplicationManager;
import com.ponysdk.core.ApplicationManagerOption;
import com.ponysdk.core.SystemProperty;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.servlet.AbstractApplicationLoader;
import com.ponysdk.impl.webapplication.page.InitializingActivity;
import com.ponysdk.ui.server.basic.PHistory;

public class SpringApplicationLoader extends AbstractApplicationLoader {

    private static final Logger log = LoggerFactory.getLogger(SpringApplicationLoader.class);

    private XmlWebApplicationContext context;

    private String serverConfiguration;

    private final List<String> clientConfigurations = new ArrayList<>();

    public SpringApplicationLoader(final ApplicationManagerOption option) {
        super(option);
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

        final ServletContext servletContext = event.getServletContext();

        try {
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

    @Override
    protected AbstractApplicationManager createApplicationManager(final ApplicationManagerOption option) {
        return new AbstractApplicationManager(option) {

            @Override
            protected EntryPoint initializePonySession(final UIContext ponySession) {
                return newPonySession(ponySession);
            }
        };
    }

    protected EntryPoint newPonySession(final UIContext ponySession) {
        final List<String> configurations = new ArrayList<>();
        if (clientConfigurations.isEmpty()) configurations.addAll(Arrays.asList("conf/client_application.inc.xml", "client_application.xml"));
        else configurations.addAll(clientConfigurations);

        EntryPoint entryPoint = null;

        try (final ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(configurations.toArray(new String[0]))) {
            final EventBus rootEventBus = applicationContext.getBean(EventBus.class);
            entryPoint = applicationContext.getBean(EntryPoint.class);
            final PHistory history = applicationContext.getBean(PHistory.class);

            ponySession.setRootEventBus(rootEventBus);
            ponySession.setHistory(history);

            final Map<String, InitializingActivity> initializingPages = applicationContext.getBeansOfType(InitializingActivity.class);
            if (initializingPages != null && !initializingPages.isEmpty()) {
                for (final InitializingActivity p : initializingPages.values()) {
                    p.afterContextInitialized();
                }
            }
        }

        return entryPoint;
    }

    public void addClientConfiguration(final String conf) {
        clientConfigurations.add(conf);
    }

}
