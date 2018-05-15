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

package com.ponysdk.impl.java;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ponysdk.core.server.application.ApplicationManagerOption;
import com.ponysdk.core.server.servlet.ApplicationLoader;
import com.ponysdk.core.ui.main.EntryPoint;
import com.ponysdk.impl.java.server.JavaApplicationLoader;
import com.ponysdk.impl.main.PonySDKServer;

public class Main {

    public static void main(final String[] args) throws Exception {
        final ApplicationManagerOption applicationManagerOption = new ApplicationManagerOption();
        applicationManagerOption.setApplicationID(System.getProperty(ApplicationManagerOption.APPLICATION_ID, "ID"));
        applicationManagerOption.setApplicationName(System.getProperty(ApplicationManagerOption.APPLICATION_NAME, "NAME"));
        applicationManagerOption
            .setApplicationDescription(System.getProperty(ApplicationManagerOption.APPLICATION_DESCRIPTION, "DESCRIPTION"));
        applicationManagerOption.setApplicationContextName(System.getProperty(ApplicationManagerOption.APPLICATION_CONTEXT_NAME, ""));
        applicationManagerOption.setSessionTimeout(1000);
        applicationManagerOption.setEntryPointClass((Class<? extends EntryPoint>) Class
            .forName(System.getProperty(ApplicationManagerOption.POINTCLASS, "com.ponysdk.impl.main.BasicEntryPoint")));

        final String styles = System.getProperty(ApplicationManagerOption.STYLESHEETS);
        if (styles != null && !styles.isEmpty()) {
            applicationManagerOption
                .setStyle(Arrays.stream(styles.trim().split(";")).collect(Collectors.toMap(Function.identity(), Function.identity())));
        }

        final String scripts = System.getProperty(ApplicationManagerOption.JAVASCRIPTS);
        if (scripts != null && !scripts.isEmpty()) {
            applicationManagerOption.setJavascript(Arrays.stream(scripts.trim().split(";")).collect(Collectors.toSet()));
        }

        final ApplicationLoader applicationLoader = new JavaApplicationLoader();
        applicationLoader.setApplicationManagerOption(applicationManagerOption);

        final PonySDKServer ponySDKServer = new PonySDKServer();
        ponySDKServer.setApplicationLoader(applicationLoader);
        ponySDKServer.setPort(8081);
        ponySDKServer.setHost("0.0.0.0");
        ponySDKServer.setUseSSL(false);
        ponySDKServer.start();
    }

}
