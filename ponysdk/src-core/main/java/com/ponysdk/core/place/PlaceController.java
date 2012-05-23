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

package com.ponysdk.core.place;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.PonySession;
import com.ponysdk.core.event.PEventBus;
import com.ponysdk.ui.server.basic.PHistory;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;

public class PlaceController implements PValueChangeHandler<String>, PlaceChangeRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(PlaceController.class);

    private final PHistory history;

    // private final Map<String, Place> placeContextByToken = new ConcurrentHashMap<String, Place>();

    public PlaceController(final PHistory history, final PEventBus eventBus) {
        this.history = history;
        // this.history.addValueChangeHandler(this);

        eventBus.addHandler(PlaceChangeRequestEvent.TYPE, this);
    }

    @Override
    public void onPlaceChange(final PlaceChangeRequestEvent event) {
        goTo(event.getPlace());
    }

    /**
     * GoTo Page with history
     * 
     * @param activity
     * @param place
     * @param world
     */
    public void goTo(final Place place) {
        final String token = place.getToken();
        // placeContextByToken.put(place.getToken(), place);

        // history.newItem(token, false);
        PonySession.getRootEventBus().fireEvent(new PlaceChangeEvent(this, place));
    }

    @Override
    public void onValueChange(final PValueChangeEvent<String> event) {

        // final Place place = placeContextByToken.get(event.getValue());
        //
        // if (place == null) {
        // log.warn("No context found for this token #" + event.getValue());
        // return;
        // }
        //
        // PonySession.getRootEventBus().fireEvent(new PlaceChangeEvent(this, place));
    }

}
