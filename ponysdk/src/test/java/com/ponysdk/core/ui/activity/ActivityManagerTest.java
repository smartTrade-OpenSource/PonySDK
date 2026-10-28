/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.core.ui.activity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.eventbus.Event;
import com.ponysdk.core.ui.eventbus.EventBus;
import com.ponysdk.core.ui.eventbus.EventHandler;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.place.Place;
import com.ponysdk.core.ui.place.PlaceChangeEvent;
import com.ponysdk.core.ui.place.PlaceChangeHandler;
import com.ponysdk.core.ui.place.PlaceChangeRequest;
import com.ponysdk.test.PSuite;

public class ActivityManagerTest extends PSuite {

    private static final Event.Type PING = new Event.Type();

    private interface PingHandler extends EventHandler {

        void onPing();
    }

    private static class PingEvent extends Event<PingHandler> {

        PingEvent() {
            super(null);
        }

        @Override
        public Event.Type getAssociatedType() {
            return PING;
        }

        @Override
        protected void dispatch(final PingHandler handler) {
            handler.onPing();
        }
    }

    private static class TestActivity extends AbstractActivity<PLabel> {

        private int builds;
        private final List<Place> updates = new ArrayList<>();
        private int stops;

        TestActivity(final String text) {
            view = Element.newPLabel(text);
        }

        @Override
        protected void buildView() {
            builds++;
            assertTrue(view.hasStyleName("pony-LoadingBox"));
        }

        @Override
        protected void updateView(final Place place) {
            updates.add(place);
        }

        @Override
        public void stop() {
            super.stop();
            stops++;
        }

        boolean isStarted() {
            return started;
        }
    }

    private static final Place HOME = new Place() {
    };
    private static final Place SETTINGS = new Place() {
    };
    private static final Place UNKNOWN = new Place() {
    };

    @Test
    public void testActivityLifecycle() {
        final TestActivity home = new TestActivity("home");
        final TestActivity settings = new TestActivity("settings");
        final Map<Place, Activity> activities = new HashMap<>();
        activities.put(HOME, home);
        activities.put(SETTINGS, settings);
        final ActivityManager manager = new ActivityManager(activities::get);

        // No display yet
        PlaceChangeRequest.fire(this, HOME);
        assertFalse(home.isStarted());

        final PSimplePanel world = Element.newPSimplePanel();
        manager.setDisplay(world);
        PlaceChangeRequest.fire(this, HOME);
        assertTrue(home.isStarted());
        assertSame(home.getView(), world.getWidget());
        assertFalse(home.getView().hasStyleName("pony-LoadingBox"));
        assertEquals(1, home.builds);
        assertEquals(List.of(HOME), home.updates);

        // Same activity again: not stopped, view built once
        PlaceChangeRequest.fire(this, HOME);
        assertEquals(0, home.stops);
        assertEquals(1, home.builds);
        assertEquals(2, home.updates.size());

        home.goTo(SETTINGS);
        assertEquals(1, home.stops);
        assertFalse(home.isStarted());
        assertTrue(settings.isStarted());
        assertSame(settings.getView(), world.getWidget());

        // A place without activity changes nothing
        PlaceChangeRequest.fire(this, UNKNOWN);
        assertTrue(settings.isStarted());
        assertSame(settings.getView(), world.getWidget());
    }

    @Test
    public void testManagerOnDedicatedEventBus() {
        final EventBus eventBus = new EventBus();
        final TestActivity activity = new TestActivity("a");
        final ActivityManager manager = new ActivityManager(place -> activity, eventBus);
        final PSimplePanel world = Element.newPSimplePanel();
        manager.setDisplay(world);
        eventBus.fireEvent(new PlaceChangeEvent(this, HOME));
        assertTrue(activity.isStarted());
        assertSame(activity.getView(), world.getWidget());
    }

    @Test
    public void testActivityEventBusShortcuts() {
        final TestActivity activity = new TestActivity("a");
        final int[] pings = { 0 };
        final PingHandler handler = () -> pings[0]++;
        final HandlerRegistration registration = activity.addHandler(PING, handler);
        activity.fireEvent(new PingEvent());
        assertEquals(1, pings[0]);
        activity.removeHandler(PING, handler);
        activity.fireEvent(new PingEvent());
        assertEquals(1, pings[0]);
        registration.removeHandler();

        final List<Event<?>> broadcast = new ArrayList<>();
        final com.ponysdk.core.ui.eventbus.BroadcastEventHandler broadcastHandler = broadcast::add;
        activity.addHandler(broadcastHandler);
        activity.fireEvent(new PingEvent());
        assertEquals(1, broadcast.size());
        activity.removeHandler(broadcastHandler);
        activity.fireEvent(new PingEvent());
        assertEquals(1, broadcast.size());

        final List<Place> places = new ArrayList<>();
        final HandlerRegistration placeRegistration = UIContext.get().getRootEventBus().addHandler(PlaceChangeEvent.TYPE,
            (PlaceChangeHandler) event -> places.add(event.getNewPlace()));
        activity.goTo(HOME);
        placeRegistration.removeHandler();
        assertEquals(List.of(HOME), places);
    }

    @Test
    public void anActivityWithoutHooksShowsItsView() {
        final AbstractActivity<PLabel> activity = new AbstractActivity<>() {
        };
        activity.view = Element.newPLabel("plain");
        final PSimplePanel world = Element.newPSimplePanel();

        activity.start(world, HOME);
        activity.start(world, SETTINGS);

        assertSame(activity.getView(), world.getWidget());
        assertFalse(activity.getView().hasStyleName("pony-LoadingBox"));
        assertFalse(activity.firstStart);
    }
}
