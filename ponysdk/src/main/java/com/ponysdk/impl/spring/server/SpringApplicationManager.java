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

package com.ponysdk.impl.spring.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.util.StringUtils;

import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.ui.activity.InitializingActivity;
import com.ponysdk.core.ui.main.EntryPoint;

public class SpringApplicationManager extends ApplicationManager implements ApplicationContextAware {

    public static final String SERVER_CONFIG_LOCATION = "ponysdk.spring.application.server.configuration.file";

    private String[] configurations;
    private ApplicationContext serverApplicationContext;

    @Override
    public void start() {
        final List<String> files = new ArrayList<>();

        final String clientConfigFile = configuration.getClientConfigFile();
        if (StringUtils.isEmpty(clientConfigFile))
            files.addAll(Arrays.asList("conf/client_application.inc.xml", "etc/client_application.xml"));
        else files.add(clientConfigFile);

        configurations = files.toArray(new String[0]);
    }

    @Override
    protected EntryPoint initializeEntryPoint() {
        try (GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext()) {
            applicationContext.getEnvironment().setActiveProfiles(serverApplicationContext.getEnvironment().getActiveProfiles());
            applicationContext.load(configurations);
            applicationContext.refresh();

            final EntryPoint entryPoint = applicationContext.getBean(EntryPoint.class);

            final Map<String, InitializingActivity> initializingPages = applicationContext.getBeansOfType(InitializingActivity.class);
            initializingPages.values().forEach(InitializingActivity::afterContextInitialized);

            return entryPoint;
        }
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.serverApplicationContext = applicationContext;
    }
}
