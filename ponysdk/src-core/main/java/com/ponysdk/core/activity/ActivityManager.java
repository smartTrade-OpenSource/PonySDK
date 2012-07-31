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

package com.ponysdk.core.activity;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.place.PlaceChangeEvent;
import com.ponysdk.core.place.PlaceChangeHandler;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;

public class ActivityManager implements PlaceChangeHandler {

    private final ActivityMapper mapper;
    private PAcceptsOneWidget world;

    public ActivityManager(final ActivityMapper mapper) {
        this.mapper = mapper;

        UIContext.getRootEventBus().addHandler(PlaceChangeEvent.TYPE, this);
    }

    public ActivityManager(final ActivityMapper mapper, final EventBus eventBus) {
        this.mapper = mapper;

        eventBus.addHandler(PlaceChangeEvent.TYPE, this);
    }

    public void setDisplay(final PAcceptsOneWidget world) {
        this.world = world;
    }

    @Override
    public void onPlaceChange(final PlaceChangeEvent event) {
        final Activity activity = mapper.getActivity(event.getNewPlace());
        activity.start(world, event.getNewPlace());
    }

}
