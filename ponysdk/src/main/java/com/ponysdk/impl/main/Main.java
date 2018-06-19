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

import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.JavaApplicationManager;
import com.ponysdk.core.server.context.CommunicationSanityChecker;
import com.ponysdk.core.ui.main.EntryPoint;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main {


    @SuppressWarnings("unchecked")
    public static void main(final String[] args) throws Exception {
        final ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setApplicationID(System.getProperty(ApplicationConfiguration.APPLICATION_ID, "ID"));
        configuration.setApplicationName(System.getProperty(ApplicationConfiguration.APPLICATION_NAME, "NAME"));
        configuration
            .setApplicationDescription(System.getProperty(ApplicationConfiguration.APPLICATION_DESCRIPTION, "DESCRIPTION"));
        configuration.setApplicationContextName(System.getProperty(ApplicationConfiguration.APPLICATION_CONTEXT_NAME, ""));
        configuration.setSessionTimeout(1000);
        configuration.setEntryPointClass((Class<? extends EntryPoint>) Class
            .forName(System.getProperty(ApplicationConfiguration.POINTCLASS, "com.ponysdk.impl.main.BasicEntryPoint")));

        final String styles = System.getProperty(ApplicationConfiguration.STYLESHEETS);
        if (styles != null && !styles.isEmpty()) {
            configuration
                .setStyle(Arrays.stream(styles.trim().split(";")).collect(Collectors.toMap(Function.identity(), Function.identity())));
        }

        final String scripts = System.getProperty(ApplicationConfiguration.JAVASCRIPTS);
        if (scripts != null && !scripts.isEmpty()) {
            configuration.setJavascript(Arrays.stream(scripts.trim().split(";")).collect(Collectors.toSet()));
        }

        final PonySDKServer ponySDKServer = new PonySDKServer();
        JavaApplicationManager javaApplicationManager = new JavaApplicationManager();
        javaApplicationManager.setConfiguration(configuration);
        ponySDKServer.setApplicationManager(javaApplicationManager);

        long period = configuration.getHeartBeatPeriodTimeUnit().toMillis(configuration.getHeartBeatPeriod());
        CommunicationSanityChecker communicationSanityChecker = new CommunicationSanityChecker(Duration.ofMillis(period));
        ponySDKServer.setCommunicationSanityChecker(communicationSanityChecker);

        ponySDKServer.setPort(8081);
        ponySDKServer.setHost("0.0.0.0");
        ponySDKServer.setUseSSL(false);
        ponySDKServer.start();
    }

}
