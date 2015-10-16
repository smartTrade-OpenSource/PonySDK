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

    public SpringApplicationLoader() {
        super();
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
            // context.refresh();

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
    public AbstractApplicationManager createApplicationManager(final ApplicationManagerOption applicationManagerOption) {
        return new AbstractApplicationManager(applicationManagerOption) {

            @Override
            protected EntryPoint initializePonySession(final UIContext ponySession) {
                final List<String> configurations = new ArrayList<>();

                final String clientConfigFile = applicationManagerOption.getClientConfigFile();
                if (StringUtils.isEmpty(clientConfigFile)) configurations.addAll(Arrays.asList("conf/client_application.inc.xml", "client_application.xml"));
                else configurations.add(clientConfigFile);

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
        };
    }

}
