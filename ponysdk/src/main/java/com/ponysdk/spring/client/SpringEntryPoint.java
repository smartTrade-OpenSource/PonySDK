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

package com.ponysdk.spring.client;

import org.springframework.beans.factory.annotation.Autowired;

import com.ponysdk.core.ui.activity.ActivityManager;
import com.ponysdk.core.ui.activity.ActivityMapper;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PHistory;
import com.ponysdk.core.ui.basic.PRootLayoutPanel;
import com.ponysdk.core.ui.basic.PSimpleLayoutPanel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.eventbus.EventBus;
import com.ponysdk.core.ui.main.EntryPoint;
import com.ponysdk.core.ui.place.Place;
import com.ponysdk.core.ui.place.PlaceController;
import com.ponysdk.core.ui.place.PlaceHistoryHandler;
import com.ponysdk.core.ui.place.PlaceHistoryMapper;

public abstract class SpringEntryPoint implements EntryPoint {

    @Autowired
    protected PlaceController placeController;

    @Autowired
    protected EventBus eventBus;

    @Autowired
    protected PHistory history;

    @Autowired
    protected ActivityMapper mapper;

    @Autowired
    protected PlaceHistoryMapper historyMapper;

    protected void start(final Place place) {
        final PSimpleLayoutPanel panel = Element.newPSimpleLayoutPanel();
        PRootLayoutPanel.get(PWindow.getMain().getID()).add(panel);

        final ActivityManager activityManager = new ActivityManager(mapper);
        activityManager.setDisplay(panel);

        final PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(history, historyMapper, placeController, eventBus);
        historyHandler.setDefaultPlace(place);
        historyHandler.handleCurrentHistory();
    }

}
