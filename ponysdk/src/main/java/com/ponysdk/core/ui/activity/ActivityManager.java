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

package com.ponysdk.core.ui.activity;

import com.ponysdk.core.server.context.UIContextImpl;
import com.ponysdk.core.ui.basic.PAcceptsOneWidget;
import com.ponysdk.core.ui.eventbus.EventBus;
import com.ponysdk.core.ui.place.PlaceChangeEvent;
import com.ponysdk.core.ui.place.PlaceChangeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages {@link Activity} objects that should be kicked off in response to
 * {@link PlaceChangeEvent} events.
 */
public class ActivityManager implements PlaceChangeHandler {

    private static final Logger log = LoggerFactory.getLogger(ActivityManager.class);

    private static final Activity NULL_ACTIVITY = new AbstractActivity<>() {
    };

    private final ActivityMapper mapper;
    private PAcceptsOneWidget world;

    private Activity currentActivity = NULL_ACTIVITY;

    public ActivityManager(final ActivityMapper mapper) {
        this.mapper = mapper;

        UIContextImpl.get().getRootEventBus().addHandler(PlaceChangeEvent.TYPE, this);
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
        if (world == null) {
            if (log.isDebugEnabled()) log.debug("No world to display this place #{}", event);
            return;
        }

        final Activity activity = mapper.getActivity(event.getNewPlace());

        if (activity == null) return;

        if (!activity.equals(currentActivity)) {
            currentActivity.stop();
        }

        currentActivity = activity;
        currentActivity.start(world, event.getNewPlace());
    }
}
