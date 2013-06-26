/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.sample.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.impl.webapplication.page.InitializingActivity;
import com.ponysdk.impl.webapplication.page.place.LoginPlace;
import com.ponysdk.sample.client.event.UserLoggedOutEvent;
import com.ponysdk.sample.client.event.UserLoggedOutHandler;
import com.ponysdk.spring.client.SpringEntryPoint;
import com.ponysdk.ui.server.basic.PScript;
import com.ponysdk.ui.server.basic.PScript.ExecutionCallback;

public class UISampleEntryPoint extends SpringEntryPoint implements EntryPoint, UserLoggedOutHandler, InitializingActivity {

    public static final String USER = "user";

    private static Logger log = LoggerFactory.getLogger(UISampleEntryPoint.class);

    @Override
    public void start(final UIContext uiContext) {
        if (uiContext.getApplicationAttribute(USER) == null) uiContext.getHistory().newItem("", false);

        script();

        start(new LoginPlace());
    }

    private void script() {
        PScript.get().execute("less.watch();");
        final StringBuilder builder = new StringBuilder();
        builder.append("less.watch();");

        PScript.get().execute(builder.toString());

        PScript.get().execute("if ('ontouchstart' in document) { document.documentElement.className +=' touch ' ; } else { document.documentElement.className +=' notouch ' ; } ", new ExecutionCallback() {

            @Override
            public void onSuccess(final String msg) {
                log.info("Touch support ? " + msg);
            }

            @Override
            public void onFailure(final String msg) {}
        });

    }

    @Override
    public void restart(final UIContext session) {
        if (session.getApplicationAttribute(USER) == null) session.getHistory().newItem("", false);

        script();

        start(new LoginPlace());
    }

    @Override
    public void onUserLoggedOut(final UserLoggedOutEvent event) {
        UIContext.get().close();
    }

    @Override
    public void afterContextInitialized() {
        eventBus.addHandler(UserLoggedOutEvent.TYPE, this);
    }

}
