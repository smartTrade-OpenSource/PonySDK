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

package com.ponysdk.sample.client;

import com.ponysdk.core.server.context.UIContextImpl;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PSimpleLayoutPanel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.eventbus.EventBus;
import com.ponysdk.core.ui.main.EntryPoint;
import com.ponysdk.core.ui.place.DefaultPlaceHistoryMapper;
import com.ponysdk.core.ui.place.PlaceController;
import com.ponysdk.core.ui.place.PlaceHistoryHandler;
import com.ponysdk.core.ui.place.PlaceHistoryMapper;
import com.ponysdk.sample.client.place.LoginPlace;

public class TradingSampleEntryPoint implements EntryPoint {

    public static final String USER = "user";

    @Override
    public void start(final UIContextImpl uiContext) {
        if (uiContext.getApplication().getAttribute(USER) == null) uiContext.getHistory().newItem("", false);
        final PSimpleLayoutPanel panel = Element.newPSimpleLayoutPanel();
        PWindow.getMain().add(panel);

        final EventBus eventBus = UIContextImpl.getRootEventBus();

        final PlaceHistoryMapper historyMapper = new DefaultPlaceHistoryMapper(eventBus);
        final PlaceController placeController = new PlaceController(uiContext.getHistory(), eventBus);

        final PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(uiContext.getHistory(), historyMapper, placeController,
            eventBus);
        historyHandler.setDefaultPlace(new LoginPlace());
        historyHandler.handleCurrentHistory();

        PWindow.getMain().add(createReconnectionPanel());
    }

    private PElement createReconnectionPanel() {
        final PElement pElement = Element.newDiv();
        pElement.setAttribute("id", "reconnection");
        return pElement;
    }
}
