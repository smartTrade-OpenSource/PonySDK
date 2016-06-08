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

import java.util.HashMap;
import java.util.Map;

import com.ponysdk.core.ui.eventbus.EventBus;

public class DefaultPlaceHistoryMapper implements PlaceHistoryMapper {

    protected final Map<String, Place> placeContextByToken = new HashMap<>();

    protected PlaceTokenizer<Place> placeTokenizer;

    public DefaultPlaceHistoryMapper(final EventBus eventBus) {
        eventBus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeHandler() {

            @Override
            public void onPlaceChange(final PlaceChangeEvent event) {
                final Place place = event.getNewPlace();
                placeContextByToken.put(getToken(place), place);
            }
        });
    }

    @Override
    public Place getPlace(final String token) {
        return placeContextByToken.get(token);
    }

    @Override
    public String getToken(final Place place) {
        return place.getClass().getSimpleName();
    }

    public void setPlaceTokenizer(final PlaceTokenizer<Place> placeTokenizer) {
        this.placeTokenizer = placeTokenizer;
    }
}
