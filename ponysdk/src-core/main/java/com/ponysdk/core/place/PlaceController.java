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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.activity.Activity;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.basic.PHistory;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;

public class PlaceController implements PValueChangeHandler<String> {

    private static final Logger log = LoggerFactory.getLogger(PlaceController.class);

    private PHistory history;

    private final Map<String, PlaceContext> placeContextByToken = new ConcurrentHashMap<String, PlaceContext>();

    /**
     * GoTo Page without history
     * 
     * @param placeContext
     */
    public void registerPlaceContext(PlaceContext placeContext) {
        placeContextByToken.put(placeContext.getPlace().getToken(), placeContext);
        placeContext.getActivity().goTo(placeContext.getPlace(), placeContext.getWorld());
    }

    /**
     * GoTo Page with history
     * 
     * @param activity
     * @param place
     * @param world
     */
    public void goTo(Activity activity, Place place, PAcceptsOneWidget world) {
        final PlaceContext context = new PlaceContext();
        context.setPlace(place);
        context.setActivity(activity);
        context.setWorld(world);

        final String token = place.getToken();

        placeContextByToken.put(token, context);

        history.newItem(token);
    }

    @Override
    public void onValueChange(String value) {
        final PlaceContext placeContext = placeContextByToken.get(value);

        if (placeContext == null) {
            log.warn("No context found for this token #" + value);
            return;
        }

        placeContext.getActivity().goTo(placeContext.getPlace(), placeContext.getWorld());
    }

    public void setHistory(PHistory history){
        this.history = history;
    	this.history.addValueChangeHandler(this);
    }
    
    public PlaceContext getPlaceContext(String token) {
        return placeContextByToken.get(token);
    }

}
