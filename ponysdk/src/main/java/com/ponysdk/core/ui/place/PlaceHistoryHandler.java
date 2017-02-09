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

package com.ponysdk.core.ui.place;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.ui.basic.PHistory;
import com.ponysdk.core.ui.eventbus.EventBus;

public class PlaceHistoryHandler {

    private static final Logger log = LoggerFactory.getLogger(PlaceHistoryHandler.class);

    private final PHistory history;
    private final PlaceHistoryMapper mapper;
    private final PlaceController placeController;

    private Place defaultPlace;

    public PlaceHistoryHandler(final PHistory history, final PlaceHistoryMapper mapper, final PlaceController placeController,
            final EventBus eventBus) {
        this.history = history;
        this.mapper = mapper;
        this.placeController = placeController;

        eventBus.addHandler(PlaceChangeEvent.TYPE,
            (PlaceChangeHandler) event -> history.newItem(mapper.getToken(event.getNewPlace()), false));

        history.addValueChangeHandler((event) -> handleHistoryToken(event.getValue()));
    }

    public void setDefaultPlace(final Place defaultPlace) {
        this.defaultPlace = defaultPlace;
    }

    public void handleCurrentHistory() {
        handleHistoryToken(history.getToken());
    }

    private void handleHistoryToken(final String token) {

        Place newPlace = null;

        if (token == null || token.isEmpty()) {
            newPlace = defaultPlace;
        }

        if (newPlace == null) {
            newPlace = mapper.getPlace(token);
        }

        if (newPlace == null) {
            if (defaultPlace != null) {
                newPlace = defaultPlace;
                log.warn("Unrecognized history token: " + token + ". Going to default place: " + defaultPlace);
            } else {
                log.warn("Unrecognized history token: " + token);
                return;
            }
        }

        placeController.goTo(newPlace);
    }

}