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

package com.ponysdk.sample.trading.client;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.activity.ActivityManager;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.place.DefaultPlaceHistoryMapper;
import com.ponysdk.core.place.PlaceController;
import com.ponysdk.core.place.PlaceHistoryHandler;
import com.ponysdk.core.place.PlaceHistoryMapper;
import com.ponysdk.sample.trading.client.activity.SampleActivityMapper;
import com.ponysdk.sample.trading.client.place.LoginPlace;
import com.ponysdk.ui.server.basic.PElement;
import com.ponysdk.ui.server.basic.PRootLayoutPanel;
import com.ponysdk.ui.server.basic.PRootPanel;
import com.ponysdk.ui.server.basic.PSimpleLayoutPanel;

public class TradingSampleEntryPoint implements EntryPoint {

    public static final String USER = "user";

    @Override
    public void start(final UIContext uiContext) {
        if (uiContext.getApplicationAttribute(USER) == null) uiContext.getHistory().newItem("", false);
        start0(uiContext);
    }

    @Override
    public void restart(final UIContext uiContext) {
        if (uiContext.getApplicationAttribute(USER) == null) uiContext.getHistory().newItem("", false);
        start0(uiContext);
    }

    private void start0(final UIContext uiContext) {

        final PSimpleLayoutPanel panel = new PSimpleLayoutPanel();
        PRootLayoutPanel.get().add(panel);

        final EventBus eventBus = UIContext.getRootEventBus();

        final SampleActivityMapper mapper = new SampleActivityMapper();
        final PlaceHistoryMapper historyMapper = new DefaultPlaceHistoryMapper(eventBus);
        final PlaceController placeController = new PlaceController(uiContext.getHistory(), eventBus);

        final ActivityManager activityManager = new ActivityManager(mapper);
        activityManager.setDisplay(panel);

        final PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(uiContext.getHistory(), historyMapper, placeController,
                eventBus);
        historyHandler.setDefaultPlace(new LoginPlace());
        historyHandler.handleCurrentHistory();

        PRootPanel.get().add(createReconnectionPanel());
    }

    private PElement createReconnectionPanel() {
        final PElement pElement = new PElement("div");
        pElement.setAttribute("id", "reconnection");
        return pElement;
    }
}
