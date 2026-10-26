/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.sample.quickstart;

import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.impl.java.server.JavaApplicationManager;
import com.ponysdk.impl.main.PonySDKServer;

/**
 * The quick start (README.md, ARCHITECTURE.md section 2): {@code ./gradlew :sample:runQuickStart}, then
 * http://localhost:8080/hello/. QuickStartTest starts it.
 */
public final class HelloWorldMain {

    private HelloWorldMain() {
    }

    public static void main(final String[] args) throws Exception {
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        start(port);
        System.out.println("Open http://localhost:" + port + "/hello/");
    }

    static PonySDKServer start(final int port) throws Exception {
        final ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setApplicationName("Hello");
        configuration.setApplicationContextName("hello"); // the application's URL: http://localhost:8080/hello/
        configuration.setEntryPointClass(HelloWorldEntryPoint.class);

        final JavaApplicationManager applicationManager = new JavaApplicationManager();
        applicationManager.setConfiguration(configuration);

        final PonySDKServer server = new PonySDKServer();
        server.setApplicationManager(applicationManager);
        server.setPort(port);
        server.setUseSSL(false); // plain HTTP on this machine; in production, HTTPS with a key store
        server.setSessionCookieSecure(false); // a Secure cookie is not sent over plain HTTP
        server.start();
        return server;
    }
}
