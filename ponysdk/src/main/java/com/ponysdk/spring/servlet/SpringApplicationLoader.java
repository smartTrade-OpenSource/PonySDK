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

import javax.servlet.ServletContextEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.ponysdk.core.AbstractApplicationManager;
import com.ponysdk.core.ApplicationManagerOption;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.servlet.AbstractApplicationLoader;
import com.ponysdk.impl.webapplication.page.InitializingActivity;

public class SpringApplicationLoader extends AbstractApplicationLoader {

    public static final String SERVER_CONFIG_LOCATION = "ponysdk.spring.application.server.configuration.file";
    public static final String CLIENT_CONFIG_LOCATION = "ponysdk.spring.application.client.configuration.file";

    private static final Logger log = LoggerFactory.getLogger(SpringApplicationLoader.class);

    private XmlWebApplicationContext context;

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        log.info("Closing Spring context");
        try {
            if (this.context != null) {
                this.context.close();
            }
        } catch (final Exception e) {
            log.error("Failure during Spring context closure", e);
        }

        super.contextDestroyed(event);
    }

    @Override
    public AbstractApplicationManager createApplicationManager(final ApplicationManagerOption applicationManagerOption) {
        return new AbstractApplicationManager(applicationManagerOption) {

            @Override
            protected EntryPoint initializeUIContext(final UIContext uiContext) {
                final List<String> configurations = new ArrayList<>();

                final String clientConfigFile = applicationManagerOption.getClientConfigFile();
                if (StringUtils.isEmpty(clientConfigFile))
                    configurations.addAll(Arrays.asList("conf/client_application.inc.xml", "etc/client_application.xml"));
                else configurations.add(clientConfigFile);

                EntryPoint entryPoint = null;

                try (final ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                        configurations.toArray(new String[0]))) {
                    entryPoint = applicationContext.getBean(EntryPoint.class);

                    final Map<String, InitializingActivity> initializingPages = applicationContext
                            .getBeansOfType(InitializingActivity.class);
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
