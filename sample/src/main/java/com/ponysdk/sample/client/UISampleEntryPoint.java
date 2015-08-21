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

import com.ponysdk.core.ClientDataOutput;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.socket.ConnectionListener;
import com.ponysdk.impl.webapplication.page.InitializingActivity;
import com.ponysdk.impl.webapplication.page.place.LoginPlace;
import com.ponysdk.sample.client.event.UserLoggedOutEvent;
import com.ponysdk.sample.client.event.UserLoggedOutHandler;
import com.ponysdk.spring.client.SpringEntryPoint;
import com.ponysdk.ui.server.basic.PObject;
import com.ponysdk.ui.server.basic.PPusher;

public class UISampleEntryPoint extends SpringEntryPoint implements EntryPoint, UserLoggedOutHandler, InitializingActivity, ConnectionListener {

    public static final String USER = "user";

    @Override
    public void start(final UIContext uiContext) {
        uiContext.setClientDataOutput(new ClientDataOutput() {

            @Override
            public void onClientData(final PObject object, final JSONObject instruction) {
                System.err.println(object + "" + instruction);
            }
        });

        if (uiContext.getApplicationAttribute(USER) == null) uiContext.getHistory().newItem("", false);
        script();
    }

    private void script() {
        PPusher.initialize();
        PPusher.get().addConnectionListener(this);
    }

    @Override
    public void restart(final UIContext session) {
        if (session.getApplicationAttribute(USER) == null) session.getHistory().newItem("", false);
        script();
    }

    @Override
    public void onUserLoggedOut(final UserLoggedOutEvent event) {
        UIContext.get().close();
    }

    @Override
    public void afterContextInitialized() {
        eventBus.addHandler(UserLoggedOutEvent.TYPE, this);
    }

    @Override
    public void onOpen() {
        // PScript.get().execute("less.watch();");
        start(new LoginPlace());
    }

    @Override
    public void onClose() {}

}
