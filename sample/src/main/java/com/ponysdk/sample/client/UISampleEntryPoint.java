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

import com.ponysdk.core.UIContext;
import com.ponysdk.core.activity.ActivityManager;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.place.PlaceController;
import com.ponysdk.core.place.PlaceHistoryHandler;
import com.ponysdk.core.place.PlaceHistoryMapper;
import com.ponysdk.impl.webapplication.page.InitializingActivity;
import com.ponysdk.sample.client.activity.SampleActivityMapper;
import com.ponysdk.sample.client.event.UserLoggedOutEvent;
import com.ponysdk.sample.client.event.UserLoggedOutHandler;
import com.ponysdk.sample.client.place.LoginPlace;
import com.ponysdk.ui.server.basic.PHistory;
import com.ponysdk.ui.server.basic.PPusher;
import com.ponysdk.ui.server.basic.PRootLayoutPanel;
import com.ponysdk.ui.server.basic.PSimpleLayoutPanel;

public class UISampleEntryPoint implements EntryPoint, UserLoggedOutHandler, InitializingActivity {

    public static final String USER = "user";

    @Autowired
    private PlaceController placeController;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private PHistory history;

    @Autowired
    private SampleActivityMapper mapper;

    @Autowired
    private PlaceHistoryMapper historyMapper;

    private void start() {
        final PSimpleLayoutPanel panel = new PSimpleLayoutPanel();
        PRootLayoutPanel.get().add(panel);

        PPusher.initialize();

        final ActivityManager activityManager = new ActivityManager(mapper);
        activityManager.setDisplay(panel);

        final PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(history, historyMapper, placeController, eventBus);
        historyHandler.setDefaultPlace(new LoginPlace());
        historyHandler.handleCurrentHistory();
    }

    @Override
    public void start(final UIContext uiContext) {
        if (uiContext.getApplicationAttribute(USER) == null) uiContext.getHistory().newItem("", false);
        start();
    }

    @Override
    public void restart(final UIContext session) {
        if (session.getApplicationAttribute(USER) == null) session.getHistory().newItem("", false);
        start();
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
