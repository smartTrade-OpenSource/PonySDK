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

import org.springframework.beans.factory.annotation.Autowired;

import com.ponysdk.core.PonySession;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.place.PlaceController;
import com.ponysdk.impl.webapplication.application.ApplicationActivity;
import com.ponysdk.impl.webapplication.page.InitializingActivity;
import com.ponysdk.sample.client.event.UserLoggedOutEvent;
import com.ponysdk.sample.client.event.UserLoggedOutHandler;
import com.ponysdk.sample.client.place.LoginPlace;
import com.ponysdk.ui.server.basic.PRootLayoutPanel;
import com.ponysdk.ui.server.basic.PSimpleLayoutPanel;

public class UISampleEntryPoint implements EntryPoint, UserLoggedOutHandler, InitializingActivity {

    public static final String USER = "user";

    @Autowired
    private ApplicationActivity applicationActivity;

    @Autowired
    private LoginActivity loginActivity;

    @Autowired
    private PlaceController placeController;

    @Autowired
    private EventBus eventBus;

    final PSimpleLayoutPanel panel = new PSimpleLayoutPanel();

    @Override
    public void start(final PonySession session) {
        panel.setSizeFull();
        PRootLayoutPanel.get().add(panel);
        placeController.goTo(loginActivity, new LoginPlace(), panel);
    }

    @Override
    public void restart(final PonySession session) {
        if (session.getApplicationAttribute(USER) == null) {
            start(session);
        } else {
            panel.setSizeFull();
            PRootLayoutPanel.get().add(panel);
            final String currentToken = session.getHistory().getToken();
            applicationActivity.start(panel);
            PonySession.getCurrent().getHistory().newItem(currentToken);
        }
    }

    @Override
    public void onUserLoggedOut(final UserLoggedOutEvent event) {
        PonySession.getCurrent().close();
    }

    @Override
    public void afterContextInitialized() {
        eventBus.addHandler(UserLoggedOutEvent.TYPE, this);
    }

}
