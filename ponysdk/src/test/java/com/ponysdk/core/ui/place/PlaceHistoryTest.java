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

package com.ponysdk.core.ui.place;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.PHistory;
import com.ponysdk.core.ui.eventbus.EventBus;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.test.PSuite;

public class PlaceHistoryTest extends PSuite {

    static class HomePlace implements Place {
    }

    static class SettingsPlace implements Place {
    }

    private final List<Place> places = new ArrayList<>();
    private HandlerRegistration registration;

    @BeforeEach
    public void listen() {
        registration = UIContext.get().getRootEventBus().addHandler(PlaceChangeEvent.TYPE,
            (PlaceChangeHandler) event -> places.add(event.getNewPlace()));
    }

    @AfterEach
    public void stopListening() {
        registration.removeHandler();
    }

    @Test
    public void testPlaceChangeEvents() {
        final Place home = new HomePlace();
        final PlaceChangeEvent event = new PlaceChangeEvent(this, home);
        assertSame(home, event.getNewPlace());
        assertSame(this, event.getSource());
        assertSame(PlaceChangeEvent.TYPE, event.getAssociatedType());

        final PlaceChangeRequestEvent request = new PlaceChangeRequestEvent(this, home);
        assertSame(home, request.getPlace());
        assertSame(PlaceChangeRequestEvent.TYPE, request.getAssociatedType());

        PlaceChangeRequest.fire(this, Place.NOWHERE);
        assertEquals(List.of(Place.NOWHERE), places);
    }

    @Test
    public void testPlaceControllerAnswersRequests() {
        final EventBus bus = new EventBus();
        final PlaceController controller = new PlaceController(new PHistory(), bus);
        final Place home = new HomePlace();
        bus.fireEvent(new PlaceChangeRequestEvent(this, home));
        assertEquals(List.of(home), places);
        controller.goTo(Place.NOWHERE);
        assertEquals(List.of(home, Place.NOWHERE), places);
    }

    @Test
    public void testDefaultPlaceHistoryMapper() {
        final EventBus bus = new EventBus();
        final DefaultPlaceHistoryMapper mapper = new DefaultPlaceHistoryMapper(bus);
        final Place home = new HomePlace();
        assertEquals("HomePlace", mapper.getToken(home));
        assertNull(mapper.getPlace("HomePlace"));
        bus.fireEvent(new PlaceChangeEvent(this, home));
        assertSame(home, mapper.getPlace("HomePlace"));

        final PlaceTokenizer<Place> tokenizer = new PlaceTokenizer<>() {

            @Override
            public Place getPlace(final String token) {
                return home;
            }

            @Override
            public String getToken(final Place place) {
                return "home";
            }
        };
        mapper.setPlaceTokenizer(tokenizer);
        assertSame(tokenizer, mapper.placeTokenizer);
    }

    @Test
    public void testHistoryDrivesPlaces() {
        final EventBus bus = UIContext.get().getRootEventBus();
        final PHistory history = new PHistory();
        final DefaultPlaceHistoryMapper mapper = new DefaultPlaceHistoryMapper(bus);
        final PlaceController controller = new PlaceController(history, bus);
        final PlaceHistoryHandler handler = new PlaceHistoryHandler(history, mapper, controller, bus);

        final Place home = new HomePlace();
        final Place settings = new SettingsPlace();

        // Unknown token and no default place: nothing happens
        history.fireHistoryChanged("Nowhere");
        assertEquals(List.of(), places);

        // A place change records a history token
        controller.goTo(home);
        assertEquals("HomePlace", history.getToken());
        controller.goTo(settings);
        assertEquals("SettingsPlace", history.getToken());
        places.clear();

        // Back to a known token
        history.fireHistoryChanged("HomePlace");
        assertEquals(List.of(home), places);

        handler.setDefaultPlace(settings);
        history.fireHistoryChanged("");
        assertEquals(List.of(home, settings), places);
        history.fireHistoryChanged("Unknown");
        assertEquals(List.of(home, settings, settings), places);
        history.fireHistoryChanged(null);
        assertEquals(4, places.size());

        history.fireHistoryChanged("HomePlace");
        places.clear();
        handler.handleCurrentHistory();
        assertEquals(List.of(home), places);
    }
}
