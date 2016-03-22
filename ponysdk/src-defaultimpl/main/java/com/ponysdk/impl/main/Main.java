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

import java.util.Arrays;

import com.ponysdk.core.ApplicationManagerOption;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.servlet.ApplicationLoader;
import com.ponysdk.core.servlet.JavaApplicationLoader;

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
            applicationManagerOption.setStyle(Arrays.asList(styles.trim().split(";")));
        }

        final String scripts = System.getProperty(ApplicationManagerOption.JAVASCRIPTS);
        if (scripts != null && !scripts.isEmpty()) {
            applicationManagerOption.setJavascript(Arrays.asList(scripts.trim().split(";")));
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
